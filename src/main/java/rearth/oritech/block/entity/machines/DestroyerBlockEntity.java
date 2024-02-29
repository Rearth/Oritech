package rearth.oritech.block.entity.machines;

import net.minecraft.block.*;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.Objects;

public class DestroyerBlockEntity extends ItemEnergyFrameInteractionBlockEntity {
    public DestroyerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {
        
        var targetPosition = toolPosition.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        // skip not grown crops
        if (targetState.getBlock() instanceof CropBlock cropBlock && !cropBlock.isMature(targetState)) {
            return false;
        }
        
        return !targetState.getBlock().equals(Blocks.AIR);
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var targetPosition = processed.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        // skip not grown crops
        if (targetState.getBlock() instanceof CropBlock cropBlock && !cropBlock.isMature(targetState)) {
            return;
        }
        
        if (!targetState.getBlock().equals(Blocks.AIR)) {
            
            var targetEntity = world.getBlockEntity(targetPosition);
            var dropped = Block.getDroppedStacks(targetState, (ServerWorld) world, targetPosition, targetEntity);
            
            // only proceed if all stacks fit
            for (var stack : dropped) {
                if (!this.inventory.canInsert(stack)) return;
            }
            
            for (var stack : dropped) {
                this.inventory.addStack(stack);
            }
            
            world.addBlockBreakParticles(targetPosition, world.getBlockState(targetPosition));
            world.breakBlock(targetPosition, false);
            super.finishBlockWork(processed);
        }
    }
    
    @Override
    public BlockState getMachineHead() {
        return BlockContent.MACHINE_INVENTORY_PROXY_ADDON.getDefaultState().with(WallMountedBlock.FACE, BlockFace.FLOOR);
    }
    
    @Override
    public int getMoveTime() {
        return 4;
    }
    
    @Override
    public int getWorkTime() {
        return 10;
    }
    
    @Override
    public int getMoveEnergyUsage() {
        return 5;
    }
    
    @Override
    public int getOperationEnergyUsage() {
        return 100;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.DESTROYER_SCREEN;
    }
}
