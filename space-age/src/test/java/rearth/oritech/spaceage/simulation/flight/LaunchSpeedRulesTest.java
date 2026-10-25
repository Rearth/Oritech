package rearth.oritech.spaceage.simulation.flight;

import org.junit.jupiter.api.Test;
import rearth.oritech.spaceage.SpaceAgeConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchSpeedRulesTest {

    @Test
    void windowFindsBothBoundsWhenTooFastAlsoFails() {

        var window = LaunchSpeedRules.window(30_000, speed -> speed >= 6_200 && speed <= 11_800);
        assertTrue(Math.abs(window.min() - 6_200) <= 2, "lower bound " + window.min());
        assertTrue(Math.abs(window.max() - 11_800) <= 2, "upper bound " + window.max());
    }

    @Test
    void windowReportsPlansWithoutSpeedAndUnreachablePlans() {

        var free = LaunchSpeedRules.window(30_000, speed -> true);
        assertEquals(0, free.min());
        assertEquals(Long.MAX_VALUE, free.max());
        assertNull(LaunchSpeedRules.window(30_000, speed -> false));
        var open = LaunchSpeedRules.window(30_000, speed -> speed >= 9_000);
        assertEquals(Long.MAX_VALUE, open.max());
    }

    @Test
    void launchVelocityIsCappedAndOrbitNeedsAReachableSpeed() {

        assertEquals(SpaceAgeConfig.launchSpeedCap(), LaunchSpeedRules.velocity(Long.MAX_VALUE / 2));
        assertTrue(LaunchSpeedRules.orbitVelocity() > 0);
        assertTrue(LaunchSpeedRules.velocity(LaunchSpeedRules.orbitParticleSpeed()) >= LaunchSpeedRules.orbitVelocity());
        assertTrue(LaunchSpeedRules.orbitParticleSpeed() <= LaunchSpeedRules.maxUsefulSpeed(), "the default cap reaches orbit");
    }

    @Test
    void finerSamplingFindsAWindowBetweenTheOriginalCoarseSamples() {

        var window = LaunchSpeedRules.window(30_000, speed -> speed >= 6_500 && speed <= 6_600);
        assertEquals(6_500, window.min());
        assertEquals(6_600, window.max());
    }

}
