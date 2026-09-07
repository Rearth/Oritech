package rearth.oritech.spaceage.simulation;

import static rearth.oritech.spaceage.simulation.RocketFlightPathState.BURN_TOLERANCE;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.ArrivalPrediction;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.NavigationAbortMoment;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.TerminalState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Runs one Navigate To card against an already-created craft. */
final class RocketFlightNavigation {

    // Limits keep one invalid card from locking up the planner.
    private static final int MAX_STEPS = 10_000;
    // Keep enough samples for smooth lines without growing paths without limit.
    private static final int MAX_SAMPLES = 1_000;
    // The preview does not need to simulate routes lasting more than a thousand Minecraft days.
    private static final double MAX_SECONDS = 1_200_000;
    // Tiny phases still need a useful time step.
    private static final double MIN_STEP_SECONDS = 0.05;
    // Arrival tolerances absorb normal floating-point integration leftovers.
    private static final double POSITION_TOLERANCE = 0.01;
    private static final double VELOCITY_TOLERANCE = 0.00001;
    private static final double MAX_TARGET_POINT_OFFSET = Math.toRadians(70);

    private RocketFlightNavigation() {
    }

    static boolean navigate(SpaceSimulation.FlightPlanAction action, Craft craft,
                            Context context) {
        var target = context.objects.get(action.targetId());
        if (target == null || craft.segments.isEmpty()) return false;
        while (RocketFlightPathCalculator.finishStage(action, craft, context)) {
            // Empty stages are valid when a saved plan is reused on a smaller rocket.
        }

        var destinationAngle = targetPointAngle(action, craft, target);
        var destination = targetPoint(target.xAt(craft.time), target.yAt(craft.time), target.radius(),
                action.orbit(), destinationAngle);
        var startTime = craft.time;
        var approachX = destination.x - craft.x;
        var approachY = destination.y - craft.y;
        var approachLength = Math.hypot(approachX, approachY);
        approachX = approachLength < 1 ? 1 : approachX / approachLength;
        approachY = approachLength < 1 ? 0 : approachY / approachLength;
        var targetSpeed = action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM
                ? action.targetVelocity() : 0;
        var targetVelocityX = target.velocityX() + approachX * targetSpeed;
        var targetVelocityY = target.velocityY() + approachY * targetSpeed;
        var samples = new ArrayList<PathSample>();
        TransferPlan transferPlan = null;

        var step = 0;
        for (; step < MAX_STEPS && craft.time - startTime < MAX_SECONDS; step++) {
            destination = targetPoint(target.xAt(craft.time), target.yAt(craft.time), target.radius(),
                    action.orbit(), destinationAngle);
            var offsetX = destination.x - craft.x;
            var offsetY = destination.y - craft.y;
            if (completeArrival(action, craft, target, destination, targetVelocityX, targetVelocityY, samples, context)) {
                return true;
            }

            var active = craft.activeSegments(context);
            // Delta-v burns at the same rate it changes velocity, so resources and movement stay in sync.
            var acceleration = craft.deltaVPerSecond(active);
            if (transferPlan == null) {
                var transfer = FullPowerTransfer.solve(offsetX, offsetY, craft.velocityX - target.velocityX(),
                        craft.velocityY - target.velocityY(), targetVelocityX - target.velocityX(),
                        targetVelocityY - target.velocityY(),
                        action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.MAXIMUM, craft.burnProfile(context),
                        action.maxSpeed() == 0 ? Double.POSITIVE_INFINITY : action.maxSpeed(),
                        MAX_SECONDS - (craft.time - startTime));
                if (transfer == null) {
                    craft.blockedState = acceleration <= 0.0001
                            ? TerminalState.NO_ACTIVE_ENGINES
                            : TerminalState.NO_FEASIBLE_TRANSFER;
                    appendSamples(craft, samples);
                    return false;
                }
                transferPlan = new TransferPlan(craft.time, transfer);
            }
            var abort = navigationAbort(action, craft, destination, transferPlan);
            if (abort != null) {
                samples.add(craft.createSample(PathPhase.COAST, action.targetId(), action.id(),
                        Set.of()));
                appendSamples(craft, samples);
                if (action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == SpaceSimulation.OrbitBand.SURFACE) {
                    craft.lastEarthSurfaceAction = action;
                }
                context.navigationAborts.add(new NavigationAbortMoment(action.id(), craft.branchId,
                        abort.addon.id(), abort.actualValue, craft.time, craft.x, craft.y, destination.x, destination.y));
                return true;
            }
            var command = transferPlan.commandAt(craft, acceleration);
            if (command.stepLimitSeconds <= BURN_TOLERANCE) {
                craft.blockedState = TerminalState.NO_FEASIBLE_TRANSFER;
                appendSamples(craft, samples);
                return false;
            }
            var stepSeconds = Math.min(command.sampleStepSeconds, command.stepLimitSeconds);
            if (command.burning) {
                if (active.isEmpty() || acceleration <= 0.0001) {
                    appendSamples(craft, samples);
                    return false;
                }
                // Stop exactly at fuel boundaries so stage markers do not drift.
                var nextEngineStop = active.stream().map(craft.segments::get)
                        .mapToDouble(segment -> segment.remainingBurnSeconds).min().orElse(0);
                stepSeconds = Math.min(stepSeconds, nextEngineStop);
            } else if (Math.hypot(craft.velocityX, craft.velocityY) <= 0.0001
                    && !Double.isFinite(command.stepLimitSeconds)) {
                appendSamples(craft, samples);
                return false;
            }
            stepSeconds = Math.min(stepSeconds, MAX_SECONDS - (craft.time - startTime));
            if (stepSeconds <= BURN_TOLERANCE) break;

            craft.advance(command.burning, command.directionX, command.directionY, command.acceleration, active, stepSeconds);
            samples.add(craft.createSample(command.phase, action.targetId(), action.id(),
                    command.burning ? Set.copyOf(active) : Set.of()));
            if (RocketFlightPathCalculator.finishStage(action, craft, context)) {
                samples.add(craft.createSample(PathPhase.COAST, action.targetId(), action.id(),
                        Set.of()));
                while (RocketFlightPathCalculator.finishStage(action, craft, context)) {
                    // Continue over later empty stages.
                }
            }
        }
        if (completeArrival(action, craft, target, destination, targetVelocityX, targetVelocityY, samples, context)) return true;
        craft.blockedState = step >= MAX_STEPS ? TerminalState.INTEGRATION_STEP_LIMIT
                : TerminalState.INTEGRATION_TIME_LIMIT;
        appendSamples(craft, samples);
        return false;
    }

