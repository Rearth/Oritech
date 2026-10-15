package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.util.HashMap;
import java.util.HashSet;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpacePowerTest {

    private Map<?, ?> previousSolar;

    @BeforeEach
    void solarData() {

        var registry = (BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK;
        previousSolar = registry.getDataMaps().get(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS);
        registry.getDataMaps().put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS,
                Map.of(Blocks.DAYLIGHT_DETECTOR.builtInRegistryHolder().unwrapKey().orElseThrow(), 100d));
    }

    @AfterEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void restoreData() {

        var maps = ((BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK).getDataMaps();
        if (previousSolar == null) maps.remove(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS);
        else maps.put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS, (Map) previousSolar);
    }

    private static MissionState station(UUID owner, double x) {

        var id = UUID.randomUUID();
        var structure = new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                Blocks.DAYLIGHT_DETECTOR.defaultBlockState())), Map.of(), 100, 0, 0, 0);
        var resources = new DynamicRocketSegment(0, 0, 0, Set.of());
        resources.rfCapacity = 100_000;
        var rocket = new ActiveRocketData(Map.of(id, structure), Map.of(id, resources));
        var action = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(action))), List.of());
        return new MissionState(owner, rocket, plan, new MissionState.Position(x, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.LOW,
                -1, 1, FlightPlanAction.NO_TARGET, SegmentRef.of(structure)), new SurveyKnowledge(), BlockPos.ZERO, 0);
    }

    @Test
    void automaticBeamSendingHonorsReserveAndApertureCapacity() {

        assertEquals(500, BeamAperture.availablePower(1000, 2000, 500, 20));
        assertEquals(200, BeamAperture.availablePower(600, 2000, 500, 20));
        assertEquals(0, BeamAperture.availablePower(399, 2000, 500, 20));
        assertEquals(0, BeamAperture.availablePower(2000, 2000, 500, 100));
        assertEquals(0, BeamAperture.availablePower(2000, 2000, 0, 0));
        var settings = new RocketServiceSettings.Beam(37);
        assertEquals(settings, RocketServiceSettings.Beam.CODEC.parse(JsonOps.INSTANCE,
                RocketServiceSettings.Beam.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow()).getOrThrow());
    }

    @Test
    void beamForecastThrottlesAtReserveAndMatchesSmallLiveSteps() {

        var budgets = List.of(new StationServiceRules.Budget(new RocketHardware(0, 0, 0, 0), 0, 100_000, 0, 100_000));
        assertEquals(RocketFlightPathCalculator.StationKeepingEnd.SUSTAINABLE,
                StationServiceRules.beamEstimate(budgets, 100, 10, 500, 20_000, null).end());
        assertEquals(RocketFlightPathCalculator.StationKeepingEnd.SUSTAINABLE,
                StationServiceRules.beamEstimate(budgets, 100, 10, 500, 20_000,
                        new ActionAddon(UUID.randomUUID(), ActionAddonType.LOW_RF, 10)).end());
        var limit = StationServiceRules.beamEstimate(budgets, 100, 10, 500, 20_000,
                new ActionAddon(UUID.randomUUID(), ActionAddonType.LOW_RF, 50));
        assertEquals((100_000 - 50_000) / 410d / 20, limit.seconds(), 1e-9);
        for (var net : new double[]{-10, 0, 90, 600}) {
            for (var initial : new double[]{10_000, 100_000}) {
                var live = initial;
                var steps = 10_000;
                for (int tick = 0; tick < steps; tick++) {
                    live = Math.max(0, live + net);
                    live -= BeamAperture.availablePower(live, 100_000, 500, 20);
                }
                assertEquals(live, StationServiceRules.beamRemainingRF(initial, 20_000, net, 500, steps), 500);
            }
        }
    }

    @Test
    void idealBeamPreviewHasNoDependencyOnChosenReceiver() {

        assertTrue(BeamAperture.idealEfficiency(50_000_000, 5, false) > BeamAperture.idealEfficiency(100_000_000, 5, false));
        assertTrue(BeamAperture.idealEfficiency(50_000_000, 7, false) > BeamAperture.idealEfficiency(50_000_000, 3, false));
        assertTrue(BeamAperture.idealEfficiency(50_000_000, 5, false) > BeamAperture.idealEfficiency(50_000_000, 5, true));
        assertEquals(.9, BeamAperture.idealEfficiency(0, 5, false));
        assertEquals(.9, BeamAperture.idealEfficiency(0, 5, true));
    }

    @Test
    void sharedBeamCollectionScalesWithAreaAndAtmosphere() {

        var vacuum = BeamAperture.footprint(2_000_000, 5, false);
        var surface = BeamAperture.footprint(2_000_000, 5, true);
        assertEquals(4, vacuum);
        assertEquals(40, surface);
        var spaceShare = BeamAperture.collectionFraction(16, vacuum);
        assertEquals(spaceShare / 100, BeamAperture.collectionFraction(16, surface), 1e-12);
        assertEquals(spaceShare * 2, BeamAperture.collectionFraction(32, vacuum), 1e-12);
        assertEquals(0, BeamAperture.collectionFraction(0, vacuum));
        assertEquals(1, BeamAperture.collectionFraction(1000, vacuum));
        assertEquals(1, BeamAperture.footprint(0, 5, false));
        assertEquals(4, BeamAperture.footprint(0, 5, true));
    }

    @Test
    void earthRelayRefocusesTheFinalLegForAMajorGroundCollectionGain() {

        // A 7x7 collector at the near side of Sun orbit, a 7x7 low Earth relay and a 7x7 ground array.
        var directRadius = BeamAperture.footprint(2_440_000, 7, true);
        var relayRadius = BeamAperture.footprint(10_000, 7, true);
        var relaySpaceRadius = BeamAperture.footprint(2_430_000, 7, false);
        var directShare = .9 * BeamAperture.collectionFraction(36, directRadius);
        // Four corner cells lie outside the radius-four ground circle; all plating is excluded.
        var relayShare = .9 * BeamAperture.collectionFraction(36, relaySpaceRadius)
                * .9 * BeamAperture.collectionFraction(32, relayRadius);
        assertTrue(directRadius > 30);
        assertEquals(4, relayRadius);
        assertTrue(directShare < .01);
        assertTrue(relayShare > .45 && relayShare < 1);
        assertTrue(relayShare > directShare * 50);
        // Splitting a short link gains nothing once the whole beam already fits the receiver.
        var shortDirect = .9 * BeamAperture.collectionFraction(36, BeamAperture.footprint(20_000, 7, false));
        var shortHop = .9 * BeamAperture.collectionFraction(36, BeamAperture.footprint(10_000, 7, false));
        assertTrue(shortHop * shortHop < shortDirect);
    }

    @Test
    void solarScalesCurtailedStorageAndSurvivesReload() {

        assertEquals(1, SolarPower.multiplier(-3_000_000, 0));
        assertEquals(4, SolarPower.multiplier(-1_500_000, 0));
        assertEquals(36, SolarPower.multiplier(500_000, 0));
        assertEquals(36, SolarPower.multiplier(0, 0));
        var state = station(UUID.randomUUID(), -3_000_000);
        var data = new MissionSavedData();
        data.craft.put(state.rocket.getRocketId(), state);
        for (int i = 0; i < 2000; i++) SpacePowerService.generate(data);
        assertEquals(100_000, state.rocket.getDynamicSegments().values().iterator().next().rf());
        assertTrue(StationServiceRules.available(state));
        assertTrue(StationServiceRules.upkeep(state, 100_000, 200));
        var saved = ActiveRocketData.CODEC.encodeStart(JsonOps.INSTANCE, state.rocket).getOrThrow();
        var restored = ActiveRocketData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        assertEquals(100, restored.powerHardware().solar());
        assertEquals(100_000, restored.getDynamicSegments().values().iterator().next().rf());
    }

    @Test
    void hardwareCacheTracksLayoutAndDataReload() {

        var state = station(UUID.randomUUID(), -3_000_000);
        var hardware = state.rocket.powerHardware();
        assertSame(hardware, state.rocket.powerHardware());
        var resources = state.rocket.getDynamicSegments().values().iterator().next();
        resources.layout.put(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        assertEquals(0, state.rocket.powerHardware().solar());
        resources.layout.clear();
        assertEquals(100, state.rocket.powerHardware().solar());
        ((BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK).getDataMaps().put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS, Map.of());
        assertEquals(0, state.rocket.powerHardware().solar());
        assertFalse(StationServiceRules.available(state));
    }

    @Test
    void solarCanPayExactUpkeepWithoutLeavingBatteryRfBehind() {

        var state = station(UUID.randomUUID(), -3_000_000);
        var structure = state.rocket.getOriginalSegments().values().iterator().next();
        var blocks = new HashSet<>(structure.blocks());
        blocks.add(new StaticRocketSegment.BlockData(new BlockPos(0, 1, 0), SpaceAgeBlocks.ANTENNA.get().defaultBlockState()));
        structure = new StaticRocketSegment(structure.segmentId(), blocks, Map.of(), 100, 0, 0, 0);
        var resources = state.rocket.getDynamicSegments().values().iterator().next();
        resources.rfCapacity = 10;
        state.rocket = new ActiveRocketData(Map.of(structure.segmentId(), structure), Map.of(structure.segmentId(), resources));
        var data = new MissionSavedData();
        data.craft.put(state.rocket.getRocketId(), state);
        SpacePowerService.generate(data);
        assertEquals(1, SpaceCommunications.poweredAntennas(state, true));
        assertEquals(0, resources.rf());
        assertTrue(StationServiceRules.available(state));
        ((BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK).getDataMaps().put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS,
                Map.of(Blocks.DAYLIGHT_DETECTOR.builtInRegistryHolder().unwrapKey().orElseThrow(), 5d));
        assertFalse(StationServiceRules.available(state));
    }

    @Test
    void fractionalGenerationDoesNotCreateOrLoseRf() {

        var resource = new DynamicRocketSegment(0, 0, 0, Set.of());
        resource.rfCapacity = 1000;
        for (int i = 0; i < 10_000; i++) {
            resource.receiveRF(.13);
            resource.spendRF(.08);
        }
        assertEquals(500, resource.rf(), 1e-8);
    }

    @Test
    void solarForecastUsesSustainableStateAndFiniteConditions() {

        var budget = List.of(new StationServiceRules.Budget(new RocketHardware(0, 0, 0, 1), 0, 10_000, 0, 10_000));
        var sustainable = StationServiceRules.solarEstimate(budget, 100, 10, null);
        assertEquals(RocketFlightPathCalculator.StationKeepingEnd.SUSTAINABLE, sustainable.end());
        assertTrue(Double.isFinite(sustainable.seconds()));
        var duration = StationServiceRules.solarEstimate(budget, 100, 10, new ActionAddon(UUID.randomUUID(), ActionAddonType.DURATION, 60));
        assertEquals(60, duration.seconds());
        var draining = StationServiceRules.solarEstimate(budget, 5, 10, new ActionAddon(UUID.randomUUID(), ActionAddonType.LOW_RF, 50));
        assertEquals(50, draining.seconds());
    }

    @Test
    void completeArraysWorkInEveryFacingAndStripsHavePoorFocus() {

        for (var facing : Direction.values()) {
            var horizontal = facing.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
            var vertical = facing.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
            var blocks = new HashMap<BlockPos, BlockState>();
            for (int x = -2; x <= 2; x++)
                for (int y = -1; y <= 1; y++)
                    if (x != 0 || y != 0) blocks.put(BlockPos.ZERO.relative(horizontal, x).relative(vertical, y),
                            (x == 0 || y == 0 ? Blocks.IRON_BLOCK : Blocks.GLASS).defaultBlockState());
            var array = BeamAperture.scan(BlockPos.ZERO, facing, p -> blocks.getOrDefault(p, Blocks.AIR.defaultBlockState()),
                    s -> s.is(Blocks.IRON_BLOCK), s -> s.is(Blocks.GLASS) ? 1 : 0);
            assertTrue(array.valid(), facing.toString());
            assertEquals(8, array.area());
            assertEquals(3, array.focus());
            blocks.remove(BlockPos.ZERO.relative(horizontal, 2).relative(vertical));
            assertFalse(BeamAperture.scan(BlockPos.ZERO, facing, p -> blocks.getOrDefault(p, Blocks.AIR.defaultBlockState()),
                    s -> s.is(Blocks.IRON_BLOCK), s -> s.is(Blocks.GLASS) ? 1 : 0).valid());
        }
        assertTrue(BeamAperture.footprint(2_000_000, 7, false) < BeamAperture.footprint(2_000_000, 3, false));
    }

    @Test
    void sunSlotsAreUniqueAndFullRingStaysUnassigned() {

        var owner = UUID.randomUUID();
        var data = new MissionSavedData();
        for (int i = 0; i < 201; i++) {
            var mission = station(owner, 500_000);
            var plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT,
                    List.of(FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceSimulation.SUN_ID).withOrbit(OrbitBand.SOLAR)))), List.of());
            mission.plan = MissionController.resolveOrbitSlots(plan, data, owner, mission.rocket.getRocketId());
            assertEquals(i < 200 ? i : -1, mission.action().service().slot());
            data.craft.put(mission.rocket.getRocketId(), mission);
        }
        assertEquals(6, SpaceBalance.slots(SpaceObjects.EARTH_ID, OrbitBand.LOW));
        assertEquals(0, SpaceBalance.slots(SpaceSimulation.SUN_ID, OrbitBand.LOW));
        assertEquals(200, SpaceBalance.slots(SpaceSimulation.SUN_ID, OrbitBand.SOLAR));
    }

    @Test
    void sunEntryAndSustainablePreviewUseActualHardware() {

        var mission = station(UUID.randomUUID(), -3_000_000);
        var system = new SpaceSimulation();
        var maintain = RocketFlightPathCalculator.calculateFrom(mission.rocket, system.createObjectData(), mission.plan, mission.position);
        assertEquals(RocketFlightPathCalculator.TerminalState.SUSTAINABLE, maintain.paths().getFirst().terminalState());
        assertEquals(RocketFlightPathCalculator.StationKeepingEnd.SUSTAINABLE, maintain.stationKeepingEstimates().getFirst().end());
        var launchNavigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceSimulation.SUN_ID).withOrbit(OrbitBand.SOLAR);
        var launchPlan = mission.plan.withBranches(List.of(mission.plan.root().withActions(List.of(launchNavigation))));
        var solarDeparture = RocketFlightPathCalculator.calculateFrom(mission.rocket, system.createObjectData(), launchPlan, MissionState.Position.surface());
        assertFalse(solarDeparture.paths().getFirst().terminalState() == RocketFlightPathCalculator.TerminalState.SOLAR_REQUIRED);
        var resources = mission.rocket.getDynamicSegments().values().iterator().next();
        resources.layout.put(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        var navigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceSimulation.SUN_ID).withOrbit(OrbitBand.SOLAR);
        var plan = mission.plan.withBranches(List.of(mission.plan.root().withActions(List.of(navigation))));
        var rejected = RocketFlightPathCalculator.calculateFrom(mission.rocket, system.createObjectData(), plan, mission.position);
        assertEquals(RocketFlightPathCalculator.TerminalState.SOLAR_REQUIRED, rejected.paths().getFirst().terminalState());
    }

    @Test
    void losingSolarReleasesSunSlotAndKeepsCraftAvailable() {

        var data = new MissionSavedData();
        var station = station(UUID.randomUUID(), 500_000);
        var p = station.position;
        station.position = new MissionState.Position(p.x(), p.y(), 0, 0, SpaceSimulation.SUN_ID, OrbitBand.SOLAR, 2, 1, p.asteroid(), p.anchor());
        data.craft.put(station.rocket.getRocketId(), station);
        station.rocket.getDynamicSegments().values().iterator().next().layout.put(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        SpacePowerService.generate(data);
        assertEquals(-1, station.position.slot());
        assertEquals("status.oritech_space_age.solar_lost", station.status);
        assertFalse(station.ended);
        assertTrue(station.plan.root().actions().isEmpty());
        assertEquals(1, data.craft.size());
    }

    @Test
    void stationaryFleetBenchmark() {

        for (var count : new int[]{200, 500}) {
            var data = new MissionSavedData();
            var owner = UUID.randomUUID();
            for (int i = 0; i < count; i++) {
                var station = station(owner, 500_000);
                var original = station.rocket.getOriginalSegments().values().iterator().next();
                var blocks = new HashSet<>(original.blocks());
                blocks.add(new StaticRocketSegment.BlockData(new BlockPos(0, 1, 0), SpaceAgeBlocks.ANTENNA.get().defaultBlockState()));
                for (int x = 1; x <= 20; x++)
                    for (int z = 1; z <= 20; z++) blocks.add(new StaticRocketSegment.BlockData(new BlockPos(x, 0, z), Blocks.GLASS.defaultBlockState()));
                var structure = new StaticRocketSegment(original.segmentId(), blocks, Map.of(), 1000, 0, 0, 0);
                station.rocket = new ActiveRocketData(station.rocket.getRocketId(), Map.of(structure.segmentId(), structure), station.rocket.getDynamicSegments(), null);
                data.craft.put(station.rocket.getRocketId(), station);
            }
            SpacePowerService.generate(data);
            var system = new SpaceSimulation();
            var start = System.nanoTime();
            for (int tick = 0; tick < 100; tick++) {
                SpacePowerService.generate(data);
                RocketProcessingService.tickJobs(data, ignored -> system.truth());
                for (var station : data.craft.values()) {
                    SpaceCommunications.poweredAntennas(station, true);
                    MissionController.step(null, data, station, system, tick);
                }
                SpacePowerService.beams(null, data, false);
                SpacePowerService.finishAccounting(data);
            }
            var elapsed = (System.nanoTime() - start) / 1_000_000d / 100;
            var full = data.craft.values().iterator().next();
            var summary = full.summary(100);
            var fullBytes = MissionState.Telemetry.CODEC.encodeStart(JsonOps.INSTANCE, full.telemetry(100)).getOrThrow().toString().length();
            var summaryBytes = MissionState.Telemetry.CODEC.encodeStart(JsonOps.INSTANCE, summary).getOrThrow().toString().length();
            assertTrue(summaryBytes < fullBytes / 4);
            System.out.printf("Fleet entry full/summary: %d/%d JSON bytes%n", fullBytes, summaryBytes);
            assertFalse(summary.fullLayout());
            assertTrue(summary.rocket().getStaticSegments().values().stream().allMatch(s -> s.blocks().isEmpty()));
            System.out.printf("Solar fleet %d: %.3f ms/station service tick%n", count, elapsed);
        }
    }
}
