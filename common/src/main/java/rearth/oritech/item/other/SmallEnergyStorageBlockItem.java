package rearth.oritech.item.other;

import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyItemStorage;
import rearth.oritech.util.TooltipHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class SmallEnergyStorageBlockItem extends BlockItem implements EnergyApi.ItemProvider {
    
    public SmallEnergyStorageBlockItem(Block block, Properties settings) {
        super(block, settings);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        var storedEnergy = stack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(), 0L);
        
        if (storedEnergy != 0) {
            var text = Component.translatable("tooltip.oritech.energy_stored", TooltipHelper.getEnergyText(storedEnergy));
            tooltip.add(text.withStyle(ChatFormatting.GOLD));
        }
        
        super.appendHoverText(stack, context, tooltip, type);
        
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        var contentEmpty = stack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(), 0L) <= 0;
        return !contentEmpty;
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        
        var capacity = Oritech.CONFIG.smallEnergyStorage.energyCapacity();
        var fillAmount = stack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(), 0L);
        
        return Math.round((fillAmount * 100f / capacity) * MAX_BAR_WIDTH) / 100;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(ItemStack stack) {
        return new SimpleEnergyItemStorage(Oritech.CONFIG.smallEnergyStorage.maxEnergyInsertion(), Oritech.CONFIG.smallEnergyStorage.maxEnergyExtraction(), Oritech.CONFIG.smallEnergyStorage.energyCapacity(), stack);
    }
}
