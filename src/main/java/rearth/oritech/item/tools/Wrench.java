package rearth.oritech.item.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import rearth.oritech.client.cablesurfer.ClientCableFinder;
import rearth.oritech.client.cablesurfer.ClientZiplineHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.SoundContent;

import java.util.List;
import java.util.function.Consumer;

public class Wrench extends Item {

    public static int ACTION_COOLDOWN = 8;

    public Wrench(Properties settings) {
        super(settings);
    }

    public static Tool createToolComponent() {

        return new Tool(List.of(
                Tool.Rule.minesAndDrops(HolderSet.direct(
                        BlockContent.ENERGY_PIPE,
                        BlockContent.SUPERCONDUCTOR,
                        BlockContent.FLUID_PIPE,
                        BlockContent.ITEM_PIPE,
                        BlockContent.TRANSPARENT_ITEM_PIPE,
                        BlockContent.ENERGY_PIPE_CONNECTION,
                        BlockContent.SUPERCONDUCTOR_CONNECTION,
                        BlockContent.FLUID_PIPE_CONNECTION,
                        BlockContent.ITEM_PIPE_CONNECTION,
                        BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION,
                        BlockContent.ENERGY_PIPE_DUCT,
                        BlockContent.SUPERCONDUCTOR_DUCT,
                        BlockContent.FLUID_PIPE_DUCT,
                        BlockContent.ITEM_PIPE_DUCT,
                        BlockContent.FRAMED_ENERGY_PIPE,
                        BlockContent.FRAMED_SUPERCONDUCTOR,
                        BlockContent.FRAMED_FLUID_PIPE,
                        BlockContent.FRAMED_ITEM_PIPE,
                        BlockContent.FRAMED_ENERGY_PIPE_CONNECTION,
                        BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION,
                        BlockContent.FRAMED_FLUID_PIPE_CONNECTION,
                        BlockContent.FRAMED_ITEM_PIPE_CONNECTION,
                        BlockContent.MACHINE_FRAME
                ), 25f)
        ), 1.f, 1, true);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        var stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            var hit = ClientCableFinder.findLookedAtCable(player, 6f);
            if (hit != null) {
                ClientZiplineHandler.start(hit.selectedStart(), hit.selectedEnd(), hit.parallelStart(), hit.parallelEnd(), player.getSpeed() * 3);
                return InteractionResult.SUCCESS;
            }
        }

        return useWrench(stack, player, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /**
     * Attempts to use the wrench on a block
     *
     * @param item   The wrench item
     * @param player The player using the wrench
     */
    protected boolean useWrench(ItemStack item, Player player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(item)) return false;
        player.getCooldowns().addCooldown(item, ACTION_COOLDOWN);

        if (!(player instanceof ServerPlayer)) return false;

        var level = player.level();
        var result = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (result.getType() != HitResult.Type.BLOCK) return false;

        var blockPos = result.getBlockPos();
        var blockState = level.getBlockState(blockPos);
        if (blockState.getBlock() instanceof Wrenchable wrenchable) {
            // Wrench used on a wrenchable block
            var resultAction = wrenchable.onWrenchUse(blockState, level, blockPos, player, hand);
            if (resultAction == InteractionResult.SUCCESS) {
                onUsed(item, player, hand);
                return true;
            }
        } else {
            // Wrench used on block
            var direction = result.getDirection();
            var neighborPos = blockPos.relative(direction);
            var neighborState = level.getBlockState(neighborPos);

            // If the neighbor block is wrenchable, call the onWrenchUseNeighbor method
            if (neighborState.getBlock() instanceof Wrenchable wrenchable) {
                var resultAction = wrenchable.onWrenchUseNeighbor(neighborState, blockState, level, neighborPos, blockPos, direction, player, hand);
                if (resultAction == InteractionResult.SUCCESS) {
                    onUsed(item, player, hand);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        builder.accept(Component.translatable("tooltip.oritech.wrench"));
    }

    protected void onUsed(ItemStack item, Player player, InteractionHand hand) {
        playSound(player.level(), player);
    }

    protected void playSound(Level level, Player player) {
        level.playSound(null, player.blockPosition(), SoundContent.WRENCH_TURN.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Interface for blocks that be interacted with by a wrench
     */
    public interface Wrenchable {
        /**
         * Called when a wrench is used on the block
         *
         * @param state  the block state
         * @param level  the level
         * @param pos    the block position
         * @param player the player using the wrench
         * @return the result of the wrench use
         */
        InteractionResult onWrenchUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand);

        /**
         * Called when a wrench is used on a neighbor block
         *
         * @param state         The wrenchable block state
         * @param neighborState The neighbor block state
         * @param level         The level
         * @param pos           The wrenchable block position
         * @param neighborPos   The neighbor block position
         * @param neighborFace  The face of the neighbor block that was clicked
         * @param player        The player using the wrench
         * @param hand          The hand the wrench is being used in
         * @return the result of the wrench use
         */
        InteractionResult onWrenchUseNeighbor(BlockState state, BlockState neighborState, Level level, BlockPos pos, BlockPos neighborPos, Direction neighborFace, Player player, InteractionHand hand);
    }
}
