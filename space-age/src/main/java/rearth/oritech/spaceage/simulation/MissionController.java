package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import rearth.oritech.spaceage.network.MissionNetworking;
import java.util.*;
import static rearth.oritech.spaceage.simulation.SpaceSimulation.*;

/** Tick actual missions; predictions use the same transfer compiler but never execute service effects. */
public final class MissionController {
    public static void launch(ServerLevel level, UUID owner, ActiveRocketData rocket, FlightPlan plan, BlockPos launch) {
        rocket.setLaunchPosition(launch);
        var system = SpaceSimulationSavedData.get(level.getServer()).getOrCreate(owner);
        plan = resolveOrbitSlots(plan, MissionSavedData.get(level.getServer()), owner, rocket.getRocketId());
        var objects = new HashMap<UUID, SpaceObjectData>(); system.createObjectData().forEach(o -> objects.put(o.id(), o));
        var initial = RocketFlightPathCalculator.createInitialCraft(rocket, objects.get(SpaceObjects.EARTH_ID), plan.root(), objects);
        var position = new MissionState.Position(initial.x, initial.y, 0, 0, SpaceObjects.EARTH_ID, OrbitBand.SURFACE,
                -1, 1, FlightPlanAction.NO_TARGET, new SegmentRef(BlockPos.ZERO));
        var state = new MissionState(owner, rocket, plan, position, system.earthKnowledge.copy(), launch, missionTime(level.getServer()));
        var data = MissionSavedData.get(level.getServer()); data.craft.put(rocket.getRocketId(), state); data.receive(state); predictSeparations(data, state, system.createObjectData(), missionTime(level.getServer())); data.setDirty();
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
        var nodes = SpaceCommunications.update(server, data);
        for (var state : List.copyOf(data.craft.values())) {
            if (state.ended) continue;
            var system = SpaceSimulationSavedData.get(server).getOrCreate(state.owner);
            step(server, data, state, system, tick);
            if (!state.ended && !state.position.asteroid().equals(FlightPlanAction.NO_TARGET))
                system.moveAttached(state.position.asteroid(), state.position.x(), state.position.y());
            if (state.connected || MissionState.isRecovered(state.status)) { state.report(tick); data.receive(state); }
        }
        shareFleetKnowledge(data, nodes);
        SpaceSimulationSavedData.get(server).setDirty();
        if (!data.craft.isEmpty()) data.setDirty();
    }

