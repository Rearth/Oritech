package rearth.oritech.spaceage.client.starmap;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Items;
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
    void orbitRingsGainDetailAsTheyGrowOnScreen() {
        assertEquals(48, StarMapObjects.circleSegments(20));
        assertEquals(189, StarMapObjects.circleSegments(240));
        assertEquals(512, StarMapObjects.circleSegments(10_000));
    }
}
