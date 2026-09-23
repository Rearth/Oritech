package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.simulation.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Segment;

import java.nio.charset.StandardCharsets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Compiles high-level navigation actions into a bounded two-dimensional flight preview.
 * The guidance simulation is deliberately small, but it keeps velocity continuous between navigation actions.
 */
public final class RocketFlightPathCalculator {

    private static final Codec<Set<SpaceSimulation.SegmentRef>> SEGMENT_SET_CODEC = SpaceSimulation.SegmentRef.CODEC.listOf().xmap(Set::copyOf, List::copyOf);

    private RocketFlightPathCalculator() {
    }

    public static FlightPath calculate(ActiveRocketData rocket,
                                       List<SpaceSimulation.SpaceObjectData> objects,
                                       SpaceSimulation.FlightPlan plan) {
        return calculateFrom(rocket, objects, plan, null);
    }

    public static FlightPath calculateFrom(ActiveRocketData rocket, List<SpaceSimulation.SpaceObjectData> objects,
                                    SpaceSimulation.FlightPlan plan, MissionState.Position position) {
        var objectsById = new HashMap<UUID, SpaceSimulation.SpaceObjectData>();
        objects.forEach(object -> objectsById.put(object.id(), object));
        var earth = objectsById.get(SpaceObjects.EARTH_ID);
        if (earth == null) return new FlightPath(List.of(), List.of(), 0);

        var branchesByParent = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        plan.branches().stream().filter(branch -> !branch.isRoot())
                .forEach(branch -> branchesByParent.put(branch.parentSeparationAction(), branch));
        var configurations = new HashMap<SpaceSimulation.SegmentRef, SpaceSimulation.SegmentConfiguration>();
        rocket.getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of)
                .forEach(ref -> configurations.put(ref, plan.configurationFor(ref)));

        var context = new Context(objectsById, branchesByParent, configurations,
                RocketFlightPlanRules.stageCount(plan, rocket.getStaticSegments().size()));
        var initial = createInitialCraft(rocket, earth, plan.root(), objectsById);
        if (position != null) {
            initial.x = position.x(); initial.y = position.y();
            initial.headingX = position.headingX(); initial.headingY = position.headingY();
            initial.velocityX = position.vx(); initial.velocityY = position.vy();
            initial.currentTarget = position.target(); initial.currentOrbit = position.orbit();
            initial.currentStage = position.stage();
            initial.attachedAsteroid = objectsById.get(position.asteroid());
            initial.asteroidAnchor = position.anchor();
        }
        simulateBranch(plan.root(), initial, context);

