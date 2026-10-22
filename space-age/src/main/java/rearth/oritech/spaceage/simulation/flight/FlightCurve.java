package rearth.oritech.spaceage.simulation.flight;

import rearth.oritech.spaceage.simulation.SpaceBalance;

/**
 * Cubic Bezier with an optional interior reversal bow and a bounded arc-length table.
 */
final class FlightCurve {

    private static final int STEPS = 256;
    private final double ax;
    private final double ay;
    private final double bx;
    private final double by;
    private final double cx;
    private final double cy;
    private final double dx;
    private final double dy;
    private final double bowX;
    private final double bowY;
    private final double[] lengths = new double[STEPS + 1];

    static FlightCurve between(double startX, double startY, double endX, double endY,
                               double sx, double sy, double ex, double ey, double startHandle) {

        var dx = endX - startX;
        var dy = endY - startY;
        var distance = Math.max(1e-6, Math.hypot(dx, dy));
        var endHandle = Math.max(distance / 3, startHandle / 4);
        var collinear = Math.abs(sx * dy - sy * dx) < distance * .01
                && Math.abs(ex * dy - ey * dx) < distance * .01;
        var turnsBack = sx * dx + sy * dy < 0 || ex * dx + ey * dy < 0;
        // Long braking approaches need width and an arrival handle proportional to their extent.
        var bow = Math.max(0, startHandle - distance) * SpaceBalance.REVERSAL_STEERING_RATIO;
        if (collinear && turnsBack) bow = Math.max(bow, distance * SpaceBalance.REVERSAL_STEERING_RATIO);
        // Stay on the control polygon's side so the bow reinforces its curvature.
        var side = (sx * dy - sy * dx) * startHandle - (ex * dy - ey * dx) * endHandle;
        if (side > 0) bow = -bow;
        return new FlightCurve(startX, startY, startX + sx * startHandle, startY + sy * startHandle,
                endX - ex * endHandle, endY - ey * endHandle, endX, endY, -dy / distance * bow, dx / distance * bow);
    }

    private FlightCurve(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy, double bowX, double bowY) {

        this.ax = ax;
        this.ay = ay;
        this.bx = bx;
        this.by = by;
        this.bowX = bowX;
        this.bowY = bowY;
        this.cx = cx;
        this.cy = cy;
        this.dx = dx;
        this.dy = dy;
        var previous = at(0);
        for (int i = 1; i <= STEPS; i++) {
            var next = at(i / (double) STEPS);
            lengths[i] = lengths[i - 1] + Math.hypot(next.x() - previous.x(), next.y() - previous.y());
            previous = next;
        }
    }

    double turnAngle() {

        double angle = 0;
        var previous = at(0);
        for (int i = 1; i <= STEPS; i++) {
            var next = at(i / (double) STEPS);
            angle += Math.abs(Math.atan2(previous.vx() * next.vy() - previous.vy() * next.vx(),
                    previous.vx() * next.vx() + previous.vy() * next.vy()));
            previous = next;
        }
        return angle;
    }

    double length() {

        return lengths[STEPS];
    }

    FlightMotion.State atDistance(double distance, double speed) {

        distance = Math.clamp(distance, 0, length());
        // Long overshoots can leave tiny arc-length errors near a short endpoint handle.
        // Use the exact endpoint tangent once the remaining distance is below numerical precision.
        if (length() - distance <= Math.max(1e-7, Math.ulp(length()) * 32)) distance = length();
        var low = 1;
        var high = STEPS;
        while (low < high) {
            var middle = (low + high) >>> 1;
            if (lengths[middle] < distance) low = middle + 1;
            else high = middle;
        }
        var span = lengths[low] - lengths[low - 1];
        var t = (low - 1 + (span == 0 ? 0 : (distance - lengths[low - 1]) / span)) / STEPS;
        var point = at(t);
        var magnitude = Math.hypot(point.vx(), point.vy());
        return new FlightMotion.State(point.x(), point.y(), point.vx() / Math.max(1e-12, magnitude) * speed,
                point.vy() / Math.max(1e-12, magnitude) * speed);
    }

    private FlightMotion.State at(double t) {

        var u = 1 - t;
        var bow = 16 * t * t * u * u;
        var bowDerivative = 32 * t * u * (1 - 2 * t);
        return new FlightMotion.State(bowX * bow + u * u * u * ax + 3 * u * u * t * bx + 3 * u * t * t * cx + t * t * t * dx,
                bowY * bow + u * u * u * ay + 3 * u * u * t * by + 3 * u * t * t * cy + t * t * t * dy,
                bowX * bowDerivative + 3 * u * u * (bx - ax) + 6 * u * t * (cx - bx) + 3 * t * t * (dx - cx),
                bowY * bowDerivative + 3 * u * u * (by - ay) + 6 * u * t * (cy - by) + 3 * t * t * (dy - cy));
    }
}
