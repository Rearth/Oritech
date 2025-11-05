package rearth.oritech.block.blocks.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;

public abstract class AbstractPipeBlock extends Block {
    
    private static final Boolean USE_ACCURATE_OUTLINES = Oritech.CONFIG.tightCableHitboxes();
    protected VoxelShape[] boundingShapes;
    
    public AbstractPipeBlock(Properties settings) {
        super(settings);
        this.boundingShapes = createShapes();
    }
    
    protected abstract VoxelShape getShape(BlockState state);
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (!USE_ACCURATE_OUTLINES)
            return super.getShape(state, world, pos, context);
        return getShape(state);
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state);
    }
    
    protected abstract VoxelShape[] createShapes();
    
    @Override
    public abstract void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify);
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var baseState = addFluidState(super.getStateForPlacement(ctx), ctx.getClickedPos(), ctx.getLevel());
        return addConnectionStates(baseState, ctx.getLevel(), ctx.getClickedPos(), true);
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor worldAccess, BlockPos pos, BlockPos neighborPos) {
        var world = (Level) worldAccess;
        if (world.isClientSide) return state;
        
        if (neighborState.is(Blocks.AIR))
            // remove potential stale machine -> neighboring pipes mapping
            getNetworkData(world).machinePipeNeighbors.remove(neighborPos);
        
        return state;
    }
    
    public BlockState addFluidState(BlockState state, BlockPos pos, Level level) {
        return state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
    }
    
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, world, pos, newState, moved);
        
        if (!state.is(newState.getBlock()) && !(newState.getBlock() instanceof AbstractPipeBlock)) {
            // block was removed/replaced instead of updated
            onBlockRemoved(pos, state, world);
        }
        
    }
    
    /**
     * Updates all the neighboring pipes of the target position.
     *
     * @param world           The target world
     * @param pos             The target position
     * @param neighborToggled Whether the neighbor was toggled
     */
    public abstract void updateNeighbors(Level world, BlockPos pos, boolean neighborToggled);
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!player.isCreative() && !world.isClientSide) {
            onBlockRemoved(pos, state, world);
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    /**
     * Adds the connection states to the pipe block-state.
     *
     * @param state            The current pipe block-state
     * @param world            The target world
     * @param pos              The target pipe position
     * @param createConnection Whether to create a connection
     * @return The updated block-state
     */
    public abstract BlockState addConnectionStates(BlockState state, Level world, BlockPos pos, boolean createConnection);
    
    /**
     * Adds the connection states to the pipe block-state.
     * Attempts to create a connection ONLY in the specified direction.
     * Useful for when only one connection needs to be created.
     *
     * @param state           The current pipe block-state
     * @param world           The target world
     * @param pos             The target pipe position
     * @param createDirection The direction to create a connection in
     * @return The updated block-state
     */
    public abstract BlockState addConnectionStates(BlockState state, Level world, BlockPos pos, Direction createDirection);
    
    /**
     * Adds the straight property to the pipe block-state.
     *
     * @param state The current pipe block-state
     * @return The updated block-state
     */
    public abstract BlockState addStraightState(BlockState state);
    
    /**
     * Check if the pipe should connect in a specific direction.
     *
     * @param current          The current pipe block-state
     * @param direction        The direction to check
     * @param currentPos       The current pipe position
     * @param world            The target world
     * @param createConnection Whether to create a connection
     * @return Boolean whether the pipe should connect
     */
    public abstract boolean shouldConnect(BlockState current, Direction direction, BlockPos currentPos, Level world, boolean createConnection);
    
    /**
     * Check if the pipe is connecting in a specific direction.
     *
     * @param current          The target pipe block-state
     * @param direction        The direction to check
     * @param createConnection Whether to create a connection
     * @return Boolean whether the pipe is connecting
     */
    public abstract boolean isConnectingInDirection(BlockState current, Direction direction, BlockPos currentPos, Level world, boolean createConnection);
    
    /**
     * Check if the pipe node has a neighboring machine.
     *
     * @param state The target pipe block-state
     * @param world The target world
     * @param pos   The target pipe position
     * @return Boolean whether a machine is connected
     */
    public boolean hasNeighboringMachine(BlockState state, Level world, BlockPos pos, boolean createConnection) {
        var lookup = apiValidationFunction();
        return (isConnectingInDirection(state, Direction.NORTH, pos, world, createConnection) && hasMachineInDirection(Direction.NORTH, world, pos, lookup))
                 || (isConnectingInDirection(state, Direction.EAST, pos, world, createConnection) && hasMachineInDirection(Direction.EAST, world, pos, lookup))
                 || (isConnectingInDirection(state, Direction.SOUTH, pos, world, createConnection) && hasMachineInDirection(Direction.SOUTH, world, pos, lookup))
                 || (isConnectingInDirection(state, Direction.WEST, pos, world, createConnection) && hasMachineInDirection(Direction.WEST, world, pos, lookup))
                 || (isConnectingInDirection(state, Direction.UP, pos, world, createConnection) && hasMachineInDirection(Direction.UP, world, pos, lookup))
                 || (isConnectingInDirection(state, Direction.DOWN, pos, world, createConnection) && hasMachineInDirection(Direction.DOWN, world, pos, lookup));
    }
    
    /**
     * Check if a machine is connected in a specific direction.
     *
     * @param direction The direction to check
     * @param world     The target world
     * @param ownPos    The target pipe position
     * @param lookup    The lookup function {@link AbstractPipeBlock#apiValidationFunction()}
     * @return Boolean whether a machine is connected
     */
    public boolean hasMachineInDirection(Direction direction, Level world, BlockPos ownPos, TriFunction<Level, BlockPos, Direction, Boolean> lookup) {
        var neighborPos = ownPos.offset(direction.getNormal());
        var neighborState = world.getBlockState(neighborPos);
        return !(neighborState.getBlock() instanceof GenericPipeBlock) && lookup.apply(world, neighborPos, direction.getOpposite());
    }
    
    /**
     * Check if the target block is a valid connection target.
     *
     * @param target    The target block
     * @param world     The target world
     * @param direction The direction to check (IMPORTANT: This is the direction from the target to the current pipe)
     * @param pos       The target pipe position
     * @return Boolean whether the target is a valid connection target
     */
    public boolean isValidConnectionTarget(Block target, Level world, Direction direction, BlockPos pos) {
        var lookupFunction = apiValidationFunction();
        return connectToOwnBlockType(target) || (lookupFunction.apply(world, pos, direction) && isCompatibleTarget(target));
    }
    
    /**
     * Check if the target block is a valid interface target.
     *
     * @param target    The target block
     * @param world     The target world
     * @param direction The direction to check (IMPORTANT: This is the direction from the target to the current pipe)
     * @param pos       The target pipe position
     * @return Boolean whether the target is a valid interface target
     */
    public boolean isValidInterfaceTarget(Block target, Level world, Direction direction, BlockPos pos) {
        var lookupFunction = apiValidationFunction();
        return (lookupFunction.apply(world, pos, direction) && isCompatibleTarget(target));
    }
    
    /**
     * Check if the target block is compatible with the pipe block.
     *
     * @param block The target block
     * @return Boolean whether the block is compatible
     */
    public boolean isCompatibleTarget(Block block) {
        return true;
    }
    
    /**
     * Validation function which utilizes lookup API's to check if a block is a valid connection target.
     *
     * @return The validation function for the pipe block
     */
    public abstract TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction();
    
    public abstract BlockState getConnectionBlock();
    
    public abstract BlockState getNormalBlock();
    
    public abstract String getPipeTypeName();
    
    public abstract boolean connectToOwnBlockType(Block block);
    
    public abstract GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level world);
    
    protected abstract void onBlockRemoved(BlockPos pos, BlockState oldState, Level world);
}
