package rearth.oritech.spaceage.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsteroidMotionTest {

    @Test
    void dragHalvesSpeedOverTwoMinutesAndSettlesAtFiniteDistance() {

        assertEquals(.5, AsteroidMotion.velocityFactor(1_000, 120), 1e-12);
        var stop = AsteroidMotion.stopTime(1_000);
        assertTrue(stop > 120);
        assertEquals(0, AsteroidMotion.velocityFactor(1_000, stop));
        assertEquals(AsteroidMotion.distanceFactor(1_000, stop), AsteroidMotion.distanceFactor(1_000, stop + 1_000));
        assertEquals(0, AsteroidMotion.distanceFactor(SpaceBalance.ASTEROID_STOP_SPEED, 100));
        assertEquals(0, AsteroidMotion.velocityFactor(SpaceBalance.ASTEROID_STOP_SPEED, 100));
    }

    @Test
    void smallLiveStepsAndCoarsePlannerStepsTravelTheSameDistance() {

        var speed = 1_000d;
        var distance = 0d;
        for (int tick = 0; tick < 2_400; tick++) {
            distance += speed * AsteroidMotion.distanceFactor(speed, .05);
            speed *= AsteroidMotion.velocityFactor(speed, .05);
        }
        assertEquals(500, speed, 1e-8);
        assertEquals(1_000 * AsteroidMotion.distanceFactor(1_000, 120), distance, 1e-6);
        var fraction = .3;
        var hit = AsteroidMotion.timeAtFraction(1_000, 120, fraction);
        assertEquals(AsteroidMotion.distanceFactor(1_000, 120) * fraction, AsteroidMotion.distanceFactor(1_000, hit), 1e-10);
    }
}
