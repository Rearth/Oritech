package rearth.oritech.item.tools;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.SoundContent;

import java.util.List;

public class Wrench extends Item {

	public static byte DOWN = 0;
	public static byte UP = 1;
	public static byte NORTH = 2;
	public static byte SOUTH = 3;
	public static byte WEST = 4;
	public static byte EAST = 5;

	public static int ACTION_COOLDOWN = 8;

	public Wrench(Settings settings) {
		super(settings);
	}

	public static Direction getDirection(ItemStack stack) {
		byte mode = stack.getOrDefault(ComponentContent.WRENCH_DIRECTION.get(), NORTH);
		return Direction.byId(mode);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		var stack = user.getStackInHand(hand);
		useWrench(stack, user, hand, true);
		return super.use(world, user, hand);
	}

	@Override
	public float getMiningSpeed(ItemStack stack, BlockState state) {
		return 100.f;
	}

	@Override
	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
		if (miner.getItemCooldownManager().isCoolingDown(this)) return false;

		var itemStack = miner.getMainHandStack();
		if (itemStack.isOf(this)) {
			useWrench(itemStack, miner, Hand.MAIN_HAND, false);
			return false;
		}

		itemStack = miner.getOffHandStack();
		if (itemStack.isOf(this)) {
			useWrench(itemStack, miner, Hand.OFF_HAND, false);
			return false;
		}

		return false;
	}

	/**
	 * Rotate one forward in the 6 wrench directions
	 *
	 * @param item   The wrench item
	 * @param player The player using the wrench
	 */
	protected void useWrench(ItemStack item, PlayerEntity player, Hand hand, boolean updateBlock) {
		if (player.getItemCooldownManager().isCoolingDown(this)) return;

		player.getItemCooldownManager().set(this, ACTION_COOLDOWN);

		if (player instanceof ServerPlayerEntity) {
			// Wrench used on wrenchable block
			var world = player.getWorld();
			var result = raycast(world, player, RaycastContext.FluidHandling.NONE);
			if (updateBlock && result.getType() == HitResult.Type.BLOCK) {
				var blockPos = result.getBlockPos();
				var blockState = world.getBlockState(blockPos);
				if (blockState.getBlock() instanceof Wrenchable wrenchable) {
					var resultAction = wrenchable.onWrenchUse(blockState, world, blockPos, player, hand, getDirection(item));
					if (resultAction == ActionResult.SUCCESS) {
						playSound(world, player);
						item.damage(1, player, LivingEntity.getSlotForHand(hand));
						return;
					}
				}
			}

			// Rotate the wrench mode
			byte mode = item.getOrDefault(ComponentContent.WRENCH_DIRECTION.get(), NORTH);
			byte newMode = (byte) ((mode + (player.isSneaking() ? -1 : 1)) % 6);
			item.set(ComponentContent.WRENCH_DIRECTION.get(), newMode);

			// Send a message to the player
			player.sendMessage(Text.translatable("tooltip.oritech.wrench.direction_changed", Text.translatable("text.oritech.parameter." + Direction.byId(newMode).getName())), true);
		}

		playSound(player.getWorld(), player);
	}

	protected void playSound(World world, PlayerEntity player) {
		world.playSound(player, player.getBlockPos(), SoundContent.WRENCH_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(Text.translatable("tooltip.oritech.wrench.current_direction", Text.translatable("text.oritech.parameter." + getDirection(stack).getName())).formatted(Formatting.GRAY));
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, world, entity, slot, selected);
	}

	/**
	 * Interface for blocks that be interacted with by a wrench
	 */
	public interface Wrenchable {
		/**
		 * Called when a wrench is used on the block
		 * @param state the block state
		 * @param world the world
		 * @param pos the block position
		 * @param player the player using the wrench
		 * @param wrenchDirection the direction the wrench is facing
		 * @return the result of the wrench use
		 */
		ActionResult onWrenchUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, Direction wrenchDirection);
	}
}
