package rearth.oritech.api.transfer.energy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;

public class DynamicEnergyStorage implements EnergyHandler, ValueIOSerializable, UpdatableField<DynamicEnergyStorage, Long> {

    public long energy;
    public long capacity;
    public long maxInsert;
    public long maxExtract;
    private final boolean forceFullUpdate;
    private final Runnable onUpdate;

    private final EnergyJournal energyJournal = new EnergyJournal();

    public static final StreamCodec<ByteBuf, DynamicEnergyStorage> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            DynamicEnergyStorage::getCapacityAsLong,
            ByteBufCodecs.VAR_LONG,
            DynamicEnergyStorage::getMaxInsert,
            ByteBufCodecs.VAR_LONG,
            DynamicEnergyStorage::getMaxExtract,
            ByteBufCodecs.VAR_LONG,
            DynamicEnergyStorage::getAmountAsLong,
            DynamicEnergyStorage::new
    );

    public DynamicEnergyStorage(long capacity, long maxInsert, long maxExtract, long amount) {
        this(capacity, maxInsert, maxExtract, amount, () -> {
        }, false);
    }

    public DynamicEnergyStorage(long capacity, long maxInsert, long maxExtract, long amount, Runnable onUpdate, boolean alwaysFullUpdate) {
        this.maxExtract = maxExtract;
        this.maxInsert = maxInsert;
        this.capacity = capacity;
        this.energy = amount;
        this.forceFullUpdate = alwaysFullUpdate;
        this.onUpdate = onUpdate;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }


    public void setMaxInsert(long maxInsert) {
        this.maxInsert = maxInsert;
    }

    public void setMaxExtract(long maxExtract) {
        this.maxExtract = maxExtract;
    }

    public long getMaxExtract() {
        return maxExtract;
    }

    public long getMaxInsert() {
        return maxInsert;
    }

    @Override
    public Long getDeltaData() {
        return energy;
    }

    @Override
    public boolean useDeltaOnly(SyncType type) {
        if (forceFullUpdate) return false;
        return UpdatableField.super.useDeltaOnly(type);
    }

    @Override
    public DynamicEnergyStorage getFullData() {
        return this;
    }

    @Override
    public StreamCodec<? extends ByteBuf, Long> getDeltaCodec() {
        return ByteBufCodecs.VAR_LONG;
    }

    @Override
    public StreamCodec<? extends ByteBuf, DynamicEnergyStorage> getFullCodec() {
        return PACKET_CODEC;
    }

    @Override
    public void handleFullUpdate(DynamicEnergyStorage updatedData) {
        this.set(updatedData.energy);
        this.setCapacity(updatedData.capacity);
        this.setMaxExtract(updatedData.maxExtract);
        this.setMaxInsert(updatedData.maxInsert);
    }

    @Override
    public void handleDeltaUpdate(Long updatedData) {
        this.set(updatedData);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putLong("energy", energy);
        output.putLong("capacity", capacity);
        output.putLong("maxInsert", maxInsert);
        output.putLong("maxExtract", maxExtract);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.energy = input.getLongOr("energy", 0L);
        this.capacity = input.getLongOr("capacity", capacity);
        this.maxInsert = input.getLongOr("maxInsert", maxInsert);
        this.maxExtract = input.getLongOr("maxExtract", maxExtract);
    }

    @Override
    public long getAmountAsLong() {
        return energy;
    }

    @Override
    public long getCapacityAsLong() {
        return capacity;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);

        var inserted = Math.min(capacity - energy, Math.min(amount, maxInsert));
        if (inserted > 0) {
            energyJournal.updateSnapshots(transaction);
            energy += inserted;
            return (int) inserted;
        }

        return 0;
    }

    // same as insert, but ignoring insertion limits
    public long internalInsert(long amount, TransactionContext transaction) {

        var inserted = Math.min(capacity - energy, amount);
        if (inserted > 0) {
            energyJournal.updateSnapshots(transaction);
            energy += inserted;
            return (int) inserted;
        }

        return 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);

        var extracted = Math.min(energy, Math.min(amount, maxExtract));
        if (extracted > 0) {
            energyJournal.updateSnapshots(transaction);
            energy -= extracted;
            return (int) extracted;
        }
        return 0;
    }

    // same as extract, but ignoring extraction limits
    public long internalExtract(long amount, TransactionContext transaction) {

        var extracted = Math.min(energy, amount);
        if (extracted > 0) {
            energyJournal.updateSnapshots(transaction);
            energy -= extracted;
            return (int) extracted;
        }
        return 0;
    }

    public void set(long amount) {

        if (this.energy != amount) {
            var previousAmount = this.energy;
            this.energy = amount;
            onEnergyChanged(previousAmount);
        }
    }

    /**
     * Called after the amount of energy in the handler changed.
     *
     * <p>For changes that happen through {@link #set}, this method is called immediately.
     * For changes that happen through {@link #insert} or {@link #extract},
     * this function will be called at the end of the transaction.
     */
    public void onEnergyChanged(long previousAmount) {
        onUpdate.run();
    }

    private class EnergyJournal extends SnapshotJournal<Long> {
        @Override
        protected Long createSnapshot() {
            return energy;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            energy = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            if (energy != originalState) {
                onEnergyChanged(originalState);
            }
        }
    }
}