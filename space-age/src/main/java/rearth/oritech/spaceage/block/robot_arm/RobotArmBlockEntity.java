package rearth.oritech.spaceage.block.robot_arm;

import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.addons.HeartOfTheMachineAddonEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Action;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Kind;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.Geometry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class RobotArmBlockEntity extends MultiblockMachineEntity {

    public static final int RANGE = RobotArmReach.RADIUS;
    public static final int REDSTONE_IGNORED = 0;
    public static final int REDSTONE_POWERED = 1;
    public static final int REDSTONE_PULSE = 2;
    public static final int REDSTONE_MODES = 3;
    private static final int DEFAULT_PHASE_TICKS = 20;
    private static final GameProfile ROBOT_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("oritech_robot_arm".getBytes(StandardCharsets.UTF_8)), "[Oritech Robot]");

    /** Each action moves out from the warp port or previous target, performs its work, then retracts. */
    public enum Phase { PREPARE, APPROACH, WORK, RETRACT }

    public RobotProgram program = RobotProgram.EMPTY;
    public int cursor;
    public boolean running;
    public boolean repeat;
    public boolean skipBlocked;
    public int redstoneMode = REDSTONE_IGNORED;
    public String status = "idle";

    @SyncField(SyncType.GUI_OPEN)
    public int yieldAddons;
    @SyncField(SyncType.GUI_OPEN)
    public boolean silkTouch;

    @SyncField({SyncType.INITIAL, SyncType.TICK})
    public Visual visual = new Visual(BlockPos.ZERO, Kind.PLACE, Phase.PREPARE, 0, DEFAULT_PHASE_TICKS, false, ItemStack.EMPTY, 0, BlockPos.ZERO, true);

    private final List<ItemStack> cargo = new ArrayList<>();
    private Phase phase = Phase.PREPARE;
    private int elapsed;
    private int duration = DEFAULT_PHASE_TICKS;
    private BlockPos motionStart;
    private boolean returnToWarp = true;
    private boolean powered;
    private boolean retry;
    private FakePlayer robot;
    private BlockPos breakingPos;
    private int breakingStage = -1;

    public RobotArmBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.ROBOT_ARM.get(), pos, state, 128);
        motionStart = pos;
    }

    public boolean inRange(BlockPos pos) {

        return pos.distSqr(worldPosition.above()) <= RANGE * RANGE && !pos.equals(worldPosition)
                && getCorePositions().stream().noneMatch(core -> worldPosition.offset(Geometry.rotatePosition(core, getFacing())).equals(pos));
    }

    public static AABB workBounds(BlockPos pos) {

        return new AABB(pos.above()).inflate(RANGE);
    }

    @Override
    public void serverTick(ServerLevel world, BlockPos pos, BlockState state, NetworkedBlockEntity entity) {

        addBurstTicks();
        if (robot != null) robot.getCooldowns().tick();
        var signal = world.hasNeighborSignal(pos);
        var rising = signal && !powered;
        if (powered != signal) {
            powered = signal;
            setChanged();
        }
        if (redstoneMode == REDSTONE_PULSE && rising && !running && phase == Phase.PREPARE) {
            cursor = 0;
            running = true;
        }
        var active = running && isAssembled(state) && !disabledViaRedstone && (redstoneMode != REDSTONE_POWERED || signal);
        if (!active || program.actions().isEmpty()) {
            updateVisual(false);
            return;
        }
        if (cursor >= program.actions().size()) cursor = 0;
        var action = program.actions().get(cursor);
        if (phase == Phase.PREPARE && !prepare(world, action)) {
            updateVisual(false);
            return;
        }
        if (!consumeEnergy()) {
            status = "no_energy";
            updateVisual(false);
            return;
        }
        consumeBurstTicks();
        status = "working";
        if (++elapsed >= duration) finishPhase(world, action);
        updateVisual(running);
        setChanged();
    }

    /** Checks the target and reserves the needed item before the arm starts moving. */
    private boolean prepare(ServerLevel world, Action action) {

        var issue = checkAction(action);
        var needsItem = action.kind() == Kind.PLACE || action.kind() == Kind.INTERACT && !action.item().isEmpty();
        if (issue.isEmpty() && needsItem && !reserve(action.item(), action.kind() == Kind.PLACE)) issue = "missing_item";
        if (!issue.isEmpty()) {
            status = issue;
            if (skipBlocked) advance();
            return false;
        }
        if (action.kind() == Kind.PLACE) warpParticles(world);
        phase = Phase.APPROACH;
        elapsed = 0;
        duration = actionDuration(action, phase, true);
        return true;
    }

    private boolean consumeEnergy() {

        try (var transaction = Transaction.openRoot()) {
            var cost = Math.max(1, (int) calculateEnergyUsage());
            if (energyStorage.internalExtract(cost, transaction) != cost) return false;
            transaction.commit();
            return true;
        }
    }

    private void finishPhase(ServerLevel world, Action action) {

        elapsed = 0;
        switch (phase) {
            case APPROACH -> phase = Phase.WORK;
            case WORK -> {
                var issue = execute(action);
                if (!issue.isEmpty()) {
                    // Keep the reserved item until it can return to storage, including on failure.
                    status = issue;
                    retry = !skipBlocked;
                }
                phase = Phase.RETRACT;
                var actions = program.actions();
                var next = cursor + 1;
                if (next == actions.size() && repeats()) next = 0;
                returnToWarp = !cargo.isEmpty() || retry || next >= actions.size() || actions.get(next).kind() == Kind.PLACE;
            }
            case RETRACT -> {
                var hadCargo = !cargo.isEmpty();
                if (!deposit()) {
                    elapsed = duration - 1;
                    status = "inventory_full";
                } else {
                    if (hadCargo) warpParticles(world);
                    motionStart = returnToWarp ? worldPosition : action.pos();
                    if (retry) {
                        retry = false;
                        phase = Phase.PREPARE;
                    } else advance();
                }
            }
            default -> {}
        }
        if (phase != Phase.PREPARE) duration = actionDuration(action, phase, returnToWarp);
    }

    private int actionDuration(Action action, Phase actionPhase, boolean returning) {

        var destroying = action.kind() == Kind.DESTROY;
        var ticks = switch (actionPhase) {
            case WORK -> destroying ? 20 : 2;
            case RETRACT -> returning ? 28 : 1;
            default -> destroying ? 33 : 28;
        };
        var hardnessMultiplier = 1f;
        if (actionPhase == Phase.WORK && destroying) {
            var hardness = level.getBlockState(action.pos()).getDestroySpeed(level, action.pos());
            hardnessMultiplier = (float) Math.sqrt(Math.max(1, hardness));
        }
        return Math.max(1, Math.round(ticks * getSpeedMultiplier() * hardnessMultiplier));
    }

    private boolean repeats() {

        return repeat && redstoneMode != REDSTONE_PULSE;
    }

    private void warpParticles(ServerLevel world) {

        var warp = RobotArmReach.WARP;
        var offset = new Vec3(warp.x() / 16, warp.y() / 16, warp.z() / 16).yRot(RobotArmReach.facingYaw(getFacing()));
        var position = Vec3.atLowerCornerOf(worldPosition).add(.5, 0, .5).add(offset);
        world.sendParticles(ParticleTypes.PORTAL, position.x, position.y, position.z, 12, .15, .15, .15, .03);
    }

    private void advance() {

        phase = Phase.PREPARE;
        elapsed = 0;
        cursor++;
        if (cursor >= program.actions().size()) {
            cursor = 0;
            if (!repeats()) {
                running = false;
                status = "finished";
            }
        }
        setChanged();
    }

    private void resetPlayback() {

        clearBreakingProgress();
        phase = Phase.PREPARE;
        elapsed = 0;
        motionStart = worldPosition;
        returnToWarp = true;
        retry = false;
        cursor = 0;
    }

    public boolean restart() {

        if (!deposit()) {
            status = "inventory_full";
            return false;
        }
        resetPlayback();
        running = !program.actions().isEmpty();
        setChanged();
        return true;
    }

    public boolean loadCard() {

        var card = inventory.getItem(0).get(SpaceAgeComponents.ROBOT_PROGRAM.get());
        if (card == null || card.binding().isEmpty() || !card.binding().get().pos().equals(worldPosition)
                || card.binding().get().dimension() != level.dimension()) {
            status = "wrong_binding";
            return false;
        }
        if (!deposit()) {
            status = "inventory_full";
            return false;
        }
        program = card;
        resetPlayback();
        running = false;
        status = "ready";
        setChanged();
        return true;
    }

    private String checkAction(Action action) {

        if (!inRange(action.pos()) || !level.hasChunkAt(action.pos())) return "out_of_range";
        var state = level.getBlockState(action.pos());
        if (action.kind() == Kind.PLACE) {
            if (!state.canBeReplaced()) return "occupied";
            if (!action.state().canSurvive(level, action.pos())) return "unsupported";
            return "";
        }
        if (state.isAir()) return "missing_block";
        if (!action.match().test(action.state(), state)) return "wrong_block";
        if (action.kind() == Kind.DESTROY && state.getDestroySpeed(level, action.pos()) < 0) return "unbreakable";
        if (action.kind() == Kind.INTERACT && state.getMenuProvider(level, action.pos()) != null) return "gui_block";
        return "";
    }

    private boolean reserve(ItemStack wanted, boolean exactComponents) {

        for (int slot = 1; slot < getInventorySize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!(exactComponents ? ItemStack.isSameItemSameComponents(stack, wanted) : ItemStack.isSameItem(stack, wanted))) continue;
            var reserved = stack.copyWithCount(1);
            try (var transaction = Transaction.openRoot()) {
                if (inventory.extract(slot, ItemResource.of(stack), 1, transaction) == 1) {
                    cargo.add(reserved);
                    transaction.commit();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean deposit() {

        try (var transaction = Transaction.openRoot()) {
            for (var stack : cargo) {
                var remaining = stack.getCount();
                for (int slot = 1; slot < getInventorySize() && remaining > 0; slot++)
                    remaining -= inventory.insert(slot, ItemResource.of(stack), remaining, transaction);
                if (remaining > 0) return false;
            }
            transaction.commit();
        }
        cargo.clear();
        return true;
    }

    /** Places the robot player so that tools raycasting from its eyes hit the recorded spot. */
    private FakePlayer positionRobot(Action action) {

        if (robot == null) {
            robot = new FakePlayer((ServerLevel) level, ROBOT_PROFILE) {
                @Override
                public boolean hasCorrectToolForDrops(BlockState state) {

                    // The built-in gripper harvests without a separate mining tool.
                    return true;
                }
            };
            robot.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        }
        robot.setYRot(action.yaw());
        robot.setYHeadRot(action.yaw());
        robot.yHeadRotO = action.yaw();
        robot.setXRot(action.pitch());
        var eye = Vec3.atLowerCornerOf(action.pos()).add(action.hit()).subtract(robot.getLookAngle().scale(2));
        robot.setPos(eye.x, eye.y - robot.getEyeHeight(), eye.z);
        robot.setOldPosAndRot();
        robot.setShiftKeyDown(action.sneaking());
        robot.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return robot;
    }

    private String execute(Action action) {

        var issue = checkAction(action);
        if (!issue.isEmpty()) return issue;
        var world = (ServerLevel) level;
        var player = positionRobot(action);
        var pos = action.pos();
        if (!world.mayInteract(player, pos)) return "protected";
        return switch (action.kind()) {
            case DESTROY -> destroy(world, player, pos);
            case PLACE -> place(world, player, action);
            case INTERACT -> interact(world, player, action);
        };
    }

    private String destroy(ServerLevel world, FakePlayer player, BlockPos pos) {

        // Player breaking calls multipart cleanup, protection events, and normal loot hooks.
        var bounds = new AABB(pos).inflate(1);
        var existing = new HashSet<UUID>();
        for (var drop : world.getEntitiesOfClass(ItemEntity.class, bounds)) existing.add(drop.getUUID());
        player.setItemInHand(InteractionHand.MAIN_HAND, harvestTool());
        var destroyed = player.gameMode.destroyBlock(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (!destroyed) return "protected";
        for (var drop : world.getEntitiesOfClass(ItemEntity.class, bounds)) {
            if (existing.contains(drop.getUUID())) continue;
            cargo.add(drop.getItem().copy());
            drop.discard();
        }
        return "";
    }

    /** Yield and silk touch addons work like on the destroyer: loot tables see an enchanted tool. */
    private ItemStack harvestTool() {

        if (!silkTouch && yieldAddons == 0) return ItemStack.EMPTY;
        var tool = new ItemStack(Items.NETHERITE_PICKAXE);
        tool.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (silkTouch) tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        else tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), yieldAddons);
        return tool;
    }

    private String place(ServerLevel world, FakePlayer player, Action action) {

        if (cargo.isEmpty() || !(cargo.getFirst().getItem() instanceof BlockItem)) return "missing_item";
        // Placing through the item keeps multi-block placements and lets the block choose its own state.
        var held = cargo.getFirst();
        // Click the block the player placed against. Clicking the empty target instead makes blocks such as
        // buttons and addons ignore the clicked face and orient themselves by the look direction.
        var support = action.pos().relative(action.face().getOpposite());
        var hit = world.getBlockState(support).canBeReplaced() ? hitResult(action)
                : new BlockHitResult(Vec3.atLowerCornerOf(action.pos()).add(action.hit()), action.face(), support, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var result = player.gameMode.useItemOn(player, world, held, InteractionHand.MAIN_HAND, hit);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (!result.consumesAction()) return "failed";
        cargo.removeIf(ItemStack::isEmpty);
        return "";
    }

    private String interact(ServerLevel world, FakePlayer player, Action action) {

        if (!action.item().isEmpty() && cargo.isEmpty()) return "missing_item";
        var held = cargo.isEmpty() ? ItemStack.EMPTY : cargo.getFirst();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.gameMode.useItemOn(player, world, held, InteractionHand.MAIN_HAND, hitResult(action));
        var remaining = player.getMainHandItem().copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        cargo.clear();
        if (!remaining.isEmpty()) cargo.add(remaining);
        // Bucket replacements and other byproducts may be put directly into the player's inventory.
        var playerInventory = player.getInventory();
        for (int slot = 0; slot < playerInventory.getContainerSize(); slot++) {
            var stack = playerInventory.getItem(slot);
            if (stack.isEmpty()) continue;
            cargo.add(stack.copy());
            playerInventory.setItem(slot, ItemStack.EMPTY);
        }
        // A recorded click is one attempt, even when the item or block does nothing.
        return "";
    }

    private static BlockHitResult hitResult(Action action) {

        return new BlockHitResult(Vec3.atLowerCornerOf(action.pos()).add(action.hit()), action.face(), action.pos(), false);
    }

    private void updateVisual(boolean active) {

        var actions = program.actions();
        var action = actions.isEmpty() ? null : actions.get(Math.min(cursor, actions.size() - 1));
        updateBreakingProgress(action, active);
        var held = cargo.isEmpty() ? ItemStack.EMPTY : cargo.getFirst();
        var next = new Visual(action == null ? worldPosition : action.pos(), action == null ? Kind.PLACE : action.kind(),
                phase, elapsed, duration, active, held.copy(), level.getGameTime(), motionStart, returnToWarp);
        if (next.sameMotion(visual)) return;
        visual = next;
        setChanged();
    }

    private void updateBreakingProgress(Action action, boolean active) {

        if (!active || phase != Phase.WORK || action == null || action.kind() != Kind.DESTROY) {
            clearBreakingProgress();
            return;
        }
        if (!action.pos().equals(breakingPos)) {
            clearBreakingProgress();
            positionRobot(action);
            breakingPos = action.pos();
        }
        var stage = Math.min(9, elapsed * 10 / duration);
        if (stage != breakingStage) {
            ((ServerLevel) level).destroyBlockProgress(robot.getId(), breakingPos, stage);
            breakingStage = stage;
        }
    }

    private void clearBreakingProgress() {

        if (breakingPos == null) return;
        if (level instanceof ServerLevel world) world.destroyBlockProgress(robot.getId(), breakingPos, -1);
        breakingPos = null;
        breakingStage = -1;
    }

    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {

        yieldAddons = 0;
        silkTouch = false;
        super.gatherAddonStats(addons);
    }

    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {

        super.getAdditionalStatFromAddon(addonBlock);
        var block = addonBlock.state().getBlock();
        if (block == BlockContent.MACHINE_YIELD_ADDON.get()) yieldAddons++;
        if (block == BlockContent.MACHINE_SILK_TOUCH_ADDON.get()) silkTouch = true;
        if (addonBlock.addonEntity() instanceof HeartOfTheMachineAddonEntity combi) {
            yieldAddons += combi.getYieldCount();
            silkTouch |= combi.hasSilk();
        }
        yieldAddons = Math.min(yieldAddons, 3);
    }

    @Override
    public void resetAddons() {

        super.resetAddons();
        yieldAddons = 0;
        silkTouch = false;
    }

    @Override
    public List<Tuple<Component, Component>> getExtraExtensionLabels() {

        if (silkTouch) return List.of(new Tuple<>(Component.translatable("enchantment.minecraft.silk_touch"),
                Component.translatable("tooltip.oritech.machine.addon_silk_touch")));
        if (yieldAddons > 0) return List.of(new Tuple<>(Component.translatable("title.oritech.machine.addon_fortune", yieldAddons),
                Component.translatable("tooltip.oritech.machine.addon_fortune")));
        return super.getExtraExtensionLabels();
    }

    @Override
    public void setRemoved() {

        clearBreakingProgress();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {

        clearBreakingProgress();
        super.onChunkUnloaded();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {

        super.preRemoveSideEffects(pos, state);
        dropCargo();
        for (var stack : inventory.getStacks()) Block.popResource(level, pos, stack);
        inventory.getStacks().clear();
    }

    public void dropCargo() {

        for (var stack : cargo) Block.popResource(level, worldPosition, stack);
        cargo.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        output.store("program", RobotProgram.CODEC, program);
        output.store("cargo", ItemStack.OPTIONAL_CODEC.listOf(), cargo);
        output.putInt("cursor", cursor);
        output.putInt("phase", phase.ordinal());
        output.store("motionStart", BlockPos.CODEC, motionStart);
        output.putBoolean("returnToWarp", returnToWarp);
        output.putInt("elapsed", elapsed);
        output.putInt("duration", duration);
        output.putBoolean("running", running);
        output.putBoolean("repeat", repeat);
        output.putBoolean("skipBlocked", skipBlocked);
        output.putInt("redstoneMode", redstoneMode);
        output.putBoolean("powered", powered);
        output.putBoolean("retry", retry);
        output.putInt("yield", yieldAddons);
        output.putBoolean("silkTouch", silkTouch);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        program = input.read("program", RobotProgram.CODEC).orElse(RobotProgram.EMPTY);
        cargo.clear();
        cargo.addAll(input.read("cargo", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
        cursor = input.getIntOr("cursor", 0);
        phase = Phase.values()[Math.clamp(input.getIntOr("phase", 0), 0, Phase.values().length - 1)];
        motionStart = input.read("motionStart", BlockPos.CODEC).orElse(worldPosition);
        returnToWarp = input.getBooleanOr("returnToWarp", true);
        elapsed = input.getIntOr("elapsed", 0);
        duration = input.getIntOr("duration", DEFAULT_PHASE_TICKS);
        running = input.getBooleanOr("running", false);
        repeat = input.getBooleanOr("repeat", false);
        skipBlocked = input.getBooleanOr("skipBlocked", false);
        redstoneMode = Math.clamp(input.getIntOr("redstoneMode", REDSTONE_IGNORED), 0, REDSTONE_MODES - 1);
        powered = input.getBooleanOr("powered", false);
        retry = input.getBooleanOr("retry", false);
        yieldAddons = input.getIntOr("yield", 0);
        silkTouch = input.getBooleanOr("silkTouch", false);
    }

    @Override
    public PlayState onAnimationUpdate(AnimationTest<MachineBlockEntity> state) {

        var controller = state.controller();
        controller.setTransitionTicks(0);
        state.setControllerSpeed(1);
        if (!isAssembled(getBlockState())) {
            controller.stopTriggeredAnimation();
            return state.setAndContinue(PACKAGED);
        }
        if (state.isCurrentAnimation(PACKAGED)) return state.setAndContinue(SETUP);
        if (state.isCurrentAnimation(SETUP) && !controller.hasAnimationFinished()) return PlayState.CONTINUE;
        return state.setAndContinue(IDLE);
    }

    @Override
    public void triggerSetupAnimation() {

        // Like the laser, deployment follows the synced assembled state on the client.
    }

    @Override
    public boolean isActivelyWorking() { return false; }

    @Override
    protected float getAnimationSpeed() { return 1; }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {

        return new RobotMenu(id, inventory, this);
    }

    @Override
    public long getDefaultCapacity() { return 100_000; }

    @Override
    public long getDefaultInsertRate() { return 2_000; }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() { return null; }

    @Override
    public int getInventorySize() { return 19; }

    @Override
    public ContainerSlotAssignment getSlotAssignments() { return new ContainerSlotAssignment(1, 18, 1, 18); }

    @Override
    public List<GuiSlot> getGuiSlots() {

        // The card slot sits beside the load button; storage lines up with the player inventory rows.
        var slots = new ArrayList<GuiSlot>();
        slots.add(new GuiSlot(0, 235, 31));
        for (int i = 0; i < 18; i++) slots.add(new GuiSlot(i + 1, 235 + i % 6 * 18, 137 + i / 6 * 18));
        return slots;
    }

    @Override
    public BarConfiguration getEnergyConfiguration() { return new BarConfiguration(8, 30, 18, 98); }

    @Override
    public boolean showProgress() { return false; }

    @Override
    public boolean inputOptionsEnabled() { return false; }

    @Override
    public MenuType<?> getScreenHandlerType() { return SpaceAgeMenus.ROBOT.get(); }

    @Override
    public int getTickUpdateInterval() { return 1; }

    @Override
    public List<Vec3i> getCorePositions() {

        return List.of(new Vec3i(1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(1, 0, 1),
                new Vec3i(0, 1, 0), new Vec3i(1, 1, 0), new Vec3i(0, 1, 1), new Vec3i(1, 1, 1));
    }

    @Override
    public List<Vec3i> getAddonSlots() {

        return List.of(new Vec3i(0, 0, -1), new Vec3i(1, 0, -1), new Vec3i(2, 0, 0), new Vec3i(2, 0, 1));
    }

    public record Visual(BlockPos target, Kind kind, Phase phase, int elapsed, int duration, boolean active,
                         ItemStack held, long tick, BlockPos start, boolean returnToWarp) {

        /** Ignores the timestamp, so an unchanged arm is not resent every tick. */
        boolean sameMotion(Visual other) {

            return target.equals(other.target) && kind == other.kind && phase == other.phase && elapsed == other.elapsed
                    && duration == other.duration && active == other.active && start.equals(other.start)
                    && returnToWarp == other.returnToWarp && ItemStack.matches(held, other.held);
        }
    }
}
