package rearth.oritech.spaceage.simulation;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceGravityTest {

    @Test
    void celestialGravityEndsAtTheCollectorAndOuterOrbitRings() {

        for (var body : new SpaceSimulation().createObjectData()) {
            if (body.type() != SpaceObjects.ObjectType.SUN && body.type() != SpaceObjects.ObjectType.EARTH
                    && body.type() != SpaceObjects.ObjectType.MARS) continue;
            var orbit = body.type() == SpaceObjects.ObjectType.SUN ? OrbitBand.SOLAR : OrbitBand.HIGH;
            var boundary = body.radius() + orbit.altitude();
            assertEquals(boundary, SpaceGravity.range(body));
            assertEquals(0, SpaceGravity.acceleration(body, boundary));
            assertEquals(0, SpaceGravity.acceleration(body, boundary + 1));
            assertEquals(0, SpaceGravity.potential(body, boundary));
            assertEquals(0, SpaceGravity.potential(body, boundary + 1));
            assertTrue(SpaceGravity.acceleration(body, boundary - .01) > 0);
            assertTrue(SpaceGravity.potential(body, boundary - .01) > 0);
            assertEquals(body.surfaceGravity(), SpaceGravity.acceleration(body, body.radius()), 1e-6);
            var work = SpaceGravity.potential(body, boundary - 1) - SpaceGravity.potential(body, boundary);
            assertEquals(SpaceGravity.acceleration(body, boundary - .5), work, 1e-6);
        }
    }

    @Test
    void asteroidsKeepTheirExistingGravity() {

        var asteroid = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.ASTEROID,
                0, 0, 100, 1, SpaceObjects.DetectionState.PRECISE);
        assertEquals(.25, SpaceGravity.acceleration(asteroid, 200));
        assertEquals(50, SpaceGravity.potential(asteroid, 200));
    }
}
