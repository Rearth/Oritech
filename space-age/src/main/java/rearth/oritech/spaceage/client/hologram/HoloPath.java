package rearth.oritech.spaceage.client.hologram;

import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Craft;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Point;
import rearth.oritech.spaceage.block.hologram.HoloView;
import rearth.oritech.spaceage.block.hologram.HoloView.Frame;
import rearth.oritech.spaceage.client.starmap.RocketMapPaths;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.mission.MissionJourney.Leg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The star map's curve/marker geometry, with surface endpoints fitted to the hologram cubes. */
final class HoloPath {

    private final HoloSnapshot snapshot;
    private final Frame frame;
    private final CraftPath path;
    private final RocketMapPaths geometry;

    HoloPath(Craft craft, HoloSnapshot snapshot, Frame frame) {

        this.snapshot = snapshot;
        this.frame = frame;
        path = new CraftPath(craft.id(), Set.of(), craft.path(), List.of(),
                craft.path().isEmpty() ? 0 : craft.path().getLast().timeSeconds(), 0, TerminalState.READY);
        geometry = new RocketMapPaths(path, point -> {
            var mapped = surface(new Point(point.x(), point.y()));
            return new RocketMapPaths.Point(mapped.x(), mapped.y());
        });
    }

    private Point surface(Point point) {

        return HoloView.surfacePosition(point, frame, snapshot);
    }

    Point position(double time, double fallbackX, double fallbackY) {

        var point = geometry.renderedWorldPosition(path.branchId(), time, fallbackX, fallbackY);
        return new Point(point.x(), point.y());
    }

    List<Point> active() {

        // About one pixel of curve error per 128 pixels across a projected block, independent of zoom.
        var scale = 128 * Math.max(.1, Math.min(snapshot.surface().width(), snapshot.surface().depth()) - .5) / frame.coverage();
        var pieces = geometry.renderedPathSegments(path, point -> new RocketMapPaths.Point(point.x() * scale, point.y() * scale),
                -Double.MAX_VALUE / 4, -Double.MAX_VALUE / 4, Double.MAX_VALUE / 2, Double.MAX_VALUE / 2);
        if (pieces.isEmpty()) return List.of();
        var result = new ArrayList<Point>();
        result.add(new Point(pieces.getFirst().from().x() / scale, pieces.getFirst().from().y() / scale));
        for (var piece : pieces) result.add(new Point(piece.to().x() / scale, piece.to().y() / scale));
        return result;
    }

    List<Point> journey(Leg leg) {

        if (leg.points().isEmpty()) return List.of();
        var first = leg.points().getFirst();
        var last = leg.points().getLast();
        var start = surface(new Point(first.x(), first.y()));
        var end = surface(new Point(last.x(), last.y()));
        var result = new ArrayList<Point>();
        for (int i = 0; i < leg.points().size(); i++) {
            var point = leg.points().get(i);
            var t = leg.points().size() > 1 ? (double) i / (leg.points().size() - 1) : 0;
            var blend = t * t * (3 - 2 * t);
            result.add(new Point(point.x() + (start.x() - first.x()) * (1 - blend) + (end.x() - last.x()) * blend,
                    point.y() + (start.y() - first.y()) * (1 - blend) + (end.y() - last.y()) * blend));
        }
        return result;
    }
}
