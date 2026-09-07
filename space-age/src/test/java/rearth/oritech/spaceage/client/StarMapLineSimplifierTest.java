package rearth.oritech.spaceage.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarMapLineSimplifierTest {
    @Test
    void denseRouteNeedsOneFifthAsManySegments() {
        var lines = new ArrayList<StarMapLineRenderer.Line>();
        for (int i = 0; i < 1000; i++) lines.add(line(i * 0.75, (i + 1) * 0.75, -1));
        var result = StarMapLineSimplifier.simplify(lines, 0, 0, 800, 100);
        assertEquals(200, result.size());
        assertEquals(0, result.getFirst().fromX());
        assertEquals(750, result.getLast().toX());
    }

    @Test
    void phaseChangesAndDisconnectedBranchesRemainSeparate() {
        var lines = List.of(line(0, 1, -1), line(1, 2, -2), line(3, 4, -2));
        assertEquals(lines, StarMapLineSimplifier.simplify(lines, 0, 0, 100, 100));
    }

    @Test
    void longReturnTripIsNotCollapsed() {
        var lines = List.of(line(0, 100, -1), line(100, 1, -1));
        assertEquals(lines, StarMapLineSimplifier.simplify(lines, 0, 0, 100, 100));
    }

    @Test
    void cullingRetainsCrossingLinesAndTheirVisibleFringe() {
        var crossing = line(-100, 200, -1);
        var fringe = line(-1, -0.5, -1);
        assertEquals(List.of(crossing, fringe), StarMapLineSimplifier.simplify(
                List.of(line(-200, -100, -1), crossing, fringe), 0, 0, 100, 100));
    }

    @Test
    void referenceLinesAndWidthChangesRemainSeparate() {
        var lines = List.of(new StarMapLineRenderer.Line(0, 50, 1, 50, -1, 1),
                line(1, 2, -1), new StarMapLineRenderer.Line(2, 50, 3, 50, -1, 2, true));
        assertEquals(lines, StarMapLineSimplifier.simplify(lines, 0, 0, 100, 100));
    }

    private static StarMapLineRenderer.Line line(double from, double to, int color) {
        return new StarMapLineRenderer.Line(from, 50, to, 50, color, 1, true);
    }
}
