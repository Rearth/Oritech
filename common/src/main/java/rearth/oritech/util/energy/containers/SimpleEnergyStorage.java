package rearth.oritech.util.energy.containers;

import rearth.oritech.util.energy.EnergyApi;

public class SimpleEnergyStorage extends EnergyApi.EnergyContainer {
    private final long maxInsert;
    private final long maxExtract;
    private final long capacity;
    private final Runnable onUpdate;
    
    private long amount;
    
    public SimpleEnergyStorage(long maxInsert, long maxExtract, long capacity, Runnable onUpdate) {
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.capacity = capacity;
        this.onUpdate = onUpdate;
    }
    
    public SimpleEnergyStorage(long maxInsert, long maxExtract, long capacity) {
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.capacity = capacity;
        this.onUpdate = () -> {};
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
    
    public long extractIgnoringLimit(long amount, boolean simulate) {
        long extracted = Math.min(amount, this.amount);
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
    
    @Override
    public void update() {
        onUpdate.run();
    }
}