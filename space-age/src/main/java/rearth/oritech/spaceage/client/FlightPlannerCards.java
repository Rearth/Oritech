package rearth.oritech.spaceage.client;

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
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lays out branch rows, action cards and their connectors; edits remain owned by the screen. */
final class FlightPlannerCards {
    // Space reserved by each card, including its gap to the next card.
    private static final int NORMAL_CARD_WIDTH = 142;
    private static final int GENERATED_CARD_WIDTH = 98;
    private static final int BRANCH_CARD_WIDTH = 126;
    private static final int CARD_HEIGHT = 84;
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
        var rowHeight = 132;
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
                        xByBranch.getOrDefault(parent == null ? null : parent.id(), 5) + BRANCH_CARD_WIDTH + 8);
                var parentAction = screen.findAction(branch.parentSeparationAction());
                int connectorX = parentX + cardWidth(parentAction) / 2;
                branchX = connectorX + 18;
                int guideY = branchStartY + parentRow * rowHeight
                        + (parentAction != null && parentAction.isGenerated() ? 68 : CARD_HEIGHT);
                scroll.addChild(FlightPlannerCards.branchConnector(connectorX, guideY, branchX, rowY + 17));
            }
            xByBranch.put(branch.id(), branchX);

            Component branchName = branch.isRoot()
                    ? Component.translatable("screen.oritech_space_age.branch.root")
                    : Component.translatable("screen.oritech_space_age.branch.number", row + 1);
            boolean selectedBranch = branch.id().equals(screen.activeBranchId());
            var branchButton = SpaceAgeButtons.panel(branchX, rowY + 5, BRANCH_CARD_WIDTH, 58, Component.empty(),
                    ignored -> screen.selectBranch(branch.id()));
            branchButton.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
            branchButton.setActive(!selectedBranch);
            scroll.addChild(branchButton);
            var branchPath = screen.calculatedFlight().paths().stream()
                    .filter(path -> path.branchId().equals(branch.id())).findFirst().orElse(null);
            if (branchPath != null && branchPath.terminalState().isFailure()) {
                var reason = FlightPlannerLabels.blockedTooltip(branchPath.terminalState());
                branchButton.withTooltip(reason);
                scroll.addChild(blockedBorder(branchX, rowY + 5, BRANCH_CARD_WIDTH, 58, List.of(reason)));
            }
            var branchColor = selectedBranch ? LabelWidget.BRIGHT_TEXT : LabelWidget.DARK_TEXT;
            scroll.addChild(new LabelWidget(branchX + 7, rowY + 13, BRANCH_CARD_WIDTH - 14, branchName)
                    .withColor(branchColor).withShadow(selectedBranch));
            scroll.addChild(new LabelWidget(branchX + 7, rowY + 29, BRANCH_CARD_WIDTH - 14, 27,
                    branchSegments(branch, rocket)).withColor(branchColor).withWrap(true));

            int cursorX = branchX + BRANCH_CARD_WIDTH + 8;
            if (branch.isRoot()) {
                int completedIndex = 0;
                for (var action : screen.completedActions()) {
                    addCompletedCard(scroll, action, ++completedIndex, rocket, cursorX, rowY);
                    cursorX += NORMAL_CARD_WIDTH;
                }
            }
            for (int index = 0; index < branch.actions().size(); index++) {
                var action = branch.actions().get(index);
                displayXByAction.put(action.id(), cursorX);
                if (action.isGenerated()) {
                    addGeneratedCard(scroll, action, rocket, cursorX, rowY);
                    cursorX += GENERATED_CARD_WIDTH;
                } else {
                    addEditableCard(scroll, branch, action, index, rocket, cursorX, rowY);
                    if ((action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                            || action.type() == SpaceSimulation.ActionType.MAINTAIN_POSITION)
                            && (!action.addons().isEmpty() || !screen.isReadOnly() && screen.canAddNavigationAddon(branch, action))) {
                        addNavigationAddonCard(scroll, branch, action, rocket, cursorX, rowY);
                    }
                    cursorX += NORMAL_CARD_WIDTH;
                }
            }
            if (!screen.isReadOnly()) {
                int addActionX = cursorX + 2;
                scroll.addChild(SpaceAgeButtons.orangePanel(addActionX, rowY + 27, 64, 32,
                        Component.translatable("screen.oritech_space_age.action.add"),
                        ignored -> screen.openAddActionMenu(branch.id(), addActionX, rowY + 61)));
            }
            contentWidth = Math.max(contentWidth, cursorX + 74);
        }

        scroll.setContentDimensions(contentWidth,
                Math.max(editorHeight - 8, branchStartY + screen.draftPlan().branches().size() * rowHeight));
        scroll.setScrollPosition(scrollX, scrollY);
        return scroll;
    }

    private Component branchSegments(SpaceSimulation.FlightPlanBranch branch, ActiveRocketData rocket) {
        Set<SpaceSimulation.SegmentRef> segments = screen.calculatedFlight().paths().stream()
                .filter(path -> path.branchId().equals(branch.id())).findFirst()
                .map(path -> path.samples().isEmpty() ? path.segments() : path.samples().getFirst().connectedSegments())
                .orElse(Set.of());
        if (segments.isEmpty()) return Component.translatable("screen.oritech_space_age.branch.no_segments");
        var names = segments.stream().map(segment -> screen.segmentName(segment, rocket)).sorted().toList();
        return Component.translatable("screen.oritech_space_age.branch.segments", String.join(", ", names));
    }

    private void addCompletedCard(ScrollWidget scroll, SpaceSimulation.FlightPlanAction action, int number,
                                  ActiveRocketData rocket, int x, int y) {
        scroll.addChild(new SurfaceWidget(x, y, NORMAL_CARD_WIDTH - 8, CARD_HEIGHT, OritechSurface.PANEL_DARK)
                .withTooltip(Component.translatable("screen.oritech_space_age.mission.completed_read_only")));
        scroll.addChild(new LabelWidget(x + 6, y + 7, NORMAL_CARD_WIDTH - 20, 22,
                Component.literal(number + ". ").append(FlightPlannerLabels.actionName(action.type()))).withColor(0xFF999999).withWrap(true));
        scroll.addChild(new LabelWidget(x + 6, y + 32, NORMAL_CARD_WIDTH - 20, 24,
                screen.actionParameter(action, rocket)).withColor(0xFF999999).withWrap(true));
        scroll.addChild(new LabelWidget(x + 6, y + 65, NORMAL_CARD_WIDTH - 20,
                Component.translatable("screen.oritech_space_age.mission.completed")).withColor(0xFF999999));
    }

    private void addGeneratedCard(ScrollWidget scroll, SpaceSimulation.FlightPlanAction action,
                                  ActiveRocketData rocket, int cursorX, int rowY) {
        var panel = new SurfaceWidget(cursorX, rowY + 13, GENERATED_CARD_WIDTH - 8, 54, OritechSurface.PANEL_DARK);
        var event = screen.calculatedFlight().boosterEvents().stream().filter(item -> item.id().equals(action.id())).findFirst().orElse(null);
        if (event != null) panel.withTooltip(Component.translatable("screen.oritech_space_age.mission.automatic_separation",
                String.format(Locale.ROOT, "%.1f", event.timeSeconds())));
        scroll.addChild(panel);
        var label = new LabelWidget(cursorX + 5, rowY + 21, GENERATED_CARD_WIDTH - 18, 34,
                Component.translatable("screen.oritech_space_age.action.disconnect_booster",
                        screen.segmentName(action.segments().getFirst(), rocket)));
        label.withAlignment(LabelWidget.Alignment.CENTER).withWrap(true).withBrightColor();
        scroll.addChild(label);
    }

    private void addEditableCard(ScrollWidget scroll, SpaceSimulation.FlightPlanBranch branch,
                                 SpaceSimulation.FlightPlanAction action, int index,
                                 ActiveRocketData rocket, int cursorX, int rowY) {
        var card = SpaceAgeButtons.darkPanel(cursorX, rowY, NORMAL_CARD_WIDTH - 8, CARD_HEIGHT,
                Component.empty(), ignored -> { if (!screen.isReadOnly()) screen.openActionEditor(action.id()); });
        card.withTooltip(FlightPlannerLabels.actionTooltip(action.type()));
        if (action.type() == SpaceSimulation.ActionType.MAINTAIN_POSITION) {
            card.addTooltipLine(stationKeepingEstimate(action));
        }
        if (action.type() == SpaceSimulation.ActionType.DECOUPLE
                && !action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            var asteroidPath = screen.calculatedFlight().asteroidPaths().stream()
                    .filter(path -> path.asteroidId().equals(action.targetId())).findFirst().orElse(null);
            if (asteroidPath != null) card.withTooltip(FlightPlannerLabels.releasedAsteroidTooltip(asteroidPath));
        }
        var scanEstimate = FlightPlannerLabels.scanEstimate(screen.calculatedFlight(), action.id());
        if (action.type() == SpaceSimulation.ActionType.SCAN) {
            FlightPlannerLabels.scanEnergyTooltip(scanEstimate).forEach(card::addTooltipLine);
        }
        if (screen.isReadOnly()) card.addTooltipLine(Component.translatable("screen.oritech_space_age.mission.accepted_read_only"));
        scroll.addChild(card);
        if (screen.isCurrentAction(action.id())) {
            scroll.addChild(cardBorder(cursorX, rowY, NORMAL_CARD_WIDTH - 8, CARD_HEIGHT,
                    currentActionTooltip(card.getTooltip(), screen.currentActionLabel()), 0xFF5AD6EB));
            var progress = new LabelWidget(cursorX + (screen.isReadOnly() ? 6 : 83), rowY + 65,
                    screen.isReadOnly() ? 115 : 29, screen.currentActionLabel()).withColor(0xFF5AD6EB);
            screen.registerCurrentActionLabel(progress);
            scroll.addChild(progress);
        }
        var blockedPath = screen.calculatedFlight().paths().stream()
                .filter(path -> path.branchId().equals(branch.id()) && path.terminalState().isFailure())
                .filter(path -> path.actionMoments().stream().anyMatch(moment -> moment.actionId().equals(action.id())
                        && (!moment.completed() || path.actionMoments().getLast().equals(moment))))
                .findFirst().orElse(null);
        if (blockedPath != null) {
            var reason = FlightPlannerLabels.blockedTooltip(blockedPath.terminalState());
            card.addTooltipLine(reason);
            scroll.addChild(blockedBorder(cursorX, rowY, NORMAL_CARD_WIDTH - 8, CARD_HEIGHT, action.type() == SpaceSimulation.ActionType.SCAN
                    ? card.getTooltip() : List.of(reason)));
        }
        int textColor = LabelWidget.BRIGHT_TEXT;
        scroll.addChild(new LabelWidget(cursorX + 6, rowY + 7, NORMAL_CARD_WIDTH - 20,
                Component.literal((index + 1 + (branch.isRoot() ? screen.completedActions().size() : 0)) + ". ").append(FlightPlannerLabels.actionName(action.type())))
                .withColor(textColor).withShadow(true));
        Component parameter = screen.actionParameter(action, rocket);
        if (action.type() == SpaceSimulation.ActionType.DECOUPLE && action.segments().size() == 2
                && action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            parameter = Component.translatable("screen.oritech_space_age.action.decouple_connection",
                    screen.segmentName(action.segments().get(0), rocket),
                    screen.segmentName(action.segments().get(1), rocket));
        }
        scroll.addChild(new LabelWidget(cursorX + 6, rowY + 23, NORMAL_CARD_WIDTH - 20, 20,
                parameter).withColor(textColor).withWrap(true));
        if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            var details = Component.empty().append(screen.actionOrbit(action)).append(" · ")
                    .append(FlightPlannerLabels.actionVelocity(action));
            scroll.addChild(new LabelWidget(cursorX + 6, rowY + 45, NORMAL_CARD_WIDTH - 20, 18, details)
                    .withBrightColor().withWrap(true));
        } else if (action.type() == SpaceSimulation.ActionType.SCAN) {
            scroll.addChild(new LabelWidget(cursorX + 6, rowY + 45, NORMAL_CARD_WIDTH - 20, 18,
                    FlightPlannerLabels.scanEnergyLabel(scanEstimate)).withBrightColor().withWrap(true)
                    .withTooltip(FlightPlannerLabels.scanEnergyTooltip(scanEstimate)));
        } else if (action.type() == SpaceSimulation.ActionType.MAINTAIN_POSITION) {
            scroll.addChild(new LabelWidget(cursorX + 6, rowY + 45, NORMAL_CARD_WIDTH - 20, 18,
                    stationKeepingEstimate(action)).withBrightColor().withWrap(true));
        } else if (action.type() == SpaceSimulation.ActionType.DECOUPLE && action.segments().size() == 2
                && action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            scroll.addChild(new LabelWidget(cursorX + 6, rowY + 45, NORMAL_CARD_WIDTH - 20, 18,
                    Component.translatable("screen.oritech_space_age.action.decouple_branches",
                            screen.segmentName(action.segments().get(0), rocket),
                            screen.segmentName(action.segments().get(1), rocket)))
                    .withBrightColor().withWrap(true));
        }

        if (screen.isReadOnly()) return;
        var editable = branch.actions().stream().filter(item -> !item.isGenerated()).toList();
        int editableIndex = editable.indexOf(action);
        var earlier = SpaceAgeButtons.panel(cursorX + 5, rowY + 65, 35, 14, Component.literal("←"),
                ignored -> screen.moveAction(branch.id(), index, -1, rocket));
        earlier.setActive(editableIndex > 0);
        earlier.setZIndex(1);
        scroll.addChild(earlier.withTooltip(Component.translatable("screen.oritech_space_age.action.move_earlier")));
        var later = SpaceAgeButtons.panel(cursorX + 43, rowY + 65, 35, 14, Component.literal("→"),
                ignored -> screen.moveAction(branch.id(), index, 1, rocket));
        later.setActive(editableIndex >= 0 && editableIndex < editable.size() - 1);
        later.setZIndex(1);
        scroll.addChild(later.withTooltip(Component.translatable("screen.oritech_space_age.action.move_later")));
        var remove = SpaceAgeButtons.panel(cursorX + 113, rowY + 65, 16, 14, Component.literal("×"),
                ignored -> screen.removeAction(branch.id(), index, rocket));
        remove.setZIndex(1);
        scroll.addChild(remove.withTooltip(Component.translatable("screen.oritech_space_age.action.remove")));
    }

    private Component stationKeepingEstimate(SpaceSimulation.FlightPlanAction action) {
        return FlightPlannerLabels.stationKeepingDuration(
                FlightPlannerLabels.stationKeepingEstimate(screen.calculatedFlight(), action.id()));
    }

    private void addNavigationAddonCard(ScrollWidget scroll, SpaceSimulation.FlightPlanBranch branch,
                                        SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket,
                                        int cursorX, int rowY) {
        scroll.addChild(addonConnector(cursorX + 66, rowY + CARD_HEIGHT, rowY + 89));
        if (action.addons().isEmpty()) {
            scroll.addChild(SpaceAgeButtons.orangePanel(cursorX + 18, rowY + 89, 96, 20,
                    Component.translatable("screen.oritech_space_age.action.add_abort"),
                    ignored -> screen.addNavigationAddon(branch.id(), action.id(), rocket))
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.add_abort_tooltip")));
            return;
        }
        var addon = action.addons().getFirst();
        if (screen.isReadOnly()) {
            scroll.addChild(new SurfaceWidget(cursorX + 5, rowY + 89, 125, 20, OritechSurface.PANEL_DARK)
                    .withTooltip(FlightPlannerLabels.addonTooltip(addon.type())));
            scroll.addChild(new LabelWidget(cursorX + 10, rowY + 95, 115,
                    FlightPlannerLabels.addonSummary(addon)).withColor(0xFFBBBBBB));
            return;
        }
        var tooltip = new ArrayList<>(FlightPlannerLabels.addonTooltip(addon.type()));
        var moment = screen.calculatedFlight().navigationAborts().stream()
                .filter(item -> item.addonId().equals(addon.id())).findFirst().orElse(null);
        if (moment != null) tooltip.add(Component.translatable(
                "screen.oritech_space_age.action.condition_reached",
                FlightPlannerLabels.formatAddonValue(addon.type(), moment.actualValue())));
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 5, rowY + 89, 106, 20,
                FlightPlannerLabels.addonSummary(addon), ignored -> screen.openAddonEditor(action.id()))
                .withTooltip(tooltip));
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 114, rowY + 89, 16, 20,
                Component.literal("×"), ignored -> screen.removeNavigationAddon(branch.id(), action.id(), rocket))
                .withTooltip(Component.translatable("screen.oritech_space_age.action.remove_condition")));
    }

    private static int cardWidth(SpaceSimulation.FlightPlanAction action) {
        return action != null && action.isGenerated() ? GENERATED_CARD_WIDTH : NORMAL_CARD_WIDTH;
    }

    private List<Component> currentActionTooltip(List<Component> tooltip, Component progress) {
        var result = new ArrayList<>(tooltip);
        result.add(Component.translatable("screen.oritech_space_age.mission.executing", progress));
        return result;
    }

    private static UIComponent blockedBorder(int x, int y, int width, int height, List<Component> reason) {
        return cardBorder(x, y, width, height, reason, 0xFFFF6565).withZIndex(3);
    }

    private static UIComponent cardBorder(int x, int y, int width, int height, List<Component> reason, int color) {
        return new UIComponent(x, y, width, height) {
            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                graphics.fill(x, y, x + width, y + 2, color);
                graphics.fill(x, y + height - 2, x + width, y + height, color);
                graphics.fill(x, y, x + 2, y + height, color);
                graphics.fill(x + width - 2, y, x + width, y + height, color);
            }
        }.withTooltip(reason).withZIndex(2);
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
