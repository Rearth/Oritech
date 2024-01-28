package rearth.oritech.block.base;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.custom.MachineCoreBlock;
import rearth.oritech.client.init.ParticleContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class MultiblockMachineEntity extends MachineBlockEntity {
    
    public MultiblockMachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    
    // positive x = forward
    // positive y = up
    // positive z = right?
    public abstract List<Vec3i> getCorePositions();
    
    public boolean initMultiblock(BlockState state) {
        
        // check if multiblock is already created, if so cancel
        // call method the get a list of relative positions
        // check all positions if the blocks there extend MachineCoreBlock
        // if so, add them to list of used blocks
        // if not (e.g. block wrong type or air), draw a small particle to indicate the missing position
        // when all blocks are valid, multiblock is active
        // update all multiblocks state to USED=true, write controller position to block state
        
        if (state.get(MultiblockMachine.ASSEMBLED)) return true;
        
        var ownFacing = getFacing();
        
        var targetPositions = getCorePositions();
        var coreBlocks = new ArrayList<MultiBlockElement>(targetPositions.size());
        
        for (var targetPosition : targetPositions) {
            var rotatedPos = rotatePosition(targetPosition, ownFacing);
            var checkPos = pos.add(rotatedPos);
            var checkState = Objects.requireNonNull(world).getBlockState(checkPos);
            
            var blockType = checkState.getBlock();
            if (blockType instanceof MachineCoreBlock coreBlock) {
                coreBlocks.add(new MultiBlockElement(checkState, coreBlock, checkPos));
            } else {
                highlightBlock(checkPos);
            }
        }
        
        if (targetPositions.size() == coreBlocks.size()) {
            // valid
            for (var core : coreBlocks) {
                var offset = pos.subtract(core.pos);
                var newState = core.state
                                 .with(MachineCoreBlock.USED, true)
                                 .with(MachineCoreBlock.CONTROLLER_X, offset.getX() + 4)
                                 .with(MachineCoreBlock.CONTROLLER_Y, offset.getY() + 4)
                                 .with(MachineCoreBlock.CONTROLLER_Z, offset.getZ() + 4);
                world.setBlockState(core.pos, newState);
            }
            
            Objects.requireNonNull(world).setBlockState(pos, state.with(MultiblockMachine.ASSEMBLED, true));
            System.out.println("multiblock valid");
            return true;
        } else {
            // invalid
            System.out.println("multiblock invalid");
            return false;
        }
        
    }
    
    public void onCoreBroken(BlockPos corePos, BlockState coreState) {
        
        System.out.println("registering broken core!");
        
        // set assembled to false
        // go through all existing cores
        // set used to false
        
    }
    
    private void highlightBlock(BlockPos block) {
        ParticleContent.HIGHLIGHT_BLOCK.spawn(world, Vec3d.of(block), null);
    }
    
    private Vec3i rotatePosition(Vec3i relativePos, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(relativePos.getZ(), relativePos.getY(), relativePos.getX());
            case EAST -> new BlockPos(-relativePos.getX(), relativePos.getY(), -relativePos.getZ());
            case SOUTH -> new BlockPos(-relativePos.getZ(), relativePos.getY(), -relativePos.getX());
            case WEST -> new BlockPos(relativePos.getX(), relativePos.getY(), relativePos.getZ());
            default -> relativePos;
        };
    }
    
    private record MultiBlockElement(BlockState state, MachineCoreBlock coreBlock, BlockPos pos) {
    }
}
