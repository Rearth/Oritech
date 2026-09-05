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

/**
 * Owns one game-scaled solar system and the flight plans created inside it.
 * Positions use one shared two-dimensional orbital plane with the sun at the origin.
 */
public class SpaceSimulation {

    public static final UUID SUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID MARS_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private static final Set<SpaceObjects.SimulatedObject> CELESTIAL_OBJECTS = new HashSet<>();

    private final Set<SpaceObjects.SimulatedObject> nonCelestialObjects = new HashSet<>();
    private final UUID simulationId;
    private final Map<GlobalPos, FlightPlan> flightPlans = new HashMap<>();

    public SpaceSimulation(UUID loadedSimulationId, Set<SpaceObjects.SimulatedObject> loadedObjects) {
        simulationId = loadedSimulationId;
        nonCelestialObjects.addAll(loadedObjects);
    }

    public SpaceSimulation() {
        simulationId = UUID.randomUUID();
        generateRandomObjects();
    }

    private void generateRandomObjects() {
        // Asteroids use polar placement so the belt is meaningful in the new solar coordinate system.
        addNearEarthAsteroids(8);
        addAsteroidRing(5, 3_400_000, 4_000_000);
        addAsteroidRing(20, 5_500_000, 7_500_000);
        addAsteroidRing(20, 8_500_000, 9_500_000);
    }

    private void addNearEarthAsteroids(int count) {
        // These sit just outside Earth's high orbit, mostly on the side facing away from the sun.
        for (int index = 0; index < count; index++) {
            double angle = Math.PI - 1.1 + 2.2 * index / Math.max(1, count - 1);
            double radius = 140_000 + Math.random() * 70_000;
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f((float) (-3_000_000 + Math.cos(angle) * radius),
                    (float) (Math.sin(angle) * radius));
            asteroid.currentState = SpaceObjects.DetectionState.ROUGH;
            asteroid.weight = (float) (Math.random() * 40 + 1);
            asteroid.radius = 1_500 + asteroid.weight * 35;
            asteroid.surfaceGravity = asteroid.weight * 0.0002f;
            nonCelestialObjects.add(asteroid);
        }
    }

    private void addAsteroidRing(int count, double minimumRadius, double maximumRadius) {
        double angleOffset = Math.random() * Math.PI * 2;
        for (int index = 0; index < count; index++) {
            double angle = angleOffset + Math.PI * 2 * index / count;
            double radius = minimumRadius + Math.random() * (maximumRadius - minimumRadius);
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f((float) (Math.cos(angle) * radius),
                    (float) (Math.sin(angle) * radius));
            asteroid.currentState = SpaceObjects.DetectionState.ROUGH;
            asteroid.weight = (float) (Math.random() * 99 + 1);
            asteroid.radius = 1_500 + asteroid.weight * 35;
            asteroid.surfaceGravity = asteroid.weight * 0.0002f;
            nonCelestialObjects.add(asteroid);
        }
    }

    static {
        var sun = new SpaceObjects.SimulatedObject(SUN_ID, SpaceObjects.ObjectType.SUN);
        sun.currentPosition = new Vector2f(0, 0);
        sun.radius = 250_000;
        sun.surfaceGravity = 274;
        sun.currentState = SpaceObjects.DetectionState.PRECISE;
        CELESTIAL_OBJECTS.add(sun);

        var earth = new SpaceObjects.SimulatedObject(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH);
        earth.currentPosition = new Vector2f(-3_000_000, 0);
        earth.radius = 60_000;
        earth.surfaceGravity = 9.81f;
        earth.currentState = SpaceObjects.DetectionState.PRECISE;
        CELESTIAL_OBJECTS.add(earth);

        var mars = new SpaceObjects.SimulatedObject(MARS_ID, SpaceObjects.ObjectType.MARS);
        mars.currentPosition = new Vector2f(5_000_000, 1_500_000);
        mars.radius = 45_000;
        mars.surfaceGravity = 3.71f;
        mars.currentState = SpaceObjects.DetectionState.PRECISE;
        CELESTIAL_OBJECTS.add(mars);
    }

    public FlightPlannerSnapshot createFlightPlannerSnapshot(GlobalPos assemblerPosition, UUID rocketId) {
        var objects = createObjectData();
        return new FlightPlannerSnapshot(simulationId, rocketId, objects,
                flightPlans.getOrDefault(assemblerPosition, FlightPlan.empty()));
    }

    private List<SpaceObjectData> createObjectData() {
        var objects = new ArrayList<SpaceObjectData>();
        CELESTIAL_OBJECTS.stream().map(SpaceSimulation::toData).forEach(objects::add);
        nonCelestialObjects.stream().map(SpaceSimulation::toData).forEach(objects::add);
        objects.sort(Comparator.comparing(SpaceObjectData::type).thenComparing(SpaceObjectData::id));
        return objects;
    }

    public void updateFlightPlan(GlobalPos assemblerPosition, FlightPlan plan, ActiveRocketData rocket) {
        var validated = RocketFlightPlanRules.validate(plan, rocket, createObjectData());
        if (validated != null) flightPlans.put(assemblerPosition, validated);
    }

    private static SpaceObjectData toData(SpaceObjects.SimulatedObject object) {
        return new SpaceObjectData(object.id, object.type, object.currentPosition.x,
                object.currentPosition.y, object.radius, object.surfaceGravity, object.currentState);
    }

