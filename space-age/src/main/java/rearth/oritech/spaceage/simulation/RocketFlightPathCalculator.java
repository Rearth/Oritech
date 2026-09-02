package rearth.oritech.spaceage.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Executes a flight plan against a lightweight two-dimensional physics model.
 * The result is independent of rendering and can later be used to schedule the
 * actual server-side flight as well as client previews.
 */
public final class RocketFlightPathCalculator {

    private static final double EARTH_GRAVITY = 9.80665;
    private static final double TARGET_REACHED_DISTANCE = 1_000;
    private static final double LOW_ORBIT_DISTANCE = 1_000;
    private static final double MEDIUM_ORBIT_DISTANCE = 20_000;
    private static final double HIGH_ORBIT_DISTANCE = 40_000;
    private static final double ATMOSPHERE_DISTANCE = 100_000;
    private static final double SPACE_DRAG = 0.0001;
    private static final double STOPPED_SPEED = 0.1;

    public static final Settings DEFAULT_SETTINGS = new Settings(0.25, 2, 1, 21_600);

    private RocketFlightPathCalculator() {
    }

    public static FlightPath calculate(RocketPerformance performance,
                                       List<SpaceSimulation.SpaceObjectData> objects,
                                       List<SpaceSimulation.FlightPlanAction> actions) {
        return calculate(performance, objects, actions, DEFAULT_SETTINGS);
    }

    public static FlightPath calculate(RocketPerformance performance,
                                       List<SpaceSimulation.SpaceObjectData> objects,
                                       List<SpaceSimulation.FlightPlanAction> actions,
                                       Settings settings) {
        var objectsById = new HashMap<UUID, SpaceSimulation.SpaceObjectData>();
        objects.forEach(object -> objectsById.put(object.id(), object));

        var state = new MutableState(performance, settings);
        state.addSample(true);
        var actionMoments = new ArrayList<ActionMoment>();

        boolean planCompleted = true;
        for (int index = 0; index < actions.size(); index++) {
            var action = actions.get(index);
            boolean completed = executeAction(action, state, objectsById);
            state.addSample(true);
            actionMoments.add(new ActionMoment(index, action.id(), state.time, state.x, state.y, completed));
            if (!completed) {
                planCompleted = false;
                break;
            }
        }

        if (planCompleted && !state.crashed) {
            state.projected = true;
            state.addSample(true);
            advanceToTerminalState(state, objectsById);
        }
        state.addSample(true);
        var terminalState = state.crashed ? TerminalState.EARTH_IMPACT
                : state.isSettled() ? TerminalState.STOPPED
                : planCompleted ? TerminalState.MAX_DURATION : TerminalState.PLAN_BLOCKED;
        return new FlightPath(List.copyOf(state.samples), List.copyOf(actionMoments), state.time,
                state.remainingDeltaV, state.remainingBurnSeconds <= 0, terminalState);
    }

    private static boolean executeAction(SpaceSimulation.FlightPlanAction action, MutableState state,
                                         Map<UUID, SpaceSimulation.SpaceObjectData> objects) {
        return switch (action.type()) {
            case START_ENGINE_BURN -> {
                state.engineRunning = true;
                yield true;
            }
            case STOP_ENGINE_BURN -> {
                state.engineRunning = false;
                yield true;
            }
            case SET_NAVIGATION_TARGET -> {
                if (!objects.containsKey(action.targetId())) yield false;
                state.targetId = action.targetId();
                yield true;
            }
            case WAIT_TICKS -> advanceFor(action.value() / 20d, state, objects, ignored -> false);
            case WAIT_SECONDS -> advanceFor(action.value(), state, objects, ignored -> false);
            case WAIT_UNTIL_DISTANCE -> waitForDistance(action, state, objects);
            case WAIT_FOR_EVENT -> waitForEvent(action, state, objects);
            case DISABLE_COUPLINGS, OPEN_PARACHUTES -> true;
        };
    }

    private static boolean waitForDistance(SpaceSimulation.FlightPlanAction action, MutableState state,
                                           Map<UUID, SpaceSimulation.SpaceObjectData> objects) {
        UUID referenceId = action.targetId().equals(SpaceSimulation.FlightPlanAction.CURRENT_TARGET)
                ? state.targetId : SpaceObjects.EARTH_ID;
        var reference = objects.get(referenceId);
        if (reference == null) return false;

        double threshold = action.value();
        double initialDifference = distance(state.x, state.y, reference.x(), reference.y()) - threshold;
        if (Math.abs(initialDifference) < 0.5) return true;
        boolean initiallyOutside = initialDifference > 0;
        return advanceUntil(state, objects, current -> {
            double difference = distance(current.x, current.y, reference.x(), reference.y()) - threshold;
            return initiallyOutside ? difference <= 0 : difference >= 0;
        });
    }

