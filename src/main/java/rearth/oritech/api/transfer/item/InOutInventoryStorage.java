package rearth.oritech.api.transfer.item;

import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.util.ContainerSlotAssignment;

// in / out containers give direct access to the input or output container
public class InOutInventoryStorage extends SimpleInventoryStorage {

    private final ContainerSlotAssignment slotAssignment;
    private final ResourceHandler<ItemResource> inputContainer;
    private final ResourceHandler<ItemResource> outputContainer;

    public InOutInventoryStorage(int size, Runnable onUpdate, ContainerSlotAssignment slotAssignment) {
        super(size, onUpdate);
        this.slotAssignment = slotAssignment;

        inputContainer = new DelegatingResourceHandler<>(this) {
            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isInput(index)) return 0;
                return super.insert(index, resource, amount, transaction);
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isInput(index)) return 0;
                return super.extract(index, resource, amount, transaction);
            }

            @Override
            public int size() {
                return slotAssignment.inputCount();
            }
        };

        outputContainer = new DelegatingResourceHandler<>(this) {
            @Override
            public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isOutput(index)) return 0;
                return super.insert(index, resource, amount, transaction);
            }

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (!slotAssignment.isOutput(index)) return 0;
                return super.extract(index, resource, amount, transaction);
            }

            @Override
            public int size() {
                return slotAssignment.outputCount();
            }
        };
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

    public ResourceHandler<ItemResource> getInputContainer() {
        return inputContainer;
    }

    public ResourceHandler<ItemResource> getOutputContainer() {
        return outputContainer;
    }
}
