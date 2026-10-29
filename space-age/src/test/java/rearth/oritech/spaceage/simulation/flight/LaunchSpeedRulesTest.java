package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ArrivalVelocityMode;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchSpeedRulesTest {

    @Test
    void windowKeepsLaterWorkingRanges() {

        var window = LaunchSpeedRules.window(30_000, speed -> speed >= 4_000 && speed <= 5_000 || speed >= 12_000);
        assertEquals(List.of(new LaunchSpeedRules.Range(4_000, 5_000), new LaunchSpeedRules.Range(12_000, Long.MAX_VALUE)), window.ranges());
        assertEquals(4_000, window.min());
    }

    @Test
    void laterNavigationCannotChangeTheLaunchWindowOrFirstLeg() {

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                SpaceAgeBlocks.NAVIGATION_COMPUTER.get().defaultBlockState())), Map.of(), 1, 0);
        var rocket = new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(0, 0, 0, Set.of())));
        rocket.setLaunchPosition(BlockPos.ZERO);
        var objects = new SpaceSimulation().createObjectData();
        var first = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID)
                .withOrbit(OrbitBand.HIGH).withVelocity(ArrivalVelocityMode.MAXIMUM, 0);
        var later = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceSimulation.MARS_ID).withOrbit(OrbitBand.HIGH);
        var empty = FlightPlan.empty();
        var single = empty.withBranches(List.of(empty.root().withActions(List.of(first))));
        var extended = single.withBranches(List.of(single.root().withActions(List.of(first, later))));

        assertTrue(LaunchSpeedRules.valid(single, rocket, objects, 10_000, List.of(), List.of()));
        assertTrue(LaunchSpeedRules.valid(extended, rocket, objects, 10_000, List.of(), List.of()));
        assertEquals(LaunchSpeedRules.window(single, rocket, objects, List.of(), List.of()),
                LaunchSpeedRules.window(extended, rocket, objects, List.of(), List.of()));
        assertEquals(single, LaunchSpeedRules.firstLeg(extended));

        var position = LaunchSpeedRules.position(objects, BlockPos.ZERO, 10_000);
        var firstPath = RocketFlightPathCalculator.calculateFrom(rocket, objects, single, position, List.of(), List.of()).paths().getFirst();
        var fullPath = RocketFlightPathCalculator.calculateFrom(rocket, objects, extended, position, List.of(), List.of()).paths().getFirst();
        assertEquals(firstPath.actionMoments().getFirst(), fullPath.actionMoments().getFirst());
        assertTrue(fullPath.terminalState().isFailure(), "The unpowered craft must still fail the later stop at Mars");
    }

    @Test
    void windowFindsBothBoundsWhenTooFastAlsoFails() {

        var window = LaunchSpeedRules.window(30_000, speed -> speed >= 6_200 && speed <= 11_800);
        assertTrue(Math.abs(window.min() - 6_200) <= 2, "lower bound " + window.min());
        assertTrue(Math.abs(window.max() - 11_800) <= 2, "upper bound " + window.max());
    }

    @Test
    void windowReportsPlansWithoutSpeedAndUnreachablePlans() {

        var free = LaunchSpeedRules.window(30_000, speed -> true);
        assertEquals(0, free.min());
        assertEquals(Long.MAX_VALUE, free.max());
        assertNull(LaunchSpeedRules.window(30_000, speed -> false));
        var open = LaunchSpeedRules.window(30_000, speed -> speed >= 9_000);
        assertEquals(Long.MAX_VALUE, open.max());
    }

    @Test
    void launchVelocityIsCappedAndOrbitNeedsAReachableSpeed() {

        assertEquals(SpaceAgeConfig.launchSpeedCap(), LaunchSpeedRules.velocity(Long.MAX_VALUE / 2));
        assertTrue(LaunchSpeedRules.orbitVelocity() > 0);
        assertTrue(LaunchSpeedRules.velocity(LaunchSpeedRules.orbitParticleSpeed()) >= LaunchSpeedRules.orbitVelocity());
        assertTrue(LaunchSpeedRules.orbitParticleSpeed() <= LaunchSpeedRules.maxUsefulSpeed(), "the default cap reaches orbit");
    }

    @Test
    void finerSamplingFindsAWindowBetweenTheOriginalCoarseSamples() {

        var window = LaunchSpeedRules.window(30_000, speed -> speed >= 6_500 && speed <= 6_600);
        assertEquals(6_500, window.min());
        assertEquals(6_600, window.max());
    }

}
