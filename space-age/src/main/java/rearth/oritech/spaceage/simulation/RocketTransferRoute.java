package rearth.oritech.spaceage.simulation;

import java.util.List;
import java.util.Random;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.RocketFlightPathState.Context;

/** One geometric curve and one longitudinal engine schedule per card. No obstacle routing. */
final class RocketTransferRoute {
    record Burn(double seconds, double x, double y, boolean atmosphere) {
        boolean firing() { return x != 0; }
    }
    record Route(List<Burn> burns, double duration, FlightCurve curve, double efficiency,
                 double departureGravity, double arrivalGravity) { }

    static boolean hasSurface(SpaceSimulation.SpaceObjectData object) {
        return object.radius() > 0 && (object.type() == SpaceObjects.ObjectType.EARTH
                || object.type() == SpaceObjects.ObjectType.MARS || object.type() == SpaceObjects.ObjectType.SUN);
    }

    static Route solve(Craft craft, Context context, SpaceSimulation.FlightPlanAction action,
                       SpaceSimulation.SpaceObjectData target, double endX, double endY) {
        var dx = endX - craft.x; var dy = endY - craft.y;
        var distance = Math.max(1e-6, Math.hypot(dx, dy));
        var ex = dx / distance; var ey = dy / distance;
        var vx = craft.velocityX - target.velocityX(); var vy = craft.velocityY - target.velocityY();
        var speed = Math.hypot(vx, vy);
        var sx = speed > .001 ? vx / speed : ex;
        var sy = speed > .001 ? vy / speed : ey;
        var departure = context.objects.values().stream().filter(RocketTransferRoute::hasSurface)
                .filter(o -> Math.abs(Math.hypot(craft.x - o.x(), craft.y - o.y()) - o.radius()) < .1)
                .findFirst().orElse(null);
        if (departure != null && speed < .001) {
            sx = (craft.x - departure.x()) / departure.radius();
            sy = (craft.y - departure.y()) / departure.radius();
        } else if (speed < .001 && Math.hypot(craft.headingX, craft.headingY) > .001) {
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
        // A collinear turnaround needs an interior bow: moving an endpoint handle would violate
        // its radial/inherited direction. The extra term vanishes at both ends, including its derivative.
        var collinear = Math.abs(sx * dy - sy * dx) < distance * .01
                && Math.abs(ex * dy - ey * dx) < distance * .01;
        var turnsBack = sx * dx + sy * dy < 0 || ex * dx + ey * dy < 0;
        var bow = collinear && turnsBack ? distance * SpaceBalance.REVERSAL_STEERING_RATIO : 0;
        var curve = new FlightCurve(craft.x, craft.y, craft.x + sx * startHandle, craft.y + sy * startHandle,
                endX - ex * handle, endY - ey * handle, endX, endY, -dy / distance * bow, dx / distance * bow);
        var arrival = action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM ? action.targetVelocity() : 0;
        // Approximate steering as a small reduction in longitudinal acceleration while engines fire.
        // Coasting along the curve remains an intentional gameplay approximation.
        var efficiency = 1 / (1 + curve.turnAngle() * SpaceBalance.CURVE_STEERING_COST);
        var cap = action.maxSpeed() == 0 ? Double.POSITIVE_INFINITY : action.maxSpeed();
        var freeArrival = action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.MAXIMUM;
        var departureGravity = gravityAlong(context, curve, 0, craft.time);
        var arrivalGravity = gravityAlong(context, curve, curve.length(), craft.time);
        // With no meaningful gravity, retain the mature solver's ballistic and over-cap handling.
        if (craft.burnProfile(context).intervals().isEmpty()
                || Math.max(Math.abs(departureGravity), Math.abs(arrivalGravity)) < .01
                || speed > cap) {
            var profile = new RocketBurnProfile(craft.burnProfile(context).intervals().stream()
                    .map(interval -> new RocketBurnProfile.Interval(
                            interval.acceleration() * efficiency, interval.seconds())).toList());
            var transfer = FullPowerTransfer.solve(curve.length(), 0, speed, 0, arrival, 0,
                    freeArrival, profile, cap, 1_200_000);
            if (transfer == null) return null;
            return new Route(List.of(
                    new Burn(transfer.firstSeconds(), transfer.firstDirectionX(), 0, craft.atmosphere),
                    new Burn(transfer.coastSeconds(), 0, 0, craft.atmosphere),
                    new Burn(transfer.lastSeconds(), transfer.lastDirectionX(), 0, craft.atmosphere)),
                    transfer.duration(), curve, efficiency, 0, 0);
        }
        var gravityTransfer = CurveTransfer.solve(curve.length(), speed, arrival, freeArrival, cap,
                craft.burnProfile(context), efficiency, departureGravity, arrivalGravity);
        if (gravityTransfer == null) return null;
        return new Route(List.of(
                new Burn(gravityTransfer.firstSeconds(), 1, 0, craft.atmosphere),
                new Burn(gravityTransfer.coastSeconds(), 0, 0, craft.atmosphere),
                new Burn(gravityTransfer.lastSeconds(), -1, 0, craft.atmosphere)),
                gravityTransfer.firstSeconds() + gravityTransfer.coastSeconds()
                        + gravityTransfer.lastSeconds(), curve, efficiency,
                departureGravity, arrivalGravity);
    }

    /** Positive gravity accelerates movement along the curve; negative gravity resists it. */
    static double gravityAlong(Context context, FlightCurve curve, double distance, double time) {
        var position = curve.atDistance(distance, 1);
        var gravity = 0d;
        for (var body : context.objects.values()) {
            if (body.radius() <= 0 || body.surfaceGravity() <= 0) continue;
            var bodyX = body.xAt(time); var bodyY = body.yAt(time);
            var dx = bodyX - position.x(); var dy = bodyY - position.y();
            var actualDistance = Math.hypot(dx, dy);
            if (actualDistance < 1e-6) continue;
            var boundedDistance = Math.max(body.radius(), actualDistance);
            var acceleration = body.surfaceGravity() * body.radius() * body.radius()
                    / (boundedDistance * boundedDistance);
            gravity += acceleration * (dx * position.vx() + dy * position.vy()) / actualDistance;
        }
        return gravity;
    }
}
