package rearth.oritech.spaceage.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.HashMap;
import java.util.UUID;

/** Lays out branch rows, action cards and their connectors; edits remain owned by the screen. */
final class FlightPlannerCards {
    // Space reserved by each card, including its gap to the next card.
    private static final int NORMAL_CARD_WIDTH = 142;
    private static final int GENERATED_CARD_WIDTH = 98;
    // Reads current labels and connects buttons to the screen's editing operations.
    private final RocketFlightPlannerScreen screen;

    FlightPlannerCards(RocketFlightPlannerScreen screen) {
        this.screen = screen;
    }

    ScrollWidget build(ActiveRocketData rocket, int editorY, int panelWidth, int editorHeight,
                       float scrollX, float scrollY) {
        var scroll = new ScrollWidget(12, editorY, panelWidth - 24, editorHeight)
                .withVerticalScroll(true).withHorizontalScroll(true)
                .withScrollSpeed(24).withDragScrolling(true).withRenderCulling(4);

        int contentWidth = 8;
        var actionOwners = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        var displayXByAction = new HashMap<UUID, Integer>();
        screen.draftPlan().branches().forEach(branch -> branch.actions()
                .forEach(action -> actionOwners.put(action.id(), branch)));
        var rowByBranch = new HashMap<UUID, Integer>();
        var xByBranch = new HashMap<UUID, Integer>();
        var rowHeight = 164;
        int branchStartY = 8;

        for (int row = 0; row < screen.draftPlan().branches().size(); row++) {
            var branch = screen.draftPlan().branches().get(row);
            rowByBranch.put(branch.id(), row);
            int rowY = branchStartY + row * rowHeight;
            int branchX = 5;
            if (!branch.isRoot()) {
                var parent = actionOwners.get(branch.parentSeparationAction());
                int parentRow = parent == null ? Math.max(0, row - 1) : rowByBranch.getOrDefault(parent.id(), row - 1);
                int parentX = displayXByAction.getOrDefault(branch.parentSeparationAction(),
                        xByBranch.getOrDefault(parent == null ? null : parent.id(), 5) + 78);
                var parentAction = screen.findAction(branch.parentSeparationAction());
                int connectorX = parentX + cardWidth(parentAction) / 2;
                branchX = connectorX + 18;
                int guideY = branchStartY + parentRow * rowHeight
                        + (parentAction != null && parentAction.isGenerated() ? 68 : 130);
                scroll.addChild(FlightPlannerCards.branchConnector(connectorX, guideY, branchX, rowY + 17));
            }
            xByBranch.put(branch.id(), branchX);

            Component branchName = branch.isRoot()
                    ? Component.translatable("screen.oritech_space_age.branch.root")
                    : Component.translatable("screen.oritech_space_age.branch.number", row + 1);
            var branchButton = SpaceAgeButtons.panel(branchX, rowY + 5, 70, 24, branchName,
                    ignored -> screen.selectBranch(branch.id()));
            branchButton.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
            branchButton.setActive(!branch.id().equals(screen.activeBranchId()));
            scroll.addChild(branchButton);

            int cursorX = branchX + 78;
            for (int index = 0; index < branch.actions().size(); index++) {
                var action = branch.actions().get(index);
                displayXByAction.put(action.id(), cursorX);
                if (action.isGenerated()) {
                    addGeneratedCard(scroll, action, rocket, cursorX, rowY);
                    cursorX += GENERATED_CARD_WIDTH;
                } else {
                    addEditableCard(scroll, branch, action, index, rocket, cursorX, rowY);
                    if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                            && (!action.addons().isEmpty() || screen.canAddNavigationAddon(branch, action))) {
                        addNavigationAddonCard(scroll, branch, action, rocket, cursorX, rowY);
                    }
                    cursorX += NORMAL_CARD_WIDTH;
                }
            }
            scroll.addChild(SpaceAgeButtons.orangePanel(cursorX + 2, rowY + 27, 64, 32,
                    Component.translatable("screen.oritech_space_age.action.add"),
                    ignored -> screen.addAction(branch.id(), rocket)));
            contentWidth = Math.max(contentWidth, cursorX + 74);
        }

