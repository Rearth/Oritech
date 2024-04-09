package rearth.oritech.tools.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface ArmorEventHandler {
    
    void onEquipped(PlayerEntity playerEntity, ItemStack stack);
    void onUnequipped(PlayerEntity playerEntity, ItemStack stack);
    
}
