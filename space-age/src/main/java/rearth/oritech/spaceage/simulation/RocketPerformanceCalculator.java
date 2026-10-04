package rearth.oritech.spaceage.simulation;

/** Calculates the shared performance values used by launch checks and flight previews. */
public final class RocketPerformanceCalculator {

    public static final double STANDARD_GRAVITY = 9.80665;
    public static final int TICKS_PER_SECOND = 20;
    public static final int LAUNCH_ORBIT_HEIGHT_BLOCKS = 1_000;

    private static final double KILOGRAMS_PER_WEIGHT_UNIT = 1_000;
    static final double ENGINE_THRUST_NEWTONS = 250_000;

    private RocketPerformanceCalculator() {
    }

    // The overview calls this for the complete rocket, while the path calculator calls it once per segment so
    // resources cannot move between stages. Supporting both keeps the underlying engine rules identical.
    public static RocketPerformance calculate(ActiveRocketData rocket) {
        double dryMass = 0, fuelMass = 0, thrust = 0, launchThrust = 0, deltaV = 0, duration = 0;
        int engines = 0;
        for (var entry : rocket.getStaticSegments().entrySet()) {
            var segment = entry.getValue();
            var resources = rocket.getDynamicSegments().get(entry.getKey());
            var hardware = RocketHardware.of(segment);
            var dry = segment.staticWeight() * KILOGRAMS_PER_WEIGHT_UNIT;
            var fuel = resources.currentFuelWeight * KILOGRAMS_PER_WEIGHT_UNIT;
            var wet = Math.max(1, dry + fuel);
            var chemicalTime = hardware.chemicalSeconds(resources);
            var ionTime = hardware.ionSeconds(resources);
            var chemicalThrust = chemicalTime > 0 ? hardware.chemical() * ENGINE_THRUST_NEWTONS : 0;
            var ionThrust = ionTime > 0 ? hardware.ion() * ENGINE_THRUST_NEWTONS : 0;
            dryMass += dry;
            fuelMass += fuel;
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

    public static LaunchReadiness getLaunchReadiness(ActiveRocketData rocket, SpaceSimulation.FlightPlan plan) {
        var all = calculate(rocket);
        double thrust = 0, burnSeconds = 0, deltaV = 0;
        int engines = 0;
        for (var entry : rocket.getStaticSegments().entrySet()) {
            if (!plan.configurationFor(SpaceSimulation.SegmentRef.of(entry.getValue())).usesEnginesDuring(1)) continue;
            var resources = rocket.getDynamicSegments().get(entry.getKey());
            var hardware = RocketHardware.of(entry.getValue());
            var chemicalTime = hardware.chemicalSeconds(resources);
            var ionTime = hardware.ionSeconds(resources);
            var chemicalThrust = chemicalTime > 0 ? hardware.chemical() * ENGINE_THRUST_NEWTONS : 0;
            var ionThrust = ionTime > 0 ? hardware.ion() * ENGINE_THRUST_NEWTONS * SpaceBalance.ION_ATMOSPHERE : 0;
            engines += hardware.chemical() + hardware.ion();
            thrust += chemicalThrust + ionThrust;
            burnSeconds = Math.max(burnSeconds, Math.max(chemicalTime, ionTime));
            deltaV += chemicalThrust * chemicalTime + ionThrust * ionTime;
        }
        var mass = Math.max(1, all.wetMassKilograms());
        return getLaunchReadiness(new RocketPerformance(all.dryMassKilograms(), all.fuelMassKilograms(), all.wetMassKilograms(),
                engines, thrust, burnSeconds, deltaV / mass, thrust / mass));
    }

    public static LaunchReadiness getLaunchReadiness(ActiveRocketData rocket) {
        return getLaunchReadiness(calculate(rocket));
    }

    public static LaunchReadiness getLaunchReadiness(RocketPerformance performance) {
        if (performance.engineCount() == 0) return LaunchReadiness.NO_ENGINES;
        if (performance.wetMassKilograms() <= 0) return LaunchReadiness.NO_MASS;
        if (performance.liftoffAccelerationMetersPerSecondSquared() <= STANDARD_GRAVITY) {
            return LaunchReadiness.INSUFFICIENT_THRUST;
        }

        var netAcceleration = performance.liftoffAccelerationMetersPerSecondSquared() - STANDARD_GRAVITY;
        var ascentSeconds = Math.sqrt(2 * LAUNCH_ORBIT_HEIGHT_BLOCKS / netAcceleration);
        var requiredDeltaV = performance.liftoffAccelerationMetersPerSecondSquared() * ascentSeconds;
        if (performance.availableBurnSeconds() < ascentSeconds
                || performance.availableDeltaVMetersPerSecond() < requiredDeltaV) {
            return LaunchReadiness.INSUFFICIENT_FUEL;
        }
        return LaunchReadiness.READY;
    }

    public enum LaunchReadiness {
        READY(null),
        NO_ENGINES("Rocket has no engines"),
        NO_MASS("Rocket has no measurable mass"),
        INSUFFICIENT_THRUST("Rocket does not have enough thrust to lift off"),
        INSUFFICIENT_FUEL("Rocket does not have enough fuel to reach orbit");

        private final String failureReason;

        LaunchReadiness(String failureReason) {
            this.failureReason = failureReason;
        }

        public String failureReason() {
            return failureReason;
        }
    }
}
