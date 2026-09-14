package rearth.oritech.spaceage.simulation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketFlightPlanRulesTest {

    @Test
    void earthLandingOffsetOnlyUsesSelectedUncertainty() {
        var base = action(List.of());
        var withoutUncertainty = RocketFlightPlanRules.applyLandingUncertainty(base);
        assertEquals(0, withoutUncertainty.landingOffsetX());
        assertEquals(0, withoutUncertainty.landingOffsetZ());

        var distanceCondition = new SpaceSimulation.ActionAddon(UUID.randomUUID(),
                SpaceSimulation.ActionAddonType.DISTANCE_FROM_TARGET, 64);
        var withDistance = RocketFlightPlanRules.applyLandingUncertainty(action(List.of(distanceCondition)));
        assertEquals(0, withDistance.landingOffsetX());
        assertEquals(0, withDistance.landingOffsetZ());

        var uncertainty = new SpaceSimulation.ActionAddon(UUID.randomUUID(),
                SpaceSimulation.ActionAddonType.DESIRED_UNCERTAINTY, 256);
        var adjusted = RocketFlightPlanRules.applyLandingUncertainty(action(List.of(uncertainty)));
        assertTrue(Math.hypot(adjusted.landingOffsetX(), adjusted.landingOffsetZ()) <= 256);
        assertEquals(adjusted, RocketFlightPlanRules.applyLandingUncertainty(adjusted));
    }

    private static SpaceSimulation.FlightPlanAction action(List<SpaceSimulation.ActionAddon> addons) {
        return new SpaceSimulation.FlightPlanAction(new UUID(1, 2), SpaceSimulation.ActionType.NAVIGATE_TO,
                List.of(), SpaceObjects.EARTH_ID, SpaceSimulation.OrbitBand.SURFACE,
                SpaceSimulation.ArrivalVelocityMode.ZERO, 0, 0, 100, 200, 32, -24, addons);
    }
}
