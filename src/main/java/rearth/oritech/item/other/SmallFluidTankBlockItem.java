package rearth.oritech.item.other;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import rearth.oritech.util.FluidStack;

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
        var fluidStack = FluidStack.fromNbt(nbt);
        var variant = fluidStack.variant();
        var amount = fluidStack.amount() * 1000 / FluidConstants.BUCKET;
        tooltip.add(Text.translatable("tooltip.oritech.fluid_content", amount, variant.isBlank()
            ? Text.translatable("tooltip.oritech.fluid_empty")
            : FluidVariantAttributes.getName(variant).getString()));
    }
}
