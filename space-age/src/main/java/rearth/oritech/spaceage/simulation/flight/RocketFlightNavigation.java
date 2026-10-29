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

/**
 * Runs one Navigate To card against an already-created craft.
 */
final class RocketFlightNavigation {

    // Arrival tolerances absorb normal floating-point integration leftovers.
    private static final double POSITION_TOLERANCE = 0.01;
    private static final double VELOCITY_TOLERANCE = 0.01;

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

    /**
     * Only solar craft may approach the Sun collector ring or stations inside it.
     */
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
        if (completeArrival(action, craft, target, destination, targetVelocityX, targetVelocityY, samples, context, null))
            return true;
        var route = RocketTransferRoute.solve(craft, context, action, target, destination.x, destination.y);
        if (exceedsTimeLimit(route, craft)) return false;
        if (route == null) return fail(craft, samples, TerminalState.NO_FEASIBLE_TRANSFER);
        var transferPlan = new TransferPlan(craft.time, route);
        var end = route.curve().atDistance(route.curve().length(), targetSpeed);
        targetVelocityX = target.velocityX() + end.vx();
        targetVelocityY = target.velocityY() + end.vy();
        samples.add(craft.createSample(PathPhase.COAST, action.targetId(), action.id(), Set.of()));
        for (var event : route.events()) {
            destination = targetPoint(target.xAt(craft.time), target.yAt(craft.time), SurveyRules.navigationRadius(target), action.orbit(), destinationAngle);
            var abort = navigationAbort(action, craft, destination, transferPlan, context);
            if (abort != null) {
                appendSamples(craft, samples);
                if (action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == SpaceSimulation.OrbitBand.SURFACE)
                    craft.lastEarthSurfaceAction = action;
                context.navigationAborts.add(new NavigationAbortMoment(action.id(), craft.branchId,
                        abort.addon.id(), abort.actualValue, craft.time, craft.x, craft.y, destination.x, destination.y));
                return true;
            }
            var seconds = eventDuration(action, craft, target, destination, transferPlan, event);
            craft.atmosphere = event.atmosphere();
            EventTransfer.apply(craft, event.rates(), seconds);
            craft.time += seconds;
            var fraction = seconds / event.seconds();
            var acceleration = (event.endSpeed() - event.initialSpeed()) / event.seconds();
            var travelled = fraction >= 1 ? event.end() : event.start() + event.initialSpeed() * seconds + .5 * acceleration * seconds * seconds;
            var velocity = fraction >= 1 ? event.endSpeed() : Math.max(0, event.initialSpeed() + acceleration * seconds);
            var position = route.curve().atDistance(travelled, velocity);
            craft.x = position.x() + target.velocityX() * (craft.time - startTime);
            craft.y = position.y() + target.velocityY() * (craft.time - startTime);
            craft.velocityX = position.vx() + target.velocityX();
            craft.velocityY = position.vy() + target.velocityY();
            var heading = route.curve().atDistance(travelled, 1);
            craft.headingX = heading.vx();
            craft.headingY = heading.vy();
            var phase = event.direction() > 0 ? PathPhase.ACCELERATE : event.direction() < 0 ? PathPhase.BRAKE : PathPhase.COAST;
            samples.add(craft.createSample(phase, action.targetId(), action.id(), event.firing()).withResourceRates(event.rates()));
            destination = targetPoint(target.xAt(craft.time), target.yAt(craft.time), SurveyRules.navigationRadius(target), action.orbit(), destinationAngle);
            if (seconds < event.seconds()) {
                appendSamples(craft, samples);
                var stopped = navigationAbort(action, craft, destination, transferPlan, context);
                if (stopped != null) {
                    if (action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == SpaceSimulation.OrbitBand.SURFACE)
                        craft.lastEarthSurfaceAction = action;
                    context.navigationAborts.add(new NavigationAbortMoment(action.id(), craft.branchId,
                            stopped.addon.id(), stopped.actualValue, craft.time, craft.x, craft.y, destination.x, destination.y));
                    return true;
                }
            }
            while (RocketFlightPathCalculator.finishStage(action, craft, context)) {
                samples.add(craft.createSample(PathPhase.COAST, action.targetId(), action.id(), Set.of()));
            }
        }
        craft.atmosphere = false;
        destination = targetPoint(target.xAt(craft.time), target.yAt(craft.time), SurveyRules.navigationRadius(target), action.orbit(), destinationAngle);
        if (completeArrival(action, craft, target, destination, targetVelocityX, targetVelocityY, samples, context, route))
            return true;
        return fail(craft, samples, TerminalState.NO_FEASIBLE_TRANSFER);
    }

    private static double eventDuration(SpaceSimulation.FlightPlanAction action, Craft craft,
                                        SpaceSimulation.SpaceObjectData target, Point destination,
                                        TransferPlan plan, EventTransfer.Event event) {

        if (action.addons().isEmpty() || !action.addons().getFirst().type().isNavigationCondition())
            return event.seconds();
        var addon = action.addons().getFirst();
        if (addon.type() == SpaceSimulation.ActionAddonType.TIME_BEFORE_ARRIVAL)
            return Math.min(event.seconds(), Math.max(0, plan.transfer.duration() - (craft.time - plan.startTime) - addon.value()));
        var low = 0d;
        var high = event.seconds();
        var acceleration = (event.endSpeed() - event.initialSpeed()) / event.seconds();
        var found = false;
        for (int iteration = 0; iteration < 40; iteration++) {
            var seconds = iteration == 0 ? high : (low + high) * .5;
            var travelled = event.start() + event.initialSpeed() * seconds + .5 * acceleration * seconds * seconds;
            var position = plan.transfer.curve().atDistance(travelled, 0);
            var x = position.x() + target.velocityX() * (craft.time + seconds - plan.startTime);
            var y = position.y() + target.velocityY() * (craft.time + seconds - plan.startTime);
            var distance = Math.hypot(destination.x + target.velocityX() * seconds - x, destination.y + target.velocityY() * seconds - y);
            var value = addon.type() == SpaceSimulation.ActionAddonType.DESIRED_UNCERTAINTY
                    ? AsteroidImpactRules.landingUncertaintyBlocks(distance) : distance;
            if (value <= addon.value()) {
                found = true;
                high = seconds;
            } else {
                if (!found) return event.seconds();
                low = seconds;
            }
        }
        return high;
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
        return actual <= addon.value() + 1e-7 ? new Abort(addon, actual) : null;
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
                        && b.resourceRates().equals(sample.resourceRates())
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

    }

    private record Abort(SpaceSimulation.ActionAddon addon, double actualValue) {

    }

    private record Point(double x, double y) {

    }
}
