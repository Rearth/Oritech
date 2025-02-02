package rearth.oritech.util;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingFluidStorage implements Storage<FluidVariant> {
    
    protected final Supplier<Storage<FluidVariant>> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingFluidStorage(Supplier<Storage<FluidVariant>> backingStorage, BooleanSupplier validPredicate) {
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate;
    }
    
    @Override
    public boolean supportsInsertion() {
        if (validPredicate.getAsBoolean() && backingStorage.get() != null) {
            return backingStorage.get().supportsInsertion();
        }
        return false;
    }
    
    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (validPredicate.getAsBoolean() && backingStorage.get() != null) {
            return backingStorage.get().insert(resource, maxAmount, transaction);
        }
        return 0;
    }
    
    @Override
    public boolean supportsExtraction() {
        if (validPredicate.getAsBoolean() && backingStorage.get() != null) {
            return backingStorage.get().supportsExtraction();
        }
        return false;
    }
    
    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (validPredicate.getAsBoolean() && backingStorage.get() != null) {
            return backingStorage.get().extract(resource, maxAmount, transaction);
        }
        return 0;
    }
    
    @Override
    public @NotNull Iterator<StorageView<FluidVariant>> iterator() {
        if (validPredicate.getAsBoolean() && backingStorage.get() != null) {
            return backingStorage.get().iterator();
        }
        return Collections.emptyIterator();
    }
}
