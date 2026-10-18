package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.recipe.BlockIngredient;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceSimulation.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

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
    void bulkDeletionOnlyRemovesOwnedCraftAndCleansDockingLinks() {

        var owner = UUID.randomUUID();
        var host = station(owner, 0);
        var visitor = station(owner, 0);
        var foreign = station(UUID.randomUUID(), 0);
        var data = new MissionSavedData();
        for (var state : List.of(host, visitor, foreign)) data.craft.put(state.rocket.getRocketId(), state);
        data.dockingLinks.add(new DockingLink(host.rocket.getRocketId(), visitor.rocket.getRocketId(),
                RocketServiceSettings.HardwareRef.NONE, RocketServiceSettings.HardwareRef.NONE));
        data.deleteCrafts(owner, List.of(host.rocket.getRocketId(), foreign.rocket.getRocketId(), UUID.randomUUID()));
        assertFalse(data.craft.containsKey(host.rocket.getRocketId()));
        assertTrue(data.craft.containsKey(visitor.rocket.getRocketId()));
        assertTrue(data.craft.containsKey(foreign.rocket.getRocketId()));
        assertTrue(data.dockingLinks.isEmpty());
    }

    private static void powerTick(MissionSavedData data) {

        data.power.update(null, data, data.extraTicks);
        data.power.advance(++data.extraTicks);
    }

    @Test
    void sunFlightKeepsItsIonStageThroughSeparationAndReload() {

        for (var retainCore : List.of(false, true)) {
            for (var reload : List.of(false, true)) checkSunFlightAfterSeparation(retainCore, reload);
        }
    }

    private static void checkSunFlightAfterSeparation(boolean retainCore, boolean reload) {

        var coreId = new UUID(32, 1);
        var boosterId = new UUID(32, 2);
        var core = new StaticRocketSegment(coreId, Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(2), Blocks.DAYLIGHT_DETECTOR.defaultBlockState())),
                Map.of(boosterId, Set.of()), 100, 1, 50_000_000, 0);
        var boosterBlocks = new HashSet<StaticRocketSegment.BlockData>();
        for (int x = 2; x < 10; x++) boosterBlocks.add(new StaticRocketSegment.BlockData(new BlockPos(x, 0, 0),
                SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()));
        var booster = new StaticRocketSegment(boosterId, boosterBlocks, Map.of(coreId, Set.of()), 10, 8, 0, 200_000);
        var rocket = new ActiveRocketData(Map.of(coreId, core, boosterId, booster), Map.of(
                coreId, new DynamicRocketSegment(0, 50_000_000, 0, Set.of(boosterId)),
                boosterId, new DynamicRocketSegment(200_000, 0, 0, Set.of(coreId))));
        var ascent = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH)
                .withVelocity(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0);
        var separate = FlightPlanAction.create(ActionType.DECOUPLE).withSegments(retainCore
                ? List.of(SegmentRef.of(core), SegmentRef.of(booster)) : List.of(SegmentRef.of(booster), SegmentRef.of(core)));
        var sun = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceSimulation.SUN_ID).withOrbit(OrbitBand.SOLAR);
        var base = FlightPlan.empty();
        var child = new FlightPlanBranch(new UUID(32, 3), separate.id(), retainCore ? List.of() : List.of(sun));
        var plan = base.withBranches(List.of(base.root().withActions(retainCore ? List.of(ascent, separate, sun) : List.of(ascent, separate)), child))
                .withSegmentConfigurations(List.of(
                        new SpaceSimulation.SegmentConfiguration(SegmentRef.of(core), "Core", false, List.of(2)),
                        new SpaceSimulation.SegmentConfiguration(SegmentRef.of(booster), "Booster", true, List.of(1))));
        var system = new SpaceSimulation();
        var forecast = RocketFlightPathCalculator.calculate(rocket, system.truth(), plan).paths().stream()
                .filter(path -> path.branchId().equals(retainCore ? plan.root().id() : child.id())).findFirst().orElseThrow();
        assertEquals(RocketFlightPathCalculator.TerminalState.READY, forecast.terminalState());
        var state = new MissionState(UUID.randomUUID(), rocket, plan, MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO, 0);
        var data = new MissionSavedData();
        data.craft.put(rocket.getRocketId(), state);
        var ticks = 0;
        while (!state.completed.contains(separate) && ticks < 100_000) {
            SpaceCommunications.poweredAntennas(state, true);
            MissionController.step(null, data, state, system, ticks++);
        }
        assertTrue(state.completed.contains(separate));
        if (!retainCore) state = data.craft.get(child.id());
        if (reload) {
            var encoded = MissionState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            state = MissionState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
            data.craft.put(state.rocket.getRocketId(), state);
        }
        assertEquals(sun.id(), state.action().id());
        assertEquals(1, state.rocket.getStaticSegments().size());
        assertTrue(state.rocket.getDynamicSegments().get(coreId).rf() > 0);
        assertTrue(Math.hypot(state.position.vx(), state.position.vy()) > 0);
        MissionController.step(null, data, state, system, ticks);
        assertEquals(RocketFlightPathCalculator.TerminalState.READY, state.transfer.path.paths().getFirst().terminalState(),
                "After separation, live guidance must still reach the core's configured second stage");
        assertEquals(2, state.position.stage());
        assertEquals(MissionState.STATUS_IN_FLIGHT, state.status);
        var deadline = ticks + Math.ceil(state.transfer.path.paths().getFirst().durationSeconds() * 20) + 100;
        while (state.action() != null && ticks < deadline) {
            SpaceCommunications.poweredAntennas(state, true);
            MissionController.step(null, data, state, system, ++ticks);
            if (state.navigationBlocked() || state.status.equals(MissionState.STATUS_REPLANNING))
                throw new AssertionError("Live Sun flight stopped at " + ticks + ": " + state.status);
        }
        assertTrue(state.action() == null, "Core must finish the Sun transfer");
        assertEquals(SpaceSimulation.SUN_ID, state.position.target());
        assertEquals(0, Math.hypot(state.position.vx(), state.position.vy()), 1e-5);
        assertTrue(state.rocket.getDynamicSegments().get(coreId).rf() > 0);
    }

    @Test
    void powerJumpsAcrossLongIntervalsAndStableFullBatteriesHaveNoEvents() {

        var state = station(UUID.randomUUID(), -3_000_000);
        var resources = state.rocket.getDynamicSegments().values().iterator().next();
        var data = new MissionSavedData();
        data.craft.put(state.rocket.getRocketId(), state);
        data.power.update(null, data, 0);
        data.power.advance(123.5);
        assertEquals(12_350, resources.rf(), 1e-7);
        var encoded = ActiveRocketData.CODEC.encodeStart(JsonOps.INSTANCE, state.rocket).getOrThrow();
        var restored = ActiveRocketData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(resources.rf(), restored.getDynamicSegments().values().iterator().next().rf(), 1e-7);
        data.power.advance(20L * 60 * 60 * 24 * 365);
        assertEquals(100_000, resources.rf(), 1e-7);
        assertEquals(Double.POSITIVE_INFINITY, data.power.nextEvent());
        assertTrue(data.power.revisions() <= 3, "A year of idle time must not become tick-by-tick work");
    }

    @Test
    void outsideResourceChangesSettleTheOldIntervalWithoutLosingOrReplayingRf() {

        var state = station(UUID.randomUUID(), -3_000_000);
        var resources = state.rocket.getDynamicSegments().values().iterator().next();
        var data = new MissionSavedData();
        data.craft.put(state.rocket.getRocketId(), state);
        data.power.update(null, data, 0);
        data.power.advance(100);
        resources.spendRF(2500);
        resources.receiveRF(100);
        assertEquals(7600, resources.rf(), 1e-7);
        data.power.update(null, data, 100);
        data.power.advance(200);
        assertEquals(17_600, resources.rf(), 1e-7);
        resources.layout.put(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        data.power.update(null, data, 200);
        data.power.advance(1000);
        assertEquals(17_600, resources.rf(), 1e-7);
        assertEquals(0, state.powerReport.solar());
    }

    @Test
    void solarLimitedJobsAdvanceAnalyticallyAndKeepFractionalWorkAcrossReload() {

        var state = station(UUID.randomUUID(), -3_000_000);
        var structure = state.rocket.getOriginalSegments().values().iterator().next();
        var resources = state.rocket.getDynamicSegments().values().iterator().next();
        var machinePos = new BlockPos(4, 0, 0);
        var blocks = new HashSet<>(structure.blocks());
        blocks.add(new StaticRocketSegment.BlockData(machinePos, SpaceAgeBlocks.VACUUM_CRAFTER.get().defaultBlockState()));
        resources.crafters.put(machinePos, new RocketControllerState());
        structure = new StaticRocketSegment(structure.segmentId(), blocks, Map.of(), 100, 0, 0, 0);
        state.rocket = new ActiveRocketData(Map.of(structure.segmentId(), structure), Map.of(structure.segmentId(), resources));
        var module = state.rocket.processingModules().getFirst();
        var pair = RocketLayout.pair(state.rocket, module);
        RocketLayout.setPair(state.rocket, pair, Blocks.IRON_BLOCK.defaultBlockState(), Blocks.DIAMOND_BLOCK.defaultBlockState());
        var recipe = new RecipeHolder<VacuumRecipe>(ResourceKey.create(Registries.RECIPE, Identifier.parse("oritech_space_age:solar_event_test")),
                new VacuumRecipe(BlockIngredient.of(Blocks.IRON_BLOCK), BlockIngredient.of(Blocks.DIAMOND_BLOCK), Blocks.GOLD_BLOCK.defaultBlockState(), 10, 1000));
        assertEquals("", PhysicalCrafting.start(state.rocket, module, state.rocket, pair, List.of(recipe), state.rocket.getRocketId()));
        var data = new MissionSavedData();
        data.craft.put(state.rocket.getRocketId(), state);
        data.power.update(null, data, 0);
        data.power.advance(50.5);
        var saved = MissionSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        var restored = MissionSavedData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        var craft = restored.craft.values().iterator().next();
        assertEquals(5, RocketProcessingService.controller(craft.rocket, module).job.elapsed());
        restored.power.update(null, restored, 50.5);
        restored.power.advance(100);
        assertTrue(RocketProcessingService.controller(craft.rocket, module).job == null);
        assertTrue(RocketLayout.state(craft.rocket, pair.controller().segment(), pair.left()).is(Blocks.GOLD_BLOCK));
        assertEquals(0, craft.rocket.getDynamicSegments().values().iterator().next().rf(), 1e-5);
        restored.power.advance(110);
        assertEquals(1000, craft.rocket.getDynamicSegments().values().iterator().next().rf(), 1e-5);
    }

    @Test
    void stationaryBlockedNavigationRechargesAndPowersCommandsButActualFlightDoesNot() {

        var state = station(UUID.randomUUID(), -3_070_000);
        var structure = state.rocket.getOriginalSegments().values().iterator().next();
        var blocks = new HashSet<>(structure.blocks());
        blocks.add(new StaticRocketSegment.BlockData(new BlockPos(0, 1, 0), SpaceAgeBlocks.ANTENNA.get().defaultBlockState()));
        structure = new StaticRocketSegment(structure.segmentId(), blocks, Map.of(), 100, 0, 0, 0);
        state.rocket = new ActiveRocketData(Map.of(structure.segmentId(), structure), state.rocket.getDynamicSegments());
        var navigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH);
        state.plan = state.plan.withBranches(List.of(state.plan.root().withActions(List.of(navigation))));
        var data = new MissionSavedData();
        data.craft.put(state.rocket.getRocketId(), state);
        var resources = state.rocket.getDynamicSegments().values().iterator().next();
        var time = 0;
        for (var status : List.of(MissionState.STATUS_NO_FEASIBLE_TRANSFER, MissionState.STATUS_TRANSFER_BLOCKED)) {
            resources.spendRF(resources.rf());
            state.status = status;
            data.power.update(null, data, time);
            assertTrue(SolarPower.stationary(state));
            assertEquals(1, SpaceCommunications.poweredAntennas(state, true));
            data.power.advance(time + 100);
            assertEquals((SolarPower.output(state) - SpaceBalance.ANTENNA_RF) * 100, resources.rf(), 1e-6);
            time += 100;
        }
        state.status = MissionState.STATUS_IN_FLIGHT;
        data.power.update(null, data, time);
        assertFalse(SolarPower.stationary(state));
        var before = resources.rf();
        data.power.advance(time + 100);
        assertEquals(before, resources.rf(), 1e-6);
        state.status = MissionState.STATUS_TRANSFER_BLOCKED;
        var p = state.position;
        state.position = new MissionState.Position(p.x(), p.y(), 1, 0, p.target(), p.orbit(), p.slot(), p.stage(), p.asteroid(), p.anchor());
        assertFalse(SolarPower.stationary(state), "Blocked status alone does not make a drifting craft stationary");
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
        assertEquals(1, BeamAperture.idealEfficiency(0, 5, false));
        assertEquals(1, BeamAperture.idealEfficiency(0, 5, true));
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
    void surfaceCollectionIncludesTheControllerCrossAndPartialEdgeBlocks() {

        assertEquals(1, BeamAperture.cellArea(0, 0, 4), 1e-12, "The controller face collects");
        assertEquals(1, BeamAperture.cellArea(2, 0, 4), 1e-12, "The plating cross collects");
        assertEquals(1, BeamAperture.cellArea(2, 2, 4), 1e-12, "Glass collects the same geometric area");
        assertEquals(0, BeamAperture.cellArea(5, 0, 4), 1e-12);
        assertEquals(Math.PI / 4, BeamAperture.cellArea(0, 0, .5), 1e-12);
        for (var radius : new double[]{1, 1.25, 2, 4, 4.25}) {
            var area = 0d;
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    var cell = BeamAperture.cellArea(x, z, radius);
                    assertTrue(cell >= 0 && cell <= 1);
                    assertEquals(cell, BeamAperture.cellArea(-z, x, radius), 1e-12);
                    area += cell;
                }
            }
            assertEquals(Math.PI * radius * radius, area, 1e-10);
            assertEquals(1, BeamAperture.collectionFraction(area, radius), "A contained beam has exact 100% coverage");
        }
        var edge = BeamAperture.cellArea(4, 0, 4);
        assertTrue(edge > 0 && edge < 1, "Partial blocks at the circular edge must not count as a whole block");
    }

    @Test
    void earthRelayRefocusesTheFinalLegForAMajorGroundCollectionGain() {

        // A 7x7 collector at the near side of Sun orbit, a 7x7 low Earth relay and a 7x7 ground array.
        var directRadius = BeamAperture.footprint(2_440_000, 7, true);
        var relayRadius = BeamAperture.footprint(10_000, 7, true);
        var relaySpaceRadius = BeamAperture.footprint(2_430_000, 7, false);
        var directShare = BeamAperture.collectionFraction(49, directRadius);
        var groundArea = 0d;
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++) groundArea += BeamAperture.cellArea(x, z, relayRadius);
        var relayShare = BeamAperture.collectionFraction(49, relaySpaceRadius)
                * BeamAperture.collectionFraction(groundArea, relayRadius);
        assertTrue(directRadius > 30);
        assertEquals(4, relayRadius);
        assertTrue(directShare < .02);
        assertTrue(relayShare > .45 && relayShare < 1);
        assertTrue(relayShare > directShare * 50);
        // Splitting a short link gains nothing once the whole beam already fits the receiver.
        var shortDirect = BeamAperture.collectionFraction(49, BeamAperture.footprint(20_000, 7, false));
        var shortHop = BeamAperture.collectionFraction(49, BeamAperture.footprint(10_000, 7, false));
        assertEquals(shortDirect, shortHop * shortHop, 1e-12);
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
        for (int i = 0; i < 2000; i++) powerTick(data);
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
    void solarPaysContinuousUpkeepWithoutDiscardingGenerationBeforeConsumption() {

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
        powerTick(data);
        assertEquals(1, SpaceCommunications.poweredAntennas(state, true));
        assertEquals(10, resources.rf());
        assertTrue(StationServiceRules.available(state));
        ((BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK).getDataMaps().put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS,
                Map.of(Blocks.DAYLIGHT_DETECTOR.builtInRegistryHolder().unwrapKey().orElseThrow(), 5d));
        data.power.update(null, data, data.extraTicks);
        data.power.advance(data.extraTicks + 100);
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
            assertEquals(15, array.area());
            assertEquals(15, array.cells().size());
            assertTrue(array.cells().contains(BlockPos.ZERO));
            assertEquals(8, array.quality());
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
        powerTick(data);
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
            powerTick(data);
            var system = new SpaceSimulation();
            var start = System.nanoTime();
            for (int tick = 0; tick < 100; tick++) {
                powerTick(data);
                RocketProcessingService.tickJobs(data, ignored -> system.truth());
                for (var station : data.craft.values()) {
                    SpaceCommunications.poweredAntennas(station, true);
                    MissionController.step(null, data, station, system, tick);
                }
                data.power.update(null, data, data.extraTicks);
                data.power.update(null, data, data.extraTicks);
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
