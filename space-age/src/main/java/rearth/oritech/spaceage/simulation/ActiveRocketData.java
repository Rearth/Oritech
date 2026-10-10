package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// a rocket consists of one or more segments. Splitting creates another ActiveRocketData containing one connected
// component of this segment graph. Docked craft keep separate ownership.
public class ActiveRocketData {

    private static final Codec<Map<UUID, StaticRocketSegment>> STATIC_SEGMENTS_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, StaticRocketSegment.CODEC);
    private static final Codec<Map<UUID, DynamicRocketSegment>> DYNAMIC_SEGMENTS_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, DynamicRocketSegment.CODEC);

    public static final Codec<ActiveRocketData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("rocket_id").forGetter(ActiveRocketData::getRocketId),
            STATIC_SEGMENTS_CODEC.fieldOf("static_segments").forGetter(ActiveRocketData::getOriginalSegments),
            DYNAMIC_SEGMENTS_CODEC.fieldOf("dynamic_segments").forGetter(ActiveRocketData::getDynamicSegments),
            BlockPos.CODEC.optionalFieldOf("launch_position", BlockPos.ZERO).forGetter(ActiveRocketData::getLaunchPosition),
            RocketFlight.CODEC.optionalFieldOf("flight").forGetter(rocket -> Optional.ofNullable(rocket.flight))
    ).apply(instance, (rocketId, staticSegments, dynamicSegments, launch, flight) -> {
        var rocket = new ActiveRocketData(rocketId, staticSegments, dynamicSegments, flight.orElse(null));
        rocket.setLaunchPosition(launch);
        return rocket;
    }));

    private final UUID rocketId;
    private final Map<UUID, StaticRocketSegment> staticSegments = new HashMap<>();
    private final Map<UUID, DynamicRocketSegment> dynamicSegments = new HashMap<>();
    private RocketFlight flight;
    private BlockPos launchPosition = BlockPos.ZERO;

    public ActiveRocketData(Map<UUID, StaticRocketSegment> staticSegments, Map<UUID, DynamicRocketSegment> dynamicSegments) {

        this(UUID.randomUUID(), staticSegments, dynamicSegments, null);
    }

    public ActiveRocketData(UUID rocketId, Map<UUID, StaticRocketSegment> staticSegments,
                            Map<UUID, DynamicRocketSegment> dynamicSegments, RocketFlight flight) {

        if (!staticSegments.keySet().equals(dynamicSegments.keySet())) {
            throw new IllegalArgumentException("Static and dynamic rocket segments must have matching IDs");
        }

        this.rocketId = rocketId;
        this.staticSegments.putAll(staticSegments);
        this.dynamicSegments.putAll(dynamicSegments);
        this.flight = flight;
    }

    public BlockPos getLaunchPosition() {

        return launchPosition;
    }

    public void setLaunchPosition(BlockPos position) {

        launchPosition = position.immutable();
    }

    public UUID getRocketId() {

        return rocketId;
    }

    public Map<UUID, StaticRocketSegment> getStaticSegments() {

        if (dynamicSegments.values().stream().allMatch(segment -> segment.layout.isEmpty()))
            return getOriginalSegments();
        var current = new HashMap<UUID, StaticRocketSegment>();
        staticSegments.forEach((id, segment) -> current.put(id, RocketLayout.current(segment, dynamicSegments.get(id))));
        return Collections.unmodifiableMap(current);
    }

    public Map<UUID, StaticRocketSegment> getOriginalSegments() {

        return Collections.unmodifiableMap(staticSegments);
    }

    public Map<UUID, DynamicRocketSegment> getDynamicSegments() {

        return Collections.unmodifiableMap(dynamicSegments);
    }

    public RocketFlight getFlight() {

        return flight;
    }

    public void setFlight(RocketFlight flight) {

        this.flight = flight;
    }
}
