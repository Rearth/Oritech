package rearth.oritech.block.blocks.pipes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.block.entity.pipes.ExtractablePipeInterfaceEntity;
import rearth.oritech.init.ItemContent;

import java.util.HashSet;

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
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (player.isHolding(ItemContent.WRENCH)) return ActionResult.PASS;
		if (world.isClient) return ActionResult.SUCCESS;

		var interactDir = getInteractDirection(state, pos, player);
		if (!hasMachineInDirection(interactDir, world, pos, apiValidationFunction()))
			return ActionResult.PASS;

		var property = directionToProperty(interactDir);
		var connection = state.get(property);
		world.setBlockState(pos, state.with(property, connection != EXTRACT ? EXTRACT : CONNECTION), Block.FORCE_STATE, 0);

		// Invalidate cache
		invalidateTargetCache(world, pos);

		return ActionResult.SUCCESS;
	}

	/**
	 * Invalidates the target cache of the block entity at the given position
	 *
	 * @param world the world
	 * @param pos   the position
	 */
	protected void invalidateTargetCache(World world, BlockPos pos) {
		var data = getNetworkData(world);
		var network = data.pipeNetworkLinks.getOrDefault(pos, null);
		if (network != null) {
			var checked = new HashSet<BlockPos>();

			// Invalidate all pipe connection nodes in the network
			for (var pipeInterface : data.pipeNetworkInterfaces.get(network)) {
				// Skip node if already checked (node has multiple interface connections)
				var pipePos = pipeInterface.getLeft().offset(pipeInterface.getRight());
				if (checked.contains(pipePos)) continue;

				checked.add(pipePos);
				var pipeEntity = world.getBlockEntity(pipePos);
				if (pipeEntity instanceof ExtractablePipeInterfaceEntity)
					((ExtractablePipeInterfaceEntity) pipeEntity).invalidateTargetCache();
			}
		}
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

	@Override
	public BlockState addConnectionStates(BlockState state, World world, BlockPos pos, Direction createDirection) {
		for (var direction : Direction.values()) {
			var property = directionToProperty(direction);
			var connection = shouldConnect(state, direction, pos, world, direction.equals(createDirection));
			var newValue = connection ? isSideExtractable(state, direction) ? EXTRACT : CONNECTION : NO_CONNECTION;
			state = state.with(property, newValue);
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
