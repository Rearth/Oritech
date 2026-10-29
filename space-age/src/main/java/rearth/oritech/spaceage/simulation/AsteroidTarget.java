package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;

/**
 * Reusable asteroid selection. A zero radius limit is unrestricted.
 */
public record AsteroidTarget(boolean closest, int minRadius, int maxRadius) {

    public static final AsteroidTarget DEFAULT = new AsteroidTarget(false, 0, 0);
    public static final Codec<AsteroidTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("closest").forGetter(AsteroidTarget::closest),
            Codec.INT.fieldOf("min_radius").forGetter(AsteroidTarget::minRadius),
            Codec.INT.fieldOf("max_radius").forGetter(AsteroidTarget::maxRadius)
    ).apply(i, AsteroidTarget::new));

    public static FlightPlanAction resolve(FlightPlanAction action, Collection<SpaceObjectData> objects,
                                           double x, double y, double time, UUID currentTarget) {

        if (action.type() == ActionType.NAVIGATE_TO && action.asteroidTarget().closest()) {
            var selection = action.asteroidTarget();
            var target = objects.stream().filter(o -> selection.valid() && o.type() == SpaceObjects.ObjectType.ASTEROID
                            && o.radius() >= selection.minRadius() && (selection.maxRadius() == 0 || o.radius() <= selection.maxRadius())
                            && (action.orbit() != SpaceSimulation.OrbitBand.SURFACE || o.detectionState() == SpaceObjects.DetectionState.PRECISE))
                    .min(Comparator.comparingDouble((SpaceObjectData o) -> Math.hypot(o.xAt(time) - x, o.yAt(time) - y))
                            .thenComparing(SpaceObjectData::id)).orElse(null);
            return action.withTarget(target == null ? FlightPlanAction.NO_TARGET : target.id());
        }
        // Capture/release cards following a dynamic arrival refer to the asteroid actually reached.
        if (action.targetId().equals(FlightPlanAction.CLOSEST_ASTEROID)) return action.withTarget(currentTarget);
        return action;
    }

    public boolean valid() {

        return minRadius >= 0 && maxRadius >= 0 && (maxRadius == 0 || minRadius <= maxRadius);
    }
}
