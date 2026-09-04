package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


// Owns the lightweight star-system state and the plans created inside that simulation.
// one or more players can use the same space simulation
// A player by default gets his own simulation. However if he uses a space interaction block / module that is already
// used with another simulation, he joins that one.
// There is also the option of manually leaving existing ones to get a new instance.

// Positions in space are in 2d, where X is the "right" / relative offset to the height / orbit. Units are blocks
// Y is "away" from the earth (distance from surface), with outer space and the sun both away from earth, but at different X positions.
// different orbits are also at specific Y heights each.

// general orbit bands / distances:
// low earth orbit: ~1000 distance. High gravity
// medium earth orbit: ~20 000 distance. Medium Gravity
// High / Geostationary Orbit: ~40 000. Low gravity
// initial outer space: ~100 000. Gravity influence reaches 0 at 100 000.
// first few asteroids are between ~100000 and ~200000
// sun: 3 000 000
// mars: 8 000 000
// asteroid ring: 20 000 000

public class SpaceSimulation {

    // Space objects use one shared horizontal range. This keeps the star map and
    // flight path on the same simple coordinate system at every distance.
    public static final float HORIZONTAL_POSITION_LIMIT = 1_000_000;
    private static final float HORIZONTAL_OBJECT_SPREAD = HORIZONTAL_POSITION_LIMIT * 0.9f;

    // this is always the same and initialized once:
    private static final Set<SpaceObjects.SimulatedObject> celestialObjects = new HashSet<>();

    private final Set<SpaceObjects.SimulatedObject> nonCelestialObjects = new HashSet<>();
    private final UUID simulationId;
    // A simulation may contain assemblers from several dimensions. GlobalPos prevents two matching block
    // coordinates from accidentally sharing a plan.
    private final Map<GlobalPos, FlightPlan> flightPlans = new HashMap<>();

    // this will be used for loading the sim from disk
    public SpaceSimulation(UUID loadedSimulationId, Set<SpaceObjects.SimulatedObject> loadedObjects) {
        this.simulationId = loadedSimulationId;
        this.nonCelestialObjects.addAll(loadedObjects);
    }

    public SpaceSimulation() {
        this.simulationId = UUID.randomUUID();
        generateRandomObjects();
    }

    private void generateRandomObjects() {
        // add asteroids (and more things in the future) to nonCelestialObjects

        var nearAsteroidCount = 5; // in range 100k - 200k
        var mediumAsteroidCount = 20; // in range 1M - 18 M
        var beltAsteroidCount = 20;    // in range 19.5M - 20.5M

        for (var i = 0; i < nearAsteroidCount; i++) {
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f(
                    distributedHorizontalPosition(i, nearAsteroidCount),
                    (float) (Math.random() * 100_000 + 100_000)
            );
            asteroid.currentState = SpaceObjects.DetectionState.ROUGH;
            asteroid.weight = (float) (Math.random() * 99 + 1);
            nonCelestialObjects.add(asteroid);
        }

        for (var i = 0; i < mediumAsteroidCount; i++) {
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f(
                    distributedHorizontalPosition(i, mediumAsteroidCount),
                    (float) (Math.random() * 17_000_000 + 1_000_000)
            );
            asteroid.currentState = SpaceObjects.DetectionState.ROUGH;
            asteroid.weight = (float) (Math.random() * 99 + 1);
            nonCelestialObjects.add(asteroid);
        }

        for (var i = 0; i < beltAsteroidCount; i++) {
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f(
                    distributedHorizontalPosition(i, beltAsteroidCount),
                    (float) (Math.random() * 1_000_000 + 19_500_000)
            );
            asteroid.currentState = SpaceObjects.DetectionState.ROUGH;
            asteroid.weight = (float) (Math.random() * 99 + 1);
            nonCelestialObjects.add(asteroid);
        }
    }

