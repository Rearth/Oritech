package rearth.oritech.spaceage.client;

import java.util.List;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.client.RocketMapPaths.Point;
import rearth.oritech.spaceage.client.RocketMapPaths.RenderedPathSegment;

/**
 * A decorative bow in world space. The origin and direction locate the transfer;
 * lengthSquared measures progress, the unit normal gives its left side, and curve is the maximum offset.
 */
record RocketMapCurve(double startX, double startY,
                         double directionX, double directionY, double lengthSquared,
                         double normalX, double normalY, double curve) {

    // Keep outbound and return legs apart without overwhelming the map.
    private static final double TRANSFER_CURVE_RATIO = 0.12;

    static RocketMapCurve create(Point start, Point end, boolean bend) {
        double directionX = end.x() - start.x();
        double directionY = end.y() - start.y();
        double lengthSquared = directionX * directionX + directionY * directionY;
        double length = Math.sqrt(lengthSquared);
        double normalX = length == 0 ? 0 : -directionY / length;
        double normalY = length == 0 ? 0 : directionX / length;
        return new RocketMapCurve(start.x(), start.y(), directionX, directionY, lengthSquared,
                normalX, normalY, bend ? length * TRANSFER_CURVE_RATIO : 0);
    }

    Point apply(Point point) {
        if (lengthSquared <= 0.000001) return point;
        double progress = Math.clamp(
                ((point.x() - startX) * directionX + (point.y() - startY) * directionY) / lengthSquared,
                0, 1);
        // Quadratic Bezier bow: zero offset at each end, full offset halfway along.
        // The left normal reverses on the return trip, so both legs remain distinguishable.
        double offset = 4 * progress * (1 - progress) * curve;
        return new Point(point.x() + normalX * offset, point.y() + normalY * offset);
    }

    static void alignOrigin(Point[] points, Point origin) {
        if (origin == null) return;
        // Join the previous displayed endpoint, then fade its offset out before reaching the new target.
        double offsetX = origin.x() - points[0].x();
        double offsetY = origin.y() - points[0].y();
        var distances = new double[points.length];
        for (int index = 1; index < points.length; index++) {
            distances[index] = distances[index - 1]
                    + Math.hypot(points[index].x() - points[index - 1].x(),
                    points[index].y() - points[index - 1].y());
        }
        double totalDistance = distances[points.length - 1];
        if (totalDistance == 0) {
            java.util.Arrays.fill(points, origin);
            return;
        }
        for (int index = 0; index < points.length; index++) {
            double remaining = 1 - distances[index] / totalDistance;
            points[index] = new Point(points[index].x() + offsetX * remaining,
                    points[index].y() + offsetY * remaining);
        }
    }

    void appendSegments(List<RenderedPathSegment> segments, Point[] points, List<PathSample> samples) {
        for (int index = 1; index < points.length; index++) {
            var rawFrom = points[index - 1];
            var rawTo = points[index];
            // Subdivide before bending so sparse, early-stopped transfers are smooth too.
            double length = Math.hypot(rawTo.x() - rawFrom.x(), rawTo.y() - rawFrom.y());
            int subdivisions = lengthSquared <= 0 ? 1
                    : (int) Math.clamp(Math.ceil(length / Math.sqrt(lengthSquared) * 64), 1, 64);
            var from = apply(rawFrom);
            for (int subdivision = 1; subdivision <= subdivisions; subdivision++) {
                double progress = subdivision / (double) subdivisions;
                var to = apply(rawFrom.lerp(rawTo, progress));
                segments.add(new RenderedPathSegment(from, to, samples.get(index - 1), samples.get(index),
                        (subdivision - 1d) / subdivisions, progress));
                from = to;
            }
        }
    }

}
