package rearth.oritech.spaceage.robot;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.GameType;
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
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.robot.RobotProgram.Action;
import rearth.oritech.spaceage.robot.RobotProgram.Kind;
import rearth.oritech.spaceage.robot.RobotProgram.StepSignal;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.Geometry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RobotArmBlockEntity extends MultiblockMachineEntity {

    public static final int RANGE = RobotArmReach.RADIUS;
    public static final int PHASE_TICKS = 20;
    public RobotProgram program = RobotProgram.EMPTY;
    public int cursor;
    public boolean running;
    public boolean repeat;
    public boolean skipBlocked;
    public int redstoneMode;
    public String status = "idle";
    public int pulseTicks;
    private boolean powered;
    private boolean stepArmed;
    private boolean retry;
    private final List<ItemStack> cargo = new ArrayList<>();
    private FakePlayer robot;
    private BlockPos breakingPos;
    private int breakingStage = -1;

    @SyncField({SyncType.INITIAL, SyncType.TICK})
    public Visual visual = new Visual(BlockPos.ZERO, 0, 0, 0, PHASE_TICKS, false, ItemStack.EMPTY, 0, BlockPos.ZERO, true);
    private BlockPos motionStart;
    private boolean returnToWarp = true;
    private int phase;
    private int elapsed;
    private int duration = PHASE_TICKS;

    public RobotArmBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.ROBOT_ARM.get(), pos, state, 80);
    }

    public boolean inRange(BlockPos pos) {

        return pos.distSqr(worldPosition.above()) <= RANGE * RANGE && !pos.equals(worldPosition)
                && !getCorePositions().stream().anyMatch(core -> worldPosition.offset(Geometry.rotatePosition(core, getFacing())).equals(pos));
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
        if (powered != signal || pulseTicks > 0) setChanged();
        powered = signal;
        if (pulseTicks > 0 && --pulseTicks == 0) world.updateNeighborsAt(pos, state.getBlock());
        if (redstoneMode == 2 && rising && !running && phase == 0) {
            cursor = 0;
            running = true;
        }
        var active = running && isAssembled(state) && !disabledViaRedstone && (redstoneMode != 1 || signal);
        if (!active || program.actions().isEmpty()) {
            updateVisual(false);
            return;
        }
        if (cursor >= program.actions().size()) cursor = 0;
        var action = program.actions().get(cursor);
        if (phase == 0) {
            if (redstoneMode == 3 && action.signal() == StepSignal.WAIT_FOR_PULSE && !stepArmed) {
                if (!rising) {
                    status = "waiting_pulse";
                    updateVisual(false);
                    return;
                }
                stepArmed = true;
            }
            var issue = checkAction(action);
            if (!issue.isEmpty()) {
                blocked(issue);
                updateVisual(false);
                return;
            }
            if ((action.kind() == Kind.PLACE || action.kind() == Kind.INTERACT && !action.item().isEmpty())
                    && !reserve(action.item(), action.kind() == Kind.PLACE)) {
                blocked("missing_item");
                updateVisual(false);
                return;
            }
            if (action.kind() == Kind.PLACE) warpParticles(world);
            duration = actionDuration(action, 1, true);
            phase = 1;
            elapsed = 0;
            if (redstoneMode == 3 && action.signal() == StepSignal.EMIT_PULSE) {
                pulseTicks = 4;
                world.updateNeighborsAt(pos, state.getBlock());
            }
        }
        try (var transaction = Transaction.openRoot()) {
            var cost = Math.max(1, (int) calculateEnergyUsage());
            if (energyStorage.internalExtract(cost, transaction) != cost) {
                status = "no_energy";
                updateVisual(false);
                return;
            }
            transaction.commit();
        }
        consumeBurstTicks();
        status = "working";
        if (++elapsed >= duration) {
            elapsed = 0;
            if (phase == 2) {
                var issue = execute(action);
                if (!issue.isEmpty()) {
                    // Keep the reserved item until it can return to storage, including on failure.
                    status = issue;
                    retry = !skipBlocked;
                    phase = 3;
                } else {
                    phase = 3;
                }
                var next = cursor + 1;
                if (next == program.actions().size() && repeat && redstoneMode != 2) next = 0;
                returnToWarp = !cargo.isEmpty() || retry || next >= program.actions().size()
                        || program.actions().get(next).kind() == Kind.PLACE;
            } else if (phase == 3) {
                var hadCargo = !cargo.isEmpty();
                if (!deposit()) {
                    elapsed = duration - 1;
                    status = "inventory_full";
                } else {
                    if (hadCargo) warpParticles(world);
                    motionStart = returnToWarp ? worldPosition : action.pos();
                    if (retry) {
                        phase = 0;
                        elapsed = 0;
                        retry = false;
                    } else advance();
                }
            } else {
                phase++;
            }
            if (phase != 0) duration = actionDuration(action, phase, returnToWarp);
        }
        updateVisual(running);
        setChanged();
    }

    private int actionDuration(Action action, int actionPhase, boolean returning) {

        var kind = action.kind();
        var ticks = switch (actionPhase) {
            case 2 -> kind == Kind.DESTROY ? 20 : 2;
            case 3 -> returning ? 28 : 1;
            default -> kind == Kind.DESTROY ? 33 : 28;
        };
        var hardnessMultiplier = 1f;
        if (actionPhase == 2 && kind == Kind.DESTROY) {
            var hardness = level.getBlockState(action.pos()).getDestroySpeed(level, action.pos());
            hardnessMultiplier = (float) Math.sqrt(Math.max(1, hardness));
        }
        return Math.max(1, Math.round(ticks * getSpeedMultiplier() * hardnessMultiplier));
    }

    private void warpParticles(ServerLevel world) {

        var offset = new Vec3(1, 23 / 16d, .5).yRot(switch (getFacing()) {
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) Math.PI / 2;
            case EAST -> (float) -Math.PI / 2;
            default -> 0;
        });
        var warp = Vec3.atLowerCornerOf(worldPosition).add(.5, 0, .5).add(offset);
        world.sendParticles(ParticleTypes.PORTAL, warp.x, warp.y, warp.z, 12, .15, .15, .15, .03);
    }

    private void blocked(String issue) {

        status = issue;
        if (skipBlocked) advance();
    }

    private void advance() {

        phase = 0;
        elapsed = 0;
        stepArmed = false;
        cursor++;
        if (cursor >= program.actions().size()) {
            cursor = 0;
            if (!repeat || redstoneMode == 2) {
                running = false;
                status = "finished";
            }
        }
        setChanged();
    }

    public boolean restart() {

        if (!deposit()) {
            status = "inventory_full";
            return false;
        }
        clearBreakingProgress();
        phase = 0;
        elapsed = 0;
        motionStart = worldPosition;
        returnToWarp = true;
        retry = false;
        cursor = 0;
        stepArmed = false;
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
        if (!deposit()) return false;
        program = card;
        clearBreakingProgress();
        phase = 0;
        elapsed = 0;
        motionStart = worldPosition;
        returnToWarp = true;
        retry = false;
        cursor = 0;
        stepArmed = false;
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
        } else {
            if (state.isAir()) return "missing_block";
            if (action.kind() == Kind.DESTROY && state.getDestroySpeed(level, action.pos()) < 0) return "unbreakable";
            if (action.kind() == Kind.INTERACT && state.getMenuProvider(level, action.pos()) != null) return "gui_block";
        }
        return "";
    }

    private boolean reserve(ItemStack wanted, boolean exactComponents) {

        for (int slot = 1; slot < getInventorySize(); slot++) {
            var stack = inventory.getItem(slot);
            if (exactComponents ? ItemStack.isSameItemSameComponents(stack, wanted) : ItemStack.isSameItem(stack, wanted)) {
                var reserved = stack.copyWithCount(1);
                try (var transaction = Transaction.openRoot()) {
                    if (inventory.extract(slot, ItemResource.of(stack), 1, transaction) == 1) {
                        cargo.add(reserved);
                        transaction.commit();
                        return true;
                    }
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

    private FakePlayer player(Action action) {

        if (robot == null) {
            robot = new FakePlayer((ServerLevel) level,
                    new GameProfile(UUID.nameUUIDFromBytes("oritech_robot_arm".getBytes(StandardCharsets.UTF_8)), "[Oritech Robot]")) {
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
        // Some block tools raycast again. Keep their view ray on the recorded hit.
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
        var player = player(action);
        var pos = action.pos();
        if (!world.mayInteract(player, pos)) return "protected";
        if (action.kind() == Kind.DESTROY) {
            // Player breaking calls multipart cleanup, protection events, and normal loot hooks.
            var bounds = new AABB(pos).inflate(1);
            var existing = world.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
            if (!player.gameMode.destroyBlock(pos)) return "protected";
            for (var drop : world.getEntitiesOfClass(ItemEntity.class, bounds)) {
                if (existing.contains(drop.getUUID())) continue;
                cargo.add(drop.getItem().copy());
                drop.discard();
            }
            return "";
        }
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(action.hit()), action.face(), pos, false);
        if (action.kind() == Kind.PLACE) {
            if (cargo.isEmpty() || !(cargo.getFirst().getItem() instanceof BlockItem item)) return "missing_item";
            // Preserve the recorded state (including facing) while letting BlockItem create multi-block placements.
            var held = cargo.getFirst();
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var result = player.gameMode.useItemOn(player, world, held, InteractionHand.MAIN_HAND, hit);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (!result.consumesAction() || !world.getBlockState(pos).is(action.state().getBlock())) return "failed";
            if (world.getBlockState(pos).is(action.state().getBlock())) world.setBlock(pos, action.state(), Block.UPDATE_ALL);
            cargo.removeIf(ItemStack::isEmpty);
            return "";
        }
        if (!action.item().isEmpty() && cargo.isEmpty()) return "missing_item";
        var held = cargo.isEmpty() ? ItemStack.EMPTY : cargo.getFirst();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var result = player.gameMode.useItemOn(player, world, held, InteractionHand.MAIN_HAND, hit);
        var remaining = player.getMainHandItem().copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        cargo.clear();
        if (!remaining.isEmpty()) cargo.add(remaining);
        // Bucket replacements and other byproducts may be put directly into the player's inventory.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            cargo.add(stack.copy());
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        return result.consumesAction() ? "" : "failed";
    }

    private void updateVisual(boolean active) {

        var action = program.actions().isEmpty() ? null : program.actions().get(Math.min(cursor, program.actions().size() - 1));
        updateBreakingProgress(action, active);
        var held = cargo.isEmpty() ? ItemStack.EMPTY : cargo.getFirst().copy();
        var target = action == null ? worldPosition : action.pos();
        var kind = action == null ? 0 : action.kind().ordinal();
        var start = motionStart == null ? worldPosition : motionStart;
        if (visual.target().equals(target) && visual.kind() == kind && visual.phase() == phase
                && visual.elapsed() == elapsed && visual.duration() == duration && visual.active() == active
                && visual.start().equals(start) && visual.returnToWarp() == returnToWarp
                && ItemStack.matches(visual.held(), held)) return;
        visual = new Visual(target, kind, phase, elapsed, duration, active, held, level.getGameTime(), start, returnToWarp);
        setChanged();
    }

    private void updateBreakingProgress(Action action, boolean active) {

        if (!active || phase != 2 || action == null || action.kind() != Kind.DESTROY) {
            clearBreakingProgress();
            return;
        }
        if (!action.pos().equals(breakingPos)) {
            clearBreakingProgress();
            player(action);
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
        output.putInt("phase", phase);
        output.store("motionStart", BlockPos.CODEC, motionStart == null ? worldPosition : motionStart);
        output.putBoolean("returnToWarp", returnToWarp);
        output.putInt("elapsed", elapsed);
        output.putInt("duration", duration);
        output.putBoolean("running", running);
        output.putBoolean("repeat", repeat);
        output.putBoolean("skipBlocked", skipBlocked);
        output.putInt("redstone", redstoneMode);
        output.putBoolean("powered", powered);
        output.putBoolean("stepArmed", stepArmed);
        output.putBoolean("retry", retry);
        output.putInt("pulse", pulseTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        program = input.read("program", RobotProgram.CODEC).orElse(RobotProgram.EMPTY);
        cargo.clear();
        cargo.addAll(input.read("cargo", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
        cursor = input.getIntOr("cursor", 0);
        phase = input.getIntOr("phase", 0);
        motionStart = input.read("motionStart", BlockPos.CODEC).orElse(worldPosition);
        returnToWarp = input.getBooleanOr("returnToWarp", true);
        elapsed = input.getIntOr("elapsed", 0);
        duration = input.getIntOr("duration", PHASE_TICKS);
        running = input.getBooleanOr("running", false);
        repeat = input.getBooleanOr("repeat", false);
        skipBlocked = input.getBooleanOr("skipBlocked", false);
        redstoneMode = input.getIntOr("redstone", 0);
        powered = input.getBooleanOr("powered", false);
        stepArmed = input.getBooleanOr("stepArmed", false);
        retry = input.getBooleanOr("retry", false);
        pulseTicks = input.getIntOr("pulse", 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        controllers.add(new AnimationController<>("machine", 0, test -> test.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
    }

    @Override
    public void triggerSetupAnimation() {}

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {

        return new RobotMenu(id, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {

        sendUpdate(SyncType.GUI_OPEN);
        buffer.writeBoolean(true);
        buffer.writeBlockPos(worldPosition);
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
    public List<GuiSlot> getGuiSlots() { return List.of(); }

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

    public record Visual(BlockPos target, int kind, int phase, int elapsed, int duration, boolean active, ItemStack held, long tick, BlockPos start, boolean returnToWarp) {}
}
