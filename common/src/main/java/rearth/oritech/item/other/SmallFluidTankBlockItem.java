package rearth.oritech.item.other;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleItemFluidStorage;

import java.util.List;

public class SmallFluidTankBlockItem extends BlockItem implements FluidApi.ItemProvider {
    
    public SmallFluidTankBlockItem(Block block, Settings settings) {
        super(block, settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var data = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        
        if (data.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.oritech.fluid_empty"));
        } else {
            var amount = data.getAmount() / (float) FluidStackHooks.bucketAmount();
            tooltip.add(Text.translatable("tooltip.oritech.fluid_content_tank_tooltip", amount, FluidStackHooks.getName(data).getString()));
        }
        
        super.appendTooltip(stack, context, tooltip, type);
        
    }
    
    @Override
    public Text getName(ItemStack stack) {
        var content = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        if (content.isEmpty()) {
            return super.getName(stack);
        } else {
            return FluidStackHooks.getName(content).copy().append(Text.literal(" ")).append(super.getName(stack));
        }
    }
    
    @Override
    public FluidApi.SingleSlotStorage getFluidStorage(ItemStack stack) {
        return new SimpleItemFluidStorage(Oritech.CONFIG.portableTankCapacityBuckets() * FluidStackHooks.bucketAmount(), stack);
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        var contentEmpty = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty()).isEmpty();
        return !contentEmpty;
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x07bdff;
    }
    
    @Override
    public int getItemBarStep(ItemStack stack) {
        
        var capacity = Oritech.CONFIG.portableTankCapacityBuckets() * FluidStackHooks.bucketAmount();
        var fillAmount = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty()).getAmount();
        
        return Math.round((fillAmount * 100f / capacity) * ITEM_BAR_STEPS) / 100;
    }
}
