package rearth.oritech.spaceage.simulation.flight;

import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceGravity;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.NavigationFailure;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.service.StationServiceRules;

import java.util.List;
import java.util.Random;

/**
 * One geometric curve and one longitudinal engine schedule per card. No obstacle routing.
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

        var profile = craft.navigationBurnProfile(context, List.of());
        var route = solve(craft, context, action, target, endX, endY, profile);
        if (route == null || craft.segments.values().stream().noneMatch(segment -> segment.hardware.antennas() > 0 && segment.hardware.ion() > 0))
            return route;
        // Radio draw changes burnout times. Replay burn/coast boundaries on a copy until
        // the timing solver and the resource schedule describe the same flight.
        for (int iteration = 0; iteration < 64; iteration++) {
            profile = craft.navigationBurnProfile(context, route.burns());
            var next = solve(craft, context, action, target, endX, endY, profile);
            if (next == null) return radioBudgetFailure(craft, route.duration());
            var settled = true;
            for (int phase = 0; phase < route.burns().size(); phase++)
                settled &= Math.abs(route.burns().get(phase).seconds() - next.burns().get(phase).seconds()) < 1e-9;
            route = next;
            if (settled) return route;
        }
        return radioBudgetFailure(craft, route.duration());
    }

    private static Route radioBudgetFailure(Craft craft, double duration) {

        var radioRF = craft.segments.values().stream()
                .mapToDouble(segment -> StationServiceRules.antennaRF(segment.hardware, duration * 20)).sum();
        craft.navigationFailure = new NavigationFailure("radio_budget", radioRF,
                craft.segments.values().stream().mapToDouble(segment -> segment.rf).sum());
        return null;
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
        var acceleration = craft.deltaVPerSecond(craft.activeSegments(context));
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
        var gravityTransfer = CurveTransfer.solve(curve.length(), speed, arrival, freeArrival, cap,
                burnProfile, efficiency, gravity, brakingCredit);
        if (gravityTransfer == null) {
            var strongest = burnProfile.intervals().stream().mapToDouble(interval -> interval.acceleration() * curveEfficiency).max().orElse(0);
            if (!freeArrival && gravity > 0 && strongest <= gravity)
                craft.navigationFailure = new NavigationFailure("braking_thrust", strongest, gravity);
            else if (gravity < 0 && strongest <= -gravity)
                craft.navigationFailure = new NavigationFailure("departure_thrust", strongest, -gravity);
            else if (gravity < 0 && climbSpeed(curve.length(), freeArrival ? 0 : arrival, gravity)
                    > Math.min(cap, speed + burnProfile.deltaV() * curveEfficiency))
                // Coasting slows down uphill; a speed cap or launch speed below this stalls before arriving.
                craft.navigationFailure = new NavigationFailure("climb_speed", Math.min(cap, speed + burnProfile.deltaV() * curveEfficiency),
                        climbSpeed(curve.length(), freeArrival ? 0 : arrival, gravity));
            else if (!freeArrival && arrival > cap)
                craft.navigationFailure = new NavigationFailure("arrival_cap", arrival, cap);
            else if (speed > arrival && !freeArrival && burnProfile.deltaV() * efficiency < speed - arrival)
                craft.navigationFailure = new NavigationFailure("braking_budget", burnProfile.deltaV() * efficiency, speed - arrival);
            return null;
        }
        if (gravityTransfer.lastSeconds() > 0 && action.orbit() == SpaceSimulation.OrbitBand.SURFACE
                && hasSurface(target) && burnProfile.intervals().stream()
                .noneMatch(interval -> interval.acceleration() * curveEfficiency > localArrival)) {
            craft.navigationFailure = new NavigationFailure("braking_thrust", burnProfile.intervals().stream()
                    .mapToDouble(interval -> interval.acceleration() * curveEfficiency).max().orElse(0), localArrival);
            return null;
        }
        return new Route(List.of(
                new Burn(gravityTransfer.firstSeconds(), gravityTransfer.firstDirection(),
                        gravityTransfer.firstDirection() < 0 ? gravityTransfer.brakingFuelFactor() : 1, craft.atmosphere),
                new Burn(gravityTransfer.coastSeconds(), 0, 1, craft.atmosphere),
                new Burn(gravityTransfer.lastSeconds(), -1, gravityTransfer.brakingFuelFactor(), craft.atmosphere)),
                gravityTransfer.duration(), curve, efficiency,
                gravity, new ParachuteLanding.Result(parachuteCount, craft.mass(), brakingCredit, gravityTransfer.brakingFuelFactor()));
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

    /** Speed needed to coast a whole uphill leg and still arrive with the given speed. */
    private static double climbSpeed(double length, double arrival, double gravity) {

        return Math.sqrt(arrival * arrival - 2 * gravity * length);
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

    record Burn(double seconds, double direction, double fuelFactor, boolean atmosphere) {

        boolean firing() {

            return direction != 0;
        }
    }

    record Route(List<Burn> burns, double duration, FlightCurve curve, double efficiency,
                 double gravity, ParachuteLanding.Result parachutes) {
    }
}
