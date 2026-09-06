package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import java.util.*;

/** Integration checks for the planner's stage/resource handling and editor branch rules. */
public final class RocketFlightPathTest {
    @Test
    void flightPathsAndBranches() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, Blocks.STONE.defaultBlockState())),
                Map.of(), 25, 1);
        var rocket = new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(0, 2_000_000, 0, Set.of())));
        var objects = List.of(
                new SpaceSimulation.SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                        0, 0, 0, 0, SpaceObjects.DetectionState.PRECISE),
                new SpaceSimulation.SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                        8_000_000, 0, 0, 0, SpaceObjects.DetectionState.PRECISE));
        var action = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceSimulation.MARS_ID).withOrbit(SpaceSimulation.OrbitBand.SURFACE);
        var base = SpaceSimulation.FlightPlan.empty();
        var plan = base.withBranches(List.of(base.root().withActions(List.of(action))));
        var path = RocketFlightPathCalculator.calculate(rocket, objects, plan).paths().getFirst();
        ready(path);
        require(path.samples().stream().anyMatch(sample -> sample.phase() == RocketFlightPathCalculator.PathPhase.COAST
                && sample.timeSeconds() > 100 && sample.timeSeconds() < path.durationSeconds() - 100), "middle coast");
        var capped = plan.withBranches(List.of(base.root().withActions(List.of(action.withMaxSpeed(100)))));
        var slower = RocketFlightPathCalculator.calculate(rocket, objects, capped).paths().getFirst();
        ready(slower);
        require(slower.remainingDeltaV() > path.remainingDeltaV() + 700, "speed cap saves fuel");
        require(slower.samples().stream().allMatch(sample -> sample.speedMetersPerSecond() <= 100.0001), "speed cap");
        var maximum = action.withVelocity(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0);
        var collision = RocketFlightPathCalculator.calculate(rocket, objects,
                plan.withBranches(List.of(base.root().withActions(List.of(maximum)))));
        require(collision.paths().getFirst().terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED,
                "maximum-speed surface arrival is destructive");
        require(collision.arrivalPredictions().getFirst().impact().outcome()
                        == AsteroidImpactRules.ArrivalOutcome.HEAVY_IMPACT,
                "maximum-speed surface arrival predicts a heavy impact");

        // Stage one drops a spent booster; the retained craft must finish the transfer at its new full power.
        var boosterId = UUID.randomUUID();
        var core = new StaticRocketSegment(id, segment.blocks(), Map.of(boosterId, Set.of()), 25, 1);
        var booster = new StaticRocketSegment(boosterId,
                Set.of(new StaticRocketSegment.BlockData(new BlockPos(2, 0, 0), Blocks.STONE.defaultBlockState())),
                Map.of(id, Set.of()), 25, 1);
        var stagedRocket = new ActiveRocketData(Map.of(id, core, boosterId, booster), Map.of(
                id, new DynamicRocketSegment(0, 2_000_000, 0, Set.of(boosterId)),
                boosterId, new DynamicRocketSegment(0, 2_000_000, 0, Set.of(id))));
        var stagedPlan = plan.withSegmentConfigurations(List.of(
                new SpaceSimulation.SegmentConfiguration(SpaceSimulation.SegmentRef.of(core), "Core", false, List.of(2)),
                new SpaceSimulation.SegmentConfiguration(SpaceSimulation.SegmentRef.of(booster), "Booster", true, List.of(1))));
        var staged = RocketFlightPathCalculator.calculate(stagedRocket, objects, stagedPlan);
        ready(staged.paths().getFirst());
        require(staged.boosterEvents().size() == 1, "one stage separation");
        require(staged.paths().getFirst().samples().stream().anyMatch(sample -> sample.stage() == 2), "second stage fires");

        var unequal = new ActiveRocketData(Map.of(id, core, boosterId, booster), Map.of(
                id, new DynamicRocketSegment(0, 2_000_000, 0, Set.of(boosterId)),
                boosterId, new DynamicRocketSegment(0, 4_000_000, 0, Set.of(id))));
        var unequalPath = RocketFlightPathCalculator.calculate(unequal, objects, plan).paths().getFirst();
        ready(unequalPath);
        ready(RocketFlightPathCalculator.calculate(unequal, objects, capped).paths().getFirst());

        var link = SpaceSimulation.FlightPlanAction.disconnectBooster(UUID.randomUUID(),
                SpaceSimulation.SegmentRef.of(segment), action.id());
        var parent = base.root().withActions(List.of(action, link));
        var child = new SpaceSimulation.FlightPlanBranch(UUID.randomUUID(), link.id(),
                List.of(SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.DISCARD_CRAFT)));
        var edited = parent.withActions(RocketFlightPlanRules.preserveBoosterLinks(parent, List.of(action.withMaxSpeed(100))));
        var normalized = RocketFlightPlanRules.normalize(base.withBranches(List.of(edited, child)));
        require(normalized.branches().contains(child), "editing a navigation card preserves its booster program");
        System.out.println("Flight path and branch regressions passed");
    }

    @Test
    void marsArrivalDoesNotOvershootWhenBoosterRunsDryDuringBraking() {
        checkMarsArrival(15_000_000, 1000);
    }

    @Test
    void coastingJustBeforeEngineExhaustionKeepsTheRemainingBurn() {
        checkMarsArrival(15_000_001, 1500);
    }

    @Test
    void navigationCompletionConditionsStopEarlyWithoutEndingThePlan() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var segmentId = UUID.randomUUID();
        var segment = new StaticRocketSegment(segmentId,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, Blocks.STONE.defaultBlockState())),
                Map.of(), 25, 1);
        var rocket = new ActiveRocketData(Map.of(segmentId, segment),
                Map.of(segmentId, new DynamicRocketSegment(0, 2_000_000, 0, Set.of())));
        var objects = List.of(
                new SpaceSimulation.SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                        0, 0, 0, 0, SpaceObjects.DetectionState.PRECISE),
                new SpaceSimulation.SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                        8_000_000, 0, 0, 0, SpaceObjects.DetectionState.PRECISE));
        var base = SpaceSimulation.FlightPlan.empty();

        for (var type : SpaceSimulation.ActionAddonType.values()) {
            int threshold = switch (type) {
                case DISTANCE_FROM_TARGET -> 500_000;
                case TIME_BEFORE_ARRIVAL -> 120;
                case DESIRED_UNCERTAINTY -> 512;
            };
            var addon = new SpaceSimulation.ActionAddon(UUID.randomUUID(), type, threshold);
            var navigate = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                    .withTarget(SpaceSimulation.MARS_ID).withOrbit(SpaceSimulation.OrbitBand.SURFACE)
                    .withAddons(List.of(addon));
            var maintain = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.MAINTAIN_POSITION);
            var plan = base.withBranches(List.of(base.root().withActions(List.of(navigate, maintain))));
            var result = RocketFlightPathCalculator.calculate(rocket, objects, plan);
            var path = result.paths().getFirst();
            require(path.terminalState() == RocketFlightPathCalculator.TerminalState.MAINTAINING_POSITION,
                    type + " condition did not continue to the next card");
            require(path.actionMoments().size() == 2 && path.actionMoments().stream()
                    .allMatch(RocketFlightPathCalculator.ActionMoment::completed), type + " actions did not complete");
            require(result.navigationAborts().size() == 1, type + " did not record its completion condition");
            var abort = result.navigationAborts().getFirst();
            require(abort.addonId().equals(addon.id()) && abort.actualValue() <= threshold,
                    type + " stopped outside its configured threshold");
            require(Math.hypot(abort.x() - 8_000_000, abort.y()) > 0.01,
                    type + " completed at the destination instead of early");
            require(Math.hypot(abort.destinationX() - 8_000_000, abort.destinationY()) < 0.01,
                    type + " did not retain its intended destination");
        }
    }

    @Test
    void allTargetPointsUseStableRandomOffsets() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var segmentId = UUID.randomUUID();
        var segment = new StaticRocketSegment(segmentId,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, Blocks.STONE.defaultBlockState())),
                Map.of(), 25, 1);
        var rocket = new ActiveRocketData(Map.of(segmentId, segment),
                Map.of(segmentId, new DynamicRocketSegment(0, 100_000_000, 0, Set.of())));
        var earth = new SpaceSimulation.SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                0, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE);
        var mars = new SpaceSimulation.SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                8_000_000, 0, 45_000, 3.71f, SpaceObjects.DetectionState.PRECISE);
        var visitMars = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceSimulation.MARS_ID).withOrbit(SpaceSimulation.OrbitBand.SURFACE);
        var earthSurface = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceObjects.EARTH_ID).withOrbit(SpaceSimulation.OrbitBand.SURFACE);
        var earthHigh = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceObjects.EARTH_ID).withOrbit(SpaceSimulation.OrbitBand.HIGH);
        var actions = List.of(visitMars, earthSurface, earthHigh);
        var base = SpaceSimulation.FlightPlan.empty();
        var plan = base.withBranches(List.of(base.root().withActions(actions)));

        var firstCalculation = RocketFlightPathCalculator.calculate(rocket, List.of(earth, mars), plan);
        var path = firstCalculation.paths().getFirst();
        ready(path);
        require(path.actionMoments().size() == actions.size(), "all navigation actions completed");
        for (var action : actions) {
            require(path.samples().stream().anyMatch(sample -> sample.actionId().equals(action.id())),
                    "navigation samples lost their action boundary");
        }
        var departure = path.samples().getFirst();
        require(Math.abs(Math.hypot(departure.x() - earth.x(), departure.y() - earth.y()) - earth.radius()) < 0.01,
                "first path did not start on Earth's surface");
        var targets = Map.of(SpaceObjects.EARTH_ID, earth, SpaceSimulation.MARS_ID, mars);
        double sourceX = departure.x();
        double sourceY = departure.y();
        double startTime = 0;
        for (int index = 0; index < actions.size(); index++) {
            var action = actions.get(index);
            var target = targets.get(action.targetId());
            var arrival = path.actionMoments().get(index);
            double targetX = target.xAt(startTime);
            double targetY = target.yAt(startTime);
            double nearestAngle = Math.atan2(sourceY - targetY, sourceX - targetX);
            double arrivalAngle = Math.atan2(arrival.y() - target.yAt(arrival.timeSeconds()),
                    arrival.x() - target.xAt(arrival.timeSeconds()));
            double difference = Math.abs(nearestAngle - arrivalAngle) % (Math.PI * 2);
            difference = Math.min(difference, Math.PI * 2 - difference);
            require(difference <= Math.toRadians(70) + 0.000001,
                    "target point exceeded the 70 degree range");
            require(Math.abs(Math.hypot(arrival.x() - target.xAt(arrival.timeSeconds()),
                    arrival.y() - target.yAt(arrival.timeSeconds()))
                    - (target.radius() + action.orbit().altitude())) < 0.01,
                    "target point did not lie on the selected surface or orbit");
            sourceX = arrival.x();
            sourceY = arrival.y();
            startTime = arrival.timeSeconds();
        }

        var repeated = RocketFlightPathCalculator.calculate(rocket, List.of(earth, mars), plan)
                .paths().getFirst().actionMoments();
        for (int index = 0; index < repeated.size(); index++) {
            var first = path.actionMoments().get(index);
            var second = repeated.get(index);
            require(Math.abs(first.x() - second.x()) < 0.000001
                            && Math.abs(first.y() - second.y()) < 0.000001,
                    "random target point changed during recalculation");
        }
    }

    private void checkMarsArrival(long boosterRF, int speedCap) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var coreId = UUID.randomUUID();
        var boosterId = UUID.randomUUID();
        var core = new StaticRocketSegment(coreId,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, Blocks.STONE.defaultBlockState())),
                Map.of(boosterId, Set.of()), 100, 1);
        var booster = new StaticRocketSegment(boosterId,
                Set.of(new StaticRocketSegment.BlockData(new BlockPos(2, 0, 0), Blocks.STONE.defaultBlockState())),
                Map.of(coreId, Set.of()), 25, 4);
        var rocket = new ActiveRocketData(Map.of(coreId, core, boosterId, booster), Map.of(
                coreId, new DynamicRocketSegment(0, 40_000_000, 0, Set.of(boosterId)),
                boosterId, new DynamicRocketSegment(0, boosterRF, 0, Set.of(coreId))));
        var objects = List.of(
                new SpaceSimulation.SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
                        -3_000_000, 0, 60_000, 9.81f, SpaceObjects.DetectionState.PRECISE),
                new SpaceSimulation.SpaceObjectData(SpaceSimulation.MARS_ID, SpaceObjects.ObjectType.MARS,
                        5_000_000, 1_500_000, 45_000, 3.71f, SpaceObjects.DetectionState.PRECISE));
        var base = SpaceSimulation.FlightPlan.empty();
        var action = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(SpaceSimulation.MARS_ID).withOrbit(SpaceSimulation.OrbitBand.HIGH).withMaxSpeed(speedCap);
        var plan = new SpaceSimulation.FlightPlan(List.of(base.root().withActions(List.of(action))), List.of(
                new SpaceSimulation.SegmentConfiguration(SpaceSimulation.SegmentRef.of(core), "Core", false, List.of(2)),
                new SpaceSimulation.SegmentConfiguration(SpaceSimulation.SegmentRef.of(booster), "Booster", true, List.of(1))));
        var result = RocketFlightPathCalculator.calculate(rocket, objects, plan);
        var path = result.paths().getFirst();
        ready(path);
        var departure = path.samples().getFirst();
        var arrival = path.samples().getLast();
        double routeX = arrival.x() - departure.x();
        double routeY = arrival.y() - departure.y();
        double routeLength = Math.hypot(routeX, routeY);
        var previousDistance = 0d;
        for (var sample : path.samples()) {
            var distance = ((sample.x() - departure.x()) * routeX
                    + (sample.y() - departure.y()) * routeY) / routeLength;
            require(distance <= routeLength + 0.01, "overshot Mars orbit by " + (distance - routeLength));
            require(distance >= previousDistance - 0.01, "route turned back after overshooting");
            require(sample.speedMetersPerSecond() <= speedCap + 0.0001, "cruise cap exceeded");
            previousDistance = distance;
        }
        require(Math.abs(Math.hypot(arrival.x() - 5_000_000, arrival.y() - 1_500_000) - 105_000) < 0.01,
                "arrival missed Mars high orbit");
        require(result.boosterEvents().size() == 1, "one booster separation");
        var eventTime = result.boosterEvents().getFirst().timeSeconds();
        require(path.samples().stream().anyMatch(sample -> sample.timeSeconds() <= eventTime
                && sample.phase() == RocketFlightPathCalculator.PathPhase.BRAKE), "braking before separation");
        require(path.samples().stream().anyMatch(sample -> sample.timeSeconds() > eventTime
                && sample.phase() == RocketFlightPathCalculator.PathPhase.BRAKE), "braking after separation");
        require(path.samples().getLast().speedMetersPerSecond() < 0.0001, "stopped at arrival");
    }

    private static void ready(RocketFlightPathCalculator.CraftPath path) {
        require(!path.terminalState().isFailure(), "route failed: " + path.terminalState());
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
