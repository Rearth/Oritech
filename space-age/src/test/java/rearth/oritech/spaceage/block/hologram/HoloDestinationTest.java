package rearth.oritech.spaceage.block.hologram;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HoloDestinationTest {

    private static MissionState state(FlightPlanAction action) {

        var base = FlightPlan.empty();
        var plan = base.withBranches(List.of(base.root().withActions(List.of(action))));
        return new MissionState(UUID.randomUUID(), new ActiveRocketData(Map.of(), Map.of()), plan,
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO);
    }

    @Test
    void destinationUsesCurrentNavigationRatherThanThePreviousHost() {

        var destination = UUID.randomUUID();
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(destination);
        var state = state(action);
        assertEquals(SpaceObjects.EARTH_ID, state.position.target());
        assertEquals(destination, HoloControllerBlockEntity.destination(state, List.of()));
        var asteroid = UUID.randomUUID();
        state.plan = state.plan.withBranches(List.of(state.plan.root().withActions(List.of(action.withTarget(FlightPlanAction.CLOSEST_ASTEROID)))));
        var sample = new PathSample(10, 0, 0, 0, 0, 0, PathPhase.COAST, asteroid, action.id(), 1, Set.of(), Set.of(), FlightPlanAction.NO_TARGET);
        assertEquals(asteroid, HoloControllerBlockEntity.destination(state, List.of(sample)));
        assertEquals(SpaceObjects.EARTH_ID, HoloControllerBlockEntity.destination(state(FlightPlanAction.create(ActionType.MAINTAIN_POSITION)), List.of()));
    }
}