    /** Isolated fleets can cooperate. Ground delivery remains explicit. */
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
        if (action == null || action.type() != ActionType.NAVIGATE_TO && action.type() != ActionType.MAINTAIN_POSITION) {
            var p = state.position;
            state.position = new MissionState.Position(p.x() + p.vx() / 20, p.y() + p.vy() / 20, p.vx(), p.vy(),
                    p.target(), p.orbit(), p.slot(), p.stage(), p.asteroid(), p.anchor(), p.headingX(), p.headingY());
        }
        if (action == null) { state.status = MissionState.STATUS_COMPLETE; return; }
        switch (action.type()) {
            case SCAN -> scan(state, system, action, tick);
            case TRANSMIT_INFORMATION -> transmit(server, state, system, action);
            case RELAY -> relay(state, action);
            case NAVIGATE_TO -> navigate(server, data, state, system, action, tick);
            case CONNECT_ASTEROID -> connectAsteroid(data, state, system, action);
            case DECOUPLE -> decouple(data, state, system, action, tick);
            case MAINTAIN_POSITION -> maintainPosition(state, system, action);
            case DISCARD_CRAFT -> { state.ended = true; state.status = MissionState.STATUS_DISCARDED; }
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
        boolean scannerInstalled = false;
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
        long remaining = SpaceBalance.SCAN_RF;
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

    private static void relay(MissionState state, FlightPlanAction action) {
        if (SpaceCommunications.poweredAntennas(state, false) == 0) {
            state.status = MissionState.STATUS_RELAY_WAITING;
            return;
        }
        state.status = MissionState.STATUS_RELAYING;
        if (++state.actionTicks >= action.service().durationTicks()) state.complete();
    }

    private static void connectAsteroid(MissionSavedData data, MissionState state, SpaceSimulation system,
                                        FlightPlanAction action) {
        var target = system.truth().stream().filter(object -> object.id().equals(action.targetId()))
                .findFirst().orElse(null);
        boolean alreadyAttached = data.craft.values().stream().anyMatch(other -> other != state && !other.ended
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

    private static void maintainPosition(MissionState state, SpaceSimulation system, FlightPlanAction action) {
        if (maintainPositionConditionReached(state, action)) {
            state.complete();
            state.status = MissionState.STATUS_READY;
            return;
        }
        var performance = RocketPerformanceCalculator.calculate(state.rocket);
        boolean hasAntenna = state.rocket.getStaticSegments().values().stream()
                .anyMatch(segment -> RocketHardware.of(segment).antennas() > 0);
        boolean outOfCommunicationRF = hasAntenna && SpaceCommunications.poweredAntennas(state, false) == 0;
        if (performance.availableBurnSeconds() <= 0 || outOfCommunicationRF) {
            state.complete();
            state.status = MissionState.STATUS_READY;
            return;
        }
        if (++state.actionTicks % SpaceBalance.STATION_KEEPING_INTERVAL_TICKS == 0) {
            double mass = performance.wetMassKilograms();
            if (!state.position.asteroid().equals(FlightPlanAction.NO_TARGET)) {
                mass += system.truth().stream()
                        .filter(object -> object.id().equals(state.position.asteroid()))
                        .mapToDouble(object -> object.mass() * AsteroidImpactRules.KILOGRAMS_PER_ASTEROID_MASS)
                        .findFirst().orElse(0);
            }
            double correctionSeconds = SpaceBalance.stationKeepingBurnSecondsPerSecond(
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
        if (state.leg == null) {
            // Older/incomplete saves may have elapsed transfer ticks without the matching start snapshot.
            // Restart guidance from the persisted current position instead of sampling past a newly built path.
            state.actionTicks = 0;
            state.leg = new MissionState.Telemetry(MissionState.copyRocket(state.rocket), state.plan, state.position, tick, MissionState.STATUS_TRANSFER);
            state.legObjects = system.knownObjects(state.knowledge);
            var p = state.position;
            state.position = new MissionState.Position(p.x(), p.y(), p.vx(), p.vy(), p.target(), p.orbit(), -1, p.stage(), p.asteroid(), p.anchor(), p.headingX(), p.headingY());
        }
        if (state.path == null) {
            if (state.legObjects.isEmpty()) state.legObjects = system.knownObjects(state.knowledge);
            var root = state.plan.root().withActions(List.of(action));
            var branches = new ArrayList<>(state.plan.branches()); branches.removeIf(FlightPlanBranch::isRoot); branches.addFirst(root);
            state.path = RocketFlightPathCalculator.calculateFrom(state.leg.rocket(), state.legObjects,
                    state.plan.withBranches(branches), state.leg.position());
        }
        var path = state.path.paths().stream().filter(p -> p.branchId().equals(state.plan.root().id())).findFirst().orElse(null);
        if (path == null || path.samples().isEmpty()) { state.status = MissionState.STATUS_NO_FEASIBLE_TRANSFER; return; }
        var before = state.actionTicks / 20.0; var after = Math.min(path.durationSeconds(), before + 0.05);
        var samples = path.samples();
        for (int index = 1; index < samples.size(); index++) {
            var previous = samples.get(index - 1); var next = samples.get(index);
            double duration = Math.max(0, Math.min(after, next.timeSeconds()) - Math.max(before, previous.timeSeconds()));
            if (duration <= 0) continue;
            boolean depleted = state.rocket.getStaticSegments().entrySet().stream().anyMatch(entry -> {
                var h = RocketHardware.of(entry.getValue()); var r = state.rocket.getDynamicSegments().get(entry.getKey());
                return next.firingSegments().contains(SegmentRef.of(entry.getValue())) && h.ion() > 0 && r.availableRF < duration * 20 * h.ion() * SpaceBalance.ION_RF
                        && state.leg.rocket().getDynamicSegments().get(entry.getKey()).availableRF > 0;
            });
            if (depleted) { state.leg = null; state.path = null; state.actionTicks = 0; state.status = MissionState.STATUS_REPLANNING; return; }
            state.rocket.getStaticSegments().forEach((id, segment) -> {
                if (next.firingSegments().contains(SegmentRef.of(segment))) RocketHardware.of(segment).burn(state.rocket.getDynamicSegments().get(id), duration);
            });
        }
        state.position = sample(samples, before + 0.05, state.position);
        state.actionTicks++; state.status = MissionState.STATUS_IN_FLIGHT;
        for (var event : state.path.boosterEvents()) {
            if (!event.branchId().equals(state.plan.root().id()) || event.timeSeconds() > after) continue;
            var id = state.rocket.getStaticSegments().entrySet().stream().filter(e -> SegmentRef.of(e.getValue()).equals(event.segment())).map(Map.Entry::getKey).findFirst().orElse(null);
            if (id != null) split(data, state, Set.of(id), event.id(), tick);
        }
        var earthLanding = action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == OrbitBand.SURFACE
                && state.path.navigationAborts().stream().noneMatch(abort -> abort.actionId().equals(action.id()));
        if (earthLanding && server != null && state.landing == null && path.durationSeconds() - after <= 10) {
            var level = server.overworld();
            if (level.hasChunkAt(new BlockPos(action.landingX(), 0, action.landingZ()))) {
                state.landing = RocketRecovery.landingPosition(level, state.rocket,
                        action.landingX() + action.landingOffsetX(), action.landingZ() + action.landingOffsetZ());
                var last = path.samples().getLast();
                RocketSimulationController.beginReentry(level, state.rocket, state.landing,
                        Math.max(1, (long) ((path.durationSeconds() - after) * 20)), last.speedMetersPerSecond());
            }
        }
        if (after + 1e-7 < path.durationSeconds()) return;
        var completed = !path.actionMoments().isEmpty() && path.actionMoments().getLast().completed();
        if (!completed) { state.status = MissionState.STATUS_TRANSFER_BLOCKED; return; }
        var p = state.position;
        int slot = -1;
        if (action.targetId().equals(SpaceObjects.EARTH_ID) && SpaceBalance.hasSlots(action.orbit())
                && Math.hypot(p.vx(), p.vy()) <= 12) {
            slot = action.service().slot();
            if (slot < 0) {
                var occupied = new HashSet<Integer>();
                data.craft.values().stream().filter(c -> c.owner.equals(state.owner) && c.position.orbit() == action.orbit()).forEach(c -> occupied.add(c.position.slot()));
                slot = 0; while (occupied.contains(slot) && slot < SpaceBalance.slots(action.orbit()) - 1) slot++;
            }
            slot %= SpaceBalance.slots(action.orbit());
        }
        var px = p.x(); var py = p.y();
        if (slot >= 0) {
            var angle = Math.PI + slot * Math.PI * 2 / SpaceBalance.slots(action.orbit());
            px = -3_000_000 + Math.cos(angle) * (60_000 + action.orbit().altitude());
            py = Math.sin(angle) * (60_000 + action.orbit().altitude());
        }
        state.position = new MissionState.Position(px, py, p.vx(), p.vy(), action.targetId(), action.orbit(), slot, p.stage(), p.asteroid(), p.anchor(), p.headingX(), p.headingY());
        if (earthLanding) {
            var visual = state.rocket.getFlight();
            if (server != null && state.landing != null && visual != null
                    && server.overworld().getGameTime() < visual.impactTick()) {
                state.status = MissionState.STATUS_DESCENDING;
                return;
            }
            if (state.landing == null) { state.status = MissionState.STATUS_LANDING_UNLOADED; return; }
            state.landing = RocketRecovery.landingPosition(server.overworld(), state.rocket,
                    action.landingX() + action.landingOffsetX(), action.landingZ() + action.landingOffsetZ());
            if (path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED || Math.hypot(p.vx(), p.vy()) > 12) {
                var level = server.overworld(); var pos = state.landing;
                level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 6, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
                state.ended = true; state.status = MissionState.STATUS_DESTROYED;
            } else if (RocketRecovery.restore(server.overworld(), state.rocket, state.landing)) {
                deliverSurveyKnowledge(server, state, system); state.ended = true; state.status = MissionState.STATUS_RECOVERED;
                state.report(tick);
            } else { state.status = MissionState.STATUS_LANDING_UNAVAILABLE; return; }
        }
        if (path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED && !earthLanding) {
            state.ended = true; state.status = MissionState.STATUS_DESTROYED;
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
                    .ifPresent(arrival -> system.recoverAsteroid(server.overworld(), p.asteroid(), arrival.impact()));
        }
        state.complete();
    }
    public static MissionState.Position sample(List<RocketFlightPathCalculator.PathSample> samples, double time, MissionState.Position previous) {
        int index = FlightMotion.endIndex(samples, time);
        var sample = samples.get(index);
        var motion = FlightMotion.sample(samples, time);
        double hx = motion.vx(), hy = motion.vy();
        if (Math.hypot(hx, hy) < .001) {
            hx = previous.headingX(); hy = previous.headingY();
            // Recover an arrival direction even when a tick jumps directly to a stopped endpoint.
            for (int i = index; i > 0; i--) {
                var interval = new FlightMotion(samples.get(i - 1), samples.get(i));
                if (interval.duration() <= 0) continue;
                var incoming = interval.at(.999);
                if (Math.hypot(incoming.vx(), incoming.vy()) < .000001) continue;
                hx = incoming.vx(); hy = incoming.vy();
                break;
            }
        }
        var length = Math.max(1e-12, Math.hypot(hx, hy));
        return new MissionState.Position(motion.x(), motion.y(), motion.vx(), motion.vy(),
                previous.target(), previous.orbit(), previous.slot(), sample.stage(), sample.attachedAsteroidId(), previous.anchor(),
                hx / length, hy / length);
    }
    private static void split(MissionSavedData data, MissionState state, Set<UUID> detached, UUID action, long tick) {
        var childStatic = new HashMap<UUID, StaticRocketSegment>(); var childDynamic = new HashMap<UUID, DynamicRocketSegment>();
        var parentStatic = new HashMap<>(state.rocket.getStaticSegments()); var parentDynamic = new HashMap<>(state.rocket.getDynamicSegments());
        detached.forEach(id -> { childStatic.put(id, parentStatic.remove(id)); childDynamic.put(id, parentDynamic.remove(id)); });
        childDynamic.values().forEach(r -> r.getConnectedSegments().retainAll(detached));
        parentDynamic.values().forEach(r -> r.getConnectedSegments().removeAll(detached));
        var child = state.plan.branches().stream().filter(b -> b.parentSeparationAction().equals(action)).findFirst().orElse(new FlightPlanBranch(UUID.randomUUID(), action, List.of()));
        var childBranches = new ArrayList<FlightPlanBranch>(); childBranches.add(new FlightPlanBranch(child.id(), FlightPlanBranch.NO_PARENT, child.actions()));
        state.plan.branches().stream().filter(b -> !b.isRoot() && !b.id().equals(child.id())).forEach(childBranches::add);
        var position = state.position;
        var attachment = state.leg == null ? position : state.leg.position();
        boolean takesAsteroid = childStatic.values().stream().anyMatch(s -> SegmentRef.of(s).equals(attachment.anchor()));
        var childPosition = new MissionState.Position(position.x(), position.y(), position.vx(), position.vy(), position.target(), position.orbit(),
                position.slot(), position.stage(), takesAsteroid ? attachment.asteroid() : FlightPlanAction.NO_TARGET, attachment.anchor(), position.headingX(), position.headingY());
        if (takesAsteroid) state.position = new MissionState.Position(position.x(), position.y(), position.vx(), position.vy(), position.target(), position.orbit(),
                position.slot(), position.stage(), FlightPlanAction.NO_TARGET, attachment.anchor(), position.headingX(), position.headingY());
        var rocket = new ActiveRocketData(child.id(), childStatic, childDynamic, null);
        var childRefs = childStatic.values().stream().map(SegmentRef::of).toList();
        var childSettings = state.plan.segmentConfigurations().stream().filter(c -> childRefs.contains(c.segment())).toList();
        var mission = new MissionState(state.owner, rocket, new FlightPlan(childBranches, childSettings, state.plan.name()), childPosition, state.knowledge.copy(), state.launch, tick);
        var contact = data.contacts.get(rocket.getRocketId());
        if (contact != null) mission.earth = contact.telemetry();
        // Earth already has the forecast; executing a separation does not send telemetry.
        data.craft.put(rocket.getRocketId(), mission);
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
            int slot = 0;
            while (used.contains(slot) && slot < SpaceBalance.slots(action.orbit()) - 1) slot++;
            used.add(slot);
            var settings = action.service();
            return action.withService(new ServiceSettings(settings.durationTicks(), settings.timeoutTicks(), slot));
        }).toList())).toList());
    }

