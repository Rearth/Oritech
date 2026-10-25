package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.mission.DockingTarget;
import rearth.oritech.spaceage.simulation.mission.MissionController;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;

import java.util.Collection;
import java.util.List;
import java.util.function.LongPredicate;

/**
 * Particle launch ramps turn the speed of an accelerator particle into the starting velocity of a cube-sat,
 * pointing straight away from Earth. Shared by the planner preview and the actual launch.
 */
public final class LaunchSpeedRules {

    // Coarse samples look for valid ranges, as validity is not monotonic in speed (too fast can't brake).
    private static final int WINDOW_SAMPLES = 96;
    private static final int WINDOW_REFINEMENTS = 16;

    private LaunchSpeedRules() {
    }

    /** Launch velocity in m/s for a particle speed. */
    public static double velocity(long particleSpeed) {

        return Math.min(particleSpeed * SpaceAgeConfig.launchSpeedFactor(), SpaceAgeConfig.launchSpeedCap());
    }

    /** The lowest particle speed reaching the velocity cap; faster particles don't launch any faster. */
    public static long maxUsefulSpeed() {

        var factor = SpaceAgeConfig.launchSpeedFactor();
        return factor <= 0 ? 0 : (long) Math.ceil(SpaceAgeConfig.launchSpeedCap() / factor);
    }

    /**
     * Launch velocity at which a coasting craft reaches the low Earth orbit band, using the same gravitational
     * potential as the transfer solver.
     */
    public static double orbitVelocity() {

        if (SpaceAgeConfig.orbitSpeed() > 0) return SpaceAgeConfig.orbitSpeed();
        var radius = SpaceObjects.EARTH_RADIUS;
        var altitude = SpaceSimulation.OrbitBand.LOW.altitude();
        return Math.sqrt(2 * RocketPerformanceCalculator.STANDARD_GRAVITY * radius * altitude / (radius + altitude));
    }

    public static long orbitParticleSpeed() {

        var factor = SpaceAgeConfig.launchSpeedFactor();
        return factor <= 0 ? Long.MAX_VALUE : (long) Math.ceil(orbitVelocity() / factor);
    }

    /** Surface position at the ramp, already moving away from Earth. */
    public static MissionState.Position position(List<SpaceSimulation.SpaceObjectData> objects, BlockPos launch, long particleSpeed) {

        var earth = objects.stream().filter(object -> object.id().equals(SpaceObjects.EARTH_ID)).findFirst().orElseThrow();
        var angle = SpaceBalance.surfaceAngle(launch.getX(), launch.getZ());
        return MissionController.launchPosition(earth.x() + Math.cos(angle) * earth.radius(), earth.y() + Math.sin(angle) * earth.radius(),
                launch, velocity(particleSpeed));
    }

    public static boolean valid(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket, List<SpaceSimulation.SpaceObjectData> objects,
                                long particleSpeed, Collection<RecipeHolder<VacuumRecipe>> recipes, List<DockingTarget> stations) {

        var position = position(objects, rocket.getLaunchPosition(), particleSpeed);
        return !plan.root().actions().isEmpty()
                && RocketFlightPlanRules.inspect(plan, rocket, objects, true, position, recipes, stations).valid();
    }

    /**
     * The lowest sampled range for which the whole plan validates. Narrow ranges between samples may be missed.
     */
    public static Window window(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket, List<SpaceSimulation.SpaceObjectData> objects,
                                Collection<RecipeHolder<VacuumRecipe>> recipes, List<DockingTarget> stations) {

        return window(maxUsefulSpeed(), speed -> valid(plan, rocket, objects, speed, recipes, stations));
    }

    static Window window(long cap, LongPredicate valid) {

        var samples = new boolean[WINDOW_SAMPLES + 1];
        for (int i = 0; i <= WINDOW_SAMPLES; i++) samples[i] = valid.test(cap * i / WINDOW_SAMPLES);
        var first = -1;
        for (int i = 0; i <= WINDOW_SAMPLES && first < 0; i++) if (samples[i]) first = i;
        if (first < 0) return null;
        var last = first;
        while (last < WINDOW_SAMPLES && samples[last + 1]) last++;

        var min = cap * first / WINDOW_SAMPLES;
        if (first > 0) {
            var low = cap * (first - 1) / WINDOW_SAMPLES;
            for (int i = 0; i < WINDOW_REFINEMENTS && min - low > 1; i++) {
                var middle = (low + min) / 2;
                if (valid.test(middle)) min = middle;
                else low = middle;
            }
        }
        var max = Long.MAX_VALUE;
        if (last < WINDOW_SAMPLES) {
            max = cap * last / WINDOW_SAMPLES;
            var high = cap * (last + 1) / WINDOW_SAMPLES;
            for (int i = 0; i < WINDOW_REFINEMENTS && high - max > 1; i++) {
                var middle = (max + high) / 2;
                if (valid.test(middle)) max = middle;
                else high = middle;
            }
        }
        return new Window(min, max);
    }

    /**
     * @param max Long.MAX_VALUE when the plan still works at the velocity cap
     */
    public record Window(long min, long max) {

    }
}
