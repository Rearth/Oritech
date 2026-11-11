package rearth.oritech.spaceage.simulation.mission;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ArrivalVelocityMode;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketDetonationTest {

    private static MissionState mission(double altitude, double speed, boolean altitudeAbort) {

        var id = UUID.randomUUID();
        var structure = new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO,
                SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(), 5, 1, 0, 200_000);
        var rocket = new ActiveRocketData(Map.of(id, structure), Map.of(id, new DynamicRocketSegment(200_000, 0, 0, Set.of())));
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.SURFACE)
                .withVelocity(ArrivalVelocityMode.MAXIMUM, 0).withLanding(1_000, 0, 0, 0);
        if (altitudeAbort) action = action.withAddons(List.of(ActionAddon.create(ActionAddonType.ALTITUDE_BELOW)));
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(action, FlightPlanAction.create(ActionType.EXPLODE)))));
        var position = new MissionState.Position(SpaceObjects.EARTH_SURFACE_X - altitude, 0, speed, 0, SpaceObjects.EARTH_ID,
                OrbitBand.LOW, -1, 1, FlightPlanAction.NO_TARGET, SegmentRef.of(structure));
        return new MissionState(UUID.randomUUID(), rocket, plan, position, new SurveyKnowledge(), BlockPos.ZERO);
    }

    @Test
    void altitudeAbortDoesNotDestroyTheCraftUntilTheSeparateExplodeCard() {

        var system = new SpaceSimulation();
        for (var speed : new double[]{0, 10, 1_000}) {
            var mission = mission(4_000, speed, true);
            var preview = RocketFlightPathCalculator.calculateFrom(mission.rocket, system.createObjectData(), mission.plan, mission.position);
            var path = preview.paths().getFirst();
            assertEquals(TerminalState.EXPLODED, path.terminalState());
            assertEquals(0, path.durationSeconds());
            assertEquals(2, path.actionMoments().size());
            assertEquals(1, preview.navigationAborts().size());
            MissionController.step(null, new MissionSavedData(), mission, system, 0);
            assertFalse(mission.ended);
            assertEquals(ActionType.EXPLODE, mission.action().type());
            assertEquals(1, mission.completed.size());
            MissionController.step(null, new MissionSavedData(), mission, system, 1);
            assertTrue(mission.ended);
            assertEquals(MissionState.STATUS_EXPLODED, mission.status);
            assertEquals(2, mission.completed.size());
        }
    }

    @Test
    void reentryDetonatesAtTheForecastAltitudeAndReloadDoesNotContinueTheBranch() {

        var system = new SpaceSimulation();
        var mission = mission(20_000, 100, true);
        var preview = RocketFlightPathCalculator.calculateFrom(mission.rocket, system.createObjectData(), mission.plan, mission.position);
        assertEquals(TerminalState.EXPLODED, preview.paths().getFirst().terminalState());
        var abort = preview.navigationAborts().getFirst();
        assertEquals(5_000, SpaceObjects.earthAltitude(abort.x(), abort.y()), .001);
        assertTrue(abort.timeSeconds() > 0);
        var data = new MissionSavedData();
        for (int tick = 0; tick < 20_000 && !mission.ended; tick++) {
            MissionController.step(null, data, mission, system, tick);
            if (tick == 5)
                mission = MissionState.CODEC.parse(JsonOps.INSTANCE, MissionState.CODEC.encodeStart(JsonOps.INSTANCE, mission).getOrThrow()).getOrThrow();
        }
        assertTrue(mission.ended, mission.status);
        assertEquals(5_000, SpaceObjects.earthAltitude(mission.position.x(), mission.position.y()), .001);
        assertEquals(abort.x(), mission.position.x(), .001);
        assertEquals(abort.y(), mission.position.y(), .001);
        var saved = MissionState.CODEC.parse(JsonOps.INSTANCE, MissionState.CODEC.encodeStart(JsonOps.INSTANCE, mission).getOrThrow()).getOrThrow();
        assertTrue(saved.ended);
        assertEquals(MissionState.STATUS_EXPLODED, saved.status);
        assertEquals(2, saved.completed.size());
    }

    @Test
    void altitudeAbortAtTheDestinationKeepsTheCraftAndContinuesThePlan() {

        var mission = mission(6_000, 100, true);
        var action = mission.action().withOrbit(OrbitBand.TIGHT)
                .withAddons(List.of(ActionAddon.create(ActionAddonType.ALTITUDE_BELOW).withValue(1_000)));
        mission.plan = mission.plan.withBranches(List.of(mission.plan.root().withActions(List.of(action))));
        var system = new SpaceSimulation();
        var preview = RocketFlightPathCalculator.calculateFrom(mission.rocket, system.createObjectData(), mission.plan, mission.position);
        assertEquals(TerminalState.READY, preview.paths().getFirst().terminalState());
        assertEquals(1, preview.navigationAborts().size());
        assertEquals(1_000, preview.navigationAborts().getFirst().actualValue(), .001);
        assertTrue(preview.explosions().isEmpty());
    }

    @Test
    void aboveAltitudeStopsAnAscentWithoutDestroyingTheCraft() {

        var mission = mission(1_000, -100, true);
        var action = mission.action().withOrbit(OrbitBand.LOW)
                .withAddons(List.of(ActionAddon.create(ActionAddonType.ALTITUDE_ABOVE).withValue(5_000)));
        mission.plan = mission.plan.withBranches(List.of(mission.plan.root().withActions(List.of(action))));
        var system = new SpaceSimulation();
        var preview = RocketFlightPathCalculator.calculateFrom(mission.rocket, system.createObjectData(), mission.plan, mission.position);
        assertEquals(TerminalState.READY, preview.paths().getFirst().terminalState());
        assertEquals(5_000, preview.navigationAborts().getFirst().actualValue(), .001);
        for (int tick = 0; tick < 20_000 && mission.action() != null; tick++)
            MissionController.step(null, new MissionSavedData(), mission, system, tick);
        assertFalse(mission.ended);
        assertEquals(MissionState.STATUS_READY, mission.status);
        assertEquals(5_000, SpaceObjects.earthAltitude(mission.position.x(), mission.position.y()), .001);
    }

    @Test
    void ordinaryNavigationAbortStillContinuesWithoutDestroyingTheCraft() {

        var mission = mission(20_000, 100, false);
        var action = mission.action().withAddons(List.of(ActionAddon.create(ActionAddonType.DISTANCE_FROM_TARGET).withValue(30_000)));
        mission.plan = mission.plan.withBranches(List.of(mission.plan.root().withActions(List.of(action))));
        var system = new SpaceSimulation();
        MissionNavigation.tick(null, new MissionSavedData(), mission, system, action, 0);
        assertFalse(mission.ended);
        assertEquals(MissionState.STATUS_READY, mission.status);
        assertEquals(1, mission.completed.size());
    }
}
