package rearth.oritech.spaceage.client;

import rearth.oritech.spaceage.simulation.FlightMotion;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Projects the actual motion. Subdivision changes drawing precision, never the trajectory. */
final class RocketMapPaths {
    private final Map<UUID, RocketFlightPathCalculator.CraftPath> paths = new HashMap<>();

    RocketMapPaths(SpaceSimulation.FlightPlan plan, RocketFlightPathCalculator.FlightPath flight) {
        flight.paths().forEach(path -> paths.put(path.branchId(), path));
    }

    List<RenderedPathSegment> renderedPathSegments(RocketFlightPathCalculator.CraftPath path) {
        return renderedPathSegments(path, UnaryOperator.identity(), -Double.MAX_VALUE / 4, -Double.MAX_VALUE / 4,
                Double.MAX_VALUE / 2, Double.MAX_VALUE / 2);
    }

    List<RenderedPathSegment> renderedPathSegments(RocketFlightPathCalculator.CraftPath path,
            UnaryOperator<Point> projection, double x, double y, double width, double height) {
        var result = new ArrayList<RenderedPathSegment>();
        var samples = path.samples();
        for (int i = 1; i < samples.size(); i++) {
            var motion = new FlightMotion(samples.get(i - 1), samples.get(i));
            if (motion.duration() <= 0) continue;
            var a = projection.apply(new Point(motion.start().x(), motion.start().y()));
            var b = projection.apply(new Point(motion.start().x() + motion.start().velocityX() * motion.duration() / 3,
                    motion.start().y() + motion.start().velocityY() * motion.duration() / 3));
            var end = motion.at(1);
            var c = projection.apply(new Point(end.x() - end.vx() * motion.duration() / 3,
                    end.y() - end.vy() * motion.duration() / 3));
            var d = projection.apply(new Point(end.x(), end.y()));
            subdivide(result, motion, a, b, c, d, 0, 1, x, y, width, height, 0);
        }
        return result;
    }

    private static void subdivide(List<RenderedPathSegment> result, FlightMotion motion, Point a, Point b, Point c, Point d,
            double from, double to, double x, double y, double width, double height, int depth) {
        if (Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)) < x - 6
                || Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)) > x + width + 6
                || Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)) < y - 6
                || Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)) > y + height + 6) return;
        var first = a.lerp(d, 1.0 / 3); var second = a.lerp(d, 2.0 / 3);
        double error = Math.max(Math.hypot(b.x - first.x, b.y - first.y), Math.hypot(c.x - second.x, c.y - second.y));
        if (error <= .75 || depth >= 18) {
            result.add(new RenderedPathSegment(a, d, motion.start(), motion.end(), from, to));
            return;
        }
        var ab = a.lerp(b, .5); var bc = b.lerp(c, .5); var cd = c.lerp(d, .5);
        var abc = ab.lerp(bc, .5); var bcd = bc.lerp(cd, .5); var middle = abc.lerp(bcd, .5);
        var time = (from + to) * .5;
        subdivide(result, motion, a, ab, abc, middle, from, time, x, y, width, height, depth + 1);
        subdivide(result, motion, middle, bcd, cd, d, time, to, x, y, width, height, depth + 1);
    }

    List<RenderedAsteroidSegment> renderedAsteroidPathSegments(RocketFlightPathCalculator.AsteroidPath path) {
        var result = new ArrayList<RenderedAsteroidSegment>();
        for (int i = 1; i < path.samples().size(); i++) {
            var a = path.samples().get(i - 1); var b = path.samples().get(i);
            result.add(new RenderedAsteroidSegment(new Point(a.x(), a.y()), new Point(b.x(), b.y())));
        }
        return result;
    }

    Point renderedWorldPosition(UUID branch, double time, double fallbackX, double fallbackY) {
        var path = paths.get(branch);
        if (path == null || path.samples().isEmpty()) return new Point(fallbackX, fallbackY);
        var state = FlightMotion.sample(path.samples(), time);
        return new Point(state.x(), state.y());
    }

    record RenderedPathSegment(Point from, Point to, RocketFlightPathCalculator.PathSample firstSample,
            RocketFlightPathCalculator.PathSample secondSample, double sampleProgressFrom, double sampleProgressTo) { }
    record RenderedAsteroidSegment(Point from, Point to) { }
    record Point(double x, double y) {
        Point lerp(Point other, double t) { return new Point(x + (other.x - x) * t, y + (other.y - y) * t); }
    }
}
