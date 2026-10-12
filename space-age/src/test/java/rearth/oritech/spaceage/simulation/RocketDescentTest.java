package rearth.oritech.spaceage.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketDescentTest {

    @Test
    void descentCurvesAreMonotonicAndReachTheRequestedTouchdownSpeed() {

        var epsilon = 1e-6;
        for (double speed : new double[]{0, 5, 12}) {
            double previous = 0;
            for (int tick = 1; tick <= 200; tick++) {
                var progress = RocketSimulationController.descentProgress(tick / 200.0, 1000, 10, speed);
                assertTrue(progress >= previous && progress <= 1);
                previous = progress;
            }
            assertEquals(1, previous, 1e-12);
            var near = RocketSimulationController.descentProgress(1 - epsilon, 1000, 10, speed);
            assertEquals(speed, (1 - near) * 1000 / (epsilon * 10), .001);
        }
    }
}
