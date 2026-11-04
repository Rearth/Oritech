package rearth.oritech.spaceage.block.hologram;

import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Craft;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Point;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SpaceObjectData;
import rearth.oritech.spaceage.simulation.SpaceObjects.ObjectType;

import java.util.ArrayList;
import java.util.List;

/** Shared display geometry for the controller preview, world projection and tests. */
public final class HoloView {

    public static final double PLANE_HEIGHT = .5;

    public static double modelDiameter(SpaceObjectData object, Frame frame, HoloSnapshot snapshot) {

        var asteroid = object.type() == ObjectType.ASTEROID;
        var scale = Math.max(.1, Math.min(snapshot.surface().width(), snapshot.surface().depth()) - .5) / frame.coverage();
        // Match the map's enlarged asteroid icons, with smaller planets and readable small fragments.
        var nativeSize = object.radius() * scale * (asteroid ? 104 : .55);
        var limit = Math.min(.7, Math.min(snapshot.settings().height(), Math.min(snapshot.surface().width(), snapshot.surface().depth()) * .35));
        return Math.min(limit, Math.max((asteroid ? .16 : .08) * snapshot.settings().markerSize(), nativeSize));
    }

    private HoloView() {
    }

    public static boolean selected(HoloSettings settings, Craft craft) {

        return switch (settings.mode()) {
            case "craft" -> settings.craft().equals(craft.id());
            case "category" -> settings.category().equals(craft.category());
            default -> true;
        };
    }

    public static Frame frame(HoloSnapshot snapshot, Frame previous) {

        var s = snapshot.settings();
        if (s.mode().equals("area") || s.framing().equals("fixed")) return new Frame(s.centerX(), s.centerY(), s.coverage());
        var crafts = snapshot.craft().stream().filter(c -> selected(s, c)).toList();
        if (crafts.isEmpty()) return previous == null ? new Frame(s.centerX(), s.centerY(), s.coverage()) : previous;
        var points = new ArrayList<Point>();
        for (var craft : crafts) {
            points.add(new Point(craft.x(), craft.y()));
            if (s.framing().equals("journey")) {
                craft.path().forEach(point -> points.add(new Point(point.x(), point.y())));
                craft.journey().forEach(leg -> leg.points().forEach(point -> points.add(new Point(point.x(), point.y()))));
            }
            if (s.framing().equals("journey") || s.framing().equals("fit"))
                snapshot.objects().stream().filter(o -> o.id().equals(craft.target())).findFirst()
                        .ifPresent(o -> points.add(new Point(o.x(), o.y())));
        }
        var minX = points.stream().mapToDouble(Point::x).min().orElseThrow();
        var maxX = points.stream().mapToDouble(Point::x).max().orElseThrow();
        var minY = points.stream().mapToDouble(Point::y).min().orElseThrow();
        var maxY = points.stream().mapToDouble(Point::y).max().orElseThrow();
        var centerX = (minX + maxX) / 2;
        var centerY = (minY + maxY) / 2;
        var angle = Math.toRadians(s.rotation() + snapshot.facing());
        var extent = 0d;
        for (var point : points) {
            var dx = point.x() - centerX;
            var dy = point.y() - centerY;
            extent = Math.max(extent, 2 * Math.max(Math.abs(dx * Math.cos(angle) - dy * Math.sin(angle)),
                    Math.abs(dx * Math.sin(angle) + dy * Math.cos(angle))) * s.margin());
        }
        var coverage = s.framing().equals("follow") || s.framing().equals("local") ? s.coverage()
                : Math.clamp(extent, s.minimum(), s.maximum());
        return new Frame(centerX, centerY, coverage);
    }

    public record Frame(double x, double y, double coverage) {

