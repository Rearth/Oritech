package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.simulation.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Segment;

import java.nio.charset.StandardCharsets;
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

    private RocketFlightPathCalculator() {
    }

    public static FlightPath calculate(ActiveRocketData rocket,
                                       List<SpaceSimulation.SpaceObjectData> objects,
                                       SpaceSimulation.FlightPlan plan) {
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
                List.copyOf(context.navigationAborts));
    }

    private static Craft createInitialCraft(ActiveRocketData rocket,
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
            segments.put(ref, new Segment(performance.wetMassKilograms(), performance.thrustNewtons(),
                    performance.availableDeltaVMetersPerSecond(), performance.availableBurnSeconds()));

            var neighbours = new LinkedHashSet<SpaceSimulation.SegmentRef>();
            dynamic.getConnectedSegments().stream().map(refsById::get).filter(java.util.Objects::nonNull)
                    .forEach(neighbours::add);
            connections.put(ref, neighbours);
        }

        double initialX = earth.x();
        double initialY = earth.y();
        var firstAction = root.actions().stream().filter(action -> !action.isGenerated()).findFirst().orElse(null);
        if (firstAction != null && firstAction.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            var target = objects.get(firstAction.targetId());
            if (target != null) {
                double offsetX = target.x() - earth.x();
                double offsetY = target.y() - earth.y();
                double angle = Math.hypot(offsetX, offsetY) < 1
                        ? RocketFlightNavigation.targetPointOffset(firstAction) : Math.atan2(offsetY, offsetX);
                initialX += Math.cos(angle) * earth.radius();
                initialY += Math.sin(angle) * earth.radius();
            }
        }
        return new Craft(segments, connections, initialX, initialY, 0);
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
                case NAVIGATE_TO -> RocketFlightNavigation.navigate(action, state, context);
                case CONNECT_ASTEROID -> connectAsteroid(action, state, context);
                case DECOUPLE -> separate(action, state, context);
                case MAINTAIN_POSITION -> {
                    state.maintainingPosition = true;
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
                : state.maintainingPosition ? TerminalState.MAINTAINING_POSITION
                : completed ? TerminalState.READY : state.blockedState;
        context.paths.put(branch.id(), state.toPath(terminal, context));
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
        if (asteroid == null || asteroid.type() != SpaceObjects.ObjectType.ASTEROID
                || state.attachedAsteroid != null || !asteroid.id().equals(state.currentTarget)
                || (state.currentOrbit != SpaceSimulation.OrbitBand.SURFACE
                && state.currentOrbit != SpaceSimulation.OrbitBand.TIGHT) || action.segments().size() != 1
                || !state.segments.containsKey(action.segments().getFirst())
                || !AsteroidImpactRules.canConnectAsteroid(relativeSpeed)) return false;
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
        if (action.segments().size() != 2) return false;
        var retainedRef = action.segments().get(0);
        var detachedRef = action.segments().get(1);
        if (!state.segments.containsKey(retainedRef) || !state.segments.containsKey(detachedRef)
                || !state.connections.getOrDefault(retainedRef, Set.of()).contains(detachedRef)) return false;

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
                             List<AsteroidPath> asteroidPaths, List<NavigationAbortMoment> navigationAborts) {
        public FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents, double lastCommandSeconds) {
            this(paths, boosterEvents, lastCommandSeconds, List.of(), List.of(), List.of());
        }
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
        MAINTAINING_POSITION,
        DISCARDED,
        DESTROYED,
        PLAN_BLOCKED,
        NOT_ENOUGH_DELTA_V,
        NO_ACTIVE_ENGINES,
        NO_FEASIBLE_TRANSFER,
        INTEGRATION_STEP_LIMIT,
        INTEGRATION_TIME_LIMIT;

        public boolean isFailure() {
            return this != READY && this != MAINTAINING_POSITION && this != DISCARDED;
        }
    }
}
