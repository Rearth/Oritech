package rearth.oritech.spaceage.client;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.FlightMotion;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.AsteroidPath;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.BoosterEvent;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.FlightPath;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.MotionSample;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void drawingAndMarkersFollowTheExactAccelerationCurve() {

        var start = sample(0, 0, 0, 10, 0);
        var end = sample(10, 100, 100, 10, 20);
        var path = path(start, end);
        var geometry = geometry(path);
        var midpoint = geometry.renderedWorldPosition(ID, 5, -1, -1);
        assertEquals(50, midpoint.x(), 1e-9);
        assertEquals(25, midpoint.y(), 1e-9);
        var pieces = geometry.renderedPathSegments(path);
        assertTrue(pieces.size() > 1);
        for (var piece : pieces) {
            var state = new FlightMotion(start, end).at(piece.sampleProgressTo());
            assertEquals(state.x(), piece.to().x(), 1e-9);
            assertEquals(state.y(), piece.to().y(), 1e-9);
            assertSame(end, piece.secondSample());
        }
    }

    @Test
    void zoomDeterminesDetailAndInvisibleCurvesAreCulled() {

        var path = path(sample(0, 0, 0, 10, 0), sample(10, 100, 100, 10, 20));
        var geometry = geometry(path);
        var close = geometry.renderedPathSegments(path, p -> p, 0, 0, 200, 200);
        var far = geometry.renderedPathSegments(path, p -> new RocketMapPaths.Point(p.x() * .01, p.y() * .01), 0, 0, 200, 200);
        assertTrue(close.size() > far.size());
        assertTrue(geometry.renderedPathSegments(path, p -> p, 500, 500, 20, 20).isEmpty());
    }

    @Test
    void collinearReversalKeepsItsTurningPoint() {

        var path = path(sample(0, 0, 0, 10, 0), sample(10, 0, 0, -10, 0));
        var geometry = geometry(path);
        assertEquals(25, geometry.renderedWorldPosition(ID, 5, 0, 0).x(), 1e-9);
        var pieces = geometry.renderedPathSegments(path, p -> p, 20, -5, 10, 10);
        assertFalse(pieces.isEmpty(), "An offscreen pair of endpoints can still turn inside the viewport");
        assertTrue(pieces.stream().anyMatch(p -> p.to().x() >= 24));
    }

    @Test
    void coastsHaveOneLineAndStationaryEventsDoNotInventMotion() {

        var path = path(sample(0, 0, 0, 10, 0), sample(10, 100, 0, 10, 0));
        assertEquals(1, geometry(path).renderedPathSegments(path).size());
        var stationary = path(sample(0, 5, 6, 0, 0), sample(0, 5, 6, 0, 0));
        assertTrue(geometry(stationary).renderedPathSegments(stationary).isEmpty());
        assertEquals(6, geometry(stationary).renderedWorldPosition(ID, 0, -1, -1).y());
    }

    @Test
    void surfaceOffsetsKeepMarkersOnTheRenderedCurveAndLeaveSimulationUntouched() {

        var start = sample(0, 100, 0, 10, 0);
        var end = sample(10, 200, 100, 10, 20);
        var path = path(start, end);
        var geometry = new RocketMapPaths(SpaceSimulation.FlightPlan.empty(),
                new FlightPath(List.of(path), List.of(), path.durationSeconds()),
                point -> point.x() == 100 && point.y() == 0 ? new RocketMapPaths.Point(40, 0) : point);
        assertEquals(40, geometry.renderedWorldPosition(ID, 0, -1, -1).x());
        assertEquals(200, geometry.renderedWorldPosition(ID, 10, -1, -1).x());
        for (var piece : geometry.renderedPathSegments(path)) {
            var time = piece.sampleProgressTo() * 10;
            var marker = geometry.renderedWorldPosition(ID, time, -1, -1);
            assertEquals(piece.to().x(), marker.x(), 1e-9);
            assertEquals(piece.to().y(), marker.y(), 1e-9);
            assertSame(end, piece.secondSample());
        }
        assertEquals(100, start.x());
        assertEquals(150, FlightMotion.sample(path.samples(), 5).x());
        assertEquals(120, geometry.renderedWorldPosition(ID, 5, -1, -1).x());
    }

    @Test
    void movingASurfaceEndpointCannotIntroduceAReversalAtTheNextSample() {

        var path = path(sample(0, 100, 0, -10, 0), sample(1, 90, 0, -10, 0),
                sample(2, 80, 0, -10, 0), sample(10, 0, 0, -10, 0));
        var geometry = new RocketMapPaths(SpaceSimulation.FlightPlan.empty(),
                new FlightPath(List.of(path), List.of(), path.durationSeconds()),
                point -> point.x() == 100 ? new RocketMapPaths.Point(40, 0) : point);
        var previous = 40d;
        for (int tick = 0; tick <= 200; tick++) {
            var point = geometry.renderedWorldPosition(ID, tick / 20d, -1, -1);
            assertTrue(point.x() <= previous + 1e-9, "The whole departure must move toward the target");
            assertEquals(0, point.y(), 1e-9);
            previous = point.x();
        }
        assertEquals(0, previous, 1e-9);
    }

    @Test
    void surfaceCurveDoesNotDependOnTelemetrySampleDensity() {

        var start = sample(0, 100, 0, 10, 0);
        var end = sample(10, 200, 100, 10, 20);
        var sparse = path(start, end);
        var dense = path(start, sample(5, 150, 25, 10, 10), end);
        var sparseGeometry = new RocketMapPaths(SpaceSimulation.FlightPlan.empty(),
                new FlightPath(List.of(sparse), List.of(), 10),
                point -> point.x() == 100 ? new RocketMapPaths.Point(40, 0) : point);
        var denseGeometry = new RocketMapPaths(SpaceSimulation.FlightPlan.empty(),
                new FlightPath(List.of(dense), List.of(), 10),
                point -> point.x() == 100 ? new RocketMapPaths.Point(40, 0) : point);
        for (int tick = 0; tick <= 200; tick++) {
            var a = sparseGeometry.renderedWorldPosition(ID, tick / 20d, -1, -1);
            var b = denseGeometry.renderedWorldPosition(ID, tick / 20d, -1, -1);
            assertEquals(a.x(), b.x(), 1e-9);
            assertEquals(a.y(), b.y(), 1e-9);
        }
    }

    @Test
    void aShortSurfaceLegPreservesWaypointDirectionAndDoesNotMagnifyItsOvershoot() {

        var waypoint = sample(10, 100, 0, 10, 0);
        var nextAction = new UUID(0, 9);
        var overshoot = new PathSample(15, 1_000, 20, 10, 10, 0, PathPhase.BRAKE,
                ID, nextAction, 1, Set.of(), Set.of(), SpaceSimulation.FlightPlanAction.NO_TARGET);
        var end = new PathSample(20, 101, 0, 0, 0, 0, PathPhase.BRAKE,
                ID, nextAction, 1, Set.of(), Set.of(), SpaceSimulation.FlightPlanAction.NO_TARGET);
        var path = path(sample(0, 0, 0, 10, 0), waypoint, overshoot, end);
        var geometry = new RocketMapPaths(SpaceSimulation.FlightPlan.empty(),
                new FlightPath(List.of(path), List.of(), 20),
                point -> point.x() == 101 ? new RocketMapPaths.Point(101, 30) : point);
        var before = geometry.renderedWorldPosition(ID, 10 - 1e-4, 0, 0);
        var at = geometry.renderedWorldPosition(ID, 10, 0, 0);
        var after = geometry.renderedWorldPosition(ID, 10 + 1e-4, 0, 0);
        assertTrue(after.x() > at.x());
        assertEquals(at.x() - before.x(), after.x() - at.x(), 1e-5);
        assertEquals(at.y() - before.y(), after.y() - at.y(), 1e-5);
        var mappedOvershoot = geometry.renderedWorldPosition(ID, 15, 0, 0);
        assertEquals(1_000, mappedOvershoot.x(), 1e-9);
        assertTrue(Math.abs(mappedOvershoot.y() - 20) <= 30);
        assertEquals(new RocketMapPaths.Point(101, 30), geometry.renderedWorldPosition(ID, 20, 0, 0));
    }

    @Test
    void boosterSeparationStartsAtTheParentsDisplayedCurve() {

        var parent = path(sample(0, 100, 0, -10, 0), sample(10, 0, 0, -10, 0));
        var childId = new UUID(0, 2);
        var child = new CraftPath(childId, Set.of(), List.of(sample(5, 50, 0, -10, 0), sample(10, 0, 0, -10, 0)),
                List.of(), 10, 0, TerminalState.READY);
        var event = new BoosterEvent(new UUID(0, 3), ID, childId, ID, null, 1, 5, 50, 0);
        var geometry = new RocketMapPaths(SpaceSimulation.FlightPlan.empty(),
                new FlightPath(List.of(child, parent), List.of(event), 10),
                point -> point.x() == 100 ? new RocketMapPaths.Point(40, 0) : point);
        assertEquals(20, geometry.renderedWorldPosition(ID, 5, -1, -1).x());
        assertEquals(geometry.renderedWorldPosition(ID, 5, -1, -1),
                geometry.renderedWorldPosition(childId, 5, -1, -1));
        assertEquals(20, geometry.renderedPathSegments(child).getFirst().from().x());
    }

    @Test
    void detachedCraftAndReleasedAsteroidsNeedNoDisplayOffsets() {

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
