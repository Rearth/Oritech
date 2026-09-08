package rearth.oritech.spaceage.client;

import java.util.List;

/** Converts between the solar-system plane and the visible map viewport. */
final class StarMapCamera {

    // Vertical squash gives the shared solar-system plane its tilted appearance.
    static final double PLANE_TILT = 0.58;
    // Stop zooming before a single object fills the map.
    private static final double MAX_ZOOM = 0.024;
    // Screen-space drag velocity decays each rendered frame after release.
    private static final double PAN_FRICTION = 0.82;
    private static final double PAN_VELOCITY_BLEND = 0.65;

    // World position at the center of the viewport.
    private double centerX;
    private double centerY;
    // Screen pixels per world unit before applying the plane tilt.
    private double zoom;
    // Wheel input accumulates here while the displayed zoom catches up smoothly.
    private double targetZoom;
    // World point and viewport offset kept fixed throughout the current zoom animation.
    private double zoomAnchorX;
    private double zoomAnchorY;
    private double zoomAnchorScreenX;
    private double zoomAnchorScreenY;
    private boolean zooming;
    private double panVelocityX;
    private double panVelocityY;
    private boolean dragging;
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
        var frame = frame(points, viewportWidth, viewportHeight, 1.12);
        centerX = frame.centerX;
        centerY = frame.centerY;
        minimumZoom = frame.zoom * 0.7;
        zoom = Math.min(MAX_ZOOM, minimumZoom * 1.35);
        stopAnimations();
    }

    void focus(List<Point> points, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        if (points.isEmpty()) return;
        revision++;
        var frame = frame(points, viewportWidth, viewportHeight, 1.35);
        centerX = frame.centerX;
        centerY = frame.centerY;
        zoom = Math.clamp(frame.zoom, minimumZoom * 0.5, MAX_ZOOM);
        stopAnimations();
    }

    private static Frame frame(List<Point> points, int viewportWidth, int viewportHeight, double padding) {
        var minX = points.stream().mapToDouble(Point::x).min().orElse(-1);
        var maxX = points.stream().mapToDouble(Point::x).max().orElse(1);
        var minY = points.stream().mapToDouble(Point::y).min().orElse(-1);
        var maxY = points.stream().mapToDouble(Point::y).max().orElse(1);
        var fitX = Math.max(1, maxX - minX) * padding;
        var fitY = Math.max(1, maxY - minY) * PLANE_TILT * padding;
        var zoom = Math.min(Math.max(1, viewportWidth - 20) / fitX,
                Math.max(1, viewportHeight - 13) / fitY);
        return new Frame((minX + maxX) * 0.5, (minY + maxY) * 0.5, zoom);
    }

    void copyFrom(StarMapCamera previous) {
        revision++;
        centerX = previous.centerX;
        centerY = previous.centerY;
        zoom = Math.clamp(previous.zoom, minimumZoom * 0.5, MAX_ZOOM);
        stopAnimations();
    }

    void zoomAt(double screenX, double screenY, double scrollDelta,
                int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        if (scrollDelta == 0) return;
        revision++;
        stopPanning();
        var before = unproject(screenX, screenY, viewportX, viewportY, viewportWidth, viewportHeight);
        targetZoom = Math.clamp((zooming ? targetZoom : zoom) * Math.pow(1.2, scrollDelta),
                minimumZoom * 0.5, MAX_ZOOM);
        zoomAnchorX = before.x;
        zoomAnchorY = before.y;
        zoomAnchorScreenX = screenX - (viewportX + viewportWidth * 0.5);
        zoomAnchorScreenY = screenY - (viewportY + viewportHeight * 0.5);
        zooming = Math.abs(targetZoom - zoom) > 1e-12;
    }

    void advanceZoom() {
        if (!zooming) return;
        var difference = targetZoom - zoom;
        zoom += difference * 0.22;
        if (Math.abs(difference) <= Math.max(1e-12, targetZoom * 0.0005)) {
            zoom = targetZoom;
            zooming = false;
        }
        centerX = zoomAnchorX - zoomAnchorScreenX / zoom;
        centerY = zoomAnchorY - zoomAnchorScreenY / (zoom * PLANE_TILT);
        revision++;
    }

    void pan(double deltaX, double deltaY) {
        if (deltaX == 0 && deltaY == 0) return;
        revision++;
        stopAnimations();
        applyPan(deltaX, deltaY);
    }

    void beginDrag() {
        dragging = true;
        stopZooming();
        panVelocityX = 0;
        panVelocityY = 0;
    }

    void dragBy(double deltaX, double deltaY) {
        if (!dragging || deltaX == 0 && deltaY == 0) return;
        revision++;
        applyPan(deltaX, deltaY);
        panVelocityX = panVelocityX * (1 - PAN_VELOCITY_BLEND) + deltaX * PAN_VELOCITY_BLEND;
        panVelocityY = panVelocityY * (1 - PAN_VELOCITY_BLEND) + deltaY * PAN_VELOCITY_BLEND;
    }

    void endDrag() {
        dragging = false;
    }

    void advancePan() {
        if (dragging || Math.abs(panVelocityX) + Math.abs(panVelocityY) < 0.05) {
            if (!dragging) stopPanning();
            return;
        }
        applyPan(panVelocityX, panVelocityY);
        panVelocityX *= PAN_FRICTION;
        panVelocityY *= PAN_FRICTION;
        revision++;
    }

    private void applyPan(double deltaX, double deltaY) {
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

    private void stopZooming() {
        targetZoom = zoom;
        zooming = false;
    }

    private void stopPanning() {
        dragging = false;
        panVelocityX = 0;
        panVelocityY = 0;
    }

    private void stopAnimations() {
        stopZooming();
        stopPanning();
    }

    // Coordinates returned by projection or its inverse.
    record Point(double x, double y) {
    }

    private record Frame(double centerX, double centerY, double zoom) {
    }
}
