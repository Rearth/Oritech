package rearth.oritech.spaceage.simulation.flight;

import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.ArrivalPrediction;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.NavigationAbortMoment;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.NavigationFailure;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.power.SolarPower;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.BURN_TOLERANCE;

/**
 * Runs one Navigate To card against an already-created craft.
 */
final class RocketFlightNavigation {

    // Limits keep one invalid card from locking up the planner.
    private static final int MAX_STEPS = 10_000;
    // Arrival tolerances absorb normal floating-point integration leftovers.
    private static final double POSITION_TOLERANCE = 0.01;
    private static final double VELOCITY_TOLERANCE = 0.00001;

    private RocketFlightNavigation() {
    }

    static boolean navigate(SpaceSimulation.FlightPlanAction action, Craft craft, Context context) {

        var target = context.objects.get(action.targetId());
        if (target == null) {
            craft.blockedState = TerminalState.TARGET_UNAVAILABLE;
            return false;
        }
        if (target.type() == SpaceObjects.ObjectType.CRAFT) {
            // Stations are frozen when the card is created; any later host movement invalidates the card.
            var frozen = action.settings().docking();
            var station = context.stations.get(target.id());
            if (station == null || !frozen.host().equals(target.id()) || station.revision() != frozen.revision()
                    || Math.hypot(station.position().x() - frozen.x(), station.position().y() - frozen.y()) > .001) {
                craft.blockedState = TerminalState.TARGET_UNAVAILABLE;
                return false;
            }
        }
        if (missingSolar(target, craft, context)) return false;
        var angle = targetPointAngle(action, craft, target);
        return navigateLeg(action, craft, context, angle);
    }

    /** Only solar craft may approach the Sun collector ring or stations inside it. */
    private static boolean missingSolar(SpaceSimulation.SpaceObjectData target, Craft craft, Context context) {

        var station = context.stations.get(target.id());
        var solarTarget = target.id().equals(SpaceSimulation.SUN_ID)
                || target.type() == SpaceObjects.ObjectType.CRAFT && station != null && SolarPower.sunRegion(station.position());
        if (!solarTarget || SolarPower.rating(SolarPower.structures(craft)) > 0) return false;
        craft.blockedState = TerminalState.SOLAR_REQUIRED;
        return true;
    }

    private static boolean fail(Craft craft, List<PathSample> samples, TerminalState state) {

        craft.blockedState = state;
        appendSamples(craft, samples);
        return false;
    }

    private static boolean exceedsTimeLimit(RocketTransferRoute.Route route, Craft craft) {

        if (route == null || route.duration() <= SpaceBalance.MAX_TRANSFER_SECONDS) return false;
        craft.navigationFailure = new NavigationFailure("time_limit", route.duration(), SpaceBalance.MAX_TRANSFER_SECONDS);
        craft.blockedState = TerminalState.INTEGRATION_TIME_LIMIT;
        return true;
    }