    private static Abort navigationAbort(SpaceSimulation.FlightPlanAction action, Craft craft,
                                         Point destination, TransferPlan transferPlan) {
        if (action.addons().isEmpty()) return null;
        var addon = action.addons().getFirst();
        var distance = Math.hypot(destination.x - craft.x, destination.y - craft.y);
        var actual = switch (addon.type()) {
            case DISTANCE_FROM_TARGET -> distance;
            case TIME_BEFORE_ARRIVAL -> Math.max(0, transferPlan.transfer.duration() - (craft.time - transferPlan.startTime));
            case DESIRED_UNCERTAINTY -> AsteroidImpactRules.landingUncertaintyBlocks(distance);
        };
        return actual <= addon.value() ? new Abort(addon, actual) : null;
    }

    private static boolean completeArrival(SpaceSimulation.FlightPlanAction action, Craft craft,
                                           SpaceSimulation.SpaceObjectData target, Point destination,
                                           double targetVelocityX, double targetVelocityY,
                                           List<PathSample> samples,
                                           Context context) {
        var distance = Math.hypot(destination.x - craft.x, destination.y - craft.y);
        if (distance > POSITION_TOLERANCE || (action.velocityMode() != SpaceSimulation.ArrivalVelocityMode.MAXIMUM
                && !(Math.hypot(craft.velocityX - targetVelocityX, craft.velocityY - targetVelocityY)
                <= VELOCITY_TOLERANCE * 2))) return false;
        craft.x = destination.x;
        craft.y = destination.y;
        if (action.velocityMode() != SpaceSimulation.ArrivalVelocityMode.MAXIMUM) {
            craft.velocityX = targetVelocityX;
            craft.velocityY = targetVelocityY;
        }
        samples.add(craft.createSample(PathPhase.COAST, action.targetId(), action.id(), Set.of()));
        appendSamples(craft, samples);
        craft.currentTarget = target.id();
        craft.currentOrbit = action.orbit();
        if (action.orbit() == SpaceSimulation.OrbitBand.SURFACE) {
            if (target.id().equals(SpaceObjects.EARTH_ID)) craft.lastEarthSurfaceAction = action;
            var relativeSpeed = Math.hypot(craft.velocityX - target.velocityX(), craft.velocityY - target.velocityY());
            var prediction = AsteroidImpactRules.predictArrival(craft.rocketMass(), target, relativeSpeed,
                    craft.attachedAsteroid, action);
            context.arrivalPredictions.add(new ArrivalPrediction(action.id(), craft.branchId,
                    target.id(), prediction));
            if (prediction.outcome() != AsteroidImpactRules.ArrivalOutcome.SAFE_APPROACH) craft.destroyed = true;
        }
        return true;
    }

