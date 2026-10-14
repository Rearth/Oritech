package rearth.oritech.spaceage.client;

import rearth.oritech.spaceage.simulation.FlightMotion;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.BoosterEvent;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Projects motion with optional visual surface endpoints; simulation samples remain unchanged.
 */
final class RocketMapPaths {

    private final Map<UUID, RocketFlightPathCalculator.CraftPath> paths = new HashMap<>();
    private final Map<UUID, List<LegTransform>> transforms = new HashMap<>();
    private final UnaryOperator<Point> surfacePosition;

    RocketMapPaths(SpaceSimulation.FlightPlan plan, RocketFlightPathCalculator.FlightPath flight) {

        this(plan, flight, UnaryOperator.identity());
    }

    RocketMapPaths(SpaceSimulation.FlightPlan plan, RocketFlightPathCalculator.FlightPath flight,
                   UnaryOperator<Point> surfacePosition) {

        this.surfacePosition = surfacePosition;
        flight.paths().forEach(path -> paths.put(path.branchId(), path));
        var departures = new HashMap<UUID, BoosterEvent>();
        flight.boosterEvents().forEach(event -> departures.put(event.childBranchId(), event));
        flight.paths().forEach(path -> buildTransforms(path, departures));
    }

    private void buildTransforms(RocketFlightPathCalculator.CraftPath path, Map<UUID, BoosterEvent> departures) {

        if (transforms.containsKey(path.branchId())) return;
        if (path.samples().isEmpty()) {
            transforms.put(path.branchId(), List.of());
            return;
        }
        var first = path.samples().getFirst();
        var initial = surfacePosition.apply(new Point(first.x(), first.y()));
        var departure = departures.get(path.branchId());
        if (departure != null && paths.containsKey(departure.branchId())) {
            buildTransforms(paths.get(departure.branchId()), departures);
            initial = renderedWorldPosition(departure.branchId(), departure.timeSeconds(), departure.x(), departure.y());
        }
        transforms.put(path.branchId(), legTransforms(path, initial));
    }

    private List<LegTransform> legTransforms(RocketFlightPathCalculator.CraftPath path, Point mappedInitial) {

        var result = new ArrayList<LegTransform>();
        var samples = path.samples();
        var initial = new Point(samples.getFirst().x(), samples.getFirst().y());
        var initialTime = samples.getFirst().timeSeconds();
        result.add(LegTransform.between(initial, initial, mappedInitial, mappedInitial, initialTime, initialTime));
        for (var first = 1; first < samples.size();) {
            var last = first;
            // Phase, staging and time samples belong to one curve, not independent surface departures.
            while (last + 1 < samples.size() && samples.get(last + 1).actionId().equals(samples.get(first).actionId())) last++;
            var start = new Point(samples.get(first - 1).x(), samples.get(first - 1).y());
            var end = new Point(samples.get(last).x(), samples.get(last).y());
            var startTime = samples.get(first - 1).timeSeconds();
            var endTime = samples.get(last).timeSeconds();
            var mappedStart = result.getLast().apply(start, startTime);
            var mappedEnd = start.equals(end) ? mappedStart : surfacePosition.apply(end);
            var transform = LegTransform.between(start, end, mappedStart, mappedEnd, startTime, endTime);
            for (var index = first; index <= last; index++) result.add(transform);
            first = last + 1;
        }
        return List.copyOf(result);
    }

