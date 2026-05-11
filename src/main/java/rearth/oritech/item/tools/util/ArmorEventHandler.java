package rearth.oritech.item.tools.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ArmorEventHandler {
    
    void onEquipped(Player playerEntity, ItemStack stack);
    void onUnequipped(Player playerEntity, ItemStack stack);
    
    static void processEvent(LivingEntity livingEntity, EquipmentSlot equipmentSlot, ItemStack previousStack, ItemStack currentStack) {
        if (livingEntity instanceof Player playerEntity && equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            if (previousStack.getItem() instanceof ArmorEventHandler armorItem) {
                armorItem.onUnequipped(playerEntity, previousStack);
            }
            if (currentStack.getItem() instanceof ArmorEventHandler armorItem) {
                armorItem.onEquipped(playerEntity, currentStack);
            }
            
        }
    }
    
}
