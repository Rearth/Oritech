package rearth.oritech.block.blocks.pipes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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

import java.util.ArrayList;
import java.util.List;

public abstract class GenericPipeBlock extends Block implements Wrench.Wrenchable {

	// 0 = no connection, 1 = connection (pipe->pipe or pipe->machine)
	public static int NO_CONNECTION = 0;
	public static int CONNECTION = 1;

	public static final IntProperty NORTH = IntProperty.of("north", 0, 1);
	public static final IntProperty EAST = IntProperty.of("east", 0, 1);
	public static final IntProperty SOUTH = IntProperty.of("south", 0, 1);
	public static final IntProperty WEST = IntProperty.of("west", 0, 1);
	public static final IntProperty UP = IntProperty.of("up", 0, 1);
	public static final IntProperty DOWN = IntProperty.of("down", 0, 1);
    public static final BooleanProperty STRAIGHT = BooleanProperty.of("straight");

    private static final Boolean USE_ACCURATE_OUTLINES = Oritech.CONFIG.tightCableHitboxes();
	protected VoxelShape[] boundingShapes;
    
    public GenericPipeBlock(Settings settings) {
        super(settings);
		this.setDefaultState(getDefaultState()
				.with(getNorthProperty(), 0)
				.with(getEastProperty(), 0)
				.with(getSouthProperty(), 0)
				.with(getWestProperty(), 0)
				.with(getUpProperty(), 0)
				.with(getDownProperty(), 0)
				.with(STRAIGHT, false));
		boundingShapes = createShapes();
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(getNorthProperty(), getEastProperty(), getSouthProperty(), getWestProperty(), getUpProperty(), getDownProperty(), STRAIGHT);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    private VoxelShape getShape(BlockState state) {
        var shape = boundingShapes[0];

		if (state.get(getNorthProperty()) != NO_CONNECTION)
            shape = VoxelShapes.union(shape, boundingShapes[1]);
		if (state.get(getEastProperty()) != NO_CONNECTION)
            shape = VoxelShapes.union(shape, boundingShapes[2]);
		if (state.get(getSouthProperty()) != NO_CONNECTION)
            shape = VoxelShapes.union(shape, boundingShapes[3]);
		if (state.get(getWestProperty()) != NO_CONNECTION)
            shape = VoxelShapes.union(shape, boundingShapes[4]);
		if (state.get(getUpProperty()) != NO_CONNECTION)
            shape = VoxelShapes.union(shape, boundingShapes[5]);
		if (state.get(getDownProperty()) != NO_CONNECTION)
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

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.getBlock().equals(state.getBlock())) return;
		else if (oldState.isOf(getConnectionBlock().getBlock())) {
			GenericPipeInterfaceEntity.addNode(world, pos, false, state, getNetworkData(world));
			return;
		}

        // transform to interface block on placement when machine is neighbor
		if (hasNeighboringMachine(state, world, pos, true)) {
			var connectionBlock = getConnectionBlock();
			var interfaceState = ((GenericPipeBlock) connectionBlock.getBlock()).addConnectionStates(connectionBlock, world, pos, true);
            world.setBlockState(pos, interfaceState);
        } else {
			// no states need to be added (see getPlacementState)
            GenericPipeInterfaceEntity.addNode(world, pos, false, state, getNetworkData(world));
        }

		updateNeighbors(world, pos, false);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		var baseState = super.getPlacementState(ctx);
		return addConnectionStates(baseState, ctx.getWorld(), ctx.getBlockPos(), true);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess worldAccess, BlockPos pos, BlockPos neighborPos) {
		var world = (World) worldAccess;
		if (world.isClient) return state;

		// transform to interface when machine is placed as neighbor
		if (hasMachineInDirection(direction, world, pos, apiValidationFunction())) {
			var connectionBlock = getConnectionBlock();
			return ((GenericPipeBlock) connectionBlock.getBlock()).addConnectionStates(connectionBlock, world, pos, direction);
		}

		return state;
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		super.onStateReplaced(state, world, pos, newState, moved);

		if (!state.isOf(newState.getBlock()) && !(newState.getBlock() instanceof GenericPipeBlock)) {
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
	public void updateNeighbors(World world, BlockPos pos, boolean neighborToggled) {
		for (var direction : Direction.values()) {
			var neighborPos = pos.offset(direction);
			var neighborState = world.getBlockState(neighborPos);
			// Only update pipes
			if (neighborState.getBlock() instanceof GenericPipeBlock pipeBlock) {
				var updatedState = pipeBlock.addConnectionStates(neighborState, world, neighborPos, false);
				world.setBlockState(neighborPos, updatedState);

				// Update network data if the state was changed
				if (!neighborState.equals(updatedState)) {
					boolean interfaceBlock = updatedState.isOf(getConnectionBlock().getBlock());
					if (neighborToggled)
						GenericPipeInterfaceEntity.addNode(world, neighborPos, interfaceBlock, updatedState, getNetworkData(world));
				}
			}
		}
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!player.isCreative() && !world.isClient) {
			onBlockRemoved(pos, state, world);
		}
		return super.onBreak(world, pos, state, player);
	}

	@Override
	public ActionResult onWrenchUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand) {
		if (player.isSneaking()) {
			world.breakBlock(pos, true, player);
			return ActionResult.SUCCESS;
		}

		return !toggleSideConnection(state, getInteractDirection(state, pos, player), world, pos) ? ActionResult.FAIL : ActionResult.SUCCESS;
	}

