package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.mission.MissionController;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.mission.RocketDocking;
import rearth.oritech.spaceage.simulation.service.RocketProcessingService;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings.Processing;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings.SourceScope;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentConfiguration;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.CubeSatData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketPayloads;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeSatDeployTest {

    private static final BlockPos PAYLOAD = new BlockPos(0, 1, 0);
    private static final List<SpaceObjectData> OBJECTS = List.of(new SpaceObjectData(SpaceObjects.EARTH_ID, SpaceObjects.ObjectType.EARTH,
            0, 0, 0, 0, SpaceObjects.DetectionState.PRECISE));

    @BeforeAll
    static void bootstrap() {

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static ActiveRocketData ionRocket(boolean carriesCube) {

        var id = UUID.randomUUID();
        var blocks = new HashSet<StaticRocketSegment.BlockData>();
        blocks.add(new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()));
        if (carriesCube) blocks.add(new StaticRocketSegment.BlockData(PAYLOAD, SpaceAgeBlocks.CUBE_SAT.get().defaultBlockState()));
        var segment = new StaticRocketSegment(id, blocks, Map.of(), 25, 1);
        var resources = new DynamicRocketSegment(0, 2_000_000, 0, Set.of());
        if (carriesCube) resources.payloads.put(PAYLOAD, CubeSatData.of(ionRocket(false)));
        return new ActiveRocketData(Map.of(id, segment), Map.of(id, resources));
    }

    @Test
    void deployedCubeStartsWhereTheHostIsAndFliesItsOwnBranch() {

        var host = ionRocket(true);
        var orbit = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.LOW);
        var deploy = FlightPlanAction.create(ActionType.DEPLOY_CUBESAT).withSegments(List.of(new SegmentRef(PAYLOAD)));
        var cubeTransfer = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH);
        var root = new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(orbit, deploy));
        var cubeBranch = new FlightPlanBranch(UUID.randomUUID(), deploy.id(), List.of(cubeTransfer));
        var cubeRef = RocketPayloads.segments(PAYLOAD, RocketPayloads.all(host).get(PAYLOAD)).iterator().next();
        var plan = new FlightPlan(List.of(root, cubeBranch), List.of(new SegmentConfiguration(cubeRef, "Probe", false, List.of(1))));

        var validation = RocketFlightPlanRules.inspect(plan, host, OBJECTS, true, null);
        assertTrue(validation.valid(), () -> "plan issues: " + validation.issues());
        assertEquals(2, validation.plan().branches().size());
        assertEquals("Probe", validation.plan().configurationFor(cubeRef).name(), "cube segment settings survive validation");

        var flight = RocketFlightPathCalculator.calculate(host, OBJECTS, plan);
        var hostPath = flight.paths().stream().filter(path -> path.branchId().equals(root.id())).findFirst().orElseThrow();
        var cubePath = flight.paths().stream().filter(path -> path.branchId().equals(cubeBranch.id())).findFirst().orElseThrow();
        assertFalse(hostPath.terminalState().isFailure(), hostPath.terminalState().name());
        assertFalse(cubePath.terminalState().isFailure(), cubePath.terminalState().name());
        var deployed = hostPath.actionMoments().stream().filter(moment -> moment.actionId().equals(deploy.id())).findFirst().orElseThrow();
        var start = cubePath.samples().getFirst();
        assertEquals(deployed.x(), start.x(), 1e-6);
        assertEquals(deployed.y(), start.y(), 1e-6);
        assertEquals(Set.of(cubeRef), start.connectedSegments());
        assertTrue(hostPath.segments().stream().noneMatch(start.connectedSegments()::contains));
        assertTrue(host.getDynamicSegments().values().iterator().next().payloads.containsKey(PAYLOAD), "previews never change the real rocket");
    }

    @Test
    void deployNeedsACarriedCube() {

        var deploy = FlightPlanAction.create(ActionType.DEPLOY_CUBESAT).withSegments(List.of(new SegmentRef(new BlockPos(5, 5, 5))));
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(deploy))));
        var host = ionRocket(true);
        assertFalse(RocketFlightPlanRules.inspect(plan, host, OBJECTS, true, null).valid(), "unknown payload cells are rejected");
        assertNotNull(RocketPayloads.owner(host, RocketPayloads.segments(host).iterator().next()));
    }

    @Test
    void deployedServicesUseCubeHardwareAndLeaveHostHardwareSeparate() {

        var host = ionRocket(true);
        var cubeId = UUID.randomUUID();
        var blocks = Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.SPACE_SCANNER.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.VACUUM_CRAFTER.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(2), SpaceAgeBlocks.ROCKET_COUPLING.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(3), SpaceAgeBlocks.NAVIGATION_COMPUTER.get().defaultBlockState()));
        var cubeRocket = new ActiveRocketData(Map.of(cubeId, new StaticRocketSegment(cubeId, blocks, Map.of(), 4, 0)),
                Map.of(cubeId, new DynamicRocketSegment(0, 20_000, 0, Set.of())));
        host.getDynamicSegments().values().iterator().next().payloads.put(PAYLOAD, CubeSatData.of(cubeRocket));
        var deploy = FlightPlanAction.create(ActionType.DEPLOY_CUBESAT).withSegments(List.of(new SegmentRef(PAYLOAD)));
        var module = RocketProcessingService.modules(RocketPayloads.unfold(PAYLOAD, RocketPayloads.all(host).get(PAYLOAD))).getFirst();
        var process = FlightPlanAction.create(ActionType.PROCESS).withSettings(RocketServiceSettings.DEFAULT.withProcessing(
                new Processing(List.of(module), SourceScope.BOTH, FlightPlanAction.NO_TARGET)));
        var root = new FlightPlanBranch(UUID.randomUUID(), FlightPlanBranch.NO_PARENT, List.of(deploy));
        var child = new FlightPlanBranch(UUID.randomUUID(), deploy.id(), List.of(process));
        var plan = new FlightPlan(List.of(root, child), List.of());
        var forecast = RocketFlightPathCalculator.calculate(host, OBJECTS, plan);
        var carried = FlightPlanHardware.before(plan, root, deploy.id(), host, forecast);
        var deployed = FlightPlanHardware.before(plan, child, process.id(), host, forecast);
        assertTrue(RocketProcessingService.modules(carried).isEmpty());
        assertTrue(RocketDocking.ports(carried).isEmpty());
        assertEquals(0, NavigationComputerRules.computers(carried, NavigationComputerRules.allSegments(carried)));
        assertEquals(List.of(module), RocketProcessingService.modules(deployed));
        assertEquals(1, RocketDocking.ports(deployed).size());
        assertEquals(1, NavigationComputerRules.computers(deployed, NavigationComputerRules.allSegments(deployed)));
        assertTrue(RocketFlightPlanRules.inspect(plan, host, OBJECTS, true, null).valid(), "child processing modules survive validation");
        assertTrue(RocketPayloads.all(FlightPlanHardware.before(plan, root, null, host, forecast)).isEmpty());
        assertTrue(RocketPayloads.all(host).containsKey(PAYLOAD), "hardware lookup never consumes real payloads");
    }

    @Test
    void theSamePayloadCannotBeDeployedTwice() {

        var first = FlightPlanAction.create(ActionType.DEPLOY_CUBESAT).withSegments(List.of(new SegmentRef(PAYLOAD)));
        var second = FlightPlanAction.create(ActionType.DEPLOY_CUBESAT).withSegments(List.of(new SegmentRef(PAYLOAD)));
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(first, second))));
        var validation = RocketFlightPlanRules.inspect(plan, ionRocket(true), OBJECTS, true, null);
        assertFalse(validation.valid());
        assertTrue(validation.issues().stream().anyMatch(issue -> issue.actionId().equals(second.id()) && issue.code().equals("navigation_no_payload")));
    }

    @Test
    void liveDeploymentStopsAtAMissingPayload() {

        var deploy = FlightPlanAction.create(ActionType.DEPLOY_CUBESAT).withSegments(List.of(new SegmentRef(PAYLOAD)));
        var plan = FlightPlan.empty();
        plan = plan.withBranches(List.of(plan.root().withActions(List.of(deploy))));
        var system = new SpaceSimulation();
        var position = new MissionState.Position(0, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.LOW, -1, 1,
                FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var state = new MissionState(UUID.randomUUID(), ionRocket(false), plan, position, system.earthKnowledge.copy(), BlockPos.ZERO);
        var data = new MissionSavedData();
        MissionController.step(null, data, state, system, 1);
        assertEquals("status.oritech_space_age.no_payload", state.status);
        assertEquals(deploy.id(), state.plan.root().actions().getFirst().id());
        assertTrue(data.craft.isEmpty());
        assertTrue(state.completed.isEmpty());
    }

}
