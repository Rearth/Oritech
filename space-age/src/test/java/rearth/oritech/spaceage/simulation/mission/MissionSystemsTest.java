package rearth.oritech.spaceage.simulation.mission;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;
import rearth.oritech.api.networking.ReflectiveCodecBuilder;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeComponents.MissionCard;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.AsteroidTarget;
import rearth.oritech.spaceage.simulation.ResourceBodies.Definition;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.NavigationWaypoint;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;
import rearth.oritech.spaceage.simulation.rocket.RocketRecovery;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings.HardwareRef;
import rearth.oritech.spaceage.simulation.survey.SpaceCommunications;
import rearth.oritech.spaceage.simulation.survey.SurveyKnowledge;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionSystemsTest {

    private static final UUID OWNER = new UUID(1, 1);

    private static ActiveRocketData rocket(boolean ion, long fuel, long rf) {

        var id = UUID.randomUUID();
        var block = ion ? SpaceAgeBlocks.ION_BOOSTER_ROCKET : SpaceAgeBlocks.BASIC_BOOSTER_ROCKET;
        var segment = new StaticRocketSegment(id,
                Set.of(new StaticRocketSegment.BlockData(BlockPos.ZERO, block.get().defaultBlockState())),
                Map.of(), 5, 1, rf, fuel);
        return new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(fuel, rf, 0, Set.of())));
    }

    private static FlightPlan plan(FlightPlanAction... actions) {

        var plan = FlightPlan.empty();
        return plan.withBranches(List.of(plan.root().withActions(List.of(actions))));
    }

    private static ActiveRocketData stationKeepingRocket(int engines) {

        var id = UUID.randomUUID();
        var blocks = IntStream.range(0, engines)
                .mapToObj(x -> new StaticRocketSegment.BlockData(new BlockPos(x, 0, 0),
                        SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()))
                .collect(Collectors.toSet());
        var segment = new StaticRocketSegment(id, blocks, Map.of(), 25, engines, 0, 20_000);
        return new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(20_000, 0, 1_000, Set.of())));
    }

    @Test
    void fixedBodyCollisionMatchesForecastAfterReloadAndProducesTuggableAsteroids() {

        var system = new SpaceSimulation();
        var definition = new Definition("Test moon", "A fixed deposit", -3_130_000, 0, 2_000,
                Identifier.parse("oritech_space_age:luna"), List.of(
                new SpaceObjects.AsteroidMaterial(Identifier.parse("minecraft:iron_ore"), 3),
                new SpaceObjects.AsteroidMaterial(Identifier.parse("minecraft:copper_ore"), 1)));
        var body = definition.object(Identifier.parse("oritech_space_age:test_moon"));
        system.setResourceBodies(List.of(body));
        var craft = rocket(false, 0, 0);
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(body.id()).withOrbit(OrbitBand.SURFACE)
                .withVelocity(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0);
        var program = plan(action);
        var position = new MissionState.Position(-3_120_000, 0, -500, 0, SpaceObjects.EARTH_ID, OrbitBand.HIGH, -1, 1,
                FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var forecast = RocketFlightPathCalculator.calculateFrom(craft, system.createObjectData(), program, position);
        assertEquals(RocketFlightPathCalculator.TerminalState.DESTROYED, forecast.paths().getFirst().terminalState());
        var impact = forecast.arrivalPredictions().getFirst().impact();
        var mission = new MissionState(OWNER, craft, program, position, new SurveyKnowledge(), BlockPos.ZERO);
        var data = new MissionSavedData();
        MissionNavigation.tick(null, data, mission, system, action, 0);
        mission = MissionState.CODEC.parse(NbtOps.INSTANCE,
                MissionState.CODEC.encodeStart(NbtOps.INSTANCE, mission).getOrThrow()).getOrThrow();
        for (int tick = 1; tick < 10_000 && !mission.ended; tick++)
            MissionNavigation.tick(null, data, mission, system, mission.action(), tick);
        assertTrue(mission.ended, mission.status);
        assertEquals(body, system.truth().stream().filter(object -> object.id().equals(body.id())).findFirst().orElseThrow());
        var chunks = system.truth().stream().filter(object -> object.name().startsWith("Test moon impact fragment")).toList();
        assertEquals(impact.fragmentCount(), chunks.size());
        assertEquals(impact.fragments().stream().mapToDouble(AsteroidImpactRules.AsteroidFragment::mass).sum(),
                chunks.stream().mapToDouble(SpaceObjectData::mass).sum(), .001);
        assertTrue(chunks.stream().allMatch(object -> object.type() == SpaceObjects.ObjectType.ASTEROID
                && object.surfaceGravity() == 0 && !object.materials().isEmpty()));

        var attach = FlightPlanAction.create(ActionType.CONNECT_ASTEROID).withTarget(body.id())
                .withSegments(List.of(SegmentRef.of(craft.getStaticSegments().values().iterator().next())));
        var atBody = position.placed(body.x() + body.radius(), body.y(), 0, 0, body.id(), OrbitBand.SURFACE, -1);
        var invalid = RocketFlightPathCalculator.calculateFrom(craft, system.createObjectData(), plan(attach), atBody);
        assertEquals(RocketFlightPathCalculator.TerminalState.TARGET_UNAVAILABLE, invalid.paths().getFirst().terminalState());
    }

    @Test
    void coordinateWaypointsSurviveSavingAndLiveNavigationMatchesThePreview() {

        var waypoint = new NavigationWaypoint(-3_121_000, 2_000);
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withWaypoint(waypoint);
        var program = plan(action);
        var codec = ReflectiveCodecBuilder.create(FlightPlan.class);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        codec.encode(buffer, program);
        assertEquals(program, codec.decode(buffer));
        buffer.release();
        var system = new SpaceSimulation();
        var craft = rocket(false, 100_000, 0);
        var position = new MissionState.Position(-3_120_000, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.HIGH, -1, 1,
                FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var validation = RocketFlightPlanRules.inspect(program, craft, system.createObjectData(), false, position);
        assertTrue(validation.valid(), () -> validation.issues().toString());
        var preview = RocketFlightPathCalculator.calculateFrom(craft, system.createObjectData(), program, position);
        var path = preview.paths().getFirst();
        assertFalse(path.terminalState().isFailure(), path.terminalState().name());
        var arrival = path.samples().getLast();
        assertEquals(waypoint.x(), arrival.x(), .01);
        assertEquals(waypoint.y(), arrival.y(), .01);
        assertEquals(0, arrival.speedMetersPerSecond(), 1e-6);

        var mission = new MissionState(OWNER, craft, program, position, new SurveyKnowledge(), BlockPos.ZERO);
        var data = new MissionSavedData();
        MissionNavigation.tick(null, data, mission, system, action, 0);
        var saved = MissionState.CODEC.encodeStart(NbtOps.INSTANCE, mission).getOrThrow();
        mission = MissionState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(waypoint, mission.plan.root().actions().getFirst().settings().waypoint().orElseThrow());
        for (int tick = 1; tick < 10_000 && mission.action() != null; tick++)
            MissionNavigation.tick(null, data, mission, system, mission.action(), tick);
        assertNull(mission.action(), mission.status);
        assertEquals(arrival.x(), mission.position.x(), .01);
        assertEquals(arrival.y(), mission.position.y(), .01);
        assertEquals(action.id(), mission.position.target());
        assertEquals(0, Math.hypot(mission.position.vx(), mission.position.vy()), 1e-6);
        assertTrue(action.withTarget(SpaceObjects.EARTH_ID).settings().waypoint().isEmpty());
    }

    @Test
    void coastingThroughAWaypointAtSpeedDoesNotCauseASurfaceImpact() {

        var point = new NavigationWaypoint(-3_122_000, 0);
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withWaypoint(point)
                .withVelocity(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0);
        var position = new MissionState.Position(-3_120_000, 0, -500, 0, SpaceObjects.EARTH_ID, OrbitBand.HIGH, -1, 1,
                FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var flight = RocketFlightPathCalculator.calculateFrom(rocket(false, 0, 0), new SpaceSimulation().createObjectData(), plan(action), position);
        var path = flight.paths().getFirst();
        assertFalse(path.terminalState().isFailure(), path.terminalState().name());
        assertEquals(point.x(), path.samples().getLast().x(), .01);
        assertEquals(500, path.samples().getLast().speedMetersPerSecond(), 1e-6);
        assertTrue(flight.arrivalPredictions().isEmpty(), "A coordinate has no surface to collide with");
    }

    @Test
    void engineTypesConsumeTheirOwnResourceAndIonIsWeakInAtmosphere() {

        assertEquals(0, RocketPerformanceCalculator.calculate(rocket(false, 0, 1_000_000))
                .availableDeltaVMetersPerSecond());
        assertEquals(0, RocketPerformanceCalculator.calculate(rocket(true, 1_000_000, 0))
                .availableDeltaVMetersPerSecond());

        var ion = RocketPerformanceCalculator.calculate(rocket(true, 0, 1_000_000));
        assertEquals(ion.thrustNewtons() / ion.wetMassKilograms() * SpaceBalance.ION_ATMOSPHERE,
                ion.liftoffAccelerationMetersPerSecondSquared(), 1e-8);

        var chemical = rocket(false, 200, 1_234);
        var segment = chemical.getStaticSegments().values().iterator().next();
        var resources = chemical.getDynamicSegments().get(segment.segmentId());
        RocketHardware.of(segment).burn(resources, 1);
        assertEquals(200 - SpaceBalance.CHEMICAL_FUEL_PER_SECOND, resources.fuel(), 1e-9);
        assertEquals(1_234, resources.availableRF);
    }

    @Test
    void surveyKnowledgeStaysPrivateUntilMerged() {

        var system = new SpaceSimulation();
        var target = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.ASTEROID)
                .findFirst().orElseThrow();
        assertTrue(system.createObjectData().stream().noneMatch(o -> o.type() == SpaceObjects.ObjectType.ASTEROID));

        var first = new SurveyKnowledge();
        first.scan(List.of(target), 0, target.x() + 100_000, target.y(), SpaceBalance.SCAN_RANGE);
        assertTrue(first.precise(target.id()));
        assertFalse(system.earthKnowledge.precise(target.id()));

        system.earthKnowledge.merge(first);
        assertTrue(system.earthKnowledge.precise(target.id()));
        system.earthKnowledge.merge(first.copy());
        assertEquals(1, system.earthKnowledge.contacts().size());

        var received = system.createObjectData().stream().filter(o -> o.id().equals(target.id()))
                .findFirst().orElseThrow();
        system.moveAttached(target.id(), target.x() + 1_000_000, target.y());
        assertEquals(received, system.createObjectData().stream().filter(o -> o.id().equals(target.id()))
                .findFirst().orElseThrow());
    }

    @Test
    void regionScanReachesTheWholeRegionFromInsideItsBoundary() {

        var region = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.SURVEY_REGION,
                0, 0, 310_000, 0, SpaceObjects.DetectionState.PRECISE);
        var asteroid = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID,
                -300_000, 0, 2_000, 0, SpaceObjects.DetectionState.HIDDEN);
        var knowledge = new SurveyKnowledge();
        var craftX = region.radius() * 0.72;

        knowledge.scan(List.of(asteroid), 0, craftX, 0, SpaceBalance.SCAN_RANGE);
        assertNull(knowledge.contact(asteroid.id()));

        knowledge.scanRegion(List.of(asteroid), region, 0, craftX, 0, SpaceBalance.SCAN_RANGE);
        assertTrue(knowledge.precise(asteroid.id()));
    }

    @Test
    void completedSurveyRegionsDisappearAfterTheirContactsReachEarth() {

        var system = new SpaceSimulation();
        var region = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.SURVEY_REGION)
                .findFirst().orElseThrow();
        var asteroids = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.ASTEROID
                && Math.hypot(o.x() - region.x(), o.y() - region.y()) <= region.radius()).toList();
        assertFalse(asteroids.isEmpty());
        assertTrue(system.createObjectData().stream().anyMatch(o -> o.id().equals(region.id())));

        var delivered = new SurveyKnowledge();
        for (var asteroid : asteroids)
            delivered.scan(List.of(asteroid), 0, asteroid.x(), asteroid.y(), SpaceBalance.SCAN_RANGE);
        system.earthKnowledge.merge(delivered);

        assertTrue(system.createObjectData().stream().noneMatch(o -> o.id().equals(region.id())));
        assertTrue(asteroids.stream().allMatch(asteroid -> system.createObjectData().stream()
                .anyMatch(known -> known.id().equals(asteroid.id()))));
    }

    @Test
    void catastrophicImpactReplacesAsteroidWithNamedLocalDebrisField() {

        var system = new SpaceSimulation();
        var asteroid = system.truth().stream().filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID)
                .findFirst().orElseThrow();
        system.earthKnowledge.scan(List.of(asteroid), 0, asteroid.x(), asteroid.y(), SpaceBalance.SCAN_RANGE);
        var oldRegions = system.truth().stream().filter(object -> object.type() == SpaceObjects.ObjectType.SURVEY_REGION
                        && Math.hypot(object.x() - asteroid.x(), object.y() - asteroid.y()) <= object.radius())
                .map(SpaceObjectData::id).toList();
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO)
                .withTarget(asteroid.id()).withOrbit(OrbitBand.SURFACE);
        var impact = AsteroidImpactRules.predictArrival(asteroid.mass() * 1_000,
                asteroid, 1_000, null, action);
        assertEquals(AsteroidImpactRules.FragmentationMode.CATASTROPHIC, impact.fragmentationMode());

        var debrisId = system.applyAsteroidImpact(asteroid.id(), impact);
        var truth = system.truth();
        var debris = truth.stream().filter(object -> object.id().equals(debrisId)).findFirst().orElseThrow();
        var fragments = truth.stream().filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID
                && object.name().contains("destroyed " + asteroid.name())).toList();

        assertTrue(truth.stream().noneMatch(object -> object.id().equals(asteroid.id())));
        assertFalse(system.earthKnowledge.precise(asteroid.id()));
        assertTrue(system.createObjectData().stream().noneMatch(object -> object.id().equals(asteroid.id())),
                "stale survey knowledge must not resurrect a destroyed asteroid");
        assertEquals(impact.fragmentCount(), fragments.size());
        assertTrue(fragments.stream().allMatch(fragment -> fragment.detectionState() == SpaceObjects.DetectionState.HIDDEN));
        assertEquals(asteroid.x(), debris.x());
        assertEquals(asteroid.y(), debris.y());
        assertTrue(debris.radius() < 30_000);
        assertEquals("Debris field of destroyed " + asteroid.name(), debris.name());
        assertTrue(oldRegions.stream().noneMatch(regionId -> truth.stream().anyMatch(object -> object.id().equals(regionId))));
    }

    @Test
    void missionExecutionKeepsSciencePrivateUntilUpload() {

        var system = new SpaceSimulation();
        var region = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.SURVEY_REGION)
                .findFirst().orElseThrow();
        var asteroid = system.truth().stream().filter(o -> o.type() == SpaceObjects.ObjectType.ASTEROID
                && Math.hypot(o.x() - region.x(), o.y() - region.y()) <= region.radius()).findFirst().orElseThrow();
        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.SPACE_SCANNER.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState())),
                Map.of(), 8, 0);
        var resources = new DynamicRocketSegment(0, 1_000_000, 0, Set.of());
        var scan = FlightPlanAction.create(ActionType.SCAN);
        var transmit = FlightPlanAction.create(ActionType.TRANSMIT_INFORMATION);
        var mission = new MissionState(OWNER,
                new ActiveRocketData(Map.of(id, segment), Map.of(id, resources)), plan(scan, transmit),
                new MissionState.Position(region.x(), region.y(), 500, 0, region.id(), OrbitBand.SURFACE,
                        -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO)),
                new SurveyKnowledge(), BlockPos.ZERO);
        var data = new MissionSavedData();

        for (int tick = 0; tick < 20; tick++) MissionController.step(null, data, mission, system, tick);
        assertEquals(transmit, mission.action());
        assertTrue(mission.knowledge.precise(asteroid.id()));
        assertFalse(system.earthKnowledge.precise(asteroid.id()));
        assertEquals(1_000_000 - SpaceBalance.SCAN_RF, resources.availableRF);

        MissionController.step(null, data, mission, system, 20);
        assertEquals(transmit, mission.action());
        mission.connected = true;
        MissionController.step(null, data, mission, system, 21);
        assertNull(mission.action());
        assertTrue(system.earthKnowledge.precise(asteroid.id()));
    }

    @Test
    void missionUpdatesRequireCommandsAndPreserveExecutionState() {

        var system = new SpaceSimulation();
        var current = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var completed = FlightPlanAction.create(ActionType.SCAN);
        var mission = new MissionState(OWNER, rocket(true, 0, 1_000_000), plan(current),
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO);
        mission.completed.add(completed);
        for (int i = 0; i < 400; i++) mission.advanceAction();
        var replacement = plan(completed, current, FlightPlanAction.create(ActionType.DISCARD_CRAFT));

        assertFalse(MissionController.replace(mission, replacement, system));
        mission.connected = true;
        assertTrue(MissionController.replace(mission, replacement, system));
        assertEquals(current, mission.action());
        assertEquals(400, mission.actionTicks());
        assertEquals(List.of(completed), mission.completed);

        mission.ended = true;
        assertFalse(MissionController.replace(mission, replacement, system));
    }

    @Test
    void missionUpdatesRejectUnflyableNavigationWithoutReplacingTheRunningTask() {

        var system = new SpaceSimulation();
        var current = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var navigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH);
        var position = new MissionState.Position(-3_070_000, 0, 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.LOW, 0, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        for (var craft : List.of(rocket(true, 0, 0), rocket(false, 0, 1_000_000))) {
            var state = new MissionState(OWNER, craft, plan(current), position, new SurveyKnowledge(), BlockPos.ZERO);
            state.connected = true;
            state.advanceAction();
            var oldPlan = state.plan;
            var charge = craft.getDynamicSegments().values().iterator().next().rf();
            var replacement = plan(navigation);
            var issues = RocketFlightPlanRules.inspect(replacement, craft, system.createObjectData(), false, position).issues();
            assertTrue(issues.stream().anyMatch(issue -> issue.actionId().equals(navigation.id()) && issue.code().startsWith("navigation_")),
                    () -> "Unflyable route with " + charge + " RF: " + issues);
            assertFalse(MissionController.replace(state, replacement, system));
            assertEquals(oldPlan, state.plan);
            assertEquals(current, state.action());
            assertEquals(1, state.actionTicks());
            assertEquals(charge, craft.getDynamicSegments().values().iterator().next().rf());
            assertNull(state.transfer);
        }
    }

    @Test
    void missionValidationReportsTheSpeedACappedClimbOutOfEarthOrbitNeeds() {

        var system = new SpaceSimulation();
        var craft = rocket(false, 1_000_000, 0);
        var position = new MissionState.Position(-3_070_000, 0, 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.LOW, 0, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var navigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceSimulation.MARS_ID)
                .withOrbit(OrbitBand.LOW).withMaxSpeed(1);
        var validation = RocketFlightPlanRules.inspect(plan(navigation), craft, system.createObjectData(), false, position);
        assertFalse(validation.valid());
        var issue = validation.issues().stream().filter(item -> item.actionId().equals(navigation.id())).findFirst().orElseThrow();
        assertNotNull(issue.failure());
        // A 1 m/s cruise stalls long before leaving Earth's gravity; the time limit is never reached.
        assertEquals("climb_speed", issue.failure().reason());
        assertEquals(1, issue.failure().actual(), 1e-9);
        assertTrue(issue.failure().limit() > 500);
        var state = new MissionState(OWNER, craft, plan(FlightPlanAction.create(ActionType.MAINTAIN_POSITION)), position,
                new SurveyKnowledge(), BlockPos.ZERO);
        state.connected = true;
        assertFalse(MissionController.replace(state, plan(navigation), system));
        assertEquals(ActionType.MAINTAIN_POSITION, state.action().type());
    }

    @Test
    void failedTransferCanBeRemovedOfflineButNotThroughAnotherOwnerOrDockingLink() {

        for (var status : List.of(MissionState.STATUS_NO_FEASIBLE_TRANSFER, MissionState.STATUS_TRANSFER_BLOCKED)) {
            var navigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH);
            var mission = new MissionState(OWNER, rocket(true, 0, 0), plan(navigation), MissionState.Position.surface(),
                    new SurveyKnowledge(), BlockPos.ZERO);
            mission.status = status;
            var saved = MissionState.CODEC.encodeStart(NbtOps.INSTANCE, mission).getOrThrow();
            var restored = MissionState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
            var data = new MissionSavedData();
            var id = restored.rocket.getRocketId();
            data.craft.put(id, restored);
            assertFalse(restored.canUpdateMission());
            assertTrue(restored.canDismiss(), "Blocked craft need an X even without communication, including after reload");
            assertFalse(data.dismiss(new UUID(2, 2), id));
            var port = new HardwareRef(SegmentRef.of(restored.rocket.getOriginalSegments().values().iterator().next()), BlockPos.ZERO);
            data.dockingLinks.add(new DockingLink(id, new UUID(3, 3), port, port));
            assertFalse(data.dismiss(OWNER, id), "A docking link still protects the other participant");
            data.dockingLinks.clear();
            assertTrue(data.dismiss(OWNER, id));
            assertTrue(data.craft.isEmpty());
        }
        var busy = new MissionState(OWNER, rocket(true, 0, 0), plan(FlightPlanAction.create(ActionType.MAINTAIN_POSITION)),
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO);
        assertFalse(busy.canDismiss(), "Loss of radio alone must not enable deleting a running mission");
    }

    @Test
    void correctingABlockedNavigationDiscardsItsFrozenGuidanceEvenForTheSameCard() {

        var system = new SpaceSimulation();
        var navigation = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH);
        var position = new MissionState.Position(-3_070_000, 0, 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.LOW, 0, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var state = new MissionState(OWNER, rocket(true, 0, 0), plan(navigation), position,
                new SurveyKnowledge(), BlockPos.ZERO);
        var data = new MissionSavedData();
        MissionController.step(null, data, state, system, 0);
        assertTrue(state.navigationBlocked());
        assertNotNull(state.transfer);
        state.rocket.getDynamicSegments().values().iterator().next().rfCapacity = 20_000_000;
        state.rocket.getDynamicSegments().values().iterator().next().receiveRF(20_000_000);
        state.connected = true;
        assertTrue(MissionController.replace(state, state.plan, system));
        assertNull(state.transfer, "Re-uploading after recharging must not retain the old empty-battery forecast");
        assertEquals(0, state.actionTicks());
        MissionController.step(null, data, state, system, 1);
        assertFalse(state.navigationBlocked());
        assertEquals(MissionState.STATUS_IN_FLIGHT, state.status);
    }

    @Test
    void maintainPositionResourceConditionsAdvanceTheLiveMission() {

        var system = new SpaceSimulation();
        var data = new MissionSavedData();
        for (var type : ActionAddonType.values()) {
            if (!type.isMaintainPositionCondition()) continue;
            var rfCondition = type == ActionAddonType.LOW_RF;
            var craft = rocket(rfCondition, rfCondition ? 0 : 1_000, rfCondition ? 1_000 : 0);
            var resources = craft.getDynamicSegments().values().iterator().next();
            if (type == ActionAddonType.LOW_RF) resources.availableRF = 500;
            if (type == ActionAddonType.LOW_FUEL) resources.availableFuelBurnTimeTicks = 500;
            var condition = new ActionAddon(UUID.randomUUID(), type, 50);
            var maintain = FlightPlanAction.create(ActionType.MAINTAIN_POSITION).withAddons(List.of(condition));
            var discard = FlightPlanAction.create(ActionType.DISCARD_CRAFT);
            var mission = new MissionState(OWNER, craft, plan(maintain, discard),
                    MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO);

            MissionController.step(null, data, mission, system, 0);
            assertEquals(discard, mission.action(), type + " did not complete maintain position");
            MissionController.step(null, data, mission, system, 1);
            assertTrue(mission.ended, type + " did not continue to the next action");
        }
    }

    @Test
    void maintainPositionCompletesWhenFuelOrCommunicationRfIsUnavailable() {

        var system = new SpaceSimulation();
        var data = new MissionSavedData();
        var maintain = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var discard = FlightPlanAction.create(ActionType.DISCARD_CRAFT);

        var fuelLimited = new MissionState(OWNER, rocket(false, 0, 1_000), plan(maintain, discard),
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO);
        MissionController.step(null, data, fuelLimited, system, 0);
        assertEquals(discard, fuelLimited.action());

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ANTENNA.get().defaultBlockState())),
                Map.of(), 5, 1, 1_000, 1_000);
        var rfLimitedRocket = new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(1_000, 0, 0, Set.of())));
        var rfLimited = new MissionState(OWNER, rfLimitedRocket, plan(maintain, discard),
                MissionState.Position.surface(), new SurveyKnowledge(), BlockPos.ZERO);
        MissionController.step(null, data, rfLimited, system, 0);
        assertEquals(discard, rfLimited.action());
    }

    @Test
    void liveStationKeepingCostDoesNotIncreaseWithEngineCount() {

        var maintain = FlightPlanAction.create(ActionType.MAINTAIN_POSITION);
        var oneEngine = stationKeepingRocket(1);
        var fourEngines = stationKeepingRocket(4);
        var oneMission = new MissionState(OWNER, oneEngine, plan(maintain), MissionState.Position.surface(),
                new SurveyKnowledge(), BlockPos.ZERO);
        var fourMission = new MissionState(OWNER, fourEngines, plan(maintain), MissionState.Position.surface(),
                new SurveyKnowledge(), BlockPos.ZERO);
        var data = new MissionSavedData();
        var system = new SpaceSimulation();

        for (int tick = 0; tick < SpaceBalance.STATION_KEEPING_INTERVAL_TICKS; tick++) {
            MissionController.step(null, data, oneMission, system, tick);
            MissionController.step(null, data, fourMission, system, tick);
        }

        var oneRemaining = oneEngine.getDynamicSegments().values().iterator().next().availableFuelBurnTimeTicks;
        var fourRemaining = fourEngines.getDynamicSegments().values().iterator().next().availableFuelBurnTimeTicks;
        assertTrue(oneRemaining < 20_000);
        assertEquals(oneRemaining, fourRemaining,
                "station keeping must charge delta-v rather than full-power time per engine");
    }

    @Test
    void activeNavigationContinuesDeterministicallyAfterSerialization() {

        var system = new SpaceSimulation();
        var data = new MissionSavedData();
        var craft = rocket(true, 0, 20_000_000);
        var navigate = FlightPlanAction.create(ActionType.NAVIGATE_TO)
                .withTarget(SpaceObjects.EARTH_ID).withOrbit(OrbitBand.HIGH);
        var start = new MissionState.Position(-3_070_000, 0, 0, 0, SpaceObjects.EARTH_ID,
                OrbitBand.LOW, 0, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var original = new MissionState(OWNER, craft, plan(navigate), start,
                new SurveyKnowledge(), BlockPos.ZERO);
        for (int tick = 0; tick < 100; tick++) MissionController.step(null, data, original, system, tick);

        assertNotNull(original.transfer);
        assertEquals(100, original.actionTicks());
        original.transfer.landing = new BlockPos(1, 70, 2);
        var encoded = MissionState.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        var loaded = MissionState.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertNotNull(loaded.transfer);
        assertNull(loaded.transfer.path, "compiled guidance is rebuilt from the frozen snapshot");
        assertEquals(original.transfer.landing, loaded.transfer.landing);
        assertEquals(original.actionTicks(), loaded.actionTicks());
        for (int tick = 100; tick < 200; tick++) {
            MissionController.step(null, data, original, system, tick);
            MissionController.step(null, data, loaded, system, tick);
        }

        assertEquals(original.position, loaded.position);
        assertEquals(original.plan, loaded.plan);
        assertEquals(original.rocket.getDynamicSegments().values().iterator().next().availableRF,
                loaded.rocket.getDynamicSegments().values().iterator().next().availableRF);
        assertEquals(original.rocket.getDynamicSegments().values().iterator().next().rf(),
                loaded.rocket.getDynamicSegments().values().iterator().next().rf());
        loaded.connected = true;
        assertTrue(MissionController.replace(loaded, plan(navigate.withOrbit(OrbitBand.MEDIUM)), system));
        assertNull(loaded.transfer, "changing navigation clears guidance, clock, landing and frozen inputs together");
        assertEquals(0, loaded.actionTicks());
        assertEquals(MissionForecast.Navigation.EMPTY, loaded.navigationForecast());
    }

    @Test
    void mixedEngineMarsFlightWithAntennaFinishesInTheLiveRunner() {

        var id = new UUID(31, 1);
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(BlockPos.ZERO, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(), SpaceAgeBlocks.ION_BOOSTER_ROCKET.get().defaultBlockState()),
                new StaticRocketSegment.BlockData(BlockPos.ZERO.above(2), SpaceAgeBlocks.ANTENNA.get().defaultBlockState())),
                Map.of(), 250, 2, 20_000_000, 200_000);
        var craft = new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(200_000, 20_000_000, 0, Set.of())));
        var navigate = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(SpaceSimulation.MARS_ID).withOrbit(OrbitBand.HIGH);
        var start = new MissionState.Position(-3_120_000, 0, 500, 0, SpaceObjects.EARTH_ID,
                OrbitBand.HIGH, -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var system = new SpaceSimulation();
        var program = plan(navigate);
        var forecast = RocketFlightPathCalculator.calculateFrom(craft, system.truth(), program, start).paths().getFirst();
        assertEquals(RocketFlightPathCalculator.TerminalState.READY, forecast.terminalState());
        var state = new MissionState(OWNER, craft, program, start, new SurveyKnowledge(), BlockPos.ZERO);
        var data = new MissionSavedData();
        data.craft.put(craft.getRocketId(), state);
        var deadline = Math.ceil(forecast.durationSeconds() * 20) + 1000;
        var replans = 0;
        for (int tick = 0; tick < deadline && state.action() != null; tick++) {
            SpaceCommunications.poweredAntennas(state, true);
            MissionController.step(null, data, state, system, tick);
            if (state.status.equals(MissionState.STATUS_REPLANNING)) replans++;
            if (replans >= 5 || state.navigationBlocked())
                throw new AssertionError("status=" + state.status + " tick=" + tick
                        + " rf=" + craft.getDynamicSegments().get(id).rf() + " actionTicks=" + state.actionTicks()
                        + " position=" + state.position);
        }
        assertNull(state.action(), "Live guidance must finish; status=" + state.status + " position=" + state.position);
        assertEquals(0, replans, "Planned ion burnout must not invalidate the chemical continuation");
        assertEquals(SpaceSimulation.MARS_ID, state.position.target());
        assertEquals(0, Math.hypot(state.position.vx(), state.position.vy()), 1e-5);
        assertTrue(craft.getDynamicSegments().get(id).fuel() > 0);
    }

    @Test
    void missionLaunchOriginRestoresMissingDetachedCraftLaunchMetadata() {

        var craft = rocket(true, 0, 0);
        var launch = new BlockPos(500, 70, -800);
        var state = new MissionState(OWNER, craft, FlightPlan.empty(), MissionState.Position.surface(), new SurveyKnowledge(), launch);
        assertEquals(launch, state.rocket.getLaunchPosition());
        assertEquals(launch, state.rocket.getLaunchPadCenter(), "Older craft fall back to their known launch footprint centre");
        var saved = MissionState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
        var restored = MissionState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(launch, restored.rocket.getLaunchPadCenter());
    }

    @Test
    void landingCoordinatesCenterTheActualFootprintRatherThanItsOffsetOrigin() {

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(new BlockPos(-5, -2, -7), Blocks.IRON_BLOCK.defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(-1, 2, -3), Blocks.IRON_BLOCK.defaultBlockState())), Map.of(), 2, 0);
        var craft = new ActiveRocketData(Map.of(id, segment), Map.of(id, new DynamicRocketSegment(0, 0, 0, Set.of())));
        var centre = RocketRecovery.footprintCenter(craft);
        assertEquals(-3, centre.getX());
        assertEquals(-5, centre.getZ());
        var origin = RocketRecovery.landingPosition(craft, 100, -200, (x, z) -> 70);
        assertEquals(100, origin.getX() + centre.getX());
        assertEquals(-200, origin.getZ() + centre.getZ());
        assertEquals(72, origin.getY());
        assertEquals(74, RocketRecovery.highestBlockY(craft, origin));
        craft.getDynamicSegments().get(id).reserved.put(new BlockPos(0, 5, 0), Blocks.IRON_BLOCK.defaultBlockState());
        assertEquals(77, RocketRecovery.highestBlockY(craft, origin), "Reserved physical inputs also determine asteroid clearance");
        assertEquals(new BlockPos(-4, 0, -6), RocketRecovery.center(List.of(new BlockPos(-5, 0, -7), new BlockPos(-2, 0, -4))),
                "Even footprints use a stable nearest block-grid alignment, including negative coordinates");
    }

    @Test
    void launchPadMetadataSurvivesCopiesPersistenceAndBothSidesOfASeparation() {

        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var left = new StaticRocketSegment(first, Set.of(new StaticRocketSegment.BlockData(new BlockPos(-5, 0, -7),
                Blocks.IRON_BLOCK.defaultBlockState())), Map.of(), 1, 0);
        var right = new StaticRocketSegment(second, Set.of(new StaticRocketSegment.BlockData(new BlockPos(1, 0, 2),
                Blocks.GLASS.defaultBlockState())), Map.of(), 1, 0);
        var rocket = new ActiveRocketData(Map.of(first, left, second, right),
                Map.of(first, new DynamicRocketSegment(0, 0, 0, Set.of(second)), second, new DynamicRocketSegment(0, 0, 0, Set.of(first))));
        var pad = new BlockPos(400, 71, -900);
        var launch = pad.offset(2, 0, 1);
        rocket.setLaunchPosition(launch);
        rocket.setLaunchPadCenter(pad);
        assertEquals(pad, MissionState.copyRocket(rocket).getLaunchPadCenter());
        var saved = ActiveRocketData.CODEC.encodeStart(NbtOps.INSTANCE, rocket).getOrThrow();
        var restored = ActiveRocketData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(pad, restored.getLaunchPadCenter());
        var state = new MissionState(OWNER, restored, FlightPlan.empty(), MissionState.Position.surface(), new SurveyKnowledge(), launch);
        var data = new MissionSavedData();
        data.craft.put(restored.getRocketId(), state);
        MissionController.split(data, state, Set.of(second), UUID.randomUUID(), 0);
        assertEquals(2, data.craft.size());
        for (var craft : data.craft.values()) {
            assertEquals(pad, craft.rocket.getLaunchPadCenter());
            assertEquals(launch, craft.rocket.getLaunchPosition());
            assertEquals(pad, craft.summary(0).rocket().getLaunchPadCenter());
        }
    }

    @Test
    void recoveryHeightClearsTheCraftFootprint() {

        var id = UUID.randomUUID();
        var segment = new StaticRocketSegment(id, Set.of(
                new StaticRocketSegment.BlockData(new BlockPos(0, -2, 0), Blocks.IRON_BLOCK.defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(2, 0, 0), Blocks.IRON_BLOCK.defaultBlockState()),
                new StaticRocketSegment.BlockData(new BlockPos(0, 1, 2), Blocks.IRON_BLOCK.defaultBlockState())),
                Map.of(), 3, 0);
        var craft = new ActiveRocketData(Map.of(id, segment),
                Map.of(id, new DynamicRocketSegment(0, 0, 0, Set.of())));

        var originY = RocketRecovery.landingOriginY(craft, 100, 200,
                (x, z) -> x == 100 && z == 200 ? 70 : x == 102 ? 75 : 68);

        for (var block : segment.blocks()) {
            var relative = block.relativePos();
            var surface = relative.getX() == 0 && relative.getZ() == 0 ? 70
                    : relative.getX() == 2 ? 75 : 68;
            assertTrue(originY + relative.getY() >= surface);
        }
    }

    @Test
    void closestAsteroidUsesInclusiveRadiusLimitsAndCurrentPosition() {

        var small = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID, 10, 0, 100, 0, SpaceObjects.DetectionState.PRECISE);
        var medium = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID, 20, 0, 200, 0, SpaceObjects.DetectionState.PRECISE);
        var large = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID, 40, 0, 300, 0, SpaceObjects.DetectionState.PRECISE);
        var action = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(FlightPlanAction.CLOSEST_ASTEROID)
                .withAsteroidTarget(new AsteroidTarget(true, 200, 300)).withOrbit(OrbitBand.TIGHT);
        var objects = List.of(small, medium, large);
        assertEquals(medium.id(), AsteroidTarget.resolve(action, objects, 0, 0, 0, FlightPlanAction.NO_TARGET).targetId());
        assertEquals(large.id(), AsteroidTarget.resolve(action, objects, 50, 0, 0, FlightPlanAction.NO_TARGET).targetId());
        assertEquals(FlightPlanAction.NO_TARGET, AsteroidTarget.resolve(action, List.of(small), 0, 0, 0, FlightPlanAction.NO_TARGET).targetId());
        assertFalse(new AsteroidTarget(true, 300, 200).valid());
        assertTrue(new AsteroidTarget(true, 300, 0).valid());
        var capture = FlightPlanAction.create(ActionType.CONNECT_ASTEROID).withTarget(FlightPlanAction.CLOSEST_ASTEROID);
        assertEquals(large.id(), AsteroidTarget.resolve(capture, objects, 0, 0, 0, large.id()).targetId());
    }

    @Test
    void missionCardAndPacketsKeepDynamicSelectionAndPreviewResolvesAgain() {

        var first = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID, -3_100_000, 0, 1_000, 0, SpaceObjects.DetectionState.PRECISE);
        var second = new SpaceObjectData(UUID.randomUUID(), SpaceObjects.ObjectType.ASTEROID, -3_140_000, 0, 2_000, 0, SpaceObjects.DetectionState.PRECISE);
        var navigate = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(first.id())
                .withOrbit(OrbitBand.TIGHT).withAsteroidTarget(new AsteroidTarget(true, 1_000, 2_000));
        var card = new MissionCard(UUID.randomUUID(), "Asteroid run", plan(navigate).withCategory("automated").withName("Probe"));
        var loaded = MissionCard.CODEC.parse(NbtOps.INSTANCE, MissionCard.CODEC.encodeStart(NbtOps.INSTANCE, card).getOrThrow()).getOrThrow();
        assertEquals(card, loaded);
        assertEquals("automated", RocketFlightPlanRules.normalize(loaded.plan()).category());
        assertEquals("automated", loaded.plan().withName("Renamed").withBranches(loaded.plan().branches())
                .withSegmentConfigurations(List.of()).category());
        var codec = ReflectiveCodecBuilder.create(FlightPlan.class);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        codec.encode(buffer, loaded.plan());
        assertEquals(loaded.plan(), codec.decode(buffer));
        buffer.release();

        var craft = rocket(true, 0, 20_000_000);
        var start = new MissionState.Position(-3_080_000, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.LOW,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var earth = new SpaceSimulation().truth().stream().filter(o -> o.id().equals(SpaceObjects.EARTH_ID)).findFirst().orElseThrow();
        var preview = RocketFlightPathCalculator.calculateFrom(craft, List.of(earth, first, second), loaded.plan(), start);
        assertTrue(preview.paths().getFirst().samples().stream().anyMatch(sample -> sample.targetId().equals(first.id())));
        var repeated = RocketFlightPathCalculator.calculateFrom(craft, List.of(earth, second), loaded.plan(), start);
        assertTrue(repeated.paths().getFirst().samples().stream().anyMatch(sample -> sample.targetId().equals(second.id())));
        assertEquals(first.id(), loaded.plan().root().actions().getFirst().targetId());
    }

    @Test
    void dynamicNavigationWaitsForKnowledgeAndKeepsDestinationAcrossReload() {

        var system = new SpaceSimulation();
        var data = new MissionSavedData();
        var craft = rocket(true, 0, 20_000_000);
        var navigate = FlightPlanAction.create(ActionType.NAVIGATE_TO).withTarget(FlightPlanAction.CLOSEST_ASTEROID)
                .withOrbit(OrbitBand.TIGHT).withAsteroidTarget(new AsteroidTarget(true, 0, 0));
        var start = new MissionState.Position(-3_080_000, 0, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.LOW,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var original = new MissionState(OWNER, craft, plan(navigate), start, new SurveyKnowledge(), BlockPos.ZERO);
        MissionController.step(null, data, original, system, 0);
        assertEquals("status.oritech_space_age.no_matching_asteroid", original.status);
        assertNull(original.transfer);
        original.knowledge.scan(system.truth(), 0, 0, 0, Double.MAX_VALUE);
        var expected = AsteroidTarget.resolve(navigate, system.knownObjects(original.knowledge), start.x(), start.y(), 0, start.target());
        for (int tick = 1; tick < 30; tick++) MissionController.step(null, data, original, system, tick);
        assertEquals(expected.targetId(), original.action().targetId());
        var loaded = MissionState.CODEC.parse(NbtOps.INSTANCE, MissionState.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow()).getOrThrow();
        for (int tick = 30; tick < 60; tick++) {
            MissionController.step(null, data, original, system, tick);
            MissionController.step(null, data, loaded, system, tick);
        }
        assertEquals(original.position, loaded.position);
        assertEquals(original.plan, loaded.plan);
        assertEquals(expected.targetId(), loaded.action().targetId());
        assertTrue(loaded.action().asteroidTarget().closest());
    }

}
