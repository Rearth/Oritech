package rearth.oritech.spaceage.client;

import rearth.oritech.spaceage.client.RocketMapPaths.Point;
import rearth.oritech.api.screen.widgets.BlockWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Maps simulation surface points to the solid cube's outline in the star map. */
final class PlanetSurfaceProjection {

    private PlanetSurfaceProjection() {
    }

    static List<Point> outline(BlockWidget widget) {

        var corners = new ArrayList<Point>();
        // The solid Sun/Earth/Mars cubes occupy model coordinates [-8, 24] / 16.
        for (var x : new float[]{-.5f, 1.5f})
            for (var y : new float[]{-.5f, 1.5f})
                for (var z : new float[]{-.5f, 1.5f}) {
                    var projected = widget.projectModelPoint(x, y, z);
                    corners.add(new Point(projected.x, projected.y));
                }
        corners.sort(Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y));
        var hull = new ArrayList<Point>();
        for (var corner : corners) appendCorner(hull, corner, 0);
        var lowerSize = hull.size();
        for (var index = corners.size() - 2; index >= 0; index--) appendCorner(hull, corners.get(index), lowerSize - 1);
        hull.removeLast();
        return List.copyOf(hull);
    }

    private static void appendCorner(List<Point> hull, Point corner, int start) {

        while (hull.size() >= start + 2 && cross(hull.get(hull.size() - 2), hull.getLast(), corner) <= 1e-7)
            hull.removeLast();
        hull.add(corner);
    }

    private static double cross(Point a, Point b, Point c) {

        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    static boolean contains(List<Point> outline, Point point) {

        for (var index = 0; index < outline.size(); index++)
            if (cross(outline.get(index), outline.get((index + 1) % outline.size()), point) < -1e-7) return false;
        return true;
    }

    static Point closestEdge(Point point, double centerX, double centerY, double radius, List<Point> outline) {

        // Work in projected units without zoom, so the nearest edge stays the same at every scale.
        var scale = radius;
        var x = (point.x() - centerX) / scale;
        var y = (point.y() - centerY) * StarMapCamera.PLANE_TILT / scale;
        var closest = outline.getFirst();
        var distance = Double.POSITIVE_INFINITY;
        for (var index = 0; index < outline.size(); index++) {
            var a = outline.get(index);
            var b = outline.get((index + 1) % outline.size());
            var dx = b.x() - a.x();
            var dy = b.y() - a.y();
            var t = Math.clamp(((x - a.x()) * dx + (y - a.y()) * dy) / (dx * dx + dy * dy), 0, 1);
            var candidate = a.lerp(b, t);
            var candidateDistance = Math.hypot(x - candidate.x(), y - candidate.y());
            if (candidateDistance < distance) {
                distance = candidateDistance;
                closest = candidate;
            }
        }
        return new Point(centerX + closest.x() * scale,
                centerY + closest.y() * scale / StarMapCamera.PLANE_TILT);
    }
}
