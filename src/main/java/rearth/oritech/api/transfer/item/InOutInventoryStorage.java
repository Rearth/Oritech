package rearth.oritech.api.transfer.item;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.util.InventorySlotAssignment;

public class InOutInventoryStorage extends SimpleInventoryStorage {
    
    private final InventorySlotAssignment slotAssignment;
    
    public InOutInventoryStorage(int size, Runnable onUpdate, InventorySlotAssignment slotAssignment) {
        super(size, onUpdate);
        this.slotAssignment = slotAssignment;
    }
    
    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!slotAssignment.isInput(index)) return 0;
        return super.insert(index, resource, amount, transaction);
    }
    
    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!slotAssignment.isOutput(index)) return 0;
        return super.extract(index, resource, amount, transaction);
    }
}
