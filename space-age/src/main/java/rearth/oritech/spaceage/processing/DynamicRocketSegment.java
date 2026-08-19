package rearth.oritech.spaceage.processing;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// this part may change during the flight of the rocket, and is specific to this specific part (e.g. center, booster, etc).
public class DynamicRocketSegment {

    public long availableFuelBurnTimeTicks;
    public long availableRF;
    public long currentFuelWeight;

    // if a segment is connected, all couplings to it are still coupled/connected, and vice versa
    private final Set<UUID> connectedSegments = new HashSet<>();

    public DynamicRocketSegment(long availableFuelBurnTimeTicks, long availableRF, long currentFuelWeight, Set<UUID> connectedSegments) {
        this.availableFuelBurnTimeTicks = availableFuelBurnTimeTicks;
        this.availableRF = availableRF;
        this.currentFuelWeight = currentFuelWeight;
        this.connectedSegments.addAll(connectedSegments);
    }

    public Set<UUID> getConnectedSegments() {
        return connectedSegments;
    }

    public boolean isSegmentConnected(UUID segmentId) {
        return connectedSegments.contains(segmentId);
    }

    public void removeConnection(UUID segmentId) {
        connectedSegments.remove(segmentId);
    }
}