    private static boolean waitForEvent(SpaceSimulation.FlightPlanAction action, MutableState state,
                                        Map<UUID, SpaceSimulation.SpaceObjectData> objects) {
        var events = SpaceSimulation.WaitEvent.values();
        int eventIndex = (int) Math.clamp(action.value(), 0, events.length - 1);
        Predicate<MutableState> condition = switch (events[eventIndex]) {
            case LOW_ORBIT_REACHED -> current -> current.y >= LOW_ORBIT_DISTANCE;
            case MEDIUM_ORBIT_REACHED -> current -> current.y >= MEDIUM_ORBIT_DISTANCE;
            case HIGH_ORBIT_REACHED -> current -> current.y >= HIGH_ORBIT_DISTANCE;
            case ATMOSPHERE_EXITED -> current -> current.y >= ATMOSPHERE_DISTANCE;
            case TARGET_REACHED -> current -> {
                var target = objects.get(current.targetId);
                return target != null && distance(current.x, current.y, target.x(), target.y()) <= TARGET_REACHED_DISTANCE;
            };
            case FUEL_EMPTY -> current -> current.remainingBurnSeconds <= 0 || current.remainingDeltaV <= 0;
        };
        if (condition.test(state)) return true;
        return advanceUntil(state, objects, condition);
    }

    private static boolean advanceFor(double seconds, MutableState state,
                                      Map<UUID, SpaceSimulation.SpaceObjectData> objects,
                                      Predicate<MutableState> stopCondition) {
        double requestedTime = state.time + Math.max(0, seconds);
        double targetTime = requestedTime;
        while (state.time < targetTime) {
            integrate(state, objects, Math.min(integrationStep(state, targetTime), targetTime - state.time));
            if (stopCondition.test(state)) return true;
            if (state.crashed) return false;
        }
        return state.time >= targetTime;
    }

    private static boolean advanceUntil(MutableState state,
                                        Map<UUID, SpaceSimulation.SpaceObjectData> objects,
                                        Predicate<MutableState> condition) {
        double deadline = terminalDeadline(state);
        while (state.time < deadline) {
            integrate(state, objects, Math.min(state.settings.longStepSeconds,
                    deadline - state.time));
            if (condition.test(state)) return true;
            if (state.crashed || state.isSettled()) return false;
        }
        return false;
    }

    private static void advanceToTerminalState(MutableState state,
                                               Map<UUID, SpaceSimulation.SpaceObjectData> objects) {
        double deadline = terminalDeadline(state);
        while (state.time < deadline && !state.crashed && !state.isSettled()) {
            integrate(state, objects, Math.min(state.settings.longStepSeconds,
                    deadline - state.time));
        }
    }

    private static double terminalDeadline(MutableState state) {
        double remainingBurn = state.canProduceThrust() ? state.remainingBurnSeconds : 0;
        return state.time + remainingBurn + state.settings.maxCoastSeconds;
    }

    private static double integrationStep(MutableState state, double targetTime) {
        return targetTime - state.time > 60 ? state.settings.longStepSeconds : state.settings.stepSeconds;
    }

