package rearth.oritech.spaceage.simulation.flight;

import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;

/**
 * Mass-scaled braking credit for parachutes deployed along the normal landing curve.
 */
public final class ParachuteLanding {

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

        return SpaceBalance.PARACHUTE_DELTA_V * count * SpaceBalance.PARACHUTE_REFERENCE_MASS / Math.max(1, mass);
    }

    static double fuelFactor(double credit, double brakingDeltaV) {

        return brakingDeltaV <= 0 ? 1 : Math.max(0, 1 - credit / brakingDeltaV);
    }

    public record Result(int parachutes, double mass, double deltaVReduction, double brakingFuelFactor) {

        public static final Result NONE = new Result(0, 0, 0, 1);
    }
}
