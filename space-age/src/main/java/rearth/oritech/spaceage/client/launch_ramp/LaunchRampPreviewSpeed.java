package rearth.oritech.spaceage.client.launch_ramp;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampMenu;
import rearth.oritech.spaceage.client.SpaceAgeClientRecipes;
import rearth.oritech.spaceage.simulation.flight.LaunchSpeedRules;
import rearth.oritech.spaceage.simulation.mission.MissionState;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class LaunchRampPreviewSpeed {

    private LaunchRampPreviewSpeed() {
    }

    public static String number(long value) {

        return String.format(Locale.ROOT, "%,d", value);
    }

    public static String metersPerSecond(long particleSpeed) {

        return new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.ROOT))
                .format(LaunchSpeedRules.velocity(particleSpeed));
    }

    /**
     * Searching the window runs many previews, so it waits for plan edits to settle and runs off the render thread.
     * Keep the result on the menu so it survives returning to the launcher.
     */
    public static void tick(LaunchRampMenu menu) {

        var rocket = menu.getRocket();
        var snapshot = menu.getFlightPlannerSnapshot();
        if (rocket == null || snapshot == null || !snapshot.rocketId().equals(rocket.getRocketId())) return;
        var plan = LaunchSpeedRules.firstLeg(snapshot.resolvedPlan(menu.getDraftFlightPlan() == null ? snapshot.plan() : menu.getDraftFlightPlan()));
        var changed = !plan.equals(menu.speedWindowPlan) || menu.speedWindowPreviewRevision != menu.getPreviewRevision();
        if (changed) {
            menu.speedWindowKnown = false;
            menu.speedWindow = null;
            if (menu.speedWindowSearch != null) menu.speedWindowSearch.cancel(false);
            menu.speedWindowSearch = null;
            menu.speedWindowPlan = plan;
            menu.speedWindowPreviewRevision = menu.getPreviewRevision();
            menu.speedWindowDelay = 10;
            menu.speedWindowRevision++;
        }
        if (menu.speedWindowDelay > 0 && --menu.speedWindowDelay == 0) {
            var copy = MissionState.copyRocket(rocket);
            var recipes = List.copyOf(SpaceAgeClientRecipes.vacuum());
            menu.speedWindowSearch = CompletableFuture.supplyAsync(() -> LaunchSpeedRules.window(plan, copy, snapshot.objects(), recipes,
                    snapshot.stations()), Util.backgroundExecutor()).exceptionally(error -> null);
        }
        if (menu.speedWindowSearch == null || !menu.speedWindowSearch.isDone()) return;
        menu.speedWindow = menu.speedWindowSearch.join();
        menu.speedWindowKnown = true;
        menu.speedWindowSearch = null;
        menu.speedWindowRevision++;
    }

    /**
     * Short summary of the working particle speeds for the planner footer.
     */
    public static MutableComponent windowSummary(LaunchRampMenu menu) {

        var window = menu.speedWindow;
        if (menu.getRocket() == null) return Component.translatable("screen.oritech_space_age.launch_ramp.speed_no_design");
        var snapshot = menu.getFlightPlannerSnapshot();
        var plan = menu.getDraftFlightPlan();
        if (plan == null && snapshot != null) plan = snapshot.plan();
        if (plan != null && plan.root().actions().isEmpty())
            return Component.translatable("screen.oritech_space_age.launch_ramp.speed_no_route");
        if (!menu.speedWindowKnown)
            return Component.translatable("screen.oritech_space_age.launch_ramp.chip.computing");
        if (window == null)
            return Component.translatable("screen.oritech_space_age.launch_ramp.chip.unreachable").withStyle(ChatFormatting.DARK_RED);
        if (window.ranges().size() > 1) return Component.translatable("screen.oritech_space_age.launch_ramp.ranges",
                String.join(", ", window.ranges().stream().map(range -> range.max() == Long.MAX_VALUE
                        ? number(range.min()) + "+" : number(range.min()) + "-" + number(range.max())).toList()));
        if (window.min() == 0 && window.max() == Long.MAX_VALUE)
            return Component.translatable("screen.oritech_space_age.launch_ramp.chip.free");
        if (window.max() == Long.MAX_VALUE)
            return Component.translatable("screen.oritech_space_age.launch_ramp.chip.open", number(window.min()));
        return Component.translatable("screen.oritech_space_age.launch_ramp.chip.window", number(window.min()), number(window.max()));
    }

    public static @Nullable Component speedAdvice(LaunchRampMenu menu) {

        var window = menu.speedWindow;
        if (!menu.speedWindowKnown || window == null || menu.previewSpeed() > LaunchSpeedRules.sampleLimit()
                || window.contains(menu.previewSpeed())) return null;
        return Component.translatable("screen.oritech_space_age.launch_ramp.speed_advice", number(menu.previewSpeed()),
                number(window.nearest(menu.previewSpeed()))).withStyle(ChatFormatting.RED);
    }

    /**
     * How particle speed becomes launch speed, and what happens to cube-sats without a plan.
     */
    public static List<Component> speedRules() {

        return List.of(Component.translatable("screen.oritech_space_age.launch_ramp.conversion",
                        number(1000), number(Math.round(1000 * SpaceAgeConfig.launchSpeedFactor()))),
                Component.translatable("screen.oritech_space_age.launch_ramp.sample_limit", number(LaunchSpeedRules.sampleLimit())),
                Component.translatable("screen.oritech_space_age.launch_ramp.orbit",
                        number(LaunchSpeedRules.orbitParticleSpeed()), number(Math.round(LaunchSpeedRules.orbitVelocity()))));
    }
}
