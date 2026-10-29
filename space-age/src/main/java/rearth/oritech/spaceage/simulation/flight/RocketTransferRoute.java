package rearth.oritech.spaceage.simulation.flight;

import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceGravity;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.NavigationFailure;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Craft;

import java.util.List;
import java.util.Random;

/**
 * One geometric curve and one spatial event schedule per card. No obstacle routing.
 */
public final class RocketTransferRoute {

    private RocketTransferRoute() {
    }

    public static boolean hasSurface(SpaceSimulation.SpaceObjectData object) {

        return object.radius() > 0 && (object.type() == SpaceObjects.ObjectType.EARTH
                || object.type() == SpaceObjects.ObjectType.MARS || object.type() == SpaceObjects.ObjectType.SUN);
    }

    static Route solve(Craft craft, Context context, SpaceSimulation.FlightPlanAction action,
                       SpaceSimulation.SpaceObjectData target, double endX, double endY) {

        return solve(craft, context, action, target, endX, endY, craft.burnProfile(context));
    }

    private static double steeringEfficiency(FlightCurve curve) {

        return 1 / (1 + curve.turnAngle() * SpaceBalance.CURVE_STEERING_COST);
    }

    private static Route solve(Craft craft, Context context, SpaceSimulation.FlightPlanAction action,
                               SpaceSimulation.SpaceObjectData target, double endX, double endY, RocketBurnProfile burnProfile) {

        craft.navigationFailure = null;
        var dx = endX - craft.x;
        var dy = endY - craft.y;
        var distance = Math.max(1e-6, Math.hypot(dx, dy));
        var ex = dx / distance;
        var ey = dy / distance;
        var vx = craft.velocityX - target.velocityX();
        var vy = craft.velocityY - target.velocityY();
        var speed = Math.hypot(vx, vy);
        var sx = speed > .001 ? vx / speed : ex;
        var sy = speed > .001 ? vy / speed : ey;
        var departure = context.objects.values().stream().filter(RocketTransferRoute::hasSurface)
                .filter(o -> Math.abs(Math.hypot(craft.x - o.x(), craft.y - o.y()) - o.radius()) < .1)
                .findFirst().orElse(null);
        if (departure != null && speed < .001) {
            sx = (craft.x - departure.x()) / departure.radius();
            sy = (craft.y - departure.y()) / departure.radius();
        } else if (target.type() != SpaceObjects.ObjectType.CRAFT && speed < .001 && Math.hypot(craft.headingX, craft.headingY) > .001) {
            // Stable per-card jitter bends away from the previous arrival rather than retracing it.
            var random = new Random(action.id().getMostSignificantBits() ^ action.id().getLeastSignificantBits());
            var cross = ex * craft.headingY - ey * craft.headingX;
            var sign = Math.abs(cross) < 1e-6 ? (random.nextBoolean() ? 1 : -1) : -Math.signum(cross);
            var angle = sign * (SpaceBalance.STOPPED_DEPARTURE_ANGLE_MIN
                    + (SpaceBalance.STOPPED_DEPARTURE_ANGLE_MAX - SpaceBalance.STOPPED_DEPARTURE_ANGLE_MIN)
                    * random.nextDouble());
            sx = ex * Math.cos(angle) - ey * Math.sin(angle);
            sy = ex * Math.sin(angle) + ey * Math.cos(angle);
        }
        if (action.orbit() == SpaceSimulation.OrbitBand.SURFACE && hasSurface(target)) {
            ex = (target.xAt(craft.time) - endX) / target.radius();
            ey = (target.yAt(craft.time) - endY) / target.radius();
        }
        var atmosphere = FlightEnvironment.atmosphere(context, craft.x, craft.y, craft.time);
        var acceleration = craft.activeSegments(context).stream().map(craft.segments::get)
                .mapToDouble(segment -> segment.thrust(atmosphere)).sum() / Math.max(1, craft.mass());
        var handle = distance / 3;
        var startHandle = sx * dx + sy * dy < distance * .5
                ? Math.max(handle, speed * speed / (2 * Math.max(.001, acceleration))) : handle;
        var minimumHandle = startHandle;
        var extended = false;
        var arrival = action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM ? action.targetVelocity() : 0;
        var cap = action.maxSpeed() == 0 ? Double.POSITIVE_INFINITY : action.maxSpeed();
        var freeArrival = action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.MAXIMUM;
        var parachuteCount = ParachuteLanding.eligible(action)
                ? craft.segments.values().stream().mapToInt(segment -> segment.parachutes).sum() : 0;
        var brakingCredit = ParachuteLanding.deltaVReduction(craft.mass(), parachuteCount);
        FlightCurve curve = null;
        var efficiency = 1d;
        var gravity = 0d;
        for (int attempt = 0; attempt < 16; attempt++) {
            curve = FlightCurve.between(craft.x, craft.y, endX, endY, sx, sy, ex, ey, startHandle);
            efficiency = steeringEfficiency(curve);
            gravity = gravityForLeg(context, curve, craft.time);
            var brakingArrival = freeArrival ? cap : arrival;
            if (speed <= brakingArrival || burnProfile.intervals().isEmpty()) break;
            var brakingDistance = CurveTransfer.brakingDistance(speed, brakingArrival, burnProfile,
                    efficiency, gravity, brakingCredit);
            if (!Double.isFinite(brakingDistance)) break;
            if (brakingDistance <= curve.length()) {
                if (extended) {
                    // Fit the extra approach to the stopping distance instead of leaving space
                    // that the peak-speed solver would fill with another acceleration burn.
                    var low = minimumHandle;
                    var high = startHandle;
                    for (int refinement = 0; refinement < 32; refinement++) {
                        var middle = (low + high) * .5;
                        var candidate = FlightCurve.between(craft.x, craft.y, endX, endY, sx, sy, ex, ey, middle);
                        var candidateEfficiency = steeringEfficiency(candidate);
                        var needed = CurveTransfer.brakingDistance(speed, brakingArrival, burnProfile,
                                candidateEfficiency, gravityForLeg(context, candidate, craft.time), brakingCredit);
                        if (candidate.length() >= needed * 1.000001) {
                            high = middle;
                            curve = candidate;
                            efficiency = candidateEfficiency;
                        } else low = middle;
                    }
                }
                break;
            }
            extended = true;
            startHandle = Math.max(startHandle * 2, brakingDistance * 2);
        }
        if (extended) cap = Math.min(cap, speed);
        gravity = gravityForLeg(context, curve, craft.time);
        // Averaged gravity must not let underpowered engines lift off or touch down safely.
        var localDeparture = gravityAlong(context, curve, 0, craft.time);
        var localArrival = gravityAlong(context, curve, curve.length(), craft.time);
        var curveEfficiency = efficiency;
        if (departure != null && speed < .001 && acceleration * efficiency + localDeparture <= 0) {
            craft.navigationFailure = new NavigationFailure("departure_thrust", acceleration * efficiency, -localDeparture);
            return null;
        }
        var events = new EventTransfer(craft, context, curve, speed, arrival, efficiency, brakingCredit).solve(cap, freeArrival);
        // The vacuum stopping estimate can miss an atmospheric crossing on a long reversal.
        // Give the actual event simulation room to brake, keeping inherited speed as the cruise ceiling.
        for (int approach = 0; events == null && speed > arrival && !freeArrival && approach < 4; approach++) {
            startHandle *= 1.1;
            curve = FlightCurve.between(craft.x, craft.y, endX, endY, sx, sy, ex, ey, startHandle);
            efficiency = steeringEfficiency(curve);
            events = new EventTransfer(craft, context, curve, speed, arrival, efficiency, brakingCredit).solve(Math.min(cap, speed), false);
        }
        if (events == null) {
            var strongest = burnProfile.intervals().stream().mapToDouble(interval -> interval.acceleration() * curveEfficiency).max().orElse(0);
            if (action.orbit() == SpaceSimulation.OrbitBand.SURFACE && hasSurface(target) && strongest <= localArrival) {
                craft.navigationFailure = new NavigationFailure("braking_thrust", strongest, localArrival);
                return null;
            }
            var climb = Math.sqrt(Math.max(0, -2 * gravity * curve.length()));
            if (gravity < 0 && Double.isFinite(cap) && cap < climb) {
                craft.navigationFailure = new NavigationFailure("climb_speed", cap, climb);
                return null;
            }
            if (!freeArrival && arrival > cap)
                craft.navigationFailure = new NavigationFailure("arrival_cap", arrival, cap);
            else if (speed > arrival && !freeArrival && burnProfile.deltaV() * efficiency < speed - arrival)
                craft.navigationFailure = new NavigationFailure("braking_budget", burnProfile.deltaV() * efficiency, speed - arrival);
            if (craft.navigationFailure == null && craft.segments.values().stream().anyMatch(segment -> segment.hardware.ion() > 0 && segment.hardware.antennas() > 0))
                craft.navigationFailure = new NavigationFailure("radio_budget",
                        craft.segments.values().stream().mapToDouble(segment -> segment.hardware.antennas() * SpaceBalance.ANTENNA_RF * 20).sum()
                                * 2 * Math.sqrt(curve.length() / Math.max(.001, strongest)),
                        craft.segments.values().stream().mapToDouble(segment -> segment.rf()).sum());
            return null;
        }
        var factor = events.stream().filter(event -> event.direction() < 0).mapToDouble(EventTransfer.Event::fuelFactor).findFirst().orElse(1);
        craft.navigationFailure = null;
        return new Route(events, EventTransfer.duration(events), curve, efficiency,
                new ParachuteLanding.Result(parachuteCount, craft.mass(), brakingCredit, factor));
    }

