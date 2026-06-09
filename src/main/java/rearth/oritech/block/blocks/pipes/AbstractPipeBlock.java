package rearth.oritech.block.blocks.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.config.OritechStartupConfig;

import java.util.function.Consumer;

public abstract class AbstractPipeBlock extends Block implements TooltipProvider {

    protected VoxelShape[] boundingShapes;

    public AbstractPipeBlock(Properties settings) {
        super(settings);
        this.boundingShapes = createShapes();
    }

    protected abstract VoxelShape getShape(BlockState state);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!OritechStartupConfig.tightCableHitboxes.get())
            return super.getShape(state, level, pos, context);
        return getShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state);
    }

    protected abstract VoxelShape[] createShapes();

    @Override
    public abstract void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean notify);

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var baseState = addFluidState(super.getStateForPlacement(ctx), ctx.getClickedPos(), ctx.getLevel());
        return addConnectionStates(baseState, ctx.getLevel(), ctx.getClickedPos(), true);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {

        if (!(level instanceof ServerLevel serverLevel)) return state;

        if (neighbourState.isAir())
            // remove potential stale machine -> neighboring pipes mapping
            getNetworkData(serverLevel).machinePipeNeighbors.remove(neighbourPos);

        return state;
    }

    public BlockState addFluidState(BlockState state, BlockPos pos, Level level) {
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) return state;
        return state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        onBlockRemoved(pos, state, level);
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        // nothing here
    }

    /**
     * Updates all the neighboring pipes of the target position.
     *
     * @param level           The target level
     * @param pos             The target position
     * @param neighborToggled Whether the neighbor was toggled
     */
    public abstract void updateNeighbors(Level level, BlockPos pos, boolean neighborToggled);

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.isCreative() && !level.isClientSide()) {
            onBlockRemoved(pos, state, level);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Adds the connection states to the pipe block-state.
     *
     * @param state            The current pipe block-state
     * @param level            The target level
     * @param pos              The target pipe position
     * @param createConnection Whether to create a connection
     * @return The updated block-state
     */
    public abstract BlockState addConnectionStates(BlockState state, Level level, BlockPos pos, boolean createConnection);

    /**
     * Adds the connection states to the pipe block-state.
     * Attempts to create a connection ONLY in the specified direction.
     * Useful for when only one connection needs to be created.
     *
     * @param state           The current pipe block-state
     * @param level           The target level
     * @param pos             The target pipe position
     * @param createDirection The direction to create a connection in
     * @return The updated block-state
     */
    public abstract BlockState addConnectionStates(BlockState state, Level level, BlockPos pos, Direction createDirection);

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
     * @param level            The target level
     * @param createConnection Whether to create a connection
     * @return Boolean whether the pipe should connect
     */
    public abstract boolean shouldConnect(BlockState current, Direction direction, BlockPos currentPos, Level level, boolean createConnection);

    /**
     * Check if the pipe is connecting in a specific direction.
     *
     * @param current          The target pipe block-state
     * @param direction        The direction to check
     * @param createConnection Whether to create a connection
     * @return Boolean whether the pipe is connecting
     */
    public abstract boolean isConnectingInDirection(BlockState current, Direction direction, BlockPos currentPos, Level level, boolean createConnection);

    /**
     * Check if the pipe node has a neighboring machine.
     *
     * @param state The target pipe block-state
     * @param level The target level
     * @param pos   The target pipe position
     * @return Boolean whether a machine is connected
     */
    public boolean hasNeighboringMachine(BlockState state, Level level, BlockPos pos, boolean createConnection) {
        var lookup = apiValidationFunction();
        return (isConnectingInDirection(state, Direction.NORTH, pos, level, createConnection) && hasMachineInDirection(Direction.NORTH, level, pos, lookup))
                || (isConnectingInDirection(state, Direction.EAST, pos, level, createConnection) && hasMachineInDirection(Direction.EAST, level, pos, lookup))
                || (isConnectingInDirection(state, Direction.SOUTH, pos, level, createConnection) && hasMachineInDirection(Direction.SOUTH, level, pos, lookup))
                || (isConnectingInDirection(state, Direction.WEST, pos, level, createConnection) && hasMachineInDirection(Direction.WEST, level, pos, lookup))
                || (isConnectingInDirection(state, Direction.UP, pos, level, createConnection) && hasMachineInDirection(Direction.UP, level, pos, lookup))
                || (isConnectingInDirection(state, Direction.DOWN, pos, level, createConnection) && hasMachineInDirection(Direction.DOWN, level, pos, lookup));
    }

    /**
     * Check if a machine is connected in a specific direction.
     *
     * @param direction The direction to check
     * @param level     The target level
     * @param ownPos    The target pipe position
     * @param lookup    The lookup function {@link AbstractPipeBlock#apiValidationFunction()}
     * @return Boolean whether a machine is connected
     */
    public boolean hasMachineInDirection(Direction direction, Level level, BlockPos ownPos, TriFunction<Level, BlockPos, Direction, Boolean> lookup) {
        var neighborPos = ownPos.offset(direction.getUnitVec3i());
        var neighborState = level.getBlockState(neighborPos);
        return !(neighborState.getBlock() instanceof GenericPipeBlock) && lookup.apply(level, neighborPos, direction.getOpposite());
    }

    /**
     * Check if the target block is a valid connection target.
     *
     * @param target    The target block
     * @param level     The target level
     * @param direction The direction to check (IMPORTANT: This is the direction from the target to the current pipe)
     * @param pos       The target pipe position
     * @return Boolean whether the target is a valid connection target
     */
    public boolean isValidConnectionTarget(Block target, Level level, Direction direction, BlockPos pos) {
        var lookupFunction = apiValidationFunction();
        return connectToOwnBlockType(target) || (lookupFunction.apply(level, pos, direction) && isCompatibleTarget(target));
    }

    /**
     * Check if the target block is a valid interface target.
     *
     * @param target    The target block
     * @param level     The target level
     * @param direction The direction to check (IMPORTANT: This is the direction from the target to the current pipe)
     * @param pos       The target pipe position
     * @return Boolean whether the target is a valid interface target
     */
    public boolean isValidInterfaceTarget(Block target, Level level, Direction direction, BlockPos pos) {
        var lookupFunction = apiValidationFunction();
        return (lookupFunction.apply(level, pos, direction) && isCompatibleTarget(target));
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

    public abstract boolean connectToOwnBlockType(Block block);

    public abstract GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level);

    public abstract SavedDataType<GenericPipeInterfaceEntity.PipeNetworkData> getNetworkDataType();

    protected abstract void onBlockRemoved(BlockPos pos, BlockState oldState, Level level);
}
