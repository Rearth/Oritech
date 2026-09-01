package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


// purely a data container.
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

    // this is always the same and initialized once:
    private static final Set<SpaceObjects.SimulatedObject> celestialObjects = new HashSet<>();

    private final Set<SpaceObjects.SimulatedObject> nonCelestialObjects = new HashSet<>();
    private final UUID simulationId;
    private final Map<BlockPos, List<FlightPlanAction>> flightPlans = new HashMap<>();

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
                    (float) (Math.random() * 200_000 - 100_000),
                    (float) (Math.random() * 100_000 + 100_000)
            );
            asteroid.currentState = SpaceObjects.DetectionState.ROUGH;
            asteroid.weight = (float) (Math.random() * 99 + 1);
            nonCelestialObjects.add(asteroid);
        }

        for (var i = 0; i < mediumAsteroidCount; i++) {
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f(
                    (float) (Math.random() * 2_000_000 - 1_000_000),
                    (float) (Math.random() * 17_000_000 + 1_000_000)
            );
            asteroid.currentState = SpaceObjects.DetectionState.ROUGH;
            asteroid.weight = (float) (Math.random() * 99 + 1);
            nonCelestialObjects.add(asteroid);
        }

        for (var i = 0; i < beltAsteroidCount; i++) {
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f(
                    (float) (Math.random() * 39_000_000 - 19_500_000),
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
        sun.currentPosition = new Vector2f(-1_000_000, 3_000_000);
        sun.currentState = SpaceObjects.DetectionState.PRECISE;
        celestialObjects.add(sun);

        var mars = new SpaceObjects.SimulatedObject(UUID.fromString("00000000-0000-0000-0000-000000000003"), SpaceObjects.ObjectType.MARS);
        mars.currentPosition = new Vector2f(1_000_000, 8_000_000);
        mars.currentState = SpaceObjects.DetectionState.PRECISE;
        celestialObjects.add(mars);
    }

    // returns a value between 0 and 1
    public static float getGravityStrength(float height) {
        return Math.clamp(1 - height / 100_000, 0, 1);
    }

    public FlightPlannerSnapshot createFlightPlannerSnapshot(BlockPos assemblerPosition, UUID rocketId) {
        var objects = new ArrayList<SpaceObjectData>();
        celestialObjects.stream()
                .map(SpaceSimulation::toData)
                .forEach(objects::add);
        nonCelestialObjects.stream()
                .map(SpaceSimulation::toData)
                .forEach(objects::add);
        objects.sort(Comparator.comparing(SpaceObjectData::type).thenComparing(SpaceObjectData::id));
        return new FlightPlannerSnapshot(simulationId, rocketId, objects,
                List.copyOf(flightPlans.getOrDefault(assemblerPosition, List.of())));
    }

    public void updateFlightPlan(BlockPos assemblerPosition, List<FlightPlanAction> actions,
                                 Set<UUID> rocketSegments) {
        if (actions.size() > 32) return;

        var objectIds = new HashSet<UUID>();
        celestialObjects.forEach(object -> objectIds.add(object.id));
        nonCelestialObjects.forEach(object -> objectIds.add(object.id));

        var validated = new ArrayList<FlightPlanAction>(actions.size());
        var actionIds = new HashSet<UUID>();
        for (var action : actions) {
            if (!actionIds.add(action.id())) continue;
            var targetValid = switch (action.type()) {
                case SET_NAVIGATION_TARGET -> objectIds.contains(action.targetId());
                case WAIT_UNTIL_DISTANCE -> action.targetId().equals(FlightPlanAction.CURRENT_TARGET)
                        || action.targetId().equals(SpaceObjects.EARTH_ID);
                case DISABLE_COUPLINGS -> rocketSegments.contains(action.targetId());
                default -> true;
            };
            if (!targetValid) continue;

            long value = switch (action.type()) {
                case WAIT_TICKS -> Math.clamp(action.value(), 1, 72_000);
                case WAIT_SECONDS -> Math.clamp(action.value(), 1, 3_600);
                case WAIT_UNTIL_DISTANCE -> Math.clamp(action.value(), 0, 100_000_000);
                case WAIT_FOR_EVENT -> Math.clamp(action.value(), 0, WaitEvent.values().length - 1);
                default -> 0;
            };
            validated.add(action.withValue(value));
        }
        flightPlans.put(assemblerPosition.immutable(), List.copyOf(validated));
    }

    private static SpaceObjectData toData(SpaceObjects.SimulatedObject object) {
        return new SpaceObjectData(object.id, object.type, object.currentPosition.x,
                object.currentPosition.y, object.currentState);
    }

    public record SpaceObjectData(UUID id, SpaceObjects.ObjectType type, float x, float y,
                                  SpaceObjects.DetectionState detectionState) {
    }

    public record FlightPlannerSnapshot(UUID simulationId, UUID rocketId,
                                        List<SpaceObjectData> objects, List<FlightPlanAction> actions) {
    }

    public record FlightPlanAction(UUID id, ActionType type, UUID targetId, long value) {
        public static final UUID NO_TARGET = new UUID(0, 0);
        public static final UUID CURRENT_TARGET = new UUID(0, 1);

        public static FlightPlanAction create(ActionType type) {
            return new FlightPlanAction(UUID.randomUUID(), type, NO_TARGET, defaultValue(type));
        }

        public FlightPlanAction withType(ActionType newType) {
            return new FlightPlanAction(id, newType, NO_TARGET, defaultValue(newType));
        }

        public FlightPlanAction withTarget(UUID target) {
            return new FlightPlanAction(id, type, target, value);
        }

        public FlightPlanAction withValue(long newValue) {
            return new FlightPlanAction(id, type, targetId, newValue);
        }

        private static long defaultValue(ActionType type) {
            return switch (type) {
                case WAIT_TICKS -> 20;
                case WAIT_SECONDS -> 5;
                case WAIT_UNTIL_DISTANCE -> 1_000;
                default -> 0;
            };
        }
    }

    public enum ActionType {
        START_ENGINE_BURN,
        STOP_ENGINE_BURN,
        SET_NAVIGATION_TARGET,
        DISABLE_COUPLINGS,
        OPEN_PARACHUTES,
        WAIT_TICKS,
        WAIT_SECONDS,
        WAIT_UNTIL_DISTANCE,
        WAIT_FOR_EVENT
    }

    public enum WaitEvent {
        ORBIT_REACHED,
        ATMOSPHERE_EXITED,
        TARGET_REACHED,
        FUEL_EMPTY
    }

}