    /**
     * Average gravitational work per metre. A long loop cannot accumulate surface gravity forever.
     * Applied over the whole leg, it gives the exact energy change between the endpoints, but not the speed along
     * the way; this is guidance, not an orbital integrator.
     */
    static double gravityForLeg(Context context, FlightCurve curve, double time) {

        var start = curve.atDistance(0, 0);
        var end = curve.atDistance(curve.length(), 0);
        var work = 0d;
        for (var body : context.objects.values()) {
            if (body.radius() <= 0 || body.surfaceGravity() <= 0) continue;
            var from = Math.max(body.radius(), Math.hypot(start.x() - body.xAt(time), start.y() - body.yAt(time)));
            var to = Math.max(body.radius(), Math.hypot(end.x() - body.xAt(time), end.y() - body.yAt(time)));
            work += SpaceGravity.potential(body, to) - SpaceGravity.potential(body, from);
        }
        return work / curve.length();
    }

    /**
     * Positive gravity accelerates movement along the curve; negative gravity resists it.
     */
    static double gravityAlong(Context context, FlightCurve curve, double distance, double time) {

        var position = curve.atDistance(distance, 1);
        var gravity = 0d;
        for (var body : context.objects.values()) {
            if (body.radius() <= 0 || body.surfaceGravity() <= 0) continue;
            var bodyX = body.xAt(time);
            var bodyY = body.yAt(time);
            var dx = bodyX - position.x();
            var dy = bodyY - position.y();
            var actualDistance = Math.hypot(dx, dy);
            if (actualDistance < 1e-6) continue;
            var acceleration = SpaceGravity.acceleration(body, actualDistance);
            gravity += acceleration * (dx * position.vx() + dy * position.vy()) / actualDistance;
        }
        return gravity;
    }

    record Route(List<EventTransfer.Event> events, double duration, FlightCurve curve, double efficiency,
                 ParachuteLanding.Result parachutes) {
    }
}
