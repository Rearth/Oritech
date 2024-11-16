package rearth.oritech.block.blocks.pipes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.item.tools.Wrench;

public abstract class GenericPipeBlock extends Block implements Wrench.Wrenchable {

    public static final int NO_CONNECTION = 0;
    public static final int CONNECTION_DISABLED = 1;
    public static final int PIPE_CONNECTION = 2;
    public static final int MACHINE_CONNECTION = 3;
    //public static final int DISABLED_EXTRACT = 4;

    // 0 = no connection, 1 = disabled connection, 2 = pipe connection, 3 = pipe and machine connection
    public static final IntProperty NORTH = IntProperty.of("north", 0, 3);
    public static final IntProperty EAST = IntProperty.of("east", 0, 3);
    public static final IntProperty SOUTH = IntProperty.of("south", 0, 3);
    public static final IntProperty WEST = IntProperty.of("west", 0, 3);
    public static final IntProperty UP = IntProperty.of("up", 0, 3);
    public static final IntProperty DOWN = IntProperty.of("down", 0, 3);

    public static final BooleanProperty STRAIGHT = BooleanProperty.of("straight");
    private static final Boolean USE_ACCURATE_OUTLINES = Oritech.CONFIG.tightCableHitboxes();
    protected final VoxelShape[] boundingShapes;
    
