package rearth.oritech.spaceage.simulation;

import java.util.List;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathSample;

/** Cubic Hermite motion between position/velocity samples, shared by missions and maps. */
public record FlightMotion(PathSample start, PathSample end) {
    public double duration() { return end.timeSeconds() - start.timeSeconds(); }
    public State at(double fraction) {
        if (duration() <= 0) return new State(end.x(), end.y(), end.velocityX(), end.velocityY());
        var t = Math.clamp(fraction, 0, 1);
        var u = 1 - t;
        var bx = start.x() + start.velocityX() * duration() / 3;
        var by = start.y() + start.velocityY() * duration() / 3;
        var cx = end.x() - end.velocityX() * duration() / 3;
        var cy = end.y() - end.velocityY() * duration() / 3;
        return new State(u*u*u*start.x() + 3*u*u*t*bx + 3*u*t*t*cx + t*t*t*end.x(),
                u*u*u*start.y() + 3*u*u*t*by + 3*u*t*t*cy + t*t*t*end.y(),
                (3*u*u*(bx-start.x()) + 6*u*t*(cx-bx) + 3*t*t*(end.x()-cx)) / duration(),
                (3*u*u*(by-start.y()) + 6*u*t*(cy-by) + 3*t*t*(end.y()-cy)) / duration());
    }

    /** First sample at or after the requested time. Equal-time events keep their ordering. */
    public static int endIndex(List<PathSample> samples, double time) {
        int low = 0, high = samples.size() - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (samples.get(middle).timeSeconds() < time) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    public static State sample(List<PathSample> samples, double time) {
        var index = endIndex(samples, time);
        var end = samples.get(index);
        if (time > end.timeSeconds()) {
            var seconds = time - end.timeSeconds();
            return new State(end.x() + end.velocityX() * seconds, end.y() + end.velocityY() * seconds,
                    end.velocityX(), end.velocityY());
        }
        var motion = new FlightMotion(samples.get(Math.max(0, index - 1)), end);
        return motion.at(motion.duration() <= 0 ? 1 : (time - motion.start.timeSeconds()) / motion.duration());
    }

    public record State(double x, double y, double vx, double vy) { }
}
