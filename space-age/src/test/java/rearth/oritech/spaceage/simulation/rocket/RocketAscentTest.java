package rearth.oritech.spaceage.simulation.rocket;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceBalance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketAscentTest {

    @Test
    void ascentTimesAgreeWithIndependentlyIntegratedVelocity() {

        for (var acceleration : new double[]{-20, 0, 2, 20}) {
            for (var speed : new double[]{0, 500, 1500}) {
                if (acceleration <= 0 && speed == 0) continue;
                var ascent = new RocketAscent(speed, acceleration);
                var seconds = ascent.secondsTo(1000);
                var step = seconds / 10_000;
                var height = 0d;
                var velocity = speed;
                for (int i = 0; i < 10_000; i++) {
                    height += velocity * step + .5 * acceleration * step * step;
                    velocity += acceleration * step;
                }
                assertEquals(1000, height, 1e-6);
                assertEquals(velocity, ascent.speed(seconds), 1e-6);
            }
        }
    }

    @Test
    void ballisticLaunchVisualAndObstructionSurvivePersistence() {

        var performance = new RocketPerformance(1000, 0, 1000, 0, 0, 0, 0, 0);
        var ascent = RocketAscent.launch(performance, 500);
        var origin = new BlockPos(2, 70, 3);
        var duration = (long) Math.ceil(ascent.secondsTo(1000) * 20);
        var flight = new RocketFlight(true, null, Level.OVERWORLD, performance, origin, origin.above(1000), origin,
                new Vector2i(0, 1000), 0, 0, duration, -1, -1, null, -1, ascent)
                .withTakeoffCollision(origin.above(100), (long) Math.ceil(ascent.secondsTo(100) * 20)).scheduledAt(100);
        var decoded = RocketFlight.CODEC.parse(JsonOps.INSTANCE, RocketFlight.CODEC.encodeStart(JsonOps.INSTANCE, flight).getOrThrow()).getOrThrow();
        assertEquals(100, decoded.launchTick());
        assertEquals(500, decoded.ascent().initialSpeed());
        assertTrue(decoded.takeoffCollisionTick() > decoded.launchTick());
        assertTrue(decoded.takeoffCollisionTick() < decoded.orbitArrivalTick());
        // An unpowered craft coasts upwards against surface gravity.
        assertEquals(-SpaceBalance.STANDARD_GRAVITY, ascent.acceleration(), 1e-9);
        assertEquals(origin.getY(), RocketSimulationController.getRocketPosition(decoded, 100).y, 1e-6);
        assertEquals(origin.getY() + 500 - SpaceBalance.STANDARD_GRAVITY / 2,
                RocketSimulationController.getRocketPosition(decoded, 120).y, 1e-6);
        assertEquals(origin.getY() + 1000, RocketSimulationController.getRocketPosition(decoded, 100 + duration).y, 1e-6);
        var collisionSpeed = decoded.ascent().speed((decoded.takeoffCollisionTick() - decoded.launchTick()) / 20.0);
        assertTrue(collisionSpeed < 500 && collisionSpeed > 490);
    }

    @Test
    void particleAssistanceAdvancesObstructionTimingForPoweredCraft() {

        var performance = new RocketPerformance(1000, 0, 1000, 1, 30_000, 100, 1000, 30);
        var powered = RocketAscent.launch(performance, 0);
        var assisted = RocketAscent.launch(performance, 1500);
        assertTrue(assisted.secondsTo(100) < powered.secondsTo(100));
        assertTrue(assisted.secondsTo(100) < assisted.secondsTo(1000));
        assertTrue(assisted.speed(assisted.secondsTo(100)) >= 1500);
    }

    @Test
    void slowBallisticLaunchStillReachesOrbitHeight() {

        var performance = new RocketPerformance(1000, 0, 1000, 0, 0, 0, 0, 0);
        var ascent = RocketAscent.launch(performance, 50);
        var seconds = ascent.secondsTo(SpaceBalance.LAUNCH_ORBIT_HEIGHT_BLOCKS);
        assertTrue(Double.isFinite(seconds));
        assertTrue(ascent.speed(seconds) >= 50 / Math.sqrt(2) - 1e-9);
    }
}
