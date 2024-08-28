package rearth.oritech.item.other;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public class SmallFluidTankBlockItem extends BlockItem {
    public SmallFluidTankBlockItem(Block block, Settings settings) {
        super(block, settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        
        if (!stack.contains(DataComponentTypes.CUSTOM_DATA)) return;
        var nbt = stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        
        var amount = nbt.getLong("amount") * 1000 / FluidConstants.BUCKET;
        tooltip.add(Text.translatable("tooltip.oritech.fluid_amount", amount));
    }
}
