package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ArrivalVelocityMode;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParachuteLandingTest {

    @Test
    void heavierCraftNeedMoreCanopiesAndDragNeverReversesDescent() {

        assertTrue(ParachuteLanding.terminalSpeed(10_000, 1) > ParachuteLanding.terminalSpeed(2_000, 1));
        assertTrue(ParachuteLanding.terminalSpeed(10_000, 4) < ParachuteLanding.terminalSpeed(10_000, 1));
        var points = ParachuteLanding.descent(1_000, 200, 2_000, 1, 2);
        assertEquals(2, points.getLast().altitude(), 1e-9);
        assertTrue(points.getLast().speed() <= ParachuteLanding.SAFE_SPEED);
        for (int i = 1; i < points.size(); i++) {
            assertTrue(points.get(i).seconds() > points.get(i - 1).seconds());
            assertTrue(points.get(i).altitude() < points.get(i - 1).altitude());
            assertTrue(points.get(i).speed() > 0);
        }
    }

    @Test
    void deployedCraftCanCompletePassiveLandingWithNoFuelOrEngines() {

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                SpaceAgeBlocks.PARACHUTE.get().defaultBlockState())), Map.of(), 2, 0);
        var rocket = new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(0, 0, 0, Set.of())));
        var earth = new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH, -3_000_000, 0,
                0, 0, 60_000, 9.81f, 0, SpaceObjects.DetectionState.PRECISE, "Earth", List.of());
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withOrbit(OrbitBand.SURFACE).withTarget(SpaceObjects.EARTH_ID);
        var plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(action))), List.of());
        var position = new MissionState.Position(-2_939_000, 0, -100, 0, SpaceObjects.EARTH_ID, OrbitBand.TIGHT,
                -1, 1, FlightPlanAction.NO_TARGET, SegmentRef.of(segment));
        var path = RocketFlightPathCalculator.calculateFrom(rocket, List.of(earth), plan, position);
        assertTrue(path.paths().getFirst().actionMoments().getFirst().completed());
        assertEquals(0, path.paths().getFirst().samples().getLast().speedMetersPerSecond());
        assertTrue(path.paths().getFirst().samples().stream().allMatch(s -> s.firingSegments().isEmpty()));
        assertTrue(path.arrivalPredictions().getFirst().parachutes().sufficient());
        assertEquals(0, rocket.getDynamicSegments().get(id).availableFuelBurnTimeTicks);
        assertFalse(ParachuteLanding.eligible(action.withVelocity(ArrivalVelocityMode.MAXIMUM, 0)));
        assertFalse(ParachuteLanding.eligible(action.withTarget(UUID.randomUUID())));
    }
}
