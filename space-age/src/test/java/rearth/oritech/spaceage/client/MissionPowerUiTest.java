package rearth.oritech.spaceage.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.*;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.SpacePowerService.Report;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionPowerUiTest {

    @Test
    void livePowerRatesUseWorldTicksWithoutScalingStoredEnergyOrCoverage() {

        var outgoing = BeamReception.limited(12_160, 12_160, 1_000_000, 1_000_000, 4);
        var report = new Report(120, 12_050, 12_160, outgoing, outgoing, "", 10, 0);
        var physical = report.perWorldTick(10);
        assertEquals(121_600, physical.sent());
        assertEquals(121_600, physical.accepted());
        assertEquals(1, physical.outgoing().coverage());
        assertEquals(4, physical.outgoing().radius());
        assertEquals(1_200, physical.solar());
        assertEquals(120_500, physical.incoming());
        assertEquals(100, physical.services());
        assertEquals(0, physical.net());
        assertEquals(report, report.perWorldTick(1));
        assertEquals(12_160, report.sent(), "UI conversion must not mutate simulation rates");
    }

    @Test
    void indefiniteBeamPowerHasNoInventedCompletionPercentage() {

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                Blocks.GLASS.defaultBlockState())), Map.of(), 1, 0, 1000, 0);
        var rocket = new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(0, 1000, 0, Set.of())));
        var action = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.BEAM_POWER);
        var plan = new SpaceSimulation.FlightPlan(List.of(new SpaceSimulation.FlightPlanBranch(UUID.randomUUID(),
                SpaceSimulation.FlightPlanBranch.NO_PARENT, List.of(action))), List.of());
        var telemetry = new MissionState.Telemetry(rocket, plan, MissionState.Position.surface(), 100_000,
                "status.oritech_space_age.beaming", List.of(), 100_000, 0, 0);
        var moment = new RocketFlightPathCalculator.ActionMoment(plan.root().id(), 0, action.id(), 0, 0, 0, false,
                Set.of(), SpaceSimulation.FlightPlanAction.NO_TARGET);
        var path = new RocketFlightPathCalculator.CraftPath(plan.root().id(), Set.of(), List.of(), List.of(moment), 0, 0,
                RocketFlightPathCalculator.TerminalState.READY);
        assertEquals(-1, MissionControlScreen.progressPercent(telemetry, path));
    }

    @Test
    void craftDiagramDistinguishesSmallFlowsWithoutLettingLargeBeamsFillTheScreen() {

        assertTrue(CraftStatusWidget.flowWidth(1) > CraftStatusWidget.flowWidth(0));
        assertTrue(CraftStatusWidget.flowWidth(1000) > CraftStatusWidget.flowWidth(10));
        assertTrue(CraftStatusWidget.flowWidth(1_000_000) < CraftStatusWidget.flowWidth(1000) * 2);
        assertTrue(CraftStatusWidget.flowWidth(Long.MAX_VALUE) <= 32);
    }

    @Test
    void engineDemandUsesLiveFiringSegmentsAndIgnoresCoastingAndDepletedEngines() {

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(), 20, 1, 1000, 0);
        var resources = new DynamicRocketSegment(0, 1000, 0, Set.of());
        var rocket = new ActiveRocketData(Map.of(id, segment), Map.of(id, resources));
        var plan = SpaceSimulation.FlightPlan.empty();
        var telemetry = new MissionState.Telemetry(rocket, plan, MissionState.Position.surface(), 20,
                MissionState.STATUS_IN_FLIGHT, List.of(), 20);
        var ref = SpaceSimulation.SegmentRef.of(segment);
        for (var phase : List.of(PathPhase.ACCELERATE, PathPhase.COAST, PathPhase.BRAKE)) {
            for (var firing : List.of(Set.<SpaceSimulation.SegmentRef>of(), Set.of(ref))) {
                var sample = new PathSample(10, 0, 0, 1, 1, 0, phase, SpaceSimulation.FlightPlanAction.NO_TARGET,
                        SpaceSimulation.FlightPlanAction.NO_TARGET, 1, Set.of(ref), firing, SpaceSimulation.FlightPlanAction.NO_TARGET);
                var path = new RocketFlightPathCalculator.CraftPath(plan.root().id(), Set.of(ref), List.of(sample), List.of(),
                        10, 0, RocketFlightPathCalculator.TerminalState.READY);
                resources.availableRF = 1000;
                assertEquals(phase == PathPhase.COAST || firing.isEmpty() ? 0 : SpaceBalance.ION_RF,
                        CraftStatusWidget.propulsionDemand(telemetry, List.of(path)));
                resources.availableRF = 0;
                assertEquals(0, CraftStatusWidget.propulsionDemand(telemetry, List.of(path)));
            }
        }
    }

    @Test
    void statusCategoriesFollowInstalledHardwareRatherThanCurrentDemand() {

        var id = UUID.randomUUID();
        var radio = BlockPos.ZERO;
        var engine = new BlockPos(1, 0, 0);
        var processor = new BlockPos(2, 0, 0);
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(radio, SpaceAgeBlocks.ANTENNA.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(engine, SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(processor, SpaceAgeBlocks.VACUUM_CRAFTER.get().defaultBlockState())), Map.of(), 20, 1, 0, 0);
        var resources = new DynamicRocketSegment(0, 0, 0, Set.of());
        var rocket = new ActiveRocketData(Map.of(id, segment), Map.of(id, resources));
        assertEquals(List.of("radio", "processing", "propulsion"), CraftStatusWidget.supportedCategories(rocket));
        resources.layout.put(radio, Blocks.AIR.defaultBlockState());
        resources.layout.put(engine, Blocks.AIR.defaultBlockState());
        resources.layout.put(processor, Blocks.AIR.defaultBlockState());
        assertEquals(List.of(), CraftStatusWidget.supportedCategories(rocket));
    }

}
