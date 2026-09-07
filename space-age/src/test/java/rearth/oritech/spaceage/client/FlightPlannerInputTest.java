package rearth.oritech.spaceage.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType.*;

/** Keep the editor's accepted input and limits unchanged when moving it out of the screen. */
class FlightPlannerInputTest {

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
        assertNull(FlightPlannerLabels.parseAddonValue(DISTANCE_FROM_TARGET, "10000001"));
        assertNull(FlightPlannerLabels.parseAddonValue(TIME_BEFORE_ARRIVAL, "1000001"));
        assertNull(FlightPlannerLabels.parseAddonValue(DESIRED_UNCERTAINTY, "100001"));
        for (var type : values()) {
            assertEquals(1, FlightPlannerLabels.parseAddonValue(type, " 1 "));
            for (var invalid : new String[]{"0", "-1", "1.5", "", "2147483648"}) {
                assertNull(FlightPlannerLabels.parseAddonValue(type, invalid), invalid);
            }
        }
    }
}
