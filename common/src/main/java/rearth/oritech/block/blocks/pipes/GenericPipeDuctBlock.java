package rearth.oritech.block.blocks.pipes;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.item.tools.Wrench;

import java.util.List;

public abstract class GenericPipeDuctBlock extends AbstractPipeBlock implements Wrench.Wrenchable {

	public GenericPipeDuctBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getShape(BlockState state) {
		return VoxelShapes.fullCube();
	}

	@Override
	protected VoxelShape[] createShapes() {
		return new VoxelShape[0];
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (oldState.getBlock().equals(state.getBlock())) return;

		updateNeighbors(world, pos, true);
		// no states need to be added (see getPlacementState)
		GenericPipeInterfaceEntity.addNode(world, pos, false, state, getNetworkData(world));
	}

	@Override
	public void updateNeighbors(World world, BlockPos pos, boolean neighborToggled) {
		for (var direction : Direction.values()) {
			var neighborPos = pos.offset(direction);
			var neighborState = world.getBlockState(neighborPos);
			// Only update pipes
			if (neighborState.getBlock() instanceof AbstractPipeBlock pipeBlock) {
				var updatedState = pipeBlock.addConnectionStates(neighborState, world, neighborPos, neighborToggled);
				world.setBlockState(neighborPos, updatedState);

				// Update network data if the state was changed
				if (!neighborState.equals(updatedState)) {
					boolean interfaceBlock = updatedState.isOf(getConnectionBlock().getBlock());
					//if (neighborToggled)
						//GenericPipeInterfaceEntity.addNode(world, neighborPos, interfaceBlock, updatedState, getNetworkData(world));
				}
			}
		}
	}

	@Override
	public BlockState addConnectionStates(BlockState state, World world, BlockPos pos, boolean createConnection) {
		return state;
	}

	@Override
	public BlockState addConnectionStates(BlockState state, World world, BlockPos pos, Direction createDirection) {
		return state;
	}

	@Override
	public BlockState addStraightState(BlockState state) {
		return state;
	}

	@Override
	public boolean shouldConnect(BlockState current, Direction direction, BlockPos currentPos, World world, boolean createConnection) {
		return true;
	}

	@Override
	public boolean isConnectingInDirection(BlockState current, Direction direction, BlockPos currentPos, World world, boolean createConnection) {
		var neighborPos = currentPos.offset(direction);
		var neighborState = world.getBlockState(neighborPos);
		if (neighborState.isAir()) {
			return false;
		} else if (neighborState.getBlock() instanceof GenericPipeDuctBlock pipeBlock) {
			return true;
		} else if (neighborState.getBlock() instanceof AbstractPipeBlock pipeBlock) {
			return pipeBlock.isConnectingInDirection(neighborState, direction.getOpposite(), neighborPos, world, createConnection);
		}

		return true;
	}

	@Override
	public TriFunction<World, BlockPos, Direction, Boolean> apiValidationFunction() {
		return ((world, pos, direction) -> false);
	}

	@Override
	protected void onBlockRemoved(BlockPos pos, BlockState oldState, World world) {
		updateNeighbors(world, pos, false);
		GenericPipeInterfaceEntity.removeNode(world, pos, false, oldState, getNetworkData(world));
	}

	@Override
	public ActionResult onWrenchUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand) {
		if (player.isSneaking()) {
			world.breakBlock(pos, true, player);
			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}
	
	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
		super.appendTooltip(stack, context, tooltip, options);
		tooltip.add(Text.translatable("tooltip.oritech.pipe_duct_warning").formatted(Formatting.GRAY, Formatting.ITALIC));
	}
	
	@Override
	public ActionResult onWrenchUseNeighbor(BlockState state, BlockState neighborState, World world, BlockPos pos, BlockPos neighborPos, Direction neighborFace, PlayerEntity player, Hand hand) {
		return ActionResult.PASS;
	}
}
