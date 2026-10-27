package rearth.oritech.spaceage.client.starmap;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarMapBeamRendererTest {

    @Test
    void clipsCrossingLinksAndRejectsShortOrOffscreenLinks() {

        var id = new UUID(0, 1);
        var crossing = StarMapBeamRenderer.project(-1000, 50, 1000, 50, id, false, 0, 0, 100, 100);
        assertNotNull(crossing);
        assertEquals(996, crossing.start());
        assertEquals(1104, crossing.end());
        assertNull(StarMapBeamRenderer.project(-100, -100, 200, -100, id, false, 0, 0, 100, 100));
        assertNull(StarMapBeamRenderer.project(-100, 50, 50, -100, id, false, 0, 0, 100, 100),
                "Bounding boxes may overlap while the actual diagonal misses the viewport");
        assertNull(StarMapBeamRenderer.project(10, 10, 20, 10, id, false, 0, 0, 100, 100));
    }

    @Test
    void pulseMotionKeepsItsDirectionSpeedAndPhaseAcrossPanning() {

        var id = new UUID(0, 123);
        var first = StarMapBeamRenderer.project(-1000, 50, 1000, 50, id, false, 0, 0, 100, 100);
        var panned = StarMapBeamRenderer.project(-1010, 50, 990, 50, id, false, 0, 0, 100, 100);
        assertNotNull(first);
        assertNotNull(panned);
        var before = StarMapBeamRenderer.firstHead(first, 0);
        var after = StarMapBeamRenderer.firstHead(first, .1);
        assertEquals(7, wrap(after - before, first.spacing()), 1e-9);
        var shifted = StarMapBeamRenderer.firstHead(panned, 0);
        assertEquals(wrap(before - 10, first.spacing()), shifted, 1e-9);
    }

    @Test
    void hundredsOfLongLinksHaveAtMostThreeVisiblePulsesEach() {

        for (int index = 0; index < 500; index++) {
            var link = StarMapBeamRenderer.project(-1_000_000, -1_000_000, 1_000_000, 1_000_000,
                    new UUID(0, index), false, 0, 0, 1920, 1080);
            assertNotNull(link);
            for (double seconds = 0; seconds < 10; seconds += .13) {
                var count = 0;
                for (var head = StarMapBeamRenderer.firstHead(link, seconds); head < link.end() - link.start() + 16; head += link.spacing())
                    count++;
                assertTrue(count <= 3, "Geometry stays bounded even with distant endpoints and a large viewport");
            }
        }
    }

    private static double wrap(double value, double period) {

        return value - Math.floor(value / period) * period;
    }
}
