package rearth.oritech.spaceage.simulation;

import java.util.List;

/** Future full-power intervals, in engine-on time. Coasting does not advance this profile. */
public record RocketBurnProfile(List<Interval> intervals) {

    public RocketBurnProfile {
        intervals = List.copyOf(intervals);
    }

    public double deltaV() {
        return intervals.stream().mapToDouble(interval -> interval.acceleration() * interval.seconds()).sum();
    }

    // Integrate velocity gained during a burn, starting after the preceding burn's delta-v has been spent.
    Burn burn(double startDeltaV, double deltaV) {
        var offset = startDeltaV;
        var remaining = deltaV;
        var gained = 0d;
        var seconds = 0d;
        var distance = 0d;
        var endAcceleration = intervals.getLast().acceleration();
        for (var interval : intervals) {
            var available = interval.acceleration() * interval.seconds();
            var skipped = Math.min(offset, available);
            offset -= skipped;
            var used = Math.min(remaining, available - skipped);
            if (used <= 0) continue;
            var duration = used / interval.acceleration();
            distance += (gained + used * 0.5) * duration;
            seconds += duration;
            gained += used;
            remaining -= used;
            endAcceleration = interval.acceleration();
            if (remaining <= 0) break;
        }
        // Search iterates may exceed the fuel budget. Extend the last rate for the equations only;
        // the transfer solver rejects any resulting plan which exceeds the actual budget.
        if (remaining > 0) {
            endAcceleration = intervals.getLast().acceleration();
            var duration = remaining / endAcceleration;
            distance += (gained + remaining * 0.5) * duration;
            seconds += duration;
        }
        return new Burn(seconds, distance, endAcceleration);
    }

    public record Interval(double acceleration, double seconds) {
        public Interval {
            if (!(acceleration > 0) || !Double.isFinite(acceleration)
                    || !(seconds > 0) || !Double.isFinite(seconds)) {
                throw new IllegalArgumentException("Burn intervals need finite positive acceleration and duration");
            }
        }
    }

    record Burn(double seconds, double distance, double endAcceleration) {
    }
}
