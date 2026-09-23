package rearth.oritech.spaceage.simulation;

/** Scalar burn/coast/burn timing along a curve, with constant endpoint gravity projections. */
final class CurveTransfer {

    record Plan(double firstSeconds, double coastSeconds, double lastSeconds) { }
    private record Burn(double seconds, double distance, double gained) { }
    private record Candidate(double peak, Burn first, Burn last, double distance) { }

    private CurveTransfer() { }

    static Plan solve(double distance, double initial, double arrival, boolean freeArrival,
                      double speedLimit, RocketBurnProfile profile, double engineEfficiency,
                      double departureGravity, double arrivalGravity) {
        if (distance <= 0 || profile.intervals().isEmpty() || !(speedLimit > 0)) return null;
        var minimum = Math.max(initial, freeArrival ? 0 : arrival);
        if (minimum > speedLimit) return null;
        var totalBurnTime = profile.intervals().stream().mapToDouble(RocketBurnProfile.Interval::seconds).sum();
        var maximum = Math.min(speedLimit, minimum + profile.deltaV() * engineEfficiency
                + Math.max(0, departureGravity) * totalBurnTime);
        Candidate best = candidate(minimum, initial, arrival, freeArrival, profile,
                engineEfficiency, departureGravity, arrivalGravity);
        if (best == null || best.distance > distance + 1e-6) return null;
        for (int iteration = 0; iteration < 80; iteration++) {
            var middle = (minimum + maximum) * .5;
            var candidate = candidate(middle, initial, arrival, freeArrival, profile,
                    engineEfficiency, departureGravity, arrivalGravity);
            if (candidate != null && candidate.distance <= distance) {
                minimum = middle;
                best = candidate;
            } else maximum = middle;
        }
        if (best.peak <= 1e-9) return null;
        var coast = Math.max(0, (distance - best.distance) / best.peak);
        return new Plan(best.first.seconds, coast, best.last.seconds);
    }

    private static Candidate candidate(double peak, double initial, double arrival, boolean freeArrival,
                                       RocketBurnProfile profile, double efficiency,
                                       double departureGravity, double arrivalGravity) {
        var first = burn(profile, 0, Math.max(0, peak - initial), efficiency, departureGravity);
        if (first == null) return null;
        var last = freeArrival ? new Burn(0, 0, 0)
                : burn(profile, first.seconds, Math.max(0, peak - arrival), efficiency, -arrivalGravity);
        if (last == null) return null;
        var distance = initial * first.seconds + first.distance
                + peak * last.seconds - last.distance;
        return new Candidate(peak, first, last, distance);
    }

    /** Integrates full-power engine time after an earlier burn, including stage-rate changes. */
    private static Burn burn(RocketBurnProfile profile, double elapsed, double wanted,
                             double efficiency, double gravity) {
        if (wanted <= 1e-9) return new Burn(0, 0, 0);
        var remainingOffset = elapsed;
        var gained = 0d;
        var seconds = 0d;
        var distance = 0d;
        for (var interval : profile.intervals()) {
            var availableSeconds = interval.seconds();
            var skipped = Math.min(remainingOffset, availableSeconds);
            remainingOffset -= skipped;
            availableSeconds -= skipped;
            if (availableSeconds <= 1e-9) continue;
            var acceleration = interval.acceleration() * efficiency + gravity;
            // A stage that cannot overcome gravity cannot advance this prescribed curve.
            if (acceleration <= 1e-9) return null;
            var duration = Math.min(availableSeconds, (wanted - gained) / acceleration);
            distance += gained * duration + acceleration * duration * duration * .5;
            gained += acceleration * duration;
            seconds += duration;
            if (gained >= wanted - 1e-8) return new Burn(seconds, distance, gained);
        }
        return null;
    }
}
