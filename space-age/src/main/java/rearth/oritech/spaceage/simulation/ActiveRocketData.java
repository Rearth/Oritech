package rearth.oritech.spaceage.simulation;

import java.util.*;

// a rocket consists of one / multiple segments, each with a static and dynamic data part. Each segment has an ID and is connected to other segments via its
// internal id through couplings (one or multiple couplings).
// The internal id stored in the static part, and is also used to identify the dynamic part.
// A rocket may split / decouple. Each split will result in 1 new rocket data instance, and one being updated (the bigger one is updated accordingly).
public class ActiveRocketData {

    private final Map<UUID, StaticRocketSegment> staticSegments = new HashMap<>();
    private final Map<UUID, DynamicRocketSegment> dynamicSegments = new HashMap<>();

    public ActiveRocketData(Map<UUID, StaticRocketSegment> staticSegments, Map<UUID, DynamicRocketSegment> dynamicSegments) {
        if (!staticSegments.keySet().equals(dynamicSegments.keySet())) {
            throw new IllegalArgumentException("Static and dynamic rocket segments must have matching IDs");
        }

        this.staticSegments.putAll(staticSegments);
        this.dynamicSegments.putAll(dynamicSegments);
    }

    public Map<UUID, StaticRocketSegment> getStaticSegments() {
        return Collections.unmodifiableMap(staticSegments);
    }

    public Map<UUID, DynamicRocketSegment> getDynamicSegments() {
        return Collections.unmodifiableMap(dynamicSegments);
    }

}
