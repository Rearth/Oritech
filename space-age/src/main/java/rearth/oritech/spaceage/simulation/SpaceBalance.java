package rearth.oritech.spaceage.simulation;

/** Prototype balance in simulation ticks and map metres. */
public final class SpaceBalance {
    public static final double SURFACE_BLOCKS_PER_CIRCLE = 2_000;
    public static final double TARGET_ANGLE_SPREAD = Math.toRadians(25);
    // Stable stopped-departure jitter; surface normals and moving departures take precedence.
    public static final double STOPPED_DEPARTURE_ANGLE_MIN = Math.toRadians(12);
    public static final double STOPPED_DEPARTURE_ANGLE_MAX = Math.toRadians(20);
    // Fraction of trip distance used to separate an otherwise collinear turnaround.
    public static final double REVERSAL_STEERING_RATIO = 0.025;
    // Steering is a small fuel surcharge; it must not turn an otherwise launch-capable rocket into one below local gravity.
    public static final double CURVE_STEERING_COST = 0.002;

    public static double surfaceAngle(double x, double z) {
        return Math.PI * 2 * ((x + z) % SURFACE_BLOCKS_PER_CIRCLE) / SURFACE_BLOCKS_PER_CIRCLE;
    }
    public static final int DAY = 24_000;
    public static final double SCAN_RANGE = 250_000;
    /** One instant survey sweep, below a default portable battery (1 MRF). */
    public static final long SCAN_RF = 600_000;
    public static final long ANTENNA_RF = 10;
    public static final long ION_RF = 1_000;
    public static final double ION_ATMOSPHERE = 0.1;
    public static final double ANTENNA_RANGE = 300_000;
    public static final double GROUND_COMMAND_ALTITUDE = 10_000;
    public static final double LOW_COMMAND_ALTITUDE = 750_000;
    // Station keeping is deliberately a small, fixed delta-v sink. Communication power should normally run out first.
    public static final double STATION_KEEPING_DELTA_V_PER_DAY = 100;
    public static final int STATION_KEEPING_INTERVAL_TICKS = 200;

    public static double stationKeepingBurnSecondsPerSecond(double thrustNewtons, double massKilograms) {
        if (thrustNewtons <= 0 || massKilograms <= 0) return 0;
        var acceleration = thrustNewtons / massKilograms;
        var correctionDeltaVPerSecond = STATION_KEEPING_DELTA_V_PER_DAY * 20 / DAY;
        return correctionDeltaVPerSecond / acceleration;
    }
    public static boolean hasSlots(SpaceSimulation.OrbitBand orbit) { return slots(orbit) > 0; }
    public static int slots(SpaceSimulation.OrbitBand orbit) {
        return switch (orbit) { case LOW -> 6; case HIGH -> 36; default -> 0; };
    }
    private SpaceBalance() { }
}
