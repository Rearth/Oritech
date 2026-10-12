package rearth.oritech.spaceage.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarMapObjectsTest {

    @Test
    void orbitRingsGainDetailAsTheyGrowOnScreen() {
        assertEquals(48, StarMapObjects.circleSegments(20));
        assertEquals(189, StarMapObjects.circleSegments(240));
        assertEquals(512, StarMapObjects.circleSegments(10_000));
    }
}
