package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.joml.Vector2i;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.BeamApertureBlockEntity;
import rearth.oritech.spaceage.block.BeamApertureMenu.Snapshot;
import rearth.oritech.spaceage.block.VacuumCrafterBlockEntity;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;
import rearth.oritech.spaceage.network.GroundBeamNetworking.Beams;
import rearth.oritech.spaceage.recipe.BlockIngredient;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpacePowerService.SurfaceBeam;
import rearth.oritech.spaceage.simulation.SpaceSimulation.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Real block-entity recovery and datapack checks, registered only by the development GameTest event.
 */
public final class SpaceAgeGameTests {

    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, OritechSpaceAge.MOD_ID);
    private static final ResourceKey<Consumer<GameTestHelper>> RECOVERY = ResourceKey.create(Registries.TEST_FUNCTION, OritechSpaceAge.id("recovery"));
    private static final ResourceKey<Consumer<GameTestHelper>> CONTENT = ResourceKey.create(Registries.TEST_FUNCTION, OritechSpaceAge.id("content"));

    static {
        FUNCTIONS.register("recovery", () -> SpaceAgeGameTests::recovery);
        FUNCTIONS.register("content", () -> SpaceAgeGameTests::content);
        FUNCTIONS.register("assembly", () -> SpaceAgeGameTests::assembly);
        FUNCTIONS.register("assembly_identity", () -> SpaceAgeGameTests::assemblyIdentity);
        FUNCTIONS.register("visual_reload", () -> SpaceAgeGameTests::visualReload);
        FUNCTIONS.register("gallery_light", () -> SpaceAgeGameTests::galleryLight);
        FUNCTIONS.register("space_power", () -> SpaceAgeGameTests::spacePower);
    }

    public static void register(RegisterGameTestsEvent event) {

        var environment = event.registerEnvironment(OritechSpaceAge.id("hardware"));
        var info = new TestData<>(environment, Identifier.parse("minecraft:empty"), 20, 1, true);
        event.registerTest(RECOVERY.identifier(), new FunctionGameTestInstance(RECOVERY, info));
        event.registerTest(CONTENT.identifier(), new FunctionGameTestInstance(CONTENT, info));
        event.registerTest(OritechSpaceAge.id("assembly"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, OritechSpaceAge.id("assembly")), info));
        event.registerTest(OritechSpaceAge.id("assembly_identity"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                OritechSpaceAge.id("assembly_identity")), info));
        event.registerTest(OritechSpaceAge.id("visual_reload"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                OritechSpaceAge.id("visual_reload")), info));
        event.registerTest(OritechSpaceAge.id("space_power"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                OritechSpaceAge.id("space_power")), info));
        event.registerTest(OritechSpaceAge.id("gallery_light"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                OritechSpaceAge.id("gallery_light")), info));
    }

    private static MissionState beamStation(UUID owner, double x, long rf) {

        var id = UUID.randomUUID();
        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.BEAM_APERTURE.get().defaultBlockState()));
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) blocks.add(new StaticRocketSegment.BlockData(new BlockPos(dx, 0, dz),
                        (dx == 0 || dz == 0 ? BlockContent.IRON_PLATING.get() : Blocks.GLASS).defaultBlockState()));
        var panel = new BlockPos(10, 0, 0);
        blocks.add(new StaticRocketSegment.BlockData(panel, BlockContent.BIG_SOLAR_PANEL.get().defaultBlockState()));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                blocks.add(new StaticRocketSegment.BlockData(panel.offset(dx, 1, dz), BlockContent.MACHINE_CORE_1.get().defaultBlockState()));
                if (dx != 0) blocks.add(new StaticRocketSegment.BlockData(panel.offset(dx, 0, dz), BlockContent.MACHINE_CORE_1.get().defaultBlockState()));
            }
        }
        var structure = new StaticRocketSegment(id, blocks, Map.of(), 100, 0, rf, 0);
        var resources = new DynamicRocketSegment(0, rf, 0, Set.of());
        resources.rfCapacity = 2_000_000;
        var rocket = new ActiveRocketData(Map.of(id, structure), Map.of(id, resources));
        return new MissionState(owner, rocket, FlightPlan.empty(), new MissionState.Position(x, 0, 0, 0, SpaceObjects.EARTH_ID,
                SpaceSimulation.OrbitBand.LOW, -1, 1, SpaceSimulation.FlightPlanAction.NO_TARGET, SpaceSimulation.SegmentRef.of(structure)),
                new SurveyKnowledge(), BlockPos.ZERO, 0);
    }

    private static void beamProgram(MissionState source, MissionState target) {

        var action = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.BEAM_POWER).withTarget(target.rocket.getRocketId());
        action = action.withSettings(action.settings().withBeam(new RocketServiceSettings.Beam(0)));
        source.plan = new FlightPlan(List.of(new SpaceSimulation.FlightPlanBranch(UUID.randomUUID(), SpaceSimulation.FlightPlanBranch.NO_PARENT, List.of(action))), List.of());
    }

    private static void powerTick(GameTestHelper test, MissionSavedData data) {

        data.power.update(test.getLevel().getServer(), data, data.extraTicks);
        data.power.advance(++data.extraTicks);
    }

    private static void spacePower(GameTestHelper test) {

        var owner = UUID.randomUUID();
        var distantSource = beamStation(owner, -2_000_000, 100_000);
        var distantReceiver = beamStation(owner, -3_000_000, 0);
        beamProgram(distantSource, distantReceiver);
        var distantData = new MissionSavedData();
        distantData.craft.put(distantSource.rocket.getRocketId(), distantSource);
        distantData.craft.put(distantReceiver.rocket.getRocketId(), distantReceiver);
        powerTick(test, distantData);
        var expectedCollection = 40_000 * 9 / (Math.PI * Math.pow(1_000_000d / (100_000 * 3), 2));
        test.assertTrue(Math.abs(distantReceiver.powerReport.incoming() - expectedCollection) < 1e-6,
                "Space receivers collect full aperture area divided by the circular beam footprint");
        test.assertTrue(distantSource.powerReport.overflow() > 0, "Missed space power is accounted as harmless overflow");
        var smallSolarCraft = beamStation(owner, -3_120_000, 50_000_000);
        var original = smallSolarCraft.rocket.getOriginalSegments().values().iterator().next();
        var solarBlocks = new HashSet<>(original.blocks());
        solarBlocks.add(new StaticRocketSegment.BlockData(new BlockPos(0, 3, 0), SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()));
        var solarStructure = new StaticRocketSegment(original.segmentId(), solarBlocks, Map.of(), 100, 1, 50_000_000, 0);
        smallSolarCraft.rocket = new ActiveRocketData(smallSolarCraft.rocket.getRocketId(), Map.of(original.segmentId(), solarStructure),
                smallSolarCraft.rocket.getDynamicSegments(), null);
        smallSolarCraft.rocket.getDynamicSegments().values().iterator().next().rfCapacity = 50_000_000;
        var solarAction = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceSimulation.SUN_ID).withOrbit(SpaceSimulation.OrbitBand.SOLAR);
        smallSolarCraft.plan = new FlightPlan(List.of(smallSolarCraft.plan.root().withActions(List.of(solarAction))), List.of());
        var solarSystem = new SpaceSimulation();
        var solarFlight = RocketFlightPathCalculator.calculateFrom(smallSolarCraft.rocket, solarSystem.createObjectData(),
                smallSolarCraft.plan, smallSolarCraft.position);
        test.assertTrue(solarFlight.paths().getFirst().terminalState() == RocketFlightPathCalculator.TerminalState.READY,
                "A single low-thrust ion engine with a real complete solar panel can enter the collector ring");
        var solarData = new MissionSavedData();
        solarData.craft.put(smallSolarCraft.rocket.getRocketId(), smallSolarCraft);
        MissionController.step(null, solarData, smallSolarCraft, solarSystem, 0);
        test.assertTrue(smallSolarCraft.transfer != null && smallSolarCraft.transfer.path.paths().getFirst().terminalState() == RocketFlightPathCalculator.TerminalState.READY
                        && smallSolarCraft.status.equals(MissionState.STATUS_IN_FLIGHT),
                "Live guidance uses the same zero-Sun-gravity transfer as the preview");
        var source = beamStation(owner, -3_070_000, 100_000);
        var reserveSource = beamStation(owner, -3_070_000, 100_000);
        var reserveTarget = beamStation(owner, -3_080_000, 0);
        reserveSource.rocket.getDynamicSegments().values().iterator().next().rfCapacity = 200_000;
        beamProgram(reserveSource, reserveTarget);
        var reserveAction = reserveSource.action();
        var reserveBranch = reserveSource.plan.root();
        reserveAction = reserveAction.withSettings(reserveAction.settings().withBeam(new RocketServiceSettings.Beam(20)));
        reserveSource.plan = new FlightPlan(List.of(new SpaceSimulation.FlightPlanBranch(reserveBranch.id(), reserveBranch.parentSeparationAction(),
                List.of(reserveAction))), List.of());
        var reserveData = new MissionSavedData();
        reserveData.craft.put(reserveSource.rocket.getRocketId(), reserveSource);
        reserveData.craft.put(reserveTarget.rocket.getRocketId(), reserveTarget);
        powerTick(test, reserveData);
        test.assertTrue(Math.abs(reserveSource.rocket.getDynamicSegments().values().iterator().next().rf() - (60_000 + SolarPower.output(reserveSource))) < 1e-6, "Automatic beam sends the full aperture capacity");
        powerTick(test, reserveData);
        test.assertTrue(Math.abs(reserveSource.rocket.getDynamicSegments().values().iterator().next().rf() - 40_000) < 1e-6, "Automatic beam sends all remaining RF above reserve");
        powerTick(test, reserveData);
        test.assertTrue(Math.abs(reserveSource.powerReport.sent() - SolarPower.output(reserveSource)) < 1e-6
                && Math.abs(reserveSource.rocket.getDynamicSegments().values().iterator().next().rf() - 40_000) < 1e-6, "Beam preserves the configured reserve");
        var reservePool = reserveSource.rocket.getDynamicSegments().values().iterator().next();
        reservePool.receiveRF(reservePool.rfCapacity);
        reserveAction = reserveSource.action();
        reserveAction = reserveAction.withSettings(reserveAction.settings().withBeam(new RocketServiceSettings.Beam(100)));
        reserveSource.plan = new FlightPlan(List.of(new SpaceSimulation.FlightPlanBranch(reserveBranch.id(), reserveBranch.parentSeparationAction(),
                List.of(reserveAction))), List.of());
        powerTick(test, reserveData);
        test.assertTrue(reserveSource.powerReport.sent() == 0 && reservePool.rf() == reservePool.rfCapacity,
                "A 100 percent reserve disables sending even when solar has surplus at a full battery");
        var relay = beamStation(owner, -3_080_000, 0);
        var sink = beamStation(owner, -3_090_000, 0);
        for (var plating : List.of(BlockContent.IRON_PLATING, BlockContent.CARBON_PLATING, BlockContent.NICKEL_PLATING, BlockContent.COPPER_REINFORCED_PLATING))
            test.assertTrue(plating.get().defaultBlockState().is(BeamAperture.FRAMES), "All full plating variants qualify as aperture skeleton");
        test.assertTrue(!BlockContent.MACHINE_FRAME.get().defaultBlockState().is(BeamAperture.FRAMES), "Machine frames are not aperture plating");
        test.assertTrue(source.rocket.powerHardware().solar() > 0, "Oritech panel and all required cores count via the solar data map");
        var solarCraft = beamStation(owner, 0, 0).rocket;
        var solarLayout = solarCraft.getDynamicSegments().values().iterator().next().layout;
        var lowerCore = new BlockPos(11, 0, 0);
        solarLayout.put(lowerCore, Blocks.AIR.defaultBlockState());
        test.assertTrue(solarCraft.powerHardware().solar() == 0, "Missing a lower core in the real rotated footprint rejects the panel");
        solarLayout.put(lowerCore, BlockContent.MACHINE_CORE_1.get().defaultBlockState());
        test.assertTrue(solarCraft.powerHardware().solar() == 128, "Restoring the real lower core makes the complete panel valid again");
        test.assertTrue(source.rocket.powerHardware().throughput() == 40_000, "Tagged cross and glass data map create an aperture");
        beamProgram(source, relay);
        beamProgram(relay, sink);
        var data = new MissionSavedData();
        for (var station : List.of(source, relay, sink)) data.craft.put(station.rocket.getRocketId(), station);
        powerTick(test, data);
        test.assertTrue(Math.abs(sink.rocket.getDynamicSegments().values().iterator().next().rf() - SolarPower.output(sink)) < 1e-6, "Relay cannot forward power received in this tick");
        test.assertTrue(relay.rocket.getDynamicSegments().values().iterator().next().rf() > 0, "Relay receives while sending");
        powerTick(test, data);
        test.assertTrue(sink.rocket.getDynamicSegments().values().iterator().next().rf() > SolarPower.output(sink) * 2, "Buffered power reaches next hop on next tick");
        var stored = data.craft.values().stream().flatMap(m -> m.rocket.getDynamicSegments().values().stream()).mapToDouble(DynamicRocketSegment::rf).sum();
        var generated = 2 * (SolarPower.output(source) + SolarPower.output(relay) + SolarPower.output(sink));
        test.assertTrue(stored <= 100_000 + generated + 1e-6, "Lossless links conserve stored RF plus actual solar generation");
        beamProgram(sink, source);
        var saved = MissionSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        data = MissionSavedData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        var initial = stored;
        for (int tick = 0; tick < 100; tick++) powerTick(test, data);
        var afterCycle = data.craft.values().stream().flatMap(m -> m.rocket.getDynamicSegments().values().stream()).mapToDouble(DynamicRocketSegment::rf).sum();
        var cycleGeneration = 100 * data.craft.values().stream().mapToDouble(SolarPower::output).sum();
        test.assertTrue(afterCycle >= 0 && afterCycle <= initial + cycleGeneration + 1e-6,
                "Reloaded cyclic links conserve energy including their solar generation");
        source = data.craft.get(source.rocket.getRocketId());
        relay = data.craft.get(relay.rocket.getRocketId());
        var receiverResources = relay.rocket.getDynamicSegments().values().iterator().next();
        receiverResources.receiveRF(receiverResources.rfCapacity);
        beamProgram(source, relay);
        relay.plan = FlightPlan.empty();
        source.rocket.getDynamicSegments().values().iterator().next().receiveRF(10_000);
        powerTick(test, data);
        test.assertTrue(source.powerReport.overflow() > 0 && source.powerReport.accepted() == 0, "Full spacecraft discard incident overflow without refund");
        var origin = test.absolutePos(new BlockPos(3, 3, 3));
        var level = test.getLevel();
        level.setBlockAndUpdate(origin, SpaceAgeBlocks.BEAM_APERTURE.get().defaultBlockState());
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) level.setBlockAndUpdate(origin.offset(dx, 0, dz),
                        (dx == 0 || dz == 0 ? BlockContent.IRON_PLATING.get() : Blocks.GLASS).defaultBlockState());
        var receiver = (BeamApertureBlockEntity) level.getBlockEntity(origin);
        receiver.tick();
        test.assertTrue(receiver.array().valid(), "Physical receiver uses the same array validation");
        test.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, origin, null) != null, "Ground receiver exposes RF capability");
        var heat = SurfaceBeamData.get(level);
        var reception = heat.deliver(level, origin.getX(), origin.getZ(), 4, 100_000);
        var accepted = reception.accepted();
        test.assertTrue(reception.missed() > 0 && reception.capacityLoss() == 0 && reception.storageLoss() < 1,
                "Aperture coverage loss is distinct from saturation; whole-RF rounding is less than one RF");
        heat.tick(level);
        var limited = heat.deliver(level, origin.getX(), origin.getZ(), 4, 1_000_000);
        test.assertTrue(limited.capacityLoss() > 0 && limited.storageLoss() < 1 && limited.accepted() == 40_000,
                "Aperture throughput rejection is reported separately from missed glass and free buffer");
        var overlap = heat.deliver(level, origin.getX(), origin.getZ(), 4, 100_000);
        test.assertTrue(overlap.capacityLoss() == overlap.collected() && overlap.accepted() == 0,
                "Multiple beams share a receiver's world-tick intake capacity");
        heat.tick(level);
        test.assertTrue(accepted > 0 && accepted < 100_000, "Receiver intercepts its share of the footprint");
        test.assertTrue(accepted == (long) (100_000 * 9 / (Math.PI * 16)),
                "Ground receivers use the same collecting fraction, including the physical controller and plating cross");
        test.assertTrue(heat.temperature(origin) > 0, "Unaccepted surface energy heats the chunk");
        var pulse = beamStation(owner, -3_070_000, 1000);
        pulse.rocket.getDynamicSegments().values().iterator().next().layout.put(new BlockPos(10, 0, 0), Blocks.AIR.defaultBlockState());
        var pulseAction = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.BEAM_POWER)
                .withTarget(SpaceObjects.EARTH_ID).withLanding(origin.getX(), origin.getZ(), 0, 0);
        pulseAction = pulseAction.withSettings(pulseAction.settings().withBeam(new RocketServiceSettings.Beam(0)));
        pulse.plan = new FlightPlan(List.of(new SpaceSimulation.FlightPlanBranch(UUID.randomUUID(),
                SpaceSimulation.FlightPlanBranch.NO_PARENT, List.of(pulseAction))), List.of());
        var pulseData = new MissionSavedData();
        pulseData.craft.put(pulse.rocket.getRocketId(), pulse);
        pulseData.power.update(level.getServer(), pulseData, 0);
        pulseData.power.advance(1);
        test.assertTrue(pulse.rocket.getDynamicSegments().values().iterator().next().rf() == 0, "Analytic ground pulse drains its finite battery");
        var beforePulse = receiver.energy.energy;
        pulseData.power.surface(level.getServer());
        test.assertTrue(receiver.energy.energy - beforePulse == (long) (1000 * 9 / (Math.PI * 16)),
                "A pulse ending between world ticks is delivered exactly once, even when current sending is zero");
        pulseData.power.surface(level.getServer());
        test.assertTrue(receiver.energy.energy - beforePulse == (long) (1000 * 9 / (Math.PI * 16)), "Physical delivery cannot replay a settled pulse");
        var beforeHeat = heat.temperature(origin);
        var heatSaved = SurfaceBeamData.CODEC.encodeStart(JsonOps.INSTANCE, heat).getOrThrow();
        var restoredHeat = SurfaceBeamData.CODEC.parse(JsonOps.INSTANCE, heatSaved).getOrThrow();
        test.assertTrue(Math.abs(beforeHeat - restoredHeat.temperature(origin)) < 1e-8, "Surface heat persists independently of receiver blocks");
        receiver.energy.energy = receiver.energy.capacity;
        heat.tick(level);
        var fullReception = heat.deliver(level, origin.getX(), origin.getZ(), 4, 100_000);
        test.assertTrue(fullReception.accepted() == 0 && fullReception.storageLoss() > 0 && fullReception.capacityLoss() == 0,
                "Full buffers report storage rejection, not aperture saturation");
        test.assertTrue(heat.temperature(origin) > beforeHeat, "Overlapping fixed-output deliveries add heat");
        level.setBlockAndUpdate(origin.above(), BlockContent.PORTABLE_ENERGY_STORAGE.get().defaultBlockState());
        var groundStorage = level.getCapability(Capabilities.Energy.BLOCK, origin.above(), null);
        receiver.tick();
        test.assertTrue(groundStorage != null && groundStorage.getAmountAsLong() > 0, "Receiver exports to adjacent RF infrastructure");
        for (int tick = 0; tick < 20; tick++) receiver.tick();
        var dashboard = receiver.snapshot();
        test.assertTrue(dashboard.blocks().size() == 9 && dashboard.width() == 3 && dashboard.area() == 9,
                "Receiver dashboard includes the controller, plating and glass");
        test.assertTrue(dashboard.received() > 0 && dashboard.exported() > 0 && dashboard.heat() == heat.temperature(origin),
                "Dashboard reports actual accepted RF, output and ground heat");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            Snapshot.STREAM_CODEC.encode(buffer, dashboard);
            test.assertTrue(dashboard.equals(Snapshot.STREAM_CODEC.decode(buffer)), "Receiver dashboard survives network encoding");
            var beams = new Beams(List.of(new SurfaceBeam(origin, 4), new SurfaceBeam(origin.offset(10, 0, 10), 37.25)));
            var beamCodec = NetworkManager.<Beams>getAutoCodec(Beams.class);
            beamCodec.encode(buffer, beams);
            test.assertTrue(beams.equals(beamCodec.decode(buffer)), "Beam snapshots preserve target positions and full footprint radii");
        } finally {
            buffer.release();
        }
        level.setBlockAndUpdate(origin.offset(1, 0, 1), Blocks.AIR.defaultBlockState());
        var incomplete = receiver.snapshot();
        test.assertTrue(incomplete.defect().isPresent() && incomplete.blocks().size() == 8,
                "Incomplete receiver retains its construction preview and identifies the missing cell");
        var upgraded = beamStation(owner, 0, 0).rocket;
        var layout = upgraded.getDynamicSegments().values().iterator().next().layout;
        for (var material : Map.of(Blocks.GLASS, 40_000d, BlockContent.INDUSTRIAL_GLASS.get(), 50_000d,
                SpaceAgeBlocks.CRYSTAL_GLASS.get(), 160_000d).entrySet()) {
            for (int dx : List.of(-1, 1)) {
                for (int dz : List.of(-1, 1)) {
                    var cell = new BlockPos(dx, 0, dz);
                    level.setBlockAndUpdate(origin.offset(cell), material.getKey().defaultBlockState());
                    layout.put(cell, material.getKey().defaultBlockState());
                }
            }
            test.assertTrue(receiver.snapshot().quality() * BeamAperture.RF_PER_GLASS == material.getValue(),
                    "Physical receiver uses loaded glass qualities from the data map");
            test.assertTrue(upgraded.powerHardware().throughput() == material.getValue(),
                    "Spacecraft use the same glass upgrades and invalidate cached hardware");
            test.assertTrue(upgraded.powerHardware().area() == 9, "Glass upgrades change capacity, not collecting area");
            test.assertTrue(receiver.array().focus() == 3 && upgraded.powerHardware().focus() == 3,
                    "Glass upgrades preserve the focus advantage of larger arrays");
        }
        // The reported 11x11 receiver contains this beam; its controller and cross must collect too.
        for (int dx = -5; dx <= 5; dx++)
            for (int dz = -5; dz <= 5; dz++)
                if (dx != 0 || dz != 0) level.setBlockAndUpdate(origin.offset(dx, 0, dz),
                        (dx == 0 || dz == 0 ? BlockContent.IRON_PLATING.get() : SpaceAgeBlocks.CRYSTAL_GLASS.get()).defaultBlockState());
        receiver.energy.energy = 0;
        receiver.snapshot();
        heat.tick(level);
        var reported = heat.deliver(level, origin.getX(), origin.getZ(), 4, 975);
        test.assertTrue(reported.accepted() == 975 && reported.coverage() == 1,
                "A radius-four beam inside the 11x11 aperture has 100% coverage and accepts all 975 RF");
        test.assertTrue(reported.capacityLoss() == 0 && reported.storageLoss() == 0 && reported.missed() == 0,
                "A contained beam loses no RF on its controller, plating or partial edge cells");
        surfaceClockRegression(test, owner, receiver, origin);
        test.assertTrue(!SurfaceBeamData.loaded(level, 30_000_000, 30_000_000, 4), "Unloaded footprints suspend delivery");
        test.succeed();
    }

    private static void surfaceClockRegression(GameTestHelper test, UUID owner, BeamApertureBlockEntity receiver, BlockPos origin) {

        var server = test.getLevel().getServer();
        var heat = SurfaceBeamData.get(test.getLevel());
        for (var speed : List.of(1, 5, 10)) {
            var source = beamStation(owner, -3_070_000, 0);
            var action = FlightPlanAction.create(ActionType.BEAM_POWER).withTarget(SpaceObjects.EARTH_ID)
                    .withLanding(origin.getX(), origin.getZ(), 0, 0);
            action = action.withSettings(action.settings().withBeam(new RocketServiceSettings.Beam(0)));
            source.plan = new FlightPlan(List.of(source.plan.root().withActions(List.of(action))), List.of());
            var data = new MissionSavedData();
            data.debugSpeed = speed;
            data.craft.put(source.rocket.getRocketId(), source);
            var start = 100_000d;
            data.power.update(server, data, start);
            receiver.energy.energy = 0;
            heat.tick(test.getLevel());
            data.power.surface(server);
            test.assertTrue(receiver.energy.energy == 0, "A new ground link cannot deliver historical simulation time");
            data.power.advance(start + 1 + speed);
            data.power.surface(server);
            for (int tick = 1; tick <= 25; tick++) {
                receiver.energy.energy = 0;
                heat.tick(test.getLevel());
                data.power.advance(start + 1 + (tick + 1) * speed);
                var sent = source.powerReport.perWorldTick(speed).sent();
                data.power.surface(server);
                var received = receiver.energy.energy;
                var report = source.powerReport.perWorldTick(speed);
                test.assertTrue(received == (long) sent && Math.abs(report.outgoing().accepted() - received) < 1e-6,
                        "At " + speed + "x the sender's world-tick rates match actual lossless receiver insertion");
                test.assertTrue(source.rocket.getDynamicSegments().values().iterator().next().rf() < 1e-5,
                        "Solar forwarding neither borrows battery RF nor duplicates energy");
                data.power.surface(server);
                test.assertTrue(receiver.energy.energy == received, "World delivery cannot replay an accumulated interval");
            }
            // A finite battery pulse must be delivered even when it ends before this world tick's last substep.
            source.rocket.getDynamicSegments().values().iterator().next().layout.put(new BlockPos(10, 0, 0), Blocks.AIR.defaultBlockState());
            source.rocket.getDynamicSegments().values().iterator().next().receiveRF(1000);
            data.power.update(server, data, start + 1 + 26 * speed);
            receiver.energy.energy = 0;
            heat.tick(test.getLevel());
            data.power.advance(start + 1 + 27 * speed);
            data.power.surface(server);
            test.assertTrue(receiver.energy.energy == 1000, "At " + speed + "x a complete finite battery pulse arrives losslessly: " + receiver.energy.energy);
            test.assertTrue(source.rocket.getDynamicSegments().values().iterator().next().rf() < 1e-6,
                    "The finite pulse spends exactly the battery RF delivered to Earth");
            data.power.surface(server);
            test.assertTrue(receiver.energy.energy == 1000, "An ended pulse is delivered once");
        }
    }

    private static void recovery(GameTestHelper test) {

        var level = test.getLevel();
        var origin = test.absolutePos(new BlockPos(1, 2, 1));
        var id = UUID.randomUUID();
        var blocks = Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, Blocks.CHEST.defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(1, 0, 0), SpaceAgeBlocks.VACUUM_CRAFTER.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(2, 0, 0), BlockContent.PORTABLE_ENERGY_STORAGE.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(3, 0, 0), BlockContent.PORTABLE_TANK.get().defaultBlockState()));
        var segment = new StaticRocketSegment(id, blocks, Map.of(), 10, 0, 1_000_000, 10_000);
        var resources = new DynamicRocketSegment(8_000, 500_000, 5, Set.of());
        resources.crafters.put(new BlockPos(1, 0, 0), new RocketControllerState("Factory", Optional.empty()));
        resources.layout.put(new BlockPos(4, 0, 0), Blocks.GOLD_BLOCK.defaultBlockState());
        blocks.forEach(b -> level.setBlock(origin.offset(b.relativePos()), Blocks.AIR.defaultBlockState(), 3));
        test.assertTrue(RocketRecovery.restore(level, new ActiveRocketData(Map.of(id, segment), Map.of(id, resources)), origin), "Recovery succeeds");
        var chest = (ChestBlockEntity) level.getBlockEntity(origin);
        test.assertTrue(chest.isEmpty(), "Ordinary inventories are fresh");
        var crafter = (VacuumCrafterBlockEntity) level.getBlockEntity(origin.offset(1, 0, 0));
        test.assertTrue(crafter.name().equals("Factory"), "Crafter name survives recovery");
        test.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, origin.offset(1, 0, 0), null) == null, "Crafter has no RF storage capability");
        test.assertTrue(level.getBlockState(origin.offset(4, 0, 0)).is(Blocks.GOLD_BLOCK), "Physical products land as blocks");
        var energy = level.getCapability(Capabilities.Energy.BLOCK, origin.offset(2, 0, 0), null);
        test.assertTrue(energy != null && energy.getAmountAsLong() == 0, "Battery resets independently of aggregate launch RF");
        var fluid = level.getCapability(Capabilities.Fluid.BLOCK, origin.offset(3, 0, 0), null);
        test.assertTrue(fluid != null && fluid.getAmountAsLong(0) == 0, "Fuel tank resets independently of aggregate launch fuel");
        test.succeed();
    }

    private static void assemblyIdentity(GameTestHelper test) {

        var level = test.getLevel();
        var assemblerPos = test.absolutePos(new BlockPos(2, 1, 1));
        level.setBlockAndUpdate(assemblerPos, SpaceAgeBlocks.ROCKET_ASSEMBLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        var pad = assemblerPos.south();
        for (int dx = -1; dx <= 2; dx++)
            level.setBlockAndUpdate(pad.offset(dx, 0, 0), SpaceAgeBlocks.ROCKET_PAD.get().defaultBlockState());
        var origin = pad.above();
        level.setBlockAndUpdate(origin, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState());
        level.setBlockAndUpdate(origin.above(), Blocks.IRON_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(origin.above().east(), SpaceAgeBlocks.ROCKET_COUPLING.get().defaultBlockState());
        var corePos = origin.above().east(2);
        level.setBlockAndUpdate(corePos, SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState());
        var assembler = (RocketAssemblerBlockEntity) level.getBlockEntity(assemblerPos);
        var initial = assembler.createPreview();
        test.assertTrue(initial != null && initial.getStaticSegments().size() == 2, "Two coupled segments are scanned");
        var refs = initial.getStaticSegments().values().stream().map(SegmentRef::of).collect(Collectors.toSet());
        test.assertTrue(refs.size() == 2, "The two sides of a coupling have distinct plan identities");
        var core = initial.getStaticSegments().values().stream().filter(segment -> segment.blocks().stream()
                .anyMatch(block -> block.state().is(SpaceAgeBlocks.ION_BOOSTER_ROCKET))).findFirst().orElseThrow();
        var booster = initial.getStaticSegments().values().stream().filter(segment -> !segment.segmentId().equals(core.segmentId())).findFirst().orElseThrow();
        var configs = List.of(new SegmentConfiguration(SegmentRef.of(core), "Core", false, List.of(2)),
                new SegmentConfiguration(SegmentRef.of(booster), "Booster", true, List.of(1)));
        var decouple = FlightPlanAction.create(ActionType.DECOUPLE).withSegments(List.of(SegmentRef.of(core), SegmentRef.of(booster)));
        var plan = FlightPlan.empty().withSegmentConfigurations(configs);
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(decouple))));
        // Both edits used to change a lowest-block anchor.
        level.setBlockAndUpdate(corePos.below(), BlockContent.PORTABLE_ENERGY_STORAGE.get().defaultBlockState());
        level.setBlockAndUpdate(origin.west(), Blocks.IRON_BLOCK.defaultBlockState());
        var edited = assembler.createPreview();
        test.assertTrue(edited != null && edited.getLaunchPosition().equals(initial.getLaunchPosition()), "Ordinary edits keep the scan origin fixed");
        test.assertTrue(edited.getStaticSegments().values().stream().map(SegmentRef::of).collect(Collectors.toSet()).equals(refs),
                "Adding lower/leftmost blocks keeps both segment identities");
        var validation = RocketFlightPlanRules.inspect(plan, edited, List.of(), true, null);
        test.assertTrue(validation.valid(), "The edited craft accepts its unchanged configuration and separation program");
        var normalized = validation.plan();
        test.assertTrue(normalized.segmentConfigurations().containsAll(configs), "Names, booster flags and stages survive rescanning");
        test.assertTrue(normalized.root().actions().stream().anyMatch(action -> action.id().equals(decouple.id())), "Separation references survive rescanning");
        level.setBlockAndUpdate(origin.west().above(), Blocks.IRON_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
        var shifted = assembler.createPreview();
        test.assertTrue(shifted != null && shifted.getLaunchPosition().equals(initial.getLaunchPosition())
                        && shifted.getStaticSegments().values().stream().map(SegmentRef::of).collect(Collectors.toSet()).equals(refs),
                "Changing the first block above the pads keeps the origin and segment identities");
        var saved = ActiveRocketData.CODEC.encodeStart(JsonOps.INSTANCE, edited).getOrThrow();
        var restored = ActiveRocketData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        test.assertTrue(restored.getStaticSegments().values().stream().map(SegmentRef::of).collect(Collectors.toSet()).equals(refs),
                "Plan identities survive saving and network snapshots");
        var coreId = edited.getStaticSegments().values().stream().filter(segment -> SegmentRef.of(segment).equals(SegmentRef.of(core)))
                .findFirst().orElseThrow().segmentId();
        edited.getDynamicSegments().get(coreId).layout.put(corePos.below().subtract(origin).west(), Blocks.GLASS.defaultBlockState());
        test.assertTrue(SegmentRef.of(edited.getStaticSegments().get(coreId)).equals(SegmentRef.of(core)), "In-flight block production preserves identity");
        level.setBlockAndUpdate(origin.above().east(), Blocks.IRON_BLOCK.defaultBlockState());
        var merged = assembler.createPreview();
        test.assertTrue(merged != null && merged.getStaticSegments().size() == 1, "Removing a boundary merges the segments");
        test.assertTrue(merged.getStaticSegments().values().stream().map(SegmentRef::of).noneMatch(refs::contains),
                "A changed coupling boundary gets a new identity");
        test.assertTrue(!RocketFlightPlanRules.inspect(plan, merged, List.of(), true, null).valid(),
                "A stale separation program cannot be accepted after the boundary changes");
        test.succeed();
    }

    private static void assembly(GameTestHelper test) {

        var level = test.getLevel();
        var assemblerPos = test.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(assemblerPos, SpaceAgeBlocks.ROCKET_ASSEMBLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 3);
        level.setBlock(assemblerPos.south(), SpaceAgeBlocks.ROCKET_PAD.get().defaultBlockState(), 3);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = 0; dz <= 2; dz++) level.setBlock(assemblerPos.south().offset(dx, 0, dz), SpaceAgeBlocks.ROCKET_PAD.get().defaultBlockState(), 3);
        var enginePos = assemblerPos.south().above();
        level.setBlock(enginePos, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState(), 3);
        level.setBlock(enginePos.east(), BlockContent.PORTABLE_ENERGY_STORAGE.get().defaultBlockState(), 3);
        level.setBlock(enginePos.west(), SpaceAgeBlocks.ROCKET_COUPLING.get().defaultBlockState(), 3);
        var energy = level.getCapability(Capabilities.Energy.BLOCK, enginePos.east(), null);
        try (var transaction = Transaction.openRoot()) {
            for (int i = 0; i < 4096 && energy.insert(1_000_000, transaction) > 0; i++) {
            }
            transaction.commit();
        }
        var stored = energy.getAmountAsLong();
        var assembler = (RocketAssemblerBlockEntity) level.getBlockEntity(assemblerPos);
        var preview = assembler.createPreview();
        test.assertTrue(preview != null, "An exposed coupling is valid assembled hardware");
        test.assertTrue(RocketDocking.ports(preview).size() == 1, "Unconnected coupling is a docking port");
        // Mounting blocks can wrap around an exposed coupling without making another segment.
        var mounting = List.of(enginePos.north(), enginePos.north().west(), enginePos.north().west(2), enginePos.west(2));
        mounting.forEach(pos -> level.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), 3));
        var wrapped = assembler.createPreview();
        test.assertTrue(wrapped != null && RocketDocking.ports(wrapped).size() == 1,
                "An exposed coupling touching the same segment on both sides remains a docking port");
        mounting.forEach(pos -> level.removeBlock(pos, false));
        test.assertTrue(preview.getDynamicSegments().values().stream().mapToLong(r -> r.availableRF).sum() == stored, "Capability faces do not multiply battery RF");
        test.assertTrue(energy.getAmountAsLong() == stored, "Assembly preview rolls resource probes back");
        var pad = enginePos.south();
        test.assertTrue(preview.getLaunchPadCenter().equals(pad), "Launch metadata uses the connected pad centre, not the first scanned rocket block");
        var persisted = ActiveRocketData.CODEC.parse(JsonOps.INSTANCE, ActiveRocketData.CODEC.encodeStart(JsonOps.INSTANCE, preview).getOrThrow()).getOrThrow();
        test.assertTrue(persisted.getLaunchPadCenter().equals(pad), "Launch pad centre survives saved and network craft snapshots");
        for (var segment : preview.getStaticSegments().values())
            for (var block : segment.blocks()) level.setBlock(preview.getLaunchPosition().offset(block.relativePos()), Blocks.AIR.defaultBlockState(), 3);
        var landing = RocketRecovery.landingPosition(level, preview, pad.getX(), pad.getZ());
        var centre = RocketRecovery.footprintCenter(preview);
        test.assertTrue(landing.getX() + centre.getX() == pad.getX() && landing.getZ() + centre.getZ() == pad.getZ(),
                "Landing places the craft centre at the selected pad coordinates despite its scan-origin offset");
        test.assertTrue(RocketRecovery.restore(level, preview, landing), "A returning craft fits onto its launch pad");
        test.succeed();
    }

    private static void visualReload(GameTestHelper test) {

        var level = test.getLevel();
        var server = level.getServer();
        var storage = server.overworld().getDataStorage();
        var original = storage.computeIfAbsent(RocketSimulationController.ActiveRocketSavedData.TYPE);
        var missions = MissionSavedData.get(server);
        var segmentId = UUID.randomUUID();
        var origin = test.absolutePos(new BlockPos(1, 2, 1));
        var segment = new StaticRocketSegment(segmentId, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(), 1, 1, 1000, 10000);
        var resources = new DynamicRocketSegment(10000, 1000, 1, Set.of());
        var malformed = new ActiveRocketData(Map.of(segmentId, segment), Map.of(segmentId, resources));
        var valid = MissionState.copyRocket(malformed);
        valid = new ActiveRocketData(UUID.randomUUID(), valid.getStaticSegments(), valid.getDynamicSegments(), null);
        var performance = RocketPerformanceCalculator.calculate(malformed);
        var flight = new RocketFlight(true, null, level.dimension(), performance, origin, origin.above(1000), origin,
                new Vector2i(0, 1000), 0, 0, level.getGameTime() + 100, -1, -1, null, -1);
        malformed.setFlight(flight);
        valid.setFlight(flight);
        var json = ActiveRocketData.CODEC.listOf().encodeStart(JsonOps.INSTANCE, List.of(malformed, valid)).getOrThrow();
        json.getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("flight").remove("performance");
        // Match SavedDataStorage: keep the partial result produced by an incompatible visual record.
        var loaded = RocketSimulationController.ActiveRocketSavedData.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(ignored -> {
                }).orElseThrow();
        try {
            storage.set(RocketSimulationController.ActiveRocketSavedData.TYPE, loaded);
            var partial = RocketSimulationController.getActiveRockets(level).get(malformed.getRocketId());
            test.assertTrue(partial != null && partial.getFlight() == null, "Broken saved flight decodes as a partial rocket");
            var mission = new MissionState(UUID.randomUUID(), partial, FlightPlan.empty(), MissionState.Position.surface(),
                    new SurveyKnowledge(), origin, 0);
            missions.craft.put(partial.getRocketId(), mission);
            var player = FakePlayerFactory.getMinecraft(level);
            player.setPos(origin.getX() + 1000, origin.getY(), origin.getZ() + 1000);
            RocketSimulationController.syncActiveRocketsToPlayer(player);
            RocketSimulationController.tick(server);
            var active = RocketSimulationController.getActiveRockets(level);
            test.assertTrue(active.size() == 1 && active.containsKey(valid.getRocketId()), "Tick removes only the unusable visual");
            test.assertTrue(missions.craft.get(partial.getRocketId()) == mission && !mission.ended
                    && partial.getDynamicSegments().get(segmentId).availableFuelBurnTimeTicks == 10000, "Mission identity and resources survive visual cleanup");
            var savedAgain = RocketSimulationController.ActiveRocketSavedData.CODEC.encodeStart(JsonOps.INSTANCE, loaded).getOrThrow();
            test.assertTrue(savedAgain.getAsJsonArray().size() == 1, "Removed visual is not persisted again");
            test.succeed();
        } finally {
            missions.craft.remove(malformed.getRocketId());
            storage.set(RocketSimulationController.ActiveRocketSavedData.TYPE, original);
        }
    }

    private static void galleryLight(GameTestHelper test) {

        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        var light = SpaceAgeBlocks.GALLERY_LIGHT.get();
        var item = new ItemStack(light);
        test.assertTrue(!item.isEmpty(), "Gallery light has a block item");
        for (var facing : Direction.Plane.HORIZONTAL) {
            var support = pos.relative(facing.getOpposite());
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
            var context = new DirectionalPlaceContext(level, pos, facing.getOpposite(), item, facing);
            var state = light.getStateForPlacement(context);
            test.assertTrue(state != null && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing, "Light faces away from its wall");
            test.assertTrue(state.getLightEmission(level, pos) == 15, "Gallery light emits level 15");
            level.setBlock(pos, state, 3);
            level.setBlock(support, Blocks.AIR.defaultBlockState(), 3);
            test.assertTrue(level.getBlockState(pos).isAir(), "Light drops when its wall is removed");
            test.assertTrue(light.getStateForPlacement(context) == null, "Light needs wall support");
        }
        for (var facing : List.of(Direction.UP, Direction.DOWN)) {
            var context = new DirectionalPlaceContext(level, pos, Direction.NORTH, item, facing);
            test.assertTrue(light.getStateForPlacement(context) == null, "Light cannot mount on floors or ceilings");
        }
        test.succeed();
    }

    private static void content(GameTestHelper test) {

        var recipes = test.getLevel().recipeAccess().recipeMap().byType(SpaceAgeRecipes.VACUUM_CRAFTING.get());
        test.assertTrue(recipes.size() == 2, "Both generated vacuum recipes load");
        test.assertTrue(Blocks.TNT.builtInRegistryHolder().getData(SpaceAgeDataMaps.ROCKET_EXPLOSIVES) > 0, "TNT explosive data map loads on server");
        test.assertTrue(VacuumProcessing.match(Blocks.DIAMOND_BLOCK.defaultBlockState(), BlockContent.STEEL.get().defaultBlockState(), recipes).valid(),
                "Generated block recipe matches reversed inputs");
        var tagged = new VacuumRecipe(new BlockIngredient("#minecraft:planks"),
                BlockIngredient.of(Blocks.DIAMOND_BLOCK), Blocks.GOLD_BLOCK.defaultBlockState(), 10, 100);
        test.assertTrue(tagged.matches(Blocks.OAK_PLANKS.defaultBlockState(), Blocks.DIAMOND_BLOCK.defaultBlockState()), "Physical recipes resolve loaded block tags");
        test.succeed();
    }
}
