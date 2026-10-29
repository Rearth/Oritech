package rearth.oritech.spaceage.simulation.rocket;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentConfiguration;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator.LaunchReadiness;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketPerformanceTest {

    @Test
    void shortFirstStageCanHandOffToTheNextStageBeforeOrbit() {

        var boosterId = new UUID(710, 1);
        var coreId = new UUID(710, 2);
        var booster = new StaticRocketSegment(boosterId, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(), 1, 1);
        var core = new StaticRocketSegment(coreId, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO.above(),
                SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(), 1, 1);
        var rocket = new ActiveRocketData(Map.of(boosterId, booster, coreId, core),
                Map.of(boosterId, new DynamicRocketSegment(1, 0, 0, Set.of(coreId)),
                        coreId, new DynamicRocketSegment(100_000, 0, 0, Set.of(boosterId))));
        var base = FlightPlan.empty();
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH);
        var plan = base.withBranches(List.of(base.root().withActions(List.of(action)))).withSegmentConfigurations(List.of(
                new SegmentConfiguration(SegmentRef.of(booster), "", true, List.of(1)),
                new SegmentConfiguration(SegmentRef.of(core), "", false, List.of(2))));
        var performance = RocketPerformanceCalculator.calculate(rocket, plan, 1);

        assertEquals(2000, performance.wetMassKilograms());
        assertEquals(1, performance.engineCount());
        assertEquals(SpaceBalance.CHEMICAL_THRUST_NEWTONS, performance.thrustNewtons());
        assertTrue(performance.availableBurnSeconds() < RocketAscent.launch(performance, 0).secondsTo(1000));
        assertEquals(LaunchReadiness.READY, RocketPerformanceCalculator.getLaunchReadiness(rocket, plan));
        var flight = RocketFlightPathCalculator.calculate(rocket, new SpaceSimulation().createObjectData(), plan);
        assertEquals(TerminalState.READY, flight.paths().getFirst().terminalState());
        assertEquals(1, flight.boosterEvents().size());

        // Liftoff is possible with the same hardware even when the route's fuel budget is not.
        rocket.getDynamicSegments().get(coreId).consumeFuel(100_000);
        assertEquals(LaunchReadiness.READY, RocketPerformanceCalculator.getLaunchReadiness(rocket, plan));
        assertTrue(RocketFlightPathCalculator.calculate(rocket, new SpaceSimulation().createObjectData(), plan)
                .paths().getFirst().terminalState().preventsLaunch());
    }
}
