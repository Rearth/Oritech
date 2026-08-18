package rearth.oritech.spaceage.processing;

import java.util.*;

// a rocket consists of one / multiple segments, each with a static and dynamic data part. Each segment has an ID and is connected to other segments via its
// internal id through couplings (one or multiple couplings).
// The internal id stored in the static part, and is also used to identify the dynamic part.
// A rocket may split / decouple. Each split will result in 1 new rocket data instance, and one being updated (the bigger one is updated accordingly).
public class ActiveRocketData {

    private final Map<UUID, StaticRocketSegment> staticSegments = new HashMap<>();
    private final Map<UUID, DynamicRocketSegment> dynamicSegments = new HashMap<>();

}
