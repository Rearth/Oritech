package rearth.oritech.util;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

public class DynamicEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
    public long amount = 0;
    public long capacity;
    public long maxInsert, maxExtract;
    
    public DynamicEnergyStorage(long capacity, long maxInsert, long maxExtract) {
        StoragePreconditions.notNegative(capacity);
        StoragePreconditions.notNegative(maxInsert);
        StoragePreconditions.notNegative(maxExtract);
        
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
    }
    
    @Override
    protected Long createSnapshot() {
        return amount;
    }
    
    @Override
    protected void readSnapshot(Long snapshot) {
        amount = snapshot;
    }
    
    @Override
    public boolean supportsInsertion() {
        return maxInsert > 0;
    }
    
    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        
        long inserted = Math.min(maxInsert, Math.min(maxAmount, capacity - amount));
        
        if (inserted > 0) {
            updateSnapshots(transaction);
            amount += inserted;
            return inserted;
        }
        
        return 0;
    }
    
    @Override
    public boolean supportsExtraction() {
        return maxExtract > 0;
    }
    
    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        
        long extracted = Math.min(maxExtract, Math.min(maxAmount, amount));
        
        if (extracted > 0) {
            updateSnapshots(transaction);
            amount -= extracted;
            return extracted;
        }
        
        return 0;
    }
    
    @Override
    public long getAmount() {
        return amount;
    }
    
    @Override
    public long getCapacity() {
        return capacity;
    }
}