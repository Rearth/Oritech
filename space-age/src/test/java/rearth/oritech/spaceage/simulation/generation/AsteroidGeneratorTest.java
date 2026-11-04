package rearth.oritech.spaceage.simulation.generation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.simulation.DenseOres;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceObjects.SimulatedObject;
import rearth.oritech.spaceage.simulation.SpaceObjects.Asteroid;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Field;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Grouping;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.IntRange;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Layout;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.LayoutType;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Point;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Distance;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Type;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.Material;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.TypeSelection;
import rearth.oritech.spaceage.simulation.generation.AsteroidDefinitions.WeightedType;
import rearth.oritech.spaceage.simulation.generation.AsteroidGenerator.Definitions;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsteroidGeneratorTest {

    @Test
    void bundledDefinitionsGenerateRepeatableResourceRichAsteroidsInsideTheirRegions() {

        var definitions = AsteroidGenerator.defaults();
        var generated = AsteroidGenerator.generate(41, definitions);
        assertTrue(generated.stream().anyMatch(Asteroid.class::isInstance));
        assertTrue(generated.stream().anyMatch(object -> object.type == SpaceObjects.ObjectType.SURVEY_REGION));
        assertEquals(16, generated.stream().filter(object -> object.surveyHalfAngle > 0).count());
        assertEquals(encode(generated), encode(AsteroidGenerator.generate(41, definitions)));
        assertNotEquals(encode(generated), encode(AsteroidGenerator.generate(42, definitions)));
        assertEquals(generated.size(), generated.stream().map(object -> object.id).distinct().count());
        var regions = generated.stream().filter(object -> object.type == SpaceObjects.ObjectType.SURVEY_REGION).toList();
        for (var object : generated) {
            assertTrue(definitions.fields().containsKey(object.sourceField));
            if (!(object instanceof Asteroid asteroid)) continue;
            assertTrue(regions.stream().filter(region -> region.sourceField.equals(asteroid.sourceField))
                    .anyMatch(region -> SurveyRules.contains(data(region), asteroid.currentPosition.x, asteroid.currentPosition.y)));
            var volume = asteroid.materials.stream().mapToInt(SpaceObjects.AsteroidMaterial::amount).sum();
            assertTrue(volume >= 50_000 && volume <= 250_000);
            var filler = asteroid.materials.stream().filter(material -> material.block().getNamespace().equals("minecraft")
                    && List.of("stone", "deepslate").contains(material.block().getPath())).mapToInt(SpaceObjects.AsteroidMaterial::amount).sum();
            assertEquals(.2, (double) filler / volume, .0001);
            assertEquals(DenseOres.mass(asteroid.materials), asteroid.weight);
        }
    }

    @Test
    void groupedSelectionSharesCompositionWhilePerAsteroidSelectionCanMix() {

        var types = Map.of(OritechSpaceAge.id("a"), type("minecraft:iron_block"), OritechSpaceAge.id("b"), type("minecraft:gold_block"));
        for (var selection : TypeSelection.values()) {
            var field = field(selection);
            var generated = AsteroidGenerator.generate(19, new Definitions(types, Map.of(OritechSpaceAge.id("cloud"), field)));
            var groups = new HashMap<String, HashSet<Identifier>>();
            for (var object : generated) {
                if (!(object instanceof Asteroid asteroid)) continue;
                var key = asteroid.currentPosition.x + ":" + asteroid.currentPosition.y;
                groups.computeIfAbsent(key, ignored -> new HashSet<>()).add(asteroid.materials.getFirst().block());
            }
            assertEquals(2, groups.size());
            if (selection == TypeSelection.PER_GROUP) assertTrue(groups.values().stream().allMatch(set -> set.size() == 1));
            else assertTrue(groups.values().stream().anyMatch(set -> set.size() == 2));
        }
    }

    @Test
    void invalidRangesWeightsReferencesAndBlocksFailClearly() {

        assertTrue(IntRange.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"min\":5,\"max\":2}")).error().isPresent());
        assertTrue(Material.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"block\":\"minecraft:stone\",\"weight\":0}")).error().isPresent());
        var field = field(TypeSelection.PER_GROUP);
        assertThrows(IllegalArgumentException.class, () -> new Definitions(Map.of(), Map.of(OritechSpaceAge.id("missing"), field)));
        var invalid = new Definitions(Map.of(OritechSpaceAge.id("bad"), type("minecraft:unknown_block")), Map.of());
        assertThrows(IllegalArgumentException.class, invalid::validateBlocks);
        for (var air : List.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air")) {
            var airDefinition = new Definitions(Map.of(OritechSpaceAge.id("air"), type(air)), Map.of());
            assertThrows(IllegalArgumentException.class, airDefinition::validateBlocks);
        }
        var composition = AsteroidGenerator.defaults().types().values().iterator().next().composition(73);
        assertEquals(73, composition.stream().mapToInt(SpaceObjects.AsteroidMaterial::amount).sum());
    }

    @Test
    void fieldOrderAndUnrelatedFieldsDoNotChangeExistingGeneration() {

        var defs = AsteroidGenerator.defaults();
        var first = defs.fields().entrySet().iterator().next();
        var one = new Definitions(defs.types(), Map.of(first.getKey(), first.getValue()));
        var before = AsteroidGenerator.generate(11, one);
        var after = AsteroidGenerator.generate(11, defs).stream().filter(object -> first.getKey().equals(object.sourceField)).toList();
        assertEquals(encode(before), encode(after));
    }

    @Test
    void existingSystemsRetainResolvedAsteroidsAndSurveyKnowledgeOnLoad() {

        var simulation = new SpaceSimulation(8, AsteroidGenerator.defaults());
        var target = simulation.truth().stream().filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID).findFirst().orElseThrow();
        simulation.earthKnowledge.scan(List.of(target), 0, target.x(), target.y(), 1);
        var initial = SpaceSimulation.CODEC.encodeStart(NbtOps.INSTANCE, simulation).getOrThrow();
        var loaded = SpaceSimulation.CODEC.parse(NbtOps.INSTANCE, initial).getOrThrow();
        assertEquals(simulation.truth(), loaded.truth());
        assertEquals(simulation.id(), loaded.id());
        assertTrue(loaded.earthKnowledge.precise(target.id()));
    }

    private static Type type(String block) {

        return new Type(new IntRange(50, 50), List.of(new Material(Identifier.parse(block), 1)), 0);
    }

    private static Field field(TypeSelection selection) {

        return new Field(new Layout(LayoutType.CLOUD, new Point(100, 200), 1_000, 0, 1, 1, new Distance(0, 0)),
                new IntRange(60, 60), List.of(new WeightedType(OritechSpaceAge.id("a"), 1), new WeightedType(OritechSpaceAge.id("b"), 1)),
                Optional.of(new Grouping(new IntRange(2, 2), 0, selection)));
    }

    private static Object encode(List<SimulatedObject> objects) {

        return SimulatedObject.CODEC.listOf().encodeStart(NbtOps.INSTANCE, objects).getOrThrow();
    }

    private static SpaceSimulation.SpaceObjectData data(SimulatedObject object) {

        return new SpaceSimulation.SpaceObjectData(object.id, object.type, object.currentPosition.x, object.currentPosition.y,
                0, 0, object.radius, 0, 0, object.currentState, object.name, List.of(), object.surveyHalfAngle);
    }
}
