package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ServiceSettings;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketExtensionRulesTest {

    private static ActiveRocketData rocket(int computers) {

        var id = UUID.randomUUID();
        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.ANTENNA.get().defaultBlockState()));
        for (int i = 0; i < computers; i++)
            blocks.add(new StaticRocketSegment.BlockData(new BlockPos(i + 1, 0, 0),
                    SpaceAgeBlocks.NAVIGATION_COMPUTER.get().defaultBlockState()));
        var segment = new StaticRocketSegment(id, blocks, Map.of(), 1, 0);
        return new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(0, 100000, 0, Set.of())));
    }

    @Test
    void computerCapacityAndOptionalAutomationUseActualHardware() {

        var actions = IntStream.range(0, 4).mapToObj(i -> FlightPlanAction.create(ActionType.TRANSMIT_INFORMATION)).toList();
        var plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, actions)), List.of());
        var empty = new RocketFlightPathCalculator.FlightPath(List.of(), List.of(), 0);
        assertEquals("capacity", NavigationComputerRules.issues(plan, rocket(0), empty).getFirst().code());
        assertTrue(NavigationComputerRules.issues(plan, rocket(1), empty).isEmpty());
        var conditional = actions.getFirst().withService(new ServiceSettings(20, 100, -1));
        var shortPlan = plan.withBranches(List.of(plan.root().withActions(List.of(conditional))));
        assertEquals("computer_required", NavigationComputerRules.issues(shortPlan, rocket(0), empty).getFirst().code());
        assertTrue(NavigationComputerRules.issues(shortPlan, rocket(2), empty).isEmpty());
    }

    @Test
    void childProgramsConsumeMemoryUntilSeparation() {

        var split = FlightPlanAction.create(ActionType.DECOUPLE);
        var root = new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(split));
        var child = new FlightPlanBranch(UUID.randomUUID(), split.id(), List.of(
                FlightPlanAction.create(ActionType.SCAN), FlightPlanAction.create(ActionType.SCAN), FlightPlanAction.create(ActionType.SCAN)));
        var plan = new FlightPlan(List.of(root, child), List.of());
        assertEquals(4, NavigationComputerRules.unfinished(plan, root, 0));
        assertEquals(0, NavigationComputerRules.unfinished(plan, root, 1));
        assertEquals(3, NavigationComputerRules.unfinished(plan, child, 0));
    }

    @Test
    void fuelMassAndBlastDecreaseProportionallyWithoutRoundingEachTick() {

        var rocket = rocket(0);
        var resources = rocket.getDynamicSegments().values().iterator().next();
        resources.availableFuelBurnTimeTicks = 100;
        resources.currentFuelWeight = 1;
        var blast = RocketExplosives.remaining(rocket);
        for (int i = 0; i < 50; i++) resources.consumeFuel(1);
        assertEquals(.5, resources.currentFuelWeight, 1e-12);
        assertEquals(blast / 2, RocketExplosives.remaining(rocket), 1e-12);
        resources.consumeFuel(100);
        assertEquals(0, resources.currentFuelWeight);
        assertEquals(0, RocketExplosives.remaining(rocket));
    }

    @Test
    void safePayloadArrivalDoesNotIgniteButDetonationFragmentsAsteroids() {

        var target = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID,
                0, 0, 0, 0, 100, .01f, 1, SpaceObjects.DetectionState.PRECISE, "Test", List.of());
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO);
        var safe = AsteroidImpactRules.predictArrival(1000, target, 0, null, action, 1_000_000_000, false);
        assertEquals(AsteroidImpactRules.ArrivalOutcome.SAFE_APPROACH, safe.outcome());
        assertEquals(0, safe.explosiveEnergyJoules());
        var blast = AsteroidImpactRules.predictArrival(1000, target, 0, null, action, 1_000_000_000, true);
        assertEquals(AsteroidImpactRules.FragmentationMode.CATASTROPHIC, blast.fragmentationMode());
        assertEquals(1_000_000_000, blast.totalEnergyJoules());
        assertTrue(RocketExplosives.worldStrength(Double.MAX_VALUE) <= 32);
    }

    @Test
    void droppingComputersChecksFutureMemoryEvenWhenAServiceForecastIsBlocked() {

        var parentId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        var parent = new StaticRocketSegment(parentId, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.ANTENNA.get().defaultBlockState())), Map.of(), 1, 0);
        var child = new StaticRocketSegment(childId, Set.of(new StaticRocketSegment.BlockData(new BlockPos(10, 0, 0), SpaceAgeBlocks.NAVIGATION_COMPUTER.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(11, 0, 0), SpaceAgeBlocks.NAVIGATION_COMPUTER.get().defaultBlockState())), Map.of(), 1, 0);
        var rocket = new ActiveRocketData(Map.of(parentId, parent, childId, child), Map.of(parentId, new DynamicRocketSegment(0, 0, 0, Set.of(childId)),
                childId, new DynamicRocketSegment(0, 0, 0, Set.of(parentId))));
        var split = FlightPlanAction.create(ActionType.DECOUPLE).withSegments(List.of(SegmentRef.of(parent), SegmentRef.of(child)));
        var actions = new ArrayList<FlightPlanAction>();
        actions.add(split);
        for (int n = 0; n < 4; n++) actions.add(FlightPlanAction.create(ActionType.TRANSMIT_INFORMATION));
        var plan = new FlightPlan(List.of(new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, actions)), List.of());
        var issues = NavigationComputerRules.issues(plan, rocket, new RocketFlightPathCalculator.FlightPath(List.of(), List.of(), 0));
        assertTrue(issues.stream().anyMatch(issue -> issue.actionId().equals(split.id()) && issue.required() == 4 && issue.available() == 3));
        assertTrue(issues.stream().noneMatch(issue -> issue.actionId().equals(FlightPlanAction.NO_TARGET)));
    }
}
