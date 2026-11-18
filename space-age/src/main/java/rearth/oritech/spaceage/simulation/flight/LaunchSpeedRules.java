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

import java.util.ArrayList;
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

    /**
     * Launch velocity in m/s for a particle speed.
     */
    public static double velocity(long particleSpeed) {

        return particleSpeed * SpaceAgeConfig.launchSpeedFactor();
    }

    public static long particleSpeedForVelocity(double metersPerSecond) {

        var factor = SpaceAgeConfig.launchSpeedFactor();
        return factor > 0 ? Math.round(metersPerSecond / factor) : 0;
    }

    /**
     * Finite advisory search horizon. Higher speeds remain available for preview and launch.
     */
    public static long sampleLimit() {

        var factor = SpaceAgeConfig.launchSpeedFactor();
        return factor <= 0 ? 0 : (long) Math.ceil(1500 / factor);
    }

    /**
     * Launch velocity at which a coasting craft reaches the low Earth orbit band, using the same gravitational
     * potential as the transfer solver.
     */
    public static double orbitVelocity() {

        if (SpaceAgeConfig.orbitSpeed() > 0) return SpaceAgeConfig.orbitSpeed();
        var radius = SpaceObjects.EARTH_RADIUS;
        var altitude = SpaceSimulation.OrbitBand.LOW.altitude();
        return Math.sqrt(2 * SpaceBalance.STANDARD_GRAVITY * radius * altitude / (radius + altitude));
    }

    public static long orbitParticleSpeed() {

        var factor = SpaceAgeConfig.launchSpeedFactor();
        return factor <= 0 ? Long.MAX_VALUE : (long) Math.ceil(orbitVelocity() / factor);
    }

    /**
     * Surface position at the ramp, already moving away from Earth.
     */
    public static MissionState.Position position(List<SpaceSimulation.SpaceObjectData> objects, BlockPos launch, long particleSpeed) {

        var earth = objects.stream().filter(object -> object.id().equals(SpaceObjects.EARTH_ID)).findFirst().orElseThrow();
        var angle = SpaceBalance.surfaceAngle(launch.getX(), launch.getZ());
        return MissionController.launchPosition(earth.x() + Math.cos(angle) * earth.radius(), earth.y() + Math.sin(angle) * earth.radius(),
                launch, velocity(particleSpeed));
    }

    public static boolean valid(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket, List<SpaceSimulation.SpaceObjectData> objects,
                                long particleSpeed, Collection<RecipeHolder<VacuumRecipe>> recipes, List<DockingTarget> stations) {

        var first = plan.root().actions().isEmpty() ? null : plan.root().actions().getFirst();
        if (first == null || first.type() != SpaceSimulation.ActionType.NAVIGATE_TO) return false;
        var position = position(objects, rocket.getLaunchPosition(), particleSpeed);
        var predicted = RocketFlightPathCalculator.calculateFrom(rocket, objects, firstLeg(plan), position, recipes, stations);
        return !predicted.paths().isEmpty() && !predicted.paths().getFirst().terminalState().preventsLaunch();
    }

    /**
     * Only the initial navigation leg determines which particle speeds can launch the craft.
     */
    public static SpaceSimulation.FlightPlan firstLeg(SpaceSimulation.FlightPlan plan) {

        return plan.withBranches(List.of(plan.root().withActions(plan.root().actions().stream().limit(1).toList())));
    }

    /**
     * All sampled ranges for which the first leg flies. Narrow ranges between samples may be missed.
     */
    public static Window window(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket, List<SpaceSimulation.SpaceObjectData> objects,
                                Collection<RecipeHolder<VacuumRecipe>> recipes, List<DockingTarget> stations) {

        return window(sampleLimit(), speed -> valid(plan, rocket, objects, speed, recipes, stations));
    }

    static Window window(long sampleLimit, LongPredicate valid) {

        var samples = new boolean[WINDOW_SAMPLES + 1];
        for (int i = 0; i <= WINDOW_SAMPLES; i++) samples[i] = valid.test(sampleLimit * i / WINDOW_SAMPLES);
        var ranges = new ArrayList<Range>();
        for (int first = 0; first <= WINDOW_SAMPLES; first++) {
            if (!samples[first]) continue;
            var last = first;
            while (last < WINDOW_SAMPLES && samples[last + 1]) last++;
            var min = sampleLimit * first / WINDOW_SAMPLES;
            if (first > 0) {
                var low = sampleLimit * (first - 1) / WINDOW_SAMPLES;
                for (int i = 0; i < WINDOW_REFINEMENTS && min - low > 1; i++) {
                    var middle = (low + min) / 2;
                    if (valid.test(middle)) min = middle;
                    else low = middle;
                }
            }
            var max = sampleLimit;
            if (last < WINDOW_SAMPLES) {
                max = sampleLimit * last / WINDOW_SAMPLES;
                var high = sampleLimit * (last + 1) / WINDOW_SAMPLES;
                for (int i = 0; i < WINDOW_REFINEMENTS && high - max > 1; i++) {
                    var middle = (max + high) / 2;
                    if (valid.test(middle)) max = middle;
                    else high = middle;
                }
            }
            ranges.add(new Range(min, max));
            first = last;
        }
        return ranges.isEmpty() ? null : new Window(ranges.getFirst().min(), ranges.getFirst().max(), List.copyOf(ranges));
    }

    /**
     * @param max highest sampled working particle speed; not a launch-speed limit
     */
    public record Window(long min, long max, List<Range> ranges) {

        public Window(long min, long max) {

            this(min, max, List.of(new Range(min, max)));
        }

        public boolean contains(long speed) {

            return ranges.stream().anyMatch(range -> speed >= range.min() && speed <= range.max());
        }

        public long nearest(long speed) {

            var nearest = min;
            for (var range : ranges) {
                var candidate = Math.clamp(speed, range.min(), range.max());
                if (Math.abs(candidate - speed) < Math.abs(nearest - speed)) nearest = candidate;
            }
            return nearest;
        }

    }

    public record Range(long min, long max) {
    }
}
