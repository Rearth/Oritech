package rearth.oritech.spaceage.simulation.mission;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ServiceSettings;
import rearth.oritech.spaceage.simulation.flight.LaunchSpeedRules;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.SpaceBalance.OrbitSlot;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlannerSnapshot;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RampGuidanceTest {

    @Test
    void acceptedRampTransfersFinishWithTheSameResourcesAsTheirForecasts() {

        var owner = new UUID(102, 1);
        var system = new SpaceSimulation();
        for (var rf : new long[]{100_000, 1_000_000, 20_000_000}) {
            for (var slot = -1; slot < 6; slot++) {
                var id = new UUID(103, 1);
                var segment = new StaticRocketSegment(id, Set.of(
                        new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()),
                        new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState())),
                        Map.of(), 5, 1, rf, 0);
                var craft = new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(0, rf, 0, Set.of())));
                craft.setLaunchPosition(new BlockPos(15, 65, 7));
                var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.LOW)
                        .withService(new ServiceSettings(0, 0, slot));
                var plan = FlightPlan.empty();
                plan = plan.withBranches(List.of(plan.root().withActions(List.of(action))));
                var start = LaunchSpeedRules.position(system.createObjectData(), craft.getLaunchPosition(), 10_000);
                var forecast = RocketFlightPathCalculator.calculateFrom(craft, system.createObjectData(), plan, start).paths().getFirst();
                if (forecast.terminalState().preventsLaunch()) continue;
                var state = new MissionState(owner, craft, plan, start, system.earthKnowledge.copy(), craft.getLaunchPosition());
                var data = new MissionSavedData();
                data.craft.put(craft.getRocketId(), state);
                var deadline = Math.ceil(forecast.durationSeconds() * 20) + 10;
                for (var tick = 0; tick < deadline && state.action() != null; tick++) {
                    MissionController.step(null, data, state, system, tick);
                    assertFalse(state.navigationBlocked(), "rf=" + rf + " slot=" + slot + " tick=" + tick + " status=" + state.status);
                }
                assertNull(state.action(), state.status);
            }
        }
    }

    @Test
    void previewAndLaunchResolveAutoAndOccupiedRequestsToTheSameFreeSlot() {

        var owner = new UUID(104, 1);
        var system = new SpaceSimulation();
        var rocket = ionSatellite();
        var data = new MissionSavedData();
        for (var slot = 0; slot < 4; slot++) {
            var occupied = MissionState.copyRocket(rocket);
            occupied = new ActiveRocketData(UUID.randomUUID(), occupied.getOriginalSegments(), occupied.getDynamicSegments(), null);
            var position = MissionState.Position.surface().placed(-3_000_000, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.LOW, slot);
            data.craft.put(occupied.getRocketId(), new MissionState(owner, occupied, FlightPlan.empty(), position,
                    system.earthKnowledge.copy(), BlockPos.ZERO));
        }
        for (var requested : new int[]{-1, 2, 5}) {
            var action = navigation(requested);
            var plan = program(action);
            var snapshot = new FlightPlannerSnapshot(system.id(), rocket.getRocketId(), system.createObjectData(), plan)
                    .withOrbitSlots(MissionController.orbitSlots(data, owner, rocket.getRocketId()));
            var resolved = snapshot.resolvedPlan(plan);
            assertEquals(requested == 5 ? 5 : 4, resolved.currentAction().service().slot());
            assertEquals(MissionController.resolveOrbitSlots(plan, data, owner, rocket.getRocketId()), resolved);
            assertEquals(requested, plan.currentAction().service().slot(), "Preview resolution leaves the editable Auto setting intact");
        }
    }

    @Test
    void slotAllocationMustHappenBeforeCheckingRampFlightFeasibility() {

        var system = new SpaceSimulation();
        var rocket = ionSatellite();
        rocket.getDynamicSegments().values().forEach(resources -> resources.setResources(0, 20_000));
        var plan = program(navigation(-1));
        var snapshot = new FlightPlannerSnapshot(system.id(), rocket.getRocketId(), system.createObjectData(), plan)
                .withOrbitSlots(List.of(new OrbitSlot(SpaceObjects.EARTH_ID, OrbitBand.LOW, 0),
                        new OrbitSlot(SpaceObjects.EARTH_ID, OrbitBand.LOW, 1),
                        new OrbitSlot(SpaceObjects.EARTH_ID, OrbitBand.LOW, 2),
                        new OrbitSlot(SpaceObjects.EARTH_ID, OrbitBand.LOW, 3)));
        assertTrue(LaunchSpeedRules.valid(plan, rocket, snapshot.objects(), 7_000, List.of(), List.of()),
                "The nearest Auto destination is flyable before resolving slots");
        var resolved = snapshot.resolvedPlan(plan);
        assertEquals(4, resolved.currentAction().service().slot());
        assertFalse(LaunchSpeedRules.valid(resolved, rocket, snapshot.objects(), 7_000, List.of(), List.of()),
                "This actual available slot is not flyable; the ramp must use its fallback instead of starting a blocked mission");
    }

    private static ActiveRocketData ionSatellite() {

        var id = new UUID(103, 1);
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState())),
                Map.of(), 5, 1, 100_000, 0);
        var rocket = new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(0, 100_000, 0, Set.of())));
        rocket.setLaunchPosition(new BlockPos(15, 65, 7));
        return rocket;
    }

    private static FlightPlanAction navigation(int slot) {

        return FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.LOW)
                .withService(new ServiceSettings(0, 0, slot));
    }

    private static FlightPlan program(FlightPlanAction action) {

        var plan = FlightPlan.empty();
        return plan.withBranches(List.of(plan.root().withActions(List.of(action))));
    }

}
