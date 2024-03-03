package rearth.oritech.block.entity.machines.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ScreenProvider;

import java.util.List;
import java.util.Objects;

public class PlacerBlockEntity extends ItemEnergyFrameInteractionBlockEntity {
    public PlacerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PLACER_BLOCK_ENTITY, pos, state);
    }
    
    
    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {
        
        var firstBlock = getFirstInInventory();
        if (firstBlock == null) return false;
        var block = Block.getBlockFromItem(firstBlock.getItem());
        if (block == null) return false;
        
        var targetPosition = toolPosition.down();
        return Objects.requireNonNull(world).getBlockState(targetPosition).getBlock().equals(Blocks.AIR) && block.getDefaultState().canPlaceAt(world, targetPosition);
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var firstBlock = getFirstInInventory();
        if (firstBlock == null) return;
        var block = Block.getBlockFromItem(firstBlock.getItem());
        if (block == null) return;
        
        var targetPosition = processed.down();
        if (Objects.requireNonNull(world).getBlockState(targetPosition).getBlock().equals(Blocks.AIR) && block.getDefaultState().canPlaceAt(world, targetPosition)) {
            world.setBlockState(targetPosition, block.getDefaultState());
            firstBlock.decrement(1);
            world.playSound(null, targetPosition, block.getDefaultState().getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1f, 1f);
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
    public BlockState getMachineHead() {
        return BlockContent.MACHINE_SPEED_ADDON.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR);
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, -1,0)
        );
    }
    
    @Override
    public int getMoveTime() {
        return 6;
    }
    
    @Override
    public int getWorkTime() {
        return 1;
    }
    
    
    @Override
    public int getMoveEnergyUsage() {
        return 10;
    }
    
    @Override
    public int getOperationEnergyUsage() {
        return 10;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.PLACER_SCREEN;
    }
}
