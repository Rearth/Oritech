package rearth.oritech.spaceage.simulation;

import net.minecraft.gametest.framework.GameTestHelper;
import com.mojang.serialization.JsonOps;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AsteroidMotionGameTests {

    private AsteroidMotionGameTests() {
    }

    public static void motion(GameTestHelper test) {

        var systems = new SpaceSimulationSavedData();
        var system = systems.getOrCreate(UUID.randomUUID());
        var original = system.truth().stream().filter(body -> body.type() == SpaceObjects.ObjectType.ASTEROID).findFirst().orElseThrow();
        system.earthKnowledge.scan(List.of(original), 0, original.x(), original.y(), 1);
        systems.tickAsteroids(test.getLevel(), List.of());
        test.assertTrue(system.asteroidData(original.id()).orElseThrow().equals(original), "Untouched asteroids stay stationary");
        system.moveAttached(original.id(), -3_000_000, 1_000_000, 1_000, 0, UUID.randomUUID());
        var attached = system.asteroidData(original.id()).orElseThrow();
        var known = system.createObjectData().stream().filter(body -> body.id().equals(original.id())).findFirst().orElseThrow();
        test.assertTrue(known.x() == attached.x() && known.velocityX() == attached.velocityX() && known.tugId().equals(attached.tugId()),
                "A discovered asteroid reports current tug motion without communication or another scan");
        var saved = SpaceSimulation.CODEC.encodeStart(JsonOps.INSTANCE, system).getOrThrow();
        var restored = SpaceSimulation.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        test.assertTrue(restored.asteroidData(original.id()).orElseThrow().equals(attached), "Current tug ownership and motion survive a world save");
        system.tickAsteroids(test.getLevel(), Set.of(original.id()));
        test.assertTrue(attached.equals(system.asteroidData(original.id()).orElseThrow()), "A tethered asteroid is not integrated as a free drifting body");
        system.releaseAsteroid(original.id(), attached.x(), attached.y(), attached.velocityX(), attached.velocityY(), null);
        for (int tick = 0; tick < 2_400; tick++) systems.tickAsteroids(test.getLevel(), List.of());
        var drifted = system.asteroidData(original.id()).orElseThrow();
        var reported = system.createObjectData().stream().filter(body -> body.id().equals(original.id())).findFirst().orElseThrow();
        test.assertTrue(reported.x() == drifted.x() && reported.velocityX() == drifted.velocityX()
                && reported.tugId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET), "Discovered released asteroids report current drift");
        test.assertTrue(Math.abs(drifted.velocityX() - 500) < .1, "Released asteroids lose half their speed after two minutes, even with no craft left");
        test.assertTrue(Math.abs(drifted.x() - attached.withTug(SpaceSimulation.FlightPlanAction.NO_TARGET).xAt(120)) < 2,
                "Live drift agrees with the predicted motion");
        system.releaseAsteroid(original.id(), drifted.x(), drifted.y(), .051, 0, null);
        for (int tick = 0; tick < 100; tick++) systems.tickAsteroids(test.getLevel(), List.of());
        var stopped = system.asteroidData(original.id()).orElseThrow();
        test.assertTrue(stopped.velocityX() == 0 && stopped.velocityY() == 0, "Slow drifting asteroids settle completely");
        for (int tick = 0; tick < 100; tick++) systems.tickAsteroids(test.getLevel(), List.of());
        test.assertTrue(stopped.equals(system.asteroidData(original.id()).orElseThrow()), "Settled asteroids remain stationary");
        test.succeed();
    }
}