    public static void predictSeparations(MissionSavedData data, MissionState state, List<SpaceObjectData> objects, long tick) {
        var forecast = RocketFlightPathCalculator.calculateFrom(state.rocket, objects, state.plan, state.position);
        for (var path : forecast.paths()) {
            if (path.branchId().equals(state.plan.root().id()) || path.samples().isEmpty() || data.contacts.containsKey(path.branchId())) continue;
            var first = path.samples().getFirst();
            var statics = new HashMap<UUID, StaticRocketSegment>(); var dynamics = new HashMap<UUID, DynamicRocketSegment>();
            var copy = MissionState.copyRocket(state.rocket);
            copy.getStaticSegments().forEach((id, segment) -> {
                if (first.connectedSegments().contains(SegmentRef.of(segment))) {
                    statics.put(id, segment); dynamics.put(id, copy.getDynamicSegments().get(id));
                }
            });
            for (var parent : forecast.paths()) for (int i = 1; i < parent.samples().size(); i++) {
                var before = parent.samples().get(i - 1); var after = parent.samples().get(i);
                var seconds = Math.max(0, Math.min(first.timeSeconds(), after.timeSeconds()) - before.timeSeconds());
                statics.forEach((id, segment) -> {
                    if (after.firingSegments().contains(SegmentRef.of(segment))) RocketHardware.of(segment).burn(dynamics.get(id), seconds);
                });
            }
            dynamics.values().forEach(r -> r.getConnectedSegments().retainAll(statics.keySet()));
            var rocket = new ActiveRocketData(path.branchId(), statics, dynamics, null);
            var branch = state.plan.branches().stream().filter(b -> b.id().equals(path.branchId())).findFirst()
                    .orElse(new FlightPlanBranch(path.branchId(), FlightPlanBranch.NO_PARENT, List.of()));
            var branches = new ArrayList<FlightPlanBranch>();
            branches.add(new FlightPlanBranch(branch.id(), FlightPlanBranch.NO_PARENT, branch.actions()));
            state.plan.branches().stream().filter(b -> !b.isRoot() && !b.id().equals(branch.id())).forEach(branches::add);
            var plan = new FlightPlan(branches, state.plan.segmentConfigurations(), state.plan.name());
            var position = sample(List.of(first), first.timeSeconds(), state.position);
            var telemetry = new MissionState.Telemetry(rocket, plan, position, tick + (long) (first.timeSeconds() * 20), MissionState.STATUS_PLANNED_SEPARATION);
            data.contacts.put(rocket.getRocketId(), new MissionSavedData.Contact(state.owner, telemetry));
        }
    }

