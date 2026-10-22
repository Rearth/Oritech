package rearth.oritech.spaceage.simulation.flight;

import java.util.List;

/**
 * Future full-power intervals, in engine-on time. Coasting does not advance this profile.
 */
public record RocketBurnProfile(List<Interval> intervals) {

    public RocketBurnProfile {

        intervals = List.copyOf(intervals);
    }

    public double deltaV() {

        return intervals.stream().mapToDouble(interval -> interval.acceleration() * interval.seconds()).sum();
    }

    public record Interval(double acceleration, double seconds) {

        public Interval {

            if (!(acceleration > 0) || !Double.isFinite(acceleration)
                    || !(seconds > 0) || !Double.isFinite(seconds)) {
                throw new IllegalArgumentException("Burn intervals need finite positive acceleration and duration");
            }
        }
    }

}
