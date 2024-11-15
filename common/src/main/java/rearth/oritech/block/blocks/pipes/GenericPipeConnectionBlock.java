package rearth.oritech.block.blocks.pipes;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;

public abstract class GenericPipeConnectionBlock extends GenericPipeBlock implements BlockEntityProvider {
    
    public GenericPipeConnectionBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.getBlock().equals(state.getBlock())) return;
        GenericPipeInterfaceEntity.addNode(world, pos, true, state, getNetworkData(world));
        
        var regKey = world.getRegistryKey().getValue();
        var dataId = getPipeTypeName() + "_" + regKey.getNamespace() + "_" + regKey.getPath();
        Oritech.LOGGER.debug("saving for: " + dataId);
        ((ServerWorld) world).getPersistentStateManager().set(dataId, getNetworkData(world));
    }
    
    @Override
    protected void onBlockRemoved(BlockPos pos, BlockState oldState, World world) {
        GenericPipeInterfaceEntity.removeNode(world, pos, true, oldState, getNetworkData(world));
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var worldImp = (World) world;
        var baseState = super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        var interfaceState = PipeConnectionHelper.addInterfaceStates(baseState, worldImp, pos);

        if (interfaceState.get(NORTH) != GenericPipeBlock.MACHINE_CONNECTION
                && interfaceState.get(SOUTH) != GenericPipeBlock.MACHINE_CONNECTION
                && interfaceState.get(WEST) != GenericPipeBlock.MACHINE_CONNECTION
                && interfaceState.get(EAST) != GenericPipeBlock.MACHINE_CONNECTION
                && interfaceState.get(UP) != GenericPipeBlock.MACHINE_CONNECTION
                && interfaceState.get(DOWN) != GenericPipeBlock.MACHINE_CONNECTION) {
            var normalPipeState = PipeConnectionHelper.addDisabledConnectionStates(getNormalBlock(), interfaceState);
            normalPipeState = PipeConnectionHelper.addConnectionStates(normalPipeState, worldImp, pos);
            return normalPipeState;
        }
        
        if (!interfaceState.equals(state)) {
            // reload connection when state has changed (e.g. machine added/removed)
            GenericPipeInterfaceEntity.addNode(worldImp, pos, true, interfaceState, getNetworkData(worldImp));
        }
        
        return interfaceState;
    }

    @Override
    protected void toggleSideConnection(BlockState state, Direction side, World world, BlockPos pos) {
        var property = PipeConnectionHelper.directionToProperty(side);
        var newState = state.with(property, state.get(property) == CONNECTION_DISABLED ? NO_CONNECTION : CONNECTION_DISABLED);
        newState = PipeConnectionHelper.addInterfaceStates(newState, world, pos);

        // transform to interface block if side is being enabled and machine is connected
        if ((newState.get(NORTH) != GenericPipeBlock.MACHINE_CONNECTION
                && newState.get(SOUTH) != GenericPipeBlock.MACHINE_CONNECTION
                && newState.get(WEST) != GenericPipeBlock.MACHINE_CONNECTION
                && newState.get(EAST) != GenericPipeBlock.MACHINE_CONNECTION
                && newState.get(UP) != GenericPipeBlock.MACHINE_CONNECTION
                && newState.get(DOWN) != GenericPipeBlock.MACHINE_CONNECTION)) {
            var interfaceState = PipeConnectionHelper.addConnectionStates(PipeConnectionHelper.addDisabledConnectionStates(getNormalBlock(), newState), world, pos);
            world.setBlockState(pos, interfaceState);
        } else {
            world.setBlockState(pos, newState);
            GenericPipeInterfaceEntity.addNode(world, pos, true, newState, getNetworkData(world));

            // update neighbor if it's a pipe
            updateNeighborState(world, pos.offset(side));
        }
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
    
    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return new ItemStack(getNormalBlock().getBlock());
    }
}
