package rearth.oritech.spaceage.simulation.mission;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.FlightPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionJourneyTest {

    private static PathSample sample(double time, double x, double y, double vx, double vy, UUID action) {

        return new PathSample(time, x, y, Math.hypot(vx, vy), vx, vy, PathPhase.COAST, FlightPlanAction.NO_TARGET,
                action, 1, Set.of(), Set.of(), FlightPlanAction.NO_TARGET);
    }

    private static MissionState state(FlightPlan plan) {

        return new MissionState(UUID.randomUUID(), new ActiveRocketData(Map.of(), Map.of()), plan,
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO);
    }

    @Test
    void journeyPreservesActionBoundariesAndSamplesActualCurves() {

        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        var samples = List.of(sample(0, 0, 0, 100, 0, a), sample(2, 100, 100, 0, 100, a), sample(4, 200, 200, 100, 0, b));
        var path = new CraftPath(UUID.randomUUID(), Set.of(), samples, List.of(), 4, 0, TerminalState.READY);
        var legs = MissionJourney.legs(path);
        assertEquals(List.of(a, b), legs.stream().map(MissionJourney.Leg::action).toList());
        assertEquals(new MissionJourney.Point(100, 100), legs.getFirst().points().getLast());
        assertEquals(new MissionJourney.Point(100, 100), legs.getLast().points().getFirst());
        assertEquals(new MissionJourney.Point(75, 25), MissionJourney.sample(samples, 0, 1, 8).getLast());
    }

    @Test
    void replanningRecordsOnlyTravelledMotionAndSurvivesReload() {

        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO);
        var base = FlightPlan.empty();
        var state = state(base.withBranches(List.of(base.root().withActions(List.of(action)))));
        var samples = List.of(sample(0, 0, 0, 100, 0, action.id()), sample(2, 100, 100, 0, 100, action.id()));
        state.transfer = new ActiveTransfer(state.telemetry(0), List.of(), List.of());
        state.transfer.path = new FlightPath(List.of(new CraftPath(state.plan.root().id(), Set.of(), samples, List.of(), 2, 0, TerminalState.READY)), List.of(), 0);
        state.transfer.ticks = 20;
        var position = state.position;
        state.resetAction();
        assertEquals(position, state.position, "Display history cannot change live position");
        assertEquals(1, state.journeyHistory.size());
        assertEquals(new MissionJourney.Point(75, 25), state.journeyHistory.getFirst().points().getLast());
        var restored = MissionState.CODEC.parse(NbtOps.INSTANCE, MissionState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow()).getOrThrow();
        assertEquals(state.journeyHistory, restored.journeyHistory);
        restored.resetAction();
        assertEquals(1, restored.journeyHistory.size(), "Repeated resets must not duplicate an archived transfer");
    }

}
