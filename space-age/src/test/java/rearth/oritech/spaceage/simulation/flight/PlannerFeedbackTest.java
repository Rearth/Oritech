package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.survey.EarthScan;
import rearth.oritech.spaceage.simulation.survey.EarthScan.Target;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState.NOT_ENOUGH_SERVICE_RF;
import static rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState.EARTH_SCAN_OUT_OF_RANGE;
import static rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState.NO_FEASIBLE_TRANSFER;
import static rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState.NO_SCANNER;
import static rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState.READY;

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
        // Keep ion acceleration near 5 m/s², below Earth gravity, even when its thrust is retuned.
        var weight = Math.round(SpaceBalance.ION_THRUST_NEWTONS * SpaceBalance.ION_ATMOSPHERE
                / (5 * SpaceBalance.KILOGRAMS_PER_WEIGHT_UNIT));
        var segment = new StaticRocketSegment(id, blocks, Map.of(), weight, 1);
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

    @Test
    void earthScanPreviewUsesCraftPositionAndKeepsScannerPowerRules() {

        var scan = FlightPlanAction.create(ActionType.SCAN);
        scan = scan.withSettings(scan.settings().withEarthScan(Optional.of(new EarthScan(100, -200,
                new Target(Identifier.withDefaultNamespace("deep_dark"), true)))));
        var orbit = new MissionState.Position(SpaceObjects.EARTH_X + SpaceObjects.EARTH_RADIUS + 10_000, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.LOW,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        assertEquals(EARTH_SCAN_OUT_OF_RANGE, RocketFlightPathCalculator.calculateFrom(rocket(false, true, 1_000_000),
                List.of(EARTH, REGION), plan(scan), atRegion()).paths().getFirst().terminalState());
        var surface = orbit.placed(SpaceObjects.EARTH_SURFACE_X, 0, 0, 0, orbit.target(), OrbitBand.SURFACE, -1);
        assertEquals(EARTH_SCAN_OUT_OF_RANGE, RocketFlightPathCalculator.calculateFrom(rocket(false, true, 1_000_000),
                List.of(EARTH, REGION), plan(scan), surface).paths().getFirst().terminalState());
        assertEquals(NOT_ENOUGH_SERVICE_RF, RocketFlightPathCalculator.calculateFrom(rocket(false, true, 100),
                List.of(EARTH, REGION), plan(scan), orbit).paths().getFirst().terminalState());
        var low = RocketFlightPathCalculator.calculateFrom(rocket(false, true, 1_000_000), List.of(EARTH, REGION), plan(scan), orbit);
        assertEquals(READY, low.paths().getFirst().terminalState());
        assertEquals(500, low.scanEstimates().getFirst().radius());
        assertEquals(600_000, low.scanEstimates().getFirst().requiredRF());

        var waypoint = new NavigationWaypoint(-2_943_966, 44_794).point(REGION_ID);
        var between = orbit.placed(waypoint.x(), waypoint.y(), 0, 0, waypoint.id(), OrbitBand.SURFACE, -1);
        var waypointScan = RocketFlightPathCalculator.calculateFrom(rocket(false, true, 1_000_000),
                List.of(EARTH, waypoint), plan(scan), between);
        assertEquals(READY, waypointScan.paths().getFirst().terminalState());
        assertEquals(587, waypointScan.scanEstimates().getFirst().radius());
        assertEquals(704_400, waypointScan.scanEstimates().getFirst().requiredRF());

        var higherOrbit = orbit.placed(orbit.x() + 20_000, orbit.y(), 0, 0, orbit.target(), OrbitBand.MEDIUM, -1);
        var medium = RocketFlightPathCalculator.calculateFrom(rocket(false, true, 1_000_000), List.of(EARTH, REGION), plan(scan), higherOrbit);
        assertEquals(1500, medium.scanEstimates().getFirst().radius());
        assertEquals(1_800_000, medium.scanEstimates().getFirst().requiredRF());
        assertEquals(NOT_ENOUGH_SERVICE_RF, medium.paths().getFirst().terminalState());
    }

}
