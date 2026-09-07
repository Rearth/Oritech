package rearth.oritech.spaceage.client;

import java.util.List;

/** Converts between the solar-system plane and the visible map viewport. */
final class StarMapCamera {

    // Vertical squash gives the shared solar-system plane its tilted appearance.
    static final double PLANE_TILT = 0.58;
    // Stop zooming before a single object fills the map.
    private static final double MAX_ZOOM = 0.024;

    // World position at the center of the viewport.
    private double centerX;
    private double centerY;
    // Screen pixels per world unit before applying the plane tilt.
    private double zoom;
    // Fit-to-system scale used by zoom-out and view-copy limits.
    private double minimumZoom;
    // Cached screen geometry only needs rebuilding when the camera changes.
    private long revision;

    void fit(List<Point> points, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        revision++;
        if (points.isEmpty()) {
            zoom = minimumZoom = 0.0001;
            return;
        }
        var minX = points.stream().mapToDouble(Point::x).min().orElse(-1);
        var maxX = points.stream().mapToDouble(Point::x).max().orElse(1);
        var minY = points.stream().mapToDouble(Point::y).min().orElse(-1);
        var maxY = points.stream().mapToDouble(Point::y).max().orElse(1);
        centerX = (minX + maxX) * 0.5;
        centerY = (minY + maxY) * 0.5;
        var fitX = Math.max(1, maxX - minX) * 1.12;
        var fitY = Math.max(1, maxY - minY) * PLANE_TILT * 1.12;
        minimumZoom = Math.min((viewportWidth - 20) / fitX, (viewportHeight - 13) / fitY) * 0.7;
        zoom = minimumZoom * 1.35;
    }

    void copyFrom(StarMapCamera previous) {
        revision++;
        centerX = previous.centerX;
        centerY = previous.centerY;
        zoom = Math.clamp(previous.zoom, minimumZoom * 0.5, MAX_ZOOM);
    }

    void zoomAt(double screenX, double screenY, double scrollDelta,
                int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        if (scrollDelta == 0) return;
        revision++;
        var before = unproject(screenX, screenY, viewportX, viewportY, viewportWidth, viewportHeight);
        zoom = Math.clamp(zoom * Math.pow(1.2, scrollDelta), minimumZoom * 0.5, MAX_ZOOM);
        var after = unproject(screenX, screenY, viewportX, viewportY, viewportWidth, viewportHeight);
        centerX += before.x - after.x;
        centerY += before.y - after.y;
    }

    void pan(double deltaX, double deltaY) {
        if (deltaX == 0 && deltaY == 0) return;
        revision++;
        centerX -= deltaX / zoom;
        centerY -= deltaY / (zoom * PLANE_TILT);
    }

    Point project(double worldX, double worldY, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        return new Point(viewportX + viewportWidth * 0.5 + (worldX - centerX) * zoom,
                viewportY + viewportHeight * 0.5 + (worldY - centerY) * zoom * PLANE_TILT);
    }

    Point unproject(double screenX, double screenY, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        return new Point(centerX + (screenX - (viewportX + viewportWidth * 0.5)) / zoom,
                centerY + (screenY - (viewportY + viewportHeight * 0.5)) / (zoom * PLANE_TILT));
    }

    double zoom() {
        return zoom;
    }

    long revision() {
        return revision;
    }

    // Coordinates returned by projection or its inverse.
    record Point(double x, double y) {
    }
}
