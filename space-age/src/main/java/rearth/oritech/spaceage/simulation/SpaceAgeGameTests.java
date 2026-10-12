package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import rearth.oritech.spaceage.block.VacuumCrafterBlockEntity;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;
import rearth.oritech.spaceage.recipe.BlockIngredient;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;

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
    }

    public static void register(RegisterGameTestsEvent event) {

        var environment = event.registerEnvironment(OritechSpaceAge.id("hardware"));
        var info = new TestData<>(environment, Identifier.parse("minecraft:empty"), 20, 1, true);
        event.registerTest(RECOVERY.identifier(), new FunctionGameTestInstance(RECOVERY, info));
        event.registerTest(CONTENT.identifier(), new FunctionGameTestInstance(CONTENT, info));
        event.registerTest(OritechSpaceAge.id("assembly"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, OritechSpaceAge.id("assembly")), info));
        event.registerTest(OritechSpaceAge.id("visual_reload"), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,
                OritechSpaceAge.id("visual_reload")), info));
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
        json.getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("flight").remove("parachute_descent");
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
