package rearth.oritech.item.other;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class CustomTooltipItem extends Item {
    
    private final String translationKey;
    
    public CustomTooltipItem(Settings settings, String translationKey) {
        super(settings);
        this.translationKey = translationKey;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            tooltip.add(Text.translatable(translationKey).formatted(Formatting.ITALIC, Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
    }
}
