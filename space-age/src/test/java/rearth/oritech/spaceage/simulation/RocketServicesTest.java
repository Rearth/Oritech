package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.ReflectiveCodecBuilder;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.fluid.SimpleFluidStorage;
import rearth.oritech.init.FluidContent;
import rearth.oritech.spaceage.block.BlockPairController;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.recipe.BlockIngredient;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Docking;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Exchange;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.Processing;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.ResourceExchange;
import rearth.oritech.spaceage.simulation.RocketServiceSettings.SourceScope;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ServiceSettings;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketServicesTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final BlockPos MACHINE = new BlockPos(3, 0, 0);

    private static RecipeHolder<VacuumRecipe> recipe(String name) {

        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.parse("oritech_space_age:" + name)),
                new VacuumRecipe(BlockIngredient.of(Blocks.IRON_BLOCK), BlockIngredient.of(Blocks.DIAMOND_BLOCK), Blocks.GOLD_BLOCK.defaultBlockState(), 10, 100));
    }

    private static MissionState mission(int machines, int cargoCount) {

        var id = UUID.randomUUID();
        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()));
        blocks.add(new StaticRocketSegment.BlockData(new BlockPos(0, 1, 0), SpaceAgeBlocks.ROCKET_COUPLING.get().defaultBlockState()));
        var r = new DynamicRocketSegment(100000, 1000000, 1, Set.of());
        r.fuelCapacity = 200000;
        r.rfCapacity = 2000000;
        for (int n = 0; n < machines; n++) {
            var pos = MACHINE.offset(0, n * 3, 0);
            blocks.add(new StaticRocketSegment.BlockData(pos, SpaceAgeBlocks.VACUUM_CRAFTER.get().defaultBlockState()));
            r.crafters.put(pos, new RocketControllerState());
        }
        for (int n = 0; n < cargoCount; n++) {
            var pos = new BlockPos(8, n * 3, 0);
            blocks.add(new StaticRocketSegment.BlockData(pos, SpaceAgeBlocks.CARGO.get().defaultBlockState()));
            r.crafters.put(pos, new RocketControllerState());
        }
        var structure = new StaticRocketSegment(id, blocks, Map.of(), 20, 1, r.availableRF, r.availableFuelBurnTimeTicks);
        var rocket = new ActiveRocketData(Map.of(id, structure), Map.of(id, r));
        return new MissionState(OWNER, rocket, FlightPlan.empty(), new MissionState.Position(-3090000, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.HIGH, -1, 1,
                FlightPlanAction.NO_TARGET, SegmentRef.of(structure)), new SurveyKnowledge(), BlockPos.ZERO, 0);
    }

    private static void load(ActiveRocketData rocket, RocketControllerState.Pair pair) {

        RocketLayout.setPair(rocket, pair, Blocks.IRON_BLOCK.defaultBlockState(), Blocks.DIAMOND_BLOCK.defaultBlockState());
    }

    private static MissionSavedData fleet(MissionState... states) {

        var data = new MissionSavedData();
        for (var state : states) data.craft.put(state.rocket.getRocketId(), state);
        return data;
    }

    private static FlightPlanAction craft(MissionState requester, MissionState host) {

        var action = FlightPlanAction.create(ActionType.PROCESS).withSettings(RocketServiceSettings.DEFAULT.withProcessing(new Processing(
                RocketProcessingService.modules(host.rocket), SourceScope.BOTH, host == requester ? FlightPlanAction.NO_TARGET : host.rocket.getRocketId())));
        requester.plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(action))), List.of());
        return action;
    }

    private static void link(MissionSavedData data, MissionState visitor, MissionState host) {

        data.dockingLinks.add(new DockingLink(host.rocket.getRocketId(), visitor.rocket.getRocketId(), RocketDocking.ports(host.rocket).getFirst(),
                RocketDocking.ports(visitor.rocket).getFirst()));
    }

    private static void tick(MissionSavedData data, MissionState requester, FlightPlanAction action, int ticks) {

        var system = new SpaceSimulation();
        for (int n = 0; n < ticks; n++) {
            RocketProcessingService.tickJobs(data, owner -> system.truth());
            if (requester.action() != null)
                RocketProcessingService.process(data, requester, system, action, List.of(recipe("test")));
        }
    }

    @Test
    void facingUsesMarkedCellsInAllSixDirections() {

        for (var facing : Direction.values()) {
            var cells = BlockPairController.cells(BlockPos.ZERO, SpaceAgeBlocks.CARGO.get().defaultBlockState().setValue(BlockStateProperties.FACING, facing));
            assertEquals(1, cells.getFirst().distManhattan(BlockPos.ZERO));
            assertEquals(BlockPos.ZERO, cells.getFirst().offset(cells.getLast()));
            if (facing.getAxis().isVertical()) assertEquals(new BlockPos(-1, 0, 0), cells.getFirst());
        }
    }

    @Test
    void recipesAreUnorderedPhysicalBlocksAndRejectAmbiguity() {

        assertTrue(VacuumProcessing.match(Blocks.DIAMOND_BLOCK.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(), List.of(recipe("a"))).valid());
        assertEquals("ambiguous_recipe", VacuumProcessing.match(Blocks.DIAMOND_BLOCK.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(),
                List.of(recipe("a"), recipe("b"))).issue());
        assertEquals("no_valid_input_pair", VacuumProcessing.match(Blocks.AIR.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(), List.of(recipe("a"))).issue());
        var encoded = VacuumRecipe.CODEC.codec().encodeStart(JsonOps.INSTANCE, recipe("a").value()).getOrThrow();
        assertEquals(recipe("a").value(), VacuumRecipe.CODEC.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void lowOrbitAllowsCraftingAtEveryArrivalAngleDespiteCoordinateRounding() {

        var objects = new SpaceSimulation().createObjectData();
        var earth = objects.stream().filter(object -> object.id().equals(SpaceObjects.EARTH_ID)).findFirst().orElseThrow();
        var radius = earth.radius() + OrbitBand.LOW.altitude();
        for (var degree = 0; degree < 360; degree += 5) {
            var angle = Math.toRadians(degree);
            var x = earth.x() + Math.cos(angle) * radius;
            var y = earth.y() + Math.sin(angle) * radius;
            assertTrue(VacuumProcessing.zeroG(x, y, objects), "Low-orbit angle " + degree);
            assertTrue(VacuumProcessing.zeroGForDuration(x, y, 0, 0, 1200, objects));
            assertFalse(VacuumProcessing.zeroG(earth.x() + Math.cos(angle) * (radius - 1), earth.y() + Math.sin(angle) * (radius - 1), objects));
        }
    }

    @Test
    void lowOrbitProcessingSucceedsAndInsufficientPowerReportsTheSpecificCause() {

        var state = mission(1, 0);
        var system = new SpaceSimulation();
        var objects = system.createObjectData();
        var earth = objects.stream().filter(object -> object.id().equals(SpaceObjects.EARTH_ID)).findFirst().orElseThrow();
        var angle = Math.toRadians(65);
        var radius = earth.radius() + OrbitBand.LOW.altitude();
        var p = state.position;
        state.position = new MissionState.Position(earth.x() + Math.cos(angle) * radius, earth.y() + Math.sin(angle) * radius,
                0, 0, p.target(), OrbitBand.LOW, -1, p.stage(), p.asteroid(), p.anchor());
        load(state.rocket, RocketLayout.pair(state.rocket, RocketProcessingService.modules(state.rocket).getFirst()));
        craft(state, state);
        var forecast = RocketFlightPathCalculator.calculateFrom(state.rocket, objects, state.plan, state.position, List.of(recipe("a")));
        assertEquals("", forecast.processingEstimates().getFirst().issue());
        assertEquals(RocketFlightPathCalculator.TerminalState.READY, forecast.paths().getFirst().terminalState());

        state.rocket.getDynamicSegments().values().iterator().next().availableRF = 0;
        forecast = RocketFlightPathCalculator.calculateFrom(state.rocket, objects, state.plan, state.position, List.of(recipe("a")));
        assertEquals("processing_no_power", forecast.processingEstimates().getFirst().issue());
        assertFalse(forecast.paths().getFirst().actionMoments().getFirst().completed());
    }

    @Test
    void datapackRecipeNumbersHaveNoArbitraryUpperBounds() {

        var base = recipe("large").value();
        var large = new VacuumRecipe(base.first(), base.second(), base.resultState(), 25_000_000, 2_000_000);
        var encoded = VacuumRecipe.CODEC.codec().encodeStart(JsonOps.INSTANCE, large).getOrThrow();
        assertEquals(large, VacuumRecipe.CODEC.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void zeroRFRecipesMatchRuntimeAndForecastWithoutAPowerPool() {

        var state = mission(1, 1);
        var data = fleet(state);
        var module = RocketProcessingService.modules(state.rocket).getFirst();
        var pair = RocketLayout.pair(state.rocket, RocketLayout.controllers(state.rocket, false).getFirst());
        load(state.rocket, pair);
        state.rocket.getDynamicSegments().values().iterator().next().availableRF = 0;

        var base = recipe("free");
        var free = new RecipeHolder<>(base.id(), new VacuumRecipe(base.value().first(), base.value().second(), base.value().resultState(), 10, 0));
        var action = craft(state, state);
        var system = new SpaceSimulation();
        var path = RocketFlightPathCalculator.calculateFrom(state.rocket, system.createObjectData(), state.plan, state.position, List.of(free));
        assertEquals(10, path.processingEstimates().getFirst().ticks());
        assertEquals(0, path.processingEstimates().getFirst().rf());
        assertEquals(RocketFlightPathCalculator.TerminalState.READY, path.paths().getFirst().terminalState());

        for (var tick = 0; tick < 12; tick++) {
            RocketProcessingService.tickJobs(data, owner -> system.truth());
            if (state.action() != null) RocketProcessingService.process(data, state, system, action, List.of(free));
        }
        assertNull(state.action());
        assertNull(RocketProcessingService.controller(state.rocket, module).job);
        assertTrue(RocketLayout.state(state.rocket, pair.controller().segment(), pair.left()).is(Blocks.GOLD_BLOCK));
        assertEquals(0, state.rocket.getDynamicSegments().values().iterator().next().availableRF);
    }

    @Test
    void localPairPrecedesCargoAndPairReservationPreventsDoubleConsumption() {

        var state = mission(2, 1);
        var modules = RocketProcessingService.modules(state.rocket);
        var local = RocketLayout.pair(state.rocket, modules.getFirst());
        load(state.rocket, local);
        var cargo = RocketLayout.pair(state.rocket, RocketLayout.controllers(state.rocket, false).getFirst());
        load(state.rocket, cargo);
        var settings = new Processing(modules, SourceScope.BOTH, FlightPlanAction.NO_TARGET);
        assertEquals(local, PhysicalCrafting.candidates(state.rocket, state.rocket, settings, List.of(recipe("a"))).getFirst());
        var mass = RocketPerformanceCalculator.calculate(state.rocket).wetMassKilograms();
        assertEquals("", PhysicalCrafting.start(state.rocket, modules.getFirst(), state.rocket, cargo, List.of(recipe("a")), state.rocket.getRocketId()));
        assertTrue(RocketLayout.busy(state.rocket, cargo));
        assertEquals(mass, RocketPerformanceCalculator.calculate(state.rocket).wetMassKilograms());
        assertEquals("module_busy", PhysicalCrafting.start(state.rocket, modules.getLast(), state.rocket, cargo, List.of(recipe("a")), state.rocket.getRocketId()));
        assertTrue(RocketLayout.state(state.rocket, cargo.controller().segment(), cargo.left()).isAir());
    }

    @Test
    void stationProcessesVisitorInPlaceUsingHostPowerAndHoldsDeparture() {

        var host = mission(1, 0);
        var visitor = mission(0, 1);
        var data = fleet(host, visitor);
        link(data, visitor, host);
        var pair = RocketLayout.pair(visitor.rocket, RocketLayout.controllers(visitor.rocket, false).getFirst());
        load(visitor.rocket, pair);
        var action = craft(visitor, host);
        var power = visitor.rocket.getDynamicSegments().values().iterator().next().availableRF;
        tick(data, visitor, action, 1);
        assertTrue(RocketProcessingService.remoteBusy(data, visitor.rocket.getRocketId()));
        assertFalse(RocketDocking.undock(data, visitor));
        assertEquals(1, data.dockingLinks.size());
        tick(data, visitor, action, 10);
        assertNull(visitor.action());
        assertTrue(RocketLayout.state(visitor.rocket, pair.controller().segment(), pair.left()).is(Blocks.GOLD_BLOCK));
        assertTrue(RocketLayout.state(visitor.rocket, pair.controller().segment(), pair.right()).is(Blocks.GOLD_BLOCK));
        assertEquals(power, visitor.rocket.getDynamicSegments().values().iterator().next().availableRF);
        assertEquals(999000, host.rocket.getDynamicSegments().values().iterator().next().availableRF);
    }

    @Test
    void parallelMachinesFinishMoreCargoThanMachinesWithoutReprocessingOutputs() {

        var host = mission(2, 0);
        var visitor = mission(0, 5);
        var data = fleet(host, visitor);
        link(data, visitor, host);
        for (var ref : RocketLayout.controllers(visitor.rocket, false))
            load(visitor.rocket, RocketLayout.pair(visitor.rocket, ref));
        var action = craft(visitor, host);
        tick(data, visitor, action, 31);
        assertNull(visitor.action());
        assertEquals(995000, host.rocket.getDynamicSegments().values().iterator().next().availableRF);
        for (var ref : RocketLayout.controllers(visitor.rocket, false)) {
            var pair = RocketLayout.pair(visitor.rocket, ref);
            assertTrue(RocketLayout.state(visitor.rocket, ref.segment(), pair.left()).is(Blocks.GOLD_BLOCK));
        }
    }

    @Test
    void finitePassDoesNotAdmitLateCargoAndSurvivesReload() {

        var state = mission(1, 2);
        var data = fleet(state);
        var refs = RocketLayout.controllers(state.rocket, false);
        load(state.rocket, RocketLayout.pair(state.rocket, refs.getFirst()));
        var action = craft(state, state);
        tick(data, state, action, 4);
        var loaded = MissionSavedData.CODEC.parse(JsonOps.INSTANCE, MissionSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
        var resumed = loaded.craft.get(state.rocket.getRocketId());
        load(resumed.rocket, RocketLayout.pair(resumed.rocket, refs.getLast()));
        tick(loaded, resumed, action, 7);
        assertNull(resumed.action());
        assertTrue(RocketLayout.state(resumed.rocket, refs.getLast().segment(), RocketLayout.pair(resumed.rocket, refs.getLast()).left()).is(Blocks.IRON_BLOCK));
        assertTrue(RocketProcessingService.controller(state.rocket, RocketProcessingService.modules(state.rocket).getFirst()).job.elapsed() < 10);
    }

    @Test
    void missingPowerPausesAndCancellationRestoresInputsWithoutRefund() {

        var state = mission(1, 1);
        var data = fleet(state);
        var pair = RocketLayout.pair(state.rocket, RocketLayout.controllers(state.rocket, false).getFirst());
        load(state.rocket, pair);
        var r = state.rocket.getDynamicSegments().values().iterator().next();
        r.availableRF = 300;
        var action = craft(state, state);
        tick(data, state, action, 8);
        var machine = RocketProcessingService.controller(state.rocket, RocketProcessingService.modules(state.rocket).getFirst());
        assertEquals(3, machine.job.elapsed());
        assertEquals(0, r.availableRF);
        RocketProcessingService.cancel(data, state.rocket.getRocketId());
        assertNull(machine.job);
        assertTrue(r.reserved.isEmpty());
        assertTrue(RocketLayout.state(state.rocket, pair.controller().segment(), pair.left()).is(Blocks.IRON_BLOCK));
        assertEquals(0, r.availableRF);
    }

    @Test
    void destroyedHostReturnsSurvivingVisitorInputsAndDestroyedSourceDoesNotConjureBlocks() {

        var host = mission(1, 0);
        var visitor = mission(0, 1);
        var data = fleet(host, visitor);
        link(data, visitor, host);
        var pair = RocketLayout.pair(visitor.rocket, RocketLayout.controllers(visitor.rocket, false).getFirst());
        load(visitor.rocket, pair);
        var action = craft(visitor, host);
        tick(data, visitor, action, 1);
        host.ended = true;
        RocketProcessingService.tickJobs(data, owner -> new SpaceSimulation().truth());
        assertTrue(visitor.rocket.getDynamicSegments().values().iterator().next().reserved.isEmpty());
        assertTrue(RocketLayout.state(visitor.rocket, pair.controller().segment(), pair.left()).is(Blocks.IRON_BLOCK));
    }

    @Test
    void cargoMovesWholePartialPairsOnlyIntoEmptyUnreservedDestinations() {

        var source = mission(0, 3);
        var target = mission(0, 1);
        var refs = RocketLayout.controllers(source.rocket, false);
        load(source.rocket, RocketLayout.pair(source.rocket, refs.getFirst()));
        load(source.rocket, RocketLayout.pair(source.rocket, refs.get(1)));
        var partial = RocketLayout.pair(source.rocket, refs.getLast());
        RocketLayout.setPair(source.rocket, partial, Blocks.TNT.defaultBlockState(), Blocks.AIR.defaultBlockState());
        var settings = new Exchange(RocketServiceSettings.Direction.SEND, List.of(), List.of(), Identifier.parse("minecraft:air"), ResourceExchange.DEFAULT,
                ResourceExchange.DEFAULT);
        var result = RocketCargoTransfer.exchange(source.rocket, target.rocket, settings);
        assertEquals(1, result.moved());
        assertEquals(2, result.remaining());
        assertEquals(0, RocketCargoTransfer.exchange(source.rocket, target.rocket, settings).moved());
        var destination = RocketLayout.pair(target.rocket, RocketLayout.controllers(target.rocket, false).getFirst());
        assertFalse(RocketCargoTransfer.empty(target.rocket, destination));
    }

    @Test
    void instantResourceExchangeRespectsCapacityReserveAndSeparatesRFAndFuel() {

        var source = mission(0, 0);
        var target = mission(0, 0);
        var a = source.rocket.getDynamicSegments().values().iterator().next();
        var b = target.rocket.getDynamicSegments().values().iterator().next();
        a.availableRF = 1000;
        b.availableRF = 900;
        b.rfCapacity = 1000;
        assertEquals(100, RocketResourceTransfer.move(source.rocket, target.rocket, false, Long.MAX_VALUE, 100));
        assertEquals(900, a.availableRF);
        assertEquals(1000, b.availableRF);
        a.availableFuelBurnTimeTicks = 1000;
        a.currentFuelWeight = 1;
        b.availableFuelBurnTimeTicks = 0;
        b.fuelCapacity = 2000;
        b.currentFuelWeight = 0;
        assertEquals(750, RocketResourceTransfer.move(source.rocket, target.rocket, true, 1000, 250));
        assertEquals(.25, a.currentFuelWeight, 1e-9);
        assertEquals(.75, b.currentFuelWeight, 1e-9);
    }

    @Test
    void storageProbeCountsAliasesOnceAndRollsBackContents() {

        var energy = new DynamicEnergyStorage(10000, 10000, 10000, 5000, () -> {
        }, false);
        var scan = new RocketStorageScan(Map.of());
        try (var transaction = Transaction.openRoot()) {
            var first = scan.probe(null, energy, transaction);
            var second = scan.probe(null, energy, transaction);
            assertEquals(5000, first.rf());
            assertEquals(10000, first.rfCapacity());
            assertEquals(0, second.rf());
            assertEquals(0, second.rfCapacity());
        }
        assertEquals(5000, energy.getAmountAsLong());
    }

    @Test
    void turbofuelMassDependsOnVolumeAndDensityAndTheEngineHeavyRocketCanLaunch() {

        // Reproduce the reported 16-engine rocket with 213 buckets of turbofuel.
        var tank = new SimpleFluidStorage(256000, () -> {
        });
        var fluid = FluidContent.STILL_FUEL.get();
        // The unit-test bootstrap does not bind default components for modded fluids.
        if (!fluid.builtInRegistryHolder().areComponentsBound())
            fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        var turbofuel = FluidResource.of(fluid);
        try (var transaction = Transaction.openRoot()) {
            tank.insert(turbofuel, 213000, transaction);
            transaction.commit();
        }

        RocketStorageScan.Budget budget;
        try (var transaction = Transaction.openRoot()) {
            budget = new RocketStorageScan(Map.of(turbofuel, 3.2)).probe(tank, null, transaction);
            assertEquals(681600, budget.fuel());
            assertEquals(166.14, budget.fuelWeight(), 1e-9);
        }
        assertEquals(213000, tank.getAmount());

        // Changing a recipe's energy yield must not change the mass of the same fluid.
        try (var transaction = Transaction.openRoot()) {
            var weaker = new RocketStorageScan(Map.of(turbofuel, 1.0)).probe(tank, null, transaction);
            assertEquals(213000, weaker.fuel());
            assertEquals(budget.fuelWeight(), weaker.fuelWeight(), 1e-9);
        }

        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        for (var engine = 0; engine < 16; engine++) {
            blocks.add(new StaticRocketSegment.BlockData(new BlockPos(engine, 0, 0),
                    SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()));
        }
        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, blocks, Map.of(), 69, 16);
        var resources = new DynamicRocketSegment(budget.fuel(), 0, budget.fuelWeight(), Set.of());
        var rocket = new ActiveRocketData(Map.of(id, segment), Map.of(id, resources));
        var performance = RocketPerformanceCalculator.calculate(rocket);
        assertEquals(235140, performance.wetMassKilograms(), 1e-6);
        assertEquals(4000000 / 235140d, performance.liftoffAccelerationMetersPerSecondSquared(), 1e-9);
        assertEquals(RocketPerformanceCalculator.LaunchReadiness.READY,
                RocketPerformanceCalculator.getLaunchReadiness(rocket, FlightPlan.empty()));
    }

    @Test
    void dockingTransfersActualFuelMassInsteadOfRecreatingItFromBurnDuration() {

        var source = mission(0, 0);
        var destination = mission(0, 0);
        var sourcePool = source.rocket.getDynamicSegments().values().iterator().next();
        var destinationPool = destination.rocket.getDynamicSegments().values().iterator().next();
        sourcePool.availableFuelBurnTimeTicks = 1000;
        sourcePool.currentFuelWeight = .1;
        destinationPool.availableFuelBurnTimeTicks = 100;
        destinationPool.currentFuelWeight = .9;

        assertEquals(750, RocketResourceTransfer.move(source.rocket, destination.rocket, true, 750, 0));
        assertEquals(.025, sourcePool.currentFuelWeight, 1e-9);
        assertEquals(.975, destinationPool.currentFuelWeight, 1e-9);
        assertEquals(1, sourcePool.currentFuelWeight + destinationPool.currentFuelWeight, 1e-9);
        assertEquals(850, destinationPool.availableFuelBurnTimeTicks);
    }

    @Test
    void forecastMatchesRemoteProcessingDurationCostAndNeverMutatesOriginalLayouts() {

        var host = mission(2, 0);
        var visitor = mission(0, 5);
        var data = fleet(host, visitor);
        link(data, visitor, host);
        for (var ref : RocketLayout.controllers(visitor.rocket, false))
            load(visitor.rocket, RocketLayout.pair(visitor.rocket, ref));
        craft(visitor, host);
        var path = RocketFlightPathCalculator.calculateFrom(visitor.rocket, new SpaceSimulation().createObjectData(), visitor.plan, visitor.position,
                List.of(recipe("a")), RocketDocking.targets(data, OWNER));
        var estimate = path.processingEstimates().getFirst();
        assertEquals(30, estimate.ticks());
        assertEquals(5000, estimate.rf());
        assertEquals(RocketFlightPathCalculator.TerminalState.READY, path.paths().getFirst().terminalState());
        var pair = RocketLayout.pair(visitor.rocket, RocketLayout.controllers(visitor.rocket, false).getFirst());
        assertTrue(RocketLayout.state(visitor.rocket, pair.controller().segment(), pair.left()).is(Blocks.IRON_BLOCK));
        assertEquals(1000000, host.rocket.getDynamicSegments().values().iterator().next().availableRF);
    }

    @Test
    void dockingStillChecksRevisionPortsAndIndependentPersistedOwnership() {

        var host = mission(0, 0);
        var visitor = mission(0, 0);
        var data = fleet(host, visitor);
        var docking = new Docking(host.rocket.getRocketId(), RocketDocking.ports(visitor.rocket).getFirst(), RocketDocking.ports(host.rocket).getFirst(), 0,
                host.position.x(), host.position.y());
        host.positionRevision++;
        assertEquals("dock_target_moved", RocketDocking.check(data, visitor, docking));
        host.positionRevision = 0;
        visitor.plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(FlightPlanAction.create(ActionType.DOCK)))), List.of());
        assertTrue(RocketDocking.connect(data, visitor, docking));
        assertEquals(1, data.dockingLinks.size());
        var loaded = MissionSavedData.CODEC.parse(JsonOps.INSTANCE, MissionSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
        assertEquals(data.dockingLinks, loaded.dockingLinks);
        assertEquals(2, loaded.craft.size());
    }

    @Test
    void navigationReachesStationAndDockingAttachesWithoutAnotherTransfer() {

        var host = mission(0, 0);
        var visitor = mission(0, 0);
        var p = host.position;
        host.position = new MissionState.Position(p.x() + 2000, p.y(), 0, 0, p.target(), p.orbit(), p.slot(), p.stage(), p.asteroid(), p.anchor());
        var data = fleet(host, visitor);
        var target = RocketDocking.targets(data, OWNER).stream().filter(item -> item.id().equals(host.rocket.getRocketId())).findFirst().orElseThrow();
        var docking = target.rendezvous(RocketDocking.ports(visitor.rocket).getFirst(), target.ports().getFirst());
        var navigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(target.id()).withOrbit(OrbitBand.SURFACE)
                .withSettings(RocketServiceSettings.DEFAULT.withDocking(docking));
        var dock = FlightPlanAction.create(ActionType.DOCK).withSettings(RocketServiceSettings.DEFAULT.withDocking(docking));
        visitor.plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(navigation, dock))), List.of());
        var system = new SpaceSimulation();
        var targets = RocketDocking.targets(data, OWNER);
        assertTrue(RocketFlightPlanRules.inspect(visitor.plan, visitor.rocket, system.createObjectData(), false, visitor.position, List.of(), targets).issues().isEmpty());
        var forecast = RocketFlightPathCalculator.calculateFrom(visitor.rocket, system.createObjectData(), visitor.plan, visitor.position, List.of(), targets);
        var path = forecast.paths().getFirst();
        assertTrue(path.actionMoments().getFirst().completed());
        assertTrue(path.actionMoments().getLast().completed());
        assertEquals(path.actionMoments().getFirst().timeSeconds(), path.actionMoments().getLast().timeSeconds());
        assertEquals(target.position().x(), path.actionMoments().getLast().x(), 1);
        assertTrue(forecast.arrivalPredictions().isEmpty());

        for (var tick = 0; tick < 4000 && visitor.action() != null && visitor.action().type() == ActionType.NAVIGATE_TO; tick++)
            MissionController.step(null, data, visitor, system, tick);
        assertEquals(ActionType.DOCK, visitor.action().type());
        var fuel = visitor.rocket.getDynamicSegments().values().iterator().next().availableFuelBurnTimeTicks;
        MissionController.step(null, data, visitor, system, 4000);
        assertEquals(1, data.dockingLinks.size());
        assertEquals(fuel, visitor.rocket.getDynamicSegments().values().iterator().next().availableFuelBurnTimeTicks);
    }

    @Test
    void dockingFromFarAwayFailsWithoutMovingOrBurningFuel() {

        var host = mission(0, 0);
        var visitor = mission(0, 0);
        var data = fleet(host, visitor);
        var docking = new Docking(host.rocket.getRocketId(), RocketDocking.ports(visitor.rocket).getFirst(), RocketDocking.ports(host.rocket).getFirst(), 0,
                host.position.x(), host.position.y());
        var p = visitor.position;
        visitor.position = new MissionState.Position(p.x() + 2000, p.y(), 0, 0, p.target(), p.orbit(), p.slot(), p.stage(), p.asteroid(), p.anchor());
        visitor.plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT,
                List.of(FlightPlanAction.create(ActionType.DOCK).withSettings(RocketServiceSettings.DEFAULT.withDocking(docking))))), List.of());
        var position = visitor.position;
        var fuel = visitor.rocket.getDynamicSegments().values().iterator().next().availableFuelBurnTimeTicks;
        var system = new SpaceSimulation();
        var forecast = RocketFlightPathCalculator.calculateFrom(visitor.rocket, system.createObjectData(), visitor.plan, position, List.of(), RocketDocking.targets(data, OWNER));
        assertFalse(forecast.paths().getFirst().actionMoments().getFirst().completed());
        assertEquals(0, forecast.paths().getFirst().durationSeconds());
        MissionController.step(null, data, visitor, system, 1);
        assertEquals(position, visitor.position);
        assertEquals(fuel, visitor.rocket.getDynamicSegments().values().iterator().next().availableFuelBurnTimeTicks);
        assertTrue(data.dockingLinks.isEmpty());
        assertEquals("status.oritech_space_age.dock_approach_required", visitor.status);
    }

    @Test
    void navigationToMovedStationFailsInsteadOfChasingIt() {

        var host = mission(0, 0);
        var visitor = mission(0, 0);
        var data = fleet(host, visitor);
        var target = RocketDocking.targets(data, OWNER).stream().filter(item -> item.id().equals(host.rocket.getRocketId())).findFirst().orElseThrow();
        var docking = target.rendezvous(RocketDocking.ports(visitor.rocket).getFirst(), target.ports().getFirst());
        var navigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(target.id()).withOrbit(OrbitBand.SURFACE)
                .withSettings(RocketServiceSettings.DEFAULT.withDocking(docking));
        visitor.plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(navigation))), List.of());
        host.positionRevision++;
        var system = new SpaceSimulation();
        var forecast = RocketFlightPathCalculator.calculateFrom(visitor.rocket, system.createObjectData(), visitor.plan, visitor.position, List.of(), RocketDocking.targets(data, OWNER));
        assertFalse(forecast.paths().getFirst().actionMoments().getFirst().completed());
        var position = visitor.position;
        MissionController.step(null, data, visitor, system, 1);
        assertEquals(position, visitor.position);
        assertEquals("status.oritech_space_age.dock_target_moved", visitor.status);
        assertNull(visitor.leg);
    }

    @Test
    void selfMatchingProductsAreProcessedOnlyOnce() {

        var state = mission(1, 1);
        var data = fleet(state);
        var pair = RocketLayout.pair(state.rocket, RocketLayout.controllers(state.rocket, false).getFirst());
        RocketLayout.setPair(state.rocket, pair, Blocks.IRON_BLOCK.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState());
        var self = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.parse("oritech_space_age:self")),
                new VacuumRecipe(BlockIngredient.of(Blocks.IRON_BLOCK), BlockIngredient.of(Blocks.IRON_BLOCK), Blocks.IRON_BLOCK.defaultBlockState(), 2, 100));
        var action = craft(state, state);
        var system = new SpaceSimulation();
        for (int n = 0; n < 10; n++) {
            RocketProcessingService.tickJobs(data, owner -> system.truth());
            if (state.action() != null) RocketProcessingService.process(data, state, system, action, List.of(self));
        }
        assertNull(state.action());
        assertEquals(999800, state.rocket.getDynamicSegments().values().iterator().next().availableRF);
    }

    @Test
    void fallbackCannotStealAnotherMachinesLocalPair() {

        var state = mission(2, 0);
        var data = fleet(state);
        var modules = RocketProcessingService.modules(state.rocket);
        var pair = RocketLayout.pair(state.rocket, modules.getLast());
        load(state.rocket, pair);
        var action = craft(state, state);
        tick(data, state, action, 1);
        assertNull(RocketProcessingService.controller(state.rocket, modules.getFirst()).job);
        assertNotNull(RocketProcessingService.controller(state.rocket, modules.getLast()).job);
    }

    @Test
    void localMachineCanUseDirectlyDockedPartnerCargo() {

        var host = mission(0, 1);
        var visitor = mission(1, 0);
        var data = fleet(host, visitor);
        link(data, visitor, host);
        var pair = RocketLayout.pair(host.rocket, RocketLayout.controllers(host.rocket, false).getFirst());
        load(host.rocket, pair);
        var action = craft(visitor, visitor);
        tick(data, visitor, action, 1);
        assertTrue(RocketProcessingService.remoteBusy(data, visitor.rocket.getRocketId()));
        tick(data, visitor, action, 10);
        assertNull(visitor.action());
        assertTrue(RocketLayout.state(host.rocket, pair.controller().segment(), pair.left()).is(Blocks.GOLD_BLOCK));
    }

    @Test
    void fluidCapacityIsMaximumYieldPerTankNotSummedPerFuelType() {

        for (var fluid : List.of(Fluids.WATER, Fluids.LAVA))
            if (!fluid.builtInRegistryHolder().areComponentsBound())
                fluid.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        var tank = new SimpleFluidStorage(1000, () -> {
        });
        var water = FluidResource.of(Fluids.WATER);
        var lava = FluidResource.of(Fluids.LAVA);
        try (var transaction = Transaction.openRoot()) {
            tank.insert(water, 100, transaction);
            transaction.commit();
        }
        var scan = new RocketStorageScan(Map.of(water, 1.0, lava, 2.0));
        try (var transaction = Transaction.openRoot()) {
            var budget = scan.probe(tank, null, transaction);
            assertEquals(100, budget.fuel());
            assertEquals(2000, budget.fuelCapacity());
            assertEquals(0, scan.probe(tank, null, transaction).fuelCapacity());
        }
        assertEquals(100, tank.getAmount());
        try (var transaction = Transaction.openRoot()) {
            tank.extract(water, 100, transaction);
            transaction.commit();
        }
        try (var transaction = Transaction.openRoot()) {
            var budget = new RocketStorageScan(Map.of(water, 1.0, lava, 2.0)).probe(tank, null, transaction);
            assertEquals(0, budget.fuel());
            assertEquals(2000, budget.fuelCapacity());
        }
    }

    @Test
    void resourceDockingEventCannotReplayAfterReload() {

        var host = mission(0, 0);
        var visitor = mission(0, 0);
        var data = fleet(host, visitor);
        var docking = new Docking(host.rocket.getRocketId(), RocketDocking.ports(visitor.rocket).getFirst(), RocketDocking.ports(host.rocket).getFirst(), 0,
                host.position.x(), host.position.y());
        var exchange = new Exchange(RocketServiceSettings.Direction.NONE, List.of(), List.of(), Identifier.parse("minecraft:air"), ResourceExchange.DEFAULT,
                new ResourceExchange(RocketServiceSettings.Direction.SEND, 500, 0));
        var action = FlightPlanAction.create(ActionType.DOCK).withSettings(RocketServiceSettings.DEFAULT.withDocking(docking).withExchange(exchange));
        visitor.plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(action))), List.of());
        assertTrue(RocketDocking.connect(data, visitor, docking));
        var loaded = MissionSavedData.CODEC.parse(JsonOps.INSTANCE, MissionSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
        var resumed = loaded.craft.get(visitor.rocket.getRocketId());
        assertFalse(RocketDocking.connect(loaded, resumed, docking));
        assertEquals(999500, resumed.rocket.getDynamicSegments().values().iterator().next().availableRF);
        assertEquals(500, resumed.rocket.getDynamicSegments().values().iterator().next().lastExchange.rf());
    }

    @Test
    void controllerSettingsAndCraftPassSurviveNetworkSnapshots() {

        var state = mission(1, 1);
        var data = fleet(state);
        var pair = RocketLayout.pair(state.rocket, RocketLayout.controllers(state.rocket, false).getFirst());
        load(state.rocket, pair);
        var action = craft(state, state);
        tick(data, state, action, 3);
        NetworkManager.loadDefaultCodecs();
        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(ActiveRocketData.CODEC), ActiveRocketData.class);
        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(MissionState.Position.CODEC), MissionState.Position.class);
        var codec = ReflectiveCodecBuilder.create(MissionState.Telemetry.class);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        try {
            codec.encode(buffer, state.telemetry(3));
            var decoded = codec.decode(buffer);
            assertEquals(SourceScope.BOTH, decoded.plan().root().actions().getFirst().settings().processing().scope());
            var r = decoded.rocket().getDynamicSegments().values().iterator().next();
            assertEquals(2, r.reserved.size());
            assertEquals(action.id(), r.pass.action());
            assertEquals(2, RocketProcessingService.controller(decoded.rocket(), RocketProcessingService.modules(decoded.rocket()).getFirst()).job.elapsed());
        } finally {
            buffer.release();
        }
    }

    @Test
    void timedForecastAccountsForPartialPowerWithoutFinishingReservedBlocks() {

        var state = mission(1, 1);
        var pair = RocketLayout.pair(state.rocket, RocketLayout.controllers(state.rocket, false).getFirst());
        load(state.rocket, pair);
        state.rocket.getDynamicSegments().values().iterator().next().availableRF = 450;
        var action = craft(state, state).withService(new ServiceSettings(0, 6, -1));
        state.plan = state.plan.withBranches(List.of(state.plan.root().withActions(List.of(action))));
        var path = RocketFlightPathCalculator.calculateFrom(state.rocket, new SpaceSimulation().createObjectData(), state.plan, state.position, List.of(recipe("a")));
        var estimate = path.processingEstimates().getFirst();
        assertEquals(6, estimate.ticks());
        assertEquals(400, estimate.rf());
        assertTrue(estimate.products().isEmpty());
        assertEquals("processing_timed_out", estimate.issue());
        assertEquals(450, state.rocket.getDynamicSegments().values().iterator().next().availableRF);
        assertTrue(RocketLayout.state(state.rocket, pair.controller().segment(), pair.left()).is(Blocks.IRON_BLOCK));
    }

}
