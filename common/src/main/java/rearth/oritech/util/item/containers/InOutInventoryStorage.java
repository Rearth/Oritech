package rearth.oritech.util.item.containers;

import net.minecraft.item.ItemStack;
import rearth.oritech.util.InventorySlotAssignment;

public class InOutInventoryStorage extends SimpleInventoryStorage {
    
    private final InventorySlotAssignment slotAssignment;
    
    public InOutInventoryStorage(int size, Runnable onUpdate, InventorySlotAssignment slotAssignment) {
        super(size, onUpdate);
        this.slotAssignment = slotAssignment;
    }
    
    @Override
    public int insertToSlot(ItemStack addedStack, int slot, boolean simulate) {
        if (!slotAssignment.isInput(slot)) return 0;
        return super.insertToSlot(addedStack, slot, simulate);
    }
    
    @Override
    public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
        if (!slotAssignment.isOutput(slot)) return 0;
        return super.extractFromSlot(extracted, slot, simulate);
    }
}
