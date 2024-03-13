package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;

public abstract class GenericPipeConnectionBlock extends GenericPipeBlock implements BlockEntityProvider {
    public static final BooleanProperty INTERFACE_NORTH = BooleanProperty.of("con_north");
    public static final BooleanProperty INTERFACE_EAST = BooleanProperty.of("con_east");
    public static final BooleanProperty INTERFACE_SOUTH = BooleanProperty.of("con_south");
    public static final BooleanProperty INTERFACE_WEST = BooleanProperty.of("con_west");
    public static final BooleanProperty INTERFACE_UP = BooleanProperty.of("con_up");
    public static final BooleanProperty INTERFACE_DOWN = BooleanProperty.of("con_down");
    
    public GenericPipeConnectionBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(INTERFACE_NORTH, false).with(INTERFACE_EAST, false).with(INTERFACE_SOUTH, false).with(INTERFACE_WEST, false).with(INTERFACE_UP, false).with(INTERFACE_DOWN, false));
    }
    
    public static BlockState addInterfaceState(BlockState state, World world, BlockPos pos) {
        
        var baseState = GenericPipeBlock.addConnectionState(state, world, pos);
        var lookup = ((GenericPipeBlock) state.getBlock()).getSidesLookup();
        
        var northConnected = checkConnection(pos.north(), world, Direction.SOUTH, lookup);
        var eastConnected = checkConnection(pos.east(), world, Direction.WEST, lookup);
        var southConnected = checkConnection(pos.south(), world, Direction.NORTH, lookup);
        var westConnected = checkConnection(pos.west(), world, Direction.WEST, lookup);
        var upConnected = checkConnection(pos.up(), world, Direction.DOWN, lookup);
        var downConnected = checkConnection(pos.down(), world, Direction.UP, lookup);
        
        return baseState
                 .with(INTERFACE_NORTH, northConnected)
                 .with(INTERFACE_EAST, eastConnected)
                 .with(INTERFACE_SOUTH, southConnected)
                 .with(INTERFACE_WEST, westConnected)
                 .with(INTERFACE_UP, upConnected)
                 .with(INTERFACE_DOWN, downConnected);
    }
    
    private static boolean checkConnection(BlockPos pos, World world, Direction direction, BlockApiLookup<?, Direction> lookup) {
        return lookup.find(world, pos, direction) != null && !(world.getBlockState(pos).getBlock() instanceof GenericPipeBlock);
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(INTERFACE_NORTH, INTERFACE_EAST, INTERFACE_SOUTH, INTERFACE_WEST, INTERFACE_UP, INTERFACE_DOWN);
    }
    
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.getBlock().equals(state.getBlock())) return;
        GenericPipeInterfaceEntity.addNode(pos, true, state, getNetworkData());
        
        var regKey = world.getRegistryKey().getValue();
        var dataId = getPipeTypeName() + "_" + regKey.getNamespace() + "_" + regKey.getPath();
        ((ServerWorld) world).getPersistentStateManager().set(dataId, getNetworkData());
    }
    
    @Override
    protected void onBlockRemoved(BlockPos pos, BlockState oldState) {
        GenericPipeInterfaceEntity.removeNode(pos, true, oldState, getNetworkData());
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var baseState = super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        var interfaceState = addInterfaceState(baseState, (World) world, pos);
        
        if (!interfaceState.get(INTERFACE_NORTH)
              && !interfaceState.get(INTERFACE_SOUTH)
              && !interfaceState.get(INTERFACE_WEST)
              && !interfaceState.get(INTERFACE_EAST)
              && !interfaceState.get(INTERFACE_UP)
              && !interfaceState.get(INTERFACE_DOWN)) {
            var normalPipeState = getNormalBlock();
            normalPipeState = GenericPipeBlock.addConnectionState(normalPipeState, (World) world, pos);
            return normalPipeState;
        }
        
        
        return interfaceState;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (world.isClient) return ActionResult.SUCCESS;
        
        if (world.getBlockEntity(pos) instanceof GenericPipeInterfaceEntity interfaceEntity) {
            interfaceEntity.reloadNetworkForInterface();
        }
        
        return ActionResult.SUCCESS;
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
