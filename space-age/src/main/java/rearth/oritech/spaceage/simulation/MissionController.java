package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
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
                        || !SpaceCommunications.linked(leftNode, rightNode, nodes)) continue;
                var left = data.craft.get(leftNode.id());
                var right = data.craft.get(rightNode.id());
                left.knowledge.merge(right.knowledge);
                right.knowledge.merge(left.knowledge);
            }
        }
    }

    static void step(MinecraftServer server, MissionSavedData data, MissionState state, SpaceSimulation system, long tick) {

        var action = state.action();
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
                        action) || action.type() == ActionType.DETONATE || action.type() == ActionType.DISCARD_CRAFT)) {
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
            case NAVIGATE_TO -> navigate(server, data, state, system, action, tick);
            case CONNECT_ASTEROID -> connectAsteroid(data, state, system, action);
            case DECOUPLE -> decouple(data, state, system, action, tick);
            case MAINTAIN_POSITION -> maintainPosition(data, state, system, action);
            case DETONATE -> {
                var target = RocketExplosives.detonationTarget(system.truth(), state.position.x(), state.position.y());
                if (target != null) system.applyAsteroidImpact(target.id(), AsteroidImpactRules.predictArrival(
                        RocketPerformanceCalculator.calculate(state.rocket).wetMassKilograms(), target,
                        Math.hypot(state.position.vx() - target.velocityX(), state.position.vy() - target.velocityY()),
                        null, action, RocketExplosives.remaining(state.rocket), true));
                state.ended = true;
                state.status = MissionState.STATUS_DESTROYED;
                state.complete();
            }
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

        state.actionTicks++;
        if (state.canTransmit) {
            deliverSurveyKnowledge(server, state, system);
            state.complete();
            state.status = MissionState.STATUS_INFORMATION_DELIVERED;
        } else if (action.service().timeoutTicks() > 0
                && state.actionTicks >= action.service().timeoutTicks()) {
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
        if (++state.actionTicks % SpaceBalance.STATION_KEEPING_INTERVAL_TICKS == 0) {
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
        if (addon == null) return false;
        long current;
        long initial;
        if (addon.type() == SpaceSimulation.ActionAddonType.LOW_RF) {
            current = state.rocket.getStaticSegments().entrySet().stream()
                    .filter(entry -> RocketHardware.of(entry.getValue()).usesStationKeepingRF())
                    .mapToLong(entry -> state.rocket.getDynamicSegments().get(entry.getKey()).availableRF).sum();
            initial = state.rocket.getStaticSegments().values().stream()
                    .filter(segment -> RocketHardware.of(segment).usesStationKeepingRF())
                    .mapToLong(StaticRocketSegment::initialRF).sum();
        } else {
            current = state.rocket.getStaticSegments().entrySet().stream()
                    .filter(entry -> RocketHardware.of(entry.getValue()).usesStationKeepingFuel())
                    .mapToLong(entry -> state.rocket.getDynamicSegments().get(entry.getKey()).availableFuelBurnTimeTicks).sum();
            initial = state.rocket.getStaticSegments().values().stream()
                    .filter(segment -> RocketHardware.of(segment).usesStationKeepingFuel())
                    .mapToLong(StaticRocketSegment::initialFuel).sum();
        }
        return initial > 0 && current * 100.0 <= initial * (double) addon.value();
    }

    private static void navigate(MinecraftServer server, MissionSavedData data, MissionState state, SpaceSimulation system, FlightPlanAction action, long tick) {

        var rendezvous = action.settings().docking();
        if (action.targetId().equals(rendezvous.host())) {
            var issue = RocketDocking.rendezvousIssue(data, state, rendezvous);
            if (!issue.isEmpty()) {
                state.status = "status.oritech_space_age." + issue;
                return;
            }
        }
        if (state.leg == null) {
            state.positionRevision++;
            // Older/incomplete saves may have elapsed transfer ticks without the matching start snapshot.
            // Restart guidance from the persisted current position instead of sampling past a newly built path.
            state.actionTicks = 0;
            state.leg = new MissionState.Telemetry(MissionState.copyRocket(state.rocket), state.plan, state.position, tick, MissionState.STATUS_TRANSFER);
            state.legObjects = system.knownObjects(state.knowledge);
            state.legStations = RocketDocking.targets(data, state.owner);
            var p = state.position;
            state.position = new MissionState.Position(p.x(), p.y(), p.vx(), p.vy(), p.target(), p.orbit(), -1, p.stage(), p.asteroid(), p.anchor(), p.headingX(), p.headingY());
        }
        if (state.path == null) {
            if (state.legObjects.isEmpty()) state.legObjects = system.knownObjects(state.knowledge);
            var root = state.plan.root().withActions(List.of(action));
            var branches = new ArrayList<>(state.plan.branches());
            branches.removeIf(FlightPlanBranch::isRoot);
            branches.addFirst(root);
            state.path = RocketFlightPathCalculator.calculateFrom(state.leg.rocket(), state.legObjects,
                    state.plan.withBranches(branches), state.leg.position(), server == null ? List.of()
                            : server.overworld().recipeAccess().recipeMap().byType(SpaceAgeRecipes.VACUUM_CRAFTING.get()), state.legStations);
        }
        var path = state.path.paths().stream().filter(p -> p.branchId().equals(state.plan.root().id())).findFirst().orElse(null);
        if (path == null || path.samples().isEmpty()) {
            state.status = MissionState.STATUS_NO_FEASIBLE_TRANSFER;
            return;
        }
        var before = state.actionTicks / 20.0;
        var after = Math.min(path.durationSeconds(), before + 0.05);
        var samples = path.samples();
        for (int index = 1; index < samples.size(); index++) {
            var previous = samples.get(index - 1);
            var next = samples.get(index);
            var duration = Math.max(0, Math.min(after, next.timeSeconds()) - Math.max(before, previous.timeSeconds()));
            if (duration <= 0) continue;
            var depleted = state.rocket.getStaticSegments().entrySet().stream().anyMatch(entry -> {
                var h = RocketHardware.of(entry.getValue());
                var r = state.rocket.getDynamicSegments().get(entry.getKey());
                return next.firingSegments().contains(SegmentRef.of(entry.getValue())) && h.ion() > 0 && r.availableRF < duration * 20 * h.ion() * SpaceBalance.ION_RF
                        && state.leg.rocket().getDynamicSegments().get(entry.getKey()).availableRF > 0;
            });
            if (depleted) {
                state.leg = null;
                state.path = null;
                state.actionTicks = 0;
                state.status = MissionState.STATUS_REPLANNING;
                return;
            }
            state.rocket.getStaticSegments().forEach((id, segment) -> {
                if (next.firingSegments().contains(SegmentRef.of(segment)))
                    RocketHardware.of(segment).burn(state.rocket.getDynamicSegments().get(id), duration);
            });
        }
        state.position = sample(samples, before + 0.05, state.position);
        state.actionTicks++;
        state.status = MissionState.STATUS_IN_FLIGHT;
        for (var event : state.path.boosterEvents()) {
            if (!event.branchId().equals(state.plan.root().id()) || event.timeSeconds() > after) continue;
            var id = state.rocket.getStaticSegments().entrySet().stream().filter(e -> SegmentRef.of(e.getValue()).equals(event.segment())).map(Map.Entry::getKey).findFirst().orElse(null);
            if (id != null) split(data, state, Set.of(id), event.id(), tick);
        }
        var earthLanding = action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == OrbitBand.SURFACE
                && state.path.navigationAborts().stream().noneMatch(abort -> abort.actionId().equals(action.id()));
        var deployment = path.samples().stream().filter(s -> s.phase() == RocketFlightPathCalculator.PathPhase.PARACHUTE).findFirst().orElse(null);
        if (earthLanding && server != null && state.landing == null
                && (path.durationSeconds() - after <= 10 || deployment != null && after >= deployment.timeSeconds())) {
            var level = server.overworld();
            if (level.hasChunkAt(new BlockPos(action.landingX(), 0, action.landingZ()))) {
                state.landing = RocketRecovery.landingPosition(level, state.rocket,
                        action.landingX() + action.landingOffsetX(), action.landingZ() + action.landingOffsetZ());
                var last = path.samples().getLast();
                var earth = system.truth().stream().filter(o -> o.id().equals(SpaceObjects.EARTH_ID)).findFirst().orElseThrow();
                var descent = deployment == null ? List.<ParachuteLanding.Point>of() : path.samples().stream()
                        .filter(s -> s.timeSeconds() >= after).map(s -> new ParachuteLanding.Point(s.timeSeconds() - after,
                                Math.max(0, Math.hypot(s.x() - earth.x(), s.y() - earth.y()) - earth.radius()), s.speedMetersPerSecond())).toList();
                if (!descent.isEmpty()) {
                    var points = new ArrayList<ParachuteLanding.Point>();
                    points.add(new ParachuteLanding.Point(0, Math.max(0, Math.hypot(state.position.x() - earth.x(), state.position.y() - earth.y()) - earth.radius()),
                            Math.hypot(state.position.vx(), state.position.vy())));
                    points.addAll(descent);
                    descent = points;
                }
                RocketSimulationController.beginReentry(level, state.rocket, state.landing,
                        Math.max(1, (long) ((path.durationSeconds() - after) * 20)), last.speedMetersPerSecond(), descent);
            }
        }
        if (after + 1e-7 < path.durationSeconds()) return;
        var completed = !path.actionMoments().isEmpty() && path.actionMoments().getLast().completed();
        if (!completed) {
            state.status = MissionState.STATUS_TRANSFER_BLOCKED;
            return;
        }
        var p = state.position;
        var slot = -1;
        if (action.targetId().equals(SpaceObjects.EARTH_ID) && SpaceBalance.hasSlots(action.orbit())
                && Math.hypot(p.vx(), p.vy()) <= 12) {
            slot = action.service().slot();
            if (slot < 0) {
                var occupied = new HashSet<Integer>();
                data.craft.values().stream().filter(c -> c.owner.equals(state.owner) && c.position.orbit() == action.orbit()).forEach(c -> occupied.add(c.position.slot()));
                slot = 0;
                while (occupied.contains(slot) && slot < SpaceBalance.slots(action.orbit()) - 1) slot++;
            }
            slot %= SpaceBalance.slots(action.orbit());
        }
        var px = p.x();
        var py = p.y();
        if (slot >= 0) {
            var angle = Math.PI + slot * Math.PI * 2 / SpaceBalance.slots(action.orbit());
            px = -3_000_000 + Math.cos(angle) * (60_000 + action.orbit().altitude());
            py = Math.sin(angle) * (60_000 + action.orbit().altitude());
        }
        state.position = new MissionState.Position(px, py, p.vx(), p.vy(), action.targetId(), action.orbit(), slot, p.stage(), p.asteroid(), p.anchor(),
                p.headingX(), p.headingY());
        if (earthLanding) {
            var visual = state.rocket.getFlight();
            if (server != null && state.landing != null && visual != null
                    && server.overworld().getGameTime() < visual.impactTick()) {
                state.status = MissionState.STATUS_DESCENDING;
                return;
            }
            if (state.landing == null) {
                state.status = MissionState.STATUS_LANDING_UNLOADED;
                return;
            }
            state.landing = RocketRecovery.landingPosition(server.overworld(), state.rocket,
                    action.landingX() + action.landingOffsetX(), action.landingZ() + action.landingOffsetZ());
            if (path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED || Math.hypot(p.vx(), p.vy()) > 12) {
                var level = server.overworld();
                var pos = state.landing;
                var energy = state.path.arrivalPredictions().stream().filter(a -> a.targetId().equals(SpaceObjects.EARTH_ID))
                        .mapToDouble(a -> a.impact().totalEnergyJoules()).findFirst().orElse(
                                .5 * RocketPerformanceCalculator.calculate(state.rocket).wetMassKilograms() * (p.vx() * p.vx() + p.vy() * p.vy())
                                        + RocketExplosives.remaining(state.rocket));
                level.explode(null, pos.getX(), pos.getY(), pos.getZ(), RocketExplosives.worldStrength(energy), Level.ExplosionInteraction.BLOCK);
                state.ended = true;
                state.status = MissionState.STATUS_DESTROYED;
            } else if (RocketRecovery.restore(server.overworld(), state.rocket, state.landing)) {
                deliverSurveyKnowledge(server, state, system);
                state.ended = true;
                state.status = MissionState.STATUS_RECOVERED;
            } else {
                state.status = MissionState.STATUS_LANDING_UNAVAILABLE;
                return;
            }
        }
        if (path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED && !earthLanding) {
            state.ended = true;
            state.status = MissionState.STATUS_DESTROYED;
            UUID debrisRegion = null;
            for (var arrival : state.path.arrivalPredictions()) {
                var created = system.applyAsteroidImpact(arrival.targetId(), arrival.impact());
                if (created != null) debrisRegion = created;
            }
            if (debrisRegion != null) {
                var position = state.position;
                state.position = new MissionState.Position(position.x(), position.y(), position.vx(), position.vy(),
                        debrisRegion, OrbitBand.SURFACE, -1, position.stage(), position.asteroid(), position.anchor(),
                        position.headingX(), position.headingY());
            }
        }
        if (earthLanding && state.ended && !p.asteroid().equals(FlightPlanAction.NO_TARGET)) {
            state.path.arrivalPredictions().stream().filter(a -> a.targetId().equals(SpaceObjects.EARTH_ID)).findFirst()
                    .ifPresent(arrival -> system.recoverAsteroid(server.overworld(), p.asteroid(), arrival.impact(), false));
        }
        state.complete();
    }

    public static MissionState.Position sample(List<RocketFlightPathCalculator.PathSample> samples, double time, MissionState.Position previous) {

        var index = FlightMotion.endIndex(samples, time);
        var sample = samples.get(index);
        var motion = FlightMotion.sample(samples, time);
        var hx = motion.vx();
        var hy = motion.vy();
        if (Math.hypot(hx, hy) < .001) {
            hx = previous.headingX();
            hy = previous.headingY();
            // Recover an arrival direction even when a tick jumps directly to a stopped endpoint.
            for (int i = index; i > 0; i--) {
                var interval = new FlightMotion(samples.get(i - 1), samples.get(i));
                if (interval.duration() <= 0) continue;
                var incoming = interval.at(.999);
                if (Math.hypot(incoming.vx(), incoming.vy()) < .000001) continue;
                hx = incoming.vx();
                hy = incoming.vy();
                break;
            }
        }
        var length = Math.max(1e-12, Math.hypot(hx, hy));
        return new MissionState.Position(motion.x(), motion.y(), motion.vx(), motion.vy(),
                previous.target(), previous.orbit(), previous.slot(), sample.stage(), sample.attachedAsteroidId(), previous.anchor(),
                hx / length, hy / length);
    }

    private static void split(MissionSavedData data, MissionState state, Set<UUID> detached, UUID action, long tick) {

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
        var attachment = state.leg == null ? position : state.leg.position();
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
        if (!unchangedCurrent) {
            state.actionTicks = 0;
            state.serviceProgress = 0;
            state.legStations = List.of();
            state.leg = null;
            state.path = null;
        }
        state.status = MissionState.STATUS_UPDATE_ACCEPTED;
        return true;
    }

    private static void deliverSurveyKnowledge(MinecraftServer server, MissionState state, SpaceSimulation system) {

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
