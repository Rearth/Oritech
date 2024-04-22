package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;

public abstract class GenericPipeConnectionBlock extends GenericPipeBlock implements BlockEntityProvider {
    
    public GenericPipeConnectionBlock(Settings settings) {
        super(settings);
    }
    
    public static BlockState addInterfaceState(BlockState state, World world, BlockPos pos) {
        
        var baseState = GenericPipeBlock.addConnectionState(state, world, pos);
        var lookup = ((GenericPipeBlock) state.getBlock()).getSidesLookup();
        
        var northConnected = checkConnection(pos.north(), world, Direction.SOUTH, lookup) ? 2 : baseState.get(NORTH);
        var eastConnected = checkConnection(pos.east(), world, Direction.WEST, lookup) ? 2 : baseState.get(EAST);
        var southConnected = checkConnection(pos.south(), world, Direction.NORTH, lookup) ? 2 : baseState.get(SOUTH);
        var westConnected = checkConnection(pos.west(), world, Direction.EAST, lookup) ? 2 : baseState.get(WEST);
        var upConnected = checkConnection(pos.up(), world, Direction.DOWN, lookup) ? 2 : baseState.get(UP);
        var downConnected = checkConnection(pos.down(), world, Direction.UP, lookup) ? 2 : baseState.get(DOWN);
        
        return baseState
                 .with(NORTH, northConnected)
                 .with(EAST, eastConnected)
                 .with(SOUTH, southConnected)
                 .with(WEST, westConnected)
                 .with(UP, upConnected)
                 .with(DOWN, downConnected);
    }
    
    private static boolean checkConnection(BlockPos pos, World world, Direction direction, BlockApiLookup<?, Direction> lookup) {
        return lookup.find(world, pos, direction) != null && !(world.getBlockState(pos).getBlock() instanceof GenericPipeBlock);
    }
    
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.getBlock().equals(state.getBlock())) return;
        GenericPipeInterfaceEntity.addNode(pos, true, state, getNetworkData(world));
        
        var regKey = world.getRegistryKey().getValue();
        var dataId = getPipeTypeName() + "_" + regKey.getNamespace() + "_" + regKey.getPath();
        System.out.println("saving for: " + dataId);
        ((ServerWorld) world).getPersistentStateManager().set(dataId, getNetworkData(world));
    }
    
    @Override
    protected void onBlockRemoved(BlockPos pos, BlockState oldState, World world) {
        GenericPipeInterfaceEntity.removeNode(pos, true, oldState, getNetworkData(world));
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var baseState = super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        var interfaceState = addInterfaceState(baseState, (World) world, pos);
        
        if (interfaceState.get(NORTH) != 2
              && interfaceState.get(SOUTH) != 2
              && interfaceState.get(WEST) != 2
              && interfaceState.get(EAST) != 2
              && interfaceState.get(UP) != 2
              && interfaceState.get(DOWN) != 2) {
            var normalPipeState = getNormalBlock();
            normalPipeState = GenericPipeBlock.addConnectionState(normalPipeState, (World) world, pos);
            return normalPipeState;
        }
        
        if (!interfaceState.equals(state)) {
            // reload connection when state has changed (e.g. machine added/removed)
            GenericPipeInterfaceEntity.addNode(pos, true, interfaceState, getNetworkData((World) world));
        }
        
        return interfaceState;
    }
    
    @SuppressWarnings("rawtypes")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
}
