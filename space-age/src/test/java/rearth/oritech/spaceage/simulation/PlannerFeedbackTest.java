package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.TerminalState.*;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.*;

class PlannerFeedbackTest {
    private static final UUID REGION_ID = new UUID(10, 20);
    private static final SpaceObjectData EARTH = new SpaceObjectData(SpaceObjects.EARTH_ID,
            SpaceObjects.ObjectType.EARTH, 0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
    private static final SpaceObjectData REGION = new SpaceObjectData(REGION_ID,
            SpaceObjects.ObjectType.SURVEY_REGION, 160_000, 0, 55_000, 0,
            SpaceObjects.DetectionState.PRECISE);

    private static ActiveRocketData rocket(boolean ion, boolean scanner, long rf) {
        var id = new UUID(1, 1);
        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                (ion ? SpaceAgeBlocks.ION_BOOSTER_ROCKET : SpaceAgeBlocks.BASIC_BOOSTER_ROCKET)
                        .get().defaultBlockState()));
        if (scanner) blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO.above(),
                SpaceAgeBlocks.SPACE_SCANNER.get().defaultBlockState()));
        var segment = new StaticRocketSegment(id, blocks, Map.of(), 5, 1);
        return new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(100_000, rf, 0, Set.of())));
    }

    private static FlightPlan plan(FlightPlanAction... actions) {
        var plan = FlightPlan.empty();
        return plan.withBranches(List.of(plan.root().withActions(List.of(actions))));
    }

    private static FlightPlanAction navigate(UUID target) {
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO)
                .withTarget(target).withOrbit(OrbitBand.SURFACE);
        return new FlightPlanAction(new UUID(3, target.getLeastSignificantBits() ^ 4), action.type(),
                action.segments(), action.targetId(), action.orbit(), action.velocityMode(),
                action.targetVelocity(), action.maxSpeed(), action.landingX(), action.landingZ(), 0, 0,
                action.addons(), action.service());
    }

    private static MissionState.Position atRegion() {
        return new MissionState.Position(105_000, 0, 800, 0, REGION_ID, OrbitBand.SURFACE,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
    }

    @Test
    void surveyRoundTripsRequireEnoughAtmosphericThrust() {
        var scan = FlightPlanAction.create(ActionType.SCAN);
        for (boolean ion : List.of(false, true)) {
            var result = RocketFlightPathCalculator.calculate(rocket(ion, true, 50_000_000),
                    List.of(EARTH, REGION), plan(navigate(REGION_ID), scan, navigate(SpaceObjects.EARTH_ID)));
            var path = result.paths().getFirst();
            if (ion) assertEquals(NO_FEASIBLE_TRANSFER, path.terminalState());
            else {
                assertEquals(READY, path.terminalState());
                assertEquals(3, path.actionMoments().size());
                assertTrue(path.actionMoments().stream().allMatch(RocketFlightPathCalculator.ActionMoment::completed));
                assertTrue(result.arrivalPredictions().stream().noneMatch(arrival -> arrival.targetId().equals(REGION_ID)),
                        "Survey regions are navigation areas, not impact surfaces");
            }
        }
    }

    @Test
    void scanPlanningReflectsHardwareRangeAndFixedEnergy() {
        var scan = FlightPlanAction.create(ActionType.SCAN);
        var result = RocketFlightPathCalculator.calculateFrom(rocket(false, true, 1_000_000),
                List.of(EARTH, REGION), plan(scan), atRegion());
        assertEquals(READY, result.paths().getFirst().terminalState());
        assertEquals(1_000_000, result.scanEstimates().getFirst().availableRF());
        assertEquals(0, result.paths().getFirst().durationSeconds(), 1e-9);
        assertEquals(800, result.paths().getFirst().samples().getLast().speedMetersPerSecond(), 1e-9);

        assertEquals(NO_SCANNER, RocketFlightPathCalculator.calculateFrom(rocket(false, false, 1_000_000),
                List.of(EARTH, REGION), plan(scan), atRegion()).paths().getFirst().terminalState());
        assertEquals(NOT_ENOUGH_SERVICE_RF,
                RocketFlightPathCalculator.calculateFrom(rocket(false, true, 100),
                        List.of(EARTH, REGION), plan(scan), atRegion()).paths().getFirst().terminalState());

        var oversized = new SpaceObjectData(REGION_ID, SpaceObjects.ObjectType.SURVEY_REGION,
                160_000, 0, 310_000, 0, SpaceObjects.DetectionState.PRECISE);
        assertEquals(READY,
                RocketFlightPathCalculator.calculateFrom(rocket(false, true, 5_000_000),
                        List.of(EARTH, oversized), plan(scan), atRegion()).paths().getFirst().terminalState());
    }

}
