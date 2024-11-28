package rearth.oritech.item.tools.util;

import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.SimpleEnergyItemStorage;

public interface OritechEnergyItem extends EnergyApi.ItemProvider, FabricItem {
    
    default long getEnergyCapacity(ItemStack stack) {return 10_000;}
    
    default long getEnergyMaxInput(ItemStack stack) {
        return 500;
    }
    
    default long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }
    
    @Override
    default boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
    
    default boolean tryUseEnergy(ItemStack stack, long amount, PlayerEntity player){
        Random random = Random.create();
        
        int unbreakingLevel = getUnbreakingLevel(stack);
        if (unbreakingLevel > 0) {
            amount = amount / (random.nextInt(unbreakingLevel) + 1);
        }
        
        var storage = getStorage(stack);
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
        ItemEnchantmentsComponent enchantments = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.getKey().isPresent() && entry.getKey().get().equals(Enchantments.UNBREAKING)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }
    
    default long getStoredEnergy(ItemStack stack) {
        return getStorage(stack).getAmount();
    }
    
    @Override
    default EnergyApi.EnergyContainer getStorage(ItemStack stack) {
        return new SimpleEnergyItemStorage(getEnergyMaxInput(stack), getEnergyMaxOutput(stack), getEnergyCapacity(stack), stack);
    }
}