package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsteroidImpactRulesTest {

    @Test
    void predictsSafeLandingsFragmentsAndRecoverableBlocks() {
        var materials = List.of(
                new SpaceObjects.AsteroidMaterial(Identifier.parse("minecraft:stone"), 80),
                new SpaceObjects.AsteroidMaterial(Identifier.parse("minecraft:iron_ore"), 20));
        var asteroid = new SpaceSimulation.SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID,
                100_000, 0, 5, 0, 2_000, 0.01f, 20,
                SpaceObjects.DetectionState.PRECISE, "Test Asteroid", materials);
        var earth = new SpaceSimulation.SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 0, 0, 60_000, 9.81f, 0,
                SpaceObjects.DetectionState.PRECISE, "", List.of());
        var action = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceObjects.EARTH_ID).withOrbit(SpaceSimulation.OrbitBand.SURFACE)
                .withLanding(100, 200, 7, -4);

        var safe = AsteroidImpactRules.predictArrival(20_000, earth, 2, asteroid, action);
        assertEquals(AsteroidImpactRules.ArrivalOutcome.SAFE_APPROACH, safe.outcome());
        assertEquals(107, safe.landingX());
        assertEquals(196, safe.landingZ());
        assertEquals(materials, safe.recoverableMaterials());

        var impact = AsteroidImpactRules.predictArrival(20_000, earth, 300, asteroid, action);
        assertEquals(AsteroidImpactRules.ArrivalOutcome.HEAVY_IMPACT, impact.outcome());
        assertTrue(impact.craterRadiusBlocks() > 0);
        assertTrue(impact.recoverableMaterials().stream().mapToInt(SpaceObjects.AsteroidMaterial::amount).sum() < 100);

        var chipped = AsteroidImpactRules.predictArrival(20_000, asteroid, 300, null, action);
        assertEquals(AsteroidImpactRules.ArrivalOutcome.ASTEROID_FRAGMENTATION, chipped.outcome());
        assertEquals(AsteroidImpactRules.FragmentationMode.CHIPPING, chipped.fragmentationMode());
        assertTrue(chipped.fragmentCount() >= 2 && chipped.fragmentCount() <= 4);
        assertTrue(chipped.remainingTargetMass() > asteroid.mass() * 0.75f);

        var breakup = AsteroidImpactRules.predictArrival(20_000, asteroid, 1_000, null, action);
        assertEquals(AsteroidImpactRules.FragmentationMode.CATASTROPHIC, breakup.fragmentationMode());
        assertTrue(breakup.fragmentCount() >= 4 && breakup.fragmentCount() <= 8);
        assertEquals(0, breakup.remainingTargetMass());
        assertTrue(breakup.fragments().stream().map(AsteroidImpactRules.AsteroidFragment::mass).distinct().count() > 2,
                "catastrophic chunks vary in size");
        assertEquals(asteroid.mass(), breakup.fragments().stream()
                .mapToDouble(AsteroidImpactRules.AsteroidFragment::mass).sum(), 0.001);
        assertEquals(100, breakup.fragments().stream().flatMap(fragment -> fragment.materials().stream())
                .mapToInt(SpaceObjects.AsteroidMaterial::amount).sum());
    }

    @Test
    void attachedAsteroidMassReducesAvailableAcceleration() {
        var segmentId = UUID.randomUUID();
        var segment = new StaticRocketSegment(segmentId,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, Blocks.STONE.defaultBlockState())),
                Map.of(), 25, 1);
        var rocket = new ActiveRocketData(Map.of(segmentId, segment),
                Map.of(segmentId, new DynamicRocketSegment(0, 60_000_000, 0, Set.of())));
        var segmentRef = SpaceSimulation.SegmentRef.of(segment);
        var asteroidId = UUID.randomUUID();
        var objects = List.of(
                new SpaceSimulation.SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                        0, 0, 0, 0, 60_000, 9.81f, 0,
                        SpaceObjects.DetectionState.PRECISE, "", List.of()),
                new SpaceSimulation.SpaceObjectData(asteroidId, SpaceObjects.ObjectType.ASTEROID,
                        150_000, 0, 0, 0, 2_000, 0.01f, 100,
                        SpaceObjects.DetectionState.PRECISE, "Payload", List.of()));
        var arrive = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(asteroidId).withOrbit(SpaceSimulation.OrbitBand.TIGHT);
        var connect = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.CONNECT_ASTEROID)
                .withTarget(asteroidId).withOrbit(SpaceSimulation.OrbitBand.TIGHT)
                .withSegments(List.of(segmentRef));
        var returnToEarth = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceObjects.EARTH_ID).withOrbit(SpaceSimulation.OrbitBand.LOW);
        var base = SpaceSimulation.FlightPlan.empty();
        var attachedPlan = base.withBranches(List.of(base.root().withActions(List.of(arrive, connect, returnToEarth))));
        var attached = RocketFlightPathCalculator.calculate(rocket, objects, attachedPlan).paths().getFirst();

        assertTrue(attached.samples().stream().anyMatch(sample -> sample.attachedAsteroidId().equals(asteroidId)));
        assertTrue(attached.remainingDeltaV() < RocketFlightPathCalculator.calculate(rocket, objects,
                base.withBranches(List.of(base.root().withActions(List.of(arrive, returnToEarth)))))
                .paths().getFirst().remainingDeltaV());
    }

    @Test
    void releasedAsteroidStopsAtEarthAndConnectsFromEitherCloseOrbit() {
        var segmentId = UUID.randomUUID();
        var segment = new StaticRocketSegment(segmentId,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, Blocks.STONE.defaultBlockState())),
                Map.of(), 25, 1);
        var rocket = new ActiveRocketData(Map.of(segmentId, segment),
                Map.of(segmentId, new DynamicRocketSegment(0, 60_000_000, 0, Set.of())));
        var segmentRef = SpaceSimulation.SegmentRef.of(segment);
        var asteroidId = UUID.randomUUID();
        var earth = new SpaceSimulation.SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 0, 0, 60_000, 9.81f, 0,
                SpaceObjects.DetectionState.PRECISE, "", List.of());
        var asteroid = new SpaceSimulation.SpaceObjectData(asteroidId, SpaceObjects.ObjectType.ASTEROID,
                -500_000, 0, 0, 0, 2_000, 0.01f, 100,
                SpaceObjects.DetectionState.PRECISE, "Payload", List.of());
        var arrive = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(asteroidId).withOrbit(SpaceSimulation.OrbitBand.TIGHT);
        var connect = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.CONNECT_ASTEROID)
                .withTarget(asteroidId).withOrbit(SpaceSimulation.OrbitBand.SURFACE)
                .withSegments(List.of(segmentRef));
        var returnToEarth = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceObjects.EARTH_ID).withOrbit(SpaceSimulation.OrbitBand.SURFACE)
                .withVelocity(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0)
                .withAddons(List.of(new SpaceSimulation.ActionAddon(UUID.randomUUID(),
                        SpaceSimulation.ActionAddonType.DISTANCE_FROM_TARGET, 150_000)));
        var release = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.DECOUPLE)
                .withTarget(asteroidId).withSegments(List.of(segmentRef));
        var base = SpaceSimulation.FlightPlan.empty();
        var plan = base.withBranches(List.of(base.root().withActions(List.of(
                arrive, connect, returnToEarth, release))));

        var result = RocketFlightPathCalculator.calculate(rocket, List.of(earth, asteroid), plan);
        var moments = result.paths().getFirst().actionMoments();
        assertEquals(SpaceSimulation.FlightPlanAction.NO_TARGET, moments.get(0).attachedAsteroidId());
        assertEquals(asteroidId, moments.get(1).attachedAsteroidId(),
                "surface/tight selection should not prevent attachment");
        assertTrue(moments.stream().allMatch(RocketFlightPathCalculator.ActionMoment::completed));
        assertEquals(1, result.asteroidPaths().size());
        var asteroidPath = result.asteroidPaths().getFirst();
        assertTrue(asteroidPath.earthImpact() != null, "the released asteroid should intersect Earth");
        var last = asteroidPath.samples().getLast();
        assertEquals(earth.radius(), Math.hypot(last.x() - earth.xAt(last.timeSeconds()),
                last.y() - earth.yAt(last.timeSeconds())), 0.01,
                "the projected path should end on Earth's surface");

        var flyby = arrive.withVelocity(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0);
        var invalidPlan = base.withBranches(List.of(base.root().withActions(List.of(flyby, connect))));
        var invalidPath = RocketFlightPathCalculator.calculate(rocket, List.of(earth, asteroid), invalidPlan)
                .paths().getFirst();
        assertEquals(RocketFlightPathCalculator.TerminalState.PLAN_BLOCKED, invalidPath.terminalState());
        assertTrue(!invalidPath.actionMoments().getLast().completed(),
                "a maximum-speed asteroid flyby must not attach the asteroid");
        assertEquals(SpaceSimulation.FlightPlanAction.NO_TARGET,
                invalidPath.actionMoments().getLast().attachedAsteroidId());
    }
}
