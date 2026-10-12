package rearth.oritech.spaceage.simulation;

import java.util.Map;

/**
 * Instant finite-pool exchange. Docking never grants access to the other craft's engine budgets.
 */
public final class RocketResourceTransfer {

    private RocketResourceTransfer() {
    }

    public static long move(ActiveRocketData source, ActiveRocketData destination, boolean fuel, long limit, long reserve) {

        var sources = source.getDynamicSegments().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
        var destinations = destination.getDynamicSegments().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
        var stored = sources.stream().mapToLong(s -> fuel ? s.availableFuelBurnTimeTicks : s.availableRF).sum();
        var room = destinations.stream().mapToLong(s -> Math.max(0, fuel ? s.fuelCapacity - s.availableFuelBurnTimeTicks : s.rfCapacity - s.availableRF)).sum();
        var remaining = Math.min(Math.min(Math.max(0, limit), Math.max(0, stored - reserve)), room);
        var moved = remaining;
        var movedFuelWeight = 0d;

        // Determine the complete move first. Anything that does not fit stays in the source pools.
        for (var segment : sources) {
            if (remaining == 0) break;
            var amount = Math.min(remaining, fuel ? segment.availableFuelBurnTimeTicks : segment.availableRF);
            if (fuel) {
                var previousWeight = segment.currentFuelWeight;
                segment.consumeFuel(amount);
                movedFuelWeight += previousWeight - segment.currentFuelWeight;
            } else segment.availableRF -= amount;
            remaining -= amount;
        }
        remaining = moved;
        for (var segment : destinations) {
            if (remaining == 0) break;
            var amount = Math.min(remaining, Math.max(0, fuel ? segment.fuelCapacity - segment.availableFuelBurnTimeTicks : segment.rfCapacity - segment.availableRF));
            if (fuel) {
                segment.availableFuelBurnTimeTicks += amount;
                // Transfer the moved mixture's mass with its burn budget, without retaining fluid types.
                segment.currentFuelWeight += movedFuelWeight * amount / moved;
            } else segment.availableRF += amount;
            remaining -= amount;
        }
        return moved;
    }
}
