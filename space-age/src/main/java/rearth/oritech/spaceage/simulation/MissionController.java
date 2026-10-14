package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ServiceSettings;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tick actual missions; predictions use the same transfer compiler but never execute service effects.
 */
public final class MissionController {

    private MissionController() {
    }

    public static void launch(ServerLevel level, UUID owner, ActiveRocketData rocket, FlightPlan plan, BlockPos launch) {

        rocket.setLaunchPosition(launch);
        var system = SpaceSimulationSavedData.get(level.getServer()).getOrCreate(owner);
        plan = resolveOrbitSlots(plan, MissionSavedData.get(level.getServer()), owner, rocket.getRocketId());
        var objects = new HashMap<UUID, SpaceObjectData>();
        system.createObjectData().forEach(o -> objects.put(o.id(), o));
        var initial = RocketFlightPathCalculator.createInitialCraft(rocket, objects.get(SpaceObjects.EARTH_ID), plan.root(), objects);
        var position = new MissionState.Position(initial.x, initial.y, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.SURFACE,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var state = new MissionState(owner, rocket, plan, position, system.earthKnowledge.copy(), launch, missionTime(level.getServer()));
        var data = MissionSavedData.get(level.getServer());
        data.craft.put(rocket.getRocketId(), state);
        data.setDirty();
        RocketSimulationController.launchMissionVisual(level, rocket, launch);
    }

    public static void tick(MinecraftServer server) {

        var data = MissionSavedData.get(server);
        for (int step = 0; step < data.debugSpeed; step++) {
            if (step > 0) data.extraTicks++;
            tickOnce(server, data, missionTime(server));
        }
    }

    public static long missionTime(MinecraftServer server) {

        return server.overworld().getGameTime() + MissionSavedData.get(server).extraTicks;
    }

    private static void tickOnce(MinecraftServer server, MissionSavedData data, long tick) {

        data.craft.values().stream().map(m -> m.owner).distinct().forEach(owner ->
                SpaceSimulationSavedData.get(server).getOrCreate(owner).tickAsteroids(server.overworld()));
        RocketDocking.synchronize(data);
        RocketProcessingService.tickJobs(data, owner -> SpaceSimulationSavedData.get(server).getOrCreate(owner).truth());
        var nodes = SpaceCommunications.update(server, data);
        for (var state : List.copyOf(data.craft.values())) {
            if (state.ended) continue;
            var system = SpaceSimulationSavedData.get(server).getOrCreate(state.owner);
            step(server, data, state, system, tick);
            if (!state.ended && !state.position.asteroid().equals(FlightPlanAction.NO_TARGET))
                system.moveAttached(state.position.asteroid(), state.position.x(), state.position.y());
        }
        // An idle host still pays for its visitors. Maintain Position and local Process pay in their runners.
        data.dockingLinks.stream().map(DockingLink::host).distinct().map(data.craft::get).filter(Objects::nonNull)
                .filter(host -> host.action() == null || host.action().type() != ActionType.MAINTAIN_POSITION && host.action().type() != ActionType.PROCESS)
                .forEach(host -> {
                    if (tick % SpaceBalance.STATION_KEEPING_INTERVAL_TICKS == 0 && !StationServiceRules.upkeep(host, RocketDocking.mass(data, host),
                            SpaceBalance.STATION_KEEPING_INTERVAL_TICKS))
                        host.status = MissionState.STATUS_STATION_KEEPING_EXHAUSTED;
                });
        RocketDocking.synchronize(data);
        shareFleetKnowledge(data, nodes);
        SpaceSimulationSavedData.get(server).setDirty();
        if (!data.craft.isEmpty()) data.setDirty();
    }

    /**
     * Isolated fleets can cooperate. Ground delivery remains explicit.
     */
    private static void shareFleetKnowledge(MissionSavedData data, List<SpaceCommunications.Node> nodes) {

        for (var leftNode : nodes) {
            if (leftNode.ground()) continue;
            for (var rightNode : nodes) {
                if (leftNode.id().compareTo(rightNode.id()) >= 0 || rightNode.ground()
                        || !SpaceCommunications.linked(leftNode, rightNode)) continue;
                var left = data.craft.get(leftNode.id());
                var right = data.craft.get(rightNode.id());
                left.knowledge.merge(right.knowledge);
                right.knowledge.merge(left.knowledge);
            }
        }
    }

    static void step(MinecraftServer server, MissionSavedData data, MissionState state, SpaceSimulation system, long tick) {

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
            var p = state.position;
            if (Math.hypot(p.vx(), p.vy()) > .001) state.positionRevision++;
            state.position = new MissionState.Position(p.x() + p.vx() / 20, p.y() + p.vy() / 20, p.vx(), p.vy(),
                    p.target(), p.orbit(), p.slot(), p.stage(), p.asteroid(), p.anchor(), p.headingX(), p.headingY());
        }
        if (action == null) {
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
            state.status = "status.oritech_space_age.cargo_working";
            return;
        }
        switch (action.type()) {
            case PROCESS -> {
                var visual = state.rocket.getFlight();
                if (server != null && visual != null && !visual.isInSpace(server.overworld().getGameTime()))
                    state.status = "status.oritech_space_age.requires_zero_g";
                else RocketProcessingService.process(data, state, system, action,
                        server == null ? List.of() : server.overworld().recipeAccess().recipeMap().byType(SpaceAgeRecipes.VACUUM_CRAFTING.get()));
            }
            case DOCK -> RocketDocking.connect(data, state, action.settings().docking());
            case UNDOCK -> RocketDocking.undock(data, state);
            case SCAN -> scan(state, system, action, tick);
            case TRANSMIT_INFORMATION -> transmit(server, state, system, action);
            case NAVIGATE_TO -> MissionNavigation.tick(server, data, state, system, action, tick);
            case CONNECT_ASTEROID -> connectAsteroid(data, state, system, action);
            case DECOUPLE -> decouple(data, state, system, action, tick);
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
            available += state.rocket.getDynamicSegments().get(entry.getKey()).availableRF;
        }
        if (!scannerInstalled || available < SpaceBalance.SCAN_RF) {
            state.status = MissionState.STATUS_SCAN_WAITING;
            return;
        }
        var remaining = SpaceBalance.SCAN_RF;
        for (var entry : state.rocket.getStaticSegments().entrySet()) {
            if (RocketHardware.of(entry.getValue()).scanners() == 0) continue;
            var resources = state.rocket.getDynamicSegments().get(entry.getKey());
            var spent = Math.min(remaining, resources.availableRF);
            resources.availableRF -= spent;
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
        if (alreadyAttached) {
            state.status = MissionState.STATUS_ASTEROID_ATTACHED;
            return;
        }
        if (target == null || !state.knowledge.precise(action.targetId())) {
            state.status = MissionState.STATUS_PRECISE_REQUIRED;
            return;
        }
        if (!state.position.target().equals(target.id()) || Math.hypot(state.position.vx(), state.position.vy()) > 12
                || Math.hypot(state.position.x() - target.x(), state.position.y() - target.y()) > target.radius() + 1_010
                || action.segments().isEmpty()) {
            state.status = MissionState.STATUS_SLOW_APPROACH;
            return;
        }
        var position = state.position;
        state.position = new MissionState.Position(position.x(), position.y(), position.vx(), position.vy(),
                position.target(), position.orbit(), position.slot(), position.stage(), target.id(),
                action.segments().getFirst(), position.headingX(), position.headingY());
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
            state.position = new MissionState.Position(position.x(), position.y(), position.vx(), position.vy(),
                    position.target(), position.orbit(), position.slot(), position.stage(),
                    FlightPlanAction.NO_TARGET, position.anchor(), position.headingX(), position.headingY());
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
            state.status = "status.oritech_space_age.docked";
            return;
        }
        if (maintainPositionConditionReached(state, action)) {
            state.complete();
            state.status = MissionState.STATUS_READY;
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
                        .mapToDouble(object -> object.mass() * AsteroidImpactRules.KILOGRAMS_PER_ASTEROID_MASS)
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

        var addon = action.addons().stream().filter(item -> item.type().isMaintainPositionCondition())
                .findFirst().orElse(null);
        return StationServiceRules.conditionReached(StationServiceRules.budgets(state.rocket), addon, 0, 0);
    }

    static void split(MissionSavedData data, MissionState state, Set<UUID> detached, UUID action, long tick) {

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
        var child = state.plan.branches().stream().filter(b -> b.parentSeparationAction().equals(action)).findFirst().orElse(new FlightPlanBranch(UUID.randomUUID(), action, List.of()));
        var childBranches = new ArrayList<FlightPlanBranch>();
        childBranches.add(new FlightPlanBranch(child.id(), FlightPlanBranch.NO_PARENT, child.actions()));
        state.plan.branches().stream().filter(b -> !b.isRoot() && !b.id().equals(child.id())).forEach(childBranches::add);
        var position = state.position;
        var attachment = state.transfer == null ? position : state.transfer.start.position();
        var takesAsteroid = childStatic.values().stream().anyMatch(s -> SegmentRef.of(s).equals(attachment.anchor()));
        var childPosition = new MissionState.Position(position.x(), position.y(), position.vx(), position.vy(), position.target(), position.orbit(),
                position.slot(), position.stage(), takesAsteroid ? attachment.asteroid() : FlightPlanAction.NO_TARGET, attachment.anchor(),
                        position.headingX(), position.headingY());
        if (takesAsteroid)
            state.position = new MissionState.Position(position.x(), position.y(), position.vx(), position.vy(), position.target(), position.orbit(),
                    position.slot(), position.stage(), FlightPlanAction.NO_TARGET, attachment.anchor(), position.headingX(), position.headingY());
        var rocket = new ActiveRocketData(child.id(), childStatic, childDynamic, null);
        childDynamic.values().forEach(r -> r.crafters.values().forEach(c -> c.reassignOperator(state.rocket.getRocketId(), rocket.getRocketId())));
        var childRefs = childStatic.values().stream().map(SegmentRef::of).toList();
        var childSettings = state.plan.segmentConfigurations().stream().filter(c -> childRefs.contains(c.segment())).toList();
        var mission = new MissionState(state.owner, rocket, RocketFlightPlanRules.normalize(new FlightPlan(childBranches, childSettings, state.plan.name())),
                childPosition, state.knowledge.copy(), state.launch, tick);
        data.craft.put(rocket.getRocketId(), mission);
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
        state.rocket = new ActiveRocketData(state.rocket.getRocketId(), parentStatic, parentDynamic, state.rocket.getFlight());
        var parentRefs = parentStatic.values().stream().map(SegmentRef::of).toList();
        state.plan = state.plan.withSegmentConfigurations(state.plan.segmentConfigurations().stream().filter(c -> parentRefs.contains(c.segment())).toList());
    }

    public static FlightPlan resolveOrbitSlots(FlightPlan plan, MissionSavedData data, UUID owner, UUID self) {

        var occupied = new HashMap<OrbitBand, Set<Integer>>();
        for (var craft : data.craft.values()) {
            if (!craft.owner.equals(owner) || craft.ended || craft.rocket.getRocketId().equals(self)) continue;
            occupied.computeIfAbsent(craft.position.orbit(), ignored -> new HashSet<>()).add(craft.position.slot());
            craft.plan.branches().stream().flatMap(b -> b.actions().stream()).filter(a -> a.type() == ActionType.NAVIGATE_TO
                    && a.targetId().equals(SpaceObjects.EARTH_ID) && a.service().slot() >= 0).forEach(a ->
                    occupied.computeIfAbsent(a.orbit(), ignored -> new HashSet<>()).add(a.service().slot()));
        }
        return plan.withBranches(plan.branches().stream().map(branch -> branch.withActions(branch.actions().stream().map(action -> {
            if (action.type() != ActionType.NAVIGATE_TO || !action.targetId().equals(SpaceObjects.EARTH_ID)
                    || !SpaceBalance.hasSlots(action.orbit()) || action.service().slot() >= 0) return action;
            var used = occupied.computeIfAbsent(action.orbit(), ignored -> new HashSet<>());
            var slot = 0;
            while (used.contains(slot) && slot < SpaceBalance.slots(action.orbit()) - 1) slot++;
            used.add(slot);
            var settings = action.service();
            return action.withService(new ServiceSettings(settings.durationTicks(), settings.timeoutTicks(), slot));
        }).toList())).toList());
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
        state.plan = validated;
        if (!unchangedCurrent) state.resetAction();
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
