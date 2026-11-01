package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceObjects.DetectionState;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurveyBeltTest {

    @Test
    void sixteenSegmentsCoverTheThinBeltAndSurviveSaving() {

        var simulation = new SpaceSimulation();
        var truth = simulation.truth();
        var regions = truth.stream().filter(SurveyRules::beltSegment).toList();
        assertEquals(16, regions.size());
        var belt = truth.stream().filter(object -> object.type() == ObjectType.ASTEROID
                && Math.abs(Math.hypot(object.x(), object.y()) - 6_500_000) <= 310_001).toList();
        assertTrue(belt.size() >= 288, "The original belt population is retained");
        for (var asteroid : belt)
            assertTrue(regions.stream().anyMatch(region -> SurveyRules.contains(region, asteroid.x(), asteroid.y())));
        var loaded = SpaceSimulation.CODEC.parse(NbtOps.INSTANCE,
                SpaceSimulation.CODEC.encodeStart(NbtOps.INSTANCE, simulation).getOrThrow()).getOrThrow();
        assertEquals(truth, loaded.truth());
    }

    @Test
    void scanRevealsTheFullArcButNotTheNeighbouringSegment() {

        var region = new SpaceObjectData(UUID.randomUUID(), ObjectType.SURVEY_REGION, -6_500_000, 0,
                0, 0, 310_000, 0, 0, DetectionState.PRECISE, "Belt", List.of(), (float) (Math.PI / 16));
        var end = SurveyRules.beltPoint(region, region.surveyHalfAngle() * .95, 300_000);
        var neighbour = SurveyRules.beltPoint(region, region.surveyHalfAngle() * 1.5, 0);
        assertTrue(SurveyRules.contains(region, end.x(), end.y()), "Angles wrap correctly across minus/plus pi");
        assertFalse(SurveyRules.contains(region, neighbour.x(), neighbour.y()));
        assertEquals(190_000, SurveyRules.surveyDistance(region, -7_000_000, 0), .1);
        for (var point : SurveyRules.outline(region)) assertTrue(SurveyRules.contains(region, point.x(), point.y()));
        for (var point : SurveyRules.fogCenters(region)) assertTrue(SurveyRules.contains(region, point.x(), point.y()));
        var inside = asteroid(end.x(), end.y());
        var outside = asteroid(neighbour.x(), neighbour.y());
        var knowledge = new SurveyKnowledge();
        knowledge.scanRegion(List.of(region, inside, outside), region, 0, region.x(), region.y(), 1);
        assertTrue(knowledge.precise(inside.id()), "One survey reveals the complete long segment");
        assertFalse(knowledge.precise(outside.id()));
    }

    @Test
    void existingSavedRegionsAreNotRegeneratedFromNewDefinitions() {

        var simulation = new SpaceSimulation();
        var before = simulation.truth().stream().filter(object -> object.type() == ObjectType.ASTEROID).toList();
        var json = SpaceSimulation.CODEC.encodeStart(JsonOps.INSTANCE, simulation).getOrThrow().getAsJsonObject();
        var objects = json.getAsJsonArray("objects");
        var iterator = objects.iterator();
        while (iterator.hasNext()) {
            var object = iterator.next().getAsJsonObject();
            if (object.has("survey_half_angle") && object.get("survey_half_angle").getAsFloat() > 0) iterator.remove();
        }
        for (var i = 0; i < 72; i++) {
            var angle = i * Math.PI * 2 / 72;
            var region = new SpaceObjects.SimulatedObject(UUID.randomUUID(), ObjectType.SURVEY_REGION);
            region.currentPosition = new Vector2f((float) (Math.cos(angle) * 6_500_000), (float) (Math.sin(angle) * 6_500_000));
            region.radius = 310_000;
            objects.add(SpaceObjects.SimulatedObject.CODEC.encodeStart(JsonOps.INSTANCE, region).getOrThrow());
        }
        var loaded = SpaceSimulation.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(0, loaded.truth().stream().filter(SurveyRules::beltSegment).count());
        assertEquals(before, loaded.truth().stream().filter(object -> object.type() == ObjectType.ASTEROID).toList());
        assertEquals(72 + 6 + 10, loaded.truth().stream().filter(object -> object.type() == ObjectType.SURVEY_REGION).count());
    }

    private static SpaceObjectData asteroid(double x, double y) {

        return new SpaceObjectData(UUID.randomUUID(), ObjectType.ASTEROID, (float) x, (float) y,
                0, 0, 10, 0, 1, DetectionState.HIDDEN, "Test", List.of(
                new SpaceObjects.AsteroidMaterial(Identifier.parse("minecraft:stone"), 1)));
    }
}
