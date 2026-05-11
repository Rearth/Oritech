package rearth.oritech.item.tools.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyItemStorage;

public interface OritechEnergyItem extends EnergyApi.ItemProvider {
    
    default long getEnergyCapacity(ItemStack stack) {return 10_000;}
    
    default long getEnergyMaxInput(ItemStack stack) {
        return 500;
    }
    
    default long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }
    
    default boolean tryUseEnergy(ItemStack stack, long amount, Player player){
        RandomSource random = RandomSource.create();
        
        int unbreakingLevel = getUnbreakingLevel(stack);
        if (unbreakingLevel > 0) {
            amount = amount / (random.nextInt(unbreakingLevel) + 1);
        }
        
        var storage = getEnergyStorage(stack);
        if (storage instanceof SimpleEnergyItemStorage itemStorage) {
            var extracted = itemStorage.extractIgnoringLimit(amount, false);
            if (extracted > 0) {
                itemStorage.update();
            }
            
            return extracted == amount;
        }
        
        return false;
        
    }
    
    // A hack to do this without context of the DRM
    private int getUnbreakingLevel(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> entry : enchantments.keySet()) {
            if (entry.unwrapKey().isPresent() && entry.unwrapKey().get().equals(Enchantments.UNBREAKING)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }
    
    default long getStoredEnergy(ItemStack stack) {
        return getEnergyStorage(stack).getAmount();
    }
    
    @Override
    default EnergyApi.EnergyStorage getEnergyStorage(ItemStack stack) {
        return new SimpleEnergyItemStorage(getEnergyMaxInput(stack), getEnergyMaxOutput(stack), getEnergyCapacity(stack), stack);
    }
}