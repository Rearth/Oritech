package rearth.oritech.spaceage.simulation;

import org.joml.Vector2f;

import java.util.HashSet;
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

    // this will be used for loading the sim from disk
    public SpaceSimulation(UUID loadedSimulationId, Set<SpaceObjects.SimulatedObject> loadedObjects) {
        this.simulationId = loadedSimulationId;
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
            asteroid.weight = (float) (Math.random() * 99 + 1);
            nonCelestialObjects.add(asteroid);
        }

        for (var i = 0; i < mediumAsteroidCount; i++) {
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f(
                    (float) (Math.random() * 2_000_000 - 1_000_000),
                    (float) (Math.random() * 17_000_000 + 1_000_000)
            );
            asteroid.weight = (float) (Math.random() * 99 + 1);
            nonCelestialObjects.add(asteroid);
        }

        for (var i = 0; i < beltAsteroidCount; i++) {
            var asteroid = new SpaceObjects.Asteroid();
            asteroid.currentPosition = new Vector2f(
                    (float) (Math.random() * 39_000_000 - 19_500_000),
                    (float) (Math.random() * 1_000_000 + 19_500_000)
            );
            asteroid.weight = (float) (Math.random() * 99 + 1);
            nonCelestialObjects.add(asteroid);
        }
    }

    // initializes celestial Objects once
    static {

        var earth = new SpaceObjects.SimulatedObject();
        earth.currentPosition = new Vector2f(0, 0);
        earth.currentState = SpaceObjects.DetectionState.PRECISE;
        celestialObjects.add(earth);

        var sun = new SpaceObjects.SimulatedObject();
        sun.currentPosition = new Vector2f(-1_000_000, 3_000_000);
        sun.currentState = SpaceObjects.DetectionState.PRECISE;
        celestialObjects.add(sun);

        var mars = new SpaceObjects.SimulatedObject();
        mars.currentPosition = new Vector2f(1_000_000, 8_000_000);
        mars.currentState = SpaceObjects.DetectionState.PRECISE;
        celestialObjects.add(mars);
    }

    // returns a value between 0 and 1
    public static float getGravityStrength(float height) {
        return Math.clamp(1 - height / 100_000, 0, 1);
    }

}
