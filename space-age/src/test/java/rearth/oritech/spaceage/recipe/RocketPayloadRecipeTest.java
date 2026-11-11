package rearth.oritech.spaceage.recipe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.recipe.RocketPayloadRecipe.Effect;
import rearth.oritech.spaceage.recipe.RocketPayloadRecipe.Input;
import rearth.oritech.spaceage.recipe.RocketPayloadRecipe.Requirement;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment.BlockData;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment.CouplingData;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketPayloadRecipeTest {

    private static RocketPayloadRecipe rain(int count) {

        return new RocketPayloadRecipe(List.of(new Requirement(BlockIngredient.of(Blocks.CALCITE), 1),
                new Requirement(BlockIngredient.of(Blocks.PACKED_ICE), count)), Effect.RAIN, 12_000, 5_000, 0);
    }

    private static RecipeHolder<RocketPayloadRecipe> holder(String name, RocketPayloadRecipe recipe) {

        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, Identifier.parse("oritech_space_age:" + name)), recipe);
    }

    @Test
    void payloadsRequireAllPhysicalBlocksAndRespectCounts() {

        var recipe = rain(2);
        assertFalse(recipe.matches(new Input(List.of(Blocks.PACKED_ICE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState())), null));
        assertFalse(recipe.matches(new Input(List.of(Blocks.CALCITE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState())), null));
        assertTrue(recipe.matches(new Input(List.of(Blocks.CALCITE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(),
                Blocks.PACKED_ICE.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState())), null));
        var repeated = new RocketPayloadRecipe(List.of(new Requirement(BlockIngredient.of(Blocks.CALCITE), 1),
                new Requirement(BlockIngredient.of(Blocks.CALCITE), 1)), Effect.RAIN, 12_000, 5_000, 0);
        assertFalse(repeated.matches(new Input(List.of(Blocks.CALCITE.defaultBlockState())), null));
    }

    @Test
    void oneRecipeWinsDeterministicallyAndOnlyInsideItsAltitude() {

        var input = new Input(List.of(Blocks.CALCITE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState()));
        var rain = holder("rain", rain(1));
        var thunder = holder("thunder", new RocketPayloadRecipe(rain.value().ingredients(), Effect.THUNDER, 6_000, 5_000, 10));
        for (var recipes : List.of(List.of(rain, thunder), List.of(thunder, rain))) {
            assertEquals(thunder, RocketPayloadRecipe.select(recipes, input, 5_000).orElseThrow());
            assertEquals(thunder, RocketPayloadRecipe.select(recipes, input, 0).orElseThrow());
            assertTrue(RocketPayloadRecipe.select(recipes, input, 5_001).isEmpty());
        }
        var otherRain = holder("another_rain", rain(1));
        assertEquals(otherRain, RocketPayloadRecipe.select(List.of(rain, otherRain), input, 0).orElseThrow());
    }

    @Test
    void matchingUsesCurrentLayoutInsteadOfLaunchBlocks() {

        var id = UUID.randomUUID();
        var structure = new StaticRocketSegment(id, Set.of(new BlockData(BlockPos.ZERO, Blocks.CALCITE.defaultBlockState()),
                new BlockData(BlockPos.ZERO.above(), Blocks.PACKED_ICE.defaultBlockState())), Map.of(), 2, 0);
        var resources = new DynamicRocketSegment(0, 0, 0, Set.of());
        var rocket = new ActiveRocketData(Map.of(id, structure), Map.of(id, resources));
        assertTrue(rain(1).matches(Input.of(rocket), null));
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(FlightPlanAction.create(ActionType.EXPLODE)))));
        var objects = new SpaceSimulation().createObjectData();
        var forecast = RocketFlightPathCalculator.calculate(rocket, objects, plan);
        assertTrue(forecast.explosions().getFirst().payload(List.of(holder("rain", rain(1)))).isPresent());
        resources.layout.put(BlockPos.ZERO.above(), Blocks.AIR.defaultBlockState());
        assertFalse(rain(1).matches(Input.of(rocket), null));
        forecast = RocketFlightPathCalculator.calculate(rocket, objects, plan);
        assertTrue(forecast.explosions().getFirst().payload(List.of(holder("rain", rain(1)))).isEmpty());
    }

    @Test
    void explosionPreviewDoesNotCountBlocksFromADetachedStage() {

        var firstId = UUID.randomUUID();
        var secondId = UUID.randomUUID();
        var first = new StaticRocketSegment(firstId, Set.of(new BlockData(BlockPos.ZERO, Blocks.PACKED_ICE.defaultBlockState())),
                Map.of(secondId, Set.of(new CouplingData(BlockPos.ZERO.east(), BlockPos.ZERO.east(2)))), 1, 0);
        var second = new StaticRocketSegment(secondId, Set.of(new BlockData(BlockPos.ZERO.east(3), Blocks.CALCITE.defaultBlockState())),
                Map.of(firstId, Set.of(new CouplingData(BlockPos.ZERO.east(2), BlockPos.ZERO.east()))), 1, 0);
        var rocket = new ActiveRocketData(Map.of(firstId, first, secondId, second), Map.of(
                firstId, new DynamicRocketSegment(0, 0, 0, Set.of(secondId)),
                secondId, new DynamicRocketSegment(0, 0, 0, Set.of(firstId))));
        var explode = FlightPlanAction.create(ActionType.EXPLODE);
        var plan = FlightPlan.empty();
        var objects = new SpaceSimulation().createObjectData();
        var together = RocketFlightPathCalculator.calculate(rocket, objects,
                plan.withBranches(List.of(plan.root().withActions(List.of(explode)))));
        assertTrue(together.explosions().getFirst().payload(List.of(holder("rain", rain(1)))).isPresent());
        var decouple = FlightPlanAction.create(ActionType.DECOUPLE).withSegments(List.of(SegmentRef.of(first), SegmentRef.of(second)));
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(decouple, explode)),
                new FlightPlanBranch(UUID.randomUUID(), decouple.id(), List.of(FlightPlanAction.create(ActionType.EXPLODE)))));
        var separated = RocketFlightPathCalculator.calculate(rocket, objects, plan);
        assertEquals(2, separated.explosions().size());
        assertTrue(separated.explosions().stream().allMatch(explosion -> explosion.payload(List.of(holder("rain", rain(1)))).isEmpty()));
    }

    @Test
    void codecPreservesDatapackSettingsAndRejectsInvalidCounts() {

        var recipe = rain(2);
        assertEquals(recipe, RocketPayloadRecipe.CODEC.codec().parse(JsonOps.INSTANCE,
                RocketPayloadRecipe.CODEC.codec().encodeStart(JsonOps.INSTANCE, recipe).getOrThrow()).getOrThrow());
        var invalid = JsonParser.parseString("""
                {"ingredients":[{"block":"minecraft:tnt","count":0}],"effect":"rain","duration":12000}
                """);
        assertTrue(RocketPayloadRecipe.CODEC.codec().parse(JsonOps.INSTANCE, invalid).error().isPresent());
    }
}
