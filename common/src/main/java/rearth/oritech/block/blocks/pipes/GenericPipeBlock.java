package rearth.oritech.block.blocks.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.item.tools.Wrench;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class GenericPipeBlock extends AbstractPipeBlock implements Wrench.Wrenchable, SimpleWaterloggedBlock {
    
    // 0 = no connection, 1 = connection (pipe->pipe or pipe->machine)
    public static int NO_CONNECTION = 0;
    public static int CONNECTION = 1;
    
    public static final IntegerProperty NORTH = IntegerProperty.create("north", 0, 1);
    public static final IntegerProperty EAST = IntegerProperty.create("east", 0, 1);
    public static final IntegerProperty SOUTH = IntegerProperty.create("south", 0, 1);
    public static final IntegerProperty WEST = IntegerProperty.create("west", 0, 1);
    public static final IntegerProperty UP = IntegerProperty.create("up", 0, 1);
    public static final IntegerProperty DOWN = IntegerProperty.create("down", 0, 1);
    public static final BooleanProperty STRAIGHT = BooleanProperty.create("straight");
    
    public static final VoxelShape[] THICK_SHAPES = createShapes(
      Block.box(5, 5, 5, 11, 11, 11),
      Block.box(5, 5, 0, 11, 11, 5),
      Block.box(11, 5, 5, 16, 11, 11),
      Block.box(5, 5, 11, 11, 11, 16),
      Block.box(0, 5, 5, 5, 11, 11),
      Block.box(5, 11, 5, 11, 16, 11),
      Block.box(5, 0, 5, 11, 5, 11)
    );
    public static final VoxelShape[] EXTRA_THICK_SHAPES = createShapes(
      Block.box(4, 4, 4, 12, 12, 12),
      Block.box(4, 4, 0, 12, 12, 4),
      Block.box(12, 4, 4, 16, 12, 12),
      Block.box(4, 4, 12, 12, 12, 16),
      Block.box(0, 4, 4, 4, 12, 12),
      Block.box(4, 12, 4, 12, 16, 12),
      Block.box(4, 0, 4, 12, 4, 12)
    );
    public static final VoxelShape[] THIN_SHAPES = createShapes(
      Block.box(6, 6, 6, 10, 10, 10),
      Block.box(6, 6, 0, 10, 10, 6),
      Block.box(10, 6, 6, 16, 10, 10),
      Block.box(6, 6, 10, 10, 10, 16),
      Block.box(0, 6, 6, 6, 10, 10),
      Block.box(6, 10, 6, 10, 16, 10),
      Block.box(6, 0, 6, 10, 6, 10)
    );
    
    public GenericPipeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState()
                                    .setValue(getNorthProperty(), 0)
                                    .setValue(getEastProperty(), 0)
                                    .setValue(getSouthProperty(), 0)
                                    .setValue(getWestProperty(), 0)
                                    .setValue(getUpProperty(), 0)
                                    .setValue(getDownProperty(), 0)
                                    .setValue(STRAIGHT, false)
                                    .setValue(BlockStateProperties.WATERLOGGED, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(getNorthProperty(), getEastProperty(), getSouthProperty(), getWestProperty(), getUpProperty(), getDownProperty(), STRAIGHT, BlockStateProperties.WATERLOGGED);
    }
    
    protected VoxelShape getShape(BlockState state) {
        return boundingShapes[packStates(state)];
    }
    
    protected VoxelShape[] createShapes() {
        return THICK_SHAPES;
    }
    
    public static VoxelShape[] createShapes(VoxelShape inner, VoxelShape north, VoxelShape east, VoxelShape south, VoxelShape west, VoxelShape up, VoxelShape down) {
        VoxelShape[] shapes = new VoxelShape[64];
        
        for (int i = 0; i <= 63; i++) {
            VoxelShape shape = inner;
            if ((i & 1) != 0) shape = Shapes.joinUnoptimized(shape, north, BooleanOp.OR);
            if ((i & 2) != 0) shape = Shapes.joinUnoptimized(shape, east, BooleanOp.OR);
            if ((i & 4) != 0) shape = Shapes.joinUnoptimized(shape, south, BooleanOp.OR);
            if ((i & 8) != 0) shape = Shapes.joinUnoptimized(shape, west, BooleanOp.OR);
            if ((i & 16) != 0) shape = Shapes.joinUnoptimized(shape, up, BooleanOp.OR);
            if ((i & 32) != 0) shape = Shapes.joinUnoptimized(shape, down, BooleanOp.OR);
            shapes[i] = shape.optimize();
        }
        
        return shapes;
    }
    
    private int packStates(BlockState state) {
        int i = 0;
        if (state.getValue(getNorthProperty()) != NO_CONNECTION) i |= 1;
        if (state.getValue(getEastProperty()) != NO_CONNECTION) i |= 2;
        if (state.getValue(getSouthProperty()) != NO_CONNECTION) i |= 4;
        if (state.getValue(getWestProperty()) != NO_CONNECTION) i |= 8;
        if (state.getValue(getUpProperty()) != NO_CONNECTION) i |= 16;
        if (state.getValue(getDownProperty()) != NO_CONNECTION) i |= 32;
        return i;
    }
    
    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.getBlock().equals(state.getBlock())) return;
        else if (oldState.is(getConnectionBlock().getBlock())) {
            GenericPipeInterfaceEntity.addNode(world, pos, false, state, getNetworkData(world));
            return;
        }
        
        // transform to interface block on placement when machine is neighbor
        if (hasNeighboringMachine(state, world, pos, true)) {
            var connectionBlock = getConnectionBlock();
            var interfaceState = ((GenericPipeBlock) connectionBlock.getBlock()).addConnectionStates(connectionBlock, world, pos, true);
            world.setBlockAndUpdate(pos, interfaceState);
        } else {
            // no states need to be added (see getPlacementState)
            GenericPipeInterfaceEntity.addNode(world, pos, false, state, getNetworkData(world));
        }
        
        updateNeighbors(world, pos, false);
    }
    
    // also known as 'getStateForNeighborUpdate'
    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor worldAccess, BlockPos pos, BlockPos neighborPos) {
        var world = (Level) worldAccess;
        if (world.isClientSide) return state;
        
        if (state.getValue(BlockStateProperties.WATERLOGGED))
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        
        // transform to interface when machine is placed as neighbor
        if (hasMachineInDirection(direction, world, pos, apiValidationFunction())) {
            // Only update if the neighbor is a new machine
            var hasMachine = getNetworkData(world).machinePipeNeighbors.getOrDefault(neighborPos, HashSet.newHashSet(0)).contains(direction.getOpposite());
            if (hasMachine) return state;
            
            var connectionBlock = getConnectionBlock();
            return ((GenericPipeBlock) connectionBlock.getBlock()).addConnectionStates(connectionBlock, world, pos, direction);
        } else if (neighborState.is(Blocks.AIR))
            // remove potential stale machine -> neighboring pipes mapping
            getNetworkData(world).machinePipeNeighbors.remove(neighborPos);
        
        return state;
    }
    
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, world, pos, newState, moved);
        
        if (!state.is(newState.getBlock()) && !(newState.getBlock() instanceof GenericPipeBlock)) {
            // block was removed/replaced instead of updated
            onBlockRemoved(pos, state, world);
        }
        
    }
    
    @Override
    protected @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    
    /**
     * Updates all the neighboring pipes of the target position.
     *
     * @param world           The target world
     * @param pos             The target position
     * @param neighborToggled Whether the neighbor was toggled
     */
    public void updateNeighbors(Level world, BlockPos pos, boolean neighborToggled) {
        for (var direction : Direction.values()) {
            var neighborPos = pos.relative(direction);
            var neighborState = world.getBlockState(neighborPos);
            // Only update pipes
            if (neighborState.getBlock() instanceof AbstractPipeBlock pipeBlock) {
                var updatedState = pipeBlock.addConnectionStates(neighborState, world, neighborPos, false);
                world.setBlockAndUpdate(neighborPos, updatedState);
                
                // Update network data if the state was changed
                if (!neighborState.equals(updatedState) || pipeBlock instanceof GenericPipeDuctBlock) {
                    boolean interfaceBlock = updatedState.is(getConnectionBlock().getBlock());
                    if (neighborToggled)
                        GenericPipeInterfaceEntity.addNode(world, neighborPos, interfaceBlock, updatedState, getNetworkData(world));
                }
            }
        }
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!player.isCreative() && !world.isClientSide) {
            onBlockRemoved(pos, state, world);
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        
        if (!level.isClientSide() && stack.is(TagContent.WRENCHES) && !stack.is(ItemContent.WRENCH)) {
            this.onWrenchUse(state, level, pos, player, hand);
            return ItemInteractionResult.SUCCESS;
        }
        
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
    
    @Override
    public InteractionResult onWrenchUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            this.playerWillDestroy(world, pos, state, player);
            world.destroyBlock(pos, true, player);
            return InteractionResult.SUCCESS;
        }
        
        return toggleSideConnection(state, getInteractDirection(state, pos, player), world, pos) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    
    @Override
    public InteractionResult onWrenchUseNeighbor(BlockState state, BlockState neighborState, Level world, BlockPos pos, BlockPos neighborPos, Direction neighborFace, Player player, InteractionHand hand) {
        return toggleSideConnection(state, neighborFace.getOpposite(), world, pos) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    
    protected Direction getInteractDirection(BlockState state, BlockPos pos, Player player) {
        var shapes = getActiveShapes(state);
        var start = player.getEyePosition(0f);
        var end = start.add(player.getViewVector(0).scale(5));
        
        var targetShape = shapes.getFirst();
        var distance = Double.MAX_VALUE;
        var hitPos = Vec3.ZERO;
        for (var shape : shapes) {
            var hitResult = shape.clip(start, end, pos);
            if (hitResult == null) continue;
            
            // skip center if we already matched one of the outer ones
            if (shape.equals(shapes.getLast()) && distance < Double.MAX_VALUE) continue;
            
            var shapeDistance = hitResult.getLocation().distanceTo(start);
            if (shapeDistance < distance) {
                distance = shapeDistance;
                targetShape = shape;
                hitPos = hitResult.getLocation();
            }
        }
        
        var center = targetShape.bounds().getCenter();
        var diff = center.subtract(new Vec3(0.5, 0.5, 0.5));
        if (diff.equals(Vec3.ZERO))
            // center hit
            diff = hitPos.subtract(center.add(Vec3.atLowerCornerOf(pos)));
        
        return Direction.getNearest(diff.x, diff.y, diff.z);
    }
    
    // only returns the outside shapes
    private List<VoxelShape> getActiveShapes(BlockState state) {
        
        var shapes = new ArrayList<VoxelShape>();
        if (state.getValue(getNorthProperty()) != NO_CONNECTION)
            shapes.add(Block.box(5, 5, 0, 11, 11, 5));
        if (state.getValue(getEastProperty()) != NO_CONNECTION)
            shapes.add(Block.box(11, 5, 5, 16, 11, 11));
        if (state.getValue(getSouthProperty()) != NO_CONNECTION)
            shapes.add(Block.box(5, 5, 11, 11, 11, 16));
        if (state.getValue(getWestProperty()) != NO_CONNECTION)
            shapes.add(Block.box(0, 5, 5, 5, 11, 11));
        if (state.getValue(getUpProperty()) != NO_CONNECTION)
            shapes.add(Block.box(5, 11, 5, 11, 16, 11));
        if (state.getValue(getDownProperty()) != NO_CONNECTION)
            shapes.add(Block.box(5, 0, 5, 11, 5, 11));
        
        shapes.add(Block.box(5, 5, 5, 11, 11, 11));
        
        return shapes;
    }
    
    /**
     * Toggles the connection state of a pipe side between disabled and enabled.
     *
     * @param state The current pipe block-state
     * @param side  The side to toggle the connection state
     * @param world The target world
     * @param pos   The target pipe position
     */
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
        if (!newState.is(getConnectionBlock().getBlock()) && createConnection && hasMachineInDirection(side, world, pos, apiValidationFunction())) {
            var connectionState = getConnectionBlock();
            var interfaceState = ((GenericPipeBlock) connectionState.getBlock()).addConnectionStates(connectionState, world, pos, side);
            interfaceState = addFluidState(interfaceState, pos, world);
            world.setBlockAndUpdate(pos, interfaceState);
        } else {
            world.setBlockAndUpdate(pos, newState);
            GenericPipeInterfaceEntity.addNode(world, pos, false, newState, getNetworkData(world));
            
            // update neighbor if it's a pipe
            updateNeighbors(world, pos, true);
        }
        
        // play sound
        var soundGroup = getSoundType(state);
        world.playSound(null, pos, soundGroup.getPlaceSound(), SoundSource.BLOCKS, soundGroup.getVolume() * .5f, soundGroup.getPitch());
        
        return true;
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
    public BlockState addConnectionStates(BlockState state, Level world, BlockPos pos, boolean createConnection) {
        
        state = addFluidState(state, pos, world);
        
        for (var direction : Direction.values()) {
            var property = directionToProperty(direction);
            var connection = shouldConnect(state, direction, pos, world, createConnection);
            state = state.setValue(property, connection ? CONNECTION : NO_CONNECTION);
        }
        
        return addStraightState(state);
    }
    
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
    public BlockState addConnectionStates(BlockState state, Level world, BlockPos pos, Direction createDirection) {
        for (var direction : Direction.values()) {
            var property = directionToProperty(direction);
            var connection = shouldConnect(state, direction, pos, world, direction.equals(createDirection));
            state = state.setValue(property, connection ? CONNECTION : NO_CONNECTION);
        }
        return addFluidState(addStraightState(state), pos, world);
    }
    
    /**
     * Adds the straight property to the pipe block-state.
     *
     * @param state The current pipe block-state
     * @return The updated block-state
     */
    public BlockState addStraightState(BlockState state) {
        var north = state.getValue(getNorthProperty()) != NO_CONNECTION;
        var south = state.getValue(getSouthProperty()) != NO_CONNECTION;
        var east = state.getValue(getEastProperty()) != NO_CONNECTION;
        var west = state.getValue(getWestProperty()) != NO_CONNECTION;
        var up = state.getValue(getUpProperty()) != NO_CONNECTION;
        var down = state.getValue(getDownProperty()) != NO_CONNECTION;
        
        // Check for straight connections along each axis
        boolean straightX = north && south && !east && !west && !up && !down;
        boolean straightY = up && down && !north && !south && !east && !west;
        boolean straightZ = east && west && !north && !south && !up && !down;
        
        // The pipe is straight if exactly one of the axes has a straight connection
        var straight = straightX || straightY || straightZ;
        
        return state.setValue(STRAIGHT, straight);
    }
    
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
    public boolean shouldConnect(BlockState current, Direction direction, BlockPos currentPos, Level world, boolean createConnection) {
        var targetPos = currentPos.relative(direction);
        var targetState = world.getBlockState(targetPos);
        
        // If creating a connection we don't check the other pipe's connection state, force the connection
        // Otherwise we check if the other pipe is connecting in the opposite direction
        if (createConnection) {
            return isValidConnectionTarget(targetState.getBlock(), world, direction.getOpposite(), targetPos);
        } else if (targetState.getBlock() instanceof AbstractPipeBlock pipeBlock) {
            return pipeBlock.isConnectingInDirection(targetState, direction.getOpposite(), targetPos, world, false);
        } else
            return isConnectingInDirection(current, direction, currentPos, world, false) && isValidInterfaceTarget(targetState.getBlock(), world, direction.getOpposite(), targetPos);
    }
    
    /**
     * Check if the pipe is connecting in a specific direction.
     *
     * @param current          The target pipe block-state
     * @param direction        The direction to check
     * @param createConnection Whether to create a connection
     * @return Boolean whether the pipe is connecting
     */
    public boolean isConnectingInDirection(BlockState current, Direction direction, BlockPos currentPos, Level world, boolean createConnection) {
        var block = current.getBlock();
        if (!(block instanceof GenericPipeBlock pipeBlock)) return false;
        var property = pipeBlock.directionToProperty(direction);
        return current.getValue(property) >= CONNECTION || createConnection && current.getValue(property) == NO_CONNECTION;
    }
    
    /**
     * Converts a {@link Direction} into an IntProperty value for a connection
     *
     * @param state     State to pull the value from
     * @param direction Respective direction
     * @return the connection value
     */
    public int directionToPropertyValue(BlockState state, Direction direction) {
        if (direction == Direction.NORTH)
            return state.getValue(getNorthProperty());
        else if (direction == Direction.EAST)
            return state.getValue(getEastProperty());
        else if (direction == Direction.SOUTH)
            return state.getValue(getSouthProperty());
        else if (direction == Direction.WEST)
            return state.getValue(getWestProperty());
        else if (direction == Direction.UP)
            return state.getValue(getUpProperty());
        else return state.getValue(getDownProperty());
    }
    
    /**
     * Converts a {@link Direction} into a {@link IntegerProperty} for a connection
     *
     * @param direction Respective direction
     * @return the property
     */
    public IntegerProperty directionToProperty(Direction direction) {
        if (direction == Direction.NORTH)
            return getNorthProperty();
        else if (direction == Direction.EAST)
            return getEastProperty();
        else if (direction == Direction.SOUTH)
            return getSouthProperty();
        else if (direction == Direction.WEST)
            return getWestProperty();
        else if (direction == Direction.UP)
            return getUpProperty();
        else return getDownProperty();
    }
    
    protected int getNextConnectionState(BlockState state, Direction side, Level world, BlockPos pos, int current) {
        return current == NO_CONNECTION ? CONNECTION : NO_CONNECTION;
    }
    
    protected void onBlockRemoved(BlockPos pos, BlockState oldState, Level world) {
        updateNeighbors(world, pos, false);
        GenericPipeInterfaceEntity.removeNode(world, pos, false, oldState, getNetworkData(world));
    }
    
    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0f;
    }
    
    /*
     * The following is a hacky implementation to allow child classes to modify the connection properties
     */
    
    public IntegerProperty getNorthProperty() {
        return NORTH;
    }
    
    public IntegerProperty getEastProperty() {
        return EAST;
    }
    
    public IntegerProperty getSouthProperty() {
        return SOUTH;
    }
    
    public IntegerProperty getWestProperty() {
        return WEST;
    }
    
    public IntegerProperty getUpProperty() {
        return UP;
    }
    
    public IntegerProperty getDownProperty() {
        return DOWN;
    }
}