        var orderedPaths = new ArrayList<CraftPath>();
        for (var branch : plan.branches()) {
            var path = context.paths.remove(branch.id());
            if (path != null) orderedPaths.add(path);
        }
        orderedPaths.addAll(context.paths.values());
        double lastCommand = orderedPaths.stream().flatMap(path -> path.actionMoments().stream())
                .mapToDouble(ActionMoment::timeSeconds).max().orElse(0);
        return new FlightPath(orderedPaths, List.copyOf(context.boosterEvents), lastCommand,
                List.copyOf(context.arrivalPredictions), List.copyOf(context.asteroidPaths),
                List.copyOf(context.navigationAborts), List.copyOf(context.scanEstimates),
                List.copyOf(context.stationKeepingEstimates));
    }

    static Craft createInitialCraft(ActiveRocketData rocket,
                                                  SpaceSimulation.SpaceObjectData earth,
                                                  SpaceSimulation.FlightPlanBranch root,
                                                  Map<UUID, SpaceSimulation.SpaceObjectData> objects) {
        var refsById = new HashMap<UUID, SpaceSimulation.SegmentRef>();
        rocket.getStaticSegments().forEach((id, segment) -> refsById.put(id, SpaceSimulation.SegmentRef.of(segment)));
        var segments = new LinkedHashMap<SpaceSimulation.SegmentRef, Segment>();
        var connections = new HashMap<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>>();
        for (var entry : rocket.getStaticSegments().entrySet()) {
            var ref = refsById.get(entry.getKey());
            var dynamic = rocket.getDynamicSegments().get(entry.getKey());
            var segmentRocket = new ActiveRocketData(Map.of(entry.getKey(), entry.getValue()),
                    Map.of(entry.getKey(), dynamic));
            var performance = RocketPerformanceCalculator.calculate(segmentRocket);
            var hardware = RocketHardware.of(entry.getValue());
            var previewSegment = new Segment(performance.wetMassKilograms(),
                    hardware.chemical() * RocketPerformanceCalculator.ENGINE_THRUST_NEWTONS,
                    hardware.ion() * RocketPerformanceCalculator.ENGINE_THRUST_NEWTONS,
                    hardware.chemicalSeconds(dynamic), hardware.ionSeconds(dynamic), hardware);
            previewSegment.rf = dynamic.availableRF;
            previewSegment.initialRF = entry.getValue().initialRF();
            previewSegment.initialFuelTicks = entry.getValue().initialFuel();
            segments.put(ref, previewSegment);

            var neighbours = new LinkedHashSet<SpaceSimulation.SegmentRef>();
            dynamic.getConnectedSegments().stream().map(refsById::get).filter(java.util.Objects::nonNull)
                    .forEach(neighbours::add);
            connections.put(ref, neighbours);
        }

        var launch = rocket.getLaunchPosition();
        var angle = SpaceBalance.surfaceAngle(launch.getX(), launch.getZ());
        var craft = new Craft(segments, connections, earth.x() + Math.cos(angle) * earth.radius(),
                earth.y() + Math.sin(angle) * earth.radius(), 0);
        craft.currentTarget = earth.id();
        craft.currentOrbit = SpaceSimulation.OrbitBand.SURFACE;
        return craft;
    }

    private static void simulateBranch(SpaceSimulation.FlightPlanBranch branch, Craft state,
                                       Context context) {
        state.branchId = branch.id();
        state.addSample(PathPhase.COAST, SpaceSimulation.FlightPlanAction.NO_TARGET,
                SpaceSimulation.FlightPlanAction.NO_TARGET);
        boolean completed = true;

        for (int index = 0; index < branch.actions().size(); index++) {
            var action = branch.actions().get(index);
            if (action.type() == SpaceSimulation.ActionType.DISCONNECT_BOOSTER) continue;
            completed = switch (action.type()) {
                case SCAN, RELAY, TRANSMIT_INFORMATION -> service(action, state, context);
                case NAVIGATE_TO -> RocketFlightNavigation.navigate(action, state, context);
                case CONNECT_ASTEROID -> connectAsteroid(action, state, context);
                case DECOUPLE -> separate(action, state, context);
                case MAINTAIN_POSITION -> {
                    maintainPosition(action, state, context);
                    state.maintainingPosition = false;
                    yield true;
                }
                case DISCARD_CRAFT -> {
                    state.discarded = true;
                    yield true;
                }
                case DISCONNECT_BOOSTER -> true;
            };
            state.actionMoments.add(new ActionMoment(branch.id(), index, action.id(), state.time,
                    state.x, state.y, completed, Set.copyOf(state.segments.keySet()),
                    state.attachedAsteroid == null ? SpaceSimulation.FlightPlanAction.NO_TARGET
                            : state.attachedAsteroid.id()));
            if (!completed || state.maintainingPosition || state.discarded || state.destroyed) break;
        }

        var terminal = state.destroyed ? TerminalState.DESTROYED
                : state.discarded ? TerminalState.DISCARDED
                : state.maintainingPosition ? TerminalState.STATION_KEEPING_EXHAUSTED
                : completed ? TerminalState.READY : state.blockedState;
        context.paths.put(branch.id(), state.toPath(terminal, context));
    }

    private static boolean service(SpaceSimulation.FlightPlanAction action, Craft state, Context context) {
        if (action.type() == SpaceSimulation.ActionType.SCAN) return scan(action, state, context);
        double remainingTicks = action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION
                ? Math.max(1, action.service().timeoutTicks()) : action.service().durationTicks();
        var cost = SpaceBalance.ANTENNA_RF;
        boolean installed = state.segments.values().stream().anyMatch(segment -> segment.hardware.antennas() > 0);
        if (!installed) {
            state.blockedState = TerminalState.NO_ANTENNA;
            return false;
        }
        // Spend local RF until the action finishes or the next module bank runs out.
        // Different banks may run for different durations; a short-lived bank still contributes.
        while (remainingTicks > 0.0001) {
            int modules = 0;
            double step = Double.POSITIVE_INFINITY;
            for (var segment : state.segments.values()) {
                var count = segment.hardware.antennas();
                if (count == 0 || segment.rf < cost) continue;
                modules += count;
                step = Math.min(step, segment.rf / (count * (double) cost));
            }
            if (modules == 0) {
                state.blockedState = TerminalState.NOT_ENOUGH_SERVICE_RF;
                return false;
            }
            step = Math.min(step, remainingTicks);
            for (var segment : state.segments.values()) {
                var count = segment.hardware.antennas();
                if (count > 0 && segment.rf >= cost) segment.spendRF(count * cost * step);
            }
            remainingTicks -= step;
            state.time += step / 20;
            state.x += state.velocityX * step / 20;
            state.y += state.velocityY * step / 20;
        }
        state.addSample(PathPhase.COAST, state.currentTarget, action.id());
        return true;
    }

    private static boolean scan(SpaceSimulation.FlightPlanAction action, Craft state, Context context) {
        var target = context.objects.get(action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)
                ? state.currentTarget : action.targetId());
        int scanners = state.segments.values().stream().mapToInt(segment -> segment.hardware.scanners()).sum();
        var available = state.segments.values().stream().filter(segment -> segment.hardware.scanners() > 0)
                .mapToDouble(segment -> segment.rf).sum();
        context.scanEstimates.add(new ScanEstimate(action.id(), (long) Math.floor(available), scanners));
        if (scanners == 0) {
            state.blockedState = TerminalState.NO_SCANNER;
            return false;
        }
        if (target == null || target.type() != SpaceObjects.ObjectType.SURVEY_REGION) {
            state.blockedState = TerminalState.NO_SCAN_TARGET;
            return false;
        }
        if (SurveyRules.surveyDistance(target, state.x, state.y) > SpaceBalance.SCAN_RANGE) {
            state.blockedState = TerminalState.SCAN_OUT_OF_RANGE;
            return false;
        }
        if (available < SpaceBalance.SCAN_RF) {
            state.blockedState = TerminalState.NOT_ENOUGH_SERVICE_RF;
            return false;
        }
        double remaining = SpaceBalance.SCAN_RF;
        for (var segment : state.segments.values()) {
            if (segment.hardware.scanners() == 0) continue;
            var spent = Math.min(remaining, segment.rf);
            segment.spendRF(spent);
            remaining -= spent;
            if (remaining <= 0) break;
        }
        return true;
    }

    private static void maintainPosition(SpaceSimulation.FlightPlanAction action, Craft state, Context context) {
        var thrust = state.segments.values().stream().mapToDouble(segment -> segment.thrust(false)).sum();
        var correctionBurnSecondsPerSecond = SpaceBalance.stationKeepingBurnSecondsPerSecond(thrust, state.mass());
        var addon = action.addons().stream().filter(item -> item.type().isMaintainPositionCondition())
                .findFirst().orElse(null);
        double chemicalSeconds = 0;
        double ionSeconds = 0;
        double antennaSeconds = Double.POSITIVE_INFINITY;
        boolean hasAntenna = false;
        for (var segment : state.segments.values()) {
            if (segment.hardware.chemical() > 0 && segment.chemicalSeconds > 0)
                chemicalSeconds = Math.max(chemicalSeconds, segment.chemicalSeconds / correctionBurnSecondsPerSecond);
            if (segment.hardware.ion() > 0 && segment.rf > 0) {
                var rate = stationKeepingRfPerSecond(segment, correctionBurnSecondsPerSecond);
                if (rate > 0) ionSeconds = Math.max(ionSeconds, segment.rf / rate);
            }
            if (segment.hardware.antennas() > 0) {
                if (!hasAntenna) antennaSeconds = 0;
                hasAntenna = true;
                var rate = stationKeepingRfPerSecond(segment, correctionBurnSecondsPerSecond);
                if (rate > 0) antennaSeconds = Math.max(antennaSeconds, segment.rf / rate);
            }
        }
        double propulsionSeconds = Math.max(chemicalSeconds, ionSeconds);
        double automaticSeconds = Math.min(propulsionSeconds, antennaSeconds);
        var automaticEnd = antennaSeconds <= propulsionSeconds || ionSeconds > chemicalSeconds
                ? StationKeepingEnd.RF : StationKeepingEnd.FUEL;
        double conditionSeconds = addon == null ? Double.POSITIVE_INFINITY
                : maintainPositionConditionSeconds(state, addon, correctionBurnSecondsPerSecond);
        boolean conditionReached = conditionSeconds + 1e-7 < automaticSeconds;
        double duration = conditionReached ? conditionSeconds : automaticSeconds;
        var end = conditionReached
                ? addon.type() == SpaceSimulation.ActionAddonType.LOW_RF
                    ? StationKeepingEnd.LOW_RF : StationKeepingEnd.LOW_FUEL
                : automaticEnd;
        context.stationKeepingEstimates.add(new StationKeepingEstimate(action.id(), duration, end));
        state.velocityX = 0;
        state.velocityY = 0;
        state.addSample(PathPhase.COAST, state.currentTarget, action.id());
        if (duration > 0 && Double.isFinite(duration)) {
            state.time += duration;
            state.addSample(PathPhase.COAST, state.currentTarget, action.id());
        }
        state.segments.values().forEach(segment -> {
            if (segment.hardware.chemical() > 0)
                segment.chemicalSeconds = Math.max(0, segment.chemicalSeconds - duration * correctionBurnSecondsPerSecond);
            segment.spendRF(duration * stationKeepingRfPerSecond(segment, correctionBurnSecondsPerSecond));
        });
        if (addon != null && conditionReached) {
            context.navigationAborts.add(new NavigationAbortMoment(action.id(), state.branchId, addon.id(), addon.value(),
                    state.time, state.x, state.y, state.x, state.y));
        }
    }

    private static double stationKeepingRfPerSecond(Segment segment, double correctionRate) {
        return segment.hardware.antennas() * SpaceBalance.ANTENNA_RF * 20.0
                + segment.hardware.ion() * SpaceBalance.ION_RF * 20.0 * correctionRate;
    }

    private static double maintainPositionConditionSeconds(Craft state, SpaceSimulation.ActionAddon addon,
                                                            double correctionRate) {
        boolean rf = addon.type() == SpaceSimulation.ActionAddonType.LOW_RF;
        var relevant = state.segments.values().stream().filter(segment -> rf
                ? segment.hardware.usesStationKeepingRF()
                : segment.hardware.usesStationKeepingFuel()).toList();
        double initial = relevant.stream().mapToDouble(segment ->
                rf ? segment.initialRF : segment.initialFuelTicks).sum();
        double target = initial * addon.value() / 100;
        java.util.function.DoubleUnaryOperator remaining = seconds -> relevant.stream().mapToDouble(segment -> {
            double current = rf ? segment.rf : segment.chemicalSeconds * segment.hardware.chemical() * 20;
            double rate = rf ? stationKeepingRfPerSecond(segment, correctionRate)
                    : segment.hardware.chemical() * 20.0 * correctionRate;
            return Math.max(0, current - rate * seconds);
        }).sum();
        if (remaining.applyAsDouble(0) <= target) return 0;
        double high = 1;
        while (high < 1e12 && remaining.applyAsDouble(high) > target) high *= 2;
        if (remaining.applyAsDouble(high) > target) return Double.POSITIVE_INFINITY;
        double low = 0;
        for (int i = 0; i < 64; i++) {
            double middle = (low + high) * .5;
            if (remaining.applyAsDouble(middle) <= target) high = middle;
            else low = middle;
        }
        return high;
    }

    static boolean finishStage(SpaceSimulation.FlightPlanAction navigation, Craft state,
                                       Context context) {
        var canAdvance = state.currentStage < context.stageCount;
        if (!state.stageFinished(context) && !(canAdvance && state.activeSegments(context).isEmpty())) return false;
        var finishedStage = state.currentStage;
        var detachedBoosters = state.boostersEndingCurrentStage(context);
        for (var booster : detachedBoosters) disconnectBooster(navigation, booster, state, context, finishedStage);
        if (!canAdvance) return !detachedBoosters.isEmpty();
        state.currentStage++;
        return true;
    }

    /** Attach an asteroid only after the preceding arrival established a slow, close approach. */
    private static boolean connectAsteroid(SpaceSimulation.FlightPlanAction action, Craft state,
                                           Context context) {
        var asteroid = context.objects.get(action.targetId());
        var relativeSpeed = asteroid == null ? Double.POSITIVE_INFINITY
                : Math.hypot(state.velocityX - asteroid.velocityX(), state.velocityY - asteroid.velocityY());
        if (asteroid == null || asteroid.type() != SpaceObjects.ObjectType.ASTEROID) {
            state.blockedState = TerminalState.TARGET_UNAVAILABLE;
            return false;
        }
        if (asteroid.detectionState() != SpaceObjects.DetectionState.PRECISE) {
            state.blockedState = TerminalState.PRECISE_POSITION_REQUIRED;
            return false;
        }
        if (state.attachedAsteroid != null) {
            state.blockedState = TerminalState.ASTEROID_ALREADY_ATTACHED;
            return false;
        }
        if (!asteroid.id().equals(state.currentTarget)
                || state.currentOrbit != SpaceSimulation.OrbitBand.SURFACE && state.currentOrbit != SpaceSimulation.OrbitBand.TIGHT
                || !AsteroidImpactRules.canConnectAsteroid(relativeSpeed)) {
            state.blockedState = TerminalState.UNSAFE_ASTEROID_APPROACH;
            return false;
        }
        if (action.segments().size() != 1 || !state.segments.containsKey(action.segments().getFirst())) {
            state.blockedState = TerminalState.NO_ASTEROID_ANCHOR;
            return false;
        }
        state.attachedAsteroid = asteroid;
        state.asteroidAnchor = action.segments().getFirst();
        state.addSample(PathPhase.COAST, asteroid.id(), action.id());
        return true;
    }

    private static void disconnectBooster(SpaceSimulation.FlightPlanAction navigation,
                                          SpaceSimulation.SegmentRef booster, Craft state,
                                          Context context, int stage) {
        if (!state.segments.containsKey(booster)) return;
        // Copy before removing: the detached craft inherits velocity, stage and any anchored asteroid.
        var detached = state.copyFor(Set.of(booster));
        state.removeSegment(booster);
        if (booster.equals(state.asteroidAnchor)) state.clearAsteroidAttachment();

        UUID eventId = boosterEventId(navigation.id(), booster);
        var child = context.branchesByParent.get(eventId);
        if (child == null) child = new SpaceSimulation.FlightPlanBranch(eventId, eventId, List.of());
        context.boosterEvents.add(new BoosterEvent(eventId, state.branchId, child.id(), navigation.id(), booster,
                stage, state.time, state.x, state.y));
        simulateBranch(child, detached, context);
    }

    private static UUID boosterEventId(UUID navigationAction, SpaceSimulation.SegmentRef booster) {
        String key = navigationAction + ":" + booster.anchor().asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean separate(SpaceSimulation.FlightPlanAction action, Craft state,
                                    Context context) {
        if (state.attachedAsteroid != null && action.targetId().equals(state.attachedAsteroid.id())
                && action.segments().size() == 1 && action.segments().getFirst().equals(state.asteroidAnchor)) {
            context.asteroidPaths.add(ReleasedAsteroidPrediction.predict(state, context));
            state.clearAsteroidAttachment();
            state.addSample(PathPhase.COAST, action.targetId(), action.id());
            return true;
        }
        if (action.segments().size() != 2) {
            state.blockedState = TerminalState.NO_COUPLING;
            return false;
        }
        var retainedRef = action.segments().get(0);
        var detachedRef = action.segments().get(1);
        if (!state.segments.containsKey(retainedRef) || !state.segments.containsKey(detachedRef)
                || !state.connections.getOrDefault(retainedRef, Set.of()).contains(detachedRef)) {
            state.blockedState = TerminalState.NO_COUPLING;
            return false;
        }

        state.connections.get(retainedRef).remove(detachedRef);
        state.connections.get(detachedRef).remove(retainedRef);
        var retained = state.connectedComponent(retainedRef);
        if (retained.contains(detachedRef)) return true;
        var detached = state.connectedComponent(detachedRef);
        var detachedState = state.copyFor(detached);
        state.retain(retained);

        var child = context.branchesByParent.get(action.id());
        if (child == null) child = new SpaceSimulation.FlightPlanBranch(action.id(), action.id(), List.of());
        simulateBranch(child, detachedState, context);
        return true;
    }

    /**
     * @param timeSeconds Seconds since the root branch started.
     * @param x X position in the simulation plane.
     * @param y Y position in the simulation plane.
     * @param speedMetersPerSecond Speed magnitude for display.
     * @param velocityX X velocity in meters per second.
     * @param velocityY Y velocity in meters per second.
     * @param phase Engine phase for the interval ending at this sample.
     * @param targetId Navigation target, or NO_TARGET.
     * @param actionId Card which produced this result.
     * @param stage One-based engine stage.
     * @param connectedSegments Segments attached at this exact time.
     * @param firingSegments Segments burning during this interval.
     * @param attachedAsteroidId Carried asteroid, or NO_TARGET.
     */
    public record PathSample(double timeSeconds, double x, double y, double speedMetersPerSecond,
                             double velocityX, double velocityY, PathPhase phase, UUID targetId, UUID actionId,
                             int stage, Set<SpaceSimulation.SegmentRef> connectedSegments,
                             Set<SpaceSimulation.SegmentRef> firingSegments, UUID attachedAsteroidId) {
        public static final Codec<PathSample> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("timeSeconds").forGetter(PathSample::timeSeconds),
                Codec.DOUBLE.fieldOf("x").forGetter(PathSample::x),
                Codec.DOUBLE.fieldOf("y").forGetter(PathSample::y),
                Codec.DOUBLE.fieldOf("speedMetersPerSecond").forGetter(PathSample::speedMetersPerSecond),
                Codec.DOUBLE.fieldOf("velocityX").forGetter(PathSample::velocityX),
                Codec.DOUBLE.fieldOf("velocityY").forGetter(PathSample::velocityY),
                Codec.STRING.xmap(PathPhase::valueOf, PathPhase::name).fieldOf("phase").forGetter(PathSample::phase),
                UUIDUtil.STRING_CODEC.fieldOf("targetId").forGetter(PathSample::targetId),
                UUIDUtil.STRING_CODEC.fieldOf("actionId").forGetter(PathSample::actionId),
                Codec.INT.fieldOf("stage").forGetter(PathSample::stage),
                SEGMENT_SET_CODEC.fieldOf("connectedSegments").forGetter(PathSample::connectedSegments),
                SEGMENT_SET_CODEC.fieldOf("firingSegments").forGetter(PathSample::firingSegments),
                UUIDUtil.STRING_CODEC.fieldOf("attachedAsteroidId").forGetter(PathSample::attachedAsteroidId)
        ).apply(i, PathSample::new));

    }

    /**
     * @param branchId Branch owning this result.
     * @param actionIndex Card position within the branch.
     * @param actionId Card which produced this result.
     * @param timeSeconds Seconds since the root branch started.
     * @param x X position in the simulation plane.
     * @param y Y position in the simulation plane.
     * @param completed Whether this card finished successfully.
     * @param connectedSegments Segments attached at this exact time.
     * @param attachedAsteroidId Carried asteroid, or NO_TARGET.
     */
    public record ActionMoment(UUID branchId, int actionIndex, UUID actionId, double timeSeconds,
                               double x, double y, boolean completed,
                               Set<SpaceSimulation.SegmentRef> connectedSegments, UUID attachedAsteroidId) {
        public static final Codec<ActionMoment> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("branchId").forGetter(ActionMoment::branchId),
                Codec.INT.fieldOf("actionIndex").forGetter(ActionMoment::actionIndex),
                UUIDUtil.STRING_CODEC.fieldOf("actionId").forGetter(ActionMoment::actionId),
                Codec.DOUBLE.fieldOf("timeSeconds").forGetter(ActionMoment::timeSeconds),
                Codec.DOUBLE.fieldOf("x").forGetter(ActionMoment::x),
                Codec.DOUBLE.fieldOf("y").forGetter(ActionMoment::y),
                Codec.BOOL.fieldOf("completed").forGetter(ActionMoment::completed),
                SEGMENT_SET_CODEC.fieldOf("connectedSegments").forGetter(ActionMoment::connectedSegments),
                UUIDUtil.STRING_CODEC.fieldOf("attachedAsteroidId").forGetter(ActionMoment::attachedAsteroidId)
        ).apply(i, ActionMoment::new));

    }

    /**
     * @param id Stable event ID linking the separation to its child program.
     * @param branchId Branch owning this result.
     * @param childBranchId Detached craft's branch.
     * @param navigationActionId Navigation card active when the booster ran out.
     * @param segment Booster which separated.
     * @param stage One-based engine stage.
     * @param timeSeconds Seconds since the root branch started.
     * @param x X position in the simulation plane.
     * @param y Y position in the simulation plane.
     */
    public record BoosterEvent(UUID id, UUID branchId, UUID childBranchId, UUID navigationActionId,
                               SpaceSimulation.SegmentRef segment, int stage,
                               double timeSeconds, double x, double y) {
    }

    /**
     * @param branchId Branch owning this result.
     * @param segments Segments remaining at the end of this branch.
     * @param samples Ordered motion samples.
     * @param actionMoments Exact card outcomes and attachment states.
     * @param durationSeconds End time on the shared preview clock.
     * @param remainingDeltaV Remaining velocity change budget with future stages included.
     * @param terminalState Why this branch ended.
     */
    public record CraftPath(UUID branchId, Set<SpaceSimulation.SegmentRef> segments,
                            List<PathSample> samples, List<ActionMoment> actionMoments,
                            double durationSeconds, double remainingDeltaV, TerminalState terminalState) {
        public static final Codec<CraftPath> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("branchId").forGetter(CraftPath::branchId),
                SEGMENT_SET_CODEC.fieldOf("segments").forGetter(CraftPath::segments),
                PathSample.CODEC.listOf().fieldOf("samples").forGetter(CraftPath::samples),
                ActionMoment.CODEC.listOf().fieldOf("actionMoments").forGetter(CraftPath::actionMoments),
                Codec.DOUBLE.fieldOf("durationSeconds").forGetter(CraftPath::durationSeconds),
                Codec.DOUBLE.fieldOf("remainingDeltaV").forGetter(CraftPath::remainingDeltaV),
                Codec.STRING.xmap(TerminalState::valueOf, TerminalState::name).fieldOf("terminalState").forGetter(CraftPath::terminalState)
        ).apply(i, CraftPath::new));

    }

    /**
     * @param paths Craft previews in plan order.
     * @param boosterEvents Automatic booster separations.
     * @param lastCommandSeconds Latest card completion time across all branches.
     * @param arrivalPredictions Surface arrival outcomes.
     * @param asteroidPaths Motion after asteroids are released.
     * @param navigationAborts Cards completed early by an addon.
     */
    public record FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents,
                             double lastCommandSeconds, List<ArrivalPrediction> arrivalPredictions,
                             List<AsteroidPath> asteroidPaths, List<NavigationAbortMoment> navigationAborts,
                             List<ScanEstimate> scanEstimates,
                             List<StationKeepingEstimate> stationKeepingEstimates) {
        public FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents, double lastCommandSeconds,
                          List<ArrivalPrediction> arrivalPredictions, List<AsteroidPath> asteroidPaths,
                          List<NavigationAbortMoment> navigationAborts, List<ScanEstimate> scanEstimates) {
            this(paths, boosterEvents, lastCommandSeconds, arrivalPredictions, asteroidPaths, navigationAborts,
                    scanEstimates, List.of());
        }
        public FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents, double lastCommandSeconds,
                          List<ArrivalPrediction> arrivalPredictions, List<AsteroidPath> asteroidPaths,
                          List<NavigationAbortMoment> navigationAborts) {
            this(paths, boosterEvents, lastCommandSeconds, arrivalPredictions, asteroidPaths, navigationAborts,
                    List.of(), List.of());
        }
        public FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents, double lastCommandSeconds) {
            this(paths, boosterEvents, lastCommandSeconds, List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    /** Scanner hardware and its local RF at the instant this action runs. */
    public record ScanEstimate(UUID actionId, long availableRF, int scanners) {
    }

    public record StationKeepingEstimate(UUID actionId, double durationSeconds, StationKeepingEnd end) {
    }

    public enum StationKeepingEnd {
        RF,
        FUEL,
        LOW_RF,
        LOW_FUEL
    }

    /**
     * @param actionId Card which produced this result.
     * @param branchId Branch owning this result.
     * @param targetId Navigation target, or NO_TARGET.
     * @param impact Predicted surface arrival outcome.
     */
    public record ArrivalPrediction(UUID actionId, UUID branchId, UUID targetId,
                                    AsteroidImpactRules.ImpactPrediction impact) {
    }

    /**
     * @param asteroidId Asteroid being followed.
     * @param samples Ordered motion samples.
     * @param earthImpact Predicted Earth impact, or null if none was found.
     * @param landingUncertaintyBlocks Landing spread estimated at the release point.
     */
    public record AsteroidPath(UUID asteroidId, List<MotionSample> samples,
                               AsteroidImpactRules.ImpactPrediction earthImpact, int landingUncertaintyBlocks) {
    }

    /**
     * @param actionId Card which produced this result.
     * @param branchId Branch owning this result.
     * @param addonId Condition which stopped navigation.
     * @param actualValue Measured value when the condition became true.
     * @param timeSeconds Seconds since the root branch started.
     * @param x X position in the simulation plane.
     * @param y Y position in the simulation plane.
     * @param destinationX Original goal X before stopping early.
     * @param destinationY Original goal Y before stopping early.
     */
    public record NavigationAbortMoment(UUID actionId, UUID branchId, UUID addonId, double actualValue,
                                        double timeSeconds, double x, double y,
                                        double destinationX, double destinationY) {
    }

    /**
     * @param timeSeconds Seconds since the root branch started.
     * @param x X position in the simulation plane.
     * @param y Y position in the simulation plane.
     * @param speedMetersPerSecond Speed magnitude for display.
     */
    public record MotionSample(double timeSeconds, double x, double y, double speedMetersPerSecond) {
    }

    public enum PathPhase {
        ACCELERATE,
        REDIRECT,
        COAST,
        BRAKE
    }

    public enum TerminalState {
        READY,
        STATION_KEEPING_EXHAUSTED,
        DISCARDED,
        DESTROYED,
        PLAN_BLOCKED,
        TARGET_UNAVAILABLE,
        EMPTY_CRAFT,
        ASTEROID_ALREADY_ATTACHED,
        UNSAFE_ASTEROID_APPROACH,
        NO_ASTEROID_ANCHOR,
        NO_COUPLING,
        NO_SCAN_TARGET,
        SCAN_OUT_OF_RANGE,
        NO_SCANNER,
        NO_ANTENNA,
        NOT_ENOUGH_SERVICE_RF,
        PRECISE_POSITION_REQUIRED,
        NOT_ENOUGH_DELTA_V,
        NO_ACTIVE_ENGINES,
        NO_FEASIBLE_TRANSFER,
        INTEGRATION_STEP_LIMIT,
        INTEGRATION_TIME_LIMIT;

        public boolean isFailure() {
            return this != READY && this != STATION_KEEPING_EXHAUSTED && this != DISCARDED;
        }

        /** Destruction may be the requested result of an impact mission. */
        public boolean preventsLaunch() {
            return isFailure() && this != DESTROYED;
        }
    }
}
