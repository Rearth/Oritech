package rearth.oritech.spaceage.simulation.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.List;
import java.util.UUID;

/** A fixed position in the solar-system plane, owned by a navigation card. */
public record NavigationWaypoint(double x, double y) {

    public static final Codec<NavigationWaypoint> CODEC = RecordCodecBuilder.<NavigationWaypoint>create(i -> i.group(
            Codec.DOUBLE.fieldOf("x").forGetter(NavigationWaypoint::x),
            Codec.DOUBLE.fieldOf("y").forGetter(NavigationWaypoint::y)
    ).apply(i, NavigationWaypoint::new)).validate(point -> Float.isFinite((float) point.x) && Float.isFinite((float) point.y)
            ? DataResult.success(point) : DataResult.error(() -> "Waypoint coordinates must be finite"));

    public SpaceObjectData point(UUID id) {

        return new SpaceObjectData(id, SpaceObjects.ObjectType.WAYPOINT, (float) x, (float) y, 0, 0, SpaceObjects.DetectionState.PRECISE);
    }

    public static List<SpaceObjectData> targets(SpaceSimulation.FlightPlan plan) {

        return plan.branches().stream().flatMap(branch -> branch.actions().stream())
                .filter(action -> action.type() == SpaceSimulation.ActionType.NAVIGATE_TO && action.targetId().equals(action.id()))
                .flatMap(action -> action.settings().waypoint().stream().map(point -> point.point(action.id()))).toList();
    }
}