    public record SpaceObjectData(UUID id, SpaceObjects.ObjectType type, float x, float y, float radius,
                                  float surfaceGravity, SpaceObjects.DetectionState detectionState) {
    }

    public record FlightPlannerSnapshot(UUID simulationId, UUID rocketId,
                                        List<SpaceObjectData> objects, FlightPlan plan) {
    }

    /** Stable segment identity for plans applied to another rocket built at the same relative positions. */
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

    /** Configuration follows the segment's relative anchor so reusable plans still match an identical rocket. */
    public record SegmentConfiguration(SegmentRef segment, String name, boolean booster,
                                       List<Integer> engineStages) {

        public SegmentConfiguration {
            engineStages = List.copyOf(engineStages);
        }

        public boolean usesEnginesDuring(int stage) {
            return engineStages.contains(stage);
        }

        public int lastEngineStage() {
            return engineStages.stream().mapToInt(Integer::intValue).max().orElse(0);
        }

        public SegmentConfiguration withName(String newName) {
            return new SegmentConfiguration(segment, newName, booster, engineStages);
        }

        public SegmentConfiguration withBooster(boolean newBooster) {
            return new SegmentConfiguration(segment, name, newBooster, engineStages);
        }

        public SegmentConfiguration withEngineStages(List<Integer> newEngineStages) {
            return new SegmentConfiguration(segment, name, booster, newEngineStages);
        }
    }

    public record FlightPlan(List<FlightPlanBranch> branches, List<SegmentConfiguration> segmentConfigurations) {

        public FlightPlan {
            branches = List.copyOf(branches);
            segmentConfigurations = List.copyOf(segmentConfigurations);
        }

        public static FlightPlan empty() {
            return new FlightPlan(List.of(FlightPlanBranch.root()), List.of());
        }

        public FlightPlanBranch root() {
            return branches.stream().filter(FlightPlanBranch::isRoot).findFirst()
                    .orElseGet(FlightPlanBranch::root);
        }

        public FlightPlan withBranches(List<FlightPlanBranch> newBranches) {
            return new FlightPlan(newBranches, segmentConfigurations);
        }

        public FlightPlan withSegmentConfigurations(List<SegmentConfiguration> newConfigurations) {
            return new FlightPlan(branches, newConfigurations);
        }

        public SegmentConfiguration configurationFor(SegmentRef segment) {
            return segmentConfigurations.stream().filter(configuration -> configuration.segment().equals(segment))
                    .findFirst().orElse(new SegmentConfiguration(segment, "", false, List.of(1)));
        }
    }

    /** Branches stay flat because this makes timeline editing and network serialization easy to follow. */
    public record FlightPlanBranch(UUID id, UUID parentSeparationAction, List<FlightPlanAction> actions) {
        public static final UUID NO_PARENT = new UUID(0, 1);

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

    /** A navigation action blocks its branch until the automatically calculated transfer is complete. */
    public record FlightPlanAction(UUID id, ActionType type, List<SegmentRef> segments,
                                   UUID targetId, OrbitBand orbit,
                                   ArrivalVelocityMode velocityMode, int targetVelocity) {
        public static final UUID NO_TARGET = new UUID(0, 0);

        public FlightPlanAction {
            segments = List.copyOf(segments);
        }

        public static FlightPlanAction create(ActionType type) {
            return new FlightPlanAction(UUID.randomUUID(), type, List.of(), NO_TARGET, OrbitBand.LOW,
                    ArrivalVelocityMode.ZERO, 0);
        }

        public static FlightPlanAction disconnectBooster(UUID id, SegmentRef segment, UUID navigationAction) {
            return new FlightPlanAction(id, ActionType.DISCONNECT_BOOSTER, List.of(segment),
                    navigationAction, OrbitBand.LOW, ArrivalVelocityMode.ZERO, 0);
        }

        public FlightPlanAction withType(ActionType newType) {
            return new FlightPlanAction(id, newType, List.of(), NO_TARGET, OrbitBand.LOW,
                    ArrivalVelocityMode.ZERO, 0);
        }

        public FlightPlanAction withTarget(UUID target) {
            return new FlightPlanAction(id, type, segments, target, orbit, velocityMode, targetVelocity);
        }

        public FlightPlanAction withOrbit(OrbitBand newOrbit) {
            return new FlightPlanAction(id, type, segments, targetId, newOrbit, velocityMode, targetVelocity);
        }

        public FlightPlanAction withVelocity(ArrivalVelocityMode newMode, int newVelocity) {
            return new FlightPlanAction(id, type, segments, targetId, orbit, newMode, newVelocity);
        }

        public FlightPlanAction withSegments(List<SegmentRef> newSegments) {
            return new FlightPlanAction(id, type, newSegments, targetId, orbit, velocityMode, targetVelocity);
        }

        public boolean isGenerated() {
            return type == ActionType.DISCONNECT_BOOSTER;
        }
    }

    public enum ActionType {
        NAVIGATE_TO,
        DECOUPLE,
        DISCONNECT_BOOSTER,
        MAINTAIN_POSITION,
        DISCARD_CRAFT
    }

    public enum OrbitBand {
        SURFACE(0),
        TIGHT(1_000),
        LOW(10_000),
        MEDIUM(30_000),
        HIGH(60_000);

        private final double altitude;

        OrbitBand(double altitude) {
            this.altitude = altitude;
        }

        public double altitude() {
            return altitude;
        }
    }

    public enum ArrivalVelocityMode {
        ZERO,
        MAXIMUM,
        CUSTOM
    }
}
