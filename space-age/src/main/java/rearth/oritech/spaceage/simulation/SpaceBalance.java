package rearth.oritech.spaceage.simulation;

import java.util.Set;
import java.util.UUID;

/**
 * Shared gameplay balance in simulation ticks, seconds, map metres and kilograms.
 * Server-configurable options live in SpaceAgeConfig; block ratings and recipes remain data-driven.
 */
public final class SpaceBalance {

    // Units and propulsion.
    /** Simulation ticks per second; unit conversion, not a gameplay speed setting. */
    public static final int TICKS_PER_SECOND = 20;
    /** Kilograms per scanned structural/fuel weight unit. Higher values make all rockets heavier. */
    public static final double KILOGRAMS_PER_WEIGHT_UNIT = 1_000;
    /** Earth gravity in m/s² used by physical ascent and the launch readiness estimate. */
    public static final double STANDARD_GRAVITY = 9.80665;
    /** Height in world blocks of the physical ascent visual and launch readiness estimate. */
    public static final int LAUNCH_ORBIT_HEIGHT_BLOCKS = 1_000;
    /** Full-power thrust in newtons per chemical engine, in vacuum and atmosphere. */
    public static final double CHEMICAL_THRUST_NEWTONS = 500_000;
    /** Full-power vacuum thrust in newtons per ion engine. */
    public static final double ION_THRUST_NEWTONS = 500_000;
    /** Stored fuel burn ticks spent per second per chemical engine; recipes determine burn ticks per mB. */
    public static final double CHEMICAL_FUEL_PER_SECOND = 40;
    /** RF per simulation tick per ion engine; 20 RF/t gives 2,500 seconds per 1 MRF, before radio use. */
    public static final long ION_RF = 20;
    /** Fraction of ion vacuum thrust available on atmospheric legs, with unchanged RF use. */
    public static final double ION_ATMOSPHERE = 0.1;
    /** Height of Earth's atmosphere in map metres, independent of orbit destinations. */
    public static final double ATMOSPHERE_HEIGHT = 10_000;
    /** Braking delta-v credit in m/s per parachute at the reference mass; reduces landing fuel/RF use. */
    public static final double PARACHUTE_DELTA_V = 200;
    /** Reference rocket mass in kg for parachute credit; credit scales inversely with actual mass. */
    public static final double PARACHUTE_REFERENCE_MASS = 8_000;
    /** Editable program cards available without navigation computers; each computer adds one. */
    public static final int BASE_NAVIGATION_STEPS = 3;

    // Navigation and station keeping.
    /** World X + Z blocks per full turn when projecting Earth landing coordinates onto the map. */
    public static final double SURFACE_BLOCKS_PER_CIRCLE = 2_000;
    /** Maximum deterministic angular offset in radians for non-surface target approach positions. */
    public static final double TARGET_ANGLE_SPREAD = Math.toRadians(25);
    /** Minimum departure-heading jitter in radians when starting stopped away from a surface. */
    public static final double STOPPED_DEPARTURE_ANGLE_MIN = Math.toRadians(12);
    /** Maximum departure-heading jitter in radians when starting stopped away from a surface. */
    public static final double STOPPED_DEPARTURE_ANGLE_MAX = Math.toRadians(20);
    /** Turnaround curve width relative to approach extent, including braking overshoots. */
    public static final double REVERSAL_STEERING_RATIO = 0.2;
    /** Steering fuel surcharge coefficient; larger values charge more burn time for curved transfers. */
    public static final double CURVE_STEERING_COST = 0.002;
    /** Simulation ticks per Minecraft day, used to express daily station-keeping costs. */
    public static final int DAY = 24_000;
    /** Maximum allowed duration in seconds of one navigation transfer. */
    public static final double MAX_TRANSFER_SECONDS = 1_200_000;
    /** Propulsive correction delta-v in m/s per day for non-solar station keeping. */
    public static final double STATION_KEEPING_DELTA_V_PER_DAY = 100;
    /** Simulation ticks between live station-keeping correction burns. */
    public static final int STATION_KEEPING_INTERVAL_TICKS = 200;
    /** Number of Earth low-orbit slots needed for the complete low communications ring. */
    public static final int LOW_ORBIT_SLOTS = 6;
    /** Number of Earth high-orbit slots needed, with the low ring, for system-wide communication. */
    public static final int HIGH_ORBIT_SLOTS = 36;
    /** Maximum number of installations in the Sun's solar collector ring. */
    public static final int SOLAR_ORBIT_SLOTS = 200;

    // Surveys and communication.
    /** Maximum survey distance in map metres; regions measure from their boundary. */
    public static final double SCAN_RANGE = 250_000;
    /** RF spent once per instant survey sweep, independent of installed scanner count. */
    public static final long SCAN_RF = 600_000;
    /** RF per simulation tick per powered spacecraft antenna. */
    public static final long ANTENNA_RF = 10;
    /** Maximum distance in map metres for direct observation sharing between powered craft. */
    public static final double ANTENNA_RANGE = 300_000;
    /** Earth altitude in metres covered by powered ground arrays without an orbital relay ring. */
    public static final double GROUND_COMMAND_ALTITUDE = 10_000;
    /** Earth altitude in metres covered after completing the low-orbit relay ring. */
    public static final double LOW_COMMAND_ALTITUDE = 750_000;

