package rearth.oritech.item.other;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rearth.oritech.Oritech;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.SimpleEnergyItemStorage;

import java.util.List;

public class SmallEnergyStorageBlockItem extends BlockItem implements EnergyApi.ItemProvider {
    
    public SmallEnergyStorageBlockItem(Block block, Settings settings) {
        super(block, settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var storedEnergy = stack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(), 0L);
        
        if (storedEnergy != 0) {
            var text = Text.translatable("tooltip.oritech.energy_stored", storedEnergy);
            tooltip.add(text.formatted(Formatting.GOLD));
        }
        
        super.appendTooltip(stack, context, tooltip, type);
        
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        var contentEmpty = stack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(), 0L) <= 0;
        return !contentEmpty;
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    @Override
    public int getItemBarStep(ItemStack stack) {
        
        var capacity = Oritech.CONFIG.smallEnergyStorage.energyCapacity();
        var fillAmount = stack.getOrDefault(EnergyApi.ITEM.getEnergyComponent(), 0L);
        
        return Math.round((fillAmount * 100f / capacity) * ITEM_BAR_STEPS) / 100;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(ItemStack stack) {
        return new SimpleEnergyItemStorage(Oritech.CONFIG.smallEnergyStorage.maxEnergyInsertion(), Oritech.CONFIG.smallEnergyStorage.maxEnergyExtraction(), Oritech.CONFIG.smallEnergyStorage.energyCapacity(), stack);
    }
}
