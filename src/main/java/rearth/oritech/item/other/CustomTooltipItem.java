package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class CustomTooltipItem extends Item {

    private final String translationKey;

    public CustomTooltipItem(Properties settings, String translationKey) {
        super(settings);
        this.translationKey = translationKey;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra) {
            builder.accept(Component.translatable(translationKey).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        } else {
            builder.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }
}