    // Solar generation and beam transmission. Per-block ratings remain in the solar/optical data maps.
    /** Sun distance in metres where space solar panels produce their data-map RF/t rating. */
    public static final double SOLAR_REFERENCE_DISTANCE = 3_000_000;
    /** Minimum solar distance in metres for inverse-square output, also the collector-ring radius. */
    public static final double SOLAR_MIN_DISTANCE = 500_000;
    /** Aperture RF/t throughput per unit of summed optical glass quality. */
    public static final double BEAM_RF_PER_GLASS = 10_000;
    /** Metres of travel per aperture focus block per block of beam half-width growth. */
    public static final double BEAM_METRES_PER_FOCUS = 100_000;
    /** Multiplier on accumulated beam spread when delivering through the atmosphere. */
    public static final double BEAM_ATMOSPHERIC_SPREAD = 10;
    /** Minimum beam footprint half-width in blocks for spacecraft receivers. */
    public static final double BEAM_MIN_SPACE_HALF_WIDTH = 1;
    /** Minimum beam footprint half-width in blocks for ground receivers. */
    public static final double BEAM_MIN_SURFACE_HALF_WIDTH = 4;

    // Impacts and asteroid handling.
    /** Joules per TNT block when accounting for explosive payload energy. */
    public static final double TNT_ENERGY_JOULES = 4_184_000;
    /** Impact/explosion joules per stored chemical burn tick; does not set engine output or consumption. */
    public static final double FUEL_ENERGY_PER_BURN_TICK = 20_000;
    /** Kilograms per asteroid mass unit stored in solar-system object data. */
    public static final double KILOGRAMS_PER_ASTEROID_MASS = 1_000;
    /** Exponential velocity damping coefficient per second for released asteroids. */
    public static final double SPACE_DRAG_PER_SECOND = 0.000002;
    /** Maximum relative arrival speed in m/s for connecting to an asteroid or an unladen safe landing. */
    public static final double MAX_ASTEROID_CONNECTION_SPEED = 12;
    /** Maximum safe landing speed in m/s while carrying an asteroid. */
    public static final double SAFE_ASTEROID_SPEED = 6;
    /** Impact joules per kilogram used to excavate asteroid chips. */
    public static final double ASTEROID_CHIP_ENERGY_PER_KILOGRAM = 3_000;
    /** Reference minimum excavated mass in kg; small asteroids use a fraction of their mass instead. */
    public static final double MIN_CHIPPED_MASS_KILOGRAMS = 8_000;
    /** Impact joules per target kilogram at which an asteroid breaks apart catastrophically. */
    public static final double ASTEROID_BREAKUP_ENERGY_PER_KILOGRAM = 180_000;
    /** Joules required for an unladen unsafe landing to create a heavy impact and crater. */
    public static final double HEAVY_IMPACT_ENERGY = 100_000_000;

    private SpaceBalance() {
    }

    public static double surfaceAngle(double x, double z) {

        return Math.PI * 2 * ((x + z) % SURFACE_BLOCKS_PER_CIRCLE) / SURFACE_BLOCKS_PER_CIRCLE;
    }

    public static double stationKeepingBurnSecondsPerSecond(double thrustNewtons, double massKilograms) {

        if (thrustNewtons <= 0 || massKilograms <= 0) return 0;
        var acceleration = thrustNewtons / massKilograms;
        var correctionDeltaVPerSecond = STATION_KEEPING_DELTA_V_PER_DAY * TICKS_PER_SECOND / DAY;
        return correctionDeltaVPerSecond / acceleration;
    }

    public static int slots(UUID body, SpaceSimulation.OrbitBand orbit) {

        if (body.equals(SpaceSimulation.SUN_ID)) return orbit == SpaceSimulation.OrbitBand.SOLAR ? SOLAR_ORBIT_SLOTS : 0;
        return body.equals(SpaceObjects.EARTH_ID) ? slots(orbit) : 0;
    }

    /** Keeps the requested slot while it is free, otherwise takes the lowest free one. */
    public static int freeSlot(int requested, Set<Integer> occupied) {

        if (requested >= 0 && !occupied.contains(requested)) return requested;
        var slot = 0;
        while (occupied.contains(slot)) slot++;
        return slot;
    }

    public static boolean hasSlots(UUID body, SpaceSimulation.OrbitBand orbit) {

        return slots(body, orbit) > 0;
    }

    public static boolean hasSlots(SpaceSimulation.OrbitBand orbit) {

        return slots(orbit) > 0;
    }

    public static int slots(SpaceSimulation.OrbitBand orbit) {

        return switch (orbit) {
            case LOW -> LOW_ORBIT_SLOTS;
            case HIGH -> HIGH_ORBIT_SLOTS;
            default -> 0;
        };
    }
}