    private static boolean navigateLeg(SpaceSimulation.FlightPlanAction action, Craft craft,
                                       Context context, double destinationAngle) {

        var target = context.objects.get(action.targetId());
        if (craft.segments.isEmpty()) {
            craft.blockedState = TerminalState.EMPTY_CRAFT;
            return false;
        }
        if (target.type() == SpaceObjects.ObjectType.ASTEROID && action.orbit() == SpaceSimulation.OrbitBand.SURFACE
                && target.detectionState() != SpaceObjects.DetectionState.PRECISE) {
            craft.blockedState = TerminalState.PRECISE_POSITION_REQUIRED;
            return false;
        }
        while (RocketFlightPathCalculator.finishStage(action, craft, context)) {
            // Empty stages are valid when a saved plan is reused on a smaller rocket.
        }

        var earth = context.objects.get(SpaceObjects.EARTH_ID);
        craft.atmosphere = earth != null && earth.radius() > 0
                && (Math.hypot(craft.x - earth.x(), craft.y - earth.y()) < earth.radius() + SpaceSimulation.OrbitBand.LOW.altitude() - 0.1
                || target.id().equals(earth.id()) && action.orbit() == SpaceSimulation.OrbitBand.SURFACE);
        var destination = targetPoint(target.xAt(craft.time), target.yAt(craft.time), SurveyRules.navigationRadius(target),
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
        if (action.orbit() == SpaceSimulation.OrbitBand.SURFACE && RocketTransferRoute.hasSurface(target)) {
            targetVelocityX = target.velocityX() - Math.cos(destinationAngle) * targetSpeed;
            targetVelocityY = target.velocityY() - Math.sin(destinationAngle) * targetSpeed;
        }
        var samples = new ArrayList<PathSample>();
        TransferPlan transferPlan = null;
        double travelled = 0;
        var speed = Math.hypot(craft.velocityX - target.velocityX(), craft.velocityY - target.velocityY());

        var step = 0;
        for (; step < MAX_STEPS && craft.time - startTime < SpaceBalance.MAX_TRANSFER_SECONDS; step++) {
            destination = targetPoint(target.xAt(craft.time), target.yAt(craft.time), SurveyRules.navigationRadius(target),
                    action.orbit(), destinationAngle);
            if (completeArrival(action, craft, target, destination, targetVelocityX, targetVelocityY, samples, context, transferPlan == null ? null : transferPlan.transfer)) {
                return true;
            }

            var active = craft.activeSegments(context);
            // Delta-v burns at the same rate it changes velocity, so resources and movement stay in sync.
            var acceleration = craft.deltaVPerSecond(active);
            if (transferPlan == null) {
                var transfer = RocketTransferRoute.solve(craft, context, action, target, destination.x, destination.y);
                if (exceedsTimeLimit(transfer, craft)) return false;
                if (transfer == null)
                    return fail(craft, samples, acceleration <= 0.0001 ? TerminalState.NO_ACTIVE_ENGINES : TerminalState.NO_FEASIBLE_TRANSFER);
                transferPlan = new TransferPlan(craft.time, transfer);
                var end = transfer.curve().atDistance(transfer.curve().length(), targetSpeed);
                targetVelocityX = target.velocityX() + end.vx();
                targetVelocityY = target.velocityY() + end.vy();
            }
            var abort = navigationAbort(action, craft, destination, transferPlan, context);
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
            craft.atmosphere = transferPlan.atmosphereAt(craft.time);
            acceleration = craft.deltaVPerSecond(active) * transferPlan.transfer.efficiency();
            var command = transferPlan.commandAt(craft);
            if (command.stepLimitSeconds <= BURN_TOLERANCE) return fail(craft, samples, TerminalState.NO_FEASIBLE_TRANSFER);
            var stepSeconds = Math.min(command.stepLimitSeconds, transferPlan.transfer.duration() / 96);
            var distanceStep = transferPlan.transfer.curve().length() / 96;
            if (speed > .001) stepSeconds = Math.min(stepSeconds, distanceStep / speed);
            if (command.burning && command.directionX > 0)
                stepSeconds = Math.min(stepSeconds, Math.sqrt(2 * distanceStep / acceleration));
            if (command.burning) {
                if (active.isEmpty() || acceleration <= 0.0001) return fail(craft, samples, TerminalState.NO_ACTIVE_ENGINES);
                // Stop exactly at fuel boundaries so stage markers do not drift.
                var factor = command.directionX < 0 ? Math.max(1e-9, command.fuelFactor) : 1;
                var nextEngineStop = active.stream().map(craft.segments::get)
                        .mapToDouble(segment -> segment.burnSeconds(factor)).min().orElse(0);
                stepSeconds = Math.min(stepSeconds, nextEngineStop);
            } else if (Math.hypot(craft.velocityX, craft.velocityY) <= 0.0001 && !Double.isFinite(command.stepLimitSeconds)) {
                return fail(craft, samples, TerminalState.NO_FEASIBLE_TRANSFER);
            }
            stepSeconds = Math.min(stepSeconds, SpaceBalance.MAX_TRANSFER_SECONDS - (craft.time - startTime));
            if (stepSeconds <= BURN_TOLERANCE) break;

            var phase = !command.burning ? PathPhase.COAST
                    : command.directionX > 0 ? PathPhase.ACCELERATE : PathPhase.BRAKE;
            // Gravity also acts while coasting, matching the transfer solver.
            var signedAcceleration = (command.burning ? command.directionX * acceleration : 0) + transferPlan.transfer.gravity();
            travelled += speed * stepSeconds + .5 * signedAcceleration * stepSeconds * stepSeconds;
            speed = Math.max(0, speed + signedAcceleration * stepSeconds);
            craft.time += stepSeconds;
            if (command.burning) craft.consumeBurnTime(active, stepSeconds * (command.directionX < 0 ? command.fuelFactor : 1));
            craft.spendRadio(stepSeconds);
            var curve = transferPlan.transfer.curve();
            var position = curve.atDistance(travelled, speed);
            craft.x = position.x() + target.velocityX() * (craft.time - startTime);
            craft.y = position.y() + target.velocityY() * (craft.time - startTime);
            craft.velocityX = position.vx() + target.velocityX();
            craft.velocityY = position.vy() + target.velocityY();
            var heading = curve.atDistance(travelled, 1);
            craft.headingX = heading.vx();
            craft.headingY = heading.vy();
            samples.add(craft.createSample(phase, action.targetId(), action.id(),
                    command.burning ? Set.copyOf(active) : Set.of()));
            if (RocketFlightPathCalculator.finishStage(action, craft, context)) {
                samples.add(craft.createSample(PathPhase.COAST, action.targetId(), action.id(),
                        Set.of()));
                while (RocketFlightPathCalculator.finishStage(action, craft, context)) {
                    // Continue over later empty stages.
                }
            }
        }
        if (completeArrival(action, craft, target, destination, targetVelocityX, targetVelocityY, samples, context, transferPlan == null ? null : transferPlan.transfer))
            return true;
        return fail(craft, samples, step >= MAX_STEPS ? TerminalState.INTEGRATION_STEP_LIMIT : TerminalState.INTEGRATION_TIME_LIMIT);
    }

    private static Abort navigationAbort(SpaceSimulation.FlightPlanAction action, Craft craft,
                                         Point destination, TransferPlan transferPlan, Context context) {

        if (action.addons().isEmpty()) return null;
        var addon = action.addons().getFirst();
        if (!addon.type().isNavigationCondition()) return null;
        var distance = Math.hypot(destination.x - craft.x, destination.y - craft.y);
        var actual = switch (addon.type()) {
            case DISTANCE_FROM_TARGET -> distance;
            case TIME_BEFORE_ARRIVAL ->
                    Math.max(0, transferPlan.transfer.duration() - (craft.time - transferPlan.startTime));
            case DESIRED_UNCERTAINTY -> AsteroidImpactRules.landingUncertaintyBlocks(distance);
            case LOW_RF, LOW_FUEL, DURATION -> Double.POSITIVE_INFINITY;
        };
        return actual <= addon.value() ? new Abort(addon, actual) : null;
    }

    private static boolean completeArrival(SpaceSimulation.FlightPlanAction action, Craft craft,
                                           SpaceSimulation.SpaceObjectData target, Point destination,
                                           double targetVelocityX, double targetVelocityY,
                                           List<PathSample> samples,
                                           Context context, RocketTransferRoute.Route route) {

        // Separations during the leg can remove the solar hardware.
        if (missingSolar(target, craft, context)) return false;
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
        if (action.orbit() == SpaceSimulation.OrbitBand.SURFACE && target.type() != SpaceObjects.ObjectType.SURVEY_REGION
                && target.type() != SpaceObjects.ObjectType.CRAFT && target.type() != SpaceObjects.ObjectType.WAYPOINT) {
            if (target.id().equals(SpaceObjects.EARTH_ID)) craft.lastEarthSurfaceAction = action;
            var relativeSpeed = Math.hypot(craft.velocityX - target.velocityX(), craft.velocityY - target.velocityY());
            var prediction = AsteroidImpactRules.predictArrival(craft.rocketMass(), target, relativeSpeed,
                    craft.attachedAsteroid, action, craft.explosiveEnergy(), craft.craftId);
            context.arrivalPredictions.add(new ArrivalPrediction(action.id(), craft.branchId,
                    target.id(), prediction, route == null ? ParachuteLanding.Result.NONE : route.parachutes()));
            if (prediction.outcome() != AsteroidImpactRules.ArrivalOutcome.SAFE_APPROACH) craft.destroyed = true;
        }
        return true;
    }

    private static void appendSamples(Craft craft,
                                      List<PathSample> samples) {

        if (samples.isEmpty()) return;
        for (var sample : samples) {
            var size = craft.samples.size();
            if (size >= 2) {
                var a = craft.samples.get(size - 2);
                var b = craft.samples.get(size - 1);
                var first = b.timeSeconds() - a.timeSeconds();
                var second = sample.timeSeconds() - b.timeSeconds();
                if (first > 0 && second > 0 && b.phase() == sample.phase() && b.stage() == sample.stage()
                        && b.actionId().equals(sample.actionId()) && b.connectedSegments().equals(sample.connectedSegments())
                        && b.firingSegments().equals(sample.firingSegments()) && b.attachedAsteroidId().equals(sample.attachedAsteroidId())
                        && sameMotion(a, b, sample)) {
                    craft.samples.removeLast();
                }
            }
            craft.samples.add(sample);
        }
    }

    private static boolean sameMotion(PathSample a, PathSample b, PathSample c) {

        var state = new FlightMotion(a, c).at((b.timeSeconds() - a.timeSeconds()) / (c.timeSeconds() - a.timeSeconds()));
        return Math.hypot(state.x() - b.x(), state.y() - b.y()) < 1e-5
                && Math.hypot(state.vx() - b.velocityX(), state.vy() - b.velocityY()) < 1e-7;
    }

    private static double targetPointAngle(SpaceSimulation.FlightPlanAction action, Craft craft,
                                           SpaceSimulation.SpaceObjectData target) {

        if (target.id().equals(SpaceObjects.EARTH_ID) && action.orbit() == SpaceSimulation.OrbitBand.SURFACE)
            return SpaceBalance.surfaceAngle((double) action.landingX() + action.landingOffsetX(),
                    (double) action.landingZ() + action.landingOffsetZ());
        if (SpaceBalance.hasSlots(target.id(), action.orbit())
                && action.service().slot() >= 0)
            return Math.PI + Math.PI * 2 * action.service().slot() / SpaceBalance.slots(target.id(), action.orbit());
        var nearestAngle = Math.atan2(craft.y - target.yAt(craft.time), craft.x - target.xAt(craft.time));
        return nearestAngle + targetPointOffset(action);
    }

    /**
     * Stable per-card offset keeps each approach point in the same place after recalculation.
     */
    static double targetPointOffset(SpaceSimulation.FlightPlanAction action) {

        var seed = action.id().getMostSignificantBits() ^ Long.rotateLeft(action.id().getLeastSignificantBits(), 32);
        return (new Random(seed).nextDouble() * 2 - 1) * SpaceBalance.TARGET_ANGLE_SPREAD;
    }

    private static Point targetPoint(double x, double y, double radius, SpaceSimulation.OrbitBand orbit, double angle) {

        var orbitRadius = radius + orbit.altitude();
        return new Point(x + Math.cos(angle) * orbitRadius, y + Math.sin(angle) * orbitRadius);
    }

    /**
     * The complete card is solved before any fuel is consumed.
     */
    private record TransferPlan(double startTime, RocketTransferRoute.Route transfer) {

        boolean atmosphereAt(double time) {

            var elapsed = Math.max(0, time - startTime);
            for (var burn : transfer.burns()) {
                if (elapsed < burn.seconds() - 1e-7) return burn.atmosphere();
                elapsed -= burn.seconds();
            }
            return false;
        }

        GuidanceCommand commandAt(Craft craft) {

            var elapsed = Math.max(0, craft.time - startTime);
            for (var burn : transfer.burns()) {
                if (elapsed < burn.seconds() - 1e-7) {
                    var remaining = burn.seconds() - elapsed;
                    if (!burn.firing()) return new GuidanceCommand(false, 0, 1, remaining);
                    return new GuidanceCommand(true, burn.direction(), burn.fuelFactor(), remaining);
                }
                elapsed -= burn.seconds();
            }
            return new GuidanceCommand(false, 0, 1, 0);
        }
    }

    /**
     * Longitudinal thrust sign and time remaining in the current engine phase.
     */
    private record GuidanceCommand(boolean burning, double directionX, double fuelFactor, double stepLimitSeconds) {

    }

    private record Abort(SpaceSimulation.ActionAddon addon, double actualValue) {

    }

    private record Point(double x, double y) {

    }
}
