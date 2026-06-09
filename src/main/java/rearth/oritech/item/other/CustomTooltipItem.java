package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CustomTooltipItem extends Item {

    private final String translationKey;

    public CustomTooltipItem(Properties settings, String translationKey) {
        super(settings);
        this.translationKey = translationKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra) {
            consumer.accept(Component.translatable(translationKey).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        } else {
            consumer.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }
}
