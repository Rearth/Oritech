package rearth.oritech.spaceage.simulation.rocket;

import net.minecraft.core.BlockPos;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Folded cube-sats carried by a rocket. Their segments are part of the host's flight plan (deploy branches, stages,
 * names), but never part of the host's physics until they are deployed.
 */
public final class RocketPayloads {

    private RocketPayloads() {
    }

    /** All carried cubes by their cell, in the host's local coordinates. */
    public static Map<BlockPos, CubeSatData> all(ActiveRocketData rocket) {

        var result = new LinkedHashMap<BlockPos, CubeSatData>();
        rocket.getDynamicSegments().values().forEach(segment -> result.putAll(segment.payloads));
        return result;
    }

    /** The cube's rocket as it appears in the host's plan. Its IDs and plan anchors are stable for the cell. */
    public static ActiveRocketData unfold(BlockPos payload, CubeSatData cube) {

        var id = UUID.nameUUIDFromBytes(("payload/" + payload.asLong()).getBytes(StandardCharsets.UTF_8));
        return cube.unfold(id, payload);
    }

    /**
     * The host's segments plus the unfolded segments of all carried cubes, used only to resolve plan references.
     * Cube segments are not connected to the host.
     */
    public static ActiveRocketData planView(ActiveRocketData rocket) {

        var payloads = all(rocket);
        if (payloads.isEmpty()) return rocket;
        var structures = new HashMap<>(rocket.getOriginalSegments());
        var resources = new HashMap<>(rocket.getDynamicSegments());
        payloads.forEach((pos, cube) -> {
            var unfolded = unfold(pos, cube);
            structures.putAll(unfolded.getOriginalSegments());
            resources.putAll(unfolded.getDynamicSegments());
        });
        return new ActiveRocketData(rocket.getRocketId(), structures, resources, null);
    }

    /** Plan references of the segments inside one carried cube. */
    public static Set<SegmentRef> segments(BlockPos payload, CubeSatData cube) {

        return unfold(payload, cube).getStaticSegments().values().stream().map(SegmentRef::of).collect(Collectors.toSet());
    }

    /** Plan references of the segments inside all carried cubes. */
    public static Set<SegmentRef> segments(ActiveRocketData rocket) {

        var result = new HashSet<SegmentRef>();
        all(rocket).forEach((pos, cube) -> result.addAll(segments(pos, cube)));
        return result;
    }

    /** The carried cube owning a payload segment reference, or null for the host's own segments. */
    public static Map.Entry<BlockPos, CubeSatData> owner(ActiveRocketData rocket, SegmentRef ref) {

        for (var entry : all(rocket).entrySet()) {
            var unfolded = unfold(entry.getKey(), entry.getValue());
            if (unfolded.getStaticSegments().values().stream().anyMatch(segment -> SegmentRef.of(segment).equals(ref))) return entry;
        }
        return null;
    }
}