    private static void appendSamples(Craft craft,
                                      List<PathSample> samples) {
        if (samples.isEmpty()) return;
        var stride = Math.max(1, (int) Math.ceil(samples.size() / (double) MAX_SAMPLES));
        PathSample previous = null;
        for (var index = 0; index < samples.size(); index++) {
            var sample = samples.get(index);
            var changed = previous == null || sample.phase() != previous.phase() || sample.stage() != previous.stage()
                    || !sample.connectedSegments().equals(previous.connectedSegments())
                    || !sample.firingSegments().equals(previous.firingSegments());
            // Keep both sides of a state boundary so downsampling never paints a burn as a long coast.
            if (changed && previous != null && craft.samples.getLast() != previous) craft.samples.add(previous);
            if (changed || index % stride == 0 || index == samples.size() - 1) craft.samples.add(sample);
            previous = sample;
        }
    }

    private static double sampleStep(double seconds) {
        // Constant acceleration integrates exactly, so this fixed spacing remains smooth on long transfers.
        return Math.max(MIN_STEP_SECONDS, seconds / 160);
    }

    private static PathPhase phaseFor(Craft craft, double x, double y) {
        var speed = Math.hypot(craft.velocityX, craft.velocityY);
        var speedChange = speed < 0.0001 ? 1 : (x * craft.velocityX + y * craft.velocityY) / speed;
        return speedChange > 0.2 ? PathPhase.ACCELERATE
                : speedChange < -0.2 ? PathPhase.BRAKE
                : PathPhase.REDIRECT;
    }

    private static double targetPointAngle(SpaceSimulation.FlightPlanAction action, Craft craft,
                                           SpaceSimulation.SpaceObjectData target) {
        var nearestAngle = Math.atan2(craft.y - target.yAt(craft.time), craft.x - target.xAt(craft.time));
        return nearestAngle + targetPointOffset(action);
    }

    /** Stable per-card offset keeps each approach point in the same place after recalculation. */
    static double targetPointOffset(SpaceSimulation.FlightPlanAction action) {
        var seed = action.id().getMostSignificantBits() ^ Long.rotateLeft(action.id().getLeastSignificantBits(), 32);
        return (new Random(seed).nextDouble() * 2 - 1) * MAX_TARGET_POINT_OFFSET;
    }

    private static Point targetPoint(double x, double y, double radius, SpaceSimulation.OrbitBand orbit, double angle) {
        var orbitRadius = radius + orbit.altitude();
        return new Point(x + Math.cos(angle) * orbitRadius, y + Math.sin(angle) * orbitRadius);
    }

    /** A solved burn/coast/burn route anchored to the shared preview clock. */
    private record TransferPlan(double startTime, FullPowerTransfer transfer) {
        GuidanceCommand commandAt(Craft craft, double acceleration) {
            var elapsed = Math.max(0, craft.time - startTime);
            var endFirst = transfer.firstSeconds();
            var endCoast = endFirst + transfer.coastSeconds();
            double x, y, remaining;
            if (elapsed < endFirst - 1e-7) {
                x = transfer.firstDirectionX();
                y = transfer.firstDirectionY();
                remaining = endFirst - elapsed;
            } else if (elapsed < endCoast - 1e-7) {
                return GuidanceCommand.coast(endCoast - elapsed, sampleStep(transfer.coastSeconds()));
            } else {
                x = transfer.lastDirectionX();
                y = transfer.lastDirectionY();
                remaining = Math.max(0, transfer.duration() - elapsed);
            }
            if (x == 0 && y == 0) return GuidanceCommand.coast(remaining, sampleStep(remaining));
            return new GuidanceCommand(true, x, y, acceleration, remaining,
                    sampleStep(Math.max(transfer.firstSeconds(), transfer.lastSeconds())), phaseFor(craft, x, y));
        }
    }

    /** One constant-acceleration integration step from the solved route. */
    private record GuidanceCommand(boolean burning, double directionX, double directionY, double acceleration,
                                   double stepLimitSeconds, double sampleStepSeconds,
                                   PathPhase phase) {
        static GuidanceCommand coast(double seconds, double stepSeconds) {
            return new GuidanceCommand(false, 0, 0, 0, seconds, stepSeconds,
                    PathPhase.COAST);
        }
    }

    private record Abort(SpaceSimulation.ActionAddon addon, double actualValue) {
    }

    private record Point(double x, double y) {
    }
}
