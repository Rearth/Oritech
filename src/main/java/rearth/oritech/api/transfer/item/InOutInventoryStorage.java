package rearth.oritech.api.transfer.item;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.api.transfer.SlotRangeResourceHandler;
import rearth.oritech.util.ContainerSlotAssignment;

// this storage itself is fully unrestricted (like a plain inventory): any slot can be inserted into or
// extracted from directly. That's what's needed for the machine's own internal logic (recipe crafting,
// canOutputRecipe checks, etc.) and for the GUI (e.g. so the player can take items back out of input slots).
// externally exposed access (e.g. to pipes/hoppers) should go through getExternalAccess() instead, which
// restricts insertion to input slots and extraction to output slots.
public class InOutInventoryStorage extends SimpleInventoryStorage {

    private final ResourceHandler<ItemResource> inputContainer;
    private final ResourceHandler<ItemResource> outputContainer;
    private final ResourceHandler<ItemResource> externalAccess;

    public InOutInventoryStorage(int size, Runnable onUpdate, ContainerSlotAssignment slotAssignment) {
        super(size, onUpdate);

        inputContainer = new SlotRangeResourceHandler<>(this, slotAssignment.inputStart(), slotAssignment.inputCount(), this::insert, this::extract);
        outputContainer = new SlotRangeResourceHandler<>(this, slotAssignment.outputStart(), slotAssignment.outputCount(), this::insert, this::extract);

        externalAccess = new DelegatingResourceHandler<>(this) {
            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isInput(index)) return 0;
                return super.insert(index, resource, amount, transaction);
            }

            @Override
            public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                var inserted = 0;
                var end = slotAssignment.inputStart() + slotAssignment.inputCount();
                for (int index = slotAssignment.inputStart(); index < end && inserted < amount; index++) {
                    inserted += insert(index, resource, amount - inserted, transaction);
                }
                return inserted;
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isOutput(index)) return 0;
                return super.extract(index, resource, amount, transaction);
            }

            @Override
            public int extract(ItemResource resource, int amount, TransactionContext transaction) {
                var extracted = 0;
                var end = slotAssignment.outputStart() + slotAssignment.outputCount();
                for (int index = slotAssignment.outputStart(); index < end && extracted < amount; index++) {
                    extracted += extract(index, resource, amount - extracted, transaction);
                }
                return extracted;
            }
        };
    }

    public ResourceHandler<ItemResource> getInputContainer() {
        return inputContainer;
    }

    public ResourceHandler<ItemResource> getOutputContainer() {
        return outputContainer;
    }

    // restricted view for externally exposed access (e.g. pipes/hoppers): insertion only allowed into
    // input slots, extraction only from output slots.
    public ResourceHandler<ItemResource> getExternalAccess() {
        return externalAccess;
    }
}
