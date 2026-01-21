package rearth.oritech.block.blocks.pipes;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import java.util.HashSet;
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

public abstract class GenericPipeConnectionBlock extends GenericPipeBlock implements EntityBlock {
    
    public GenericPipeConnectionBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.getBlock().equals(state.getBlock())) return;
        GenericPipeInterfaceEntity.addNode(world, pos, true, state, getNetworkData(world));
        
        var regKey = world.dimension().location();
        var dataId = getPipeTypeName() + "_" + regKey.getNamespace() + "_" + regKey.getPath();
        Oritech.LOGGER.debug("saving for: " + dataId);
        ((ServerLevel) world).getDataStorage().set(dataId, getNetworkData(world));
    }
    
    @Override
    protected void onBlockRemoved(BlockPos pos, BlockState oldState, Level world) {
        updateNeighbors(world, pos, false);
        GenericPipeInterfaceEntity.removeNode(world, pos, true, oldState, getNetworkData(world));
    }
    
    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        var worldImp = (Level) world;
        if (worldImp.isClientSide) return state;
        
        if (state.getValue(BlockStateProperties.WATERLOGGED))
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        
        if (!hasNeighboringMachine(state, worldImp, pos, false)) {
            // remove stale machine -> neighboring pipes mapping
            GenericPipeInterfaceEntity.removeStaleMachinePipeNeighbors(pos, getNetworkData(worldImp));
            
            var normalState = getNormalBlock();
            return ((GenericPipeBlock) normalState.getBlock()).addConnectionStates(normalState, worldImp, pos, false);
        }
        
        var interfaceState = state;
        if (!(neighborState.getBlock() instanceof AbstractPipeBlock)) {
            // only update connection if neighbor is a new machine
            var hasMachine = getNetworkData(worldImp).machinePipeNeighbors.getOrDefault(neighborPos, HashSet.newHashSet(0)).contains(direction.getOpposite());
            if (neighborState.is(Blocks.AIR) || !hasMachine) {
                interfaceState = addConnectionStates(state, worldImp, pos, direction);
            }
            
            if (!interfaceState.equals(state)) {
                // reload connection when state has changed (e.g. machine added/removed)
                GenericPipeInterfaceEntity.addNode(worldImp, pos, true, interfaceState, getNetworkData(worldImp));
            }
        }
        
        return interfaceState;
    }
    
    @Override
    protected boolean toggleSideConnection(BlockState state, Direction side, Level world, BlockPos pos) {
        var property = directionToProperty(side);
        var createConnection = state.getValue(property) == NO_CONNECTION;
        
        // check if connection would be valid if state is toggled
        var targetPos = pos.relative(side);
        if (createConnection && !isValidConnectionTarget(world.getBlockState(targetPos).getBlock(), world, side.getOpposite(), targetPos))
            return false;
        
        // toggle connection state
        int nextConnectionState = getNextConnectionState(state, side, world, pos, state.getValue(property));
        var newState = addStraightState(state.setValue(property, nextConnectionState));
        
        // transform to interface block if side is being enabled and machine is connected
        if (!hasNeighboringMachine(newState, world, pos, false)) {
            var normalBlock = (GenericPipeBlock) getNormalBlock().getBlock();
            var interfaceState = normalBlock.addConnectionStates(normalBlock.defaultBlockState(), world, pos, false);
            interfaceState = interfaceState.setValue(normalBlock.directionToProperty(side), newState.getValue(property)); // Hacky way to copy connection state
            world.setBlockAndUpdate(pos, normalBlock.addStraightState(interfaceState));
        } else {
            world.setBlockAndUpdate(pos, newState);
            GenericPipeInterfaceEntity.addNode(world, pos, true, newState, getNetworkData(world));
            
            // update neighbor if it's a pipe
            updateNeighbors(world, pos, true);
        }
        
        // play sound
        var soundGroup = getSoundType(state);
        world.playSound(null, pos, soundGroup.getPlaceSound(), SoundSource.BLOCKS, soundGroup.getVolume() * .5f, soundGroup.getPitch());
        
        return true;
    }
    
    @SuppressWarnings("rawtypes")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        return new ItemStack(getNormalBlock().getBlock());
    }
}
