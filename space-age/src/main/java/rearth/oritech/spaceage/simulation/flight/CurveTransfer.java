package rearth.oritech.spaceage.simulation.flight;

/**
 * Full-power burn/coast/burn timing along a curve. Coasting does not spend engine time, but the leg's averaged
 * gravity keeps acting on it, so the whole leg conserves energy against the gravitational potential.
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
        if (profile.intervals().isEmpty()) {
            if (!(initial > 0) || initial > speedLimit) return null;
            var end = coastSpeed(initial, gravity, distance);
            // Without engines a climb the launch speed can't make stalls, and only free arrival can be exact.
            if (end < 0 || !freeArrival && Math.abs(end - arrival) >= 1e-7) return null;
            return new Plan(0, coastSeconds(initial, end, distance), 0, 1, 1);
        }
        var minimum = Math.max(Math.min(initial, speedLimit), freeArrival ? 0 : arrival);
        var totalBurnTime = profile.intervals().stream().mapToDouble(RocketBurnProfile.Interval::seconds).sum();
        var maximum = Math.min(speedLimit, minimum + profile.deltaV() * engineEfficiency
                + Math.max(0, gravity) * totalBurnTime);
        var lowest = attempt(minimum, distance, initial, arrival, freeArrival, profile,
                engineEfficiency, gravity, brakingCredit);
        if (lowest.fit == Fit.LONG) return null;
        var best = lowest.plan;
        // The fastest flight uses the highest peak which still fits. Peaks too slow to finish an uphill coast
        // lie below that one, so they also move the search upwards.
        for (int iteration = 0; iteration < 80; iteration++) {
            var middle = (minimum + maximum) * .5;
            var attempt = attempt(middle, distance, initial, arrival, freeArrival, profile,
                    engineEfficiency, gravity, brakingCredit);
            if (attempt.fit == Fit.LONG) maximum = middle;
            else {
                minimum = middle;
                if (attempt.fit == Fit.FITS) best = attempt.plan;
            }
        }
        return best;
    }

    /**
     * Places the coast between the two burns for one peak speed. Gravity changes the coasting speed and so the
     * final braking burn.
     */
    private static Attempt attempt(double peak, double distance, double initial, double arrival, boolean freeArrival,
                                   RocketBurnProfile profile, double efficiency, double gravity, double brakingCredit) {

        var candidate = candidate(peak, initial, arrival, freeArrival, profile, efficiency, gravity, brakingCredit);
        if (candidate == null || candidate.firstDistance > distance + 1e-6) return Attempt.LONG;
        if (peak <= 1e-9) return Attempt.SLOW;
        var direction = peak < initial ? -1 : 1;
        var remaining = Math.max(0, distance - candidate.firstDistance);
        var end = coastSpeed(peak, gravity, remaining);
        if (freeArrival) {
            if (end < 0) return Attempt.SLOW;
            return new Attempt(Fit.FITS, new Plan(candidate.first.seconds, coastSeconds(peak, end, remaining), 0,
                    candidate.brakingFuelFactor, direction));
        }
        if (end < arrival) return Attempt.SLOW;
        // Coast plus braking distance grows with the coast length, whichever way gravity pulls. Braking away
        // speed gained while falling may exceed the remaining engine time.
        var elapsed = candidate.first.seconds * (direction < 0 ? candidate.brakingFuelFactor : 1);
        var low = 0d;
        var high = remaining;
        Burn last = null;
        var covered = 0d;
        for (int iteration = 0; iteration < 80; iteration++) {
            var middle = (low + high) * .5;
            var speed = coastSpeed(peak, gravity, middle);
            var braking = burn(profile, elapsed, Math.max(0, speed - arrival), efficiency, -gravity, candidate.brakingFuelFactor);
            if (braking == null) {
                // Uphill, a longer coast sheds speed and reduces the final burn's fuel demand.
                if (gravity < 0) low = middle;
                else high = middle;
                continue;
            }
            var total = middle + speed * braking.seconds - braking.distance;
            if (total <= remaining) {
                low = middle;
                last = braking;
                covered = total;
            } else high = middle;
        }
        if (last == null || remaining - covered > 1e-4) return Attempt.LONG;
        return new Attempt(Fit.FITS, new Plan(candidate.first.seconds, coastSeconds(peak, coastSpeed(peak, gravity, low), low),
                last.seconds, candidate.brakingFuelFactor, direction));
    }

    /** Speed after coasting a distance under constant gravity, negative if the craft stalls first. */
    static double coastSpeed(double speed, double gravity, double distance) {

        var squared = speed * speed + 2 * gravity * distance;
        return squared < 0 ? -1 : Math.sqrt(squared);
    }

    private static double coastSeconds(double start, double end, double distance) {

        return distance <= 0 ? 0 : 2 * distance / (start + end);
    }

    private static Candidate candidate(double peak, double initial, double arrival, boolean freeArrival,
                                       RocketBurnProfile profile, double efficiency,
                                       double gravity, double brakingCredit) {

        var brakingFuelFactor = freeArrival ? 1 : ParachuteLanding.fuelFactor(brakingCredit, Math.max(initial, peak) - arrival);
        var firstFactor = peak < initial ? brakingFuelFactor : 1;
        var first = burn(profile, 0, Math.abs(peak - initial), efficiency,
                peak < initial ? -gravity : gravity, firstFactor);
        if (first == null) return null;
        var firstDistance = initial * first.seconds + (peak < initial ? -first.distance : first.distance);
        return new Candidate(first, firstDistance, brakingFuelFactor);
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

    private record Candidate(Burn first, double firstDistance, double brakingFuelFactor) {

    }

    private enum Fit {
        /** Stalls on an uphill coast or arrives too slowly; a higher peak may fit. */
        SLOW,
        FITS,
        /** Needs more distance or engine time than available; only a lower peak may fit. */
        LONG
    }

    private record Attempt(Fit fit, Plan plan) {

        static final Attempt SLOW = new Attempt(Fit.SLOW, null);
        static final Attempt LONG = new Attempt(Fit.LONG, null);
    }
}