    public GenericPipeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(NORTH, 0).with(EAST, 0).with(SOUTH, 0).with(WEST, 0).with(UP, 0).with(DOWN, 0).with(STRAIGHT, false));
        boundingShapes = createShapes();
    }

    public boolean isCompatibleTarget(Block block) {
        return true;
    }

    /**
     * Validation function which utilizes lookup API's to check if a block is a valid connection target.
     *
     * @return The validation function for the pipe block
     */
    public abstract TriFunction<World, BlockPos, Direction, Boolean> apiValidationFunction();
    
    public abstract BlockState getConnectionBlock();
    public abstract BlockState getNormalBlock();
    public abstract String getPipeTypeName();
    
    public abstract boolean connectToOwnBlockType(Block block);
    
    public abstract GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world);
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, STRAIGHT);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    private VoxelShape getShape(BlockState state) {
        var shape = boundingShapes[0];

        if (state.get(NORTH) > CONNECTION_DISABLED)
            shape = VoxelShapes.union(shape, boundingShapes[1]);
        if (state.get(EAST) > CONNECTION_DISABLED)
            shape = VoxelShapes.union(shape, boundingShapes[2]);
        if (state.get(SOUTH) > CONNECTION_DISABLED)
            shape = VoxelShapes.union(shape, boundingShapes[3]);
        if (state.get(WEST) > CONNECTION_DISABLED)
            shape = VoxelShapes.union(shape, boundingShapes[4]);
        if (state.get(UP) > CONNECTION_DISABLED)
            shape = VoxelShapes.union(shape, boundingShapes[5]);
        if (state.get(DOWN) > CONNECTION_DISABLED)
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
        var stateBase = PipeConnectionHelper.addConnectionStates(state, world, pos);

        // transform to interface block on placement when machine is neighbor
        if (!state.isOf(getConnectionBlock().getBlock()) && PipeConnectionHelper.hasNeighboringMachine(state, world, pos)) {
            var interfaceState = PipeConnectionHelper.addInterfaceStates(PipeConnectionHelper.addDisabledConnectionStates(getConnectionBlock(), stateBase), world, pos);
            world.setBlockState(pos, interfaceState);
        } else {
            GenericPipeInterfaceEntity.addNode(world, pos, false, state, getNetworkData(world));
        }
    }

    @Override
    public ActionResult onWrenchUse(BlockState state, ItemUsageContext context, ItemStack stack) {
        toggleSideConnection(state, Wrench.getDirection(stack), context.getWorld(), context.getBlockPos());
        return ActionResult.SUCCESS;
    }

    /**
     * Toggles the connection state of a pipe side between disabled and enabled.
     *
     * @param state The current pipe block-state
     * @param side  The side to toggle the connection state
     * @param world The target world
     * @param pos   The target pipe position
     */
    protected void toggleSideConnection(BlockState state, Direction side, World world, BlockPos pos) {
        var property = PipeConnectionHelper.directionToProperty(side);
        var newState = state.with(property, state.get(property) == CONNECTION_DISABLED ? NO_CONNECTION : CONNECTION_DISABLED);
        newState = PipeConnectionHelper.addConnectionStates(newState, world, pos);

        // transform to interface block if side is being enabled and machine is connected
        if (!newState.isOf(getConnectionBlock().getBlock()) && newState.get(property) != CONNECTION_DISABLED && PipeConnectionHelper.hasMachineInDirection(side, world, pos, apiValidationFunction())) {
            var interfaceState = PipeConnectionHelper.addInterfaceStates(PipeConnectionHelper.addDisabledConnectionStates(getConnectionBlock(), newState), world, pos);
            world.setBlockState(pos, interfaceState);
        } else {
            world.setBlockState(pos, newState);
            GenericPipeInterfaceEntity.addNode(world, pos, false, newState, getNetworkData(world));

            // update neighbor if it's a pipe
            updateNeighborState(world, pos.offset(side));
        }

        var soundGroup = getSoundGroup(state);
        world.playSound(null, pos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS, soundGroup.getVolume() * .5f, soundGroup.getPitch());
    }

    /**
     * Updates the neighbor state of a pipe block.
     * Used to update the neighboring pipe block when a connection is toggled.
     *
     * @param world The target world
     * @param pos   The target pipe position
     */
    protected void updateNeighborState(World world, BlockPos pos) {
        var state = world.getBlockState(pos);
        if (state.isOf(getConnectionBlock().getBlock())) {
            var neighborStateBase = PipeConnectionHelper.addInterfaceStates(state, world, pos);
            world.setBlockState(pos, neighborStateBase);
            GenericPipeInterfaceEntity.addNode(world, pos, true, neighborStateBase, getNetworkData(world));
        } else if (state.isOf(getNormalBlock().getBlock())) {
            var neighborStateBase = PipeConnectionHelper.addConnectionStates(state, world, pos);
            world.setBlockState(pos, neighborStateBase);
            GenericPipeInterfaceEntity.addNode(world, pos, false, neighborStateBase, getNetworkData(world));
        }
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
        GenericPipeInterfaceEntity.removeNode(world, pos, false, oldState, getNetworkData(world));
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
        return PipeConnectionHelper.addConnectionStates(baseState, ctx.getWorld(), ctx.getBlockPos());
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess worldAccess, BlockPos pos, BlockPos neighborPos) {
        var world = (World) worldAccess;

        // transform to interface when machine is placed as neighbor
        if (!(state.getBlock() instanceof GenericPipeConnectionBlock) && PipeConnectionHelper.hasNeighboringMachine(state, world, pos)) {
            var stateBase = PipeConnectionHelper.addDisabledConnectionStates(getConnectionBlock(), state);
            return PipeConnectionHelper.addInterfaceStates(stateBase, world, pos);
        }
        return PipeConnectionHelper.addConnectionStates(state, world, pos);
    }

    /**
     * Helper class for pipe connections related methods.
     */
    public static class PipeConnectionHelper {

        /**
         * Adds the connection related states to the pipe block-state.
         *
         * @param state The current pipe block-state
         * @param world The current world
         * @param pos   The current pipe position
         * @return The updated block-state
         */
        public static BlockState addConnectionStates(BlockState state, World world, BlockPos pos) {
            var ownBlock = (GenericPipeBlock) state.getBlock();

            // Add state for each direction
            for (Direction direction : Direction.values()) {
                // Check if the pipe can connect in the direction
                if (canConnectInDirection(state, direction))
                    // Check if a connection between nodes is allowed
                    if (isValidConnection(ownBlock, world, pos.offset(direction), direction.getOpposite()))
                        state = state.with(directionToProperty(direction), PIPE_CONNECTION);
                    else state = state.with(directionToProperty(direction), NO_CONNECTION);
                else state = state.with(directionToProperty(direction), CONNECTION_DISABLED);
            }

            return addStraightState(state);
        }

        /**
         * Adds the machine connection states to the pipe block-state.
         *
         * @param state The current pipe block-state
         * @param world The target world
         * @param pos   The target pipe position
         * @return The updated block-state
         */
        public static BlockState addInterfaceStates(BlockState state, World world, BlockPos pos) {
            var baseState = addConnectionStates(state, world, pos);
            var lookup = ((GenericPipeBlock) state.getBlock()).apiValidationFunction();

            // Add machine connection state for each direction
            for (Direction direction : Direction.values()) {
                var property = directionToProperty(direction);
                int connection = baseState.get(property);

                // Check if the pipe can connect in the direction and if a machine is connected
                if (canConnectInDirection(baseState, direction) && isValidInterfaceConnection(pos.offset(direction), world, direction.getOpposite(), lookup))
                    connection = MACHINE_CONNECTION;
                baseState = baseState.with(property, connection);
            }

            return baseState;
        }

        /**
         * Adds the disabled connection states to the pipe block-state.
         * Used to maintain disabled connections when updating the pipe block-state.
         *
         * @param newState The new pipe block-state
         * @param oldState The old pipe block-state
         * @return The updated block-state
         */
        public static BlockState addDisabledConnectionStates(BlockState newState, BlockState oldState) {
            for (Direction direction : Direction.values()) {
                if (oldState.get(directionToProperty(direction)) == CONNECTION_DISABLED)
                    newState = newState.with(directionToProperty(direction), CONNECTION_DISABLED);
            }

            return newState;
        }

        /**
         * Adds the straight property to the pipe block-state.
         *
         * @param state The current pipe block-state
         * @return The updated block-state
         */
        public static BlockState addStraightState(BlockState state) {
            var north = state.get(NORTH) > CONNECTION_DISABLED;
            var south = state.get(SOUTH) > CONNECTION_DISABLED;
            var east = state.get(EAST) > CONNECTION_DISABLED;
            var west = state.get(WEST) > CONNECTION_DISABLED;
            var up = state.get(UP) > CONNECTION_DISABLED;
            var down = state.get(DOWN) > CONNECTION_DISABLED;

            // Check for straight connections along each axis
            boolean straightX = north && south && !east && !west && !up && !down;
            boolean straightY = up && down && !north && !south && !east && !west;
            boolean straightZ = east && west && !north && !south && !up && !down;

            // The pipe is straight if exactly one of the axes has a straight connection
            var straight = straightX || straightY || straightZ;

            return state.with(STRAIGHT, straight);
        }

        /**
         * Check if a connection between two nodes is valid.
         *
         * @param currentPipe The current pipe block
         * @param world       The target world
         * @param targetPos   The target pipe position
         * @param direction   The direction to check (IMPORTANT: This is the direction from the target to the current pipe)
         * @return Boolean whether the connection is valid
         */
        public static boolean isValidConnection(GenericPipeBlock currentPipe, World world, BlockPos targetPos, Direction direction) {
            var targetState = world.getBlockState(targetPos);
            if (!isValidConnectionTarget(currentPipe, targetState.getBlock(), world, targetPos, direction))
                return false;
            if (targetState.getBlock() instanceof GenericPipeBlock)
                return PipeConnectionHelper.canConnectInDirection(targetState, direction);

            return true;
        }

        /**
         * Check if a connection between a pipe and a machine is valid.
         * @param pos The target pipe position
         * @param world The target world
         * @param direction The direction to check (IMPORTANT: This is the direction from the target to the current pipe)
         * @param lookup The lookup function {@link GenericPipeBlock#apiValidationFunction()}
         * @return Boolean whether the connection is valid
         */
        public static boolean isValidInterfaceConnection(BlockPos pos, World world, Direction direction, TriFunction<World, BlockPos, Direction, Boolean> lookup) {
            return lookup.apply(world, pos, direction) && !(world.getBlockState(pos).getBlock() instanceof GenericPipeBlock);
        }

        /**
         * Check if the target block is a valid connection target.
         *
         * @param currentPipe The current pipe block
         * @param target      The target block
         * @param world       The target world
         * @param targetPos   The target pipe position
         * @param direction   The direction to check (IMPORTANT: This is the direction from the target to the current pipe)
         * @return Boolean whether the target is a valid connection target
         */
        protected static boolean isValidConnectionTarget(GenericPipeBlock currentPipe, Block target, World world, BlockPos targetPos, Direction direction) {
            var lookupFunction = currentPipe.apiValidationFunction();
            return currentPipe.connectToOwnBlockType(target) || (lookupFunction.apply(world, targetPos, direction) && currentPipe.isCompatibleTarget(target));
        }

        /**
         * Check if the pipe can connect in a specific direction.
         *
         * @param state     The pipe block-state
         * @param direction nThe direction to check
         * @return Boolean whether the connection is enabled or disabled
         */
        public static boolean canConnectInDirection(BlockState state, Direction direction) {
            return PipeConnectionHelper.directionToPropertyValue(state, direction) != CONNECTION_DISABLED;
        }

        /**
         * Check if the pipe node has a neighboring machine.
         *
         * @param state The target pipe block-state
         * @param world The target world
         * @param pos   The target pipe position
         * @return Boolean whether a machine is connected
         */
        public static boolean hasNeighboringMachine(BlockState state, World world, BlockPos pos) {
            var pipeBlock = (GenericPipeBlock) state.getBlock();
            var lookup = pipeBlock.apiValidationFunction();
            return (canConnectInDirection(state, Direction.NORTH) && hasMachineInDirection(Direction.NORTH, world, pos, lookup))
                    || (canConnectInDirection(state, Direction.EAST) && hasMachineInDirection(Direction.EAST, world, pos, lookup))
                    || (canConnectInDirection(state, Direction.SOUTH) && hasMachineInDirection(Direction.SOUTH, world, pos, lookup))
                    || (canConnectInDirection(state, Direction.WEST) && hasMachineInDirection(Direction.WEST, world, pos, lookup))
                    || (canConnectInDirection(state, Direction.UP) && hasMachineInDirection(Direction.UP, world, pos, lookup))
                    || (canConnectInDirection(state, Direction.DOWN) && hasMachineInDirection(Direction.DOWN, world, pos, lookup));
        }

        /**
         * Check if a machine is connected in a specific direction.
         *
         * @param direction The direction to check
         * @param world     The target world
         * @param ownPos    The target pipe position
         * @param lookup    The lookup function {@link GenericPipeBlock#apiValidationFunction()}
         * @return Boolean whether a machine is connected
         */
        public static boolean hasMachineInDirection(Direction direction, World world, BlockPos ownPos, TriFunction<World, BlockPos, Direction, Boolean> lookup) {
            var neighborPos = ownPos.add(direction.getVector());
            var neighborState = world.getBlockState(neighborPos);
            return !(neighborState.getBlock() instanceof GenericPipeBlock) && lookup.apply(world, neighborPos, direction.getOpposite());
        }

        /**
         * Converts a {@link Direction} into an IntProperty value for a connection
         *
         * @param state     State to pull the value from
         * @param direction Respective direction
         * @return the connection value
         */
        public static int directionToPropertyValue(BlockState state, Direction direction) {
            if (direction == Direction.NORTH)
                return state.get(NORTH);
            else if (direction == Direction.EAST)
                return state.get(EAST);
            else if (direction == Direction.SOUTH)
                return state.get(SOUTH);
            else if (direction == Direction.WEST)
                return state.get(WEST);
            else if (direction == Direction.UP)
                return state.get(UP);
            else return state.get(DOWN);
        }

        /**
         * Converts a {@link Direction} into a {@link IntProperty} for a connection
         *
         * @param direction Respective direction
         * @return the property
         */
        public static IntProperty directionToProperty(Direction direction) {
            if (direction == Direction.NORTH)
                return NORTH;
            else if (direction == Direction.EAST)
                return EAST;
            else if (direction == Direction.SOUTH)
                return SOUTH;
            else if (direction == Direction.WEST)
                return WEST;
            else if (direction == Direction.UP)
                return UP;
            else return DOWN;
        }
    }
}
