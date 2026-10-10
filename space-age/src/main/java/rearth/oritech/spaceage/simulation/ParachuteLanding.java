package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Atmospheric drag only. The last few metres settle gently when terminal speed is safe.
 */
public final class ParachuteLanding {

    public static final double DEPLOYMENT_ALTITUDE = 1_000;
    public static final double MAX_DEPLOYMENT_SPEED = 200;
    public static final double CANOPY_AREA = 1_200;
    public static final double DRAG_COEFFICIENT = .75;
    public static final double AIR_DENSITY = 1.225;
    public static final double SAFE_SPEED = 12;

    private ParachuteLanding() {
    }

    public static int count(StaticRocketSegment segment) {

        return (int) segment.blocks().stream().filter(b -> b.state().is(SpaceAgeBlocks.PARACHUTE)).count();
    }

    public static double terminalSpeed(double mass, int count) {

        return count <= 0 ? Double.POSITIVE_INFINITY : Math.sqrt(2 * Math.max(1, mass)
                                                                 * RocketPerformanceCalculator.STANDARD_GRAVITY / (AIR_DENSITY * DRAG_COEFFICIENT * CANOPY_AREA * count));
    }

    public static boolean eligible(SpaceSimulation.FlightPlanAction action) {

        return action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == SpaceSimulation.OrbitBand.SURFACE
                && (action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.ZERO
                || action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM && action.targetVelocity() == 0);
    }

    public static List<Point> descent(double altitude, double speed, double mass, int count, double finalAltitude) {

        var points = new ArrayList<Point>();
        var terminal = terminalSpeed(mass, count);
        double time = 0;
        double nextSample = 0;
        points.add(new Point(0, altitude, speed));
        for (int step = 0; step < 100_000 && altitude > finalAltitude; step++) {
            var acceleration = RocketPerformanceCalculator.STANDARD_GRAVITY * (1 - speed * speed / (terminal * terminal));
            var nextSpeed = speed + acceleration * .05;
            // Bound the explicit step so drag cannot reverse descent or jump past equilibrium.
            nextSpeed = speed > terminal ? Math.max(terminal, nextSpeed) : Math.min(terminal, nextSpeed);
            var dt = Math.min(.05, (altitude - finalAltitude) / Math.max(.001, (speed + nextSpeed) / 2));
            altitude = Math.max(finalAltitude, altitude - (speed + nextSpeed) * .5 * dt);
            speed = nextSpeed;
            time += dt;
            if (time >= nextSample || altitude == finalAltitude) {
                points.add(new Point(time, altitude, speed));
                nextSample = time + 1;
            }
        }
        return List.copyOf(points);
    }

    public record Point(double seconds, double altitude, double speed) {

        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("seconds").forGetter(Point::seconds),
                Codec.DOUBLE.fieldOf("altitude").forGetter(Point::altitude),
                Codec.DOUBLE.fieldOf("speed").forGetter(Point::speed)
        ).apply(i, Point::new));
    }

    public record Result(int parachutes, double mass, double deploymentSpeed, double terminalSpeed,
                         double descentSeconds, double brakingDeltaV, double fuelSavedTicks, boolean sufficient) {

        public static final Result NONE = new Result(0, 0, 0, 0, 0, 0, 0, false);
    }
}
