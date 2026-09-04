package rearth.oritech.spaceage.simulation;

/** Calculates the shared performance values used by launch checks and flight previews. */
public final class RocketPerformanceCalculator {

    public static final double STANDARD_GRAVITY = 9.80665;
    public static final int TICKS_PER_SECOND = 20;
    public static final int LAUNCH_ORBIT_HEIGHT_BLOCKS = 1_000;

    private static final double KILOGRAMS_PER_WEIGHT_UNIT = 1_000;
    private static final double ENGINE_THRUST_NEWTONS = 250_000;
    private static final double ENGINE_SPECIFIC_IMPULSE_SECONDS = 300;
    private static final long RF_PER_ENGINE_TICK = 1_000;

    private RocketPerformanceCalculator() {
    }

    // The overview calls this for the complete rocket, while the path calculator calls it once per segment so
    // resources cannot move between stages. Supporting both keeps the underlying engine rules identical.
    public static RocketPerformance calculate(ActiveRocketData rocket) {
        long dryWeight = 0;
        long fuelWeight = 0;
        long fuelBurnTicks = 0;
        long availableRF = 0;
        int engineCount = 0;

        for (var entry : rocket.getStaticSegments().entrySet()) {
            var dynamicSegment = rocket.getDynamicSegments().get(entry.getKey());
            var staticSegment = entry.getValue();
            dryWeight += Math.max(0, staticSegment.staticWeight());
            fuelWeight += Math.max(0, dynamicSegment.currentFuelWeight);
            fuelBurnTicks += Math.max(0, dynamicSegment.availableFuelBurnTimeTicks);
            availableRF += Math.max(0, dynamicSegment.availableRF);
            engineCount += Math.max(0, staticSegment.engineCount());
        }

        var dryMass = dryWeight * KILOGRAMS_PER_WEIGHT_UNIT;
        var fuelMass = fuelWeight * KILOGRAMS_PER_WEIGHT_UNIT;
        var wetMass = dryMass + fuelMass;
        var thrust = engineCount * ENGINE_THRUST_NEWTONS;

        var fuelBurnSeconds = engineCount == 0 ? 0
                : fuelBurnTicks / (double) engineCount / TICKS_PER_SECOND;
        var electricBurnSeconds = engineCount == 0 ? 0
                : availableRF / (double) RF_PER_ENGINE_TICK / engineCount / TICKS_PER_SECOND;

        var chemicalDeltaV = dryMass > 0 && fuelMass > 0 && fuelBurnSeconds > 0
                ? ENGINE_SPECIFIC_IMPULSE_SECONDS * STANDARD_GRAVITY * Math.log(wetMass / dryMass) : 0;
        // RF does not add fuel mass, so its contribution uses the constant dry mass.
        var electricDeltaV = dryMass > 0 ? thrust / dryMass * electricBurnSeconds : 0;
        var liftoffAcceleration = wetMass > 0 ? thrust / wetMass : 0;

        return new RocketPerformance(dryMass, fuelMass, wetMass, engineCount, thrust,
                fuelBurnSeconds + electricBurnSeconds, chemicalDeltaV + electricDeltaV, liftoffAcceleration);
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
