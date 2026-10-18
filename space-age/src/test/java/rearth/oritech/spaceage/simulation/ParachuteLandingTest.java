package rearth.oritech.spaceage.simulation;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ArrivalVelocityMode;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ParachuteLandingTest {

    @Test
    void brakingCreditScalesWithCanopyCountAndRocketWeight() {

        assertEquals(0, ParachuteLanding.deltaVReduction(248_140, 0));
        var single = ParachuteLanding.deltaVReduction(248_140, 1);
        assertEquals(single * 3, ParachuteLanding.deltaVReduction(248_140, 3), 1e-9);
        assertEquals(single / 2, ParachuteLanding.deltaVReduction(496_280, 1), 1e-9);
        assertEquals(ParachuteLanding.DELTA_V_PER_PARACHUTE, ParachuteLanding.deltaVReduction(ParachuteLanding.REFERENCE_MASS, 1));
        assertEquals(.8, ParachuteLanding.fuelFactor(20, 100), 1e-9);
        assertEquals(0, ParachuteLanding.fuelFactor(200, 100));
    }

    @Test
    void parachutesOnlyDiscountStoppedEarthSurfaceArrivals() {

        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.SURFACE);
        assertTrue(ParachuteLanding.eligible(action));
        assertFalse(ParachuteLanding.eligible(action.withVelocity(ArrivalVelocityMode.MAXIMUM, 0)));
        assertFalse(ParachuteLanding.eligible(action.withOrbit(OrbitBand.LOW)));
        assertFalse(ParachuteLanding.eligible(action.withTarget(UUID.randomUUID())));
    }
}
