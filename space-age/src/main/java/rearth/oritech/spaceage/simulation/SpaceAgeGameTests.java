package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
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
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.joml.Vector2i;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.BeamApertureBlockEntity;
import rearth.oritech.spaceage.block.BeamApertureMenu.Snapshot;
import rearth.oritech.spaceage.block.VacuumCrafterBlockEntity;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;
import rearth.oritech.spaceage.recipe.BlockIngredient;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

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

    private static void spacePower(GameTestHelper test) {

        var owner = UUID.randomUUID();
        var distantSource = beamStation(owner, -2_000_000, 100_000);
        var distantReceiver = beamStation(owner, -3_000_000, 0);
        beamProgram(distantSource, distantReceiver);
        var distantData = new MissionSavedData();
        distantData.craft.put(distantSource.rocket.getRocketId(), distantSource);
        distantData.craft.put(distantReceiver.rocket.getRocketId(), distantReceiver);
        SpacePowerService.beams(test.getLevel().getServer(), distantData, false);
        var expectedCollection = 40_000 * .9 * 4 / (Math.PI * Math.pow(1_000_000d / (100_000 * 3), 2));
        test.assertTrue(Math.abs(distantReceiver.powerReport.incoming() - expectedCollection) < 1e-6,
                "Space receivers collect glass area divided by the circular beam footprint");
        test.assertTrue(distantSource.powerReport.overflow() > 0, "Missed space power is accounted as harmless overflow");
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
        SpacePowerService.beams(test.getLevel().getServer(), reserveData, false);
        test.assertTrue(reserveSource.powerReport.sent() == 40_000, "Automatic beam sends the full aperture capacity");
        SpacePowerService.beams(test.getLevel().getServer(), reserveData, false);
        test.assertTrue(reserveSource.powerReport.sent() == 20_000, "Automatic beam sends all remaining RF above reserve");
        SpacePowerService.beams(test.getLevel().getServer(), reserveData, false);
        test.assertTrue(reserveSource.powerReport.sent() == 0
                && reserveSource.rocket.getDynamicSegments().values().iterator().next().rf() == 40_000, "Beam preserves the configured reserve");
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
        SpacePowerService.beams(test.getLevel().getServer(), data, false);
        test.assertTrue(sink.rocket.getDynamicSegments().values().iterator().next().rf() == 0, "Relay cannot forward power received in this tick");
        test.assertTrue(relay.rocket.getDynamicSegments().values().iterator().next().rf() > 0, "Relay receives while sending");
        SpacePowerService.beams(test.getLevel().getServer(), data, false);
        test.assertTrue(sink.rocket.getDynamicSegments().values().iterator().next().rf() > 0, "Buffered power reaches next hop on next tick");
        var stored = data.craft.values().stream().flatMap(m -> m.rocket.getDynamicSegments().values().stream()).mapToDouble(DynamicRocketSegment::rf).sum();
        test.assertTrue(stored < 100_000, "Conversion and distance losses never create energy");
        beamProgram(sink, source);
        var saved = MissionSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        data = MissionSavedData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
        var initial = stored;
        for (int tick = 0; tick < 100; tick++) SpacePowerService.beams(test.getLevel().getServer(), data, false);
        var afterCycle = data.craft.values().stream().flatMap(m -> m.rocket.getDynamicSegments().values().stream()).mapToDouble(DynamicRocketSegment::rf).sum();
        test.assertTrue(afterCycle >= 0 && afterCycle < initial, "Reloaded cyclic links lose energy and cannot duplicate it");
        source = data.craft.get(source.rocket.getRocketId());
        relay = data.craft.get(relay.rocket.getRocketId());
        var receiverResources = relay.rocket.getDynamicSegments().values().iterator().next();
        receiverResources.receiveRF(receiverResources.rfCapacity);
        beamProgram(source, relay);
        relay.plan = FlightPlan.empty();
        source.rocket.getDynamicSegments().values().iterator().next().receiveRF(10_000);
        SpacePowerService.beams(test.getLevel().getServer(), data, false);
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
        var accepted = heat.deliver(level, origin.getX(), origin.getZ(), 4, 100_000);
        test.assertTrue(accepted > 0 && accepted < 100_000, "Receiver intercepts its share of the footprint");
        test.assertTrue(accepted == (long) (100_000 * 4 / (Math.PI * 16)),
                "Ground receivers use the same collecting fraction, with physical glass columns inside the footprint");
        test.assertTrue(heat.temperature(origin) > 0, "Unaccepted surface energy heats the chunk");
        var beforeHeat = heat.temperature(origin);
        var heatSaved = SurfaceBeamData.CODEC.encodeStart(JsonOps.INSTANCE, heat).getOrThrow();
        var restoredHeat = SurfaceBeamData.CODEC.parse(JsonOps.INSTANCE, heatSaved).getOrThrow();
        test.assertTrue(Math.abs(beforeHeat - restoredHeat.temperature(origin)) < 1e-8, "Surface heat persists independently of receiver blocks");
        receiver.energy.energy = receiver.energy.capacity;
        test.assertTrue(heat.deliver(level, origin.getX(), origin.getZ(), 4, 100_000) == 0, "Full receivers cause fixed-output overflow");
        test.assertTrue(heat.temperature(origin) > beforeHeat, "Overlapping fixed-output deliveries add heat");
        level.setBlockAndUpdate(origin.above(), BlockContent.PORTABLE_ENERGY_STORAGE.get().defaultBlockState());
        var groundStorage = level.getCapability(Capabilities.Energy.BLOCK, origin.above(), null);
        receiver.tick();
        test.assertTrue(groundStorage != null && groundStorage.getAmountAsLong() > 0, "Receiver exports to adjacent RF infrastructure");
        for (int tick = 0; tick < 20; tick++) receiver.tick();
        var dashboard = receiver.snapshot();
        test.assertTrue(dashboard.blocks().size() == 9 && dashboard.width() == 3 && dashboard.area() == 4,
                "Receiver dashboard includes the controller, plating and glass");
        test.assertTrue(dashboard.received() > 0 && dashboard.exported() > 0 && dashboard.heat() == heat.temperature(origin),
                "Dashboard reports actual accepted RF, output and ground heat");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            Snapshot.STREAM_CODEC.encode(buffer, dashboard);
            test.assertTrue(dashboard.equals(Snapshot.STREAM_CODEC.decode(buffer)), "Receiver dashboard survives network encoding");
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
            test.assertTrue(upgraded.powerHardware().area() == 4, "Glass upgrades change capacity, not collecting area");
            test.assertTrue(receiver.array().focus() == 3 && upgraded.powerHardware().focus() == 3,
                    "Glass upgrades preserve the focus advantage of larger arrays");
        }
        test.assertTrue(!SurfaceBeamData.loaded(level, 30_000_000, 30_000_000, 4), "Unloaded footprints suspend delivery");
        test.succeed();
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

    private static void assembly(GameTestHelper test) {

        var level = test.getLevel();
        var assemblerPos = test.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(assemblerPos, SpaceAgeBlocks.ROCKET_ASSEMBLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 3);
        level.setBlock(assemblerPos.south(), SpaceAgeBlocks.ROCKET_PAD.get().defaultBlockState(), 3);
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
        test.assertTrue(preview.getDynamicSegments().values().stream().mapToLong(r -> r.availableRF).sum() == stored, "Capability faces do not multiply battery RF");
        test.assertTrue(energy.getAmountAsLong() == stored, "Assembly preview rolls resource probes back");
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
