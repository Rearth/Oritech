package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;

public abstract class GenericPipeBlock extends Block {
    
    public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
    public static final BooleanProperty EAST = ConnectingBlock.EAST;
    public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;
    public static final BooleanProperty WEST = ConnectingBlock.WEST;
    public static final BooleanProperty UP = ConnectingBlock.UP;
    public static final BooleanProperty DOWN = ConnectingBlock.DOWN;
    private static final Boolean USE_ACCURATE_OUTLINES = true;
    protected final VoxelShape[] boundingShapes;
    
    public GenericPipeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(UP, false).with(DOWN, false));
        boundingShapes = createShapes();
    }
    
    public static BlockState addConnectionState(BlockState state, World world, BlockPos pos) {
        
        var ownBlock = (GenericPipeBlock) state.getBlock();
        var northConnected = checkConnection(pos.north(), world, Direction.SOUTH, ownBlock);
        var eastConnected = checkConnection(pos.east(), world, Direction.WEST, ownBlock);
        var southConnected = checkConnection(pos.south(), world, Direction.NORTH, ownBlock);
        var westConnected = checkConnection(pos.west(), world, Direction.WEST, ownBlock);
        var upConnected = checkConnection(pos.up(), world, Direction.DOWN, ownBlock);
        var downConnected = checkConnection(pos.down(), world, Direction.UP, ownBlock);
        
        return state.with(NORTH, northConnected).with(EAST, eastConnected).with(SOUTH, southConnected).with(WEST, westConnected).with(UP, upConnected).with(DOWN, downConnected);
    }
    
    private static boolean checkConnection(BlockPos pos, World world, Direction direction, GenericPipeBlock self) {
        var state = world.getBlockState(pos);
        return isValidConnectionTarget(state.getBlock(), world, direction, pos, self);
    }
    
    protected static boolean isValidConnectionTarget(Block block, World world, Direction direction, BlockPos pos, GenericPipeBlock self) {
        var lookup = self.getSidesLookup();
        return self.connectToBlockType(block) || lookup.find(world, pos, direction) != null;
    }
    
    public abstract BlockApiLookup<?, Direction> getSidesLookup();
    
    public abstract BlockState getConnectionBlock();
    public abstract BlockState getNormalBlock();
    public abstract String getPipeTypeName();
    
    public abstract boolean connectToBlockType(Block block);
    
    public abstract GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world);
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    private VoxelShape getShape(BlockState state) {
        var shape = boundingShapes[0];
        
        if (state.get(NORTH))
            shape = VoxelShapes.union(shape, boundingShapes[1]);
        if (state.get(EAST))
            shape = VoxelShapes.union(shape, boundingShapes[2]);
        if (state.get(SOUTH))
            shape = VoxelShapes.union(shape, boundingShapes[3]);
        if (state.get(WEST))
            shape = VoxelShapes.union(shape, boundingShapes[4]);
        if (state.get(UP))
            shape = VoxelShapes.union(shape, boundingShapes[5]);
        if (state.get(DOWN))
            shape = VoxelShapes.union(shape, boundingShapes[6]);
        
        return shape;
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!USE_ACCURATE_OUTLINES)
            return super.getOutlineShape(state, world, pos, context);
        return getShape(state);
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShape(state);
    }
    
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        
        if (oldState.getBlock().equals(state.getBlock())) return;
        
        var pipeBlock = (GenericPipeBlock) state.getBlock();
        var lookup = pipeBlock.getSidesLookup();
        
        // transform to interface block on placement when machine is neighbor
        if (hasMachineInDirection(Direction.NORTH, world, pos, lookup)
              || hasMachineInDirection(Direction.EAST, world, pos, lookup)
              || hasMachineInDirection(Direction.SOUTH, world, pos, lookup)
              || hasMachineInDirection(Direction.WEST, world, pos, lookup)
              || hasMachineInDirection(Direction.UP, world, pos, lookup)
              || hasMachineInDirection(Direction.DOWN, world, pos, lookup)) {
            
            var stateBase = getConnectionBlock();
            var stateInterface = GenericPipeConnectionBlock.addInterfaceState(stateBase, world, pos);
            world.setBlockState(pos, stateInterface);
        } else {
            GenericPipeInterfaceEntity.addNode(pos, false, state, getNetworkData(world));
        }
        
    }
    
    public boolean hasMachineInDirection(Direction direction, World world, BlockPos ownPos, BlockApiLookup<?, Direction> lookup) {
        var neighborPos = ownPos.add(direction.getVector());
        var neighborState = world.getBlockState(neighborPos);
        return !(neighborState.getBlock() instanceof GenericPipeBlock) && lookup.find(world, neighborPos, direction.getOpposite()) != null;
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        
        if (!state.isOf(newState.getBlock()) && !(newState.getBlock() instanceof GenericPipeBlock)) {
            // block was removed/replaced instead of updated
            onBlockRemoved(pos, state, world);
        }
        
    }
    
    protected void onBlockRemoved(BlockPos pos, BlockState oldState, World world) {
        GenericPipeInterfaceEntity.removeNode(pos, false, oldState, getNetworkData(world));
    }
    
    protected VoxelShape[] createShapes() {
        VoxelShape inner = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
        VoxelShape north = Block.createCuboidShape(5, 5, 0, 11, 11, 5);
        VoxelShape east = Block.createCuboidShape(0, 5, 5, 5, 11, 11);
        VoxelShape south = Block.createCuboidShape(5, 5, 11, 11, 11, 16);
        VoxelShape west = Block.createCuboidShape(11, 5, 5, 16, 11, 11);
        VoxelShape up = Block.createCuboidShape(5, 11, 5, 11, 16, 11);
        VoxelShape down = Block.createCuboidShape(5, 0, 5, 11, 5, 11);
        
        return new VoxelShape[]{inner, north, west, south, east, up, down};
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var baseState = super.getPlacementState(ctx);
        return addConnectionState(baseState, ctx.getWorld(), ctx.getBlockPos());
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess worldAccess, BlockPos pos, BlockPos neighborPos) {
        
        var world = (World) worldAccess;
        var pipeBlock = (GenericPipeBlock) state.getBlock();
        var lookup = pipeBlock.getSidesLookup();
        
        // transform to interface when machine is placed as neighbor
        if (!(state.getBlock() instanceof GenericPipeConnectionBlock) &&
              (hasMachineInDirection(Direction.NORTH, world, pos, lookup)
              || hasMachineInDirection(Direction.EAST, world, pos, lookup)
              || hasMachineInDirection(Direction.SOUTH, world, pos, lookup)
              || hasMachineInDirection(Direction.WEST, world, pos, lookup)
              || hasMachineInDirection(Direction.UP, world, pos, lookup)
              || hasMachineInDirection(Direction.DOWN, world, pos, lookup))) {
              
            var stateBase = getConnectionBlock();
            return GenericPipeConnectionBlock.addInterfaceState(stateBase, world, pos);
        }
        
        return addConnectionState(state, world, pos);
    }
}
