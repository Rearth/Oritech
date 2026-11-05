package rearth.oritech.spaceage.client.starmap;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.contents.TranslatableContents;
import rearth.oritech.spaceage.simulation.DenseOres;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;
import java.util.Map;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarMapObjectsTest {

    @Test
    void asteroidMapEnlargementDoesNotChangePhysicalRadiusOrSizeTooltip() {

        var asteroid = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID,
                0, 0, 0, 0, 4_000, 0, 1, SpaceObjects.DetectionState.PRECISE, "Asteroid", List.of(
                new SpaceObjects.AsteroidMaterial(DenseOres.Family.IRON.id(), 50_000)));
        var physicalRadius = asteroid.radius();
        assertTrue(StarMapObjects.displayRadius(asteroid) > physicalRadius * 10);
        assertEquals(physicalRadius, asteroid.radius());
        var lines = StarMapTooltip.build(null, asteroid, null, null, SpaceSimulation.FlightPlan.empty(),
                Map.of(), Map.of(), ignored -> null, Map.of());
        var keys = lines.stream().map(line -> line.getContents()).filter(TranslatableContents.class::isInstance)
                .map(TranslatableContents.class::cast).map(TranslatableContents::getKey).toList();
        assertTrue(keys.contains("screen.oritech_space_age.object.physical_size"));
        assertFalse(keys.contains("screen.oritech_space_age.object.radius"));
    }

    @Test
    void physicalAsteroidHasVisibleSelectableTightOrbitOutsideItsIcon() {

        var asteroid = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID,
                0, 0, 0, 0, 4_000, 0, 1, SpaceObjects.DetectionState.PRECISE, "Asteroid", List.of(
                new SpaceObjects.AsteroidMaterial(DenseOres.Family.IRON.id(), 50_000)));
        Items.FIREWORK_ROCKET.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        Items.ENDER_EYE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        var objects = new StarMapObjects();
        objects.setSnapshot(new SpaceSimulation.FlightPlannerSnapshot(UUID.randomUUID(), UUID.randomUUID(),
                List.of(asteroid), SpaceSimulation.FlightPlan.empty()));
        var viewport = new StarMapObjects.Viewport(0, 0, 320, 200);
        var camera = new StarMapCamera();
        var selection = new RocketStarMapWidget.NavigationSelection(asteroid.id(), OrbitBand.TIGHT);
        camera.fit(objects.focusPoints(selection), 0, 0, viewport.width(), viewport.height());
        var radius = SurveyRules.orbitRadius(asteroid, OrbitBand.TIGHT);
        assertEquals(StarMapObjects.displayRadius(asteroid) * .75, radius, .001);
        assertEquals(radius, StarMapObjects.surfaceDiscRadius(asteroid));
        assertEquals(asteroid.radius(), SurveyRules.orbitRadius(asteroid, OrbitBand.SURFACE));
        var lines = new ArrayList<StarMapLineRenderer.Line>();
        objects.addReferenceLines(lines, camera, viewport, selection, objects.visibleIcons(camera, viewport, Set.of()));
        assertFalse(lines.isEmpty());
        var ring = camera.project(radius, 0, 0, 0, viewport.width(), viewport.height());
        assertEquals(selection, objects.findHoveredOrbit(ring.x(), ring.y(), camera, viewport, null));
    }

    @Test
    void offscreenSunStillHasVisibleAndSelectableCollectorOrbit() {

        var sun = new SpaceObjectData(SpaceSimulation.SUN_ID, SpaceObjects.ObjectType.SUN,
                0, 0, 250_000, 274, SpaceObjects.DetectionState.PRECISE);
        // Unit-test registries do not bind the map icons' default item components.
        Items.FIREWORK_ROCKET.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        Items.ENDER_EYE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        var objects = new StarMapObjects();
        objects.setSnapshot(new SpaceSimulation.FlightPlannerSnapshot(UUID.randomUUID(),
                UUID.randomUUID(), List.of(sun), SpaceSimulation.FlightPlan.empty()));
        var viewport = new StarMapObjects.Viewport(0, 0, 320, 200);
        var camera = new StarMapCamera();
        camera.fit(List.of(new StarMapCamera.Point(0, 0), new StarMapCamera.Point(1_000_000, 0)),
                0, 0, viewport.width(), viewport.height());
        var orbitX = sun.radius() + OrbitBand.SOLAR.altitude();
        camera.focus(List.of(new StarMapCamera.Point(orbitX - 1_000, -1_000),
                new StarMapCamera.Point(orbitX + 1_000, 1_000)), 0, 0, viewport.width(), viewport.height());
        var visible = objects.visibleIcons(camera, viewport, Set.of());
        assertTrue(visible.isEmpty(), "The Sun's model is fully offscreen");
        var lines = new ArrayList<StarMapLineRenderer.Line>();
        objects.addReferenceLines(lines, camera, viewport, null, visible);
        var clipped = StarMapLineSimplifier.simplify(lines, 0, 0, viewport.width(), viewport.height());
        assertFalse(clipped.isEmpty(), "The collector ring still crosses the viewport");
        assertEquals(new RocketStarMapWidget.NavigationSelection(sun.id(), OrbitBand.SOLAR),
                objects.findHoveredOrbit(160, 100, camera, viewport, null));
    }

    @Test
    void tugPlacementUsesCurrentMotionWithoutACommunicationOrObservationAgeGate() {

        Items.ENDER_EYE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        var tug = UUID.randomUUID();
        var asteroid = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID,
                1_000, 2_000, 0, 0, 10, 0, 1, SpaceObjects.DetectionState.PRECISE, "Tugged asteroid", List.of()).withTug(tug);
        var objects = new StarMapObjects();
        objects.setSnapshot(new SpaceSimulation.FlightPlannerSnapshot(UUID.randomUUID(), UUID.randomUUID(), List.of(asteroid), SpaceSimulation.FlightPlan.empty()));
        objects.setFleetMarkers(Set.of(tug));
        var camera = new StarMapCamera();
        camera.fit(List.of(new StarMapCamera.Point(0, 0), new StarMapCamera.Point(10_000, 10_000)), 0, 0, 320, 200);
        objects.beginTugFrame();
        objects.moveTug(tug, 3_000, 4_000, 3_500, 4_500, 24, 0, camera);
        var entry = objects.byId(asteroid.id());
        assertEquals(3_000, objects.displayedData(entry).x());
        assertEquals(24, objects.displayedData(entry).velocityX());
        assertEquals(4_500, objects.positions().getFirst().y());
        assertTrue(objects.positions().getFirst().x() < 3_500, "The asteroid sits beside the craft on its displayed route");
        var keys = StarMapTooltip.build(null, objects.displayedData(entry), null, null, SpaceSimulation.FlightPlan.empty(), Map.of(), Map.of(),
                ignored -> null, Map.of()).stream().map(line -> line.getContents()).filter(TranslatableContents.class::isInstance)
                .map(TranslatableContents.class::cast).map(TranslatableContents::getKey).toList();
        assertTrue(keys.contains("screen.oritech_space_age.object.tugged"));
        assertTrue(keys.contains("screen.oritech_space_age.object.velocity"));
        assertFalse(keys.contains("screen.oritech_space_age.object.observation_age"));
    }

}
