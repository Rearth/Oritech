package rearth.oritech.api.transfer.item;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingInventoryStorage implements ResourceHandler<ItemResource> {
    
    protected final Supplier<ResourceHandler<ItemResource>> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingInventoryStorage(Supplier<ResourceHandler<ItemResource>> backingStorage, @Nullable BooleanSupplier validPredicate) {
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate == null ? () -> true : validPredicate;
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
    public long getAmountAsLong(int index) {
        if (canUseBackend()) return backingStorage.get().getAmountAsLong(index);
        return 0;
    }
    
    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        if (canUseBackend()) return backingStorage.get().getCapacityAsLong(index, resource);
        return 0;
    }
    
    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (canUseBackend()) return backingStorage.get().isValid(index, resource);
        return false;
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
