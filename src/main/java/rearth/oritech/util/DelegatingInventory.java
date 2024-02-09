package rearth.oritech.util;

import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface DelegatingInventory extends ImplementedInventory {
    
    @Override
    default DefaultedList<ItemStack> getItems() {
        if (!isEnabled())
            return DefaultedList.ofSize(0, ItemStack.EMPTY);
        
        return getDelegatedInventory().getItems();
    }
    
    @Override
    default boolean canExtract(int slot, ItemStack stack, Direction side) {
        if (!isEnabled()) return false;
        return getDelegatedInventory().canExtract(slot, stack, side);
    }
    
    @Override
    default boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        if (!isEnabled()) return false;
        return getDelegatedInventory().canInsert(slot, stack, side);
    }
    
    ImplementedInventory getDelegatedInventory();
    boolean isEnabled();
}
