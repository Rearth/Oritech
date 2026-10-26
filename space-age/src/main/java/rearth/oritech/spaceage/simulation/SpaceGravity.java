package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

/** Shared gravity boundaries for flight guidance, orbit labels and zero-g processing. */
public final class SpaceGravity {

    private SpaceGravity() {
    }

    public static double range(SpaceObjectData body) {

        return switch (body.type()) {
            case SUN -> body.radius() + OrbitBand.SOLAR.altitude();
            case EARTH, MARS -> body.radius() + OrbitBand.HIGH.altitude();
            default -> Double.POSITIVE_INFINITY;
        };
    }

    public static double acceleration(SpaceObjectData body, double distance) {

        if (body.radius() <= 0 || distance >= range(body) - 1e-6) return 0;
        var radius = (double) body.radius();
        var boundedDistance = Math.max(body.radius(), distance);
        return body.surfaceGravity() * radius * radius / (boundedDistance * boundedDistance);
    }

    public static double potential(SpaceObjectData body, double distance) {

        if (body.radius() <= 0) return 0;
        var radius = (double) body.radius();
        var inverseDistance = Math.max(0, 1 / Math.max(body.radius(), distance) - 1 / range(body));
        return body.surfaceGravity() * radius * radius * inverseDistance;
    }
}
