package rearth.oritech.block.blocks.pipes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.item.tools.Wrench;

import java.util.List;

public abstract class GenericPipeDuctBlock extends AbstractPipeBlock implements Wrench.Wrenchable {

	public GenericPipeDuctBlock(Properties settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getShape(BlockState state) {
		return Shapes.block();
	}

	@Override
	protected VoxelShape[] createShapes() {
		return new VoxelShape[0];
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
		if (oldState.getBlock().equals(state.getBlock())) return;

		updateNeighbors(world, pos, true);
		// no states need to be added (see getPlacementState)
		GenericPipeInterfaceEntity.addNode(world, pos, false, state, getNetworkData(world));
	}

	@Override
	public void updateNeighbors(Level world, BlockPos pos, boolean neighborToggled) {
		for (var direction : Direction.values()) {
			var neighborPos = pos.relative(direction);
			var neighborState = world.getBlockState(neighborPos);
			// Only update pipes
			if (neighborState.getBlock() instanceof AbstractPipeBlock pipeBlock) {
				var updatedState = pipeBlock.addConnectionStates(neighborState, world, neighborPos, neighborToggled);
				world.setBlockAndUpdate(neighborPos, updatedState);

				// Update network data if the state was changed
				if (!neighborState.equals(updatedState)) {
					boolean interfaceBlock = updatedState.is(getConnectionBlock().getBlock());
					//if (neighborToggled)
						//GenericPipeInterfaceEntity.addNode(world, neighborPos, interfaceBlock, updatedState, getNetworkData(world));
				}
			}
		}
	}

	@Override
	public BlockState addConnectionStates(BlockState state, Level world, BlockPos pos, boolean createConnection) {
		return state;
	}

	@Override
	public BlockState addConnectionStates(BlockState state, Level world, BlockPos pos, Direction createDirection) {
		return state;
	}

	@Override
	public BlockState addStraightState(BlockState state) {
		return state;
	}

	@Override
	public boolean shouldConnect(BlockState current, Direction direction, BlockPos currentPos, Level world, boolean createConnection) {
		return true;
	}

	@Override
	public boolean isConnectingInDirection(BlockState current, Direction direction, BlockPos currentPos, Level world, boolean createConnection) {
		var neighborPos = currentPos.relative(direction);
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
	public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
		return ((world, pos, direction) -> false);
	}

	@Override
	protected void onBlockRemoved(BlockPos pos, BlockState oldState, Level world) {
		updateNeighbors(world, pos, false);
		GenericPipeInterfaceEntity.removeNode(world, pos, false, oldState, getNetworkData(world));
	}

	@Override
	public InteractionResult onWrenchUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			world.destroyBlock(pos, true, player);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
		super.appendHoverText(stack, context, tooltip, options);
		tooltip.add(Component.translatable("tooltip.oritech.pipe_duct_warning").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
	
	@Override
	public InteractionResult onWrenchUseNeighbor(BlockState state, BlockState neighborState, Level world, BlockPos pos, BlockPos neighborPos, Direction neighborFace, Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}
}
