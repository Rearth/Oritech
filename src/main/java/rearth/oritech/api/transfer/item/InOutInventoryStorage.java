package rearth.oritech.api.transfer.item;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.api.transfer.SlotRangeResourceHandler;
import rearth.oritech.util.ContainerSlotAssignment;

// in / out containers give fully unrestricted direct access to just the input or just the output slots.
// this is separate from the pipe-facing insert(index, ...)/extract(index, ...) below, which restrict
// insertion to input slots and extraction to output slots for externally exposed access.
public class InOutInventoryStorage extends SimpleInventoryStorage {

    private final ContainerSlotAssignment slotAssignment;
    private final ResourceHandler<ItemResource> inputContainer;
    private final ResourceHandler<ItemResource> outputContainer;

    public InOutInventoryStorage(int size, Runnable onUpdate, ContainerSlotAssignment slotAssignment) {
        super(size, onUpdate);
        this.slotAssignment = slotAssignment;

        inputContainer = new SlotRangeResourceHandler<>(this, slotAssignment.inputStart(), slotAssignment.inputCount(), this::rawInsert, this::rawExtract);
        outputContainer = new SlotRangeResourceHandler<>(this, slotAssignment.outputStart(), slotAssignment.outputCount(), this::rawInsert, this::rawExtract);
    }

    // externally exposed (e.g. pipe) access: insertion only allowed into input slots, extraction only from output slots
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

    // raw, unrestricted index-based insert/extract - bypasses the restriction above.
    // backs the input/output sub-views used for internal logic (crafting, guis, checks, etc.)
    private int rawInsert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return super.insert(index, resource, amount, transaction);
    }

    private int rawExtract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return super.extract(index, resource, amount, transaction);
    }

    public ResourceHandler<ItemResource> getInputContainer() {
        return inputContainer;
    }

    public ResourceHandler<ItemResource> getOutputContainer() {
        return outputContainer;
    }
}
