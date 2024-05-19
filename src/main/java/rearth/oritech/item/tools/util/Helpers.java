package rearth.oritech.item.tools.util;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

public class Helpers {
    
    public static void distributePower(PlayerEntity player, ItemStack pack, long maxTransfer) {
        
        var playerStorage = PlayerInventoryStorage.of(player);
        SingleSlotStorage<ItemVariant> packItem = null;
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i) == pack) {
                packItem = playerStorage.getSlot(i);
                break;
            }
        }
        
        if (packItem == null) return;
        var energyItem = ContainerItemContext.ofPlayerSlot(player, packItem).find(EnergyStorage.ITEM);
        if (energyItem == null) return;
        if (energyItem.getAmount() <= 10) return;
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            var stack = player.getInventory().getStack(i);
            if (stack.isEmpty() || stack == pack) continue;
            
            EnergyStorageUtil.move(energyItem, ContainerItemContext.ofPlayerSlot(player, playerStorage.getSlot(i)).find(EnergyStorage.ITEM), maxTransfer, null);
        }
        
    }
    
    public static void onClientTickEvent(MinecraftClient client) {
        
        if (client.player == null) return;
        
        var stack = client.player.getMainHandStack();
        if (stack.getItem() instanceof PromethiumPickaxeItem pickaxeItem) {
            pickaxeItem.onHeldTick(stack, client.player, client.world);
        }
        
    }
}
