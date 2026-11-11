package rearth.oritech.spaceage.simulation.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import rearth.oritech.spaceage.recipe.RocketPayloadRecipe;
import rearth.oritech.spaceage.recipe.RocketPayloadRecipe.Effect;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.mission.MissionController;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment.BlockData;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RocketPayloadGameTests {

    private RocketPayloadGameTests() {
    }

    public static void weather(GameTestHelper helper) {

        var server = helper.getLevel().getServer();
        var weather = server.getWeatherData();
        var raining = weather.isRaining();
        var thundering = weather.isThundering();
        var rainTime = weather.getRainTime();
        var thunderTime = weather.getThunderTime();
        var clearTime = weather.getClearWeatherTime();
        try {
            var recipes = RocketPayloadRecipe.all(server);
            helper.assertTrue(recipes.size() == 3, Component.literal("All three payload recipes load from datapacks"));
            for (var holder : recipes) {
                var recipe = holder.value();
                helper.assertTrue(recipe.ingredients().size() == 2, Component.literal("Weather payloads require two blocks"));
                var blocks = new HashSet<BlockData>();
                var index = 0;
                for (var ingredient : recipe.ingredients()) {
                    var block = Block.byItem(ingredient.display().getFirst().getItem());
                    for (int count = 0; count < ingredient.count(); count++)
                        blocks.add(new BlockData(new BlockPos(index++, 0, 0), block.defaultBlockState()));
                }
                var id = UUID.randomUUID();
                var structure = new StaticRocketSegment(id, blocks, Map.of(), blocks.size(), 0);
                var rocket = new ActiveRocketData(Map.of(id, structure), Map.of(id, new DynamicRocketSegment(0, 0, 0, Set.of())));
                server.setWeatherParameters(0, 100, false, false);
                RocketDetonation.applyPayload(server, rocket, 5_001);
                helper.assertTrue(!weather.isRaining() && weather.getRainTime() == 100,
                        Component.literal("Payloads outside the lower atmosphere do not change weather"));

                var action = FlightPlanAction.create(ActionType.EXPLODE);
                var plan = FlightPlan.empty();
                plan = plan.withBranches(List.of(plan.root().withActions(List.of(action))));
                var position = new MissionState.Position(SpaceObjects.EARTH_SURFACE_X - 4_000, 0, 0, 0,
                        SpaceObjects.EARTH_ID, OrbitBand.LOW, -1, 1, FlightPlanAction.NO_TARGET, SegmentRef.of(structure));
                var state = new MissionState(UUID.randomUUID(), rocket, plan, position, new SurveyKnowledge(),
                        helper.absolutePos(BlockPos.ZERO));
                var missions = new MissionSavedData();
                MissionController.step(server, missions, state, new SpaceSimulation(), 0);
                helper.assertTrue(state.ended && state.status.equals(MissionState.STATUS_EXPLODED),
                        Component.literal("Explode destroys a stationary craft without TNT"));
                var effect = recipe.effect();
                helper.assertTrue(weather.isRaining() == (effect != Effect.CLEAR)
                                && weather.isThundering() == (effect == Effect.THUNDER),
                        Component.literal("Payload sets command-equivalent weather: " + effect));
                helper.assertTrue((effect == Effect.CLEAR ? weather.getClearWeatherTime()
                                : weather.getRainTime()) == recipe.durationTicks(),
                        Component.literal("Weather uses the recipe duration"));
                server.setWeatherParameters(0, 123, false, false);
                RocketDetonation.explode(server, missions, state, new SpaceSimulation());
                helper.assertTrue(weather.getRainTime() == 123 && !weather.isRaining(),
                        Component.literal("A destroyed craft cannot activate the payload twice"));
            }
            helper.succeed();
        } finally {
            weather.setRaining(raining);
            weather.setThundering(thundering);
            weather.setRainTime(rainTime);
            weather.setThunderTime(thunderTime);
            weather.setClearWeatherTime(clearTime);
        }
    }
}
