package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// a rocket consists of one or more segments. Splitting creates another ActiveRocketData containing one connected
// component of this segment graph; docking can merge two graphs again
public class ActiveRocketData {

    private static final Codec<Map<UUID, StaticRocketSegment>> STATIC_SEGMENTS_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, StaticRocketSegment.CODEC);
    private static final Codec<Map<UUID, DynamicRocketSegment>> DYNAMIC_SEGMENTS_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, DynamicRocketSegment.CODEC);

    public static final Codec<ActiveRocketData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("rocket_id").forGetter(ActiveRocketData::getRocketId),
            STATIC_SEGMENTS_CODEC.fieldOf("static_segments").forGetter(ActiveRocketData::getStaticSegments),
            DYNAMIC_SEGMENTS_CODEC.fieldOf("dynamic_segments").forGetter(ActiveRocketData::getDynamicSegments),
            RocketFlight.CODEC.optionalFieldOf("flight").forGetter(rocket -> Optional.ofNullable(rocket.flight))
    ).apply(instance, (rocketId, staticSegments, dynamicSegments, flight) ->
            new ActiveRocketData(rocketId, staticSegments, dynamicSegments, flight.orElse(null))));

    private final UUID rocketId;
    private final Map<UUID, StaticRocketSegment> staticSegments = new HashMap<>();
    private final Map<UUID, DynamicRocketSegment> dynamicSegments = new HashMap<>();
    private RocketFlight flight;

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

    public UUID getRocketId() {
        return rocketId;
    }

    public Map<UUID, StaticRocketSegment> getStaticSegments() {
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
