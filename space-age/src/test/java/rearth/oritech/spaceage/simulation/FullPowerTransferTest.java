package rearth.oritech.spaceage.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Dependency-free numerical regression tests; run with :space-age:transferTest. */
public final class FullPowerTransferTest {
    public static void main(String[] args) {
        var longTrip = check(8_000_000, 0, 0, 0, 0, 0, false, 10, 1000, Double.POSITIVE_INFINITY);
        close(longTrip.firstSeconds(), 50, 1e-5, "full fuel acceleration burn");
        close(longTrip.coastSeconds(), 15950, 1e-3, "long trip coast");
        var economical = check(8_000_000, 0, 0, 0, 0, 0, false, 10, 1000, 100);
        close(economical.firstSeconds(), 10, 1e-5, "capped burn");
        require(economical.duration() > longTrip.duration(), "economical route takes longer");
        var shortTrip = check(1000, 0, 0, 0, 0, 0, false, 10, 1000, Double.POSITIVE_INFINITY);
        close(shortTrip.duration(), 20, 1e-3, "short triangular flight");
        var intercept = check(8_000_000, 0, 0, 0, 0, 0, true, 10, 1000, Double.POSITIVE_INFINITY);
        close(intercept.firstSeconds(), 100, 1e-5, "maximum arrival burn");
        require(intercept.coastSeconds() > 0 && intercept.lastSeconds() == 0, "coast after fuel exhaustion");
        check(8_000_000, 0, 0, 0, 0, 0, true, 10, 1000, 100);
        check(800_000, 200_000, 250, -50, 0, 0, false, 3, 2000, 500);
        check(-800_000, -200_000, 250, -50, -100, 0, false, 3, 2000, 500);
        check(800_000, 200_000, 250, -50, 0, 0, true, 3, 2000, 500);
        check(10_000, 0, 500, 0, 0, 0, false, 10, 1500, 100);
        var ballistic = check(100_000, 0, 1000, 0, 0, 0, true, 0, 0, 1000);
        close(ballistic.coastSeconds(), 100, 1e-6, "unpowered inherited velocity");
        require(FullPowerTransfer.solve(1e6, 0, 0, 0, 200, 0, false, 10, 1000, 100, 1.2e6) == null,
                "arrival above cap must fail");
        require(FullPowerTransfer.solve(1e6, 0, 0, 0, 200, 0, false, 10, 100, 500, 1.2e6) == null,
                "insufficient braking/arrival fuel must fail");
        require(FullPowerTransfer.solve(1e9, 0, 0, 0, 0, 0, false, 10, 1000, 1, 100) == null,
                "time limit must fail");
        var weakerFinalStage = new RocketBurnProfile(List.of(
                new RocketBurnProfile.Interval(8, 187.5), new RocketBurnProfile.Interval(2.5, 2000)));
        var staged = check(8_000_000, 0, 0, 0, 0, 0, false, weakerFinalStage, 1000);
        close(staged.firstSeconds(), 125, 1e-5, "first burn at speed cap");
        close(staged.lastSeconds(), 262.5, 1e-5, "braking spans both engine rates");
        close(staged.coastSeconds(), 7840.625, 1e-3, "coast reserves the longer braking distance");
        var strongerFinalStage = new RocketBurnProfile(List.of(
                new RocketBurnProfile.Interval(2.5, 600), new RocketBurnProfile.Interval(8, 1000)));
        check(8_000_000, 0, 0, 0, 0, 0, false, strongerFinalStage, 1000);
        var severalStages = new RocketBurnProfile(List.of(new RocketBurnProfile.Interval(8, 50),
                new RocketBurnProfile.Interval(2.5, 300), new RocketBurnProfile.Interval(12, 100)));
        check(8_000_000, 0, 0, 0, 0, 0, false, severalStages, 1000);
        check(8_000_000, 0, 0, 0, 0, 0, false, severalStages, 400);
        check(8_000_000, 0, 0, 0, 0, 0, true, severalStages, Double.POSITIVE_INFINITY);
        check(800_000, 200_000, 250, -50, 0, 0, false, severalStages, 500);
        check(-800_000, -200_000, 250, -50, -100, 0, false, severalStages, 500);
        check(800_000, 200_000, 250, -50, 100, 100, false, severalStages, 500);

        var random = new Random(721);
        for (var example = 0; example < 200; example++) {
            var intervals = new ArrayList<RocketBurnProfile.Interval>();
            for (var stage = 0; stage < 2 + example % 4; stage++) {
                intervals.add(new RocketBurnProfile.Interval(1 + random.nextDouble() * 30,
                        20 + random.nextDouble() * 400));
            }
            var profile = new RocketBurnProfile(intervals);
            var cap = profile.deltaV() * (0.1 + random.nextDouble() * 0.35);
            var angle = random.nextDouble() * Math.PI * 2;
            check(cap * 10_000 * Math.cos(angle), cap * 10_000 * Math.sin(angle),
                    0, 0, 0, 0, false, profile, cap);
        }
        System.out.println("Full-power transfer regressions passed");
    }