	@Override
	public ActionResult onWrenchUseNeighbor(BlockState state, BlockState neighborState, World world, BlockPos pos, BlockPos neighborPos, Direction neighborFace, PlayerEntity player, Hand hand) {
		return toggleSideConnection(state, neighborFace.getOpposite(), world, pos) ? ActionResult.SUCCESS : ActionResult.FAIL;
	}

	protected Direction getInteractDirection(BlockState state, BlockPos pos, PlayerEntity player) {
		var shapes = getActiveShapes(state);
		var start = player.getCameraPosVec(0f);
		var end = start.add(player.getRotationVec(0).multiply(5));

		var targetShape = shapes.getFirst();
		var distance = Double.MAX_VALUE;
		var hitPos = Vec3d.ZERO;
		for (var shape : shapes) {
			var hitResult = shape.raycast(start, end, pos);
			if (hitResult == null) continue;

			var shapeDistance = hitResult.getPos().distanceTo(start);
			if (shapeDistance < distance) {
				distance = shapeDistance;
				targetShape = shape;
				hitPos = hitResult.getPos();
			}
		}

		var center = targetShape.getBoundingBox().getCenter();
		var diff = center.subtract(shapes.getFirst().getBoundingBox().getCenter());
		if (diff.equals(Vec3d.ZERO))
			// center hit
			diff = hitPos.subtract(center.add(Vec3d.of(pos)));

		return Direction.getFacing(diff.x, diff.y, diff.z);
	}

