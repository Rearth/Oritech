package rearth.oritech.spaceage.simulation.flight;

import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceGravity;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.power.SolarPower;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Spatial events, independent of flight duration. Gravity work is exact between cell endpoints. */
final class FlightEnvironment {

    private FlightEnvironment() {
    }

    static List<Cell> along(FlightCurve curve, Context context, double time) {

        return along(curve, context, time, false);
    }

    static List<Cell> along(FlightCurve curve, Context context, double time, boolean solar) {

        var boundaries = new TreeSet<Double>();
        boundaries.add(0d);
        boundaries.add(curve.length());
        for (var body : context.objects.values()) {
            if (body.radius() <= 0 || body.surfaceGravity() <= 0) continue;
            addCrossings(boundaries, curve, body.xAt(time), body.yAt(time), body.radius());
            if (Double.isFinite(SpaceGravity.range(body)))
                addCrossings(boundaries, curve, body.xAt(time), body.yAt(time), SpaceGravity.range(body));
            if (body.type() == SpaceObjects.ObjectType.EARTH)
                addCrossings(boundaries, curve, body.xAt(time), body.yAt(time), body.radius() + SpaceBalance.ATMOSPHERE_HEIGHT);
        }
        var result = new ArrayList<Cell>();
        var start = 0d;
        for (var end : boundaries) {
            if (end > start + 1e-7) subdivide(result, curve, context, time, start, end, 0, solar);
            start = end;
        }
        return List.copyOf(result);
    }

    private static void addCrossings(TreeSet<Double> boundaries, FlightCurve curve, double x, double y, double radius) {

        var previous = distance(curve, 0, x, y) - radius;
        for (int i = 1; i <= 256; i++) {
            var end = curve.length() * i / 256;
            var value = distance(curve, end, x, y) - radius;
            var start = curve.length() * (i - 1) / 256;
            if ((previous < 0) != (value < 0)) {
                boundaries.add(crossing(curve, x, y, radius, start, end, previous < 0));
            } else if (previous >= 0 && value >= 0) {
                // A small field can lie entirely between two geometry samples.
                var from = curve.atDistance(start, 0);
                var to = curve.atDistance(end, 0);
                var dx = to.x() - from.x();
                var dy = to.y() - from.y();
                var fraction = Math.clamp(((x - from.x()) * dx + (y - from.y()) * dy) / Math.max(1e-12, dx * dx + dy * dy), 0, 1);
                var nearest = start + (end - start) * fraction;
                if (distance(curve, nearest, x, y) < radius) {
                    boundaries.add(crossing(curve, x, y, radius, start, nearest, false));
                    boundaries.add(crossing(curve, x, y, radius, nearest, end, true));
                }
            }
            previous = value;
        }
    }

    private static double crossing(FlightCurve curve, double x, double y, double radius, double low, double high, boolean inside) {

        for (int iteration = 0; iteration < 40; iteration++) {
            var middle = (low + high) * .5;
            if ((distance(curve, middle, x, y) < radius) == inside) low = middle;
            else high = middle;
        }
        return (low + high) * .5;
    }

    private static double distance(FlightCurve curve, double s, double x, double y) {

        var point = curve.atDistance(s, 0);
        return Math.hypot(point.x() - x, point.y() - y);
    }

    private static void subdivide(List<Cell> cells, FlightCurve curve, Context context, double time,
                                  double start, double end, int depth, boolean solar) {

        var middle = (start + end) * .5;
        var workStart = potential(context, curve, start, time);
        var workMiddle = potential(context, curve, middle, time);
        var workEnd = potential(context, curve, end, time);
        var firstGravity = (workMiddle - workStart) / (middle - start);
        var lastGravity = (workEnd - workMiddle) / (end - middle);
        var a = curve.atDistance(start, 1);
        var b = curve.atDistance(end, 1);
        var solarStart = SolarPower.multiplier(a.x(), a.y());
        var solarEnd = SolarPower.multiplier(b.x(), b.y());
        var varying = Math.abs(firstGravity - lastGravity) > .05 * Math.max(.01, Math.max(Math.abs(firstGravity), Math.abs(lastGravity)))
                || solar && Math.abs(solarStart - solarEnd) > .1 * Math.max(solarStart, solarEnd)
                || a.vx() * b.vx() + a.vy() * b.vy() < .98;
        if (depth < 12 && end - start > 1 && varying) {
            subdivide(cells, curve, context, time, start, middle, depth + 1, solar);
            subdivide(cells, curve, context, time, middle, end, depth + 1, solar);
            return;
        }
        var point = curve.atDistance(middle, 0);
        var atmosphere = atmosphere(context, point.x(), point.y(), time);
        cells.add(new Cell(start, end, (workEnd - workStart) / (end - start), atmosphere,
                SolarPower.multiplier(point.x(), point.y())));
    }

    static boolean atmosphere(Context context, double x, double y, double time) {

        return context.objects.values().stream().anyMatch(body -> body.type() == SpaceObjects.ObjectType.EARTH
                && body.radius() > 0 && Math.hypot(x - body.xAt(time), y - body.yAt(time)) < body.radius() + SpaceBalance.ATMOSPHERE_HEIGHT);
    }

    static double potential(Context context, FlightCurve curve, double distance, double time) {

        var point = curve.atDistance(distance, 0);
        var result = 0d;
        for (var body : context.objects.values()) {
            if (body.surfaceGravity() <= 0) continue;
            result += SpaceGravity.potential(body, Math.hypot(point.x() - body.xAt(time), point.y() - body.yAt(time)));
        }
        return result;
    }

    record Cell(double start, double end, double gravity, boolean atmosphere, double solar) {
    }
}
