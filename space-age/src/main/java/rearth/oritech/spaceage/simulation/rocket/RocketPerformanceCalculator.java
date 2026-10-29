package rearth.oritech.spaceage.simulation.rocket;

import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.util.function.Predicate;

/**
 * Calculates the shared performance values used by launch checks and flight previews.
 */
public final class RocketPerformanceCalculator {

    private RocketPerformanceCalculator() {
    }

    // A static rating for the overview and liftoff checks. The flight solver accounts for staging and charging.
    public static RocketPerformance calculate(ActiveRocketData rocket) {

        return calculate(rocket, segment -> true);
    }

    public static RocketPerformance calculate(ActiveRocketData rocket, FlightPlan plan, int stage) {

        return calculate(rocket, segment -> plan.configurationFor(SegmentRef.of(segment)).usesEnginesDuring(stage));
    }

    private static RocketPerformance calculate(ActiveRocketData rocket, Predicate<StaticRocketSegment> active) {

        double dryMass = 0;
        double fuelMass = 0;
        double thrust = 0;
        double launchThrust = 0;
        double deltaV = 0;
        double duration = 0;
        var engines = 0;
        for (var entry : rocket.getStaticSegments().entrySet()) {
            var segment = entry.getValue();
            var resources = rocket.getDynamicSegments().get(entry.getKey());
            var hardware = RocketHardware.of(segment);
            var dry = segment.staticWeight() * SpaceBalance.KILOGRAMS_PER_WEIGHT_UNIT;
            var fuel = resources.currentFuelWeight * SpaceBalance.KILOGRAMS_PER_WEIGHT_UNIT;
            dryMass += dry;
            fuelMass += fuel;
            if (!active.test(segment)) continue;
            var chemicalTime = hardware.chemicalSeconds(resources);
            var ionTime = hardware.ionSeconds(resources);
            var chemicalThrust = chemicalTime > 0 ? hardware.chemicalThrust() : 0;
            var ionThrust = ionTime > 0 ? hardware.ionThrust() : 0;
            thrust += chemicalThrust + ionThrust;
            launchThrust += chemicalThrust + ionThrust * SpaceBalance.ION_ATMOSPHERE;
            engines += hardware.chemical() + hardware.ion();
            duration = Math.max(duration, Math.max(chemicalTime, ionTime));
            deltaV += chemicalThrust * chemicalTime + ionThrust * ionTime;
        }
        var mass = dryMass + fuelMass;
        return new RocketPerformance(dryMass, fuelMass, mass, engines, thrust, duration,
                mass > 0 ? deltaV / mass : 0, mass > 0 ? launchThrust / mass : 0);
    }

    public static LaunchReadiness getLaunchReadiness(ActiveRocketData rocket, FlightPlan plan) {

        return getLaunchReadiness(calculate(rocket, plan, 1));
    }

    public static LaunchReadiness getLaunchReadiness(ActiveRocketData rocket) {

        return getLaunchReadiness(calculate(rocket));
    }

    public static LaunchReadiness getLaunchReadiness(RocketPerformance performance) {

        if (performance.engineCount() == 0) return LaunchReadiness.NO_ENGINES;
        if (performance.wetMassKilograms() <= 0) return LaunchReadiness.NO_MASS;
        if (performance.liftoffAccelerationMetersPerSecondSquared() <= SpaceBalance.STANDARD_GRAVITY) {
            return LaunchReadiness.INSUFFICIENT_THRUST;
        }

        // This only checks liftoff. The shared flight compiler validates fuel, staging and arrival.
        return LaunchReadiness.READY;
    }

    public enum LaunchReadiness {

        READY(null),
        NO_ENGINES("Rocket has no engines"),
        NO_MASS("Rocket has no measurable mass"),
        INSUFFICIENT_THRUST("Rocket does not have enough thrust to lift off");

        private final String failureReason;

        LaunchReadiness(String failureReason) {

            this.failureReason = failureReason;
        }

        public String failureReason() {

            return failureReason;
        }
    }
}
