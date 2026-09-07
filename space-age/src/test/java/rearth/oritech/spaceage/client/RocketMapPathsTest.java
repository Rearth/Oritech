package rearth.oritech.spaceage.client;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.*;

/** Check displayed paths without starting a Minecraft client or constructing GUI widgets. */
class RocketMapPathsTest {

    @Test
    void roundTripBendsInOppositeDirectionsAndKeepsEndpoints() {
        var outward = navigation(UUID.randomUUID());
        var returning = navigation(UUID.randomUUID());
        var plan = plan(outward, returning);
        var path = path(plan.root().id(), List.of(sample(0, 0, 0, outward),
                sample(10, 100, 0, outward), sample(20, 0, 0, returning)));
        var geometry = new RocketMapPaths(plan, new FlightPath(List.of(path), List.of(), 20));

        assertPoint(geometry.renderedWorldPosition(path.branchId(), 0, 0, 0), 0, 0);
        assertPoint(geometry.renderedWorldPosition(path.branchId(), 5, 50, 0), 50, 12);
        assertPoint(geometry.renderedWorldPosition(path.branchId(), 10, 100, 0), 100, 0);
        assertPoint(geometry.renderedWorldPosition(path.branchId(), 15, 50, 0), 50, -12);
        assertPoint(geometry.renderedWorldPosition(path.branchId(), 20, 0, 0), 0, 0);
        assertJoined(geometry.renderedPathSegments(path));
    }

    @Test
    void earlyStopKeepsOriginalCurveAcrossAnInstantCardAndContinuation() {
        var outward = navigation(UUID.randomUUID());
        var release = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.DECOUPLE);
        var continuation = navigation(outward.targetId());
        var plan = plan(outward, release, continuation);
        var path = path(plan.root().id(), List.of(sample(0, 0, 0, outward),
                sample(5, 50, 0, outward), sample(5, 50, 0, release), sample(10, 100, 0, continuation)));
        var abort = new NavigationAbortMoment(outward.id(), path.branchId(), UUID.randomUUID(),
                50, 5, 50, 0, 100, 0);
        var geometry = new RocketMapPaths(plan,
                new FlightPath(List.of(path), List.of(), 10, List.of(), List.of(), List.of(abort)));