    // initializes celestial Objects once
    static {

        var earth = new SpaceObjects.SimulatedObject(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH);
        earth.currentPosition = new Vector2f(0, 0);
        earth.currentState = SpaceObjects.DetectionState.PRECISE;
        celestialObjects.add(earth);

        var sun = new SpaceObjects.SimulatedObject(UUID.fromString("00000000-0000-0000-0000-000000000002"), SpaceObjects.ObjectType.SUN);
        sun.currentPosition = new Vector2f(HORIZONTAL_POSITION_LIMIT * -2 / 3, 3_000_000);
        sun.currentState = SpaceObjects.DetectionState.PRECISE;
        celestialObjects.add(sun);

        var mars = new SpaceObjects.SimulatedObject(UUID.fromString("00000000-0000-0000-0000-000000000003"), SpaceObjects.ObjectType.MARS);
        mars.currentPosition = new Vector2f(HORIZONTAL_POSITION_LIMIT * 2 / 3, 8_000_000);
        mars.currentState = SpaceObjects.DetectionState.PRECISE;
        celestialObjects.add(mars);
    }

    // returns a value between 0 and 1
    public static float getGravityStrength(float height) {
        return Math.clamp(1 - height / 100_000, 0, 1);
    }

    public FlightPlannerSnapshot createFlightPlannerSnapshot(GlobalPos assemblerPosition, UUID rocketId) {
        var objects = createObjectData();
        return new FlightPlannerSnapshot(simulationId, rocketId, assemblerPosition.pos().getX(),
                assemblerPosition.pos().getZ(), objects,
                flightPlans.getOrDefault(assemblerPosition, FlightPlan.empty()));
    }

    private List<SpaceObjectData> createObjectData() {
        // Screens receive immutable values rather than the mutable simulation objects. This also keeps internal
        // asteroid state out of the networking codec when more simulation-only fields are added later.
        var objects = new ArrayList<SpaceObjectData>();
        celestialObjects.stream()
                .map(SpaceSimulation::toData)
                .forEach(objects::add);
        nonCelestialObjects.stream()
                .map(SpaceSimulation::toData)
                .forEach(objects::add);
        objects.sort(Comparator.comparing(SpaceObjectData::type).thenComparing(SpaceObjectData::id));
        return objects;
    }

    public void updateFlightPlan(GlobalPos assemblerPosition, FlightPlan plan, ActiveRocketData rocket) {
        // The client edits freely, but the stored copy is always rebuilt from values valid for this exact rocket.
        var validated = RocketFlightPlanRules.validate(plan, rocket, createObjectData());
        if (validated != null) flightPlans.put(assemblerPosition, validated);
    }

    private static SpaceObjectData toData(SpaceObjects.SimulatedObject object) {
        return new SpaceObjectData(object.id, object.type, object.currentPosition.x,
                object.currentPosition.y, object.currentState);
    }

    public record SpaceObjectData(UUID id, SpaceObjects.ObjectType type, float x, float y,
                                  SpaceObjects.DetectionState detectionState) {
    }

    public record FlightPlannerSnapshot(UUID simulationId, UUID rocketId, int launchX, int launchZ,
                                        List<SpaceObjectData> objects, FlightPlan plan) {
    }

    /** A stable segment reference used by plans that may be applied to another identical rocket. */
    public record SegmentRef(BlockPos anchor) {

        public static SegmentRef of(StaticRocketSegment segment) {
            var anchor = segment.blocks().stream().map(StaticRocketSegment.BlockData::relativePos)
                    .min(Comparator.comparingInt((BlockPos pos) -> pos.getY())
                            .thenComparingInt(pos -> pos.getX())
                            .thenComparingInt(pos -> pos.getZ()))
                    .orElse(BlockPos.ZERO);
            return new SegmentRef(anchor);
        }
    }

    private static float distributedHorizontalPosition(int index, int count) {
        if (count <= 1) return 0;
        return -HORIZONTAL_OBJECT_SPREAD + index * HORIZONTAL_OBJECT_SPREAD * 2 / (count - 1);
    }

    public record FlightPlan(List<FlightPlanBranch> branches) {

        public FlightPlan {
            branches = List.copyOf(branches);
        }

