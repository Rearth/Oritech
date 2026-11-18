package rearth.oritech.spaceage.client.launch_ramp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.BoxWidget;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.ItemSlotWidget;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlock;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampBlockEntity.Outcome;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampMenu;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.spaceage.client.assembler.RocketAssemblerClientController;
import rearth.oritech.spaceage.client.assembler.RocketAssemblerScreen;
import rearth.oritech.spaceage.client.planner.RocketFlightPlannerScreen;
import rearth.oritech.spaceage.client.starmap.RocketStarMapWidget;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlannerSnapshot;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LaunchRampScreen extends OritechWidgetScreen<LaunchRampMenu> {

    private static final int WIDTH = LaunchRampMenu.PANEL_WIDTH;
    private static final int HEIGHT = 213;
    private static final int SIDE_WIDTH = 96;
    private static final int PLAN_WIDTH = 112;
    private static final int PLAN_TOP = -20;
    private static final int PLAN_BOTTOM = 106;
    private static final int PANEL_CENTER = (PLAN_TOP + PLAN_BOTTOM) / 2;
    private static final int DESIGN_TOP = LaunchRampMenu.DESIGN_PANEL_TOP;
    private static final int DESIGN_BOTTOM = 2 * PANEL_CENTER - DESIGN_TOP;
    private static final int LAUNCH_TOP = PANEL_CENTER - 42;
    private static final int LAUNCH_BOTTOM = PANEL_CENTER + 42;
    private static final int DESIGN_X = 0;
    private static final int PLAN_X = 104;
    private static final int LAUNCH_X = 224;
    private static final int PANEL_PADDING = 6;
    private static final int TEXT_PADDING = 9;
    private static final int SIDE_TEXT_WIDTH = SIDE_WIDTH - TEXT_PADDING * 2;
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_X = (WIDTH - INVENTORY_WIDTH) / 2;
    private static final int INVENTORY_TOP = 110;
    private static final int DETAIL_TEXT = 0xFF5E6064;
    private static final int ROUTE_COLOR = 0xFF466A7D;
    private static final int MATCH_BACKGROUND = 0xFFB6CAB8;
    private static final int MATCH_TEXT = 0xFF335541;
    private final Inventory screenInventory;
    private int statusRevision;
    private int previewRevision;
    private int planRevision;
    private ItemStack displayedPattern = ItemStack.EMPTY;

    public LaunchRampScreen(LaunchRampMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, WIDTH, HEIGHT);
        screenInventory = inventory;
    }

    @Override
    protected void buildComponents() {

        topPos = panelTop();
        menu.slotsVisible = true;
        statusRevision = menu.statusRevision;
        previewRevision = menu.getPreviewRevision();
        planRevision = menu.getFlightPlannerRevision();
        displayedPattern = menu.pattern().copy();

        addComponent(new SurfaceWidget(INVENTORY_X, INVENTORY_TOP, INVENTORY_WIDTH, HEIGHT - INVENTORY_TOP, OritechSurface.PANEL));
        addConnectors(SIDE_WIDTH, PLAN_X);
        addConnectors(PLAN_X + PLAN_WIDTH, LAUNCH_X);
        addPanel(DESIGN_X, DESIGN_TOP, DESIGN_BOTTOM, SIDE_WIDTH, DESIGN_TOP, "pattern");
        addPanel(PLAN_X, PLAN_TOP, PLAN_BOTTOM, PLAN_WIDTH, planContentOffset(), "plan");
        addPanel(LAUNCH_X, LAUNCH_TOP, LAUNCH_BOTTOM, SIDE_WIDTH, LAUNCH_TOP, "last");

        var status = menu.status;
        var hasSavedPlan = status.hasPlan() && (status.viewingSaved() || status.planMatches());
        var canInspect = menu.getRocket() != null;
        buildDesign(canInspect);
        buildPlan(hasSavedPlan, canInspect);
        buildLastLaunch();

        addComponent(new LabelWidget(LaunchRampMenu.INVENTORY_X, 119, 162,
                Component.translatable("container.inventory")).withDarkColor());
        for (var slot : menu.slots) {
            if (slot == menu.slots.getFirst()) {
                var frame = BoxWidget.outline(slot.x - 1, slot.y - 1, 18, 18, 0xFF85888B);
                if (!slot.hasItem()) frame.withTooltip(text("template_help"));
                addComponent(frame);
            } else addComponent(new ItemSlotWidget(slot.x, slot.y));
        }

        addOracleButton(INVENTORY_X - 10, Math.min(HEIGHT - 13, height - topPos - 17),
                OritechSpaceAge.MOD_ID, getTitleState().getBlock(), Optional.empty());
    }

    private void addConnectors(int left, int right) {

        var width = right - left + 8;
        addComponent(new SurfaceWidget(left - 4, PANEL_CENTER - 19, width, 12, OritechSurface.PANEL_DARK));
        addComponent(new SurfaceWidget(left - 4, PANEL_CENTER + 7, width, 12, OritechSurface.PANEL_DARK));
    }

    private void addPanel(int x, int top, int bottom, int panelWidth, int contentOffset, String key) {

        addComponent(new SurfaceWidget(x, top, panelWidth, bottom - top, OritechSurface.PANEL));
        var heading = new LabelWidget(x + PANEL_PADDING, 9 + contentOffset, panelWidth - PANEL_PADDING * 2, text(key))
                .withAlignment(LabelWidget.Alignment.CENTER).withDarkColor();
        if (x == DESIGN_X) heading.withTooltip(text("template_help"));
        if (x == LAUNCH_X) heading.withTooltip(text("automatic"));
        addComponent(heading);
        addComponent(BoxWidget.filled(x + TEXT_PADDING, 23 + contentOffset, panelWidth - TEXT_PADDING * 2, 1, 0xFF96989B));
    }

    private void buildDesign(boolean canInspect) {

        var status = menu.status;
        var pattern = CubeSatBlock.data(menu.pattern());
        var showSaved = status.hasPlan() && status.viewingSaved();
        var hasChoice = status.hasPlan() && pattern != null && !status.planMatches();
        var nameX = LaunchRampMenu.PATTERN_X + 21;
        var nameWidth = SIDE_WIDTH - TEXT_PADDING - nameX;

        if (pattern != null) {
            addLine(nameX, DESIGN_TOP + 32, nameWidth, menu.pattern().getHoverName(), LabelWidget.DARK_TEXT);
            addLine(nameX, DESIGN_TOP + 44, nameWidth, text("blocks", pattern.blockCount()), DETAIL_TEXT);
        } else {
            var label = new LabelWidget(nameX, DESIGN_TOP + (showSaved ? 37 : 32), nameWidth, 22,
                    text(showSaved ? "slot_empty" : "insert_template")).withColor(DETAIL_TEXT).withWrap(true);
            addComponent(label);
        }

        if (hasChoice) {
            var toggle = SpaceAgeButtons.darkPanel(TEXT_PADDING, DESIGN_TOP + 61, SIDE_TEXT_WIDTH, 14,
                    text(showSaved ? "edit_inserted" : "view_saved"), ignored -> {
                        RocketAssemblerClientController.submitFlightPlanIfDirty(menu);
                        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, showSaved ? 0 : 1);
                    });
            toggle.withTooltip(text(showSaved ? "showing_saved" : "plan_other"));
            addComponent(toggle);
        } else if (showSaved) {
            var ghost = new ItemWidget(LaunchRampMenu.PATTERN_X + 2, DESIGN_TOP + 55, 12, new ItemStack(SpaceAgeBlocks.CUBE_SAT.get()))
                    .withGhost(true).withTooltipFromStack(false);
            ghost.withTooltip(text("plan_stored"));
            addComponent(ghost);
            var name = designName(status.patternName());
            var saved = addLine(nameX, DESIGN_TOP + 57, nameWidth, name, LabelWidget.DARK_TEXT);
            saved.withTooltip(name, text("plan_stored"));
        } else if (status.planMatches()) {
            addLine(TEXT_PADDING, DESIGN_TOP + 63, SIDE_TEXT_WIDTH, text("matches"), DETAIL_TEXT);
        }

        addComponent(panelAction(DESIGN_X, DESIGN_BOTTOM, SIDE_WIDTH, "inspect", canInspect, () ->
                Minecraft.getInstance().setScreen(new RocketAssemblerScreen(menu, screenInventory, title))));
    }

    private void buildPlan(boolean hasSavedPlan, boolean canInspect) {

        var offset = planContentOffset();
        var status = menu.status;
        var snapshot = menu.getFlightPlannerSnapshot();
        var x = PLAN_X + TEXT_PADDING;
        var contentWidth = PLAN_WIDTH - TEXT_PADDING * 2;
        if (hasSavedPlan) {
            var name = status.planName().isBlank() ? text("unnamed_plan") : Component.literal(status.planName());
            var nameLabel = addLine(x, 31 + offset, contentWidth, name, LabelWidget.DARK_TEXT);
            if (!status.owner().isBlank()) nameLabel.withTooltip(name, text("saved_by", status.owner()));
            if (snapshot != null) {
                var actions = snapshot.plan().root().actions().stream().filter(action -> !action.isGenerated()).toList();
                var destinations = actions.stream().filter(action -> action.type() == ActionType.NAVIGATE_TO).toList();
                if (!destinations.isEmpty()) buildRoute(x, 47 + offset, contentWidth, snapshot, destinations);
                else if (!actions.isEmpty()) addLine(x, 51 + offset, contentWidth, firstStep(snapshot, actions.getFirst()), DETAIL_TEXT);
                else addLine(x, 51 + offset, contentWidth, text("no_route"), DETAIL_TEXT);
            }
        } else {
            var label = new LabelWidget(x, 32 + offset, contentWidth, 24,
                    text(canInspect ? "no_plan" : "template_required")).withColor(DETAIL_TEXT).withWrap(true);
            if (status.hasPlan()) label.withTooltip(text("plan_other"));
            addComponent(label);
        }
        addComponent(panelAction(PLAN_X, PLAN_BOTTOM, PLAN_WIDTH, hasSavedPlan ? "open_plan" : "create_plan", canInspect, this::openPlanner));
    }

    private void buildRoute(int x, int y, int width, FlightPlannerSnapshot snapshot, List<FlightPlanAction> stops) {

        var rows = Math.min(y <= 35 ? 4 : 3, stops.size() + 1);
        var rowHeight = 12;
        addComponent(BoxWidget.filled(x + 2, y + 3, 1, (rows - 1) * rowHeight, ROUTE_COLOR));
        addComponent(BoxWidget.filled(x, y + 1, 5, 5, ROUTE_COLOR));
        addLine(x + 10, y, width - 10, text("route_start"), DETAIL_TEXT);
        for (var row = 1; row < rows; row++) {
            var stopY = y + row * rowHeight;
            if (row == rows - 1 && stops.size() >= rows) {
                addComponent(BoxWidget.outline(x, stopY + 1, 5, 5, ROUTE_COLOR));
                var more = addLine(x + 10, stopY, width - 10, text("route_more", stops.size() - row + 1), DETAIL_TEXT);
                more.withTooltip(stops.stream().skip(row - 1L).limit(5).map(stop -> firstStep(snapshot, stop)).toList());
            } else {
                var stop = stops.get(row - 1);
                addComponent(BoxWidget.outline(x, stopY + 1, 5, 5, ROUTE_COLOR));
                var destination = firstStep(snapshot, stop);
                var orbit = snapshot.objects().stream().anyMatch(object -> object.id().equals(stop.targetId()));
                var detail = orbit ? Component.translatable("screen.oritech_space_age.selected_target",
                        destination, RocketStarMapWidget.orbitName(stop.orbit())) : destination;
                addLine(x + 10, stopY, width - 10, detail, DETAIL_TEXT);
            }
        }
    }

    private Component firstStep(FlightPlannerSnapshot snapshot, FlightPlanAction action) {

        if (action.type() == ActionType.NAVIGATE_TO) {
            var target = snapshot.objects().stream().filter(object -> object.id().equals(action.targetId())).findFirst();
            if (target.isPresent()) return RocketStarMapWidget.objectName(target.get());
            var station = snapshot.stations().stream().filter(candidate -> candidate.id().equals(action.targetId())).findFirst();
            if (station.isPresent()) return Component.literal(station.get().name());
            if (action.targetId().equals(FlightPlanAction.CLOSEST_ASTEROID))
                return Component.translatable("screen.oritech_space_age.action.closest_asteroid");
        }
        return Component.translatable("screen.oritech_space_age.action." + action.type().name().toLowerCase(Locale.ROOT));
    }

    private void buildLastLaunch() {

        var status = menu.status;
        var outcome = Outcome.values()[status.outcome()];
        var x = LAUNCH_X + TEXT_PADDING;
        if (outcome == Outcome.NONE) {
            addLine(x, LAUNCH_TOP + 32, SIDE_TEXT_WIDTH, text("last_none"), DETAIL_TEXT);
        } else {
            var key = outcome.name().toLowerCase(Locale.ROOT);
            var result = text("result." + key);
            var detail = text("outcome." + key);
            if (outcome == Outcome.PLAN) {
                addComponent(BoxWidget.filled(LAUNCH_X + PANEL_PADDING, LAUNCH_TOP + 29, SIDE_WIDTH - PANEL_PADDING * 2, 15, MATCH_BACKGROUND));
                var badge = new LabelWidget(x, LAUNCH_TOP + 33, SIDE_TEXT_WIDTH, fit(result, SIDE_TEXT_WIDTH))
                        .withAlignment(LabelWidget.Alignment.CENTER).withColor(MATCH_TEXT);
                badge.withTooltip(detail);
                addComponent(badge);
            } else {
                addLine(x, LAUNCH_TOP + 33, SIDE_TEXT_WIDTH, result, LabelWidget.DARK_TEXT).withTooltip(detail);
            }
            var particle = text("particle_reading", LaunchRampPreviewSpeed.number(status.lastSpeed()));
            if (outcome == Outcome.LOST) {
                addLine(x, LAUNCH_TOP + 55, SIDE_TEXT_WIDTH, particle, LabelWidget.DARK_TEXT).withTooltip(particle);
            } else {
                addLaunchVelocity(x, text("speed_value", LaunchRampPreviewSpeed.metersPerSecond(status.lastSpeed())));
                addLine(x, LAUNCH_TOP + 67, SIDE_TEXT_WIDTH, particle, DETAIL_TEXT).withTooltip(particle);
            }
        }
    }

    private void addLaunchVelocity(int x, Component velocity) {

        var scale = 1.25f;
        var display = fit(velocity, (int) (SIDE_TEXT_WIDTH / scale));
        var label = new UIComponent(x, LAUNCH_TOP + 51, SIDE_TEXT_WIDTH, 12) {
            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

                var pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(contentX(), contentY());
                pose.scale(scale);
                graphics.text(font, display, 0, 0, LabelWidget.DARK_TEXT, false);
                pose.popMatrix();
            }
        };
        label.withTooltip(velocity);
        addComponent(label);
    }

    private ButtonWidget panelAction(int x, int bottom, int panelWidth, String key, boolean active, Runnable action) {

        var label = text(key);
        var width = panelWidth - PANEL_PADDING * 2;
        var button = SpaceAgeButtons.darkPanel(x + PANEL_PADDING, bottom - 25, width, 17, fit(label, width - 8), ignored -> action.run());
        if (font.width(label) > width - 8) button.withTooltip(label);
        button.setActive(active);
        return button;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {

        var template = isHovering(LaunchRampMenu.PATTERN_X, LaunchRampMenu.PATTERN_Y, 16, 16, event.x(), event.y());
        var handled = super.mouseReleased(event);
        // The slot uses Minecraft's normal transfer. Clear any drag preview once it has consumed the cursor stack.
        if (template && !menu.pattern().isEmpty() && menu.getCarried().isEmpty()) clearDraggingState();
        return handled;
    }

    private LabelWidget addLine(int x, int y, int width, Component value, int color) {

        var label = new LabelWidget(x, y, width, fit(value, width)).withColor(color);
        if (font.width(value) > width) label.withTooltip(value);
        addComponent(label);
        return label;
    }

    private Component fit(Component value, int width) {

        if (font.width(value) <= width) return value;
        return Component.literal(font.plainSubstrByWidth(value.getString(), width - font.width("…")) + "…");
    }

    private static Component designName(String name) {

        return name.isBlank() ? SpaceAgeBlocks.CUBE_SAT.get().getName() : Component.literal(name);
    }

    private static MutableComponent text(String key, Object... args) {

        return Component.translatable("screen.oritech_space_age.launch_ramp." + key, args);
    }

    private void openPlanner() {

        Minecraft.getInstance().setScreen(new RocketFlightPlannerScreen(menu, screenInventory, title));
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        if (statusRevision != menu.statusRevision || previewRevision != menu.getPreviewRevision()
                || planRevision != menu.getFlightPlannerRevision()
                || !ItemStack.matches(displayedPattern, menu.pattern())) rebuildComponents();
    }

    @Override
    public void onClose() {

        RocketAssemblerClientController.submitFlightPlanIfDirty(menu);
        super.onClose();
    }

    @Override
    protected void addTitle() {

        addTitle(WIDTH, true);
    }

    @Override
    public int getTitleY() {

        return Math.max(-45, 2 - panelTop());
    }

    private int panelTop() {

        var freeHeight = height - HEIGHT;
        return Math.min(freeHeight - 3, (freeHeight + 45) / 2);
    }

    private int planContentOffset() {

        // Keep the heading below the raised title even at the largest GUI scale.
        return -Math.clamp(-getTitleY() - 19, 3, 16);
    }

    @Override
    public Component getTitleText() {

        return text("title");
    }

    @Override
    public BlockState getTitleState() {

        return SpaceAgeBlocks.LAUNCH_RAMP.get().defaultBlockState();
    }
}
