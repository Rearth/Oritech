package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.*;

class MissionSystemsTest {
    private static final UUID OWNER = new UUID(1, 1);

    private static ActiveRocketData rocket(boolean ion, long fuel, long rf) {
        var id = UUID.randomUUID();
        var block = ion ? SpaceAgeBlocks.ION_BOOSTER_ROCKET : SpaceAgeBlocks.BASIC_BOOSTER_ROCKET;
        var segment = new StaticRocketSegment(id,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, block.get().defaultBlockState())),
                Map.of(), 5, 1, rf, fuel);
        return new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(fuel, rf, 0, Set.of())));
    }

    private static FlightPlan plan(FlightPlanAction... actions) {
        var plan = FlightPlan.empty();
        return plan.withBranches(List.of(plan.root().withActions(List.of(actions))));
    }

    @Test
    void engineTypesConsumeTheirOwnResourceAndIonIsWeakInAtmosphere() {
        assertEquals(0, RocketPerformanceCalculator.calculate(rocket(false, 0, 1_000_000))
                .availableDeltaVMetersPerSecond());
        assertEquals(0, RocketPerformanceCalculator.calculate(rocket(true, 1_000_000, 0))
                .availableDeltaVMetersPerSecond());

        var ion = RocketPerformanceCalculator.calculate(rocket(true, 0, 1_000_000));
        assertEquals(ion.thrustNewtons() / ion.wetMassKilograms() * .1,
                ion.liftoffAccelerationMetersPerSecondSquared(), 1e-8);

        var chemical = rocket(false, 200, 1_234);
        var segment = chemical.getStaticSegments().values().iterator().next();
        var resources = chemical.getDynamicSegments().get(segment.segmentId());
        RocketHardware.of(segment).burn(resources, 1);
        assertEquals(180, resources.availableFuelBurnTimeTicks);
        assertEquals(1_234, resources.availableRF);
    }

    @Test
    void surveyKnowledgeStaysPrivateUntilMerged() {
        var system = new SpaceSimulation();
        var target = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.ASTEROID)
                .findFirst().orElseThrow();
        assertTrue(system.createObjectData().stream().noneMatch(o -> o.type() == SpaceObjects.ObjectType.ASTEROID));

        var first = new SurveyKnowledge();
        first.scan(List.of(target), 0, target.x() + 100_000, target.y(), SpaceBalance.SCAN_RANGE);
        assertTrue(first.precise(target.id()));
        assertFalse(system.earthKnowledge.precise(target.id()));

        system.earthKnowledge.merge(first);
        assertTrue(system.earthKnowledge.precise(target.id()));
        system.earthKnowledge.merge(first.copy());
        assertEquals(1, system.earthKnowledge.contacts().size());

        var received = system.createObjectData().stream().filter(o -> o.id().equals(target.id()))
                .findFirst().orElseThrow();
        system.moveAttached(target.id(), target.x() + 1_000_000, target.y());
        assertEquals(received, system.createObjectData().stream().filter(o -> o.id().equals(target.id()))
                .findFirst().orElseThrow());
    }

    @Test
    void regionScanReachesTheWholeRegionFromInsideItsBoundary() {
        var region = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.SURVEY_REGION,
                0, 0, 310_000, 0, SpaceObjects.DetectionState.PRECISE);
        var asteroid = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID,
                -300_000, 0, 2_000, 0, SpaceObjects.DetectionState.HIDDEN);
        var knowledge = new SurveyKnowledge();
        var craftX = region.radius() * 0.72;

        knowledge.scan(List.of(asteroid), 0, craftX, 0, SpaceBalance.SCAN_RANGE);
        assertNull(knowledge.contact(asteroid.id()));

        knowledge.scanRegion(List.of(asteroid), region, 0, craftX, 0, SpaceBalance.SCAN_RANGE);
        assertTrue(knowledge.precise(asteroid.id()));
    }

    @Test
    void completedSurveyRegionsDisappearAfterTheirContactsReachEarth() {
        var system = new SpaceSimulation();
        var region = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.SURVEY_REGION)
                .findFirst().orElseThrow();
        var asteroids = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.ASTEROID
                && Math.hypot(o.x() - region.x(), o.y() - region.y()) <= region.radius()).toList();
        assertFalse(asteroids.isEmpty());
        assertTrue(system.createObjectData().stream().anyMatch(o -> o.id().equals(region.id())));

        var delivered = new SurveyKnowledge();
        for (var asteroid : asteroids)
            delivered.scan(List.of(asteroid), 0, asteroid.x(), asteroid.y(), SpaceBalance.SCAN_RANGE);
        system.earthKnowledge.merge(delivered);

        assertTrue(system.createObjectData().stream().noneMatch(o -> o.id().equals(region.id())));
        assertTrue(asteroids.stream().allMatch(asteroid -> system.createObjectData().stream()
                .anyMatch(known -> known.id().equals(asteroid.id()))));
    }

    @Test
    void catastrophicImpactReplacesAsteroidWithNamedLocalDebrisField() {
        var system = new SpaceSimulation();
        var asteroid = system.truth().stream().filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID)
                .findFirst().orElseThrow();
        system.earthKnowledge.scan(List.of(asteroid), 0, asteroid.x(), asteroid.y(), SpaceBalance.SCAN_RANGE);
        var oldRegions = system.truth().stream().filter(object -> object.type() == SpaceObjects.ObjectType.SURVEY_REGION
                && Math.hypot(object.x() - asteroid.x(), object.y() - asteroid.y()) <= object.radius())
                .map(SpaceObjectData::id).toList();
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO)
                .withTarget(asteroid.id()).withOrbit(OrbitBand.SURFACE);
        var impact = AsteroidImpactRules.predictArrival(asteroid.mass() * 1_000,
                asteroid, 1_000, null, action);
        assertEquals(AsteroidImpactRules.FragmentationMode.CATASTROPHIC, impact.fragmentationMode());

        var debrisId = system.applyAsteroidImpact(asteroid.id(), impact);
        var truth = system.truth();
        var debris = truth.stream().filter(object -> object.id().equals(debrisId)).findFirst().orElseThrow();
        var fragments = truth.stream().filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID
                && object.name().contains("destroyed " + asteroid.name())).toList();

        assertTrue(truth.stream().noneMatch(object -> object.id().equals(asteroid.id())));
        assertFalse(system.earthKnowledge.precise(asteroid.id()));
        assertTrue(system.createObjectData().stream().noneMatch(object -> object.id().equals(asteroid.id())),
                "stale survey knowledge must not resurrect a destroyed asteroid");
        assertEquals(impact.fragmentCount(), fragments.size());
        assertTrue(fragments.stream().allMatch(fragment -> fragment.detectionState() == SpaceObjects.DetectionState.HIDDEN));
        assertEquals(asteroid.x(), debris.x());
        assertEquals(asteroid.y(), debris.y());
        assertTrue(debris.radius() < 30_000);
        assertEquals("Debris field of destroyed " + asteroid.name(), debris.name());
        assertTrue(oldRegions.stream().noneMatch(regionId -> truth.stream().anyMatch(object -> object.id().equals(regionId))));
    }

    @Test
    void missionExecutionKeepsSciencePrivateUntilUpload() {
        var system = new SpaceSimulation();
        var region = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.SURVEY_REGION)
                .findFirst().orElseThrow();
        var asteroid = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.ASTEROID
                && Math.hypot(o.x() - region.x(), o.y() - region.y()) <= region.radius()).findFirst().orElseThrow();
        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.SPACE_SCANNER.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState())),
                Map.of(), 8, 0);
        var resources = new DynamicRocketSegment(0, 1_000_000, 0, Set.of());
        var scan = FlightPlanAction.create(ActionType.SCAN);
        var transmit = FlightPlanAction.create(ActionType.TRANSMIT_INFORMATION);
        var mission = new MissionState(OWNER,
                new ActiveRocketData(Map.of(id, segment), Map.of(id, resources)), plan(scan, transmit),
                new MissionState.Position(region.x(), region.y(), 500, 0, region.id(), OrbitBand.SURFACE,
                        -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO)),
                new SurveyKnowledge(), BlockPos.ZERO, 0);
        var data = new MissionSavedData();

        for (int tick = 0; tick < 20; tick++) MissionController.step(null, data, mission, system, tick);
        assertEquals(transmit, mission.action());
        assertTrue(mission.knowledge.precise(asteroid.id()));
        assertFalse(system.earthKnowledge.precise(asteroid.id()));
        assertEquals(1_000_000 - SpaceBalance.SCAN_RF, resources.availableRF);

        MissionController.step(null, data, mission, system, 20);
        assertEquals(transmit, mission.action());
        mission.canTransmit = true;
        MissionController.step(null, data, mission, system, 21);
        assertNull(mission.action());
        assertTrue(system.earthKnowledge.precise(asteroid.id()));
    }

    @Test
    void missionUpdatesRequireCommandsAndPreserveExecutionState() {
        var system = new SpaceSimulation();
        var current = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var completed = FlightPlanAction.create(ActionType.SCAN);
        var mission = new MissionState(OWNER, rocket(true, 0, 1_000_000), plan(current),
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO, 0);
        mission.completed.add(completed);
        mission.actionTicks = 400;
        var replacement = plan(completed, current, FlightPlanAction.create(ActionType.DISCARD_CRAFT));

        assertFalse(MissionController.replace(mission, replacement, system));
        mission.connected = true;
        assertTrue(MissionController.replace(mission, replacement, system));
        assertEquals(current, mission.action());
        assertEquals(400, mission.actionTicks);
        assertEquals(List.of(completed), mission.completed);

        mission.ended = true;
        assertFalse(MissionController.replace(mission, replacement, system));
    }

    @Test
    void maintainPositionResourceConditionsAdvanceTheLiveMission() {
        var system = new SpaceSimulation();
        var data = new MissionSavedData();
        for (var type : ActionAddonType.values()) {
            if (!type.isMaintainPositionCondition()) continue;
            boolean rfCondition = type == ActionAddonType.LOW_RF;
            var craft = rocket(rfCondition, rfCondition ? 0 : 1_000, rfCondition ? 1_000 : 0);
            var resources = craft.getDynamicSegments().values().iterator().next();
            if (type == ActionAddonType.LOW_RF) resources.availableRF = 500;
            if (type == ActionAddonType.LOW_FUEL) resources.availableFuelBurnTimeTicks = 500;
            var condition = new ActionAddon(UUID.randomUUID(), type, 50);
            var maintain = FlightPlanAction.create(ActionType.MAINTAIN_POSITION).withAddons(List.of(condition));
            var discard = FlightPlanAction.create(ActionType.DISCARD_CRAFT);
            var mission = new MissionState(OWNER, craft, plan(maintain, discard),
                    MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO, 0);

            MissionController.step(null, data, mission, system, 0);
            assertEquals(discard, mission.action(), type + " did not complete maintain position");
            MissionController.step(null, data, mission, system, 1);
            assertTrue(mission.ended, type + " did not continue to the next action");
        }
    }

    @Test
    void maintainPositionCompletesWhenFuelOrCommunicationRfIsUnavailable() {
        var system = new SpaceSimulation();
        var data = new MissionSavedData();
        var maintain = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var discard = FlightPlanAction.create(ActionType.DISCARD_CRAFT);

        var fuelLimited = new MissionState(OWNER, rocket(false, 0, 1_000), plan(maintain, discard),
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO, 0);
        MissionController.step(null, data, fuelLimited, system, 0);
        assertEquals(discard, fuelLimited.action());

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState())),
                Map.of(), 5, 1, 1_000, 1_000);
        var rfLimitedRocket = new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(1_000, 0, 0, Set.of())));
        var rfLimited = new MissionState(OWNER, rfLimitedRocket, plan(maintain, discard),
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO, 0);
        MissionController.step(null, data, rfLimited, system, 0);
        assertEquals(discard, rfLimited.action());
    }

    @Test
    void liveStationKeepingCostDoesNotIncreaseWithEngineCount() {
        var maintain = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var oneEngine = stationKeepingRocket(1);
        var fourEngines = stationKeepingRocket(4);
        var oneMission = new MissionState(OWNER, oneEngine, plan(maintain), MissionState.Position.surface(),
                new SurveyKnowledge(), BlockPos.ZERO, 0);
        var fourMission = new MissionState(OWNER, fourEngines, plan(maintain), MissionState.Position.surface(),
                new SurveyKnowledge(), BlockPos.ZERO, 0);
        var data = new MissionSavedData();
        var system = new SpaceSimulation();

        for (int tick = 0; tick < SpaceBalance.STATION_KEEPING_INTERVAL_TICKS; tick++) {
            MissionController.step(null, data, oneMission, system, tick);
            MissionController.step(null, data, fourMission, system, tick);
        }

        var oneRemaining = oneEngine.getDynamicSegments().values().iterator().next().availableFuelBurnTimeTicks;
        var fourRemaining = fourEngines.getDynamicSegments().values().iterator().next().availableFuelBurnTimeTicks;
        assertTrue(oneRemaining < 20_000);
        assertEquals(oneRemaining, fourRemaining,
                "station keeping must charge delta-v rather than full-power time per engine");
    }

    private static ActiveRocketData stationKeepingRocket(int engines) {
        var id = UUID.randomUUID();
        var blocks = java.util.stream.IntStream.range(0, engines)
                .mapToObj(x -> new StaticRocketSegment.BlockData(new BlockPos(x, 0, 0),
                        SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()))
                .collect(java.util.stream.Collectors.toSet());
        var segment = new StaticRocketSegment(id, blocks, Map.of(), 25, engines, 0, 20_000);
        return new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(20_000, 0, 1_000, Set.of())));
    }

    @Test
    void activeNavigationContinuesDeterministicallyAfterSerialization() {
        var system = new SpaceSimulation();
        var data = new MissionSavedData();
        var craft = rocket(true, 0, 20_000_000);
        var navigate = FlightPlanAction.create(ActionType.NAVIGATE_TO)
                .withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH);
        var start = new MissionState.Position(-3_070_000, 0, 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.LOW, 0, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var original = new MissionState(OWNER, craft, plan(navigate), start,
                new SurveyKnowledge(), BlockPos.ZERO, 0);
        for (int tick = 0; tick < 100; tick++) MissionController.step(null, data, original, system, tick);

        var encoded = MissionState.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        var loaded = MissionState.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        for (int tick = 100; tick < 200; tick++) {
            MissionController.step(null, data, original, system, tick);
            MissionController.step(null, data, loaded, system, tick);
        }

        assertEquals(original.position, loaded.position);
        assertEquals(original.plan, loaded.plan);
        assertEquals(original.rocket.getDynamicSegments().values().iterator().next().availableRF,
                loaded.rocket.getDynamicSegments().values().iterator().next().availableRF);
    }

    @Test
    void recoveryHeightClearsTheCraftFootprint() {
        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(new BlockPos(0, -2, 0), Blocks.IRON_BLOCK.defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(2, 0, 0), Blocks.IRON_BLOCK.defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(0, 1, 2), Blocks.IRON_BLOCK.defaultBlockState())),
                Map.of(), 3, 0);
        var craft = new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(0, 0, 0, Set.of())));

        int originY = RocketRecovery.landingOriginY(craft, 100, 200,
                (x, z) -> x == 100 && z == 200 ? 70 : x == 102 ? 75 : 68);

        for (var block : segment.blocks()) {
            var relative = block.relativePos();
            int surface = relative.getX() == 0 && relative.getZ() == 0 ? 70
                    : relative.getX() == 2 ? 75 : 68;
            assertTrue(originY + relative.getY() >= surface);
        }
    }
}
