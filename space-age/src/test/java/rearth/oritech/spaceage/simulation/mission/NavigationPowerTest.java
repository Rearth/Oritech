package rearth.oritech.spaceage.simulation.mission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.NavigationWaypoint;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.survey.SpaceCommunications;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationPowerTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void solarAndAntennaBudgetsMatchThePreviewAcrossReload() {

        var maps = ((BaseMappedRegistry<Block>) BuiltInRegistries.BLOCK).getDataMaps();
        var previous = maps.get(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS);
        maps.put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS,
                Map.of(Blocks.DAYLIGHT_DETECTOR.builtInRegistryHolder().unwrapKey().orElseThrow(), 100d));
        try {
            for (long stored : new long[]{0, 100_000}) {
                var id = new UUID(950, 1);
                var structure = new StaticRocketSegment(id, Set.of(
                        new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()),
                        new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState()),
                        new StaticRocketSegment.BlockData(BlockPos.ZERO.above(2), Blocks.DAYLIGHT_DETECTOR.defaultBlockState())), Map.of(), 50, 1);
                var resources = new DynamicRocketSegment(0, stored, 0, Set.of());
                resources.rfCapacity = 2_000_000;
                var rocket = new ActiveRocketData(Map.of(id, structure), Map.of(id, resources));
                var system = new SpaceSimulation();
                var position = new MissionState.Position(-3_120_000, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.HIGH,
                        -1, 1, FlightPlanAction.NO_TARGET, SegmentRef.of(structure));
                var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withWaypoint(new NavigationWaypoint(-3_320_000, 0));
                var base = FlightPlan.empty();
                var program = base.withBranches(List.of(base.root().withActions(List.of(action))));
                var path = RocketFlightPathCalculator.calculateFrom(rocket, system.createObjectData(), program, position).paths().getFirst();
                assertEquals(TerminalState.READY, path.terminalState());
                var expected = resources.rf();
                for (int i = 1; i < path.samples().size(); i++) {
                    var sample = path.samples().get(i);
                    var rate = sample.resourceRates().get(SegmentRef.of(structure));
                    if (rate == null) continue;
                    expected = Math.clamp(expected - rate.rf() * (sample.timeSeconds() - path.samples().get(i - 1).timeSeconds()), 0, resources.rfCapacity);
                }
                assertTrue(expected > resources.rf(), "Solar panels must charge during navigation, including powered flight");
                assertTrue(path.samples().size() < 100);
                var mission = new MissionState(new UUID(951, 1), rocket, program, position, new SurveyKnowledge(), BlockPos.ZERO);
                var data = new MissionSavedData();
                for (int tick = 0; tick < 20_000 && mission.action() != null; tick++) {
                    SpaceCommunications.poweredAntennas(mission, true);
                    MissionNavigation.tick(null, data, mission, system, mission.action(), tick);
                    if (tick == 200) mission = MissionState.CODEC.parse(NbtOps.INSTANCE,
                            MissionState.CODEC.encodeStart(NbtOps.INSTANCE, mission).getOrThrow()).getOrThrow();
                }
                assertNull(mission.action(), mission.status);
                assertEquals(expected, mission.rocket.getDynamicSegments().get(id).rf(), .001);
                assertEquals(0, Math.hypot(mission.position.vx(), mission.position.vy()), 1e-6);
            }
        } finally {
            if (previous == null) maps.remove(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS);
            else maps.put(SpaceAgeDataMaps.SPACE_SOLAR_GENERATORS, (Map) previous);
        }
    }
}
