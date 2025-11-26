package rearth.oritech.item.other;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleItemFluidStorage;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;

public class SmallFluidTankBlockItem extends BlockItem implements FluidApi.ItemProvider {
    
    public SmallFluidTankBlockItem(Block block, Properties settings) {
        super(block, settings);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        var data = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        
        if (data.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.oritech.fluid_empty"));
        } else {
            var amount = data.getAmount() / (float) FluidStackHooks.bucketAmount();
            tooltip.add(Component.translatable("tooltip.oritech.fluid_content_tank_tooltip", amount, FluidStackHooks.getName(data).getString()).withStyle(ChatFormatting.GRAY));
        }
        
        super.appendHoverText(stack, context, tooltip, type);
        
    }
    
    @Override
    public Component getName(ItemStack stack) {
        var content = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        if (content.isEmpty()) {
            return super.getName(stack);
        } else {
            return FluidStackHooks.getName(content).copy().append(Component.literal(" ")).append(super.getName(stack));
        }
    }
    
    @Override
    public FluidApi.SingleSlotStorage getFluidStorage(ItemStack stack) {
        return new SimpleItemFluidStorage(Oritech.CONFIG.portableTankCapacityBuckets() * FluidStackHooks.bucketAmount(), stack);
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        var contentEmpty = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty()).isEmpty();
        return !contentEmpty;
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        var content = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        if (content.isEmpty())
            return 0x07bdff;
        
        if (content.getFluid().equals(Fluids.LAVA))
            return 0xff8000;
        
        return FluidStackHooks.getColor(content);
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        
        var capacity = Oritech.CONFIG.portableTankCapacityBuckets() * FluidStackHooks.bucketAmount();
        var fillAmount = stack.getOrDefault(FluidApi.ITEM.getFluidComponent(), FluidStack.empty()).getAmount();
        
        return Math.round((fillAmount * 100f / capacity) * MAX_BAR_WIDTH) / 100;
    }
}
