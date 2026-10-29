package rearth.oritech.spaceage.simulation.mission;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import rearth.oritech.spaceage.network.GroundBeamNetworking;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.AsteroidTarget;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.AsteroidDeliveries;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ServiceSettings;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.power.SolarPower;
import rearth.oritech.spaceage.simulation.power.SpacePowerService;
import rearth.oritech.spaceage.simulation.power.SurfaceBeamData;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.RocketLayout;
import rearth.oritech.spaceage.simulation.rocket.RocketPayloads;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;
import rearth.oritech.spaceage.simulation.rocket.RocketSimulationController;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.service.RocketProcessingService;
import rearth.oritech.spaceage.simulation.service.StationServiceRules;
import rearth.oritech.spaceage.simulation.survey.SpaceCommunications;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tick actual missions; predictions use the same transfer compiler but never execute service effects.
 */
public final class MissionController {

    // Docked hosts pay visitor upkeep separately unless their own card already pays it.
    private static final Set<ActionType> SELF_PAYING_ACTIONS = EnumSet.of(ActionType.MAINTAIN_POSITION, ActionType.PROCESS, ActionType.BEAM_POWER);

    private MissionController() {
    }

    public static void launch(ServerLevel level, UUID owner, ActiveRocketData rocket, FlightPlan plan, BlockPos launch) {

        launch(level, owner, rocket, plan, launch, 0);
    }

    /**
     * @param speed initial velocity pointing away from Earth, e.g. from a particle launch ramp
     */
    public static void launch(ServerLevel level, UUID owner, ActiveRocketData rocket, FlightPlan plan, BlockPos launch, double speed) {

        rocket.setLaunchPosition(launch);
        var system = SpaceSimulationSavedData.get(level.getServer()).getOrCreate(owner);
        plan = resolveOrbitSlots(plan, MissionSavedData.get(level.getServer()), owner, rocket.getRocketId());
        var objects = new HashMap<UUID, SpaceObjectData>();
        system.createObjectData().forEach(o -> objects.put(o.id(), o));
        var initial = RocketFlightPathCalculator.createInitialCraft(rocket, objects.get(SpaceObjects.EARTH_ID), plan.root(), objects);
        var position = launchPosition(initial.x, initial.y, launch, speed);
        var state = new MissionState(owner, rocket, plan, position, system.earthKnowledge.copy(), launch);
        var data = MissionSavedData.get(level.getServer());
        data.craft.put(rocket.getRocketId(), state);
        data.setDirty();
        RocketSimulationController.launchMissionVisual(level, rocket, plan, launch, speed);
    }

