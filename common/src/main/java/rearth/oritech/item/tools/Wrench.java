package rearth.oritech.item.tools;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Direction;
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
		if (!world.isClient && user.isSneaking()) {
			rotateWrenchDirection(stack, user);
			world.playSound(null, user.getBlockPos(), SoundContent.WRENCH_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return new TypedActionResult<>(ActionResult.SUCCESS_NO_ITEM_USED, stack);
		}

		return super.use(world, user, hand);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		var world = context.getWorld();
		var player = context.getPlayer();

		if (world.isClient || player == null || player.isSneaking()) return super.useOnBlock(context);

		var blockState = world.getBlockState(context.getBlockPos());
		if (blockState.getBlock() instanceof Wrenchable) {
			var stack = context.getStack();
			world.playSound(null, player.getBlockPos(), SoundContent.WRENCH_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);

			var result = ((Wrenchable) blockState.getBlock()).onWrenchUse(blockState, context, stack);
			if (result == ActionResult.SUCCESS) {
				stack.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
				return ActionResult.success(true);
			}
		}

		return super.useOnBlock(context);
	}

	/**
	 * Rotate one forward in the 6 wrench directions
	 *
	 * @param item   The wrench item
	 * @param player The player using the wrench
	 */
	protected void rotateWrenchDirection(ItemStack item, PlayerEntity player) {
		// Rotate the wrench mode
		byte mode = item.getOrDefault(ComponentContent.WRENCH_DIRECTION.get(), NORTH);
		byte newMode = (byte) ((mode + 1) % 6);
		item.set(ComponentContent.WRENCH_DIRECTION.get(), newMode);

		// Send a message to the player
		player.sendMessage(Text.translatable("tooltip.oritech.wrench.direction_changed", Text.translatable("text.oritech.parameter." + Direction.byId(newMode).getName())), true);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);

		var showFull = Screen.hasControlDown();
		if (!showFull) {
			tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
		} else {
			tooltip.add(Text.translatable("tooltip.oritech.wrench.description").formatted(Formatting.ITALIC, Formatting.GRAY));
			tooltip.add(Text.translatable("tooltip.oritech.wrench.current_direction", Text.translatable("text.oritech.parameter." + getDirection(stack).getName())).formatted(Formatting.GRAY));
		}
	}

	/**
	 * Interface for blocks that be interacted with by a wrench
	 */
	public interface Wrenchable {
		/**
		 * Called when a wrench is used on the block
		 *
		 * @param state   The block state
		 * @param context The usage context
		 * @param stack   The wrench item stack
		 * @return The result of the wrench use
		 */
		ActionResult onWrenchUse(BlockState state, ItemUsageContext context, ItemStack stack);
	}
}
