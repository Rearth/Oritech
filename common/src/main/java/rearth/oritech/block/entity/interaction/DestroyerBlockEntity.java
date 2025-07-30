package rearth.oritech.block.entity.interaction;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
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
    public BlockPos quarryTarget = BlockPos.ZERO;
    
    public float targetHardness = 1f;
    private ServerPlayer destroyerPlayerEntity = null;

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
        
        yieldAddons = Math.min(yieldAddons, 3);
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
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putBoolean("cropAddon", hasCropFilterAddon);
        nbt.putBoolean("silkTouchAddon", hasSilkTouchAddon);
        nbt.putInt("range", range);
        nbt.putInt("yield", yieldAddons);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
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

        var targetPosition = toolPosition.below();
        var targetState = Objects.requireNonNull(level).getBlockState(targetPosition);

        // skip not grown crops
        if (hasCropFilterAddon && isImmatureCrop(targetState)) {
            return false;
        }

        return !targetState.getBlock().equals(Blocks.AIR);
    }

    private Player getDestroyerPlayerEntity() {
        if (destroyerPlayerEntity == null && level instanceof ServerLevel serverWorld) {
            destroyerPlayerEntity = FakeMachinePlayer.create(serverWorld, new GameProfile(UUID.randomUUID(), "oritech_destroyer"), inventory);
        }

        return destroyerPlayerEntity;
    }

    private boolean hasQuarryTarget(BlockPos toolPosition) {
        return getQuarryDownwardState(toolPosition) != null;
    }

    public static boolean isImmatureCrop(BlockState targetState) {
        Block targetBlock = targetState.getBlock();
        return (targetBlock instanceof CropBlock cropBlock && !cropBlock.isMaxAge(targetState))
                || (targetBlock instanceof NetherWartBlock && targetState.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE)
                || (targetBlock instanceof CocoaBlock && targetState.getValue(CocoaBlock.AGE) < CocoaBlock.MAX_AGE);
    }

    private Tuple<BlockPos, BlockState> getQuarryDownwardState(BlockPos toolPosition) {
        for (int i = 1; i <= range; i++) {
            var checkPos = toolPosition.below(i);
            var targetState = level.getBlockState(checkPos);
            if (!targetState.isAir() && !targetState.getFluidState().isSource()) {  // pass through both air and liquid
                quarryTarget = checkPos;
                targetHardness = Math.clamp(targetState.getDestroySpeed(level, checkPos), 0, 100);
                return new Tuple<>(checkPos, targetState);
            }
        }

        quarryTarget = BlockPos.ZERO;
        return null;
    }

    @Override
    public void finishBlockWork(BlockPos processed) {

        var targetPosition = processed.below();
        var targetState = Objects.requireNonNull(level).getBlockState(targetPosition);

        if (range > 1) {
            if (quarryTarget != BlockPos.ZERO) {
                targetPosition = quarryTarget;
                targetState = level.getBlockState(targetPosition);
            } else {
                var data = getQuarryDownwardState(processed);
                if (data == null) return;
                targetPosition = data.getA();
                targetState = data.getB();
            }
        }

        // remove fluids
        if (targetState.getFluidState().isSource()) {
            level.setBlockAndUpdate(targetPosition, Blocks.AIR.defaultBlockState());
        }

        var targetHardness = targetState.getBlock().defaultDestroyTime();
        if (targetHardness < 0) return;    // skip undestroyable blocks, such as bedrock

        // skip not grown crops
        if (range == 1 && hasCropFilterAddon && isImmatureCrop(targetState)) {
            return;
        }

        if (!targetState.getBlock().equals(Blocks.AIR)) {

            var targetEntity = level.getBlockEntity(targetPosition);
            List<ItemStack> dropped;
            if (hasSilkTouchAddon) {
                dropped = getSilkTouchDrops(targetState, (ServerLevel) level, targetPosition, targetEntity, getDestroyerPlayerEntity());
            } else if (yieldAddons > 0) {
                dropped = getLootDrops(targetState, (ServerLevel) level, targetPosition, targetEntity, yieldAddons, getDestroyerPlayerEntity());
            } else {
                dropped = Block.getDrops(targetState, (ServerLevel) level, targetPosition, targetEntity);
            }

            if (dropped.isEmpty()) {
                // If the block doesn't drop any loot, try to break it again with shears
                // Good for seagrass, cobwebs, vines, etc.
                dropped = Block.getDrops(targetState, (ServerLevel) level, targetPosition, targetEntity, null, new ItemStack(Items.SHEARS));
            }

            // only proceed if all stacks fit
            for (var stack : dropped) {
                if (this.inventory.insert(stack, true) != stack.getCount()) return;
            }

            for (var stack : dropped) {
                this.inventory.insert(stack, false);
            }

            targetState.getBlock().playerWillDestroy(level, targetPosition, targetState, getDestroyerPlayerEntity());
            level.playSound(null, targetPosition, targetState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
            level.destroyBlock(targetPosition, false);
            super.finishBlockWork(processed);
        }
    }

    public static List<ItemStack> getLootDrops(BlockState state, ServerLevel world, BlockPos pos, @Nullable BlockEntity blockEntity, int yieldAddons, @Nullable Player entity) {

        var sampleTool = new ItemStack(Items.NETHERITE_PICKAXE);
        sampleTool.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        var fortuneEntry = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.FORTUNE).get();
        sampleTool.enchant(fortuneEntry, Math.min(yieldAddons, 3));

        var builder = new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, sampleTool)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        if (entity != null)
            builder.withOptionalParameter(LootContextParams.THIS_ENTITY, entity);
        return state.getDrops(builder);
    }

    public static List<ItemStack> getSilkTouchDrops(BlockState state, ServerLevel world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Player entity) {
        var sampleTool = new ItemStack(Items.NETHERITE_PICKAXE);
        sampleTool.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        var silkTouchEntry = world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.SILK_TOUCH).get();
        sampleTool.enchant(silkTouchEntry, 1);

        var builder = new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, sampleTool)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        if (entity != null)
            builder.withOptionalParameter(LootContextParams.THIS_ENTITY, entity);
        return state.getDrops(builder);
    }

    @Override
    protected void doProgress(boolean moving) {
        super.doProgress(moving);

        if (moving)
            return;

        if (range > 1 && quarryTarget != BlockPos.ZERO) {
            ParticleContent.QUARRY_DESTROY_EFFECT.spawn(level, Vec3.atCenterOf(quarryTarget).add(0, 0.5, 0), 3);
        } else if (hasWorkAvailable(getCurrentTarget())) {
            ParticleContent.BLOCK_DESTROY_EFFECT.spawn(level, Vec3.atLowerCornerOf(getCurrentTarget().below()), 4);
        }
    }

    @Override
    public List<Tuple<Component, Component>> getExtraExtensionLabels() {
        if (range == 1 && yieldAddons == 0 && !hasSilkTouchAddon) return super.getExtraExtensionLabels();
        if (hasSilkTouchAddon) {
            return List.of(new Tuple<>(
              Component.translatable("title.oritech.machine.addon_range", range),
              Component.translatable("tooltip.oritech.block_destroyer.addon_range")),
              new Tuple<>(Component.translatable("enchantment.minecraft.silk_touch"),
                Component.translatable("tooltip.oritech.machine.addon_silk_touch")));
        }
        return List.of(new Tuple<>(Component.translatable("title.oritech.machine.addon_range", range), Component.translatable("tooltip.oritech.block_destroyer.addon_range")), new Tuple<>(Component.translatable("title.oritech.machine.addon_fortune", yieldAddons), Component.translatable("tooltip.oritech.machine.addon_fortune")));
    }

    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_DESTROYER_HEAD.defaultBlockState();
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
    public MenuType<?> getScreenHandlerType() {
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