        scroll.setContentDimensions(contentWidth,
                Math.max(editorHeight - 8, branchStartY + screen.draftPlan().branches().size() * rowHeight));
        scroll.setScrollPosition(scrollX, scrollY);
        return scroll;
    }

    private void addGeneratedCard(ScrollWidget scroll, SpaceSimulation.FlightPlanAction action,
                                  ActiveRocketData rocket, int cursorX, int rowY) {
        scroll.addChild(new SurfaceWidget(cursorX, rowY + 13, GENERATED_CARD_WIDTH - 8, 54,
                OritechSurface.PANEL_DARK));
        var label = new LabelWidget(cursorX + 5, rowY + 21, GENERATED_CARD_WIDTH - 18, 34,
                Component.translatable("screen.oritech_space_age.action.disconnect_booster",
                        screen.segmentName(action.segments().getFirst(), rocket)));
        label.withAlignment(LabelWidget.Alignment.CENTER).withWrap(true).withBrightColor();
        scroll.addChild(label);
    }

    private void addEditableCard(ScrollWidget scroll, SpaceSimulation.FlightPlanBranch branch,
                                 SpaceSimulation.FlightPlanAction action, int index,
                                 ActiveRocketData rocket, int cursorX, int rowY) {
        scroll.addChild(new SurfaceWidget(cursorX, rowY, NORMAL_CARD_WIDTH - 8, 130, OritechSurface.PANEL_INSET));
        scroll.addChild(new LabelWidget(cursorX + 5, rowY + 5, 18,
                Component.literal(Integer.toString(index + 1)).withStyle(ChatFormatting.BOLD)));
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 22, rowY + 4, 103, 17,
                FlightPlannerLabels.actionName(action.type()), ignored -> screen.openActionTypeMenu(action.id(), cursorX + 22, rowY + 21)));

        var parameter = SpaceAgeButtons.panel(cursorX + 5, rowY + 27, 120, 17,
                screen.actionParameter(action, rocket), ignored -> {
                    if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
                        screen.openTargetMenu(action.id(), cursorX + 5, rowY + 44);
                    } else screen.cycleActionParameter(branch.id(), index, rocket);
                });
        parameter.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                || action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                || action.type() == SpaceSimulation.ActionType.DECOUPLE);
        if (action.type() == SpaceSimulation.ActionType.DECOUPLE
                && !action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            var asteroidPath = screen.calculatedFlight().asteroidPaths().stream()
                    .filter(path -> path.asteroidId().equals(action.targetId())).findFirst().orElse(null);
            if (asteroidPath != null) parameter.withTooltip(FlightPlannerLabels.releasedAsteroidTooltip(asteroidPath));
        }
        scroll.addChild(parameter);
        var orbit = SpaceAgeButtons.panel(cursorX + 5, rowY + 48, 120, 17,
                screen.actionOrbit(action), ignored -> {
                    if (FlightPlannerLabels.isEarthSurface(action)) screen.openLandingEditor(action.id());
                    else screen.cycleActionOrbit(branch.id(), index, rocket);
                });
        orbit.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO);
        scroll.addChild(orbit);

        var velocity = SpaceAgeButtons.panel(cursorX + 5, rowY + 69, 120, 17,
                FlightPlannerLabels.actionVelocity(action), ignored -> screen.openArrivalEditor(action.id()));
        velocity.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO);
        var prediction = screen.arrivalPrediction(action.id());
        if (prediction != null) velocity.withTooltip(FlightPlannerLabels.arrivalTooltip(prediction.impact()));
        scroll.addChild(velocity);

        var speed = SpaceAgeButtons.panel(cursorX + 5, rowY + 91, 120, 17,
                Component.translatable("screen.oritech_space_age.action.speed_limit",
                        action.maxSpeed() == 0 ? "Maximum" : action.maxSpeed() + " m/s"), ignored -> {
                    screen.openSpeedEditor(action);
                });
        speed.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO);
        scroll.addChild(speed);

        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 5, rowY + 113, 18, 13, Component.literal("<"),
                ignored -> screen.moveAction(branch.id(), index, -1, rocket)));
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 25, rowY + 113, 18, 13, Component.literal(">"),
                ignored -> screen.moveAction(branch.id(), index, 1, rocket)));
        var decrease = SpaceAgeButtons.darkPanel(cursorX + 49, rowY + 113, 18, 13, Component.literal("−"),
                ignored -> screen.adjustActionVelocity(branch.id(), index, -1, rocket));
        decrease.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                && action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM);
        scroll.addChild(decrease);
        var increase = SpaceAgeButtons.darkPanel(cursorX + 69, rowY + 113, 18, 13, Component.literal("+"),
                ignored -> screen.adjustActionVelocity(branch.id(), index, 1, rocket));
        increase.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                && action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM);
        scroll.addChild(increase);
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 108, rowY + 113, 17, 13, Component.literal("×"),
                ignored -> screen.removeAction(branch.id(), index, rocket)));
    }

    private void addNavigationAddonCard(ScrollWidget scroll, SpaceSimulation.FlightPlanBranch branch,
                                        SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket,
                                        int cursorX, int rowY) {
        scroll.addChild(FlightPlannerCards.addonConnector(cursorX + 66, rowY + 130, rowY + 135));
        if (action.addons().isEmpty()) {
            scroll.addChild(SpaceAgeButtons.orangePanel(cursorX + 54, rowY + 135, 24, 20,
                    Component.literal("+"), ignored -> screen.addNavigationAddon(branch.id(), action.id(), rocket))
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.add_completion_condition")));
            return;
        }
        var addon = action.addons().getFirst();
        var button = SpaceAgeButtons.darkPanel(cursorX + 5, rowY + 135, 99, 20,
                FlightPlannerLabels.addonSummary(addon), ignored -> screen.openAddonEditor(action.id()));
        var moment = screen.calculatedFlight().navigationAborts().stream()
                .filter(item -> item.addonId().equals(addon.id())).findFirst().orElse(null);
        if (moment != null) button.withTooltip(Component.translatable(
                "screen.oritech_space_age.action.condition_reached", FlightPlannerLabels.formatAddonValue(addon.type(), moment.actualValue())));
        scroll.addChild(button);
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 107, rowY + 135, 18, 20,
                Component.literal("×"), ignored -> screen.removeNavigationAddon(branch.id(), action.id(), rocket)));
    }

    private static int cardWidth(SpaceSimulation.FlightPlanAction action) {
        return action != null && action.isGenerated() ? GENERATED_CARD_WIDTH : NORMAL_CARD_WIDTH;
    }

    static UIComponent branchConnector(int startX, int startY, int endX, int endY) {
        return new UIComponent(Math.min(startX, endX), Math.min(startY, endY),
                Math.max(1, Math.abs(endX - startX)), Math.max(1, Math.abs(endY - startY))) {
            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                int cornerY = Math.min(endY, startY + 7);
                int color = 0xFFD58A32;
                graphics.fill(startX - 1, startY, startX + 1, cornerY + 1, color);
                graphics.fill(Math.min(startX, endX), cornerY - 1, Math.max(startX, endX) + 1, cornerY + 1, color);
                graphics.fill(endX - 1, cornerY, endX + 1, endY, color);
            }
        };
    }

    static UIComponent addonConnector(int lineX, int startY, int endY) {
        return new UIComponent(lineX - 1, startY, 2, Math.max(1, endY - startY)) {
            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                graphics.fill(lineX - 1, startY, lineX + 1, endY, 0xFFD58A32);
            }
        };
    }
}
