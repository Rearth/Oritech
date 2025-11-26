package rearth.oritech.block.entity.interaction;

import rearth.oritech.Oritech;
import rearth.oritech.api.networking.AdditionalNetworkingProvider;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PlacerBlockEntity extends ItemEnergyFrameInteractionBlockEntity implements AdditionalNetworkingProvider {
    
    public PlacerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PLACER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {
        
        var firstBlock = getFirstInInventory();
        if (firstBlock == null) return false;
        var block = Block.byItem(firstBlock.getItem());
        if (block == null) return false;
        
        var targetPosition = toolPosition.below();
        return Objects.requireNonNull(level).getBlockState(targetPosition).getBlock().equals(Blocks.AIR) && block.defaultBlockState().canSurvive(level, targetPosition);
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var firstBlock = getFirstInInventory();
        if (firstBlock == null) return;
        var block = Block.byItem(firstBlock.getItem());
        if (block == null) return;
        
        var targetPosition = processed.below();
        if (Objects.requireNonNull(level).getBlockState(targetPosition).getBlock().equals(Blocks.AIR) && block.defaultBlockState().canSurvive(level, targetPosition)) {
            level.setBlockAndUpdate(targetPosition, block.defaultBlockState());
            firstBlock.shrink(1);
            level.playSound(null, targetPosition, block.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 1f);
            super.finishBlockWork(processed);
        }
    }
    
    private ItemStack getFirstInInventory() {
        for (var stack : inventory.heldStacks) {
            if (stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return stack;
            }
        }
        
        return null;
    }
    
    @Override
    public float getCoreQuality() {
        return 3f;
    }
    
    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_PLACER_HEAD.defaultBlockState();
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, -1, 0)
        );
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 38));
    }
    
    @Override
    public ItemStack getToolheadAdditionalRender() {
        return getFirstInInventory();
    }
    
    @Override
    public float getMoveTime() {
        return Oritech.CONFIG.placerConfig.moveDuration();
    }
    
    @Override
    public float getWorkTime() {
        return Oritech.CONFIG.placerConfig.workDuration();
    }
    
    
    @Override
    public int getMoveEnergyUsage() {
        return Oritech.CONFIG.placerConfig.moveEnergyUsage();
    }
    
    @Override
    public int getOperationEnergyUsage() {
        return Oritech.CONFIG.placerConfig.workEnergyUsage();
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.PLACER_SCREEN;
    }
    
    @Override
    public List<Field> additionalSyncedFields(SyncType type) {
        if (type.equals(SyncType.TICK)) {
            try {
                return List.of(ItemEnergyFrameInteractionBlockEntity.class.getDeclaredField("inventory"));
            } catch (NoSuchFieldException e) {
                Oritech.LOGGER.error("unable to register inventory as extra synced field for placed block.");
            }
        }
        return List.of();
    }
}
