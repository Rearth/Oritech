package rearth.oritech.spaceage.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Executes a flight plan against a lightweight two-dimensional physics model.
 * Each rocket segment keeps its own fuel and engines. Connected segments share
 * one position and velocity until a separation action splits them into two craft.
 */
public final class RocketFlightPathCalculator {

    private static final double EARTH_GRAVITY = RocketPerformanceCalculator.STANDARD_GRAVITY;
    private static final double TARGET_REACHED_DISTANCE = 1_000;
    private static final double LOW_ORBIT_DISTANCE = 1_000;
    private static final double MEDIUM_ORBIT_DISTANCE = 20_000;
    private static final double HIGH_ORBIT_DISTANCE = 40_000;
    private static final double ATMOSPHERE_DISTANCE = 100_000;
    private static final double SPACE_DRAG = 0.0001;
    private static final double STOPPED_SPEED = 0.1;
    private static final double ARRIVAL_SPEED_TOLERANCE = 1;
    private static final double SURFACE_TARGET_DISTANCE = 4;

    public static final Settings DEFAULT_SETTINGS = new Settings(0.25, 2, 1, 21_600, 43_200);
    public static final InitialState EARTH_START = new InitialState(0, 0, 0, 0, 0);

    private RocketFlightPathCalculator() {
    }

    public static FlightPath calculate(ActiveRocketData rocket,
                                       List<SpaceSimulation.SpaceObjectData> objects,
                                       SpaceSimulation.FlightPlan plan) {
        return calculate(rocket, objects, plan, EARTH_START, DEFAULT_SETTINGS, 0, 0);
    }

    public static FlightPath calculate(ActiveRocketData rocket,
                                       List<SpaceSimulation.SpaceObjectData> objects,
                                       SpaceSimulation.FlightPlan plan, int launchX, int launchZ) {
        return calculate(rocket, objects, plan, EARTH_START, DEFAULT_SETTINGS, launchX, launchZ);
    }

    /** This overload also allows a future uploaded plan to begin while already in space. */
    public static FlightPath calculate(ActiveRocketData rocket,
                                       List<SpaceSimulation.SpaceObjectData> objects,
                                       SpaceSimulation.FlightPlan plan,
                                       InitialState initialState, Settings settings) {
        return calculate(rocket, objects, plan, initialState, settings, 0, 0);
    }

    private static FlightPath calculate(ActiveRocketData rocket,
                                        List<SpaceSimulation.SpaceObjectData> objects,
                                        SpaceSimulation.FlightPlan plan,
                                        InitialState initialState, Settings settings,
                                        int launchX, int launchZ) {
        var objectsById = new HashMap<UUID, SpaceSimulation.SpaceObjectData>();
        objects.forEach(object -> objectsById.put(object.id(), object));

        var initialCraft = createInitialCraft(rocket, initialState, settings, launchX, launchZ);
        var branchesByParent = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        for (var branch : plan.branches()) {
            if (!branch.isRoot()) branchesByParent.put(branch.parentSeparationAction(), branch);
        }

        var pathsByBranch = new LinkedHashMap<UUID, CraftPath>();
        simulateBranch(plan.root(), initialCraft, objectsById, branchesByParent, pathsByBranch);

        // Keep the editor order stable even though child branches are calculated recursively.
        var orderedPaths = new ArrayList<CraftPath>();
        for (var branch : plan.branches()) {
            var path = pathsByBranch.remove(branch.id());
            if (path != null) orderedPaths.add(path);
        }
        orderedPaths.addAll(pathsByBranch.values());
        double lastCommand = orderedPaths.stream()
                .flatMap(path -> path.actionMoments().stream())
                .mapToDouble(ActionMoment::timeSeconds).max().orElse(initialState.timeSeconds());
        return new FlightPath(orderedPaths, lastCommand);
    }

