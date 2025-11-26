package rearth.oritech.api.energy.containers;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingEnergyStorage extends EnergyApi.EnergyStorage {
    
    protected final Supplier<EnergyApi.EnergyStorage> backingStorage;
    protected final BooleanSupplier validPredicate;
    
    public DelegatingEnergyStorage(Supplier<EnergyApi.EnergyStorage> backingStorage, @Nullable BooleanSupplier validPredicate) {
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate == null ? () -> true : validPredicate;
    }
    
    public DelegatingEnergyStorage(EnergyApi.EnergyStorage backingStorage, @Nullable BooleanSupplier validPredicate) {
        this(() -> backingStorage, validPredicate);
    }
    
    private boolean canUseBackend() {
        return validPredicate.getAsBoolean() && backingStorage.get() != null;
    }
    
    @Override
    public long getCapacity() {
        if (canUseBackend()) {
            return backingStorage.get().getCapacity();
        }
        return 0;
    }
    
    @Override
    public void update() {
        if (canUseBackend()) {
            backingStorage.get().update();
        }
    }
    
    @Override
    public long insert(long amount, boolean simulate) {
        if (canUseBackend()) {
            return backingStorage.get().insert(amount, simulate);
        }
        return 0;
    }
    
    @Override
    public long extract(long amount, boolean simulate) {
        if (canUseBackend()) {
            return backingStorage.get().extract(amount, simulate);
        }
        return 0;
    }
    
    @Override
    public boolean supportsInsertion() {
        if (canUseBackend()) {
            return backingStorage.get().supportsInsertion();
        }
        return false;
    }
    
    @Override
    public boolean supportsExtraction() {
        if (canUseBackend()) {
            return backingStorage.get().supportsExtraction();
        }
        return false;
    }
    
    @Override
    public void setAmount(long amount) {
        if (canUseBackend()) {
            backingStorage.get().setAmount(amount);
        }
    }
    
    @Override
    public long getAmount() {
        if (canUseBackend()) {
            return backingStorage.get().getAmount();
        }
        return 0;
    }
}