        var segments = geometry.renderedPathSegments(path);
        assertTrue(segments.size() > 32, "Even sparse, stopped transfers need a smooth bend");
        assertPoint(geometry.renderedWorldPosition(path.branchId(), 2.5, 25, 0), 25, 9);
        assertPoint(geometry.renderedWorldPosition(path.branchId(), 5, 50, 0), 50, 12);
        assertPoint(geometry.renderedWorldPosition(path.branchId(), 7.5, 75, 0), 75, 9);
        assertJoined(segments);
    }

    @Test
    void childBranchesAndEmptyProgramsStartAtDisplayedSeparation() {
        var action = navigation(UUID.randomUUID());
        var plan = plan(action);
        var parent = path(plan.root().id(), List.of(sample(0, 0, 0, action), sample(10, 100, 0, action)));
        var child = path(UUID.randomUUID(), List.of(sample(5, 50, 0, action), sample(10, 0, 0, action)));
        var idleChild = path(UUID.randomUUID(), List.of(sample(5, 50, 0, action)));
        var events = List.of(booster(parent, child, action), booster(parent, idleChild, action));
        // Child-first order catches accidental dependence on rendering the parent first.
        var geometry = new RocketMapPaths(plan, new FlightPath(List.of(child, idleChild, parent), events, 10));

        assertPoint(geometry.renderedPathSegments(child).getFirst().from(), 50, 12);
        assertPoint(geometry.renderedWorldPosition(idleChild.branchId(), 5, 50, 0), 50, 12);
        assertPoint(geometry.renderedPathSegments(child).getLast().to(), 0, 0);
        assertJoined(geometry.renderedPathSegments(child));
    }

    @Test
    void releasedAsteroidStaysJoinedAndStillEndsAtItsPredictedImpact() {
        var action = navigation(UUID.randomUUID());
        var plan = plan(action);
        var asteroidId = UUID.randomUUID();
        var start = carrying(sample(0, 0, 0, action), asteroidId);
        var stop = carrying(sample(5, 50, 0, action), asteroidId);
        var path = path(plan.root().id(), List.of(start, stop));
        var asteroid = new AsteroidPath(asteroidId,
                List.of(new MotionSample(5, 50, 0, 10), new MotionSample(10, 100, 0, 10)), null, 0);
        var abort = new NavigationAbortMoment(action.id(), path.branchId(), UUID.randomUUID(),
                50, 5, 50, 0, 100, 0);
        var geometry = new RocketMapPaths(plan, new FlightPath(List.of(path), List.of(), 5,
                List.of(), List.of(asteroid), List.of(abort)));

        var segments = geometry.renderedAsteroidPathSegments(asteroid);
        assertPoint(segments.getFirst().from(), 50, 12);
        assertPoint(segments.getLast().to(), 100, 0);
    }

    @Test
    void stationaryCardsAndEmptyPathsStayFinite() {
        var action = navigation(UUID.randomUUID());
        var plan = plan(action);
        var stationary = path(plan.root().id(), List.of(sample(0, 3, 4, action), sample(0, 3, 4, action)));
        var empty = path(UUID.randomUUID(), List.of());
        var geometry = new RocketMapPaths(plan, new FlightPath(List.of(stationary, empty), List.of(), 0));
        assertPoint(geometry.renderedPathSegments(stationary).getFirst().to(), 3, 4);
        assertPoint(geometry.renderedWorldPosition(empty.branchId(), 0, 3, 4), 3, 4);
    }

    @Test
    void curvePiecesKeepTheOriginalPhaseAndSampleFractions() {
        var action = navigation(UUID.randomUUID());
        var plan = plan(action);
        var first = sample(0, 0, 0, action);
        var last = sample(10, 100, 0, action);
        var path = path(plan.root().id(), List.of(first, last));
        var geometry = new RocketMapPaths(plan, new FlightPath(List.of(path), List.of(), 10));
        double previousProgress = 0;
        for (var segment : geometry.renderedPathSegments(path)) {
            assertSame(first, segment.firstSample());
            assertSame(last, segment.secondSample());
            assertEquals(previousProgress, segment.sampleProgressFrom(), 1e-9);
            assertTrue(segment.sampleProgressTo() > previousProgress);
            previousProgress = segment.sampleProgressTo();
        }
        assertEquals(1, previousProgress, 1e-9);
        assertEquals(0, first.y(), "Decorative bends must not change the simulation");
        assertEquals(0, last.y());
    }

    private static SpaceSimulation.FlightPlanAction navigation(UUID target) {
        return SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO).withTarget(target);
    }

    private static SpaceSimulation.FlightPlan plan(SpaceSimulation.FlightPlanAction... actions) {
        var plan = SpaceSimulation.FlightPlan.empty();
        return plan.withBranches(List.of(plan.root().withActions(List.of(actions))));
    }

    private static PathSample sample(double time, double x, double y, SpaceSimulation.FlightPlanAction action) {
        return new PathSample(time, x, y, 10, 10, 0, PathPhase.COAST, action.targetId(), action.id(),
                1, Set.of(), Set.of(), SpaceSimulation.FlightPlanAction.NO_TARGET);
    }

    private static PathSample carrying(PathSample sample, UUID asteroid) {
        return new PathSample(sample.timeSeconds(), sample.x(), sample.y(), sample.speedMetersPerSecond(),
                sample.velocityX(), sample.velocityY(), sample.phase(), sample.targetId(), sample.actionId(),
                sample.stage(), sample.connectedSegments(), sample.firingSegments(), asteroid);
    }

    private static CraftPath path(UUID branch, List<PathSample> samples) {
        return new CraftPath(branch, Set.of(), samples, List.of(),
                samples.isEmpty() ? 0 : samples.getLast().timeSeconds(), 0, TerminalState.READY);
    }

    private static BoosterEvent booster(CraftPath parent, CraftPath child, SpaceSimulation.FlightPlanAction action) {
        return new BoosterEvent(UUID.randomUUID(), parent.branchId(), child.branchId(), action.id(),
                null, 1, 5, 50, 0);
    }

    private static void assertPoint(RocketMapPaths.Point point, double x, double y) {
        assertEquals(x, point.x(), 1e-8);
        assertEquals(y, point.y(), 1e-8);
    }

    private static void assertJoined(List<RocketMapPaths.RenderedPathSegment> segments) {
        for (int index = 1; index < segments.size(); index++) {
            assertPoint(segments.get(index).from(), segments.get(index - 1).to().x(), segments.get(index - 1).to().y());
        }
    }
}
