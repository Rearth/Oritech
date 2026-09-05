package rearth.oritech.spaceage.simulation;

import java.util.List;

/** Burn/coast/burn timing and directions, accounting for the full sequence of engine power changes. */
public record FullPowerTransfer(double firstSeconds, double coastSeconds, double lastSeconds,
                                double firstDirectionX, double firstDirectionY, double lastDirectionX, double lastDirectionY) {

    public double duration() {
        return firstSeconds + coastSeconds + lastSeconds;
    }

    public static FullPowerTransfer solve(double dx, double dy, double vx, double vy,
                                          double arrivalX, double arrivalY, boolean freeArrival,
                                          double acceleration, double deltaV, double speedLimit, double timeLimit) {
        var intervals = acceleration > 0 && deltaV > 0
                ? List.of(new RocketBurnProfile.Interval(acceleration, deltaV / acceleration)) : List.<RocketBurnProfile.Interval>of();
        return solve(dx, dy, vx, vy, arrivalX, arrivalY, freeArrival,
                new RocketBurnProfile(intervals), speedLimit, timeLimit);
    }

    public static FullPowerTransfer solve(double dx, double dy, double vx, double vy,
                                          double arrivalX, double arrivalY, boolean freeArrival,
                                          RocketBurnProfile profile, double speedLimit, double timeLimit) {
        var deltaV = profile.deltaV();
        if (!(speedLimit > 0) || !(timeLimit > 0)) return null;
        var speed = Math.hypot(vx, vy);
        var ballisticTime = speed == 0 ? 0 : (dx * vx + dy * vy) / (speed * speed);
        var ballistic = ballisticTime > 0 && ballisticTime <= timeLimit && speed <= speedLimit
                && Math.hypot(dx - vx * ballisticTime, dy - vy * ballisticTime) < 1e-5
                && (freeArrival || Math.hypot(arrivalX - vx, arrivalY - vy) < 1e-7);
        if (!(deltaV > 0)) {
            return ballistic ? new FullPowerTransfer(0, ballisticTime, 0, 0, 0, 0, 0) : null;
        }
        if (!freeArrival && (Math.hypot(arrivalX, arrivalY) > speedLimit
                || Math.hypot(arrivalX - vx, arrivalY - vy) > deltaV + 1e-7)) return null;

        var low = 0d;
        // A fast craft may have a very narrow intercept window; seed it with an exact ballistic intercept.
        var high = ballistic ? ballisticTime : Math.min(0.05, timeLimit);
        FullPowerTransfer result;
        while (true) {
            result = atDuration(dx, dy, vx, vy, arrivalX, arrivalY, freeArrival,
                    profile, deltaV, speedLimit, high);
            if (result != null) break;
            if (high >= timeLimit) return null;
            low = high;
            high = Math.min(timeLimit, high * 1.12);
        }
        for (var i = 0; i < 36; i++) {
            var middle = (low + high) / 2;
            var candidate = atDuration(dx, dy, vx, vy, arrivalX, arrivalY, freeArrival,
                    profile, deltaV, speedLimit, middle);
            if (candidate == null) low = middle;
            else {
                high = middle;
                result = candidate;
            }
        }
        return result;
    }

    private static FullPowerTransfer atDuration(double dx, double dy, double vx, double vy,
                                                double fx, double fy, boolean freeArrival,
                                                RocketBurnProfile profile, double budget, double cap, double time) {
        var cx = dx / time;
        var cy = dy / time;
        // Solve for cruise velocity. Each burn integrates the engine intervals it will actually use,
        // including any changes partway through braking.
        for (var iteration = 0; iteration < 80; iteration++) {
            var ux = cx - vx;
            var uy = cy - vy;
            var wx = freeArrival ? 0 : cx - fx;
            var wy = freeArrival ? 0 : cy - fy;
            var u = Math.hypot(ux, uy);
            var w = Math.hypot(wx, wy);
            var first = profile.burn(0, u);
            var last = profile.burn(u, w);
            if (first.seconds() + last.seconds() > time + 1e-7) return null;
            var firstLag = u * first.seconds() - first.distance();
            var firstScale = u == 0 ? 0 : firstLag / u;
            var lastScale = w == 0 ? 0 : last.distance() / w;
            var ex = cx * time - ux * firstScale - wx * lastScale - dx;
            var ey = cy * time - uy * firstScale - wy * lastScale - dy;
            if (Math.hypot(ex, ey) <= 1e-5) {
                if (u + w > budget + 1e-7 || Math.hypot(cx, cy) > cap + 1e-7) return null;
                return new FullPowerTransfer(first.seconds(), Math.max(0, time - first.seconds() - last.seconds()), last.seconds(),
                        u == 0 ? 0 : ux / u, u == 0 ? 0 : uy / u,
                        w == 0 ? 0 : -wx / w, w == 0 ? 0 : -wy / w);
            }
            var jxx = time - firstScale - lastScale;
            var jyy = jxx;
            var jxy = 0d;
            var jyx = 0d;
            if (u > 0) {
                var derivative = (first.seconds() - firstScale) / (u * u);
                jxx -= derivative * ux * ux;
                jyy -= derivative * uy * uy;
                jxy -= derivative * ux * uy;
                jyx = jxy;
            }
            if (w > 0) {
                var derivative = (w / last.endAcceleration() - lastScale) / (w * w);
                jxx -= derivative * wx * wx;
                jyy -= derivative * wy * wy;
                jxy -= derivative * wx * wy;
                jyx -= derivative * wx * wy;
                // Changing the first burn also changes which intervals remain for the last burn.
                if (u > 0) {
                    var offsetDerivative = (w / last.endAcceleration() - last.seconds()) / (u * w);
                    jxx -= offsetDerivative * wx * ux;
                    jyy -= offsetDerivative * wy * uy;
                    jxy -= offsetDerivative * wx * uy;
                    jyx -= offsetDerivative * wy * ux;
                }
            }
            var determinant = jxx * jyy - jxy * jyx;
            if (!(determinant > 1e-15)) return null;
            cx -= (jyy * ex - jxy * ey) / determinant;
            cy -= (jxx * ey - jyx * ex) / determinant;
            if (!Double.isFinite(cx) || !Double.isFinite(cy)) return null;
        }
        return null;
    }
}
