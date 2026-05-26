package rearth.oritech.api.transfer.item;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingInventoryStorage extends ItemStacksResourceHandler {
    
    protected final Supplier<ItemStacksResourceHandler> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingInventoryStorage(Supplier<ItemStacksResourceHandler> backingStorage, @Nullable BooleanSupplier validPredicate) {
        super(0);
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate == null ? () -> true : validPredicate;
    }
    
    public DelegatingInventoryStorage(ItemStacksResourceHandler backingStorage, @Nullable BooleanSupplier validPredicate) {
        this(() -> backingStorage, validPredicate);
    }
    
    private boolean canUseBackend() {
        return validPredicate.getAsBoolean() && backingStorage.get() != null;
    }
    
    @Override
    public int size() {
        if (canUseBackend()) return backingStorage.get().size();
        return 0;
    }

    @Override
    public ItemResource getResource(int index) {
        if (canUseBackend()) return backingStorage.get().getResource(index);
        return ItemResource.EMPTY;
    }

    @Override
    public int getAmountAsInt(int index) {
        if (canUseBackend()) return backingStorage.get().getAmountAsInt(index);
        return 0;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().insert(index, resource, amount, transaction);
        return 0;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().insert(resource, amount, transaction);
        return 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().extract(index, resource, amount, transaction);
        return 0;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().extract(resource, amount, transaction);
        return 0;
    }
}
