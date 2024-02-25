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
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.client.init.ParticleContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class MultiblockMachineEntity extends UpgradableMachineBlockEntity {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    private float coreQuality = 1f;
    
    public MultiblockMachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    public static Vec3i rotatePosition(Vec3i relativePos, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(relativePos.getZ(), relativePos.getY(), relativePos.getX());
            case WEST -> new BlockPos(relativePos.getX(), relativePos.getY(), -relativePos.getZ());
            case SOUTH -> new BlockPos(-relativePos.getZ(), relativePos.getY(), -relativePos.getX());
            case EAST -> new BlockPos(-relativePos.getX(), relativePos.getY(), relativePos.getZ());
            default -> relativePos;
        };
    }
    
    // this seems to work as expected for some reason?
    public static Vec3i worldToRelativePos(Vec3i ownWorldPos, Vec3i worldPos, Direction ownFacing) {
        var relativePos = worldPos.subtract(ownWorldPos);
        return relativePos;
//        var facingInverted = ownFacing.getOpposite();
//        return rotatePosition(relativePos, facingInverted);
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
        nbt.putFloat("coreQuality", coreQuality);
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
        
        coreQuality = nbt.getFloat("coreQuality");
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
        
        var sumCoreQuality = 0f;
        
        for (var targetPosition : targetPositions) {
            var rotatedPos = rotatePosition(targetPosition, ownFacing);
            var checkPos = pos.add(rotatedPos);
            var checkState = Objects.requireNonNull(world).getBlockState(checkPos);
            
            var blockType = checkState.getBlock();
            if (blockType instanceof MachineCoreBlock coreBlock && !checkState.get(MachineCoreBlock.USED)) {
                coreBlocks.add(new MultiBlockElement(checkState, coreBlock, checkPos));
                sumCoreQuality += coreBlock.getCoreQuality();
            } else {
                highlightBlock(checkPos);
            }
        }
        
        if (targetPositions.size() == coreBlocks.size()) {
            // valid
            for (var core : coreBlocks) {
                var newState = core.state.with(MachineCoreBlock.USED, true);
                var coreEntity = (MachineCoreEntity) world.getBlockEntity(core.pos());
                coreEntity.setControllerPos(pos);
                world.setBlockState(core.pos, newState);
                coreBlocksConnected.add(core.pos);
            }
            
            this.coreQuality = sumCoreQuality / coreBlocks.size();
            
            Objects.requireNonNull(world).setBlockState(pos, state.with(MultiblockMachine.ASSEMBLED, true));
            return true;
        } else {
            // invalid
            return false;
        }
        
    }
    
    @Override
    public float getCoreQuality() {
        return this.coreQuality;
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
    
    @Override
    public boolean isActive(BlockState state) {
        return state.get(MultiblockMachine.ASSEMBLED);
    }
    
    private record MultiBlockElement(BlockState state, MachineCoreBlock coreBlock, BlockPos pos) {
    }
}