    private static CraftState createInitialCraft(ActiveRocketData rocket, InitialState initialState,
                                                  Settings settings, int launchX, int launchZ) {
        var refsById = new HashMap<UUID, SpaceSimulation.SegmentRef>();
        rocket.getStaticSegments().forEach((id, segment) ->
                refsById.put(id, SpaceSimulation.SegmentRef.of(segment)));

        var segmentStates = new LinkedHashMap<SpaceSimulation.SegmentRef, SegmentState>();
        var connections = new HashMap<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>>();
        for (var entry : rocket.getStaticSegments().entrySet()) {
            var id = entry.getKey();
            var ref = refsById.get(id);
            var dynamic = rocket.getDynamicSegments().get(id);
            // Calculate this segment alone. This prevents its engines from using
            // fuel or energy that belongs to another attached segment.
            var singleSegmentRocket = new ActiveRocketData(Map.of(id, entry.getValue()), Map.of(id, dynamic));
            var performance = RocketPerformanceCalculator.calculate(singleSegmentRocket);
            segmentStates.put(ref, new SegmentState(performance.dryMassKilograms(),
                    performance.fuelMassKilograms(), performance.availableBurnSeconds(),
                    performance.availableBurnSeconds(), performance.thrustNewtons(), false, false));

            var neighbours = new LinkedHashSet<SpaceSimulation.SegmentRef>();
            for (var connectedId : dynamic.getConnectedSegments()) {
                var connectedRef = refsById.get(connectedId);
                if (connectedRef != null) neighbours.add(connectedRef);
            }
            connections.put(ref, neighbours);
        }
        return new CraftState(settings, segmentStates, connections, initialState, launchX, launchZ);
    }

    private static void simulateBranch(SpaceSimulation.FlightPlanBranch branch, CraftState state,
                                       Map<UUID, SpaceSimulation.SpaceObjectData> objects,
                                       Map<UUID, SpaceSimulation.FlightPlanBranch> branchesByParent,
                                       Map<UUID, CraftPath> paths) {
        state.branchId = branch.id();
        state.projected = false;
        state.addSample(true);
        boolean planCompleted = true;

        for (int index = 0; index < branch.actions().size(); index++) {
            var action = branch.actions().get(index);
            boolean completed;
            if (action.type() == SpaceSimulation.ActionType.DISABLE_COUPLINGS) {
                completed = separate(action, state, objects, branchesByParent, paths);
            } else {
                completed = executeAction(action, state, objects);
            }
            state.addSample(true);
            state.actionMoments.add(new ActionMoment(branch.id(), index, action.id(), state.time,
                    state.x, state.y, completed));
            if (!completed) {
                planCompleted = false;
                break;
            }
            // These actions intentionally end a branch. Later cards remain stored so a future editor can move them,
            // but they are unreachable until the terminal action is moved or removed.
            if (state.discarded || state.maintainingOrbit) break;
        }

        if (planCompleted && !state.crashed && !state.discarded) {
            state.projected = true;
            state.addSample(true);
            advanceToTerminalState(state);
        }
        state.addSample(true);
        paths.put(branch.id(), state.toPath(planCompleted));
    }

    private static boolean separate(SpaceSimulation.FlightPlanAction action, CraftState state,
                                    Map<UUID, SpaceSimulation.SpaceObjectData> objects,
                                    Map<UUID, SpaceSimulation.FlightPlanBranch> branchesByParent,
                                    Map<UUID, CraftPath> paths) {
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
        // Both craft inherit the exact state at separation. Their programs can then be calculated independently,
        // while recursive evaluation still produces one flat list of paths for the screen.
        var detachedState = state.copyFor(detached);
        state.retain(retained);

        var child = branchesByParent.get(action.id());
        if (child == null) {
            // A detached craft with no explicit program still needs a projected trajectory.
            child = new SpaceSimulation.FlightPlanBranch(action.id(), action.id(), List.of());
        }
        simulateBranch(child, detachedState, objects, branchesByParent, paths);
        return true;
    }

    private static boolean executeAction(SpaceSimulation.FlightPlanAction action, CraftState state,
                                         Map<UUID, SpaceSimulation.SpaceObjectData> objects) {
        return switch (action.type()) {
            case START_ENGINE_BURN -> state.setEngines(action.segments(), true, false);
            case START_BRAKING_BURN -> state.startBrakingBurn(action.segments());
            case STOP_ENGINE_BURN -> state.setEngines(action.segments(), false, false);
            case SET_NAVIGATION_TARGET -> {
                var target = objects.get(action.targetId());
                if (target == null) yield false;
                state.setNavigationTarget(target);
                yield true;
            }
            case SET_SURFACE_DESTINATION -> {
                state.setSurfaceDestination(action.value(), action.secondaryValue());
                yield true;
            }
            case SET_ARRIVAL_VELOCITY -> {
                state.arrivalSpeed = Math.max(0, action.value());
                yield true;
            }
            case WAIT_TICKS -> advanceFor(action.value() / 20d, state, ignored -> false);
            case WAIT_SECONDS -> advanceFor(action.value(), state, ignored -> false);
            case WAIT_UNTIL_DISTANCE -> waitForDistance(action, state, objects);
            case WAIT_FOR_EVENT -> waitForEvent(action, state, objects);
            case WAIT_UNTIL_BRAKING_POINT -> waitForBrakingPoint(action, state);
            case MAINTAIN_ORBIT -> state.beginStationKeeping(action.segments());
            case DISCARD_CRAFT -> {
                state.discard();
                yield true;
            }
            case OPEN_PARACHUTES -> state.hasSegments(action.segments());
            case DISABLE_COUPLINGS -> true;
        };
    }

