package rearth.oritech.spaceage.client.hologram;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Craft;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Point;
import rearth.oritech.spaceage.block.hologram.HoloSurface;
import rearth.oritech.spaceage.block.hologram.HoloView;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.simulation.FleetCategories;
import rearth.oritech.spaceage.simulation.SpaceObjects.DetectionState;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.flight.FlightMotion;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.mission.MissionJourney;
import rearth.oritech.spaceage.simulation.mission.MissionJourney.Leg;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoloPathTest {

    private static final UUID ID = new UUID(0, 1);
    private static final Frame FRAME = new Frame(0, 0, 10_000);
    private static final SpaceObjectData EARTH = new SpaceObjectData(ID, ObjectType.EARTH, 0, 0, 1_000, 9.81f, DetectionState.PRECISE);

    private static HoloSnapshot snapshot(int facing) {

        return new HoloSnapshot(HoloSettings.DEFAULT, new HoloSurface(BlockPos.ZERO, 5, 6), List.of(EARTH), List.of(),
                FleetCategories.Saved.EMPTY, 0, 1, facing, "ready");
    }

    private static PathSample sample(double time, double x, double y, double vx, double vy) {

        return new PathSample(time, x, y, Math.hypot(vx, vy), vx, vy, PathPhase.COAST, ID, ID, 1, Set.of(), Set.of(), ID);
    }

    private static Craft craft(List<PathSample> samples, double time) {

        return new Craft(ID, "Tug", "uncategorized", 1_000, 0, 0, 0, ID, true, "ready", samples, List.of(), time);
    }

    @Test
    void surfacePointsMeetVisibleCubeAtEveryPadOrientationWithoutMovingOrbits() {

        for (var facing : new int[]{0, 90, 180, 270}) {
            var snapshot = snapshot(facing);
            for (var point : List.of(new Point(1_000, 0), new Point(-600, 800), new Point(0, -1_000))) {
                var mapped = HoloView.surfacePosition(point, FRAME, snapshot);
                var projected = HoloView.project(mapped.x(), mapped.y(), FRAME, snapshot);
                var center = HoloView.project(0, 0, FRAME, snapshot);
                assertEquals(HoloView.modelDiameter(EARTH, FRAME, snapshot) / 2,
                        Math.max(Math.abs(projected.x() - center.x()), Math.abs(projected.y() - center.y())), 1e-10);
                var roundTrip = HoloView.unproject(HoloView.project(point.x(), point.y(), FRAME, snapshot), FRAME, snapshot);
                assertEquals(point.x(), roundTrip.x(), 1e-9);
                assertEquals(point.y(), roundTrip.y(), 1e-9);
            }
            var orbit = new Point(2_000, 0);
            assertEquals(orbit, HoloView.surfacePosition(orbit, FRAME, snapshot));
        }
    }

    @Test
    void landingCurveAndMarkerShareSurfaceMappingAndPreserveOvershoot() {

        var samples = List.of(sample(0, -2_000, 300, 800, 0), sample(5, 2_000, 500, 0, 0), sample(10, 1_000, 0, 0, 0));
        var craft = craft(samples, 10);
        var route = new HoloPath(craft, snapshot(0), FRAME);
        var points = route.active();
        var endpoint = HoloView.surfacePosition(new Point(1_000, 0), FRAME, snapshot(0));
        assertEquals(endpoint.x(), points.getLast().x(), 1e-9);
        assertEquals(endpoint.y(), points.getLast().y(), 1e-9);
        assertEquals(endpoint, route.position(10, 1_000, 0));
        assertTrue(points.stream().anyMatch(p -> p.x() > endpoint.x() + 500), "The braking overshoot must not be flattened");
        var middle = route.position(5, 0, 0);
        var actual = FlightMotion.sample(samples, 5);
        assertEquals(actual.x() + (endpoint.x() - 1_000) * .5, middle.x(), 1e-9);
        assertEquals(500, middle.y(), 1e-9);
        assertTrue(points.size() > samples.size(), "Display curves must interpolate guidance rather than connect knots directly");
        assertEquals(samples, craft.path(), "Display mapping must not mutate flight guidance");
    }

    @Test
    void recordedJourneysAndStationaryCraftUseTheSameSurfaceEdge() {

        var route = new HoloPath(craft(List.of(), 0), snapshot(0), FRAME);
        var leg = new Leg(ID, List.of(new MissionJourney.Point(-2_000, 0), new MissionJourney.Point(1_000, 0)));
        assertEquals(route.position(0, 1_000, 0), route.journey(leg).getLast());
        var departure = new Leg(ID, List.of(new MissionJourney.Point(1_000, 0), new MissionJourney.Point(2_000, 0)));
        assertEquals(route.position(0, 1_000, 0), route.journey(departure).getFirst());
    }
}
