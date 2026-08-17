package rearth.oritech.api.energy.containers;

import rearth.oritech.api.energy.EnergyApi;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * A read-only view over multiple energy storages.
 *
 * <p>Extraction is spread across the backing storages without moving their energy into an
 * intermediate buffer. Each backing storage keeps its own extraction limit, so the aggregate's
 * throughput is the sum of the throughput available from its members.</p>
 */
public class AggregatingEnergyStorage extends EnergyApi.EnergyStorage {

    private final List<? extends EnergyApi.EnergyStorage> storages;
    private final long[] remainingExtractable;
    private final Set<EnergyApi.EnergyStorage> changedStorages = Collections.newSetFromMap(new IdentityHashMap<>());
    private long amount;
    private long extractableAmount;
    private long capacity;
    private int extractionIndex;

    public AggregatingEnergyStorage(List<? extends EnergyApi.EnergyStorage> storages) {
        this.storages = List.copyOf(storages);
        this.remainingExtractable = new long[storages.size()];

        for (var i = 0; i < storages.size(); i++) {
            var storage = storages.get(i);
            amount = saturatedAdd(amount, storage.getAmount());
            capacity = saturatedAdd(capacity, storage.getCapacity());

            if (!storage.supportsExtraction()) continue;
            remainingExtractable[i] = storage.extract(Long.MAX_VALUE, true);
            extractableAmount = saturatedAdd(extractableAmount, remainingExtractable[i]);
        }
    }

    @Override
    public boolean supportsInsertion() {
        return false;
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        return 0;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        var requested = Math.min(maxAmount, extractableAmount);
        if (simulate) return requested;

        long extracted = 0;
        while (extracted < requested && extractionIndex < storages.size()) {
            var remainingFromStorage = remainingExtractable[extractionIndex];
            if (remainingFromStorage <= 0) {
                extractionIndex++;
                continue;
            }

            var storage = storages.get(extractionIndex);
            var toExtract = Math.min(requested - extracted, remainingFromStorage);
            var extractedFromStorage = storage.extract(toExtract, false);
            if (extractedFromStorage <= 0) {
                remainingExtractable[extractionIndex] = 0;
                extractionIndex++;
                continue;
            }

            remainingExtractable[extractionIndex] -= extractedFromStorage;
            extracted += extractedFromStorage;
            amount -= extractedFromStorage;
            extractableAmount -= extractedFromStorage;
            changedStorages.add(storage);

            if (extractedFromStorage < toExtract || remainingExtractable[extractionIndex] <= 0) {
                remainingExtractable[extractionIndex] = 0;
                extractionIndex++;
            }
        }

        return extracted;
    }

    @Override
    public void setAmount(long amount) {
        throw new UnsupportedOperationException("Cannot set the amount of an aggregating energy storage");
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
        changedStorages.forEach(EnergyApi.EnergyStorage::update);
        changedStorages.clear();
    }

    private static long saturatedAdd(long first, long second) {
        if (second > Long.MAX_VALUE - first) return Long.MAX_VALUE;
        return first + second;
    }
}
