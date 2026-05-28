package rearth.oritech.api.transfer.fluid;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingFluidStorage implements ResourceHandler<FluidResource> {
    
    protected final Supplier<ResourceHandler<FluidResource>> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingFluidStorage(Supplier<ResourceHandler<FluidResource>> backingStorage, @Nullable BooleanSupplier validPredicate) {
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
    public FluidResource getResource(int index) {
        if (canUseBackend()) return backingStorage.get().getResource(index);
        return FluidResource.EMPTY;
    }
    
    @Override
    public long getAmountAsLong(int index) {
        if (canUseBackend()) return backingStorage.get().getAmountAsLong(index);
        return 0;
    }
    
    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        if (canUseBackend()) return backingStorage.get().getCapacityAsLong(index, resource);
        return 0;
    }
    
    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (canUseBackend()) return backingStorage.get().isValid(index, resource);
        return false;
    }
    
    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().insert(index, resource, amount, transaction);
        return 0;
    }
    
    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().insert(resource, amount, transaction);
        return 0;
    }
    
    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().extract(index, resource, amount, transaction);
        return 0;
    }
    
    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().extract(resource, amount, transaction);
        return 0;
    }
}
