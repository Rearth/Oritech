package rearth.oritech.item.other;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OverchargedCrystalItem extends Item {
    public OverchargedCrystalItem(Settings settings) {
        super(settings);
    }
    
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("tooltip.oritech.overchargedcrystal").formatted(Formatting.ITALIC, Formatting.GRAY));
    }
}
