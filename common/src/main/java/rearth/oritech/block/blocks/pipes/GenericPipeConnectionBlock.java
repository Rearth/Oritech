package rearth.oritech.block.blocks.pipes;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
        updateNeighbors(world, pos, false);
        GenericPipeInterfaceEntity.removeNode(world, pos, true, oldState, getNetworkData(world));
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var worldImp = (World) world;
        if (worldImp.isClient) return state;

        if (!hasNeighboringMachine(state, worldImp, pos, false)) {
            var normalState = getNormalBlock();
            return ((GenericPipeBlock) normalState.getBlock()).addConnectionStates(normalState, worldImp, pos, false);
        }

        var interfaceState = state;
        if (!(neighborState.getBlock() instanceof GenericPipeBlock)) {
            interfaceState = addConnectionStates(state, worldImp, pos, direction);

            if (!interfaceState.equals(state)) {
                // reload connection when state has changed (e.g. machine added/removed)
                GenericPipeInterfaceEntity.addNode(worldImp, pos, true, interfaceState, getNetworkData(worldImp));
            }
        }

        return interfaceState;
    }

    @Override
    protected boolean toggleSideConnection(BlockState state, Direction side, World world, BlockPos pos) {
        var property = directionToProperty(side);
        var createConnection = state.get(property) == NO_CONNECTION;

        // check if connection would be valid if state is toggled
        var targetPos = pos.offset(side);
        if (createConnection && !isValidConnectionTarget(world.getBlockState(targetPos).getBlock(), world, side.getOpposite(), targetPos))
            return false;

        // toggle connection state
        int nextConnectionState = getNextConnectionState(state, side, world, pos, state.get(property));
        var newState = addStraightState(state.with(property, nextConnectionState));

        // transform to interface block if side is being enabled and machine is connected
        if (!hasNeighboringMachine(newState, world, pos, false)) {
            var normalBlock = (GenericPipeBlock) getNormalBlock().getBlock();
            var interfaceState = normalBlock.addConnectionStates(normalBlock.getDefaultState(), world, pos, false);
            interfaceState = interfaceState.with(normalBlock.directionToProperty(side), newState.get(property)); // Hacky way to copy connection state
            world.setBlockState(pos, normalBlock.addStraightState(interfaceState));
        } else {
            world.setBlockState(pos, newState);
            GenericPipeInterfaceEntity.addNode(world, pos, true, newState, getNetworkData(world));

            // update neighbor if it's a pipe
            updateNeighbors(world, pos, true);
        }

        // play sound
        var soundGroup = getSoundGroup(state);
        world.playSound(null, pos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS, soundGroup.getVolume() * .5f, soundGroup.getPitch());

        return true;
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
