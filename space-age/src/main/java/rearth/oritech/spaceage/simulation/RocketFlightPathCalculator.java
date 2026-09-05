package rearth.oritech.spaceage.simulation;

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

    private static final int MAX_NAVIGATION_STEPS = 10_000;
    private static final int MAX_SAMPLES_PER_NAVIGATION = 1000;
    private static final double MINECRAFT_DAY_SECONDS = 1_200;
    private static final double MAX_NAVIGATION_SECONDS = 1_000 * MINECRAFT_DAY_SECONDS;
    private static final double MIN_STEP_SECONDS = 0.05;
    private static final double BURN_TOLERANCE = 1e-9;
    private static final double POSITION_TOLERANCE = 0.01;
    private static final double VELOCITY_TOLERANCE = 0.00001;

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

        var context = new CalculationContext(objectsById, branchesByParent, configurations,
                RocketFlightPlanRules.stageCount(plan, rocket.getStaticSegments().size()));
        var initial = createInitialCraft(rocket, earth);
        simulateBranch(plan.root(), initial, context);

        var orderedPaths = new ArrayList<CraftPath>();
        for (var branch : plan.branches()) {
            var path = context.paths.remove(branch.id());
            if (path != null) orderedPaths.add(path);
        }
        orderedPaths.addAll(context.paths.values());
        double lastCommand = orderedPaths.stream().flatMap(path -> path.actionMoments().stream())
                .mapToDouble(ActionMoment::timeSeconds).max().orElse(0);
        return new FlightPath(orderedPaths, List.copyOf(context.boosterEvents), lastCommand);
    }

    private static CraftState createInitialCraft(ActiveRocketData rocket,
                                                  SpaceSimulation.SpaceObjectData earth) {
        var refsById = new HashMap<UUID, SpaceSimulation.SegmentRef>();
        rocket.getStaticSegments().forEach((id, segment) -> refsById.put(id, SpaceSimulation.SegmentRef.of(segment)));
        var segments = new LinkedHashMap<SpaceSimulation.SegmentRef, SegmentState>();
        var connections = new HashMap<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>>();
        for (var entry : rocket.getStaticSegments().entrySet()) {
            var ref = refsById.get(entry.getKey());
            var dynamic = rocket.getDynamicSegments().get(entry.getKey());
            var segmentRocket = new ActiveRocketData(Map.of(entry.getKey(), entry.getValue()),
                    Map.of(entry.getKey(), dynamic));
            var performance = RocketPerformanceCalculator.calculate(segmentRocket);
            segments.put(ref, new SegmentState(performance.wetMassKilograms(), performance.thrustNewtons(),
                    performance.availableDeltaVMetersPerSecond(), performance.availableBurnSeconds()));

            var neighbours = new LinkedHashSet<SpaceSimulation.SegmentRef>();
            dynamic.getConnectedSegments().stream().map(refsById::get).filter(java.util.Objects::nonNull)
                    .forEach(neighbours::add);
            connections.put(ref, neighbours);
        }

        // A newly assembled rocket begins on Earth itself. Its first line must not appear to originate from an
        // arbitrary side of low orbit simply because that orbit is the planner's default selection.
        return new CraftState(segments, connections, earth.x(), earth.y(), 0);
    }

    private static void simulateBranch(SpaceSimulation.FlightPlanBranch branch, CraftState state,
                                       CalculationContext context) {
        state.branchId = branch.id();
        state.addSample(PathPhase.COAST, SpaceSimulation.FlightPlanAction.NO_TARGET);
        boolean completed = true;

        for (int index = 0; index < branch.actions().size(); index++) {
            var action = branch.actions().get(index);
            if (action.type() == SpaceSimulation.ActionType.DISCONNECT_BOOSTER) continue;
            completed = switch (action.type()) {
                case NAVIGATE_TO -> navigate(action, state, context);
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
                    state.x, state.y, completed));
            if (!completed || state.maintainingPosition || state.discarded) break;
        }

        var terminal = state.discarded ? TerminalState.DISCARDED
                : state.maintainingPosition ? TerminalState.MAINTAINING_POSITION
                : completed ? TerminalState.READY : state.blockedState;
        context.paths.put(branch.id(), state.toPath(terminal, context));
    }

    private static boolean navigate(SpaceSimulation.FlightPlanAction action, CraftState state,
                                    CalculationContext context) {
        var target = context.objects.get(action.targetId());
        if (target == null || state.segments.isEmpty()) return false;
        while (finishStage(action, state, context)) {
            // Empty configured stages are skipped here so a reusable plan cannot strand a slightly different rocket.
        }
        var destination = targetPoint(state.x, state.y, target, action.orbit());
        double startTime = state.time;
        double approachX = destination.x - state.x;
        double approachY = destination.y - state.y;
        double approachLength = Math.hypot(approachX, approachY);
        approachX = approachLength < 1 ? 1 : approachX / approachLength;
        approachY = approachLength < 1 ? 0 : approachY / approachLength;
        double targetSpeed = action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM
                ? action.targetVelocity() : 0;
        double targetVelocityX = approachX * targetSpeed;
        double targetVelocityY = approachY * targetSpeed;
        var navigationSamples = new ArrayList<PathSample>();
        TransferPlan transferPlan = null;

        // This is intentionally bounded per card. A broken or impossible plan should produce PLAN_BLOCKED quickly
        // instead of stalling the client while it repeatedly circles a target it cannot reach.
        int step = 0;
        for (; step < MAX_NAVIGATION_STEPS
                && state.time - startTime < MAX_NAVIGATION_SECONDS; step++) {
            double offsetX = destination.x - state.x;
            double offsetY = destination.y - state.y;
            if (completeArrival(action, state, destination, targetVelocityX, targetVelocityY,
                    navigationSamples)) return true;

            var active = state.activeSegments(context);
            double deltaVRate = state.deltaVPerSecond(active);
            // The planner's resource is delta-v, so its burn rate is also the acceleration limit. Keeping those two
            // values identical prevents a path from spending more delta-v than it actually adds to craft velocity.
            double maximumAcceleration = deltaVRate;
            if (transferPlan == null) {
                var transfer = FullPowerTransfer.solve(offsetX, offsetY, state.velocityX, state.velocityY,
                        targetVelocityX, targetVelocityY,
                        action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.MAXIMUM,
                        state.burnProfile(context),
                        action.maxSpeed() == 0 ? Double.POSITIVE_INFINITY : action.maxSpeed(),
                        MAX_NAVIGATION_SECONDS - (state.time - startTime));
                if (transfer == null) {
                    state.blockedState = maximumAcceleration <= 0.0001
                            ? TerminalState.NO_ACTIVE_ENGINES : TerminalState.NO_FEASIBLE_TRANSFER;
                    appendNavigationSamples(state, navigationSamples);
                    return false;
                }
                transferPlan = new TransferPlan(state.time, transfer);
            }
            var command = transferPlan.commandAt(state, maximumAcceleration);

            // An exhausted transfer must already have arrived. Do not hide a prediction error with a return trip.
            if (command.stepLimitSeconds <= BURN_TOLERANCE) {
                state.blockedState = TerminalState.NO_FEASIBLE_TRANSFER;
                appendNavigationSamples(state, navigationSamples);
                return false;
            }

            double speed = Math.hypot(state.velocityX, state.velocityY);
            double stepSeconds = command.sampleStepSeconds;
            stepSeconds = Math.min(stepSeconds, command.stepLimitSeconds);
            if (command.burning) {
                if (active.isEmpty() || maximumAcceleration <= 0.0001) {
                    appendNavigationSamples(state, navigationSamples);
                    return false;
                }
                double nextEngineStop = active.stream().map(state.segments::get)
                        .mapToDouble(segment -> segment.remainingBurnSeconds)
                        .min().orElse(0);
                // Hit resource boundaries exactly. Overshooting one makes the next stage start with fuel which the
                // previous stage should already have consumed, and visibly moves its separation marker.
                stepSeconds = Math.min(stepSeconds, nextEngineStop);
            } else if (speed <= 0.0001 && !Double.isFinite(command.stepLimitSeconds)) {
                appendNavigationSamples(state, navigationSamples);
                return false;
            }
            stepSeconds = Math.min(stepSeconds, MAX_NAVIGATION_SECONDS - (state.time - startTime));
            if (stepSeconds <= BURN_TOLERANCE) break;

            double accelerationX = command.burning ? command.directionX * command.acceleration : 0;
            double accelerationY = command.burning ? command.directionY * command.acceleration : 0;
            state.x += state.velocityX * stepSeconds + accelerationX * stepSeconds * stepSeconds * 0.5;
            state.y += state.velocityY * stepSeconds + accelerationY * stepSeconds * stepSeconds * 0.5;
            state.velocityX += accelerationX * stepSeconds;
            state.velocityY += accelerationY * stepSeconds;
            state.time += stepSeconds;
            if (command.burning) {
                // Consume exactly the delta-v applied to the trajectory. Basing this on thrust acceleration instead
                // could drain more resource than the path gained whenever the two limits differ.
                state.consumeBurnTime(active, stepSeconds);
            }
            navigationSamples.add(state.createSample(command.phase, action.targetId(),
                    command.burning ? Set.copyOf(active) : Set.of()));

            if (finishStage(action, state, context)) {
                navigationSamples.add(state.createSample(PathPhase.COAST, action.targetId(), Set.of()));
                while (finishStage(action, state, context)) {
                    // Continue through any following stage which has no usable engines.
                }
            }
        }

        if (completeArrival(action, state, destination, targetVelocityX, targetVelocityY,
                navigationSamples)) return true;
        state.blockedState = step >= MAX_NAVIGATION_STEPS
                ? TerminalState.INTEGRATION_STEP_LIMIT
                : TerminalState.INTEGRATION_TIME_LIMIT;
        appendNavigationSamples(state, navigationSamples);
        return false;
    }

    private static double sampleStep(double phaseSeconds) {
        // Constant-acceleration phases integrate exactly, so a fixed sample count stays smooth without making long
        // low-thrust transfers perform tens of thousands of otherwise identical steps.
        return Math.max(MIN_STEP_SECONDS, phaseSeconds / 160);
    }

    private static boolean completeArrival(SpaceSimulation.FlightPlanAction action, CraftState state,
                                           Point destination, double targetVelocityX, double targetVelocityY,
                                           List<PathSample> navigationSamples) {
        double distance = Math.hypot(destination.x - state.x, destination.y - state.y);
        if (!hasArrived(action, state, distance, targetVelocityX, targetVelocityY)) return false;
        state.x = destination.x;
        state.y = destination.y;
        if (action.velocityMode() != SpaceSimulation.ArrivalVelocityMode.MAXIMUM) {
            state.velocityX = targetVelocityX;
            state.velocityY = targetVelocityY;
        }
        navigationSamples.add(state.createSample(PathPhase.COAST, action.targetId(), Set.of()));
        appendNavigationSamples(state, navigationSamples);
        return true;
    }

    private static PathPhase phaseFor(CraftState state, double directionX, double directionY) {
        double speed = Math.hypot(state.velocityX, state.velocityY);
        double speedChange = speed < 0.0001 ? 1
                : (directionX * state.velocityX + directionY * state.velocityY) / speed;
        return speedChange > 0.2 ? PathPhase.ACCELERATE
                : speedChange < -0.2 ? PathPhase.BRAKE : PathPhase.REDIRECT;
    }

    private static boolean hasArrived(SpaceSimulation.FlightPlanAction action, CraftState state, double distance,
                                      double targetVelocityX, double targetVelocityY) {
        if (distance > POSITION_TOLERANCE) return false;
        if (action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.MAXIMUM) return true;
        return Math.hypot(state.velocityX - targetVelocityX,
                state.velocityY - targetVelocityY) <= VELOCITY_TOLERANCE * 2;
    }

    private static boolean finishStage(SpaceSimulation.FlightPlanAction navigation, CraftState state,
                                       CalculationContext context) {
        boolean canAdvance = state.currentStage < context.stageCount;
        if (!state.stageFinished(context)
                && !(canAdvance && state.activeSegments(context).isEmpty())) return false;
        int finishedStage = state.currentStage;
        var detachedBoosters = state.boostersEndingCurrentStage(context);
        for (var booster : detachedBoosters) {
            disconnectBooster(navigation, booster, state, context, new Point(state.x, state.y),
                    new Point(state.velocityX, state.velocityY), state.time, finishedStage);
        }
        if (!canAdvance) return !detachedBoosters.isEmpty();
        state.currentStage++;
        return true;
    }

    private static void appendNavigationSamples(CraftState state, List<PathSample> samples) {
        if (samples.isEmpty()) return;
        int stride = Math.max(1, (int) Math.ceil(samples.size() / (double) MAX_SAMPLES_PER_NAVIGATION));
        PathSample previous = null;
        for (int index = 0; index < samples.size(); index++) {
            var sample = samples.get(index);
            boolean stateChanged = previous == null || sample.phase() != previous.phase()
                    || sample.stage() != previous.stage()
                    || !sample.connectedSegments().equals(previous.connectedSegments())
                    || !sample.firingSegments().equals(previous.firingSegments());
            // Keep both ends of a phase boundary so downsampling never paints a burn as part of a long coast.
            if (stateChanged && previous != null && state.samples.getLast() != previous) state.samples.add(previous);
            if (stateChanged || index % stride == 0 || index == samples.size() - 1) state.samples.add(sample);
            previous = sample;
        }
    }

    private record TransferPlan(double startTime, FullPowerTransfer transfer) {
        private GuidanceCommand commandAt(CraftState state, double acceleration) {
            double elapsed = Math.max(0, state.time - startTime);
            double endFirst = transfer.firstSeconds();
            double endCoast = endFirst + transfer.coastSeconds();
            double ax, ay, remaining;
            if (elapsed < endFirst - 1e-7) {
                ax = transfer.firstDirectionX();
                ay = transfer.firstDirectionY();
                remaining = endFirst - elapsed;
            } else if (elapsed < endCoast - 1e-7) {
                return GuidanceCommand.coast(endCoast - elapsed, sampleStep(transfer.coastSeconds()));
            } else {
                ax = transfer.lastDirectionX();
                ay = transfer.lastDirectionY();
                remaining = Math.max(0, transfer.duration() - elapsed);
            }
            if (ax == 0 && ay == 0) return GuidanceCommand.coast(remaining, sampleStep(remaining));
            return new GuidanceCommand(true, ax, ay, acceleration,
                    remaining, sampleStep(Math.max(transfer.firstSeconds(), transfer.lastSeconds())), phaseFor(state, ax, ay));
        }
    }

    private record GuidanceCommand(boolean burning, double directionX, double directionY,
                                   double acceleration,
                                   double stepLimitSeconds, double sampleStepSeconds, PathPhase phase) {
        private static GuidanceCommand coast(double seconds, double sampleStepSeconds) {
            return new GuidanceCommand(false, 0, 0, 0, seconds, sampleStepSeconds, PathPhase.COAST);
        }
    }

    private static void disconnectBooster(SpaceSimulation.FlightPlanAction navigation,
                                          SpaceSimulation.SegmentRef booster, CraftState state,
                                          CalculationContext context, Point position, Point velocity, double eventTime,
                                          int stage) {
        var segment = state.segments.remove(booster);
        if (segment == null) return;
        for (var neighbour : state.connections.getOrDefault(booster, Set.of())) {
            var neighbourConnections = state.connections.get(neighbour);
            if (neighbourConnections != null) neighbourConnections.remove(booster);
        }
        state.connections.remove(booster);

        UUID eventId = boosterEventId(navigation.id(), booster);
        var child = context.branchesByParent.get(eventId);
        if (child == null) child = new SpaceSimulation.FlightPlanBranch(eventId, eventId, List.of());
        context.boosterEvents.add(new BoosterEvent(eventId, state.branchId, child.id(), navigation.id(), booster,
                stage, eventTime, position.x, position.y));

        var detached = new CraftState(new LinkedHashMap<>(Map.of(booster, segment.copy())),
                new HashMap<>(Map.of(booster, new LinkedHashSet<>())), position.x, position.y, eventTime);
        detached.velocityX = velocity.x;
        detached.velocityY = velocity.y;
        detached.currentStage = state.currentStage;
        simulateBranch(child, detached, context);
    }

    private static UUID boosterEventId(UUID navigationAction, SpaceSimulation.SegmentRef booster) {
        String key = navigationAction + ":" + booster.anchor().asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean separate(SpaceSimulation.FlightPlanAction action, CraftState state,
                                    CalculationContext context) {
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

    private static Point targetPoint(double sourceX, double sourceY,
                                     SpaceSimulation.SpaceObjectData target,
                                     SpaceSimulation.OrbitBand orbit) {
        double offsetX = sourceX - target.x();
        double offsetY = sourceY - target.y();
        double length = Math.hypot(offsetX, offsetY);
        double orbitRadius = target.radius() + orbit.altitude();
        // A surface target lies on the body's edge, not its centre. Apart from looking more natural in the map,
        // this also keeps the navigation solver from flying through a planet before declaring arrival.
        if (length < 1) return new Point(target.x() + orbitRadius, target.y());
        return new Point(target.x() + offsetX / length * orbitRadius,
                target.y() + offsetY / length * orbitRadius);
    }

    private static final class CalculationContext {
        private final Map<UUID, SpaceSimulation.SpaceObjectData> objects;
        private final Map<UUID, SpaceSimulation.FlightPlanBranch> branchesByParent;
        private final Map<SpaceSimulation.SegmentRef, SpaceSimulation.SegmentConfiguration> configurations;
        private final int stageCount;
        private final Map<UUID, CraftPath> paths = new LinkedHashMap<>();
        private final List<BoosterEvent> boosterEvents = new ArrayList<>();

        private CalculationContext(Map<UUID, SpaceSimulation.SpaceObjectData> objects,
                                   Map<UUID, SpaceSimulation.FlightPlanBranch> branchesByParent,
                                   Map<SpaceSimulation.SegmentRef, SpaceSimulation.SegmentConfiguration> configurations,
                                   int stageCount) {
            this.objects = objects;
            this.branchesByParent = branchesByParent;
            this.configurations = configurations;
            this.stageCount = stageCount;
        }

        private SpaceSimulation.SegmentConfiguration configuration(SpaceSimulation.SegmentRef ref) {
            return configurations.getOrDefault(ref,
                    new SpaceSimulation.SegmentConfiguration(ref, "", false, List.of(1)));
        }
    }

    private static final class SegmentState {
        private final double wetMass;
        private final double thrust;
        private double remainingDeltaV;
        private double remainingBurnSeconds;

        private SegmentState(double wetMass, double thrust, double remainingDeltaV, double remainingBurnSeconds) {
            this.wetMass = wetMass;
            this.thrust = thrust;
            this.remainingDeltaV = remainingDeltaV;
            this.remainingBurnSeconds = remainingBurnSeconds;
        }

        private SegmentState copy() {
            return new SegmentState(wetMass, thrust, remainingDeltaV, remainingBurnSeconds);
        }
    }

    private static final class CraftState {
        private final Map<SpaceSimulation.SegmentRef, SegmentState> segments;
        private final Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections;
        private final List<PathSample> samples = new ArrayList<>();
        private final List<ActionMoment> actionMoments = new ArrayList<>();
        private UUID branchId;
        private double x;
        private double y;
        private double time;
        private double velocityX;
        private double velocityY;
        private int currentStage = 1;
        private boolean maintainingPosition;
        private boolean discarded;
        private TerminalState blockedState = TerminalState.PLAN_BLOCKED;

        private CraftState(Map<SpaceSimulation.SegmentRef, SegmentState> segments,
                           Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections,
                           double x, double y, double time) {
            this.segments = segments;
            this.connections = connections;
            this.x = x;
            this.y = y;
            this.time = time;
        }

        private double mass() {
            return segments.values().stream().mapToDouble(segment -> segment.wetMass).sum();
        }

        private List<SpaceSimulation.SegmentRef> activeSegments(CalculationContext context) {
            return segments.keySet().stream()
                    .filter(ref -> context.configuration(ref).usesEnginesDuring(currentStage))
                    .filter(ref -> segments.get(ref).thrust > 0
                            && segments.get(ref).remainingBurnSeconds > BURN_TOLERANCE
                            && segments.get(ref).remainingDeltaV > BURN_TOLERANCE)
                    .toList();
        }

        private double deltaVPerSecond(List<SpaceSimulation.SegmentRef> active) {
            double mass = Math.max(1, mass());
            double result = 0;
            for (var ref : active) {
                var segment = segments.get(ref);
                result += segment.remainingDeltaV / Math.max(BURN_TOLERANCE, segment.remainingBurnSeconds)
                        * segment.wetMass / mass;
            }
            return result;
        }

        private void consumeBurnTime(List<SpaceSimulation.SegmentRef> active, double seconds) {
            for (var ref : active) {
                var segment = segments.get(ref);
                double fraction = Math.min(1, seconds / Math.max(BURN_TOLERANCE, segment.remainingBurnSeconds));
                segment.remainingDeltaV *= 1 - fraction;
                segment.remainingBurnSeconds = Math.max(0, segment.remainingBurnSeconds - seconds);
            }
        }

        private boolean stageFinished(CalculationContext context) {
            // Shorter boosters may sit empty until the longest stage-ending booster is spent. They then separate
            // together, which produces one clear stage boundary in both the path and the timeline.
            var firingBoosters = segments.keySet().stream()
                    .filter(ref -> {
                        var configuration = context.configuration(ref);
                        return configuration.booster()
                                && configuration.usesEnginesDuring(currentStage);
                    }).toList();
            var endingBoosters = firingBoosters.stream()
                    .filter(ref -> context.configuration(ref).lastEngineStage() == currentStage)
                    .toList();
            if (!endingBoosters.isEmpty()) return endingBoosters.stream()
                    .allMatch(ref -> segments.get(ref).remainingBurnSeconds <= BURN_TOLERANCE
                            || segments.get(ref).remainingDeltaV <= BURN_TOLERANCE);
            // A booster may have been selected for a later stage but still run dry now. If every booster firing in
            // this stage is empty, advance instead of leaving the craft permanently stuck on an engine-less stage.
            return !firingBoosters.isEmpty() && firingBoosters.stream()
                    .allMatch(ref -> segments.get(ref).remainingBurnSeconds <= BURN_TOLERANCE
                            || segments.get(ref).remainingDeltaV <= BURN_TOLERANCE);
        }

        private List<SpaceSimulation.SegmentRef> boostersEndingCurrentStage(CalculationContext context) {
            return segments.keySet().stream().filter(ref -> {
                var configuration = context.configuration(ref);
                return configuration.booster() && (configuration.lastEngineStage() <= currentStage
                        || segments.get(ref).remainingBurnSeconds <= BURN_TOLERANCE
                        || segments.get(ref).remainingDeltaV <= BURN_TOLERANCE);
            }).toList();
        }

        private double availableDeltaV(CalculationContext context) {
            return burnProfile(context).deltaV();
        }

        private RocketBurnProfile burnProfile(CalculationContext context) {
            var copy = copyFor(Set.copyOf(segments.keySet()));
            var intervals = new ArrayList<RocketBurnProfile.Interval>();
            while (true) {
                var active = copy.activeSegments(context);
                if (copy.currentStage < context.stageCount
                        && (copy.stageFinished(context) || active.isEmpty())) {
                    for (var ref : copy.boostersEndingCurrentStage(context)) copy.removeSegment(ref);
                    copy.currentStage++;
                    continue;
                }
                if (active.isEmpty()) break;
                double rate = copy.deltaVPerSecond(active);
                if (rate <= 0) break;
                double seconds = active.stream().map(copy.segments::get)
                        .mapToDouble(segment -> segment.remainingBurnSeconds).min().orElse(0);
                if (seconds <= 0) break;
                copy.consumeBurnTime(active, seconds);
                intervals.add(new RocketBurnProfile.Interval(rate, seconds));
                if (copy.stageFinished(context)) {
                    for (var ref : copy.boostersEndingCurrentStage(context)) copy.removeSegment(ref);
                    copy.currentStage = Math.min(context.stageCount, copy.currentStage + 1);
                }
            }
            return new RocketBurnProfile(intervals);
        }

        private void removeSegment(SpaceSimulation.SegmentRef ref) {
            segments.remove(ref);
            for (var neighbour : connections.getOrDefault(ref, Set.of())) {
                var neighbourConnections = connections.get(neighbour);
                if (neighbourConnections != null) neighbourConnections.remove(ref);
            }
            connections.remove(ref);
        }

        private Set<SpaceSimulation.SegmentRef> connectedComponent(SpaceSimulation.SegmentRef start) {
            var result = new LinkedHashSet<SpaceSimulation.SegmentRef>();
            var open = new ArrayList<SpaceSimulation.SegmentRef>();
            open.add(start);
            while (!open.isEmpty()) {
                var current = open.removeLast();
                if (!result.add(current)) continue;
                connections.getOrDefault(current, Set.of()).stream().filter(neighbour -> !result.contains(neighbour))
                        .forEach(open::add);
            }
            return result;
        }

        private CraftState copyFor(Set<SpaceSimulation.SegmentRef> component) {
            var copiedSegments = new LinkedHashMap<SpaceSimulation.SegmentRef, SegmentState>();
            var copiedConnections = new HashMap<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>>();
            for (var ref : component) {
                copiedSegments.put(ref, segments.get(ref).copy());
                var neighbours = new LinkedHashSet<>(connections.getOrDefault(ref, Set.of()));
                neighbours.retainAll(component);
                copiedConnections.put(ref, neighbours);
            }
            var copy = new CraftState(copiedSegments, copiedConnections, x, y, time);
            copy.velocityX = velocityX;
            copy.velocityY = velocityY;
            copy.currentStage = currentStage;
            return copy;
        }

        private void retain(Set<SpaceSimulation.SegmentRef> retained) {
            segments.keySet().removeIf(ref -> !retained.contains(ref));
            connections.keySet().removeIf(ref -> !retained.contains(ref));
            connections.values().forEach(neighbours -> neighbours.retainAll(retained));
        }

        private void addSample(PathPhase phase, UUID target) {
            samples.add(createSample(phase, target, Set.of()));
        }

        private PathSample createSample(PathPhase phase, UUID target,
                                        Set<SpaceSimulation.SegmentRef> firingSegments) {
            return new PathSample(time, x, y, Math.hypot(velocityX, velocityY), velocityX, velocityY,
                    phase, target, currentStage, Set.copyOf(segments.keySet()), firingSegments);
        }

        private CraftPath toPath(TerminalState terminal, CalculationContext context) {
            return new CraftPath(branchId, Set.copyOf(segments.keySet()), List.copyOf(samples),
                    List.copyOf(actionMoments), time, availableDeltaV(context), terminal);
        }
    }

    private record Point(double x, double y) {
    }

    public record PathSample(double timeSeconds, double x, double y, double speedMetersPerSecond,
                             double velocityX, double velocityY, PathPhase phase, UUID targetId,
                             int stage, Set<SpaceSimulation.SegmentRef> connectedSegments,
                             Set<SpaceSimulation.SegmentRef> firingSegments) {
    }

    public record ActionMoment(UUID branchId, int actionIndex, UUID actionId, double timeSeconds,
                               double x, double y, boolean completed) {
    }

    public record BoosterEvent(UUID id, UUID branchId, UUID childBranchId, UUID navigationActionId,
                               SpaceSimulation.SegmentRef segment, int stage,
                               double timeSeconds, double x, double y) {
    }

    public record CraftPath(UUID branchId, Set<SpaceSimulation.SegmentRef> segments,
                            List<PathSample> samples, List<ActionMoment> actionMoments,
                            double durationSeconds, double remainingDeltaV, TerminalState terminalState) {
    }

    public record FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents,
                             double lastCommandSeconds) {
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