    private static boolean waitForDistance(SpaceSimulation.FlightPlanAction action, CraftState state,
                                           Map<UUID, SpaceSimulation.SpaceObjectData> objects) {
        double threshold = action.value();
        boolean currentTarget = action.targetId().equals(SpaceSimulation.FlightPlanAction.CURRENT_TARGET);
        var reference = currentTarget ? null : objects.get(SpaceObjects.EARTH_ID);
        if (currentTarget && !state.hasTarget || !currentTarget && reference == null) return false;

        double initialDistance = currentTarget ? state.distanceToTarget()
                : distance(state.x, state.y, reference.x(), reference.y());
        double initialDifference = initialDistance - threshold;
        if (Math.abs(initialDifference) < 0.5) return true;
        boolean initiallyOutside = initialDifference > 0;
        return advanceUntil(state, current -> {
            double currentDistance = currentTarget ? current.distanceToTarget()
                    : distance(current.x, current.y, reference.x(), reference.y());
            double difference = currentDistance - threshold;
            return initiallyOutside ? difference <= 0 : difference >= 0;
        });
    }

    private static boolean waitForEvent(SpaceSimulation.FlightPlanAction action, CraftState state,
                                        Map<UUID, SpaceSimulation.SpaceObjectData> objects) {
        var events = SpaceSimulation.WaitEvent.values();
        int eventIndex = (int) Math.clamp(action.value(), 0, events.length - 1);
        Predicate<CraftState> condition = switch (events[eventIndex]) {
            case LOW_ORBIT_REACHED -> current -> current.y >= LOW_ORBIT_DISTANCE;
            case MEDIUM_ORBIT_REACHED -> current -> current.y >= MEDIUM_ORBIT_DISTANCE;
            case HIGH_ORBIT_REACHED -> current -> current.y >= HIGH_ORBIT_DISTANCE;
            case ATMOSPHERE_EXITED -> current -> current.y >= ATMOSPHERE_DISTANCE;
            case TARGET_REACHED -> current -> {
                return current.hasTarget && current.distanceToTarget() <= current.targetReachedDistance();
            };
            case FUEL_EMPTY -> current -> current.areSegmentsEmpty(action.segments());
        };
        if (!state.hasSegments(action.segments())) return false;
        if (condition.test(state)) return true;
        return advanceUntil(state, condition);
    }

    private static boolean waitForBrakingPoint(SpaceSimulation.FlightPlanAction action, CraftState state) {
        if (!state.hasTarget || !state.hasSegments(action.segments())
                || !state.hasEnoughBrakingDeltaV(action.segments())) return false;
        if (state.shouldStartBraking(action.segments())) return true;
        return advanceUntil(state, current -> current.shouldStartBraking(action.segments()));
    }

    private static boolean advanceFor(double seconds, CraftState state, Predicate<CraftState> stopCondition) {
        double requestedTime = state.time + Math.max(0, seconds);
        double targetTime = Math.min(requestedTime, state.simulationDeadline);
        while (state.time < targetTime) {
            integrate(state, Math.min(integrationStep(state, targetTime), targetTime - state.time));
            if (stopCondition.test(state)) return true;
            if (state.crashed) return false;
        }
        return state.time >= requestedTime;
    }

    private static boolean advanceUntil(CraftState state, Predicate<CraftState> condition) {
        double deadline = terminalDeadline(state);
        while (state.time < deadline) {
            integrate(state, Math.min(integrationStep(state, deadline), deadline - state.time));
            if (condition.test(state)) return true;
            if (state.crashed || state.isSettled()) return false;
        }
        return false;
    }

    private static void advanceToTerminalState(CraftState state) {
        double deadline = terminalDeadline(state);
        while (state.time < deadline && !state.crashed && !state.isSettled()) {
            integrate(state, Math.min(integrationStep(state, deadline), deadline - state.time));
        }
    }

    private static double terminalDeadline(CraftState state) {
        return Math.min(state.simulationDeadline,
                state.time + state.remainingActiveBurnSeconds() + state.settings.maxCoastSeconds);
    }

    private static double integrationStep(CraftState state, double targetTime) {
        if (state.hasActiveBrakingBurn() && state.hasTarget) {
            double approachWindow = Math.max(state.targetReachedDistance() * 10,
                    Math.hypot(state.velocityX, state.velocityY) * 10);
            if (state.distanceToTarget() < approachWindow) return state.settings.stepSeconds;
        }
        return targetTime - state.time > 60 ? state.settings.longStepSeconds : state.settings.stepSeconds;
    }

