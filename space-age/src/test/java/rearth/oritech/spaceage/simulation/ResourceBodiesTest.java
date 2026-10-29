package rearth.oritech.spaceage.simulation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.ResourceBodies.Definition;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceBodiesTest {

    private static int blocks(AsteroidImpactRules.ImpactPrediction prediction) {

        return prediction.fragments().stream().flatMap(fragment -> fragment.materials().stream())
                .mapToInt(AsteroidMaterial::amount).sum();
    }

    private static Definition definition() {

        return new Definition("Test body", "A fixed deposit", -3_130_000, 0, 2_000,
                OritechSpaceAge.id("luna"), List.of(
                new AsteroidMaterial(Identifier.parse("minecraft:iron_ore"), 3),
                new AsteroidMaterial(Identifier.parse("minecraft:copper_ore"), 1)));
    }

    @Test
    void defaultDefinitionsHaveValidAssetsResourcesAndTwoNearbyMoons() throws Exception {

        var moons = 0;
        for (var key : List.of("luna", "phobos", "ceres", "vesta", "nyx", "echo")) {
            var path = "/data/oritech_space_age/oritech_space_age/resource_bodies/" + key + ".json";
            try (var reader = new InputStreamReader(getClass().getResourceAsStream(path), StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader);
                var definition = Definition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                assertEquals(json, Definition.CODEC.encodeStart(JsonOps.INSTANCE, definition).getOrThrow());
                assertFalse(definition.name().isBlank());
                assertFalse(definition.description().isBlank());
                assertTrue(definition.resources().size() >= 2 && definition.resources().size() <= 4);
                var body = definition.object(OritechSpaceAge.id(key));
                assertEquals(0, body.surfaceGravity());
                assertEquals(0, SpaceGravity.acceleration(body, 0));
                assertEquals(0, SpaceGravity.potential(body, 0));
                assertEquals(body, SpaceSimulation.SpaceObjectData.CODEC.parse(NbtOps.INSTANCE,
                        SpaceSimulation.SpaceObjectData.CODEC.encodeStart(NbtOps.INSTANCE, body).getOrThrow()).getOrThrow());
                assertNotNull(getClass().getResource("/assets/oritech_space_age/models/block/" + key + ".json"));
                assertNotNull(getClass().getResource("/assets/oritech_space_age/textures/block/" + key + "_surface.png"));
                if (key.equals("luna")) {
                    assertTrue(Math.hypot(body.x() - SpaceObjects.EARTH_X, body.y()) < 500_000);
                    moons++;
                } else if (key.equals("phobos")) {
                    assertTrue(Math.hypot(body.x() - 5_000_000, body.y() - 1_500_000) < 500_000);
                    moons++;
                }
            }
        }
        assertEquals(2, moons);
    }

    @Test
    void repeatedImpactsNeverDepleteOrMoveTheBodyAndDebrisSurvivesSaving() {

        var simulation = new SpaceSimulation();
        var body = definition().object(OritechSpaceAge.id("test_body"));
        simulation.setResourceBodies(List.of(body));
        assertTrue(simulation.createObjectData().contains(body), "Fixed bodies are known without a survey");
        var initialCount = simulation.truth().stream().filter(object -> object.type() == ObjectType.ASTEROID).count();
        var expectedChunks = 0;
        var counts = new HashSet<Integer>();
        for (int index = 0; index < 20; index++) {
            var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(body.id());
            var impact = AsteroidImpactRules.predictArrival(20_000, body, 300, null, action);
            assertEquals(impact, AsteroidImpactRules.predictArrival(20_000, body, 300, null, action));
            assertEquals(AsteroidImpactRules.FragmentationMode.CHIPPING, impact.fragmentationMode());
            assertTrue(impact.fragmentCount() >= 1 && impact.fragmentCount() <= 3);
            counts.add(impact.fragmentCount());
            assertEquals(300, impact.fragments().stream().mapToDouble(AsteroidImpactRules.AsteroidFragment::mass).sum());
            for (var fragment : impact.fragments()) {
                assertTrue(fragment.mass() > 0);
                assertEquals(fragment.mass(), DenseOres.mass(fragment.materials()));
                assertTrue(fragment.materials().stream().allMatch(material -> body.materials().stream()
                        .anyMatch(source -> source.block().equals(material.block()))));
            }
            expectedChunks += impact.fragmentCount();
            assertNotNull(simulation.applyAsteroidImpact(body.id(), impact));
            assertEquals(body, simulation.truth().stream().filter(object -> object.id().equals(body.id())).findFirst().orElseThrow());
        }
        assertTrue(counts.size() > 1);
        assertEquals(initialCount + expectedChunks, simulation.truth().stream().filter(object -> object.type() == ObjectType.ASTEROID).count());
        var loaded = SpaceSimulation.CODEC.parse(NbtOps.INSTANCE,
                SpaceSimulation.CODEC.encodeStart(NbtOps.INSTANCE, simulation).getOrThrow()).getOrThrow();
        loaded.setResourceBodies(List.of(body));
        assertEquals(simulation.truth(), loaded.truth());
    }

    @Test
    void speedMassAndPayloadIncreaseExcavationAndSafeArrivalsDoNotMine() {

        var body = definition().object(OritechSpaceAge.id("test_body"));
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO);
        var normal = AsteroidImpactRules.predictArrival(20_000, body, 300, null, action);
        var faster = AsteroidImpactRules.predictArrival(20_000, body, 600, null, action);
        var heavier = AsteroidImpactRules.predictArrival(40_000, body, 300, null, action);
        var payload = AsteroidImpactRules.predictArrival(20_000, body, 300, null, action, 900_000_000);
        assertEquals(75, blocks(normal));
        assertEquals(300, blocks(faster));
        assertEquals(150, blocks(heavier));
        assertEquals(150, blocks(payload));
        var safe = AsteroidImpactRules.predictArrival(20_000, body, 12, null, action, 1e15);
        assertEquals(AsteroidImpactRules.ArrivalOutcome.SAFE_APPROACH, safe.outcome());
        assertTrue(safe.fragments().isEmpty());
        var tiny = AsteroidImpactRules.predictArrival(1, body, 13, null, action);
        assertTrue(tiny.fragmentCount() >= 1 && tiny.fragmentCount() <= 3);
        assertTrue(tiny.fragments().stream().allMatch(fragment -> !fragment.materials().isEmpty()));
    }
}