    public static MissionState.Position launchPosition(double x, double y, BlockPos launch, double speed) {

        var angle = SpaceBalance.surfaceAngle(launch.getX(), launch.getZ());
        return new MissionState.Position(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, SpaceObjects.EARTH_ID, OrbitBand.SURFACE,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
    }

    public static void tick(MinecraftServer server) {

        var data = MissionSavedData.get(server);
        for (int step = 0; step < data.debugSpeed; step++) {
            if (step > 0) data.extraTicks++;
            tickOnce(server, data, missionTime(server), step == data.debugSpeed - 1);
        }
    }

    public static long missionTime(MinecraftServer server) {

        return server.overworld().getGameTime() + MissionSavedData.get(server).extraTicks;
    }

    private static void tickOnce(MinecraftServer server, MissionSavedData data, long tick, boolean physicalTick) {

        var systems = SpaceSimulationSavedData.get(server);
        data.craft.values().stream().map(m -> m.owner).distinct().forEach(owner ->
                systems.getOrCreate(owner).tickAsteroids(server.overworld()));
        RocketDocking.synchronize(data);
        if (physicalTick) {
            SurfaceBeamData.get(server.overworld()).tick(server.overworld());
            AsteroidDeliveries.get(server.overworld()).tick(server.overworld());
        }
        data.power.update(server, data, tick);
        RocketProcessingService.tickJobs(data, owner -> systems.getOrCreate(owner).truth());
        var nodes = SpaceCommunications.update(server, data);
        for (var state : List.copyOf(data.craft.values())) {
            if (state.ended) continue;
            var system = systems.getOrCreate(state.owner);
            step(server, data, state, system, tick);
            if (!state.ended && !state.position.asteroid().equals(FlightPlanAction.NO_TARGET))
                system.moveAttached(state.position.asteroid(), state.position.x(), state.position.y());
        }

        // An idle host still pays for its visitors.
        if (tick % SpaceBalance.STATION_KEEPING_INTERVAL_TICKS == 0) {
            data.dockingLinks.stream().map(DockingLink::host).distinct().map(data.craft::get).filter(Objects::nonNull)
                    .filter(host -> host.action() == null || !SELF_PAYING_ACTIONS.contains(host.action().type()))
                    .filter(host -> !StationServiceRules.upkeep(host, RocketDocking.mass(data, host), SpaceBalance.STATION_KEEPING_INTERVAL_TICKS))
                    .forEach(host -> host.status = MissionState.STATUS_STATION_KEEPING_EXHAUSTED);
        }
        RocketDocking.synchronize(data);
        data.power.update(server, data, tick);
        if (physicalTick) {
            data.power.surface(server);
            GroundBeamNetworking.sync(server, data.power);
        }
        shareFleetKnowledge(data, nodes);
        systems.setDirty();
        if (!data.craft.isEmpty()) data.setDirty();
    }

    /**
     * Isolated fleets can cooperate. Ground delivery remains explicit.
     */
    private static void shareFleetKnowledge(MissionSavedData data, List<SpaceCommunications.Node> nodes) {

        record Cell(UUID owner, int x, int y) {
        }

        var cells = new HashMap<Cell, List<SpaceCommunications.Node>>();
        for (var node : nodes) {
            if (node.ground() || node.antennas() <= 0) continue;
            var x = (int) Math.floor(node.x() / SpaceBalance.ANTENNA_RANGE);
            var y = (int) Math.floor(node.y() / SpaceBalance.ANTENNA_RANGE);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (var other : cells.getOrDefault(new Cell(node.owner(), x + dx, y + dy), List.of())) {
                        if (!SpaceCommunications.linked(node, other)) continue;
                        var left = data.craft.get(node.id());
                        var right = data.craft.get(other.id());
                        left.knowledge.merge(right.knowledge);
                        right.knowledge.merge(left.knowledge);
                    }
                }
            }
            cells.computeIfAbsent(new Cell(node.owner(), x, y), ignored -> new ArrayList<>()).add(node);
        }
    }

    public static void step(MinecraftServer server, MissionSavedData data, MissionState state, SpaceSimulation system, long tick) {

        var action = state.action();
        if (action != null && (state.transfer == null && action.type() == ActionType.NAVIGATE_TO && action.asteroidTarget().closest()
                || action.targetId().equals(FlightPlanAction.CLOSEST_ASTEROID))) {
            var resolved = AsteroidTarget.resolve(action, system.knownObjects(state.knowledge), state.position.x(), state.position.y(), 0, action.type() == ActionType.DECOUPLE ? state.position.asteroid() : state.position.target());
            if (resolved.targetId().equals(FlightPlanAction.NO_TARGET)) {
                state.status = "status.oritech_space_age.no_matching_asteroid";
                return;
            }
            var root = state.plan.root();
            var original = action;
            state.plan = state.plan.withBranches(state.plan.branches().stream().map(b -> b.isRoot()
                    ? root.withActions(root.actions().stream().map(a -> a.id().equals(original.id()) ? resolved : a).toList()) : b).toList());
            action = resolved;
        }
        var linked = RocketDocking.linked(data, state.rocket.getRocketId());
        if (!linked && (action == null || action.type() != ActionType.NAVIGATE_TO && action.type() != ActionType.MAINTAIN_POSITION)) {
            // Craft without active guidance drift with their current velocity.
            var p = state.position;
            if (Math.hypot(p.vx(), p.vy()) > .001) state.positionRevision++;
            state.position = p.placed(p.x() + p.vx() / 20, p.y() + p.vy() / 20, p.vx(), p.vy(), p.target(), p.orbit(), p.slot());
        }
        if (action == null) {
            // Keep the explanation when the last card ended early.
            if (!state.status.equals(MissionState.STATUS_SOLAR_LOST) && !state.status.equals(MissionState.STATUS_ORBIT_FULL)
                    && !state.status.equals(MissionState.STATUS_STATION_KEEPING_EXHAUSTED))
                state.status = MissionState.STATUS_COMPLETE;
            return;
        }
        if (linked && (action.type() == ActionType.NAVIGATE_TO || action.type() == ActionType.DOCK
                || action.type() == ActionType.DECOUPLE && !RocketDocking.preservesPorts(data, state,
                action) || action.type() == ActionType.DISCARD_CRAFT)) {
            state.status = "status.oritech_space_age.undock_required";
            return;
        }
        if ((action.type() == ActionType.DECOUPLE || action.type() == ActionType.NAVIGATE_TO) && state.rocket.getDynamicSegments().values().stream()
                .anyMatch(r -> !r.reserved.isEmpty() || r.crafters.values().stream().anyMatch(c -> c.job != null))) {
            state.status = MissionState.STATUS_CARGO_WORKING;
            return;
        }
        switch (action.type()) {
            case PROCESS -> {
                var visual = state.rocket.getFlight();
                if (server != null && visual != null && !visual.isInSpace(server.overworld().getGameTime()))
                    state.status = MissionState.STATUS_REQUIRES_ZERO_G;
                else RocketProcessingService.process(data, state, system, action, VacuumRecipe.all(server));
            }
            case DOCK -> RocketDocking.connect(data, state, action.settings().docking());
            case UNDOCK -> RocketDocking.undock(data, state);
            case SCAN -> scan(state, system, action, tick);
            case TRANSMIT_INFORMATION -> transmit(server, state, system, action);
            case NAVIGATE_TO -> MissionNavigation.tick(server, data, state, system, action, tick);
            case CONNECT_ASTEROID -> connectAsteroid(data, state, system, action);
            case DECOUPLE -> decouple(data, state, system, action, tick);
            case DEPLOY_CUBESAT -> deploy(data, state, action);
            case BEAM_POWER -> SpacePowerService.step(data, state);
            case MAINTAIN_POSITION -> maintainPosition(data, state, system, action);
            case DISCARD_CRAFT -> {
                state.ended = true;
                state.status = MissionState.STATUS_DISCARDED;
            }
            case DISCONNECT_BOOSTER -> state.complete();
        }
    }

    private static void scan(MissionState state, SpaceSimulation system, FlightPlanAction action, long tick) {

        var objects = system.truth();
        var targetId = action.targetId().equals(FlightPlanAction.NO_TARGET)
                ? state.position.target() : action.targetId();
        var target = objects.stream().filter(object -> object.id().equals(targetId)).findFirst().orElse(null);
        if (target == null || target.type() != SpaceObjects.ObjectType.SURVEY_REGION
                || SurveyRules.surveyDistance(target, state.position.x(), state.position.y()) > SpaceBalance.SCAN_RANGE) {
            state.status = MissionState.STATUS_SCAN_WAITING;
            return;
        }
        long available = 0;
        var scannerInstalled = false;
        for (var entry : state.rocket.getStaticSegments().entrySet()) {
            var hardware = RocketHardware.of(entry.getValue());
            if (hardware.scanners() == 0) continue;
            scannerInstalled = true;
            available += (long) state.rocket.getDynamicSegments().get(entry.getKey()).rf();
        }
        if (!scannerInstalled || available < SpaceBalance.SCAN_RF) {
            state.status = MissionState.STATUS_SCAN_WAITING;
            return;
        }
        var remaining = SpaceBalance.SCAN_RF;
        for (var entry : state.rocket.getStaticSegments().entrySet()) {
            if (RocketHardware.of(entry.getValue()).scanners() == 0) continue;
            var resources = state.rocket.getDynamicSegments().get(entry.getKey());
            var spent = Math.min(remaining, (long) resources.rf());
            resources.spendRF(spent);
            remaining -= spent;
            if (remaining == 0) break;
        }
        state.knowledge.scanRegion(objects, target, tick, state.position.x(), state.position.y(), SpaceBalance.SCAN_RANGE);
        state.status = MissionState.STATUS_SCANNING;
        state.complete();
    }

    private static void transmit(MinecraftServer server, MissionState state, SpaceSimulation system,
                                 FlightPlanAction action) {

        state.advanceAction();
        if (state.connected) {
            deliverSurveyKnowledge(server, state, system);
            state.complete();
            state.status = MissionState.STATUS_INFORMATION_DELIVERED;
        } else if (action.service().timeoutTicks() > 0
                && state.actionTicks() >= action.service().timeoutTicks()) {
            state.complete();
            state.status = MissionState.STATUS_TRANSMISSION_TIMED_OUT;
        } else state.status = MissionState.STATUS_WAITING_FOR_LINK;
    }

    private static void connectAsteroid(MissionSavedData data, MissionState state, SpaceSimulation system,
                                        FlightPlanAction action) {

        var target = system.truth().stream().filter(object -> object.id().equals(action.targetId()))
                .findFirst().orElse(null);
        var alreadyAttached = data.craft.values().stream().anyMatch(other -> other != state && !other.ended
                && other.owner.equals(state.owner) && other.position.asteroid().equals(action.targetId()));
        if (alreadyAttached || !state.position.asteroid().equals(FlightPlanAction.NO_TARGET)) {
            state.status = MissionState.STATUS_ASTEROID_ATTACHED;
            return;
        }
        if (target == null || target.type() != SpaceObjects.ObjectType.ASTEROID || !state.knowledge.precise(action.targetId())) {
            state.status = MissionState.STATUS_PRECISE_REQUIRED;
            return;
        }
        var relativeSpeed = Math.hypot(state.position.vx() - target.velocityX(), state.position.vy() - target.velocityY());
        if (!state.position.target().equals(target.id()) || !AsteroidImpactRules.canConnectAsteroid(relativeSpeed)
                || state.position.orbit() != OrbitBand.SURFACE && state.position.orbit() != OrbitBand.TIGHT
                || Math.hypot(state.position.x() - target.x(), state.position.y() - target.y()) > target.radius() + 1_010
                || action.segments().size() != 1 || RocketLayout.segmentId(state.rocket, action.segments().getFirst()) == null) {
            state.status = MissionState.STATUS_SLOW_APPROACH;
            return;
        }
        state.position = state.position.withAttachment(target.id(), action.segments().getFirst());
        state.complete();
    }

    private static void decouple(MissionSavedData data, MissionState state, SpaceSimulation system,
                                 FlightPlanAction action, long tick) {

        if (!state.position.asteroid().equals(FlightPlanAction.NO_TARGET)
                && action.targetId().equals(state.position.asteroid())) {
            var position = state.position;
            var landing = state.completed.reversed().stream().filter(completed -> completed.type() == ActionType.NAVIGATE_TO
                    && completed.targetId().equals(SpaceObjects.EARTH_ID)
                    && completed.orbit() == OrbitBand.SURFACE).findFirst().orElse(null);
            system.releaseAsteroid(position.asteroid(), position.x(), position.y(), position.vx(), position.vy(), landing);
            state.position = position.withAttachment(FlightPlanAction.NO_TARGET, position.anchor());
        } else if (action.segments().size() == 2) {
            var ids = new HashMap<SegmentRef, UUID>();
            state.rocket.getStaticSegments().forEach((id, segment) -> ids.put(SegmentRef.of(segment), id));
            var retained = ids.get(action.segments().get(0));
            var detached = ids.get(action.segments().get(1));
            if (retained != null && detached != null) {
                state.rocket.getDynamicSegments().get(retained).removeConnection(detached);
                state.rocket.getDynamicSegments().get(detached).removeConnection(retained);
                var component = new HashSet<UUID>();
                var open = new ArrayDeque<UUID>();
                open.add(detached);
                while (!open.isEmpty()) {
                    var id = open.removeFirst();
                    if (component.add(id)) {
                        open.addAll(state.rocket.getDynamicSegments().get(id).getConnectedSegments());
                    }
                }
                if (!component.contains(retained)) split(data, state, component, action.id(), tick);
            }
        }
        state.complete();
    }

    private static void maintainPosition(MissionSavedData data, MissionState state, SpaceSimulation system, FlightPlanAction action) {

        if (RocketDocking.visitorLink(data, state.rocket.getRocketId()) != null) {
            state.status = MissionState.STATUS_DOCKED;
            return;
        }
        if (maintainPositionConditionReached(state, action)) {
            state.complete();
            state.status = MissionState.STATUS_READY;
            return;
        }
        if (SolarPower.stationary(state) && SolarPower.output(state) > 0) {
            if (!StationServiceRules.available(state)) {
                state.complete();
                state.status = MissionState.STATUS_STATION_KEEPING_EXHAUSTED;
            } else {
                state.advanceAction();
                var services = state.rocket.hardware().values().stream().mapToDouble(hardware -> StationServiceRules.antennaRF(hardware, 1)).sum();
                state.status = SolarPower.output(state) >= services ? "status.oritech_space_age.sustainable" : "status.oritech_space_age.battery_draining";
            }
            return;
        }
        var performance = RocketPerformanceCalculator.calculate(state.rocket);
        var hasAntenna = state.rocket.getStaticSegments().values().stream()
                .anyMatch(segment -> RocketHardware.of(segment).antennas() > 0);
        var outOfCommunicationRF = hasAntenna && SpaceCommunications.poweredAntennas(state, false) == 0;
        if (performance.availableBurnSeconds() <= 0 || outOfCommunicationRF) {
            if (!RocketDocking.linked(data, state.rocket.getRocketId())) state.complete();
            state.status = MissionState.STATUS_STATION_KEEPING_EXHAUSTED;
            return;
        }
        if (state.advanceAction() % SpaceBalance.STATION_KEEPING_INTERVAL_TICKS == 0) {
            var mass = RocketDocking.mass(data, state);
            if (!state.position.asteroid().equals(FlightPlanAction.NO_TARGET)) {
                mass += system.truth().stream()
                        .filter(object -> object.id().equals(state.position.asteroid()))
                        .mapToDouble(object -> object.mass() * SpaceBalance.KILOGRAMS_PER_ASTEROID_MASS)
                        .findFirst().orElse(0);
            }
            var correctionSeconds = SpaceBalance.stationKeepingBurnSecondsPerSecond(
                    performance.thrustNewtons(), mass) * SpaceBalance.STATION_KEEPING_INTERVAL_TICKS / 20;
            state.rocket.getStaticSegments().forEach((id, segment) ->
                    RocketHardware.of(segment).burn(state.rocket.getDynamicSegments().get(id), correctionSeconds));
        }
        state.status = MissionState.STATUS_MAINTAINING_POSITION;
    }

    private static boolean maintainPositionConditionReached(MissionState state, FlightPlanAction action) {

        var addon = action.addons().stream().filter(item -> item.type().isStationaryCondition())
                .findFirst().orElse(null);
        return addon != null && StationServiceRules.conditionReached(StationServiceRules.budgets(state.rocket), addon, 0, state.actionTicks() / 20d);
    }

    static MissionState split(MissionSavedData data, MissionState state, Set<UUID> detached, UUID action, long tick) {

        var childStatic = new HashMap<UUID, StaticRocketSegment>();
        var childDynamic = new HashMap<UUID, DynamicRocketSegment>();
        var parentStatic = new HashMap<>(state.rocket.getOriginalSegments());
        var parentDynamic = new HashMap<>(state.rocket.getDynamicSegments());
        detached.forEach(id -> {
            childStatic.put(id, parentStatic.remove(id));
            childDynamic.put(id, parentDynamic.remove(id));
        });
        childDynamic.values().forEach(r -> r.getConnectedSegments().retainAll(detached));
        parentDynamic.values().forEach(r -> r.getConnectedSegments().removeAll(detached));
        var child = childBranch(state, action);
        var position = state.position;
        var attachment = state.transfer == null ? position : state.transfer.start.position();
        var takesAsteroid = childStatic.values().stream().anyMatch(s -> SegmentRef.of(s).equals(attachment.anchor()));
        var childPosition = position.withAttachment(takesAsteroid ? attachment.asteroid() : FlightPlanAction.NO_TARGET, attachment.anchor());
        if (takesAsteroid) state.position = position.withAttachment(FlightPlanAction.NO_TARGET, attachment.anchor());
        var rocket = new ActiveRocketData(child.id(), childStatic, childDynamic, null);
        rocket.copyLaunchFrom(state.rocket);
        childDynamic.values().forEach(r -> r.crafters.values().forEach(c -> c.reassignOperator(state.rocket.getRocketId(), rocket.getRocketId())));
        var mission = new MissionState(state.owner, rocket, takeChildProgram(state, child, rocket, state.plan.name()),
                childPosition, state.knowledge.copy(), state.launch);
        data.craft.put(rocket.getRocketId(), mission);
        var parent = new ActiveRocketData(state.rocket.getRocketId(), parentStatic, parentDynamic, state.rocket.getFlight());
        parent.copyLaunchFrom(state.rocket);
        state.rocket = parent;
        var parentRefs = planRefs(parent);
        state.plan = state.plan.withSegmentConfigurations(state.plan.segmentConfigurations().stream().filter(c -> parentRefs.contains(c.segment())).toList());
        return mission;
    }

    /**
     * Releases a carried cube-sat as its own craft, like a decouple. It keeps the plan anchors it had inside the host,
     * so the deploy branch still matches its segments.
     */
    private static void deploy(MissionSavedData data, MissionState state, FlightPlanAction action) {

        var holder = state.rocket.getDynamicSegments().values().stream().filter(r -> r.payloads.containsKey(action.payload())).findFirst().orElse(null);
        if (holder == null) {
            state.status = "status.oritech_space_age.no_payload";
            return;
        }
        var cube = holder.payloads.remove(action.payload());
        holder.layout.put(action.payload(), Blocks.AIR.defaultBlockState());
        var child = childBranch(state, action.id());
        var rocket = cube.unfold(child.id(), action.payload());
        rocket.copyLaunchFrom(state.rocket);
        var cubeName = cube.name().isBlank() ? "Cube-Sat" : cube.name();
        var name = state.plan.name().isBlank() ? cubeName : state.plan.name() + " · " + cubeName;
        var p = state.position;
        var position = new MissionState.Position(p.x(), p.y(), p.vx(), p.vy(), p.target(), p.orbit(), p.slot(), 1,
                FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO), p.headingX(), p.headingY());
        var mission = new MissionState(state.owner, rocket, takeChildProgram(state, child, rocket,
                name.substring(0, Math.min(name.length(), RocketFlightPlanRules.MAX_ROCKET_NAME_LENGTH))), position, state.knowledge.copy(), state.launch);
        data.craft.put(rocket.getRocketId(), mission);
        var parentRefs = planRefs(state.rocket);
        state.plan = state.plan.withSegmentConfigurations(state.plan.segmentConfigurations().stream().filter(c -> parentRefs.contains(c.segment())).toList());
        state.complete();
    }

    private static FlightPlanBranch childBranch(MissionState state, UUID action) {

        return state.plan.branches().stream().filter(b -> b.parentSeparationAction().equals(action)).findFirst()
                .orElse(new FlightPlanBranch(UUID.randomUUID(), action, List.of()));
    }

    /**
     * Segment references of a craft and of the cube-sats it still carries.
     */
    private static Set<SegmentRef> planRefs(ActiveRocketData rocket) {

        return RocketPayloads.planView(rocket).getStaticSegments().values().stream().map(SegmentRef::of).collect(Collectors.toSet());
    }

    /**
     * Moves a separated branch and its descendants into the program of the new craft.
     */
    private static FlightPlan takeChildProgram(MissionState state, FlightPlanBranch child, ActiveRocketData rocket, String name) {

        var childBranches = new ArrayList<FlightPlanBranch>();
        childBranches.add(new FlightPlanBranch(child.id(), FlightPlanBranch.NO_PARENT, child.actions()));
        state.plan.branches().stream().filter(b -> !b.isRoot() && !b.id().equals(child.id())).forEach(childBranches::add);
        var childRefs = planRefs(rocket);
        var childSettings = state.plan.segmentConfigurations().stream().filter(c -> childRefs.contains(c.segment())).toList();
        // The launched child owns its program now; it no longer occupies the parent's memory.
        var childBranchIds = new HashSet<UUID>();
        childBranchIds.add(child.id());
        var childActionIds = new HashSet<UUID>();
        child.actions().forEach(a -> childActionIds.add(a.id()));
        boolean found;
        do {
            found = false;
            for (var branch : state.plan.branches())
                if (!childBranchIds.contains(branch.id()) && childActionIds.contains(branch.parentSeparationAction())) {
                    childBranchIds.add(branch.id());
                    branch.actions().forEach(a -> childActionIds.add(a.id()));
                    found = true;
                }
        } while (found);
        state.plan = state.plan.withBranches(state.plan.branches().stream().filter(b -> !childBranchIds.contains(b.id())).toList());
        return RocketFlightPlanRules.normalize(new FlightPlan(childBranches, childSettings, name, state.plan.category()));
    }

    public static FlightPlan resolveOrbitSlots(FlightPlan plan, MissionSavedData data, UUID owner, UUID self) {

        record Ring(UUID body, OrbitBand orbit) {
        }

        var occupied = new HashMap<Ring, Set<Integer>>();
        for (var craft : data.craft.values()) {
            if (!craft.owner.equals(owner) || craft.ended || craft.rocket.getRocketId().equals(self)) continue;
            occupied.computeIfAbsent(new Ring(craft.position.target(), craft.position.orbit()), ignored -> new HashSet<>()).add(craft.position.slot());
            craft.plan.branches().stream().flatMap(b -> b.actions().stream()).filter(a -> a.type() == ActionType.NAVIGATE_TO
                    && SpaceBalance.hasSlots(a.targetId(), a.orbit()) && a.service().slot() >= 0).forEach(a ->
                    occupied.computeIfAbsent(new Ring(a.targetId(), a.orbit()), ignored -> new HashSet<>()).add(a.service().slot()));
        }
        return plan.withBranches(plan.branches().stream().map(branch -> branch.withActions(branch.actions().stream().map(action -> {
            if (action.type() != ActionType.NAVIGATE_TO || !SpaceBalance.hasSlots(action.targetId(), action.orbit()))
                return action;
            var used = occupied.computeIfAbsent(new Ring(action.targetId(), action.orbit()), ignored -> new HashSet<>());
            var slot = SpaceBalance.freeSlot(action.service().slot(), used);
            // A full ring retains an unassigned destination; arrival waits with an explicit full-ring status.
            if (slot >= SpaceBalance.slots(action.targetId(), action.orbit())) slot = -1;
            else used.add(slot);
            var settings = action.service();
            return action.withService(new ServiceSettings(settings.durationTicks(), settings.timeoutTicks(), slot));
        }).toList())).toList());
    }

    public static boolean redirectBeam(MissionState state, UUID target, int x, int z, SpaceSimulation system,
                                       Collection<RecipeHolder<VacuumRecipe>> recipes, List<DockingTarget> stations) {

        var action = state.action();
        if (!state.canUpdateMission() || action == null || action.type() != ActionType.BEAM_POWER
                || target.equals(state.rocket.getRocketId())) return false;
        if (!target.equals(SpaceObjects.EARTH_ID) && stations.stream().noneMatch(station -> station.id().equals(target)))
            return false;
        var redirected = action.withTarget(target).withLanding(x, z, 0, 0);
        var root = state.plan.root();
        var plan = state.plan.withBranches(state.plan.branches().stream().map(branch -> branch.id().equals(root.id())
                ? branch.withActions(branch.actions().stream().map(card -> card.id().equals(action.id()) ? redirected : card).toList())
                : branch).toList());
        state.knowledge.merge(system.earthKnowledge);
        var validation = RocketFlightPlanRules.inspect(plan, state.rocket, system.knownObjects(state.knowledge), false, state.position, recipes, stations);
        if (!validation.valid()) return false;
        // Changing the beam destination keeps the current action's elapsed time and completion conditions.
        state.plan = validation.plan();
        state.status = MissionState.STATUS_UPDATE_ACCEPTED;
        return true;
    }

    public static boolean replace(MissionState state, FlightPlan plan, SpaceSimulation system) {

        return replace(state, plan, system, List.of(), List.of());
    }

    public static boolean replace(MissionState state, FlightPlan plan, SpaceSimulation system,
                                  Collection<RecipeHolder<VacuumRecipe>> recipes, List<DockingTarget> stations) {

        if (!state.connected || state.ended) return false;
        // Stale editor drafts must not replay actions which completed while the player was editing.
        var finished = state.completed.stream().map(FlightPlanAction::id).collect(Collectors.toSet());
        plan = plan.withBranches(plan.branches().stream().map(branch -> branch.withActions(branch.actions().stream()
                .filter(action -> !finished.contains(action.id())).toList())).toList());
        state.knowledge.merge(system.earthKnowledge);
        var validation = RocketFlightPlanRules.inspect(plan, state.rocket, system.knownObjects(state.knowledge), false, state.position, recipes, stations);
        if (!validation.valid()) return false;
        var validated = validation.plan();
        var current = state.action();
        var unchangedCurrent = current != null && !validated.root().actions().isEmpty()
                && current.equals(validated.root().actions().getFirst());
        var unchangedGuidance = state.transfer == null || state.plan.root().id().equals(validated.root().id())
                && state.rocket.getStaticSegments().values().stream().allMatch(segment -> {
            var ref = SegmentRef.of(segment);
            var previous = state.plan.configurationFor(ref);
            var next = validated.configurationFor(ref);
            return previous.booster() == next.booster() && previous.engineStages().equals(next.engineStages());
        });
        state.plan = validated;
        if (!unchangedCurrent || !unchangedGuidance || state.navigationBlocked()) state.resetAction();
        state.status = MissionState.STATUS_UPDATE_ACCEPTED;
        return true;
    }

    static void deliverSurveyKnowledge(MinecraftServer server, MissionState state, SpaceSimulation system) {

        var before = new HashMap<UUID, SpaceObjectData>();
        system.earthKnowledge.contacts().forEach(contact -> before.put(contact.id(), contact));
        system.earthKnowledge.merge(state.knowledge);
        var discovered = 0;
        var updated = 0;
        for (var contact : system.earthKnowledge.contacts()) {
            var previous = before.get(contact.id());
            if (previous == null) discovered++;
            else if (!previous.equals(contact)) updated++;
        }
        if (server != null) MissionNetworking.sendSurveyResults(server, state.owner, discovered, updated);
    }
}
