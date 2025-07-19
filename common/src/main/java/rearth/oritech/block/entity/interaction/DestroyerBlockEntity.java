package rearth.oritech.block.entity.interaction;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MultiblockFrameInteractionEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.FakeMachinePlayer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DestroyerBlockEntity extends MultiblockFrameInteractionEntity {

    @SyncField(SyncType.GUI_OPEN)
    public boolean hasCropFilterAddon;
    @SyncField(SyncType.GUI_OPEN)
    public boolean hasSilkTouchAddon;
    @SyncField(SyncType.GUI_OPEN)
    public int yieldAddons = 0;
    @SyncField({SyncType.GUI_OPEN, SyncType.SPARSE_TICK})
    public int range = 1;

    // non-persistent
    @SyncField
    public BlockPos quarryTarget = BlockPos.ORIGIN;
    
    public float targetHardness = 1f;
    private ServerPlayerEntity destroyerPlayerEntity = null;

    public DestroyerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        range = 1;
        yieldAddons = 0;
        hasSilkTouchAddon = false;
        super.gatherAddonStats(addons);
    }

    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.CROP_FILTER_ADDON))
            hasCropFilterAddon = true;

        if (addonBlock.state().getBlock().equals(BlockContent.QUARRY_ADDON))
            range *= 8;

        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_YIELD_ADDON))
            yieldAddons++;

        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_SILK_TOUCH_ADDON))
            hasSilkTouchAddon = true;


        super.getAdditionalStatFromAddon(addonBlock);
    }

    @Override
    public void resetAddons() {
        super.resetAddons();
        hasCropFilterAddon = false;
        hasSilkTouchAddon = false;
        range = 1;
        yieldAddons = 0;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putBoolean("cropAddon", hasCropFilterAddon);
        nbt.putBoolean("silkTouchAddon", hasSilkTouchAddon);
        nbt.putInt("range", range);
        nbt.putInt("yield", yieldAddons);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        hasCropFilterAddon = nbt.getBoolean("cropAddon");
        hasSilkTouchAddon = nbt.getBoolean("silkTouchAddon");
        range = nbt.getInt("range");
        yieldAddons = nbt.getInt("yield");
    }

    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {

        if (range > 1) {
            return hasQuarryTarget(toolPosition);
        }

        var targetPosition = toolPosition.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);

        // skip not grown crops
        if (hasCropFilterAddon && isImmatureCrop(targetState)) {
            return false;
        }

        return !targetState.getBlock().equals(Blocks.AIR);
    }

    private PlayerEntity getDestroyerPlayerEntity() {
        if (destroyerPlayerEntity == null && world instanceof ServerWorld serverWorld) {
            destroyerPlayerEntity = FakeMachinePlayer.create(serverWorld, new GameProfile(UUID.randomUUID(), "oritech_destroyer"), inventory);
        }

        return destroyerPlayerEntity;
    }

    private boolean hasQuarryTarget(BlockPos toolPosition) {
        return getQuarryDownwardState(toolPosition) != null;
    }

    public static boolean isImmatureCrop(BlockState targetState) {
        Block targetBlock = targetState.getBlock();
        return (targetBlock instanceof CropBlock cropBlock && !cropBlock.isMature(targetState))
                || (targetBlock instanceof NetherWartBlock && targetState.get(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE)
                || (targetBlock instanceof CocoaBlock && targetState.get(CocoaBlock.AGE) < CocoaBlock.MAX_AGE);
    }

    private Pair<BlockPos, BlockState> getQuarryDownwardState(BlockPos toolPosition) {
        for (int i = 1; i <= range; i++) {
            var checkPos = toolPosition.down(i);
            var targetState = world.getBlockState(checkPos);
            if (!targetState.isAir() && !targetState.getFluidState().isStill()) {  // pass through both air and liquid
                quarryTarget = checkPos;
                targetHardness = Math.clamp(targetState.getHardness(world, checkPos), 0, 100);
                return new Pair<>(checkPos, targetState);
            }
        }

        quarryTarget = BlockPos.ORIGIN;
        return null;
    }

    @Override
    public void finishBlockWork(BlockPos processed) {

        var targetPosition = processed.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);

        if (range > 1) {
            if (quarryTarget != BlockPos.ORIGIN) {
                targetPosition = quarryTarget;
                targetState = world.getBlockState(targetPosition);
            } else {
                var data = getQuarryDownwardState(processed);
                if (data == null) return;
                targetPosition = data.getLeft();
                targetState = data.getRight();
            }
        }

        // remove fluids
        if (targetState.getFluidState().isStill()) {
            world.setBlockState(targetPosition, Blocks.AIR.getDefaultState());
        }

        var targetHardness = targetState.getBlock().getHardness();
        if (targetHardness < 0) return;    // skip undestroyable blocks, such as bedrock

        // skip not grown crops
        if (range == 1 && hasCropFilterAddon && isImmatureCrop(targetState)) {
            return;
        }

        if (!targetState.getBlock().equals(Blocks.AIR)) {

            var targetEntity = world.getBlockEntity(targetPosition);
            List<ItemStack> dropped;
            if (hasSilkTouchAddon) {
                dropped = getSilkTouchDrops(targetState, (ServerWorld) world, targetPosition, targetEntity, getDestroyerPlayerEntity());
            } else if (yieldAddons > 0) {
                dropped = getLootDrops(targetState, (ServerWorld) world, targetPosition, targetEntity, yieldAddons, getDestroyerPlayerEntity());
            } else {
                dropped = Block.getDroppedStacks(targetState, (ServerWorld) world, targetPosition, targetEntity);
            }

            if (dropped.isEmpty()) {
                // If the block doesn't drop any loot, try to break it again with shears
                // Good for seagrass, cobwebs, vines, etc.
                dropped = Block.getDroppedStacks(targetState, (ServerWorld) world, targetPosition, targetEntity, null, new ItemStack(Items.SHEARS));
            }

            // only proceed if all stacks fit
            for (var stack : dropped) {
                if (this.inventory.insert(stack, true) != stack.getCount()) return;
            }

            for (var stack : dropped) {
                this.inventory.insert(stack, false);
            }

            targetState.getBlock().onBreak(world, targetPosition, targetState, getDestroyerPlayerEntity());
            world.playSound(null, targetPosition, targetState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1f, 1f);
            world.breakBlock(targetPosition, false);
            super.finishBlockWork(processed);
        }
    }

    public static List<ItemStack> getLootDrops(BlockState state, ServerWorld world, BlockPos pos, @Nullable BlockEntity blockEntity, int yieldAddons, @Nullable PlayerEntity entity) {

        var sampleTool = new ItemStack(Items.NETHERITE_PICKAXE);
        sampleTool.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        var fortuneEntry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.FORTUNE).get();
        sampleTool.addEnchantment(fortuneEntry, Math.min(yieldAddons, 3));

        var builder = new LootContextParameterSet.Builder(world).add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                .add(LootContextParameters.TOOL, sampleTool)
                .addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity);
        if (entity != null)
            builder.addOptional(LootContextParameters.THIS_ENTITY, entity);
        return state.getDroppedStacks(builder);
    }

    public static List<ItemStack> getSilkTouchDrops(BlockState state, ServerWorld world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable PlayerEntity entity) {
        var sampleTool = new ItemStack(Items.NETHERITE_PICKAXE);
        sampleTool.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
        var silkTouchEntry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.SILK_TOUCH).get();
        sampleTool.addEnchantment(silkTouchEntry, 1);

        var builder = new LootContextParameterSet.Builder(world).add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                .add(LootContextParameters.TOOL, sampleTool)
                .addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity);
        if (entity != null)
            builder.addOptional(LootContextParameters.THIS_ENTITY, entity);
        return state.getDroppedStacks(builder);
    }

    @Override
    protected void doProgress(boolean moving) {
        super.doProgress(moving);

        if (moving)
            return;

        if (range > 1 && quarryTarget != BlockPos.ORIGIN) {
            ParticleContent.QUARRY_DESTROY_EFFECT.spawn(world, Vec3d.ofCenter(quarryTarget).add(0, 0.5, 0), 3);
        } else if (hasWorkAvailable(getCurrentTarget())) {
            ParticleContent.BLOCK_DESTROY_EFFECT.spawn(world, Vec3d.of(getCurrentTarget().down()), 4);
        }
    }

    @Override
    public List<Pair<Text, Text>> getExtraExtensionLabels() {
        if (range == 1 && yieldAddons == 0 && !hasSilkTouchAddon) return super.getExtraExtensionLabels();
        if (hasSilkTouchAddon) {
            return List.of(new Pair<>(
              Text.translatable("title.oritech.machine.addon_range", range),
              Text.translatable("tooltip.oritech.block_destroyer.addon_range")),
              new Pair<>(Text.translatable("enchantment.minecraft.silk_touch"),
                Text.translatable("tooltip.oritech.machine.addon_silk_touch")));
        }
        return List.of(new Pair<>(Text.translatable("title.oritech.machine.addon_range", range), Text.translatable("tooltip.oritech.block_destroyer.addon_range")), new Pair<>(Text.translatable("title.oritech.machine.addon_fortune", yieldAddons), Text.translatable("tooltip.oritech.machine.addon_fortune")));
    }

    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_DESTROYER_HEAD.getDefaultState();
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 117, 20, true),
                new GuiSlot(1, 117, 38, true),
                new GuiSlot(2, 135, 20, true),
                new GuiSlot(3, 135, 38, true));
    }

    @Override
    public int getInventorySize() {
        return 4;
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
                new Vec3i(0, 0, -2),
                new Vec3i(-1, 0, -1),
                new Vec3i(0, 0, 2),
                new Vec3i(-1, 0, 1)
        );
    }

    @Override
    public float getMoveTime() {
        var quarrySpeedBonus = range > 1 ? 0.15f : 1f;
        return Oritech.CONFIG.destroyerConfig.moveDuration() * this.getSpeedMultiplier() * quarrySpeedBonus;
    }

    @Override
    public float getWorkTime() {
        var quarrySpeedBonus = range > 1 ? 0.15f : 1f;
        return (float) (Oritech.CONFIG.destroyerConfig.workDuration() * this.getSpeedMultiplier() * Math.pow(targetHardness, 0.5f) * quarrySpeedBonus);
    }

    @Override
    public int getMoveEnergyUsage() {
        return Oritech.CONFIG.destroyerConfig.moveEnergyUsage();
    }

    @Override
    public int getOperationEnergyUsage() {
        var quarryCostBonus = range > 1 ? 4 : 1;
        return Oritech.CONFIG.destroyerConfig.workEnergyUsage() * quarryCostBonus;
    }

    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.DESTROYER_SCREEN;
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 0, -1),
                new Vec3i(0, 0, 1)
        );
    }
}
