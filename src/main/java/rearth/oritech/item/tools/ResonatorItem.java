package rearth.oritech.item.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.ResonatorSearch;

import java.util.function.Consumer;

public class ResonatorItem extends Item {
    private final TagKey<Block> targets;
    private final String translationKey;
    private final int particleColor;

    public ResonatorItem(Properties properties, TagKey<Block> targets, String translationKey, int particleColor) {
        super(properties);
        this.targets = targets;
        this.translationKey = translationKey;
        this.particleColor = particleColor;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            var stack = player.getItemInHand(hand);
            var lastResult = stack.get(ComponentContent.TARGET_GLOBAL_POSITION.get());
            if (!player.isShiftKeyDown() && lastResult != null) {
                ResonatorSearch.pointTo(serverPlayer, lastResult, translationKey, particleColor);
                player.getCooldowns().addCooldown(stack, 10);
            } else if (ResonatorSearch.start(serverPlayer, stack, targets, translationKey, particleColor)) {
                player.getCooldowns().addCooldown(stack, 100);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.oritech." + translationKey).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.oritech.resonator.controls").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.oritech.resonator.new_search").withStyle(ChatFormatting.GRAY));
    }
}
