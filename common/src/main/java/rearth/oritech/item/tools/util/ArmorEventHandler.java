package rearth.oritech.item.tools.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface ArmorEventHandler {
    
    void onEquipped(PlayerEntity playerEntity, ItemStack stack);
    void onUnequipped(PlayerEntity playerEntity, ItemStack stack);
    
    static void processEvent(LivingEntity livingEntity, EquipmentSlot equipmentSlot, ItemStack previousStack, ItemStack currentStack) {
        if (livingEntity instanceof PlayerEntity playerEntity && equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            if (previousStack.getItem() instanceof ArmorEventHandler armorItem) {
                armorItem.onUnequipped(playerEntity, previousStack);
            }
            if (currentStack.getItem() instanceof ArmorEventHandler armorItem) {
                armorItem.onEquipped(playerEntity, currentStack);
            }
            
        }
    }
    
}