        public static FlightPlan empty() {
            return new FlightPlan(List.of(FlightPlanBranch.root()));
        }

        public FlightPlanBranch root() {
            return branches.stream().filter(FlightPlanBranch::isRoot).findFirst()
                    .orElseGet(FlightPlanBranch::root);
        }
    }

    /**
     * Branches are stored as a flat list to keep editing and networking simple.
     * A child points to the separation action that creates its craft.
     */
    public record FlightPlanBranch(UUID id, UUID parentSeparationAction, List<FlightPlanAction> actions) {
        public static final UUID NO_PARENT = new UUID(0, 2);

        public FlightPlanBranch {
            actions = List.copyOf(actions);
        }

        public static FlightPlanBranch root() {
            return new FlightPlanBranch(UUID.randomUUID(), NO_PARENT, List.of());
        }

        public boolean isRoot() {
            return parentSeparationAction.equals(NO_PARENT);
        }

        public FlightPlanBranch withActions(List<FlightPlanAction> newActions) {
            return new FlightPlanBranch(id, parentSeparationAction, newActions);
        }
    }

    /**
     * An empty segment list means all segments in the current craft. A separation
     * stores two segments: the first stays on this branch and the second branches off.
     */
    public record FlightPlanAction(UUID id, ActionType type, List<SegmentRef> segments,
                                   UUID targetId, long value, long secondaryValue) {
        public static final UUID NO_TARGET = new UUID(0, 0);
        public static final UUID CURRENT_TARGET = new UUID(0, 1);
        /** Synthetic target used by the preview after world X/Z coordinates are flattened into its flight plane. */
        public static final UUID SURFACE_TARGET = new UUID(0, 3);

        public FlightPlanAction {
            segments = List.copyOf(segments);
        }

        public static FlightPlanAction create(ActionType type) {
            return new FlightPlanAction(UUID.randomUUID(), type, List.of(), NO_TARGET, defaultValue(type), 0);
        }

        public FlightPlanAction withType(ActionType newType) {
            return new FlightPlanAction(id, newType, List.of(), NO_TARGET, defaultValue(newType), 0);
        }

        public FlightPlanAction withTarget(UUID target) {
            return new FlightPlanAction(id, type, segments, target, value, secondaryValue);
        }

        public FlightPlanAction withValue(long newValue) {
            return new FlightPlanAction(id, type, segments, targetId, newValue, secondaryValue);
        }

        public FlightPlanAction withSecondaryValue(long newValue) {
            return new FlightPlanAction(id, type, segments, targetId, value, newValue);
        }

        public FlightPlanAction withSegments(List<SegmentRef> newSegments) {
            return new FlightPlanAction(id, type, newSegments, targetId, value, secondaryValue);
        }

        private static long defaultValue(ActionType type) {
            return switch (type) {
                case WAIT_TICKS -> 20;
                case WAIT_SECONDS -> 5;
                case WAIT_UNTIL_DISTANCE -> 1_000;
                case SET_ARRIVAL_VELOCITY -> 0;
                default -> 0;
            };
        }
    }

    public enum ActionType {
        START_ENGINE_BURN,
        START_BRAKING_BURN,
        STOP_ENGINE_BURN,
        SET_NAVIGATION_TARGET,
        SET_SURFACE_DESTINATION,
        SET_ARRIVAL_VELOCITY,
        DISABLE_COUPLINGS,
        OPEN_PARACHUTES,
        WAIT_TICKS,
        WAIT_SECONDS,
        WAIT_UNTIL_DISTANCE,
        WAIT_FOR_EVENT,
        WAIT_UNTIL_BRAKING_POINT,
        MAINTAIN_ORBIT,
        DISCARD_CRAFT
    }

    public enum WaitEvent {
        LOW_ORBIT_REACHED,
        MEDIUM_ORBIT_REACHED,
        HIGH_ORBIT_REACHED,
        ATMOSPHERE_EXITED,
        TARGET_REACHED,
        FUEL_EMPTY
    }

}
