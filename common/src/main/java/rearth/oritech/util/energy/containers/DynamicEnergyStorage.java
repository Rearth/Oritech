package rearth.oritech.util.energy.containers;

import rearth.oritech.util.energy.EnergyApi;

public class DynamicEnergyStorage extends EnergyApi.EnergyContainer {
    
    public long amount;
    public long capacity;
    public long maxInsert;
    public long maxExtract;
    private final Runnable onUpdate;
    
    public DynamicEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onUpdate) {
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.onUpdate = onUpdate;
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
    
    @Override
    public void update() {
        onUpdate.run();
    }
}