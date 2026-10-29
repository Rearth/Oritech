package rearth.oritech.spaceage.block.mission_control;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionControlMenuTest {

    private final ActiveRocketData rocket = new ActiveRocketData(Map.of(), Map.of());
    private final MissionControlMenu menu = new MissionControlMenu(1, null, BlockPos.ZERO);
    private final FlightPlanAction first = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
    private final FlightPlanAction second = FlightPlanAction.create(ActionType.DISCARD_CRAFT);
    private final FlightPlan plan = FlightPlan.empty();

    private FlightPlan program(FlightPlanAction... actions) {

        return plan.withBranches(List.of(plan.root().withActions(List.of(actions))));
    }

    private void receive(FlightPlan program, boolean connected, FlightPlanAction... completed) {

        menu.updateMission(new MissionState.Telemetry(rocket, program, MissionState.Position.surface(), 0,
                MissionState.STATUS_READY, List.of(completed), 0), connected);
    }

    @Test
    void uneditedDraftFollowsTelemetryWithoutCreatingUnsentChanges() {

        receive(program(first, second), true);
        receive(program(second).withName("Updated by server"), true, first);
        assertEquals(menu.acceptedPlan, menu.getDraftFlightPlan());
        assertFalse(menu.hasMissionChanges(menu.getDraftFlightPlan()));
        assertFalse(menu.isDraftFlightPlanDirty());
    }

    @Test
    void disconnectedEditsBelongToTheMenuAndSurviveMissionProgress() {

        receive(program(first, second), true);
        menu.setDraftFlightPlan(program(first, second).withName("My draft"));
        receive(program(second), false, first);
        assertEquals(menu.acceptedPlan, menu.displayedPlan());
        assertEquals("My draft", menu.getDraftFlightPlan().name());
        assertEquals(List.of(second), menu.getDraftFlightPlan().root().actions());
        receive(program(second), true, first);
        assertEquals(menu.getDraftFlightPlan(), menu.displayedPlan());
        assertTrue(menu.hasMissionChanges(menu.displayedPlan()));
    }

    @Test
    void popupCommitCannotRestoreAnAlreadyCompletedCard() {

        receive(program(first, second), true);
        var editing = program(first, second).withName("Popup edit");
        receive(program(second), true, first);
        menu.setDraftFlightPlan(editing);
        assertEquals(List.of(second), menu.getDraftFlightPlan().root().actions());
        assertEquals("Popup edit", menu.getDraftFlightPlan().name());
    }
}
