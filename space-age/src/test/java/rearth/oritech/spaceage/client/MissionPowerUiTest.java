package rearth.oritech.spaceage.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.BeamReception;
import rearth.oritech.spaceage.simulation.SpacePowerService.Report;
import rearth.oritech.spaceage.simulation.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.MissionState;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.StaticRocketSegment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
