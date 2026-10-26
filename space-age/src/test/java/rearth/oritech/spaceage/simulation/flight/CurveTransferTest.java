package rearth.oritech.spaceage.simulation.flight;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurveTransferTest {

    private static CurveTransfer.Plan check(double distance, double initial, double arrival, boolean free,
                                            RocketBurnProfile profile, double cap, double gravity, double credit) {

        var plan = CurveTransfer.solve(distance, initial, arrival, free, cap, profile, 1, gravity, credit);
        assertNotNull(plan);
        var x = 0d;
        var speed = initial;
        var stage = 0;
        var spent = 0d;
        var durations = new double[]{plan.firstSeconds(), plan.coastSeconds(), plan.lastSeconds()};
        var directions = new double[]{plan.firstDirection(), 0, -1};
        // Independently integrate each stage by actual engine time, including parachute savings.
        for (int phase = 0; phase < 3; phase++) {
            var remaining = durations[phase];
            assertTrue(remaining >= 0 && Double.isFinite(remaining));
            if (phase == 1) {
                x += speed * remaining + gravity * remaining * remaining / 2;
                speed += gravity * remaining;
                assertTrue(speed >= -1e-9, "coasting must not stall");
                continue;
            }
            var factor = directions[phase] < 0 ? plan.brakingFuelFactor() : 1;
            while (remaining > 1e-8) {
                assertTrue(stage < profile.intervals().size(), "burn must fit available stage budgets");
                var interval = profile.intervals().get(stage);
                var seconds = Math.min(remaining, (interval.seconds() - spent) / factor);
                var acceleration = directions[phase] * interval.acceleration() + gravity;
                x += speed * seconds + acceleration * seconds * seconds / 2;
                speed += acceleration * seconds;
                remaining -= seconds;
                spent += seconds * factor;
                if (spent >= interval.seconds() - 1e-9) {
                    stage++;
                    spent = 0;
                }
            }
            if (phase == 0) assertTrue(speed <= cap + 1e-5, "inherited excess speed must be shed before coasting");
        }
        assertEquals(distance, x, .001);
        if (!free) assertEquals(arrival, speed, 1e-6);
        return plan;
    }

    @Test
    void fullPowerBurnCoastAndBrakingRespectBudgetsAndCaps() {

        var profile = new RocketBurnProfile(List.of(new RocketBurnProfile.Interval(10, 100)));
        var longTrip = check(8_000_000, 0, 0, false, profile, Double.POSITIVE_INFINITY, 0, 0);
        assertEquals(50, longTrip.firstSeconds(), 1e-5);
        assertEquals(15_950, longTrip.coastSeconds(), .001);
        var capped = check(8_000_000, 0, 0, false, profile, 100, 0, 0);
        assertTrue(capped.duration() > longTrip.duration());
        assertEquals(20, check(1_000, 0, 0, false, profile, 1_000, 0, 0).duration(), 1e-6);
        assertEquals(0, check(8_000_000, 0, 0, true, profile, 1_000, 0, 0).lastSeconds());
        assertEquals(-1, check(100_000, 500, 0, false, profile, 100, 0, 0).firstDirection());
        check(100_000, 500, 0, true, profile, 100, .5, 0);
        assertNull(CurveTransfer.solve(100, 500, 0, false, 100, profile, 1, 0));
        assertNull(CurveTransfer.solve(1e6, 0, 200, false, 100, profile, 1, 0));
        assertNull(CurveTransfer.solve(1e6, 0, 2_000, false, 3_000, profile, 1, 0));
    }

    @Test
    void unpoweredCoastsAndParachuteBrakingUseTheSameSolver() {

        var empty = new RocketBurnProfile(List.of());
        assertEquals(100, check(100_000, 1_000, 0, true, empty, 1_000, 0, 0).coastSeconds());
        check(100_000, 1_000, 1_000, false, empty, 1_000, 0, 0);
        assertNull(CurveTransfer.solve(100_000, 1_000, 0, false, 1_000, empty, 1, 0));
        var staged = new RocketBurnProfile(List.of(new RocketBurnProfile.Interval(10, 10),
                new RocketBurnProfile.Interval(3, 1_000)));
        check(500_000, 1_500, 0, false, staged, 500, .3, 200);
        check(500_000, 0, 0, false, staged, 1_000, -.3, 200);
    }

    @Test
    void coastingConservesEnergyAgainstGravity() {

        var empty = new RocketBurnProfile(List.of());
        // An unpowered climb arrives with v^2 = v0^2 + 2 g d and stalls when it can't make it.
        var climb = check(10_000, 500, 0, true, empty, 1_000, -10, 0);
        assertEquals(2 * 10_000 / (500 + Math.sqrt(500 * 500 - 2 * 10 * 10_000)), climb.coastSeconds(), 1e-9);
        assertNull(CurveTransfer.solve(20_000, 500, 0, true, 1_000, empty, 1, -10));
        // Powered legs slow down during their coast and need a shorter braking burn.
        var profile = new RocketBurnProfile(List.of(new RocketBurnProfile.Interval(10, 100)));
        var flat = check(100_000, 0, 0, false, profile, 300, 0, 0);
        var uphill = check(100_000, 0, 0, false, profile, 300, -.2, 0);
        assertTrue(uphill.lastSeconds() < flat.lastSeconds());
        assertTrue(uphill.coastSeconds() > flat.coastSeconds());
        // A climb steeper than the coast can carry is infeasible even with engines left for braking.
        assertNull(CurveTransfer.solve(1_000_000, 0, 0, false, 300, profile, 1, -.2));
    }

    @Test
    void variedStageProfilesIntegrateToTheRequestedArrival() {

        var random = new Random(721);
        for (int example = 0; example < 200; example++) {
            var intervals = new ArrayList<RocketBurnProfile.Interval>();
            for (int stage = 0; stage < 2 + example % 4; stage++)
                intervals.add(new RocketBurnProfile.Interval(2 + random.nextDouble() * 30,
                        20 + random.nextDouble() * 400));
            var profile = new RocketBurnProfile(intervals);
            var cap = profile.deltaV() * .2;
            // Coasting gravity over the long leg may cost at most a quarter of the cap's kinetic energy.
            check(cap * 10_000, random.nextDouble() * cap * 2, random.nextDouble() * cap * .1,
                    example % 2 == 0, profile, cap, (random.nextDouble() - .5) * cap / 40_000, example % 3 == 0 ? 200 : 0);
        }
    }
}
