package rearth.oritech.spaceage.simulation.flight;

/**
 * Full-power burn/coast/burn timing along a curve. Coasting does not spend engine time.
 */
final class CurveTransfer {

    private CurveTransfer() {
    }

    static Plan solve(double distance, double initial, double arrival, boolean freeArrival,
                      double speedLimit, RocketBurnProfile profile, double engineEfficiency,
                      double gravity) {

        return solve(distance, initial, arrival, freeArrival, speedLimit, profile, engineEfficiency, gravity, 0);
    }

    static Plan solve(double distance, double initial, double arrival, boolean freeArrival,
                      double speedLimit, RocketBurnProfile profile, double engineEfficiency,
                      double gravity, double brakingCredit) {

        if (distance <= 0 || !(speedLimit > 0) || !freeArrival && arrival > speedLimit) return null;
        if (profile.intervals().isEmpty())
            return initial > 0 && initial <= speedLimit && (freeArrival || Math.abs(initial - arrival) < 1e-7)
                    ? new Plan(0, distance / initial, 0, 1, 1) : null;
        var minimum = Math.max(Math.min(initial, speedLimit), freeArrival ? 0 : arrival);
        var totalBurnTime = profile.intervals().stream().mapToDouble(RocketBurnProfile.Interval::seconds).sum();
        var maximum = Math.min(speedLimit, minimum + profile.deltaV() * engineEfficiency
                + Math.max(0, gravity) * totalBurnTime);
        Candidate best = candidate(minimum, initial, arrival, freeArrival, profile,
                engineEfficiency, gravity, brakingCredit);
        if (best == null || best.distance > distance + 1e-6) return null;
        for (int iteration = 0; iteration < 80; iteration++) {
            var middle = (minimum + maximum) * .5;
            var candidate = candidate(middle, initial, arrival, freeArrival, profile,
                    engineEfficiency, gravity, brakingCredit);
            if (candidate != null && candidate.distance <= distance) {
                minimum = middle;
                best = candidate;
            } else maximum = middle;
        }
        if (best.peak <= 1e-9) return null;
        var coast = Math.max(0, (distance - best.distance) / best.peak);
        return new Plan(best.first.seconds, coast, best.last.seconds, best.brakingFuelFactor,
                best.peak < initial ? -1 : 1);
    }

    private static Candidate candidate(double peak, double initial, double arrival, boolean freeArrival,
                                       RocketBurnProfile profile, double efficiency,
                                       double gravity, double brakingCredit) {

        var brakingFuelFactor = freeArrival ? 1 : ParachuteLanding.fuelFactor(brakingCredit, Math.max(initial, peak) - arrival);
        var firstFactor = peak < initial ? brakingFuelFactor : 1;
        var first = burn(profile, 0, Math.abs(peak - initial), efficiency,
                peak < initial ? -gravity : gravity, firstFactor);
        if (first == null) return null;
        var last = freeArrival ? new Burn(0, 0, 0)
                : burn(profile, first.seconds * firstFactor, Math.max(0, peak - arrival), efficiency, -gravity, brakingFuelFactor);
        if (last == null) return null;
        var distance = initial * first.seconds + (peak < initial ? -first.distance : first.distance)
                + peak * last.seconds - last.distance;
        return new Candidate(peak, first, last, distance, brakingFuelFactor);
    }

    static double brakingDistance(double initial, double arrival, RocketBurnProfile profile,
                                  double efficiency, double gravity, double brakingCredit) {

        var factor = ParachuteLanding.fuelFactor(brakingCredit, initial - arrival);
        var braking = burn(profile, 0, Math.max(0, initial - arrival), efficiency, -gravity, factor);
        return braking == null ? Double.POSITIVE_INFINITY : initial * braking.seconds - braking.distance;
    }

    /**
     * Integrates full-power engine time after an earlier burn, including stage-rate changes.
     */
    private static Burn burn(RocketBurnProfile profile, double elapsed, double wanted,
                             double efficiency, double gravity, double fuelFactor) {

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
            availableSeconds /= Math.max(1e-9, fuelFactor);
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

    record Plan(double firstSeconds, double coastSeconds, double lastSeconds, double brakingFuelFactor,
                double firstDirection) {

        double duration() {

            return firstSeconds + coastSeconds + lastSeconds;
        }

    }

    private record Burn(double seconds, double distance, double gained) {

    }

    private record Candidate(double peak, Burn first, Burn last, double distance, double brakingFuelFactor) {

    }
}
