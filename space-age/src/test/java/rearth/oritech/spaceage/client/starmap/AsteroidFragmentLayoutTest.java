package rearth.oritech.spaceage.client.starmap;

import org.junit.jupiter.api.Test;
import net.minecraft.resources.Identifier;
import rearth.oritech.spaceage.simulation.ResourceBodies;
import rearth.oritech.spaceage.simulation.generation.AsteroidGenerator;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules.ArrivalOutcome;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules.AsteroidFragment;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules.FragmentationMode;
import rearth.oritech.spaceage.simulation.AsteroidShape;
import rearth.oritech.spaceage.simulation.DenseOres;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects.AsteroidMaterial;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsteroidFragmentLayoutTest {

    @Test
    void newFragmentsAndSurvivingParentsAreIndividuallySelectableAtMaximumZoom() {

        for (int count : new int[]{1, 2, 3, 6, 24}) {
            for (var destroyed : List.of(false, true)) {
                var system = new SpaceSimulation(42, AsteroidGenerator.defaults());
                var parent = system.truth().stream().filter(body -> body.type() == ObjectType.ASTEROID).findFirst().orElseThrow();
                system.earthKnowledge.scan(List.of(parent), 0, parent.x(), parent.y(), 1);
                var previous = system.truth().stream().map(SpaceObjectData::id).collect(Collectors.toSet());
                var fragments = new ArrayList<AsteroidFragment>();
                for (int index = 0; index < count; index++) {
                    var materials = List.of(new AsteroidMaterial(DenseOres.Family.IRON.id(), 100 + index * 10));
                    fragments.add(new AsteroidFragment(DenseOres.mass(materials), AsteroidShape.size(materials).radius(), materials));
                }
                var impact = new AsteroidImpactRules.ImpactPrediction(ArrivalOutcome.ASTEROID_FRAGMENTATION, 1_000, 1, 1,
                        destroyed ? FragmentationMode.CATASTROPHIC : FragmentationMode.CHIPPING, fragments,
                        destroyed ? 0 : parent.mass() / 2, List.of(), 0, 0);
                var debrisId = system.applyAsteroidImpact(parent.id(), impact);
                assertEquals(!destroyed, system.createObjectData().stream().anyMatch(known -> known.id().equals(parent.id())),
                        "A surviving parent keeps its discovery; a destroyed parent disappears");
                var truth = system.truth();
                var debris = truth.stream().filter(body -> body.id().equals(debrisId)).findFirst().orElseThrow();
                var children = truth.stream().filter(body -> body.type() == ObjectType.ASTEROID && !previous.contains(body.id())).toList();
                assertEquals(count, children.size());
                system.earthKnowledge.scanRegion(truth, debris, 1, debris.x(), debris.y(), SpaceBalance.SCAN_RANGE);
                assertTrue(children.stream().allMatch(body -> system.createObjectData().stream().anyMatch(known -> known.id().equals(body.id()))),
                        "All fragments remain inside their surveyable debris field");
                var bodies = new ArrayList<>(children);
                if (!destroyed) bodies.add(truth.stream().filter(body -> body.id().equals(parent.id())).findFirst().orElseThrow());
                var camera = new StarMapCamera();
                camera.focus(List.of(new StarMapCamera.Point(-1, -1), new StarMapCamera.Point(1, 1)), 0, 0, 640, 400);
                var icons = bodies.stream().map(body -> {
                    var point = camera.project(body.x(), body.y(), 0, 0, 640, 400);
                    return new StarMapIconGroups.Icon(body.id(), ObjectType.ASTEROID, point.x(), point.y(), false);
                }).toList();
                assertTrue(new StarMapIconGroups().hidden(icons).isEmpty(), "Fragments must not collapse into an overlap group at maximum zoom");
                for (int first = 0; first < bodies.size(); first++) {
                    for (int second = first + 1; second < bodies.size(); second++) {
                        var a = bodies.get(first);
                        var b = bodies.get(second);
                        assertTrue(Math.hypot(a.x() - b.x(), a.y() - b.y()) >= SpaceBalance.ASTEROID_FRAGMENT_MIN_SEPARATION - .5);
                        var screenDistance = Math.hypot(icons.get(first).x() - icons.get(second).x(), icons.get(first).y() - icons.get(second).y());
                        var combinedIconHalfWidths = (StarMapObjects.displayRadius(a) + StarMapObjects.displayRadius(b)) * 1.3 / 2 * camera.zoom();
                        assertTrue(screenDistance > combinedIconHalfWidths + 12, "Each enlarged icon has a separate hover area on the tilted map");
                    }
                }
            }
        }
    }

    @Test
    void repeatedMoonImpactsAvoidEarlierFragmentsAndKeepDistinctNames() {

        var system = new SpaceSimulation(42, AsteroidGenerator.defaults());
        var definition = new ResourceBodies.Definition("Test moon", "", -3_250_000, 300_000, 18_000,
                Identifier.parse("oritech_space_age:luna"), List.of(new AsteroidMaterial(DenseOres.Family.IRON.id(), 1)));
        var moon = definition.object(Identifier.parse("oritech_space_age:test_moon"));
        system.setResourceBodies(List.of(moon));
        var material = List.of(new AsteroidMaterial(DenseOres.Family.IRON.id(), 10_000));
        var fragment = new AsteroidFragment(DenseOres.mass(material), AsteroidShape.size(material).radius(), material);
        var impact = new AsteroidImpactRules.ImpactPrediction(ArrivalOutcome.ASTEROID_FRAGMENTATION, 1_000, 1, 1,
                FragmentationMode.CHIPPING, List.of(fragment), 0, List.of(), 0, 0);
        for (int repeat = 0; repeat < 8; repeat++) system.applyAsteroidImpact(moon.id(), impact);
        var fragments = system.truth().stream().filter(body -> body.name().startsWith("Test moon impact fragment ")).toList();
        assertEquals(8, fragments.size());
        assertEquals(8, fragments.stream().map(SpaceObjectData::name).distinct().count());
        for (var first : fragments) {
            assertTrue(Math.hypot(first.x() - moon.x(), first.y() - moon.y()) < 200_000,
                    "Fragments stay near the moon instead of scaling the planet radius as an enlarged asteroid icon");
            for (var second : fragments) {
                if (first.id().equals(second.id())) continue;
                var clearance = Math.max(SpaceBalance.ASTEROID_FRAGMENT_MIN_SEPARATION,
                        (first.radius() + second.radius()) * SpaceBalance.ASTEROID_FRAGMENT_RADIUS_SPACING);
                assertTrue(Math.hypot(first.x() - second.x(), first.y() - second.y()) >= clearance - 1);
            }
        }
    }

}
