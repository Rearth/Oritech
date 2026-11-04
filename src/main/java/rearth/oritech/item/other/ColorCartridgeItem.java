package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.util.ColorableMachine;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.util.CollisionShapes;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.PaintableBlock;
import rearth.oritech.client.init.ParticleContent;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

public class ColorCartridgeItem extends Item {

    public final ColorableMachine.ColorVariant variant;

    public ColorCartridgeItem(Properties properties, ColorableMachine.ColorVariant variant) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {

        builder.accept(Component.translatable("tooltip.oritech.paint.1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("tooltip.oritech.paint.2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
    }
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {

        var targetBlock = CollisionShapes.getControllerPos(context.getLevel().getBlockState(context.getClickedPos()), context.getLevel(), context.getClickedPos());
        var targetEntity = context.getLevel().getBlockEntity(targetBlock);

        var targetState = context.getLevel().getBlockState(targetBlock);
        var colorableMachine = targetEntity instanceof ColorableMachine machine ? machine : null;

        var frame = targetEntity instanceof FrameInteractionBlockEntity gantry ? gantry.getConnectedFrame() : Set.<BlockPos>of();
        var frameNeedsColor = frame.stream().anyMatch(pos -> context.getLevel().getBlockState(pos).getValue(PaintableBlock.COLOR) != variant);

        if (colorableMachine != null) {
            if (colorableMachine.getCurrentColor() == variant) return InteractionResult.PASS;
        } else if (!targetState.hasProperty(PaintableBlock.COLOR) || (targetState.getValue(PaintableBlock.COLOR) == variant && !frameNeedsColor)) {
            return InteractionResult.PASS;
        }

        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        if (colorableMachine != null) {
            colorableMachine.assignColor(variant);
        } else {
            context.getLevel().setBlockAndUpdate(targetBlock, targetState.setValue(PaintableBlock.COLOR, variant));
        }

        for (var pos : frame) {
            var state = context.getLevel().getBlockState(pos);
            context.getLevel().setBlockAndUpdate(pos, state.setValue(PaintableBlock.COLOR, variant));
        }

        var stack = context.getItemInHand();
        stack.shrink(1);

        if (context.getPlayer() != null)
            context.getPlayer().setItemInHand(context.getHand(), stack);

        context.getLevel().playSound(null, targetBlock, SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 1f, 0.6f);

        // create particles
        var targetBlocks = new ArrayList<BlockPos>();
        targetBlocks.add(targetBlock);
        targetBlocks.addAll(frame);

        if (targetEntity instanceof MultiblockMachineController multiblockMachineController) {
            targetBlocks.addAll(multiblockMachineController.getConnectedCores());
        }

        var level = context.getLevel();
        if (context.getLevel() instanceof ServerLevel serverLevel) {
            for (var pos : targetBlocks) {
                var at = pos.getCenter().add(level.getRandom().nextFloat() * 0.1, level.getRandom().nextFloat() * 0.1, level.getRandom().nextFloat() * 0.1);
                ParticleContent.sendParticles(serverLevel, ParticleTypes.GUST, at.x, at.y, at.z, 1, level.getRandom().nextFloat(), level.getRandom().nextFloat(), level.getRandom().nextFloat(), 0.15f);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