    private static void subdivide(List<RenderedPathSegment> result, FlightMotion motion, Point a, Point b, Point c, Point d,
                                  double from, double to, double x, double y, double width, double height, int depth) {

        if (Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)) < x - 6
                || Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)) > x + width + 6
                || Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)) < y - 6
                || Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)) > y + height + 6) return;
        var first = a.lerp(d, 1.0 / 3);
        var second = a.lerp(d, 2.0 / 3);
        var error = Math.max(Math.hypot(b.x - first.x, b.y - first.y), Math.hypot(c.x - second.x, c.y - second.y));
        if (error <= .75 || depth >= 18) {
            result.add(new RenderedPathSegment(a, d, motion.start(), motion.end(), from, to));
            return;
        }
        var ab = a.lerp(b, .5);
        var bc = b.lerp(c, .5);
        var cd = c.lerp(d, .5);
        var abc = ab.lerp(bc, .5);
        var bcd = bc.lerp(cd, .5);
        var middle = abc.lerp(bcd, .5);
        var time = (from + to) * .5;
        subdivide(result, motion, a, ab, abc, middle, from, time, x, y, width, height, depth + 1);
        subdivide(result, motion, middle, bcd, cd, d, time, to, x, y, width, height, depth + 1);
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
            var transform = transforms.get(path.branchId()).get(i);
            var startTime = motion.start().timeSeconds();
            var endTime = motion.end().timeSeconds();
            var start = transform.apply(new Point(motion.start().x(), motion.start().y()), startTime);
            var startVelocity = transform.velocity(new Point(motion.start().velocityX(), motion.start().velocityY()), startTime);
            var a = projection.apply(start);
            var b = projection.apply(new Point(start.x() + startVelocity.x() * motion.duration() / 3,
                    start.y() + startVelocity.y() * motion.duration() / 3));
            var end = motion.at(1);
            var finish = transform.apply(new Point(end.x(), end.y()), endTime);
            var endVelocity = transform.velocity(new Point(end.vx(), end.vy()), endTime);
            var c = projection.apply(new Point(finish.x() - endVelocity.x() * motion.duration() / 3,
                    finish.y() - endVelocity.y() * motion.duration() / 3));
            var d = projection.apply(finish);
            subdivide(result, motion, a, b, c, d, 0, 1, x, y, width, height, 0);
        }
        return result;
    }

    List<RenderedAsteroidSegment> renderedAsteroidPathSegments(RocketFlightPathCalculator.AsteroidPath path) {

        var result = new ArrayList<RenderedAsteroidSegment>();
        if (path.samples().isEmpty()) return result;
        var start = new Point(path.samples().getFirst().x(), path.samples().getFirst().y());
        var end = new Point(path.samples().getLast().x(), path.samples().getLast().y());
        var transform = LegTransform.between(start, end, surfacePosition.apply(start), surfacePosition.apply(end),
                path.samples().getFirst().timeSeconds(), path.samples().getLast().timeSeconds());
        for (int i = 1; i < path.samples().size(); i++) {
            var a = path.samples().get(i - 1);
            var b = path.samples().get(i);
            result.add(new RenderedAsteroidSegment(transform.apply(new Point(a.x(), a.y()), a.timeSeconds()),
                    transform.apply(new Point(b.x(), b.y()), b.timeSeconds())));
        }
        return result;
    }

    Point renderedWorldPosition(UUID branch, double time, double fallbackX, double fallbackY) {

        var path = paths.get(branch);
        if (path == null || path.samples().isEmpty()) return surfacePosition.apply(new Point(fallbackX, fallbackY));
        var state = FlightMotion.sample(path.samples(), time);
        var index = FlightMotion.endIndex(path.samples(), time);
        return transforms.get(branch).get(index).apply(new Point(state.x(), state.y()), time);
    }

    /** A cubic endpoint offset preserves waypoint velocities without rotating or scaling overshoots. */
    private record LegTransform(Point startOffset, Point endOffset, double startTime, double endTime) {

        static LegTransform between(Point start, Point end, Point mappedStart, Point mappedEnd, double startTime, double endTime) {

            return new LegTransform(new Point(mappedStart.x() - start.x(), mappedStart.y() - start.y()),
                    new Point(mappedEnd.x() - end.x(), mappedEnd.y() - end.y()), startTime, endTime);
        }

        Point apply(Point point, double time) {

            var progress = endTime > startTime ? Math.clamp((time - startTime) / (endTime - startTime), 0, 1) : 0;
            var offset = startOffset.lerp(endOffset, progress * progress * (3 - 2 * progress));
            return new Point(point.x() + offset.x(), point.y() + offset.y());
        }

        Point velocity(Point velocity, double time) {

            if (endTime <= startTime) return velocity;
            var progress = Math.clamp((time - startTime) / (endTime - startTime), 0, 1);
            var rate = 6 * progress * (1 - progress) / (endTime - startTime);
            return new Point(velocity.x() + (endOffset.x() - startOffset.x()) * rate,
                    velocity.y() + (endOffset.y() - startOffset.y()) * rate);
        }
    }

    record RenderedPathSegment(Point from, Point to, RocketFlightPathCalculator.PathSample firstSample,
                               RocketFlightPathCalculator.PathSample secondSample, double sampleProgressFrom,
                               double sampleProgressTo) {

    }

    record RenderedAsteroidSegment(Point from, Point to) {

    }

    record Point(double x, double y) {

        Point lerp(Point other, double t) {

            return new Point(x + (other.x - x) * t, y + (other.y - y) * t);
        }
    }
}
