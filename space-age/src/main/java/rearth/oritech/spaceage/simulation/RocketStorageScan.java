package rearth.oritech.spaceage.simulation;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * One launch scan owns aliases across all segments. Probing never commits world changes.
 */
public final class RocketStorageScan {

    private final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<FluidResource, Double> fuels;
    public RocketStorageScan(Map<FluidResource, Double> fuels) {

        this.fuels = Map.copyOf(fuels);
    }

    public Budget probe(ResourceHandler<FluidResource> fluids, EnergyHandler energy, Transaction transaction) {

        long fuel = 0;
        long fuelCapacity = 0;
        var fuelWeight = 0d;
        long rf = 0;
        long rfCapacity = 0;
        if (fluids != null && seen.add(fluids)) {
            for (int tank = 0; tank < fluids.size(); tank++) {
                double capacity = 0;
                for (var entry : fuels.entrySet())
                    capacity = Math.max(capacity, fluids.getCapacityAsLong(tank, entry.getKey()) * entry.getValue());
                fuelCapacity += (long) capacity;
            }
            // Extracting in a rollback transaction also avoids double-counting shared backing stores.
            for (var entry : fuels.entrySet()) {
                for (int probe = 0; probe < 4096; probe++) {
                    var amount = fluids.extract(entry.getKey(), Integer.MAX_VALUE, transaction);
                    if (amount == 0) break;
                    fuel += (long) (amount * entry.getValue());
                    // Burn duration measures energy, not mass. Better fuels must not become heavier.
                    var buckets = amount / (double) FluidType.BUCKET_VOLUME;
                    var density = Math.max(0, entry.getKey().getFluidType().getDensity()) / 1000d;
                    fuelWeight += buckets * density;
                }
            }
        }
        if (energy != null && seen.add(energy)) {
            rfCapacity = energy.getCapacityAsLong();
            for (int probe = 0; probe < 4096; probe++) {
                var amount = energy.extract(Integer.MAX_VALUE, transaction);
                if (amount == 0) break;
                rf += amount;
            }
        }
        return new Budget(fuel, Math.max(fuel, fuelCapacity), rf, Math.max(rf, rfCapacity), fuelWeight);
    }

    public record Budget(long fuel, long fuelCapacity, long rf, long rfCapacity, double fuelWeight) {

    }
}
