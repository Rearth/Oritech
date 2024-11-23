package rearth.oritech.item.tools.util;

import earth.terrarium.common_storage_lib.context.ItemContext;
import earth.terrarium.common_storage_lib.context.impl.IsolatedSlotContext;
import earth.terrarium.common_storage_lib.context.impl.PlayerContext;
import earth.terrarium.common_storage_lib.energy.EnergyApi;
import earth.terrarium.common_storage_lib.energy.EnergyProvider;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
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
import rearth.oritech.Oritech;
import rearth.oritech.util.SimpleItemEnergyStorage;

public interface OritechEnergyItem extends EnergyProvider.Item, FabricItem {
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
        
        // this feels like a horrible abomination
        var slotIndex = findSlotIndex(stack, player);
        
        var slot = PlayerContext.ofSlot(player, slotIndex);
        var storage = slot.find(EnergyApi.ITEM);
        
        if (storage instanceof SimpleItemEnergyStorage simpleStorage) {
            var extracted = simpleStorage.extractIgnoringLimit(amount, false);
            if (extracted > 0) {
                stack.set(Oritech.ENERGY_CONTENT.componentType(), storage.getStoredAmount());
            }
            
            return extracted == amount;
        }
        
        return false;
        
    }
    
    static int findSlotIndex(ItemStack stack, PlayerEntity player) {
        var slotIndex = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            var invStack = player.getInventory().getStack(i);
            if (invStack == stack) {
                slotIndex = i;
                break;
            }
        }
        return slotIndex;
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
        var slot = new IsolatedSlotContext(stack);
        return getEnergy(stack, slot).getStoredAmount();
    }
    
    @Override
    default ValueStorage getEnergy(ItemStack stack, ItemContext context) {
        return new SimpleItemEnergyStorage(context, Oritech.ENERGY_CONTENT.componentType(), getEnergyCapacity(stack), getEnergyMaxInput(stack), getEnergyMaxOutput(stack), stack);
    }
}