	private List<VoxelShape> getActiveShapes(BlockState state) {
		var shapes = new ArrayList<VoxelShape>();
		shapes.add(boundingShapes[0]);
		if (state.get(getNorthProperty()) != NO_CONNECTION)
			shapes.add(boundingShapes[1]);
		if (state.get(getEastProperty()) != NO_CONNECTION)
			shapes.add(boundingShapes[2]);
		if (state.get(getSouthProperty()) != NO_CONNECTION)
			shapes.add(boundingShapes[3]);
		if (state.get(getWestProperty()) != NO_CONNECTION)
			shapes.add(boundingShapes[4]);
		if (state.get(getUpProperty()) != NO_CONNECTION)
			shapes.add(boundingShapes[5]);
		if (state.get(getDownProperty()) != NO_CONNECTION)
			shapes.add(boundingShapes[6]);
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
		if (!newState.isOf(getConnectionBlock().getBlock()) && createConnection && hasMachineInDirection(side, world, pos, apiValidationFunction())) {
			var connectionState = getConnectionBlock();
			var interfaceState = ((GenericPipeBlock) connectionState.getBlock()).addConnectionStates(connectionState, world, pos, side);
            world.setBlockState(pos, interfaceState);
        } else {
            world.setBlockState(pos, newState);
            GenericPipeInterfaceEntity.addNode(world, pos, false, newState, getNetworkData(world));

			// update neighbor if it's a pipe
			updateNeighbors(world, pos, true);
		}

		// play sound
        var soundGroup = getSoundGroup(state);
        world.playSound(null, pos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS, soundGroup.getVolume() * .5f, soundGroup.getPitch());

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
	public BlockState addConnectionStates(BlockState state, World world, BlockPos pos, boolean createConnection) {
		for (var direction : Direction.values()) {
			var property = directionToProperty(direction);
			var connection = shouldConnect(state, direction, pos, world, createConnection);
			state = state.with(property, connection ? CONNECTION : NO_CONNECTION);
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
	public BlockState addConnectionStates(BlockState state, World world, BlockPos pos, Direction createDirection) {
		for (var direction : Direction.values()) {
			var property = directionToProperty(direction);
			var connection = shouldConnect(state, direction, pos, world, direction.equals(createDirection));
			state = state.with(property, connection ? CONNECTION : NO_CONNECTION);
		}
		return addStraightState(state);
	}

	/**
	 * Adds the straight property to the pipe block-state.
	 *
	 * @param state The current pipe block-state
	 * @return The updated block-state
	 */
	public BlockState addStraightState(BlockState state) {
		var north = state.get(getNorthProperty()) != NO_CONNECTION;
		var south = state.get(getSouthProperty()) != NO_CONNECTION;
		var east = state.get(getEastProperty()) != NO_CONNECTION;
		var west = state.get(getWestProperty()) != NO_CONNECTION;
		var up = state.get(getUpProperty()) != NO_CONNECTION;
		var down = state.get(getDownProperty()) != NO_CONNECTION;

		// Check for straight connections along each axis
		boolean straightX = north && south && !east && !west && !up && !down;
		boolean straightY = up && down && !north && !south && !east && !west;
		boolean straightZ = east && west && !north && !south && !up && !down;

		// The pipe is straight if exactly one of the axes has a straight connection
		var straight = straightX || straightY || straightZ;

		return state.with(STRAIGHT, straight);
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
	public boolean shouldConnect(BlockState current, Direction direction, BlockPos currentPos, World world, boolean createConnection) {
		var targetPos = currentPos.offset(direction);
		var targetState = world.getBlockState(targetPos);

		// If creating a connection we don't check the other pipe's connection state, force the connection
		// Otherwise we check if the other pipe is connecting in the opposite direction
		if (createConnection) {
			return isValidConnectionTarget(targetState.getBlock(), world, direction.getOpposite(), targetPos);
		} else if (targetState.getBlock() instanceof GenericPipeBlock pipeBlock) {
			return pipeBlock.isConnectingInDirection(targetState, direction.getOpposite(), false);
		} else
			return isConnectingInDirection(current, direction, false) && isValidInterfaceTarget(targetState.getBlock(), world, direction.getOpposite(), targetPos);
	}

	/**
	 * Check if the pipe is connecting in a specific direction.
	 *
	 * @param state            The target pipe block-state
	 * @param direction        The direction to check
	 * @param createConnection Whether to create a connection
	 * @return Boolean whether the pipe is connecting
	 */
	public boolean isConnectingInDirection(BlockState state, Direction direction, boolean createConnection) {
		var block = state.getBlock();
		if (!(block instanceof GenericPipeBlock pipeBlock)) return false;
		var property = pipeBlock.directionToProperty(direction);
		return state.get(property) >= CONNECTION || createConnection && state.get(property) == NO_CONNECTION;
	}

	/**
	 * Check if the pipe node has a neighboring machine.
	 *
	 * @param state The target pipe block-state
	 * @param world The target world
	 * @param pos   The target pipe position
	 * @return Boolean whether a machine is connected
	 */
	public boolean hasNeighboringMachine(BlockState state, World world, BlockPos pos, boolean createConnection) {
		var lookup = apiValidationFunction();
		return (isConnectingInDirection(state, Direction.NORTH, createConnection) && hasMachineInDirection(Direction.NORTH, world, pos, lookup))
				|| (isConnectingInDirection(state, Direction.EAST, createConnection) && hasMachineInDirection(Direction.EAST, world, pos, lookup))
				|| (isConnectingInDirection(state, Direction.SOUTH, createConnection) && hasMachineInDirection(Direction.SOUTH, world, pos, lookup))
				|| (isConnectingInDirection(state, Direction.WEST, createConnection) && hasMachineInDirection(Direction.WEST, world, pos, lookup))
				|| (isConnectingInDirection(state, Direction.UP, createConnection) && hasMachineInDirection(Direction.UP, world, pos, lookup))
				|| (isConnectingInDirection(state, Direction.DOWN, createConnection) && hasMachineInDirection(Direction.DOWN, world, pos, lookup));
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
	public boolean hasMachineInDirection(Direction direction, World world, BlockPos ownPos, TriFunction<World, BlockPos, Direction, Boolean> lookup) {
		var neighborPos = ownPos.add(direction.getVector());
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
	public boolean isValidConnectionTarget(Block target, World world, Direction direction, BlockPos pos) {
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
	public boolean isValidInterfaceTarget(Block target, World world, Direction direction, BlockPos pos) {
		var lookupFunction = apiValidationFunction();
		return (lookupFunction.apply(world, pos, direction) && isCompatibleTarget(target));
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
			return state.get(getNorthProperty());
		else if (direction == Direction.EAST)
			return state.get(getEastProperty());
		else if (direction == Direction.SOUTH)
			return state.get(getSouthProperty());
		else if (direction == Direction.WEST)
			return state.get(getWestProperty());
		else if (direction == Direction.UP)
			return state.get(getUpProperty());
		else return state.get(getDownProperty());
	}

	/**
	 * Converts a {@link Direction} into a {@link IntProperty} for a connection
	 *
	 * @param direction Respective direction
	 * @return the property
	 */
	public IntProperty directionToProperty(Direction direction) {
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
	public abstract TriFunction<World, BlockPos, Direction, Boolean> apiValidationFunction();

	public abstract BlockState getConnectionBlock();

	public abstract BlockState getNormalBlock();

	public abstract String getPipeTypeName();

	public abstract boolean connectToOwnBlockType(Block block);

	public abstract GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world);

	protected int getNextConnectionState(BlockState state, Direction side, World world, BlockPos pos, int current) {
		return current == NO_CONNECTION ? CONNECTION : NO_CONNECTION;
	}

	protected void onBlockRemoved(BlockPos pos, BlockState oldState, World world) {
		updateNeighbors(world, pos, false);
		GenericPipeInterfaceEntity.removeNode(world, pos, false, oldState, getNetworkData(world));
	}

	/*
	 * The following is a hacky implementation to allow child classes to modify the connection properties
	 */

	public IntProperty getNorthProperty() {
		return NORTH;
	}

	public IntProperty getEastProperty() {
		return EAST;
	}

	public IntProperty getSouthProperty() {
		return SOUTH;
	}

	public IntProperty getWestProperty() {
		return WEST;
	}

	public IntProperty getUpProperty() {
		return UP;
	}

	public IntProperty getDownProperty() {
		return DOWN;
	}
}
