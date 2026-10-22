package rearth.oritech.spaceage.client.starmap;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.client.starmap.StarMapLineRenderer.Line;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarMapLineRendererTest {

    @Test
    void curvedPulseJoinsKeepBothEdgesAtTheSameDistanceFromAdjacentSegments() {

        for (var slope : new double[]{-.8, -.1, 0, .1, .8}) {
            var first = new Line(0, 0, 10, 0, -1, 20, true);
            var second = new Line(10, 0, 20, slope * 10, -1, 24, true);
            var join = StarMapLineRenderer.join(first, second);
            assertEquals(22, join.width(), 1e-6, "A pulse shares one interpolated width at the join");
            var length = Math.hypot(10, slope * 10);
            // The shared edge lies on the offset of each segment, so there is no wedge between them.
            assertEquals(1, join.y(), 1e-6);
            assertEquals(1, join.x() * -slope * 10 / length + join.y() * 10 / length, 1e-6);
        }
    }
}
