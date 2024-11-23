package rearth.oritech.block.blocks.pipes;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public abstract class ExtractablePipeConnectionBlock extends GenericPipeConnectionBlock {

	public static final int EXTRACT = 2;

	// 0 = no connection, 1 = normal connection, 2 = extractable connection
	public static final IntProperty NORTH = IntProperty.of("north", 0, 2);
	public static final IntProperty EAST = IntProperty.of("east", 0, 2);
	public static final IntProperty SOUTH = IntProperty.of("south", 0, 2);
	public static final IntProperty WEST = IntProperty.of("west", 0, 2);
	public static final IntProperty UP = IntProperty.of("up", 0, 2);
	public static final IntProperty DOWN = IntProperty.of("down", 0, 2);

	public ExtractablePipeConnectionBlock(Settings settings) {
		super(settings);
	}

	@Override
	public BlockState addConnectionStates(BlockState state, World world, BlockPos pos, boolean createConnection) {
		for (var direction : Direction.values()) {
			var property = directionToProperty(direction);
			var connection = shouldConnect(state, direction, pos, world, createConnection);

			if (connection && state.get(property) == EXTRACT) continue; // don't override extractable connections
			state = state.with(property, connection ? CONNECTION : NO_CONNECTION);
		}

		return addStraightState(state);
	}

	/**
	 * Checks if the block state is extractable from any side
	 *
	 * @param state the block state
	 * @return true if the block state is extractable from any side
	 */
	public boolean isExtractable(BlockState state) {
		for (Direction side : Direction.values()) {
			if (isSideExtractable(state, side))
				return true;
		}

		return false;
	}

	/**
	 * Checks if the block state is extractable from a specific side
	 *
	 * @param state the block state
	 * @param side  the side to check
	 * @return true if the block state is extractable from the side
	 */
	public boolean isSideExtractable(BlockState state, Direction side) {
		return directionToPropertyValue(state, side) == EXTRACT;
	}

	@Override
	protected int getNextConnectionState(BlockState state, Direction side, World world, BlockPos pos, int current) {
		// Insert extract value if there is a machine in the direction
		if (current == CONNECTION && hasMachineInDirection(side, world, pos, apiValidationFunction())) {
			return EXTRACT;
		}

		return super.getNextConnectionState(state, side, world, pos, current);
	}

	@Override
	public IntProperty getNorthProperty() {
		return NORTH;
	}

	@Override
	public IntProperty getEastProperty() {
		return EAST;
	}

	@Override
	public IntProperty getSouthProperty() {
		return SOUTH;
	}

	@Override
	public IntProperty getWestProperty() {
		return WEST;
	}

	@Override
	public IntProperty getUpProperty() {
		return UP;
	}

	@Override
	public IntProperty getDownProperty() {
		return DOWN;
	}
}
