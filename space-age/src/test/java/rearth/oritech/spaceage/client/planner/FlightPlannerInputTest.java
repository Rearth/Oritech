package rearth.oritech.spaceage.client.planner;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.FlightPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType.DESIRED_UNCERTAINTY;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType.DISTANCE_FROM_TARGET;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType.LOW_FUEL;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType.LOW_RF;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType.TIME_BEFORE_ARRIVAL;

/**
 * Keep the editor's accepted input and limits unchanged when moving it out of the screen.
 */
class FlightPlannerInputTest {

    @Test
    void terminalBranchesDisableAddingActionsWithoutBlockingOtherBranches() {

        var root = FlightPlan.empty().root().withActions(List.of(FlightPlanAction.create(ActionType.NAVIGATE_TO)));
        var child = new FlightPlanBranch(UUID.randomUUID(), UUID.randomUUID(), List.of(FlightPlanAction.create(ActionType.MAINTAIN_POSITION)));
        var flight = new FlightPath(List.of(new CraftPath(root.id(), Set.of(), List.of(), List.of(), 0, 0, TerminalState.DESTROYED)), List.of(), 0);
        assertTrue(FlightPlannerLabels.endsCraft(root, flight));
        assertFalse(FlightPlannerLabels.endsCraft(child, flight));
        assertTrue(FlightPlannerLabels.endsCraft(child.withActions(List.of(FlightPlanAction.create(ActionType.EXPLODE))), flight));
        assertFalse(FlightPlannerLabels.endsCraft(root, new FlightPath(List.of(), List.of(), 0)));
    }

    @Test
    void cruiseSpeedAcceptsUnitsAndMaximumButNotZero() {

        assertEquals(0, FlightPlannerLabels.parseSpeedLimit(" MAXIMUM "));
        assertEquals(0, FlightPlannerLabels.parseSpeedLimit("max"));
        assertEquals(125, FlightPlannerLabels.parseSpeedLimit(" 125 M/S "));
        assertEquals(100_000, FlightPlannerLabels.parseSpeedLimit("100000"));
        for (var invalid : new String[]{"0", "-1", "100001", "1.5", "", "fast", "2147483648"}) {
            assertNull(FlightPlannerLabels.parseSpeedLimit(invalid), invalid);
        }
    }

    @Test
    void arrivalSpeedAllowsStoppingButUsesTheSameUpperLimit() {

        assertEquals(0, FlightPlannerLabels.parseArrivalVelocity(" 0 m/s "));
        assertEquals(100_000, FlightPlannerLabels.parseArrivalVelocity("100000"));
        for (var invalid : new String[]{"max", "-1", "100001", "1.5", "", "2147483648"}) {
            assertNull(FlightPlannerLabels.parseArrivalVelocity(invalid), invalid);
        }
    }

    @Test
    void completionConditionsRejectValuesOutsideTheirOwnLimits() {

        assertEquals(10_000_000, FlightPlannerLabels.parseAddonValue(DISTANCE_FROM_TARGET, "10000000"));
        assertEquals(1_000_000, FlightPlannerLabels.parseAddonValue(TIME_BEFORE_ARRIVAL, "1000000"));
        assertEquals(100_000, FlightPlannerLabels.parseAddonValue(DESIRED_UNCERTAINTY, "100000"));
        assertEquals(100, FlightPlannerLabels.parseAddonValue(LOW_RF, "100"));
        assertEquals(100, FlightPlannerLabels.parseAddonValue(LOW_FUEL, "100"));
        assertNull(FlightPlannerLabels.parseAddonValue(DISTANCE_FROM_TARGET, "10000001"));
        assertNull(FlightPlannerLabels.parseAddonValue(TIME_BEFORE_ARRIVAL, "1000001"));
        assertNull(FlightPlannerLabels.parseAddonValue(DESIRED_UNCERTAINTY, "100001"));
        for (var type : ActionAddonType.values()) {
            assertEquals(1, FlightPlannerLabels.parseAddonValue(type, " 1 "));
            if (type.isAltitude()) assertEquals(0, FlightPlannerLabels.parseAddonValue(type, "0"));
            else assertNull(FlightPlannerLabels.parseAddonValue(type, "0"));
            for (var invalid : new String[]{"-1", "1.5", "", "2147483648"}) {
                assertNull(FlightPlannerLabels.parseAddonValue(type, invalid), invalid);
            }
        }
    }
}