        public Frame towards(Frame target, double seconds, double response) {

            var movingOut = target.coverage > coverage;
            var alpha = 1 - Math.exp(-Math.clamp(seconds, 0, .1) * (movingOut ? response * 4 : response));
            var desiredCoverage = Math.abs(target.coverage - coverage) < coverage * .1 ? coverage : target.coverage;
            return new Frame(x + (target.x - x) * alpha, y + (target.y - y) * alpha,
                    coverage + (desiredCoverage - coverage) * alpha);
        }
    }

    public static Point project(double x, double y, Frame frame, HoloSnapshot snapshot) {

        var angle = Math.toRadians(snapshot.settings().rotation() + snapshot.facing());
        var dx = x - frame.x();
        var dy = y - frame.y();
        var scale = Math.max(.1, Math.min(snapshot.surface().width(), snapshot.surface().depth()) - .5) / frame.coverage();
        return new Point(snapshot.surface().width() / 2d + (dx * Math.cos(angle) - dy * Math.sin(angle)) * scale,
                snapshot.surface().depth() / 2d + (dx * Math.sin(angle) + dy * Math.cos(angle)) * scale);
    }

    public static Point unproject(Point point, Frame frame, HoloSnapshot snapshot) {

        var angle = Math.toRadians(snapshot.settings().rotation() + snapshot.facing());
        var scale = Math.max(.1, Math.min(snapshot.surface().width(), snapshot.surface().depth()) - .5) / frame.coverage();
        var dx = (point.x() - snapshot.surface().width() / 2d) / scale;
        var dy = (point.y() - snapshot.surface().depth() / 2d) / scale;
        return new Point(frame.x() + dx * Math.cos(angle) + dy * Math.sin(angle),
                frame.y() - dx * Math.sin(angle) + dy * Math.cos(angle));
    }

    public static Point surfacePosition(Point point, Frame frame, HoloSnapshot snapshot) {

        if (!snapshot.settings().shows(HoloSettings.BODIES)) return point;
        for (var body : snapshot.objects()) {
            if (body.type() != ObjectType.EARTH && body.type() != ObjectType.MARS && body.type() != ObjectType.SUN
                    && body.type() != ObjectType.RESOURCE_BODY) continue;
            if (Math.abs(Math.hypot(point.x() - body.x(), point.y() - body.y()) - body.radius()) >= .1) continue;
            var center = project(body.x(), body.y(), frame, snapshot);
            if (!inside(center, snapshot.surface())) return point;
            var projected = project(point.x(), point.y(), frame, snapshot);
            var halfSize = modelDiameter(body, frame, snapshot) / 2;
            var dx = projected.x() - center.x();
            var dy = projected.y() - center.y();
            var x = Math.clamp(dx, -halfSize, halfSize);
            var y = Math.clamp(dy, -halfSize, halfSize);
            if (Math.abs(dx) < halfSize && Math.abs(dy) < halfSize) {
                if (Math.abs(dx) >= Math.abs(dy)) x = Math.copySign(halfSize, dx);
                else y = Math.copySign(halfSize, dy);
            }
            return unproject(new Point(center.x() + x, center.y() + y), frame, snapshot);
        }
        return point;
    }

    public static boolean inside(Point point, HoloSurface surface) {

        return point.x() >= .08 && point.y() >= .08 && point.x() <= surface.width() - .08 && point.y() <= surface.depth() - .08;
    }

    public static List<Point> clip(Point a, Point b, HoloSurface surface) {

        var dx = b.x() - a.x();
        var dy = b.y() - a.y();
        double start = 0;
        double end = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {a.x() - .08, surface.width() - .08 - a.x(), a.y() - .08, surface.depth() - .08 - a.y()};
        for (int i = 0; i < 4; i++) {
            if (Math.abs(p[i]) < 1e-12) {
                if (q[i] < 0) return List.of();
            } else if (p[i] < 0) start = Math.max(start, q[i] / p[i]);
            else end = Math.min(end, q[i] / p[i]);
        }
        return start > end ? List.of() : List.of(new Point(a.x() + dx * start, a.y() + dy * start),
                new Point(a.x() + dx * end, a.y() + dy * end));
    }
}