    private static FullPowerTransfer check(double dx, double dy, double vx, double vy,
                                           double fx, double fy, boolean free, double a, double budget, double cap) {
        var intervals = a > 0 && budget > 0 ? List.of(new RocketBurnProfile.Interval(a, budget / a))
                : List.<RocketBurnProfile.Interval>of();
        return check(dx, dy, vx, vy, fx, fy, free, new RocketBurnProfile(intervals), cap);
    }

    private static FullPowerTransfer check(double dx, double dy, double vx, double vy,
                                           double fx, double fy, boolean free, RocketBurnProfile profile, double cap) {
        var plan = FullPowerTransfer.solve(dx, dy, vx, vy, fx, fy, free, profile, cap, 1_200_000);
        require(plan != null, "expected feasible transfer for " + dx + ", " + dy);
        var x = 0d;
        var y = 0d;
        var durations = new double[]{plan.firstSeconds(), plan.coastSeconds(), plan.lastSeconds()};
        var directionsX = new double[]{plan.firstDirectionX(), 0, plan.lastDirectionX()};
        var directionsY = new double[]{plan.firstDirectionY(), 0, plan.lastDirectionY()};
        var spent = 0d;
        var intervalIndex = 0;
        var intervalElapsed = 0d;
        // Integrate by engine-on time independently of the solver's delta-v integrals.
        for (var phase = 0; phase < 3; phase++) {
            var remaining = durations[phase];
            require(remaining >= 0 && Double.isFinite(remaining), "finite nonnegative phase duration");
            if (phase == 1) {
                x += vx * remaining;
                y += vy * remaining;
                continue;
            }
            if (remaining > 0) close(Math.hypot(directionsX[phase], directionsY[phase]), 1, 1e-8, "full power direction");
            while (remaining > 1e-9) {
                if (intervalIndex >= profile.intervals().size() && remaining < 1e-6) break;
                require(intervalIndex < profile.intervals().size(), "no burning beyond available engine time");
                var interval = profile.intervals().get(intervalIndex);
                var t = Math.min(remaining, interval.seconds() - intervalElapsed);
                var ax = directionsX[phase] * interval.acceleration();
                var ay = directionsY[phase] * interval.acceleration();
                spent += interval.acceleration() * t;
                x += vx * t + ax * t * t / 2;
                y += vy * t + ay * t * t / 2;
                vx += ax * t;
                vy += ay * t;
                remaining -= t;
                intervalElapsed += t;
                if (intervalElapsed >= interval.seconds() - 1e-9) {
                    intervalIndex++;
                    intervalElapsed = 0;
                }
            }
            if (phase == 0) require(Math.hypot(vx, vy) <= cap + 1e-6, "cruise speed limit");
        }
        close(x, dx, 1e-3, "arrival x");
        close(y, dy, 1e-3, "arrival y");
        require(spent <= profile.deltaV() + 1e-6, "fuel conservation");
        if (!free) {
            close(vx, fx, 1e-6, "arrival velocity x");
            close(vy, fy, 1e-6, "arrival velocity y");
        }
        return plan;
    }

    private static void close(double actual, double expected, double tolerance, String message) {
        require(Math.abs(actual - expected) <= tolerance, message + ": " + actual + " != " + expected);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
