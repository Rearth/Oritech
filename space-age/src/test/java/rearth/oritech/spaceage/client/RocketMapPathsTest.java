package rearth.oritech.spaceage.client;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.FlightMotion;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import static rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class RocketMapPathsTest {
    private static final UUID ID = new UUID(0, 1);
    private static PathSample sample(double t, double x, double y, double vx, double vy) {
        return new PathSample(t, x, y, Math.hypot(vx, vy), vx, vy, PathPhase.REDIRECT,
                ID, ID, 1, Set.of(), Set.of(), SpaceSimulation.FlightPlanAction.NO_TARGET);
    }
    private static CraftPath path(PathSample... samples) {
        return new CraftPath(ID, Set.of(), List.of(samples), List.of(), samples[samples.length - 1].timeSeconds(), 0, TerminalState.READY);
    }
    private static RocketMapPaths geometry(CraftPath path) {
        return new RocketMapPaths(SpaceSimulation.FlightPlan.empty(), new FlightPath(List.of(path), List.of(), path.durationSeconds()));
    }
    @Test void drawingAndMarkersFollowTheExactAccelerationCurve() {
        var start = sample(0, 0, 0, 10, 0);
        var end = sample(10, 100, 100, 10, 20);
        var path = path(start, end); var geometry = geometry(path);
        var midpoint = geometry.renderedWorldPosition(ID, 5, -1, -1);
        assertEquals(50, midpoint.x(), 1e-9); assertEquals(25, midpoint.y(), 1e-9);
        var pieces = geometry.renderedPathSegments(path);
        assertTrue(pieces.size() > 1);
        for (var piece : pieces) {
            var state = new FlightMotion(start, end).at(piece.sampleProgressTo());
            assertEquals(state.x(), piece.to().x(), 1e-9);
            assertEquals(state.y(), piece.to().y(), 1e-9);
            assertSame(end, piece.secondSample());
        }
    }
    @Test void zoomDeterminesDetailAndInvisibleCurvesAreCulled() {
        var path = path(sample(0, 0, 0, 10, 0), sample(10, 100, 100, 10, 20));
        var geometry = geometry(path);
        var close = geometry.renderedPathSegments(path, p -> p, 0, 0, 200, 200);
        var far = geometry.renderedPathSegments(path, p -> new RocketMapPaths.Point(p.x() * .01, p.y() * .01), 0, 0, 200, 200);
        assertTrue(close.size() > far.size());
        assertTrue(geometry.renderedPathSegments(path, p -> p, 500, 500, 20, 20).isEmpty());
    }
    @Test void collinearReversalKeepsItsTurningPoint() {
        var path = path(sample(0, 0, 0, 10, 0), sample(10, 0, 0, -10, 0));
        var geometry = geometry(path);
        assertEquals(25, geometry.renderedWorldPosition(ID, 5, 0, 0).x(), 1e-9);
        var pieces = geometry.renderedPathSegments(path, p -> p, 20, -5, 10, 10);
        assertFalse(pieces.isEmpty(), "An offscreen pair of endpoints can still turn inside the viewport");
        assertTrue(pieces.stream().anyMatch(p -> p.to().x() >= 24));
    }
    @Test void coastsHaveOneLineAndStationaryEventsDoNotInventMotion() {
        var path = path(sample(0, 0, 0, 10, 0), sample(10, 100, 0, 10, 0));
        assertEquals(1, geometry(path).renderedPathSegments(path).size());
        var stationary = path(sample(0, 5, 6, 0, 0), sample(0, 5, 6, 0, 0));
        assertTrue(geometry(stationary).renderedPathSegments(stationary).isEmpty());
        assertEquals(6, geometry(stationary).renderedWorldPosition(ID, 0, -1, -1).y());
    }
    @Test void detachedCraftAndReleasedAsteroidsNeedNoDisplayOffsets() {
        var parent = path(sample(0, 0, 0, 10, 0), sample(10, 100, 100, 10, 20));
        var atRelease = FlightMotion.sample(parent.samples(), 5);
        var released = new AsteroidPath(UUID.randomUUID(), List.of(
                new MotionSample(5, atRelease.x(), atRelease.y(), 14),
                new MotionSample(10, 100, 75, 14)), null, 0);
        var piece = geometry(parent).renderedAsteroidPathSegments(released).getFirst();
        assertEquals(atRelease.x(), piece.from().x());
        assertEquals(atRelease.y(), piece.from().y());
    }
}
