package rearth.oritech.block.blocks.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;

import java.util.HashSet;

public abstract class GenericPipeConnectionBlock extends GenericPipeBlock implements EntityBlock {
    
    public GenericPipeConnectionBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.getBlock().equals(state.getBlock())) return;
        GenericPipeInterfaceEntity.addNode(level, pos, true, state, getNetworkData(level));
        
        var regKey = level.dimension().location();
        var dataId = getPipeTypeName() + "_" + regKey.getNamespace() + "_" + regKey.getPath();
        Oritech.LOGGER.debug("saving for: " + dataId);
        ((ServerLevel) level).getDataStorage().set(dataId, getNetworkData(level));
    }
    
    @Override
    protected void onBlockRemoved(BlockPos pos, BlockState oldState, Level level) {
        updateNeighbors(level, pos, false);
        GenericPipeInterfaceEntity.removeNode(level, pos, true, oldState, getNetworkData(level));
    }
    
    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        
        if (!(level instanceof ServerLevel serverLevel)) return state;
        
        if (state.getValue(BlockStateProperties.WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        
        if (!hasNeighboringMachine(state, serverLevel, pos, false)) {
            // remove stale machine -> neighboring pipes mapping
            GenericPipeInterfaceEntity.removeStaleMachinePipeNeighbors(pos, getNetworkData(serverLevel));
            
            var normalState = getNormalBlock();
            return ((GenericPipeBlock) normalState.getBlock()).addConnectionStates(normalState, serverLevel, pos, false);
        }
        
        var interfaceState = state;
        if (!(neighborState.getBlock() instanceof AbstractPipeBlock)) {
            // only update connection if neighbor is a new machine
            var hasMachine = getNetworkData(serverLevel).machinePipeNeighbors.getOrDefault(neighborPos, HashSet.newHashSet(0)).contains(direction.getOpposite());
            if (neighborState.is(Blocks.AIR) || !hasMachine) {
                interfaceState = addConnectionStates(state, serverLevel, pos, direction);
            }
            
            if (!interfaceState.equals(state)) {
                // reload connection when state has changed (e.g. machine added/removed)
                GenericPipeInterfaceEntity.addNode(serverLevel, pos, true, interfaceState, getNetworkData(serverLevel));
            }
        }
        
        return interfaceState;
    }
    
    @Override
    protected boolean toggleSideConnection(BlockState state, Direction side, Level level, BlockPos pos) {
        var property = directionToProperty(side);
        var createConnection = state.getValue(property) == NO_CONNECTION;
        
        // check if connection would be valid if state is toggled
        var targetPos = pos.relative(side);
        if (createConnection && !isValidConnectionTarget(level.getBlockState(targetPos).getBlock(), level, side.getOpposite(), targetPos))
            return false;
        
        // toggle connection state
        int nextConnectionState = getNextConnectionState(state, side, level, pos, state.getValue(property));
        var newState = addStraightState(state.setValue(property, nextConnectionState));
        
        // transform to interface block if side is being enabled and machine is connected
        if (!hasNeighboringMachine(newState, level, pos, false)) {
            var normalBlock = (GenericPipeBlock) getNormalBlock().getBlock();
            var interfaceState = normalBlock.addConnectionStates(normalBlock.defaultBlockState(), level, pos, false);
            interfaceState = interfaceState.setValue(normalBlock.directionToProperty(side), newState.getValue(property)); // Hacky way to copy connection state
            level.setBlockAndUpdate(pos, normalBlock.addStraightState(interfaceState));
        } else {
            level.setBlockAndUpdate(pos, newState);
            GenericPipeInterfaceEntity.addNode(level, pos, true, newState, getNetworkData(level));
            
            // update neighbor if it's a pipe
            updateNeighbors(level, pos, true);
        }
        
        // play sound
        var soundGroup = getSoundType(state);
        level.playSound(null, pos, soundGroup.getPlaceSound(), SoundSource.BLOCKS, soundGroup.getVolume() * .5f, soundGroup.getPitch());
        
        return true;
    }
    
    @SuppressWarnings("rawtypes")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(getNormalBlock().getBlock());
    }
}
