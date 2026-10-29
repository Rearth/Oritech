package rearth.oritech.spaceage.simulation.flight;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.mission.MissionState;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightMotionTest {

    private static PathSample sample(double time, double x, double vx, int stage, UUID asteroid) {

        return new PathSample(time, x, 0, vx, vx, 0, PathPhase.COAST, FlightPlanAction.NO_TARGET,
                FlightPlanAction.NO_TARGET, stage, Set.of(), Set.of(), asteroid);
    }

    @Test
    void discreteStateChangesAtTheEventAndUsesTheLastEqualTimeSample() {

        var asteroid = new UUID(740, 1);
        var samples = List.of(sample(0, 0, 1, 1, asteroid), sample(2, 2, 1, 1, asteroid),
                sample(2, 2, 0, 2, FlightPlanAction.NO_TARGET), sample(4, 2, 0, 2, FlightPlanAction.NO_TARGET));
        var previous = MissionState.Position.surface();
        var before = FlightMotion.position(samples, 1, previous);
        assertEquals(1, before.stage());
        assertEquals(asteroid, before.asteroid());
        assertEquals(1, before.x(), 1e-6);
        var atEvent = FlightMotion.position(samples, 2, previous);
        assertEquals(2, atEvent.stage());
        assertEquals(FlightPlanAction.NO_TARGET, atEvent.asteroid());
        assertEquals(0, atEvent.vx());
        assertEquals(atEvent.x(), FlightMotion.sample(samples, 2).x());
        assertEquals(2, FlightMotion.position(samples, 3, previous).x());
    }
}
