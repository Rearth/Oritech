package rearth.oritech.spaceage.simulation.mission;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.ResourceBodies.Definition;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.FlightResourceRate;
import rearth.oritech.spaceage.simulation.flight.NavigationWaypoint;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.BoosterEvent;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.ArrivalPrediction;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.FlightPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationEventsTest {

    private static final UUID CORE = new UUID(701, 1);
    private static final UUID BOOSTER = new UUID(701, 2);
    private static final SegmentRef CORE_REF = new SegmentRef(BlockPos.ZERO);
    private static final SegmentRef BOOSTER_REF = new SegmentRef(BlockPos.ZERO.above());

    private static StaticRocketSegment segment(UUID id, BlockPos position) {

        return new StaticRocketSegment(id, Set.of(new StaticRocketSegment.BlockData(position,
                SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState())), Map.of(), 5, 1);
    }

    private static MissionState mission(ActiveRocketData rocket, FlightPlanAction action) {

        var base = FlightPlan.empty();
        var plan = base.withBranches(List.of(base.root().withActions(List.of(action))));
        var position = new MissionState.Position(-3_120_000, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.HIGH,
                -1, 1, FlightPlanAction.NO_TARGET, CORE_REF);
        return new MissionState(new UUID(702, 1), rocket, plan, position, new SurveyKnowledge(), BlockPos.ZERO);
    }

    private static ActiveRocketData rocket(long rf) {

        return new ActiveRocketData(Map.of(CORE, segment(CORE, BlockPos.ZERO)),
                Map.of(CORE, new DynamicRocketSegment(0, rf, 0, Set.of())));
    }

    private static PathSample sample(MissionState state, double time, int stage, Set<SegmentRef> segments, double rfRate) {

        return new PathSample(time, -3_120_000 + time * 100, 0, 100, 100, 0, PathPhase.COAST,
                state.action().targetId(), state.action().id(), stage, segments, Set.of(), FlightPlanAction.NO_TARGET,
                Map.of(CORE_REF, new FlightResourceRate(0, rfRate)));
    }

    private static void guidance(MissionState state, List<PathSample> samples, List<BoosterEvent> events) {

        state.transfer = new ActiveTransfer(state.telemetry(0), List.of(), List.of());
        state.transfer.path = new FlightPath(List.of(new CraftPath(state.plan.root().id(), Set.of(CORE_REF), samples,
                List.of(), .1, 0, TerminalState.READY)), events, .1);
    }

    @Test
    void shortageAfterAnEventKeepsTheMotionAlreadyPaidFor() {

        var state = mission(rocket(30), FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID));
        guidance(state, List.of(sample(state, 0, 1, Set.of(CORE_REF), 0),
                sample(state, .02, 1, Set.of(CORE_REF), 1000),
                sample(state, .1, 1, Set.of(CORE_REF), 1000)), List.of());

        MissionNavigation.tick(null, new MissionSavedData(), state, new SpaceSimulation(), state.action(), 0);

        assertEquals(MissionState.STATUS_REPLANNING, state.status);
        assertNull(state.transfer);
        assertEquals(-3_119_998, state.position.x(), 1e-6);
        assertEquals(10, state.rocket.getDynamicSegments().get(CORE).rf(), 1e-6);
    }

    @Test
    void boosterSeparatesAtItsEventPositionAndKeepsItsStage() {

        var rocket = new ActiveRocketData(Map.of(CORE, segment(CORE, BlockPos.ZERO), BOOSTER, segment(BOOSTER, BlockPos.ZERO.above())),
                Map.of(CORE, new DynamicRocketSegment(0, 1000, 0, Set.of(BOOSTER)),
                        BOOSTER, new DynamicRocketSegment(0, 0, 0, Set.of(CORE))));
        var state = mission(rocket, FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID));
        var event = new BoosterEvent(new UUID(703, 1), state.plan.root().id(), new UUID(703, 2), state.action().id(),
                BOOSTER_REF, 1, .02, -3_119_998, 0);
        guidance(state, List.of(sample(state, 0, 1, Set.of(CORE_REF, BOOSTER_REF), 0),
                sample(state, .02, 1, Set.of(CORE_REF, BOOSTER_REF), 0),
                sample(state, .02, 2, Set.of(CORE_REF), 0), sample(state, .1, 2, Set.of(CORE_REF), 0)), List.of(event));
        var data = new MissionSavedData();
        data.craft.put(rocket.getRocketId(), state);

        MissionNavigation.tick(null, data, state, new SpaceSimulation(), state.action(), 0);

        var child = data.craft.values().stream().filter(c -> c != state).findFirst().orElseThrow();
        assertEquals(-3_119_998, child.position.x(), 1e-6);
        assertEquals(1, child.position.stage());
        assertEquals(-3_119_995, state.position.x(), 1e-6);
        assertEquals(2, state.position.stage());
        assertFalse(state.rocket.getStaticSegments().containsKey(BOOSTER));
    }

    @Test
    void earlyCompletionDoesNotClaimOrSnapToAnOrbitSlot() {

        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.LOW)
                .withAddons(List.of(ActionAddon.create(ActionAddonType.DISTANCE_FROM_TARGET).withValue(1_000_000)));
        var state = mission(rocket(20_000_000), action);
        var before = state.position;
        var system = new SpaceSimulation();
        var forecast = RocketFlightPathCalculator.calculateFrom(state.rocket, system.createObjectData(), state.plan, before);
        assertEquals(1, forecast.navigationAborts().size());

        MissionNavigation.tick(null, new MissionSavedData(), state, system, action, 0);

        assertNull(state.action());
        assertEquals(before.x(), state.position.x(), 1e-6);
        assertEquals(before.y(), state.position.y(), 1e-6);
        assertEquals(OrbitBand.HIGH, state.position.orbit());
        assertEquals(-1, state.position.slot());
        assertEquals(MissionState.STATUS_READY, state.status);
    }

    @Test
    void editingEngineStagesRestartsGuidanceEvenWhenTheNavigationCardIsUnchanged() {

        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withWaypoint(new NavigationWaypoint(-3_121_000, 2000));
        var state = mission(rocket(20_000_000), action);
        var system = new SpaceSimulation();
        MissionNavigation.tick(null, new MissionSavedData(), state, system, action, 0);
        assertNotNull(state.transfer);
        state.connected = true;
        var configuration = state.plan.configurationFor(CORE_REF).withEngineStages(List.of(2));

        assertTrue(MissionController.replace(state, state.plan.withSegmentConfigurations(List.of(configuration)), system));
        assertNull(state.transfer);
        assertEquals(0, state.actionTicks());
    }

    @Test
    void forecastKeepsTheTransferStartStagingAfterSeparationAndReload() {

        var rocket = new ActiveRocketData(Map.of(CORE, segment(CORE, BlockPos.ZERO), BOOSTER, segment(BOOSTER, BlockPos.ZERO.above())),
                Map.of(CORE, new DynamicRocketSegment(0, 20_000_000, 0, Set.of(BOOSTER)),
                        BOOSTER, new DynamicRocketSegment(0, 1, 0, Set.of(CORE))));
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withWaypoint(new NavigationWaypoint(-3_121_000, 2000));
        var state = mission(rocket, action);
        state.plan = state.plan.withSegmentConfigurations(List.of(
                state.plan.configurationFor(CORE_REF).withEngineStages(List.of(2)),
                state.plan.configurationFor(BOOSTER_REF).withBooster(true)));
        var system = new SpaceSimulation();
        MissionNavigation.tick(null, new MissionSavedData(), state, system, action, 0);
        assertEquals(Set.of(CORE), state.rocket.getStaticSegments().keySet());
        assertNotNull(state.transfer);
        var expected = state.transfer.path.paths().getFirst().samples();
        assertEquals(expected, state.navigationForecast().paths().getFirst().samples());

        var loaded = MissionState.CODEC.parse(NbtOps.INSTANCE, MissionState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow()).getOrThrow();
        assertEquals(expected, loaded.navigationForecast().paths().getFirst().samples());
    }

    @Test
    void anImpactDoesNotExecuteADetachedBranchesPredictedImpact() {

        var system = new SpaceSimulation();
        var definition = new Definition("Parent deposit", "", -3_130_000, 0, 2000, Identifier.parse("oritech_space_age:luna"),
                List.of(new SpaceObjects.AsteroidMaterial(Identifier.parse("minecraft:iron_ore"), 1)));
        var parentTarget = definition.object(Identifier.parse("oritech_space_age:parent"));
        var childTarget = new Definition("Child deposit", "", -3_200_000, 0, 2000, definition.modelBlock(), definition.resources())
                .object(Identifier.parse("oritech_space_age:child"));
        system.setResourceBodies(List.of(parentTarget, childTarget));
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(parentTarget.id()).withOrbit(OrbitBand.SURFACE)
                .withVelocity(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0);
        var state = mission(rocket(0), action);
        state.position = state.position.placed(state.position.x(), 0, -500, 0, SpaceObjects.EARTH_ID, OrbitBand.HIGH, -1);
        var data = new MissionSavedData();
        MissionNavigation.tick(null, data, state, system, action, 0);
        var flight = state.transfer.path;
        var parent = flight.arrivalPredictions().getFirst();
        var child = new ArrivalPrediction(new UUID(730, 1), new UUID(730, 2), childTarget.id(), parent.impact(), parent.parachutes());
        state.transfer.path = new FlightPath(flight.paths(), flight.boosterEvents(), flight.lastCommandSeconds(),
                List.of(child, parent), flight.asteroidPaths(), flight.navigationAborts(), flight.scanEstimates(),
                flight.stationKeepingEstimates(), flight.processingEstimates());
        state.transfer.ticks = (long) Math.ceil(flight.paths().getFirst().durationSeconds() * 20);

        MissionNavigation.tick(null, data, state, system, action, state.transfer.ticks);

        assertTrue(state.ended);
        assertTrue(system.truth().stream().anyMatch(object -> object.name().startsWith("Parent deposit impact fragment")));
        assertFalse(system.truth().stream().anyMatch(object -> object.name().startsWith("Child deposit impact fragment")));
    }
}