    private static void integrate(MutableState state,
                                  Map<UUID, SpaceSimulation.SpaceObjectData> objects, double step) {
        double accelerationX = 0;
        double accelerationY = -EARTH_GRAVITY * SpaceSimulation.getGravityStrength((float) Math.max(0, state.y));
        boolean producingThrust = state.canProduceThrust();
        if (!producingThrust) {
            accelerationX -= state.velocityX * SPACE_DRAG;
            accelerationY -= state.velocityY * SPACE_DRAG;
        }

        if (producingThrust) {
            var target = objects.get(state.targetId);
            double directionX = target == null ? 0 : target.x() - state.x;
            double directionY = target == null ? 1 : target.y() - state.y;
            double directionLength = Math.hypot(directionX, directionY);
            if (directionLength < 0.0001) {
                directionX = 0;
                directionY = 1;
                directionLength = 1;
            }

            double fuelRatio = state.initialBurnSeconds <= 0 ? 0
                    : state.remainingBurnSeconds / state.initialBurnSeconds;
            double currentMass = state.dryMass + state.fuelMass * Math.clamp(fuelRatio, 0, 1);
            double thrustAcceleration = currentMass <= 0 ? 0 : state.thrust / currentMass;
            double thrustStep = Math.min(step, state.remainingBurnSeconds);
            double appliedDeltaV = Math.min(state.remainingDeltaV, thrustAcceleration * thrustStep);
            double appliedAcceleration = appliedDeltaV / step;
            accelerationX += directionX / directionLength * appliedAcceleration;
            accelerationY += directionY / directionLength * appliedAcceleration;
            state.remainingDeltaV -= appliedDeltaV;
            state.remainingBurnSeconds = Math.max(0, state.remainingBurnSeconds - thrustStep);
        }

        state.velocityX += accelerationX * step;
        state.velocityY += accelerationY * step;
        state.x += state.velocityX * step;
        state.y += state.velocityY * step;
        state.time += step;

        // The planner starts at Earth's surface. Ground contact prevents an idle
        // or underpowered preview from falling into negative simulation space.
        if (state.y > 1) state.leftGround = true;
        if (state.y <= 0) {
            if (state.leftGround && state.velocityY < 0) state.crashed = true;
            state.y = 0;
            state.velocityY = state.crashed ? 0 : Math.max(0, state.velocityY);
        }
        state.addSample(false);
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    public record Settings(double stepSeconds, double longStepSeconds, double sampleIntervalSeconds,
                           double maxCoastSeconds) {
        public Settings {
            if (stepSeconds <= 0 || longStepSeconds < stepSeconds || sampleIntervalSeconds <= 0
                    || maxCoastSeconds <= 0) {
                throw new IllegalArgumentException("Flight path settings must use positive durations");
            }
        }
    }

    public record PathSample(double timeSeconds, double x, double y, double velocityX, double velocityY,
                             PathPhase phase, double remainingDeltaV, UUID targetId, boolean projected) {
    }

    public record ActionMoment(int actionIndex, UUID actionId, double timeSeconds,
                               double x, double y, boolean completed) {
    }

    public record FlightPath(List<PathSample> samples, List<ActionMoment> actionMoments,
                             double durationSeconds, double remainingDeltaV, boolean fuelDepleted,
                             TerminalState terminalState) {
    }

    public enum PathPhase {
        PLANNED_BURN,
        PLANNED_COAST,
        PROJECTED_BURN
    }

    public enum TerminalState {
        STOPPED,
        EARTH_IMPACT,
        PLAN_BLOCKED,
        MAX_DURATION
    }

    private static final class MutableState {
        private final Settings settings;
        private final List<PathSample> samples = new ArrayList<>();
        private final double dryMass;
        private final double fuelMass;
        private final double thrust;
        private final double initialBurnSeconds;
        private double remainingBurnSeconds;
        private double remainingDeltaV;
        private double nextSampleTime;
        private double time;
        private double x;
        private double y;
        private double velocityX;
        private double velocityY;
        private boolean engineRunning;
        private boolean projected;
        private boolean leftGround;
        private boolean crashed;
        private UUID targetId = SpaceSimulation.FlightPlanAction.NO_TARGET;

        private MutableState(RocketPerformance performance, Settings settings) {
            this.settings = settings;
            this.dryMass = performance.dryMassKilograms();
            this.fuelMass = performance.fuelMassKilograms();
            this.thrust = performance.thrustNewtons();
            this.initialBurnSeconds = performance.availableBurnSeconds();
            this.remainingBurnSeconds = performance.availableBurnSeconds();
            this.remainingDeltaV = performance.availableDeltaVMetersPerSecond();
        }

        private void addSample(boolean force) {
            if (!force && time + 0.0001 < nextSampleTime) return;
            var phase = canProduceThrust()
                    ? projected ? PathPhase.PROJECTED_BURN : PathPhase.PLANNED_BURN
                    : PathPhase.PLANNED_COAST;
            var sample = new PathSample(time, x, y, velocityX, velocityY, phase, remainingDeltaV,
                    targetId, projected);
            if (samples.isEmpty() || force || !samePosition(samples.getLast(), sample)) {
                samples.add(sample);
            }
            nextSampleTime = time + settings.sampleIntervalSeconds;
        }

        private static boolean samePosition(PathSample first, PathSample second) {
            return Math.abs(first.x - second.x) < 0.001 && Math.abs(first.y - second.y) < 0.001
                    && first.phase == second.phase && first.targetId.equals(second.targetId)
                    && first.projected == second.projected;
        }

        private boolean canProduceThrust() {
            return engineRunning && thrust > 0 && remainingBurnSeconds > 0 && remainingDeltaV > 0;
        }

        private boolean isSettled() {
            if (canProduceThrust()) return false;
            if (Math.hypot(velocityX, velocityY) > STOPPED_SPEED) return false;
            return y <= 0 || SpaceSimulation.getGravityStrength((float) y) <= 0;
        }
    }
}
