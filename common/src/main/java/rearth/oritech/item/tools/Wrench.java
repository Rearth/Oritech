package rearth.oritech.item.tools;

import net.minecraft.block.BlockState;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.SoundContent;

import java.util.List;

public class Wrench extends Item {

	public static int ACTION_COOLDOWN = 8;

	public Wrench(Settings settings) {
		super(settings);
	}

	public static ToolComponent createToolComponent() {
		return new ToolComponent(List.of(ToolComponent.Rule.ofAlwaysDropping(List.of(
				BlockContent.ENERGY_PIPE,
				BlockContent.SUPERCONDUCTOR,
				BlockContent.FLUID_PIPE,
				BlockContent.ITEM_PIPE,
				BlockContent.ENERGY_PIPE_CONNECTION,
				BlockContent.SUPERCONDUCTOR_CONNECTION,
				BlockContent.FLUID_PIPE_CONNECTION,
				BlockContent.ITEM_PIPE_CONNECTION
		), 25f)), 1.f, 1);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		var stack = user.getStackInHand(hand);
		return useWrench(stack, user, hand) ? TypedActionResult.success(stack) : TypedActionResult.fail(stack);
	}

	/**
	 * Attempts to use the wrench on a block
	 *
	 * @param item   The wrench item
	 * @param player The player using the wrench
	 */
	protected boolean useWrench(ItemStack item, PlayerEntity player, Hand hand) {
		if (player.getItemCooldownManager().isCoolingDown(this)) return false;
		player.getItemCooldownManager().set(this, ACTION_COOLDOWN);

		if (!(player instanceof ServerPlayerEntity)) return false;

		var world = player.getWorld();
		var result = raycast(world, player, RaycastContext.FluidHandling.NONE);
		if (result.getType() != HitResult.Type.BLOCK) return false;

		var blockPos = result.getBlockPos();
		var blockState = world.getBlockState(blockPos);
		if (blockState.getBlock() instanceof Wrenchable wrenchable) {
			// Wrench used on a wrenchable block
			var resultAction = wrenchable.onWrenchUse(blockState, world, blockPos, player, hand);
			if (resultAction == ActionResult.SUCCESS) {
				onUsed(item, player, hand);
				return true;
			}
		} else {
			// Wrench used on block
			var direction = result.getSide();
			var neighborPos = blockPos.offset(direction);
			var neighborState = world.getBlockState(neighborPos);

			// If the neighbor block is wrenchable, call the onWrenchUseNeighbor method
			if (neighborState.getBlock() instanceof Wrenchable wrenchable) {
				var resultAction = wrenchable.onWrenchUseNeighbor(neighborState, blockState, world, neighborPos, blockPos, direction, player, hand);
				if (resultAction == ActionResult.SUCCESS) {
					onUsed(item, player, hand);
					return true;
				}
			}
		}

		return false;
	}

	protected void onUsed(ItemStack item, PlayerEntity player, Hand hand) {
		playSound(player.getWorld(), player);
		item.damage(1, player, LivingEntity.getSlotForHand(hand));
	}

	protected void playSound(World world, PlayerEntity player) {
		world.playSound(null, player.getBlockPos(), SoundContent.WRENCH_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	/**
	 * Interface for blocks that be interacted with by a wrench
	 */
	public interface Wrenchable {
		/**
		 * Called when a wrench is used on the block
		 *
		 * @param state  the block state
		 * @param world  the world
		 * @param pos    the block position
		 * @param player the player using the wrench
		 * @return the result of the wrench use
		 */
		ActionResult onWrenchUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand);

		/**
		 * Called when a wrench is used on a neighbor block
		 *
		 * @param state         The wrenchable block state
		 * @param neighborState The neighbor block state
		 * @param world         The world
		 * @param pos           The wrenchable block position
		 * @param neighborPos   The neighbor block position
		 * @param neighborFace  The face of the neighbor block that was clicked
		 * @param player        The player using the wrench
		 * @param hand          The hand the wrench is being used in
		 * @return the result of the wrench use
		 */
		ActionResult onWrenchUseNeighbor(BlockState state, BlockState neighborState, World world, BlockPos pos, BlockPos neighborPos, Direction neighborFace, PlayerEntity player, Hand hand);
	}
}