    public static boolean replace(MissionState state, FlightPlan plan, SpaceSimulation system) {
        if (!state.connected || state.ended) return false;
        // Stale editor drafts must not replay actions which completed while the player was editing.
        var finished = state.completed.stream().map(FlightPlanAction::id).collect(java.util.stream.Collectors.toSet());
        plan = plan.withBranches(plan.branches().stream().map(branch -> branch.withActions(branch.actions().stream()
                .filter(action -> !finished.contains(action.id())).toList())).toList());
        state.knowledge.merge(system.earthKnowledge);
        var validated = RocketFlightPlanRules.validateInFlight(plan, state.rocket, system.knownObjects(state.knowledge));
        if (validated == null || validated.branches().stream().mapToInt(b -> b.actions().size()).sum() != plan.branches().stream().mapToInt(b -> b.actions().size()).sum()) return false;
        var current = state.action();
        boolean unchangedCurrent = current != null && !validated.root().actions().isEmpty()
                && current.equals(validated.root().actions().getFirst());
        state.plan = validated;
        if (!unchangedCurrent) { state.actionTicks = 0; state.leg = null; state.path = null; }
        state.status = MissionState.STATUS_UPDATE_ACCEPTED;
        return true;
    }

    private static void deliverSurveyKnowledge(MinecraftServer server, MissionState state, SpaceSimulation system) {
        var before = new HashMap<UUID, SpaceObjectData>();
        system.earthKnowledge.contacts().forEach(contact -> before.put(contact.id(), contact));
        system.earthKnowledge.merge(state.knowledge);
        int discovered = 0;
        int updated = 0;
        for (var contact : system.earthKnowledge.contacts()) {
            var previous = before.get(contact.id());
            if (previous == null) discovered++;
            else if (!previous.equals(contact)) updated++;
        }
        if (server != null) MissionNetworking.sendSurveyResults(server, state.owner, discovered, updated);
    }
    private MissionController() { }
}
