package rearth.oritech.item.tools.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface ClientTickableItem {

    void clientInventoryTick(ItemStack stack, Level level, Entity owner, @Nullable EquipmentSlot slot);
}
