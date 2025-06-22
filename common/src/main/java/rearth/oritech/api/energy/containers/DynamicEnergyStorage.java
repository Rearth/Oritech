package rearth.oritech.api.energy.containers;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.networking.UpdatableField;

public class DynamicEnergyStorage extends EnergyApi.EnergyStorage implements UpdatableField<DynamicEnergyStorage, Long> {
    
    public long amount;
    public long capacity;
    public long maxInsert;
    public long maxExtract;
    private final Runnable onUpdate;
    
    public static final PacketCodec<ByteBuf, DynamicEnergyStorage> PACKET_CODEC = PacketCodec.tuple(
      PacketCodecs.VAR_LONG,
      DynamicEnergyStorage::getMaxExtract,
      PacketCodecs.VAR_LONG,
      DynamicEnergyStorage::getMaxInsert,
      PacketCodecs.VAR_LONG,
      DynamicEnergyStorage::getCapacity,
      PacketCodecs.VAR_LONG,
      DynamicEnergyStorage::getAmount,
      DynamicEnergyStorage::new
    );
    
    
    public DynamicEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onUpdate) {
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.onUpdate = onUpdate;
    }
    
    public DynamicEnergyStorage(long maxExtract, long maxInsert, long capacity, long amount) {
        this.maxExtract = maxExtract;
        this.maxInsert = maxInsert;
        this.capacity = capacity;
        this.amount = amount;
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
    public DynamicEnergyStorage getFullData() {
        return this;
    }
    
    @Override
    public PacketCodec<? extends ByteBuf, Long> getDeltaCodec() {
        return PacketCodecs.VAR_LONG;
    }
    
    @Override
    public PacketCodec<? extends ByteBuf, DynamicEnergyStorage> getFullCodec() {
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