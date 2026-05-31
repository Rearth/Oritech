package rearth.oritech.api.transfer.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class DelegatingEnergyStorage implements EnergyHandler {

    protected final Supplier<EnergyHandler> backingStorage;
    protected final BooleanSupplier validPredicate;

    public DelegatingEnergyStorage(Supplier<EnergyHandler> backingStorage, @Nullable BooleanSupplier validPredicate) {
        this.backingStorage = backingStorage;
        this.validPredicate = validPredicate == null ? () -> true : validPredicate;
    }

    private boolean canUseBackend() {
        return validPredicate.getAsBoolean() && backingStorage.get() != null;
    }

    @Override
    public long getAmountAsLong() {
        if (canUseBackend()) return backingStorage.get().getAmountAsLong();
        return 0;
    }

    @Override
    public long getCapacityAsLong() {
        if (canUseBackend()) return backingStorage.get().getCapacityAsLong();
        return 0;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().insert(amount, transaction);
        return 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        if (canUseBackend()) return backingStorage.get().extract(amount, transaction);
        return 0;
    }
}
