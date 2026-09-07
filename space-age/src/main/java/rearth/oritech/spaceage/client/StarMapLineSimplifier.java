package rearth.oritech.spaceage.client;

import java.util.ArrayList;
import java.util.List;

/** Drops offscreen lines and combines tiny trajectory pieces before they reach the GUI renderer. */
final class StarMapLineSimplifier {
    // Four GUI pixels are enough to follow a smooth transfer at the current zoom.
    private static final double MAX_CHORD_SQUARED = 16;

    private StarMapLineSimplifier() {
    }

    static List<StarMapLineRenderer.Line> simplify(List<StarMapLineRenderer.Line> lines,
                                                  int x, int y, int width, int height) {
        var result = new ArrayList<StarMapLineRenderer.Line>();
        StarMapLineRenderer.Line pending = null;
        for (var line : lines) {
            if (!intersects(line.fromX(), line.fromY(), line.toX(), line.toY(),
                    x, y, width, height, line.width() * 0.5 + 1)) {
                // Do not join across a trip outside the viewport.
                if (pending != null) result.add(pending);
                pending = null;
                continue;
            }
            if (pending != null && canCombine(pending, line)) {
                pending = new StarMapLineRenderer.Line(pending.fromX(), pending.fromY(), line.toX(), line.toY(),
                        line.color(), line.width(), true);
            } else {
                if (pending != null) result.add(pending);
                pending = line;
            }
        }
        if (pending != null) result.add(pending);
        return List.copyOf(result);
    }

    private static boolean canCombine(StarMapLineRenderer.Line first, StarMapLineRenderer.Line next) {
        if (!first.antialiased() || !next.antialiased() || first.color() != next.color()
                || first.width() != next.width() || first.toX() != next.fromX() || first.toY() != next.fromY()) return false;
        double dx = next.toX() - first.fromX();
        double dy = next.toY() - first.fromY();
        // Keep long segments intact, even when their next point happens to turn back near the start.
        double previousX = first.toX() - first.fromX();
        double previousY = first.toY() - first.fromY();
        return dx * dx + dy * dy <= MAX_CHORD_SQUARED
                && previousX * previousX + previousY * previousY <= MAX_CHORD_SQUARED;
    }

    static boolean intersects(double fromX, double fromY, double toX, double toY,
                              int x, int y, int width, int height, double margin) {
        // Bounding-box rejection also keeps crossing lines whose endpoints are both outside the map.
        return Math.max(fromX, toX) >= x - margin && Math.min(fromX, toX) <= x + width + margin
                && Math.max(fromY, toY) >= y - margin && Math.min(fromY, toY) <= y + height + margin;
    }
}