    private static void integrate(CraftState state, double step) {
        if (state.maintainingOrbit) {
            state.integrateStationKeeping(step);
            return;
        }

        double accelerationX = 0;
        double accelerationY = -EARTH_GRAVITY * SpaceSimulation.getGravityStrength((float) Math.max(0, state.y));
        boolean producingThrust = state.canProduceThrust();
        if (!producingThrust) {
            // Space drag is a preview safeguard rather than realistic aerodynamics. It lets abandoned craft reach a
            // terminal state, but must never reduce acceleration while an engine is actively burning.
            accelerationX -= state.velocityX * SPACE_DRAG;
            accelerationY -= state.velocityY * SPACE_DRAG;
        }

        if (producingThrust) {
            state.updateSurfaceHeading();
            double forwardImpulse = 0;
            double brakingImpulse = 0;
            for (var segment : state.segments.values()) {
                if (!segment.canProduceThrust()) continue;
                double burnStep = Math.min(step, segment.remainingBurnSeconds);
                if (segment.brakingBurn) brakingImpulse += segment.thrust * burnStep;
                else forwardImpulse += segment.thrust * burnStep;
                segment.remainingBurnSeconds -= burnStep;
            }
            double currentMass = state.currentMass();
            if (currentMass > 0) {
                double forwardAcceleration = forwardImpulse / step / currentMass;
                accelerationX += state.headingX * forwardAcceleration;
                accelerationY += state.headingY * forwardAcceleration;

                // A braking burn aims for the requested arrival velocity, rather than simply pointing away from
                // the target. Capping this impulse avoids oscillating around zero with coarse preview time steps.
                double velocityErrorX = state.desiredArrivalVelocityX() - state.velocityX;
                double velocityErrorY = state.desiredArrivalVelocityY() - state.velocityY;
                double velocityError = Math.hypot(velocityErrorX, velocityErrorY);
                if (brakingImpulse > 0 && velocityError > 0.0001) {
                    double brakingAcceleration = Math.min(brakingImpulse / step / currentMass, velocityError / step);
                    accelerationX += velocityErrorX / velocityError * brakingAcceleration;
                    accelerationY += velocityErrorY / velocityError * brakingAcceleration;
                }
            }
        }

        state.velocityX += accelerationX * step;
        state.velocityY += accelerationY * step;
        state.x += state.velocityX * step;
        state.y += state.velocityY * step;
        state.time += step;

        // Ground contact prevents an idle or underpowered preview from falling below the map.
        if (state.y > 1) state.leftGround = true;
        if (state.y <= 0) {
            if (state.leftGround && state.velocityY < 0) state.crashed = true;
            state.y = 0;
            state.velocityY = state.crashed ? 0 : Math.max(0, state.velocityY);
        }
        if (state.hasArrived()) state.stopBrakingEngines();
        state.addSample(false);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    /** maxCoastSeconds limits automatic projection; maxDurationSeconds limits the complete client calculation. */
    public record Settings(double stepSeconds, double longStepSeconds, double sampleIntervalSeconds,
                           double maxCoastSeconds, double maxDurationSeconds) {
        public Settings(double stepSeconds, double longStepSeconds, double sampleIntervalSeconds,
                        double maxCoastSeconds) {
            this(stepSeconds, longStepSeconds, sampleIntervalSeconds, maxCoastSeconds, maxCoastSeconds * 2);
        }

        public Settings {
            if (stepSeconds <= 0 || longStepSeconds < stepSeconds || sampleIntervalSeconds <= 0
                    || maxCoastSeconds <= 0 || maxDurationSeconds <= 0) {
                throw new IllegalArgumentException("Flight path settings must use positive durations");
            }
        }
    }

    public record InitialState(double x, double y, double velocityX, double velocityY, double timeSeconds) {
    }

    public record PathSample(double timeSeconds, double x, double y, double velocityX, double velocityY,
                             PathPhase phase, double remainingDeltaV, UUID targetId, boolean projected) {
    }

    public record ActionMoment(UUID branchId, int actionIndex, UUID actionId, double timeSeconds,
                               double x, double y, boolean completed) {
    }

    public record CraftPath(UUID branchId, Set<SpaceSimulation.SegmentRef> segments,
                            List<PathSample> samples, List<ActionMoment> actionMoments,
                            double durationSeconds, double remainingDeltaV, boolean fuelDepleted,
                            TerminalState terminalState) {
    }

    public record FlightPath(List<CraftPath> paths, double lastCommandSeconds) {
    }

    public enum PathPhase {
        PLANNED_BURN,
        PLANNED_COAST,
        PROJECTED_BURN
    }

    public enum TerminalState {
        STOPPED,
        EARTH_IMPACT,
        MAINTAINING_ORBIT,
        DISCARDED,
        PLAN_BLOCKED,
        MAX_DURATION
    }

    private static final class SegmentState {
        private final double dryMass;
        private final double initialFuelMass;
        private final double initialBurnSeconds;
        private final double thrust;
        private double remainingBurnSeconds;
        private boolean enginesEnabled;
        private boolean brakingBurn;

        private SegmentState(double dryMass, double initialFuelMass, double initialBurnSeconds,
                             double remainingBurnSeconds, double thrust, boolean enginesEnabled,
                             boolean brakingBurn) {
            this.dryMass = dryMass;
            this.initialFuelMass = initialFuelMass;
            this.initialBurnSeconds = initialBurnSeconds;
            this.remainingBurnSeconds = remainingBurnSeconds;
            this.thrust = thrust;
            this.enginesEnabled = enginesEnabled;
            this.brakingBurn = brakingBurn;
        }

        private SegmentState copy() {
            return new SegmentState(dryMass, initialFuelMass, initialBurnSeconds,
                    remainingBurnSeconds, thrust, enginesEnabled, brakingBurn);
        }

        private boolean canProduceThrust() {
            return enginesEnabled && thrust > 0 && remainingBurnSeconds > 0;
        }

        private double currentMass() {
            double fuelRatio = initialBurnSeconds <= 0 ? 0 : remainingBurnSeconds / initialBurnSeconds;
            return dryMass + initialFuelMass * Math.clamp(fuelRatio, 0, 1);
        }
    }

    private static final class CraftState {
        private final Settings settings;
        private final Map<SpaceSimulation.SegmentRef, SegmentState> segments;
        private final Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections;
        private final List<PathSample> samples = new ArrayList<>();
        private final List<ActionMoment> actionMoments = new ArrayList<>();
        private final int launchX;
        private final int launchZ;
        private UUID branchId;
        private double nextSampleTime;
        private double simulationDeadline;
        private double time;
        private double x;
        private double y;
        private double velocityX;
        private double velocityY;
        private double headingX;
        private double headingY = 1;
        private boolean projected;
        private boolean leftGround;
        private boolean crashed;
        private boolean maintainingOrbit;
        private boolean discarded;
        private boolean collectSamples = true;
        private boolean hasTarget;
        private boolean surfaceTarget;
        private double targetX;
        private double targetY;
        private double surfaceArcHeight;
        private double arrivalDirectionX;
        private double arrivalDirectionY = 1;
        private double arrivalSpeed;
        private Set<SpaceSimulation.SegmentRef> stationKeepingSegments = Set.of();
        private UUID targetId = SpaceSimulation.FlightPlanAction.NO_TARGET;

        private CraftState(Settings settings, Map<SpaceSimulation.SegmentRef, SegmentState> segments,
                           Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections,
                           InitialState initial, int launchX, int launchZ) {
            this.settings = settings;
            this.segments = segments;
            this.connections = connections;
            this.launchX = launchX;
            this.launchZ = launchZ;
            this.time = initial.timeSeconds;
            this.nextSampleTime = initial.timeSeconds;
            // Malformed or extremely long client plans must not freeze the render thread. The deadline covers
            // explicit waits as well as the automatic burn/coast projection after the last command.
            this.simulationDeadline = initial.timeSeconds + settings.maxDurationSeconds;
            this.x = initial.x;
            this.y = initial.y;
            this.velocityX = initial.velocityX;
            this.velocityY = initial.velocityY;
            this.leftGround = initial.y > 1;
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
            var copy = new CraftState(settings, copiedSegments, copiedConnections,
                    new InitialState(x, y, velocityX, velocityY, time), launchX, launchZ);
            copy.targetId = targetId;
            copy.hasTarget = hasTarget;
            copy.surfaceTarget = surfaceTarget;
            copy.targetX = targetX;
            copy.targetY = targetY;
            copy.surfaceArcHeight = surfaceArcHeight;
            copy.arrivalDirectionX = arrivalDirectionX;
            copy.arrivalDirectionY = arrivalDirectionY;
            copy.arrivalSpeed = arrivalSpeed;
            copy.headingX = headingX;
            copy.headingY = headingY;
            copy.leftGround = leftGround;
            copy.simulationDeadline = simulationDeadline;
            return copy;
        }

        private void retain(Set<SpaceSimulation.SegmentRef> retained) {
            segments.keySet().removeIf(ref -> !retained.contains(ref));
            connections.keySet().removeIf(ref -> !retained.contains(ref));
            connections.values().forEach(neighbours -> neighbours.retainAll(retained));
        }

        private Set<SpaceSimulation.SegmentRef> connectedComponent(SpaceSimulation.SegmentRef start) {
            var result = new LinkedHashSet<SpaceSimulation.SegmentRef>();
            var open = new ArrayList<SpaceSimulation.SegmentRef>();
            open.add(start);
            while (!open.isEmpty()) {
                var current = open.removeLast();
                if (!result.add(current)) continue;
                for (var neighbour : connections.getOrDefault(current, Set.of())) {
                    if (!result.contains(neighbour)) open.add(neighbour);
                }
            }
            return result;
        }

        private boolean setEngines(List<SpaceSimulation.SegmentRef> selected, boolean enabled,
                                   boolean brakingBurn) {
            if (!hasSegments(selected)) return false;
            selectedSegments(selected).forEach(segment -> {
                segment.enginesEnabled = enabled;
                segment.brakingBurn = enabled && brakingBurn;
            });
            return true;
        }

        private boolean startBrakingBurn(List<SpaceSimulation.SegmentRef> selected) {
            return hasTarget && setEngines(selected, true, true);
        }

        private void setNavigationTarget(SpaceSimulation.SpaceObjectData target) {
            targetId = target.id();
            hasTarget = true;
            surfaceTarget = false;
            targetX = target.x();
            targetY = target.y();
            double offsetX = targetX - x;
            double offsetY = targetY - y;
            double targetDistance = Math.hypot(offsetX, offsetY);
            if (targetDistance < 0.0001) return;

            // Keep this heading until another navigation command is executed. Re-aiming
            // every tick makes an overshooting rocket turn around and loop over its target.
            headingX = offsetX / targetDistance;
            headingY = offsetY / targetDistance;
            arrivalDirectionX = headingX;
            arrivalDirectionY = headingY;
        }

        private void setSurfaceDestination(long worldX, long worldZ) {
            double routeDistance = Math.hypot(worldX - launchX, worldZ - launchZ);
            targetId = SpaceSimulation.FlightPlanAction.SURFACE_TARGET;
            hasTarget = true;
            surfaceTarget = true;
            targetX = routeDistance;
            targetY = 0;
            // The surface coordinates are flattened into a vertical flight plane. This keeps the reusable physics
            // two-dimensional while still making path length, fuel use and arrival timing depend on the real route.
            surfaceArcHeight = Math.clamp(routeDistance * 0.25, 1_000, 20_000);
            arrivalDirectionX = 1;
            arrivalDirectionY = 0;
            updateSurfaceHeading();
        }

        private void updateSurfaceHeading() {
            if (!surfaceTarget || targetX < 0.0001) return;
            if (x >= targetX) {
                headingX = arrivalDirectionX;
                headingY = arrivalDirectionY;
                return;
            }
            double progress = inverseSmoothStep(Math.clamp(x / targetX, 0, 1));
            double lookAhead = Math.min(1, progress + 0.025);
            double guideX = targetX * smoothStep(lookAhead);
            double guideY = 4 * surfaceArcHeight * lookAhead * (1 - lookAhead);
            double offsetX = guideX - x;
            double offsetY = guideY - y;
            double length = Math.hypot(offsetX, offsetY);
            if (length < 0.0001) return;
            headingX = offsetX / length;
            headingY = offsetY / length;
        }

        private static double smoothStep(double value) {
            return value * value * (3 - 2 * value);
        }

        private static double inverseSmoothStep(double value) {
            double low = 0;
            double high = 1;
            // There is no useful closed form here for the preview. A few binary-search steps are stable and cheap,
            // and avoid adding another coordinate system solely for surface flights.
            for (int i = 0; i < 10; i++) {
                double middle = (low + high) * 0.5;
                if (smoothStep(middle) < value) low = middle;
                else high = middle;
            }
            return (low + high) * 0.5;
        }

        private double distanceToTarget() {
            return hasTarget ? distance(x, y, targetX, targetY) : Double.POSITIVE_INFINITY;
        }

        private double targetReachedDistance() {
            return surfaceTarget ? SURFACE_TARGET_DISTANCE : TARGET_REACHED_DISTANCE;
        }

        private double desiredArrivalVelocityX() {
            return arrivalDirectionX * arrivalSpeed;
        }

        private double desiredArrivalVelocityY() {
            return arrivalDirectionY * arrivalSpeed;
        }

        private double arrivalVelocityError() {
            return Math.hypot(desiredArrivalVelocityX() - velocityX,
                    desiredArrivalVelocityY() - velocityY);
        }

        private boolean hasArrived() {
            return hasTarget && distanceToTarget() <= targetReachedDistance()
                    && arrivalVelocityError() <= ARRIVAL_SPEED_TOLERANCE;
        }

        private void stopBrakingEngines() {
            segments.values().stream().filter(segment -> segment.brakingBurn).forEach(segment -> {
                segment.enginesEnabled = false;
                segment.brakingBurn = false;
            });
        }

        private boolean hasEnoughBrakingDeltaV(List<SpaceSimulation.SegmentRef> selected) {
            double mass = currentMass();
            if (mass <= 0) return false;
            double availableDeltaV = selectedSegments(selected).stream()
                    .mapToDouble(segment -> segment.thrust * segment.remainingBurnSeconds).sum() / mass;
            return availableDeltaV + ARRIVAL_SPEED_TOLERANCE >= arrivalVelocityError();
        }

        private boolean shouldStartBraking(List<SpaceSimulation.SegmentRef> selected) {
            double remainingDistance = distanceToTarget();
            if (!Double.isFinite(remainingDistance) || remainingDistance <= targetReachedDistance()) return true;

            double offsetX = targetX - x;
            double offsetY = targetY - y;
            double directionX = offsetX / remainingDistance;
            double directionY = offsetY / remainingDistance;
            double relativeClosingSpeed = (velocityX - desiredArrivalVelocityX()) * directionX
                    + (velocityY - desiredArrivalVelocityY()) * directionY;
            if (relativeClosingSpeed <= ARRIVAL_SPEED_TOLERANCE) return false;

            double thrust = selectedSegments(selected).stream()
                    .filter(segment -> segment.thrust > 0 && segment.remainingBurnSeconds > 0)
                    .mapToDouble(segment -> segment.thrust).sum();
            double acceleration = currentMass() <= 0 ? 0 : thrust / currentMass();
            if (acceleration <= 0) return false;
            double estimatedBrakingDistance = relativeClosingSpeed * relativeClosingSpeed / (2 * acceleration);
            if (remainingDistance > estimatedBrakingDistance * 1.25 + targetReachedDistance()) return false;

            // The cheap estimate keeps long flights fast. Near turnover, copying the small craft state gives us a
            // result that also accounts for gravity, other burning segments and propellant lost during braking.
            return brakingWouldReachTarget(selected, directionX, directionY);
        }

        private boolean brakingWouldReachTarget(List<SpaceSimulation.SegmentRef> selected,
                                                double initialDirectionX, double initialDirectionY) {
            var prediction = copyFor(Set.copyOf(segments.keySet()));
            prediction.collectSamples = false;
            prediction.setEngines(selected, true, true);
            double deadline = Math.min(prediction.simulationDeadline,
                    prediction.time + prediction.remainingActiveBurnSeconds() + 60);

            while (prediction.time < deadline && !prediction.crashed) {
                integrate(prediction, Math.min(integrationStep(prediction, deadline), deadline - prediction.time));
                double remainingAlongRoute = (targetX - prediction.x) * initialDirectionX
                        + (targetY - prediction.y) * initialDirectionY;
                if (prediction.arrivalVelocityError() <= ARRIVAL_SPEED_TOLERANCE) {
                    return remainingAlongRoute <= targetReachedDistance();
                }
                // Reaching the target plane while still too fast means braking any later would also overshoot.
                if (remainingAlongRoute <= targetReachedDistance()) return true;
                if (prediction.areSegmentsEmpty(selected)) return true;
            }
            return false;
        }

        private boolean beginStationKeeping(List<SpaceSimulation.SegmentRef> selected) {
            if (!hasSegments(selected)) return false;
            var refs = selected.isEmpty() ? new LinkedHashSet<>(segments.keySet()) : new LinkedHashSet<>(selected);
            boolean hasUsableEngine = refs.stream().map(segments::get)
                    .anyMatch(segment -> segment.thrust > 0 && segment.remainingBurnSeconds > 0);
            if (!hasUsableEngine) return false;
            segments.values().forEach(segment -> {
                segment.enginesEnabled = false;
                segment.brakingBurn = false;
            });
            stationKeepingSegments = Set.copyOf(refs);
            maintainingOrbit = true;
            velocityX = 0;
            velocityY = 0;
            return true;
        }

        private void integrateStationKeeping(double step) {
            var available = stationKeepingSegments.stream().map(segments::get)
                    .filter(segment -> segment.thrust > 0 && segment.remainingBurnSeconds > 0).toList();
            if (available.isEmpty()) {
                maintainingOrbit = false;
                integrate(this, step);
                return;
            }

            // Holding is intentionally abstract: higher gravity consumes more propellant, but the craft remains at
            // one map position. A later orbital simulation can replace this without changing the plan action.
            double gravity = SpaceSimulation.getGravityStrength((float) Math.max(0, y));
            double burnSeconds = step * (0.0001 + gravity * 0.002) / available.size();
            available.forEach(segment -> segment.remainingBurnSeconds =
                    Math.max(0, segment.remainingBurnSeconds - burnSeconds));
            velocityX = 0;
            velocityY = 0;
            time += step;
            addSample(false);
        }

        private void discard() {
            discarded = true;
            maintainingOrbit = false;
            velocityX = 0;
            velocityY = 0;
            segments.values().forEach(segment -> {
                segment.enginesEnabled = false;
                segment.brakingBurn = false;
            });
        }

        private boolean hasSegments(List<SpaceSimulation.SegmentRef> selected) {
            return selected.isEmpty() || selected.stream().allMatch(segments::containsKey);
        }

        private List<SegmentState> selectedSegments(List<SpaceSimulation.SegmentRef> selected) {
            if (selected.isEmpty()) return new ArrayList<>(segments.values());
            return selected.stream().map(segments::get).toList();
        }

        private boolean areSegmentsEmpty(List<SpaceSimulation.SegmentRef> selected) {
            return hasSegments(selected) && selectedSegments(selected).stream()
                    .noneMatch(segment -> segment.remainingBurnSeconds > 0);
        }

        private boolean canProduceThrust() {
            return !discarded && !maintainingOrbit
                    && segments.values().stream().anyMatch(SegmentState::canProduceThrust);
        }

        private boolean hasActiveBrakingBurn() {
            return segments.values().stream().anyMatch(segment -> segment.canProduceThrust() && segment.brakingBurn);
        }

        private double remainingActiveBurnSeconds() {
            return segments.values().stream().filter(segment -> segment.enginesEnabled)
                    .mapToDouble(segment -> segment.remainingBurnSeconds).max().orElse(0);
        }

        private double currentMass() {
            return segments.values().stream().mapToDouble(SegmentState::currentMass).sum();
        }

        private double remainingDeltaV() {
            double mass = currentMass();
            if (mass <= 0) return 0;
            return segments.values().stream()
                    .mapToDouble(segment -> segment.thrust * segment.remainingBurnSeconds).sum() / mass;
        }

        private void addSample(boolean force) {
            if (!collectSamples) return;
            if (!force && time + 0.0001 < nextSampleTime) return;
            var phase = canProduceThrust()
                    ? projected ? PathPhase.PROJECTED_BURN : PathPhase.PLANNED_BURN
                    : PathPhase.PLANNED_COAST;
            var sample = new PathSample(time, x, y, velocityX, velocityY, phase,
                    remainingDeltaV(), targetId, projected);
            if (samples.isEmpty() || force || !samePosition(samples.getLast(), sample)) samples.add(sample);
            nextSampleTime = time + settings.sampleIntervalSeconds;
        }

        private static boolean samePosition(PathSample first, PathSample second) {
            return Math.abs(first.x - second.x) < 0.001 && Math.abs(first.y - second.y) < 0.001
                    && first.phase == second.phase && first.targetId.equals(second.targetId)
                    && first.projected == second.projected;
        }

        private boolean isSettled() {
            if (maintainingOrbit) return false;
            if (canProduceThrust()) return false;
            if (Math.hypot(velocityX, velocityY) > STOPPED_SPEED) return false;
            return y <= 0 || SpaceSimulation.getGravityStrength((float) y) <= 0;
        }

        private CraftPath toPath(boolean planCompleted) {
            var terminal = discarded ? TerminalState.DISCARDED
                    : maintainingOrbit ? TerminalState.MAINTAINING_ORBIT
                    : crashed ? TerminalState.EARTH_IMPACT
                    : isSettled() ? TerminalState.STOPPED
                    : time >= simulationDeadline - 0.0001 ? TerminalState.MAX_DURATION
                    : planCompleted ? TerminalState.MAX_DURATION : TerminalState.PLAN_BLOCKED;
            boolean depleted = segments.values().stream().noneMatch(segment -> segment.remainingBurnSeconds > 0);
            return new CraftPath(branchId, Set.copyOf(segments.keySet()), List.copyOf(samples),
                    List.copyOf(actionMoments), time, remainingDeltaV(), depleted, terminal);
        }
    }
}
