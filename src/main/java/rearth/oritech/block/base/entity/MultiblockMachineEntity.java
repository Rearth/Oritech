package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.custom.MachineCoreBlock;
import rearth.oritech.client.init.ParticleContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class MultiblockMachineEntity extends MachineBlockEntity {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    public MultiblockMachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        
        var posList = new NbtList();
        for (var pos : coreBlocksConnected) {
            var posTag = new NbtCompound();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            posList.add(posTag);
        }
        nbt.put("connectedCores", posList);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        
        var posList = nbt.getList("connectedCores", NbtElement.COMPOUND_TYPE);
        
        for (var posTag : posList) {
            var posCompound = (NbtCompound) posTag;
            var x = posCompound.getInt("x");
            var y = posCompound.getInt("y");
            var z = posCompound.getInt("z");
            var pos = new BlockPos(x, y, z);
            coreBlocksConnected.add(pos);
        }
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
            if (blockType instanceof MachineCoreBlock coreBlock && !checkState.get(MachineCoreBlock.USED)) {
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
                coreBlocksConnected.add(core.pos);
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
        
        Objects.requireNonNull(world).setBlockState(pos, world.getBlockState(pos).with(MultiblockMachine.ASSEMBLED, false));
        
        for (var core : coreBlocksConnected) {
            if (core.equals(corePos)) continue;
            
            var state = world.getBlockState(core);
            if (state.getBlock() instanceof MachineCoreBlock) {
                world.setBlockState(core, state.with(MachineCoreBlock.USED, false));
            }
        }
        
        coreBlocksConnected.clear();
    }
    
    public void onControllerBroken(BlockState controllerState) {
        
        for (var core : coreBlocksConnected) {
            var state = Objects.requireNonNull(world).getBlockState(core);
            if (state.getBlock() instanceof MachineCoreBlock) {
                world.setBlockState(core, state.with(MachineCoreBlock.USED, false));
            }
        }
        
        coreBlocksConnected.clear();
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
    
    @Override
    public boolean isActive(BlockState state) {
        return state.get(MultiblockMachine.ASSEMBLED);
    }
    
    private record MultiBlockElement(BlockState state, MachineCoreBlock coreBlock, BlockPos pos) {
    }
}
