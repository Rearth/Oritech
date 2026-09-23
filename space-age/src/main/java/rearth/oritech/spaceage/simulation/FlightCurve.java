package rearth.oritech.spaceage.simulation;

/** Cubic Bezier with an optional interior reversal bow and a bounded arc-length table. */
final class FlightCurve {
    private static final int STEPS = 256;
    private final double ax, ay, bx, by, cx, cy, dx, dy, bowX, bowY;
    private final double[] lengths = new double[STEPS + 1];

    FlightCurve(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy, double bowX, double bowY) {
        this.ax = ax; this.ay = ay; this.bx = bx; this.by = by;
        this.bowX = bowX; this.bowY = bowY;
        this.cx = cx; this.cy = cy; this.dx = dx; this.dy = dy;
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

    double length() { return lengths[STEPS]; }

    FlightMotion.State atDistance(double distance, double speed) {
        distance = Math.clamp(distance, 0, length());
        int low = 1, high = STEPS;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (lengths[middle] < distance) low = middle + 1; else high = middle;
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
        var bow = 16 * t*t*u*u;
        var bowDerivative = 32 * t*u*(1 - 2*t);
        return new FlightMotion.State(bowX * bow + u*u*u*ax + 3*u*u*t*bx + 3*u*t*t*cx + t*t*t*dx,
                bowY * bow + u*u*u*ay + 3*u*u*t*by + 3*u*t*t*cy + t*t*t*dy,
                bowX * bowDerivative + 3*u*u*(bx-ax) + 6*u*t*(cx-bx) + 3*t*t*(dx-cx),
                bowY * bowDerivative + 3*u*u*(by-ay) + 6*u*t*(cy-by) + 3*t*t*(dy-cy));
    }
}
