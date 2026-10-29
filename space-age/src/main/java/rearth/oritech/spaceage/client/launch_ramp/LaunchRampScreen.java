package rearth.oritech.spaceage.client.launch_ramp;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ItemSlotWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.SpaceAgeConfig;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlock;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampBlockEntity.Outcome;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampMenu;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.spaceage.client.SpaceAgeClientRecipes;
import rearth.oritech.spaceage.client.assembler.RocketAssemblerClientController;
import rearth.oritech.spaceage.client.assembler.RocketAssemblerScreen;
import rearth.oritech.spaceage.client.planner.RocketFlightPlannerScreen;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.flight.LaunchSpeedRules;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * First tab of the launch ramp: the pattern slot, the saved plan, the particle speeds the plan needs and the last launch.
 */
public class LaunchRampScreen extends OritechWidgetScreen<LaunchRampMenu> {

    private static final int WINDOW_PADDING = 6;
    private static final int INFO_X = 206;
    private final Inventory screenInventory;
    private final Component screenTitle;
    private int panelWidth;
    private int panelHeight;
    private int statusRevision = -1;
    private int windowRevision = -1;
    private int previewRevision = -1;
    private EditBox speedField;
    private String speedText;

    public LaunchRampScreen(LaunchRampMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 0, 0);
        screenInventory = inventory;
        screenTitle = title;
        speedText = menu.expectedSpeed < 0 ? "" : Long.toString(menu.expectedSpeed);
    }

    public static String number(long value) {

        return String.format(Locale.ROOT, "%,d", value);
    }

    /**
     * Searching the window runs many previews, so it waits for plan edits to settle and runs off the render thread.
     * Every ramp tab ticks it, so the result is shared through the menu.
     */
    public static void tickSpeedWindow(LaunchRampMenu menu) {

        var rocket = menu.getRocket();
        var snapshot = menu.getFlightPlannerSnapshot();
        if (rocket == null || snapshot == null || !snapshot.rocketId().equals(rocket.getRocketId())) return;
        var plan = LaunchSpeedRules.firstLeg(menu.getDraftFlightPlan() == null ? snapshot.plan() : menu.getDraftFlightPlan());
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

    /** Short summary of the working particle speeds for the planner footer. */
    public static MutableComponent windowSummary(LaunchRampMenu menu) {

        var window = menu.speedWindow;
        if (!menu.speedWindowKnown) return Component.translatable("screen.oritech_space_age.launch_ramp.chip.computing");
        if (window == null) return Component.translatable("screen.oritech_space_age.launch_ramp.chip.unreachable").withStyle(ChatFormatting.DARK_RED);
        if (window.ranges().size() > 1) return Component.translatable("screen.oritech_space_age.launch_ramp.ranges",
                String.join(", ", window.ranges().stream().map(range -> range.max() == Long.MAX_VALUE
                        ? number(range.min()) + "+" : number(range.min()) + "-" + number(range.max())).toList()));
        if (window.min() == 0 && window.max() == Long.MAX_VALUE) return Component.translatable("screen.oritech_space_age.launch_ramp.chip.free");
        if (window.max() == Long.MAX_VALUE) return Component.translatable("screen.oritech_space_age.launch_ramp.chip.open", number(window.min()));
        return Component.translatable("screen.oritech_space_age.launch_ramp.chip.window", number(window.min()), number(window.max()));
    }

    /** How particle speed becomes launch speed, and what happens to cube-sats without a plan. */
    public static List<Component> speedRules() {

        return List.of(Component.translatable("screen.oritech_space_age.launch_ramp.conversion",
                        number(1000), number(Math.round(1000 * SpaceAgeConfig.launchSpeedFactor())), number(SpaceAgeConfig.launchSpeedCap())),
                Component.translatable("screen.oritech_space_age.launch_ramp.orbit",
                        number(LaunchSpeedRules.orbitParticleSpeed()), number(Math.round(LaunchSpeedRules.orbitVelocity()))));
    }

    @Override
    protected void buildComponents() {

        menu.slotsVisible = true;
        speedField = null;
        panelWidth = width - WINDOW_PADDING * 2;
        panelHeight = height - WINDOW_PADDING * 2;
        setPanelSize(panelWidth, panelHeight);
        statusRevision = menu.statusRevision;
        windowRevision = menu.speedWindowRevision;
        previewRevision = menu.getPreviewRevision();
        addComponent(new SurfaceWidget(0, 0, panelWidth, panelHeight, OritechSurface.PANEL));
        SpaceAgeButtons.rampTabs(0, () -> {
        }, this::openRocket, this::openPlanner).forEach(this::addComponent);

        addComponent(new SurfaceWidget(12, 39, 186, 160, OritechSurface.PANEL_INSET));
        addComponent(new LabelWidget(21, 47, 170, title("pattern")));
        menu.slots.forEach(slot -> addComponent(new ItemSlotWidget(slot.x, slot.y)));
        addComponent(new LabelWidget(LaunchRampMenu.PATTERN_X + 22, LaunchRampMenu.PATTERN_Y - 4, 150, 26, patternText()).withWrap(true));
        addComponent(new LabelWidget(21, LaunchRampMenu.INVENTORY_Y - 12, 170, Component.translatable("container.inventory")));

        var infoWidth = panelWidth - INFO_X - 12;
        addComponent(new SurfaceWidget(INFO_X, 39, infoWidth, panelHeight - 51, OritechSurface.PANEL_INSET));
        var x = INFO_X + 10;
        var width = infoWidth - 20;
        var y = addSection(x, 47, width, "plan", List.of(planText()));
        var speeds = new ArrayList<Component>();
        speeds.add(windowText());
        speeds.addAll(speedRules());
        y = addSection(x, y, width, "speed", speeds);
        y = addSection(x, y, width, "expected", List.of(Component.translatable("screen.oritech_space_age.launch_ramp.expected_help")));
        speedField = addRenderableWidget(new EditBox(font, leftPos + x, topPos + y - 4, 90, 18, title("expected")));
        speedField.setHint(Component.literal(number(menu.previewSpeed())));
        speedField.setValue(speedText);
        speedField.setResponder(this::setExpectedSpeed);
        var lowest = SpaceAgeButtons.panel(x + 96, y - 4, 110, 18, Component.translatable("screen.oritech_space_age.launch_ramp.use_lowest"), ignored -> {
            speedText = "";
            setExpectedSpeed("");
            rebuildComponents();
        });
        addComponent(lowest);
        if (menu.status.lastSpeed() > 0) addComponent(SpaceAgeButtons.panel(x + 210, y - 4, 110, 18,
                Component.translatable("screen.oritech_space_age.launch_ramp.use_last"), ignored -> {
                    speedText = Long.toString(menu.status.lastSpeed());
                    setExpectedSpeed(speedText);
                    rebuildComponents();
                }));
        addSection(x, y + 24, width, "last", List.of(lastLaunchText()));
    }

    private int addSection(int x, int y, int width, String key, List<Component> lines) {

        addComponent(new LabelWidget(x, y, width, title(key)));
        y += 13;
        for (var line : lines) {
            var height = font.split(line, width).size() * font.lineHeight;
            addComponent(new LabelWidget(x, y, width, height, line).withWrap(true));
            y += height + 2;
        }
        return y + 8;
    }

    private static MutableComponent title(String key) {

        return Component.translatable("screen.oritech_space_age.launch_ramp." + key).withStyle(ChatFormatting.BOLD);
    }

    private void setExpectedSpeed(String text) {

        speedText = text;
        try {
            menu.expectedSpeed = text.isBlank() ? -1 : Math.max(0, Long.parseLong(text.strip().replace(",", "")));
        } catch (NumberFormatException ignored) {
            menu.expectedSpeed = -1;
        }
    }

    private Component patternText() {

        var data = CubeSatBlock.data(menu.pattern());
        if (data == null) return Component.translatable("screen.oritech_space_age.launch_ramp.pattern_empty");
        var performance = RocketPerformanceCalculator.calculate(data.unfold(UUID.randomUUID(), null));
        return Component.translatable("screen.oritech_space_age.launch_ramp.pattern_summary", menu.pattern().getHoverName(),
                data.blockCount(), number(Math.round(performance.availableDeltaVMetersPerSecond())));
    }

    private Component planText() {

        var status = menu.status;
        if (menu.pattern().isEmpty()) return Component.translatable("screen.oritech_space_age.launch_ramp.plan_no_pattern");
        if (status.owner().isEmpty()) return Component.translatable("screen.oritech_space_age.launch_ramp.plan_none");
        return Component.translatable(status.planMatches() ? "screen.oritech_space_age.launch_ramp.plan_saved"
                : "screen.oritech_space_age.launch_ramp.plan_other", status.owner());
    }

    private Component windowText() {

        var window = menu.speedWindow;
        if (menu.pattern().isEmpty() || !menu.speedWindowKnown) return windowSummary(menu);
        if (window == null) return Component.translatable("screen.oritech_space_age.launch_ramp.window_unreachable", number(LaunchSpeedRules.maxUsefulSpeed()));
        if (window.ranges().size() > 1) return Component.translatable("screen.oritech_space_age.launch_ramp.ranges",
                String.join(", ", window.ranges().stream().map(range -> range.max() == Long.MAX_VALUE
                        ? number(range.min()) + "+" : number(range.min()) + "-" + number(range.max())).toList()));
        if (window.min() == 0 && window.max() == Long.MAX_VALUE) return Component.translatable("screen.oritech_space_age.launch_ramp.window_free");
        if (window.max() == Long.MAX_VALUE) return Component.translatable("screen.oritech_space_age.launch_ramp.window_open", number(window.min()));
        return Component.translatable("screen.oritech_space_age.launch_ramp.window", number(window.min()), number(window.max()));
    }

    private Component lastLaunchText() {

        var status = menu.status;
        var outcome = Outcome.values()[status.outcome()];
        if (outcome == Outcome.NONE) return Component.translatable("screen.oritech_space_age.launch_ramp.last_none");
        var result = Component.translatable("screen.oritech_space_age.launch_ramp.outcome." + outcome.name().toLowerCase(Locale.ROOT));
        if (outcome == Outcome.LOST) return Component.translatable("screen.oritech_space_age.launch_ramp.last_lost", number(status.lastSpeed()), result);
        return Component.translatable("screen.oritech_space_age.launch_ramp.last_launch", number(status.lastSpeed()),
                number(Math.round(LaunchSpeedRules.velocity(status.lastSpeed()))), result);
    }

    private void openRocket() {

        Minecraft.getInstance().setScreen(new RocketAssemblerScreen(menu, screenInventory, screenTitle));
    }

    private void openPlanner() {

        Minecraft.getInstance().setScreen(new RocketFlightPlannerScreen(menu, screenInventory, screenTitle));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        if (speedField != null && speedField.isMouseOver(event.x(), event.y())) {
            setFocused(speedField);
            return speedField.mouseClicked(event, doubleClick);
        }
        setFocused(null);
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        if (!event.isEscape() && speedField != null && speedField.isFocused()) {
            // Container screens normally interpret E as the inventory shortcut before charTyped reaches a field.
            speedField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        components.forEach(UIComponent::tick);
        tickSpeedWindow(menu);
        if (statusRevision != menu.statusRevision || windowRevision != menu.speedWindowRevision
                || previewRevision != menu.getPreviewRevision()) rebuildComponents();
    }

    @Override
    public void onClose() {

        RocketAssemblerClientController.submitFlightPlanIfDirty(menu);
        super.onClose();
    }

    @Override
    public boolean shouldCreateTitle() {

        return false;
    }

    @Override
    public BlockState getTitleState() {

        return SpaceAgeBlocks.LAUNCH_RAMP.get().defaultBlockState();
    }
}
