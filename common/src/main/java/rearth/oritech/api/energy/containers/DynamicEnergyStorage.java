package rearth.oritech.api.energy.containers;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;

public class DynamicEnergyStorage extends EnergyApi.EnergyStorage implements UpdatableField<DynamicEnergyStorage, Long> {
    
    public long amount;
    public long capacity;
    public long maxInsert;
    public long maxExtract;
    private final Runnable onUpdate;
    private final boolean forceFullUpdate;
    
    public static final StreamCodec<ByteBuf, DynamicEnergyStorage> PACKET_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_LONG,
      DynamicEnergyStorage::getMaxExtract,
      ByteBufCodecs.VAR_LONG,
      DynamicEnergyStorage::getMaxInsert,
      ByteBufCodecs.VAR_LONG,
      DynamicEnergyStorage::getCapacity,
      ByteBufCodecs.VAR_LONG,
      DynamicEnergyStorage::getAmount,
      DynamicEnergyStorage::new
    );
    
    
    public DynamicEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onUpdate) {
        this(capacity, maxInsert, maxExtract, onUpdate, false);
    }
    
    
    public DynamicEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onUpdate, boolean alwaysFullUpdate) {
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.onUpdate = onUpdate;
        this.forceFullUpdate = alwaysFullUpdate;
    }
    
    public DynamicEnergyStorage(long maxExtract, long maxInsert, long capacity, long amount) {
        this.maxExtract = maxExtract;
        this.maxInsert = maxInsert;
        this.capacity = capacity;
        this.amount = amount;
        this.forceFullUpdate = false;
        this.onUpdate = () -> {
        };
    }
    
    @Override
    public long insert(long amount, boolean simulate) {
        long inserted = Math.min(Math.min(maxInsert, amount), capacity - this.amount);
        if (!simulate) {
            this.amount += inserted;
        }
        return inserted;
    }
    
    public long insertIgnoringLimit(long amount, boolean simulate) {
        long inserted = Math.min(amount, capacity - this.amount);
        if (!simulate) {
            this.amount += inserted;
        }
        return inserted;
    }
    
    @Override
    public long extract(long amount, boolean simulate) {
        long extracted = Math.min(Math.min(amount, maxExtract), this.amount);
        if (!simulate) {
            this.amount -= extracted;
        }
        return extracted;
    }
    
    @Override
    public void setAmount(long amount) {
        this.amount = amount;
    }
    
    @Override
    public long getAmount() {
        return amount;
    }
    
    @Override
    public long getCapacity() {
        return capacity;
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
    public void update() {
        onUpdate.run();
    }
    
    @Override
    public Long getDeltaData() {
        return amount;
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
        this.setAmount(updatedData.amount);
        this.setCapacity(updatedData.capacity);
        this.setMaxExtract(updatedData.maxExtract);
        this.setMaxInsert(updatedData.maxInsert);
    }
    
    @Override
    public void handleDeltaUpdate(Long updatedData) {
        this.setAmount(updatedData);
    }
}