package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.init.SpaceAgeBlocks;

/**
 * Mass-scaled braking credit for parachutes deployed along the normal landing curve.
 */
public final class ParachuteLanding {

    public static final double DELTA_V_PER_PARACHUTE = 200;
    public static final double REFERENCE_MASS = 8_000;

    private ParachuteLanding() {
    }

    public static int count(StaticRocketSegment segment) {

        return (int) segment.blocks().stream().filter(b -> b.state().is(SpaceAgeBlocks.PARACHUTE)).count();
    }

    public static boolean eligible(SpaceSimulation.FlightPlanAction action) {

        return action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == SpaceSimulation.OrbitBand.SURFACE
                && (action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.ZERO
                || action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM && action.targetVelocity() == 0);
    }

    public static double deltaVReduction(double mass, int count) {

        return DELTA_V_PER_PARACHUTE * count * REFERENCE_MASS / Math.max(1, mass);
    }

    static double fuelFactor(double credit, double brakingDeltaV) {

        return brakingDeltaV <= 0 ? 1 : Math.max(0, 1 - credit / brakingDeltaV);
    }

    public record Result(int parachutes, double mass, double deltaVReduction, double brakingFuelFactor) {

        public static final Result NONE = new Result(0, 0, 0, 1);
    }
}
