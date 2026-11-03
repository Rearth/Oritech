package rearth.oritech.spaceage.client.hologram;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloSurface;
import rearth.oritech.spaceage.block.hologram.HoloView;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.simulation.FleetCategories;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceObjects.DetectionState;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoloPresentationTest {

    @Test
    void largestBodyMeshesFloatAboveThePanelsAtTheSharedPlane() throws Exception {

        var settings = HoloSettings.DEFAULT;
        var snapshot = new HoloSnapshot(settings, new HoloSurface(BlockPos.ZERO, 5, 6), List.of(), List.of(),
                FleetCategories.Saved.EMPTY, 0, 1, 0, "ready");
        var frame = new Frame(0, 0, 100);
        for (var name : List.of("earth", "mars", "sun", "luna", "phobos", "ceres", "vesta", "nyx", "echo", "asteroid_1", "asteroid_2", "asteroid_3")) {
            var asteroid = name.startsWith("asteroid");
            var object = new SpaceObjectData(UUID.randomUUID(), asteroid ? ObjectType.ASTEROID : ObjectType.RESOURCE_BODY,
                    0, 0, 60_000, 0, DetectionState.PRECISE);
            var size = HoloView.modelDiameter(object, frame, snapshot);
            try (var source = getClass().getResourceAsStream("/assets/oritech_space_age/models/block/" + name + ".json")) {
                var model = JsonParser.parseReader(new InputStreamReader(source, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var element : model.getAsJsonArray("elements")) {
                    var lowest = element.getAsJsonObject().getAsJsonArray("from").get(1).getAsDouble();
                    assertTrue(HoloView.PLANE_HEIGHT + (lowest / 16 - .5) * size / (asteroid ? 1 : 2) > 0,
                            name + " must not intersect the screen");
                }
            }
        }
        assertEquals(.5, HoloView.PLANE_HEIGHT);
    }

    @Test
    void asteroidsRemainReadableWithoutLettingPlanetsDominateTheProjection() {

        var snapshot = new HoloSnapshot(HoloSettings.DEFAULT, new HoloSurface(BlockPos.ZERO, 5, 6), List.of(), List.of(),
                FleetCategories.Saved.EMPTY, 0, 1, 0, "ready");
        var frame = new Frame(0, 0, 250_000);
        var earth = new SpaceObjectData(UUID.randomUUID(), ObjectType.EARTH, 0, 0, 60_000, 9.81f, DetectionState.PRECISE);
        var asteroid = new SpaceObjectData(UUID.randomUUID(), ObjectType.ASTEROID, 0, 0, 20, 0, DetectionState.PRECISE);
        var earthSize = HoloView.modelDiameter(earth, frame, snapshot);
        var asteroidSize = HoloView.modelDiameter(asteroid, frame, snapshot);
        assertTrue(earthSize <= .7);
        assertTrue(asteroidSize >= .16);
        assertTrue(earthSize / asteroidSize < 4);
    }
}
