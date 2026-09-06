package rearth.oritech.spaceage.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Edits mission intent while the analytical calculator supplies paths and automatic booster events. */
public class RocketFlightPlannerScreen extends OritechWidgetScreen<RocketAssemblerMenu> {

    private static final int WINDOW_PADDING = 6;
    private static final int NORMAL_CARD_WIDTH = 142;
    private static final int GENERATED_CARD_WIDTH = 98;
    private static final SpaceSimulation.ActionType[] EDITABLE_ACTION_TYPES = {
            SpaceSimulation.ActionType.NAVIGATE_TO,
            SpaceSimulation.ActionType.CONNECT_ASTEROID,
            SpaceSimulation.ActionType.DECOUPLE,
            SpaceSimulation.ActionType.MAINTAIN_POSITION,
            SpaceSimulation.ActionType.DISCARD_CRAFT
    };

    private int previewRevision = -1;
    private int flightPlannerRevision = -1;
    private int draftSourceRevision = -1;
    private SpaceSimulation.FlightPlan draftPlan = SpaceSimulation.FlightPlan.empty();
    private RocketFlightPathCalculator.FlightPath calculatedFlight =
            new RocketFlightPathCalculator.FlightPath(List.of(), List.of(), 0);
    private RocketStarMapWidget flightPlanMap;
    private ActiveRocketData flightPlanRocket;
    private ScrollWidget flightPlanActionScroll;
    private float flightPlanActionScrollX;
    private float flightPlanActionScrollY;
    private UUID activeBranchId;
    private RocketStarMapWidget.NavigationSelection selectedTarget =
            new RocketStarMapWidget.NavigationSelection(SpaceObjects.EARTH_ID, SpaceSimulation.OrbitBand.LOW);
    private final Inventory screenInventory;
    private final Component screenTitle;
    private int panelWidth;
    private int panelHeight;
    private int editorY;
    private int editorHeight;
    private UUID speedAction;
    private EditBox speedField;
    private String speedText;
    private UUID actionTypeAction;
    private UUID targetAction;
    private EditBox targetSearchField;
    private String targetSearch = "";
    private ScrollWidget targetList;
    private final List<TargetOption> targetOptions = new ArrayList<>();
    private UUID arrivalAction;
    private EditBox arrivalField;
    private String arrivalText;
    private UUID landingAction;
    private EditBox landingXField;
    private EditBox landingZField;
    private String landingXText;
    private String landingZText;
    private UUID addonAction;
    private EditBox addonValueField;
    private String addonValueText;
    private RocketStarMapWidget.NavigationContextRequest mapContextRequest;
    private int dropdownX;
    private int dropdownY;

    public RocketFlightPlannerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 0, 0);
        screenInventory = inventory;
        screenTitle = title;
    }

    @Override
    protected void buildComponents() {
        panelWidth = width - WINDOW_PADDING * 2;
        panelHeight = height - WINDOW_PADDING * 2;
        setPanelSize(panelWidth, panelHeight);
        previewRevision = menu.getPreviewRevision();
        flightPlannerRevision = menu.getFlightPlannerRevision();
        addComponent(new SurfaceWidget(0, 0, panelWidth, panelHeight, OritechSurface.PANEL));

        var rocketTab = SpaceAgeButtons.panel(9, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.rocket"), ignored -> switchToRocketScreen());
        rocketTab.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
        addComponent(rocketTab);
        var flightPlanTab = SpaceAgeButtons.panel(101, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.flight_plan"), ignored -> { });
        flightPlanTab.setActive(false);
        flightPlanTab.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
        addComponent(flightPlanTab);
        buildFlightPlanTab();
        if (flightPlanMap != null) flightPlanMap.setTooltipsEnabled(!hasOpenPopup());
        speedField = null;
        targetSearchField = null;
        targetList = null;
        targetOptions.clear();
        arrivalField = null;
        landingXField = null;
        landingZField = null;
        addonValueField = null;
        if (speedAction != null) buildSpeedEditor();
        else if (actionTypeAction != null) buildActionTypeMenu();
        else if (targetAction != null) buildTargetMenu();
        else if (arrivalAction != null) buildArrivalEditor();
        else if (landingAction != null) buildLandingEditor();
        else if (addonAction != null) buildAddonEditor();
        else if (mapContextRequest != null) buildMapContextMenu();
    }

    private boolean hasOpenPopup() {
        return speedAction != null || actionTypeAction != null || targetAction != null
                || arrivalAction != null || landingAction != null || addonAction != null
                || mapContextRequest != null;
    }

    private void buildFlightPlanTab() {
        var rocket = menu.getRocket();
        var snapshot = menu.getFlightPlannerSnapshot();
        if (rocket == null) {
            addMessagePanel(Component.translatable("screen.oritech_space_age.invalid_rocket"));
            return;
        }
        if (snapshot == null || !snapshot.rocketId().equals(rocket.getRocketId())) {
            addMessagePanel(Component.translatable("screen.oritech_space_age.loading_flight_plan"));
            return;
        }

        if (draftSourceRevision != menu.getFlightPlannerRevision()) {
            var sharedDraft = menu.getDraftFlightPlan();
            draftPlan = RocketFlightPlanRules.normalize(sharedDraft == null ? snapshot.plan() : sharedDraft);
            draftSourceRevision = menu.getFlightPlannerRevision();
        }
        flightPlanRocket = rocket;
        recalculateAndSynchronize();
        if (activeBranchId == null || draftPlan.branches().stream()
                .noneMatch(branch -> branch.id().equals(activeBranchId))) activeBranchId = draftPlan.root().id();

        int availableHeight = panelHeight - 51;
        editorHeight = Math.min(Math.clamp(availableHeight / 3, 160, 270), Math.max(64, availableHeight - 112));
        int mapHeight = availableHeight - editorHeight - 6;
        editorY = 39 + mapHeight + 6;
        var previousMap = flightPlanMap;
        flightPlanMap = new RocketStarMapWidget(12, 39, panelWidth - 24, mapHeight,
                currentDraftSnapshot(), calculatedFlight, rocket, activeBranchId, selectedTarget,
                this::selectMapTarget, this::openMapContextMenu);
        flightPlanMap.copyViewFrom(previousMap);
        addComponent(flightPlanMap);
        addFlightPlanEditor(rocket);
    }

    private void recalculateAndSynchronize() {
        var snapshot = currentDraftSnapshot();
        calculatedFlight = RocketFlightPathCalculator.calculate(flightPlanRocket, snapshot.objects(), draftPlan);
        var synchronizedPlan = RocketFlightPlanRules.synchronizeBoosterEvents(
                draftPlan, calculatedFlight.boosterEvents());
        if (!synchronizedPlan.equals(draftPlan)) {
            draftPlan = synchronizedPlan;
            menu.setDraftFlightPlan(draftPlan);
            calculatedFlight = RocketFlightPathCalculator.calculate(
                    flightPlanRocket, snapshot.objects(), draftPlan);
        }
    }

    private void selectMapTarget(RocketStarMapWidget.NavigationSelection selection) {
        selectedTarget = selection;
    }

    private void openMapContextMenu(RocketStarMapWidget.NavigationContextRequest request) {
        closeEditorState();
        mapContextRequest = request;
        rebuildComponents();
    }

    private void addFlightPlanEditor(ActiveRocketData rocket) {
        var scroll = new ScrollWidget(12, editorY, panelWidth - 24, editorHeight)
                .withVerticalScroll(true).withHorizontalScroll(true)
                .withScrollSpeed(24).withDragScrolling(true);

        int contentWidth = 8;
        var actionOwners = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        var displayXByAction = new HashMap<UUID, Integer>();
        draftPlan.branches().forEach(branch -> branch.actions()
                .forEach(action -> actionOwners.put(action.id(), branch)));
        var rowByBranch = new HashMap<UUID, Integer>();
        var xByBranch = new HashMap<UUID, Integer>();
        var rowHeight = 164;
        int branchStartY = 8;

        for (int row = 0; row < draftPlan.branches().size(); row++) {
            var branch = draftPlan.branches().get(row);
            rowByBranch.put(branch.id(), row);
            int rowY = branchStartY + row * rowHeight;
            int branchX = 5;
            if (!branch.isRoot()) {
                var parent = actionOwners.get(branch.parentSeparationAction());
                int parentRow = parent == null ? Math.max(0, row - 1) : rowByBranch.getOrDefault(parent.id(), row - 1);
                int parentX = displayXByAction.getOrDefault(branch.parentSeparationAction(),
                        xByBranch.getOrDefault(parent == null ? null : parent.id(), 5) + 78);
                var parentAction = findAction(branch.parentSeparationAction());
                int connectorX = parentX + cardWidth(parentAction) / 2;
                branchX = connectorX + 18;
                int guideY = branchStartY + parentRow * rowHeight
                        + (parentAction != null && parentAction.isGenerated() ? 68 : 130);
                scroll.addChild(new BranchConnectorWidget(connectorX, guideY, branchX, rowY + 17));
            }
            xByBranch.put(branch.id(), branchX);

            Component branchName = branch.isRoot()
                    ? Component.translatable("screen.oritech_space_age.branch.root")
                    : Component.translatable("screen.oritech_space_age.branch.number", row + 1);
            var branchButton = SpaceAgeButtons.panel(branchX, rowY + 5, 70, 24, branchName,
                    ignored -> selectBranch(branch.id()));
            branchButton.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
            branchButton.setActive(!branch.id().equals(activeBranchId));
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
                            && (!action.addons().isEmpty() || canAddNavigationAddon(branch, action))) {
                        addNavigationAddonCard(scroll, branch, action, rocket, cursorX, rowY);
                    }
                    cursorX += NORMAL_CARD_WIDTH;
                }
            }
            scroll.addChild(SpaceAgeButtons.orangePanel(cursorX + 2, rowY + 27, 64, 32,
                    Component.translatable("screen.oritech_space_age.action.add"),
                    ignored -> addAction(branch.id(), rocket)));
            contentWidth = Math.max(contentWidth, cursorX + 74);
        }

        scroll.setContentDimensions(contentWidth,
                Math.max(editorHeight - 8, branchStartY + draftPlan.branches().size() * rowHeight));
        scroll.setScrollPosition(flightPlanActionScrollX, flightPlanActionScrollY);
        flightPlanActionScroll = scroll;
        addComponent(scroll);
    }

    private void addGeneratedCard(ScrollWidget scroll, SpaceSimulation.FlightPlanAction action,
                                  ActiveRocketData rocket, int cursorX, int rowY) {
        scroll.addChild(new SurfaceWidget(cursorX, rowY + 13, GENERATED_CARD_WIDTH - 8, 54,
                OritechSurface.PANEL_DARK));
        var label = new LabelWidget(cursorX + 5, rowY + 21, GENERATED_CARD_WIDTH - 18, 34,
                Component.translatable("screen.oritech_space_age.action.disconnect_booster",
                        segmentName(action.segments().getFirst(), rocket)));
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
                actionName(action.type()), ignored -> openActionTypeMenu(action.id(), cursorX + 22, rowY + 21)));

        var parameter = SpaceAgeButtons.panel(cursorX + 5, rowY + 27, 120, 17,
                actionParameter(action, rocket), ignored -> {
                    if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
                        openTargetMenu(action.id(), cursorX + 5, rowY + 44);
                    } else cycleActionParameter(branch.id(), index, rocket);
                });
        parameter.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                || action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                || action.type() == SpaceSimulation.ActionType.DECOUPLE);
        if (action.type() == SpaceSimulation.ActionType.DECOUPLE
                && !action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            var asteroidPath = calculatedFlight.asteroidPaths().stream()
                    .filter(path -> path.asteroidId().equals(action.targetId())).findFirst().orElse(null);
            if (asteroidPath != null) parameter.withTooltip(releasedAsteroidTooltip(asteroidPath));
        }
        scroll.addChild(parameter);
        var orbit = SpaceAgeButtons.panel(cursorX + 5, rowY + 48, 120, 17,
                actionOrbit(action), ignored -> {
                    if (isEarthSurface(action)) openLandingEditor(action.id());
                    else cycleActionOrbit(branch.id(), index, rocket);
                });
        orbit.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO);
        scroll.addChild(orbit);

        var velocity = SpaceAgeButtons.panel(cursorX + 5, rowY + 69, 120, 17,
                actionVelocity(action), ignored -> openArrivalEditor(action.id()));
        velocity.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO);
        var prediction = arrivalPrediction(action.id());
        if (prediction != null) velocity.withTooltip(arrivalTooltip(prediction.impact()));
        scroll.addChild(velocity);

        var speed = SpaceAgeButtons.panel(cursorX + 5, rowY + 91, 120, 17,
                Component.translatable("screen.oritech_space_age.action.speed_limit",
                        action.maxSpeed() == 0 ? "Maximum" : action.maxSpeed() + " m/s"), ignored -> {
                    speedAction = action.id();
                    speedText = action.maxSpeed() == 0 ? "maximum" : Integer.toString(action.maxSpeed());
                    rebuildComponents();
                });
        speed.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO);
        scroll.addChild(speed);

        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 5, rowY + 113, 18, 13, Component.literal("<"),
                ignored -> moveAction(branch.id(), index, -1, rocket)));
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 25, rowY + 113, 18, 13, Component.literal(">"),
                ignored -> moveAction(branch.id(), index, 1, rocket)));
        var decrease = SpaceAgeButtons.darkPanel(cursorX + 49, rowY + 113, 18, 13, Component.literal("−"),
                ignored -> adjustActionVelocity(branch.id(), index, -1, rocket));
        decrease.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                && action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM);
        scroll.addChild(decrease);
        var increase = SpaceAgeButtons.darkPanel(cursorX + 69, rowY + 113, 18, 13, Component.literal("+"),
                ignored -> adjustActionVelocity(branch.id(), index, 1, rocket));
        increase.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                && action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM);
        scroll.addChild(increase);
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 108, rowY + 113, 17, 13, Component.literal("×"),
                ignored -> removeAction(branch.id(), index, rocket)));
    }

    private void addNavigationAddonCard(ScrollWidget scroll, SpaceSimulation.FlightPlanBranch branch,
                                        SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket,
                                        int cursorX, int rowY) {
        scroll.addChild(new AddonConnectorWidget(cursorX + 66, rowY + 130, rowY + 135));
        if (action.addons().isEmpty()) {
            scroll.addChild(SpaceAgeButtons.orangePanel(cursorX + 54, rowY + 135, 24, 20,
                    Component.literal("+"), ignored -> addNavigationAddon(branch.id(), action.id(), rocket))
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.add_completion_condition")));
            return;
        }
        var addon = action.addons().getFirst();
        var button = SpaceAgeButtons.darkPanel(cursorX + 5, rowY + 135, 99, 20,
                addonSummary(addon), ignored -> openAddonEditor(action.id()));
        var moment = calculatedFlight.navigationAborts().stream()
                .filter(item -> item.addonId().equals(addon.id())).findFirst().orElse(null);
        if (moment != null) button.withTooltip(Component.translatable(
                "screen.oritech_space_age.action.condition_reached", formatAddonValue(addon.type(), moment.actualValue())));
        scroll.addChild(button);
        scroll.addChild(SpaceAgeButtons.darkPanel(cursorX + 107, rowY + 135, 18, 20,
                Component.literal("×"), ignored -> removeNavigationAddon(branch.id(), action.id(), rocket)));
    }

    private void buildSpeedEditor() {
        var popupWidth = Math.min(280, panelWidth - 12);
        var px = (panelWidth - popupWidth) / 2;
        var py = Math.max(4, (panelHeight - 112) / 2);
        addEditorBackdrop(px, py, popupWidth, 112);
        addComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 30,
                Component.translatable("screen.oritech_space_age.action.speed_help"))
                .withWrap(true).withZIndex(9_001));
        speedField = addRenderableWidget(new EditBox(font, leftPos + px + 10, topPos + py + 42,
                popupWidth - 20, 18, Component.translatable("screen.oritech_space_age.action.speed_title")));
        speedField.setMaxLength(20);
        speedField.setValue(speedText);
        speedField.setTextColor(parseSpeedLimit(speedText) != null ? 0xFFFFFFFF : 0xFFFF6666);
        var apply = SpaceAgeButtons.panel(px + 10, py + 84, 100, 18, Component.translatable("gui.done"),
                ignored -> applySpeedLimit());
        apply.setZIndex(9_001);
        apply.setActive(parseSpeedLimit(speedText) != null);
        addComponent(apply);
        speedField.setResponder(value -> {
            speedText = value;
            var valid = parseSpeedLimit(value) != null;
            speedField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
            apply.setActive(valid);
        });
        addComponent(SpaceAgeButtons.panel(px + popupWidth - 110, py + 84, 100, 18,
                Component.translatable("gui.cancel"), ignored -> closeSpeedEditor()).withZIndex(9_001));
        setFocused(speedField);
    }

    private void buildActionTypeMenu() {
        var popupWidth = Math.min(210, panelWidth - 12);
        int popupHeight = 18 + EDITABLE_ACTION_TYPES.length * 22;
        var px = Math.clamp(dropdownX, 6, Math.max(6, panelWidth - popupWidth - 6));
        var py = Math.clamp(dropdownY, 6, Math.max(6, panelHeight - popupHeight - 6));
        addContextMenuBackdrop();
        addComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(9_000));
        addComponent(new LabelWidget(px + 10, py + 7, popupWidth - 20,
                Component.translatable("screen.oritech_space_age.action.choose_type"))
                .withBrightColor().withZIndex(9_001));
        var editedAction = findAction(actionTypeAction);
        var editedBranch = draftPlan.branches().stream()
                .filter(branch -> branch.actions().stream().anyMatch(action -> action.id().equals(actionTypeAction)))
                .findFirst().orElse(null);
        boolean canConnectAsteroid = editedAction != null && editedBranch != null
                && !RocketFlightPlanRules.asteroidAnchorSegments(flightPlanRocket).isEmpty()
                && asteroidArrivalBefore(editedBranch, editedAction.id()) != null
                && connectedAsteroidBefore(editedBranch, editedAction.id()) == null;
        for (int index = 0; index < EDITABLE_ACTION_TYPES.length; index++) {
            var type = EDITABLE_ACTION_TYPES[index];
            var button = SpaceAgeButtons.panel(px + 10, py + 18 + index * 22, popupWidth - 20, 18,
                    actionName(type), ignored -> selectActionType(type));
            button.setActive(type != SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || editedAction != null && editedAction.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || canConnectAsteroid);
            addComponent(button.withZIndex(9_001));
        }
    }

    private void buildTargetMenu() {
        var popupWidth = Math.min(310, panelWidth - 12);
        var popupHeight = Math.min(250, panelHeight - 12);
        var px = Math.clamp(dropdownX, 6, Math.max(6, panelWidth - popupWidth - 6));
        var py = Math.clamp(dropdownY, 6, Math.max(6, panelHeight - popupHeight - 6));
        addContextMenuBackdrop();
        addComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(9_000));
        addComponent(new LabelWidget(px + 10, py + 8, popupWidth - 42,
                Component.translatable("screen.oritech_space_age.action.choose_target"))
                .withBrightColor().withZIndex(9_001));
        addComponent(SpaceAgeButtons.darkPanel(px + popupWidth - 28, py + 6, 18, 16, Component.literal("×"),
                ignored -> closeEditors()).withZIndex(9_001));
        targetSearchField = addRenderableWidget(new EditBox(font, leftPos + px + 10, topPos + py + 27,
                popupWidth - 20, 18, Component.translatable("screen.oritech_space_age.action.search_targets")));
        targetSearchField.setMaxLength(40);
        targetSearchField.setValue(targetSearch);
        targetList = new ScrollWidget(px + 10, py + 50, popupWidth - 20, popupHeight - 60)
                .withVerticalScroll(true).withHorizontalScroll(false).withScrollSpeed(20);
        targetList.setZIndex(9_001);
        targetOptions.clear();
        var objects = currentDraftSnapshot().objects().stream()
                .filter(object -> {
                    var action = findAction(targetAction);
                    return action == null || action.type() != SpaceSimulation.ActionType.CONNECT_ASTEROID
                            || object.type() == SpaceObjects.ObjectType.ASTEROID;
                })
                .sorted(Comparator.comparing(object -> RocketStarMapWidget.objectName(object).getString(),
                        String.CASE_INSENSITIVE_ORDER)).toList();
        for (var object : objects) {
            var button = SpaceAgeButtons.panel(4, 0, popupWidth - 36, 18,
                    RocketStarMapWidget.objectName(object), ignored -> selectNavigationTarget(object));
            targetList.addChild(button);
            targetOptions.add(new TargetOption(button,
                    RocketStarMapWidget.objectName(object).getString().toLowerCase(Locale.ROOT)));
        }
        filterTargetOptions();
        addComponent(targetList);
        targetSearchField.setResponder(value -> {
            targetSearch = value;
            filterTargetOptions();
        });
        setFocused(targetSearchField);
    }

    private void filterTargetOptions() {
        if (targetList == null) return;
        var query = targetSearch.strip().toLowerCase(Locale.ROOT);
        int y = 4;
        for (var option : targetOptions) {
            boolean visible = query.isEmpty() || option.searchText.contains(query);
            option.button.setVisible(visible);
            if (visible) {
                option.button.setY(y);
                y += 21;
            }
        }
        targetList.setContentDimensions(targetList.getWidth() - 8, Math.max(targetList.getHeight() - 8, y + 3));
    }

    private void buildArrivalEditor() {
        var popupWidth = Math.min(290, panelWidth - 12);
        var popupHeight = 132;
        var px = (panelWidth - popupWidth) / 2;
        var py = Math.max(4, (panelHeight - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        addComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 24,
                Component.translatable("screen.oritech_space_age.action.arrival_help"))
                .withWrap(true).withBrightColor().withZIndex(9_001));
        addComponent(SpaceAgeButtons.panel(px + 10, py + 35, (popupWidth - 24) / 2, 18,
                Component.translatable("screen.oritech_space_age.action.velocity_zero"),
                ignored -> applyArrival(SpaceSimulation.ArrivalVelocityMode.ZERO, 0)).withZIndex(9_001));
        addComponent(SpaceAgeButtons.panel(px + 14 + (popupWidth - 24) / 2, py + 35,
                (popupWidth - 24) / 2, 18,
                Component.translatable("screen.oritech_space_age.action.velocity_maximum"),
                ignored -> applyArrival(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0)).withZIndex(9_001));
        addComponent(new LabelWidget(px + 10, py + 64, 70,
                Component.translatable("screen.oritech_space_age.action.velocity_custom_title"))
                .withBrightColor().withZIndex(9_001));
        arrivalField = addRenderableWidget(new EditBox(font, leftPos + px + 82, topPos + py + 59,
                popupWidth - 92, 18, Component.translatable("screen.oritech_space_age.action.velocity_custom_title")));
        arrivalField.setMaxLength(10);
        arrivalField.setValue(arrivalText);
        var apply = SpaceAgeButtons.panel(px + 10, py + 104, 100, 18, Component.translatable("gui.done"),
                ignored -> applyCustomArrival());
        apply.setZIndex(9_001);
        apply.setActive(parseArrivalVelocity(arrivalText) != null);
        addComponent(apply);
        arrivalField.setResponder(value -> {
            arrivalText = value;
            var valid = parseArrivalVelocity(value) != null;
            arrivalField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
            apply.setActive(valid);
        });
        addComponent(SpaceAgeButtons.panel(px + popupWidth - 110, py + 104, 100, 18,
                Component.translatable("gui.cancel"), ignored -> closeEditors()).withZIndex(9_001));
        setFocused(arrivalField);
    }

    private void buildLandingEditor() {
        var popupWidth = Math.min(300, panelWidth - 12);
        var popupHeight = 126;
        var px = (panelWidth - popupWidth) / 2;
        var py = Math.max(4, (panelHeight - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        addComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 24,
                Component.translatable("screen.oritech_space_age.action.landing_help"))
                .withWrap(true).withBrightColor().withZIndex(9_001));
        addComponent(new LabelWidget(px + 10, py + 43, 18, Component.literal("X"))
                .withBrightColor().withZIndex(9_001));
        landingXField = addRenderableWidget(new EditBox(font, leftPos + px + 28, topPos + py + 38,
                100, 18, Component.literal("X")));
        landingXField.setValue(landingXText);
        addComponent(new LabelWidget(px + 150, py + 43, 18, Component.literal("Z"))
                .withBrightColor().withZIndex(9_001));
        landingZField = addRenderableWidget(new EditBox(font, leftPos + px + 168, topPos + py + 38,
                100, 18, Component.literal("Z")));
        landingZField.setValue(landingZText);
        var apply = SpaceAgeButtons.panel(px + 10, py + 94, 88, 18, Component.translatable("gui.done"),
                ignored -> applyLandingCoordinates());
        apply.setZIndex(9_001);
        addComponent(apply);
        addComponent(SpaceAgeButtons.panel(px + 106, py + 94, 88, 18,
                Component.translatable("screen.oritech_space_age.action.next_orbit"), ignored -> nextLandingOrbit())
                .withZIndex(9_001));
        addComponent(SpaceAgeButtons.panel(px + 202, py + 94, 88, 18,
                Component.translatable("gui.cancel"), ignored -> closeEditors()).withZIndex(9_001));
        landingXField.setResponder(value -> landingXText = value);
        landingZField.setResponder(value -> landingZText = value);
        setFocused(landingXField);
    }

    private void buildAddonEditor() {
        var action = findAction(addonAction);
        if (action == null || action.addons().isEmpty()) return;
        var addon = action.addons().getFirst();
        var popupWidth = Math.min(350, panelWidth - 12);
        var popupHeight = 144;
        var px = (panelWidth - popupWidth) / 2;
        var py = Math.max(4, (panelHeight - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        addComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 24,
                Component.translatable("screen.oritech_space_age.action.completion_help"))
                .withWrap(true).withBrightColor().withZIndex(9_001));
        int typeWidth = (popupWidth - 28) / 3;
        var types = SpaceSimulation.ActionAddonType.values();
        for (int index = 0; index < types.length; index++) {
            var type = types[index];
            var button = SpaceAgeButtons.panel(px + 10 + index * (typeWidth + 4), py + 38, typeWidth, 28,
                    addonTypeName(type), ignored -> selectAddonType(type));
            button.setActive(type != addon.type());
            addComponent(button.withZIndex(9_001));
        }
        addComponent(new LabelWidget(px + 10, py + 79, 72,
                Component.translatable("screen.oritech_space_age.action.condition_value"))
                .withBrightColor().withZIndex(9_001));
        addonValueField = addRenderableWidget(new EditBox(font, leftPos + px + 82, topPos + py + 74,
                popupWidth - 172, 18, Component.translatable("screen.oritech_space_age.action.condition_value")));
        addonValueField.setMaxLength(10);
        addonValueField.setValue(addonValueText);
        addComponent(new LabelWidget(px + popupWidth - 82, py + 79, 72, addonUnit(addon.type()))
                .withBrightColor().withZIndex(9_001));
        var apply = SpaceAgeButtons.panel(px + 10, py + 116, 100, 18, Component.translatable("gui.done"),
                ignored -> applyAddonValue());
        apply.setZIndex(9_001);
        apply.setActive(parseAddonValue(addon.type(), addonValueText) != null);
        addComponent(apply);
        addComponent(SpaceAgeButtons.panel(px + popupWidth - 110, py + 116, 100, 18,
                Component.translatable("gui.cancel"), ignored -> closeEditors()).withZIndex(9_001));
        addonValueField.setTextColor(parseAddonValue(addon.type(), addonValueText) == null
                ? 0xFFFF6666 : 0xFFFFFFFF);
        addonValueField.setResponder(value -> {
            addonValueText = value;
            var valid = parseAddonValue(addon.type(), value) != null;
            addonValueField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
            apply.setActive(valid);
        });
        setFocused(addonValueField);
    }

    private void buildMapContextMenu() {
        int popupWidth = 176;
        int popupHeight = 30;
        int px = Math.clamp(mapContextRequest.mouseX(), 6, Math.max(6, panelWidth - popupWidth - 6));
        int py = Math.clamp(mapContextRequest.mouseY(), 6, Math.max(6, panelHeight - popupHeight - 6));
        addContextMenuBackdrop();
        addComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(9_000));
        addComponent(SpaceAgeButtons.panel(px + 6, py + 6, popupWidth - 12, 18,
                Component.translatable("screen.oritech_space_age.action.add_navigation_target"),
                ignored -> addNavigationTargetFromMap()).withZIndex(9_001));
    }

    private void addEditorBackdrop(int px, int py, int popupWidth, int popupHeight) {
        var backdrop = new UIComponent(0, 0, panelWidth, panelHeight) {
            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                graphics.fill(0, 0, width, height, 0x99000000);
                OritechSurface.PANEL_DARK.render(graphics, px, py, popupWidth, popupHeight);
            }
            @Override
            public boolean handleClick(double mouseX, double mouseY, int button) {
                return true;
            }
            @Override
            public boolean handleMouseScroll(double mouseX, double mouseY, double delta) {
                return true;
            }
        };
        backdrop.setZIndex(9_000);
        addComponent(backdrop);
    }

    private void addContextMenuBackdrop() {
        var backdrop = new UIComponent(0, 0, panelWidth, panelHeight) {
            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            }

            @Override
            public boolean handleClick(double mouseX, double mouseY, int button) {
                closeEditors();
                return true;
            }
        };
        backdrop.setZIndex(8_999);
        addComponent(backdrop);
    }

    private static Integer parseSpeedLimit(String text) {
        var value = text.strip().toLowerCase(Locale.ROOT);
        if (value.equals("max") || value.equals("maximum")) return 0;
        if (value.endsWith("m/s")) value = value.substring(0, value.length() - 3).strip();
        try {
            var speed = Integer.parseInt(value);
            return speed > 0 && speed <= 100_000 ? speed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void applySpeedLimit() {
        var value = parseSpeedLimit(speedField.getValue());
        var action = findAction(speedAction);
        if (value == null || action == null) return;
        var branch = draftPlan.branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElseThrow();
        speedAction = null;
        replaceAction(branch.id(), action.id(), action.withMaxSpeed(value), flightPlanRocket);
        rebuildComponents();
    }

    private void closeSpeedEditor() {
        closeEditors();
    }

    private static Integer parseArrivalVelocity(String text) {
        var value = text.strip().toLowerCase(Locale.ROOT);
        if (value.endsWith("m/s")) value = value.substring(0, value.length() - 3).strip();
        try {
            var speed = Integer.parseInt(value);
            return speed >= 0 && speed <= 100_000 ? speed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void openActionTypeMenu(UUID actionId, int contentX, int contentY) {
        closeEditorState();
        actionTypeAction = actionId;
        setDropdownPosition(contentX, contentY);
        rebuildComponents();
    }

    private void selectActionType(SpaceSimulation.ActionType type) {
        var action = findAction(actionTypeAction);
        if (action == null) return;
        var branch = draftPlan.branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        var changed = action.withType(type);
        if (type == SpaceSimulation.ActionType.NAVIGATE_TO) {
            changed = changed.withTarget(selectedTarget.objectId()).withOrbit(selectedTarget.orbit());
            if (isEarthSurface(changed)) {
                var offset = landingOffset(changed);
                changed = changed.withLanding(0, 0, offset[0], offset[1]);
            }
        } else if (type == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            var anchors = RocketFlightPlanRules.asteroidAnchorSegments(flightPlanRocket);
            var arrival = asteroidArrivalBefore(branch, action.id());
            var asteroid = arrival == null ? selectedObject() : currentDraftSnapshot().objects().stream()
                    .filter(object -> object.id().equals(arrival.targetId())).findFirst().orElse(null);
            if (anchors.isEmpty()) return;
            if (asteroid == null || asteroid.type() != SpaceObjects.ObjectType.ASTEROID) {
                asteroid = currentDraftSnapshot().objects().stream()
                        .filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID).findFirst().orElse(null);
            }
            if (asteroid == null) return;
            var orbit = arrival == null ? RocketFlightPlanRules.compatibleOrbit(asteroid.type(), selectedTarget.orbit())
                    : arrival.orbit();
            changed = changed.withTarget(asteroid.id()).withOrbit(orbit).withSegments(List.of(anchors.getFirst()));
        } else if (type == SpaceSimulation.ActionType.DECOUPLE) {
            var connected = connectedAsteroidBefore(branch, action.id());
            if (connected != null) {
                changed = changed.withTarget(connected.targetId()).withOrbit(connected.orbit())
                        .withSegments(connected.segments());
            } else {
                var pairs = connectedPairs(flightPlanRocket);
                if (!pairs.isEmpty()) changed = changed.withSegments(pairs.getFirst());
            }
        }
        closeEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket);
        rebuildComponents();
    }

    private void openTargetMenu(UUID actionId, int contentX, int contentY) {
        closeEditorState();
        targetAction = actionId;
        targetSearch = "";
        setDropdownPosition(contentX, contentY);
        rebuildComponents();
    }

    private void setDropdownPosition(int contentX, int contentY) {
        dropdownX = flightPlanActionScroll.getX() + 4 + contentX - Math.round(flightPlanActionScroll.getScrollX());
        dropdownY = flightPlanActionScroll.getY() + 4 + contentY - Math.round(flightPlanActionScroll.getScrollY());
    }

    private void selectNavigationTarget(SpaceSimulation.SpaceObjectData target) {
        var action = findAction(targetAction);
        if (action == null) return;
        var branch = draftPlan.branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        var changed = action.withTarget(target.id()).withOrbit(
                RocketFlightPlanRules.compatibleOrbit(target.type(), action.orbit()));
        if (isEarthSurface(changed)) {
            var offset = landingOffset(changed);
            changed = changed.withLanding(changed.landingX(), changed.landingZ(), offset[0], offset[1]);
        }
        selectedTarget = new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit());
        closeEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket);
        rebuildComponents();
    }

    private void openLandingEditor(UUID actionId) {
        var action = findAction(actionId);
        if (!isEarthSurface(action)) return;
        closeEditorState();
        landingAction = actionId;
        landingXText = Integer.toString(action.landingX());
        landingZText = Integer.toString(action.landingZ());
        rebuildComponents();
    }

    private void applyLandingCoordinates() {
        var action = findAction(landingAction);
        var x = parseCoordinate(landingXField == null ? "" : landingXField.getValue());
        var z = parseCoordinate(landingZField == null ? "" : landingZField.getValue());
        if (action == null || x == null || z == null) return;
        var branch = draftPlan.branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        var offset = landingOffset(action);
        closeEditorState();
        replaceAction(branch.id(), action.id(), action.withLanding(x, z, offset[0], offset[1]), flightPlanRocket);
        rebuildComponents();
    }

    private void nextLandingOrbit() {
        var action = findAction(landingAction);
        var branch = action == null ? null : draftPlan.branches().stream()
                .filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        closeEditorState();
        cycleActionOrbit(branch.id(), branch.actions().indexOf(action), flightPlanRocket);
        rebuildComponents();
    }

    private static Integer parseCoordinate(String value) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void addNavigationAddon(UUID branchId, UUID actionId, ActiveRocketData rocket) {
        var action = findAction(actionId);
        if (action == null || !action.addons().isEmpty()) return;
        var addon = SpaceSimulation.ActionAddon.create(SpaceSimulation.ActionAddonType.DISTANCE_FROM_TARGET);
        replaceAction(branchId, actionId, action.withAddons(List.of(addon)), rocket);
        addonAction = actionId;
        addonValueText = Integer.toString(addon.value());
        rebuildComponents();
    }

    private void openAddonEditor(UUID actionId) {
        var action = findAction(actionId);
        if (action == null || action.addons().isEmpty()) return;
        closeEditorState();
        addonAction = actionId;
        addonValueText = Integer.toString(action.addons().getFirst().value());
        rebuildComponents();
    }

    private void selectAddonType(SpaceSimulation.ActionAddonType type) {
        var action = findAction(addonAction);
        if (action == null || action.addons().isEmpty()) return;
        var changed = action.addons().getFirst().withType(type);
        var branch = draftPlan.branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        addonValueText = Integer.toString(changed.value());
        replaceAction(branch.id(), action.id(), action.withAddons(List.of(changed)), flightPlanRocket);
        rebuildComponents();
    }

    private void applyAddonValue() {
        var action = findAction(addonAction);
        var value = action == null || action.addons().isEmpty() || addonValueField == null ? null
                : parseAddonValue(action.addons().getFirst().type(), addonValueField.getValue());
        if (action == null || action.addons().isEmpty() || value == null) return;
        var branch = draftPlan.branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        var changed = action.addons().getFirst().withValue(value);
        closeEditorState();
        replaceAction(branch.id(), action.id(), action.withAddons(List.of(changed)), flightPlanRocket);
        rebuildComponents();
    }

    private void removeNavigationAddon(UUID branchId, UUID actionId, ActiveRocketData rocket) {
        var action = findAction(actionId);
        if (action == null) return;
        replaceAction(branchId, actionId, action.withAddons(List.of()), rocket);
    }

    private static Integer parseAddonValue(SpaceSimulation.ActionAddonType type, String value) {
        try {
            int result = Integer.parseInt(value.strip());
            return result > 0 && RocketFlightPlanRules.clampAddonValue(type, result) == result ? result : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int[] landingOffset(SpaceSimulation.FlightPlanAction action) {
        if (action.landingOffsetX() != 0 || action.landingOffsetZ() != 0) {
            return new int[]{action.landingOffsetX(), action.landingOffsetZ()};
        }
        var random = new java.util.Random(action.id().getMostSignificantBits() ^ action.id().getLeastSignificantBits());
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = Math.sqrt(random.nextDouble()) * AsteroidImpactRules.LANDING_UNCERTAINTY_BLOCKS;
        return new int[]{(int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius)};
    }

    private void openArrivalEditor(UUID actionId) {
        var action = findAction(actionId);
        if (action == null || action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) return;
        closeEditorState();
        arrivalAction = actionId;
        arrivalText = Integer.toString(Math.max(0, action.targetVelocity()));
        rebuildComponents();
    }

    private void applyCustomArrival() {
        var velocity = arrivalField == null ? null : parseArrivalVelocity(arrivalField.getValue());
        if (velocity != null) applyArrival(SpaceSimulation.ArrivalVelocityMode.CUSTOM, velocity);
    }

    private void applyArrival(SpaceSimulation.ArrivalVelocityMode mode, int velocity) {
        var action = findAction(arrivalAction);
        if (action == null) return;
        var branch = draftPlan.branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        closeEditorState();
        replaceAction(branch.id(), action.id(), action.withVelocity(mode, velocity), flightPlanRocket);
        rebuildComponents();
    }

    private void addNavigationTargetFromMap() {
        if (mapContextRequest == null || activeBranchId == null) return;
        selectedTarget = mapContextRequest.selection();
        closeEditorState();
        addAction(activeBranchId, flightPlanRocket);
        rebuildComponents();
    }

    private void closeEditorState() {
        speedAction = null;
        actionTypeAction = null;
        targetAction = null;
        arrivalAction = null;
        landingAction = null;
        addonAction = null;
        mapContextRequest = null;
    }

    private void closeEditors() {
        closeEditorState();
        rebuildComponents();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (speedField != null && speedField.isMouseOver(event.x(), event.y())) {
            setFocused(speedField);
            return speedField.mouseClicked(event, doubleClick);
        }
        if (targetSearchField != null && targetSearchField.isMouseOver(event.x(), event.y())) {
            setFocused(targetSearchField);
            return targetSearchField.mouseClicked(event, doubleClick);
        }
        if (arrivalField != null && arrivalField.isMouseOver(event.x(), event.y())) {
            setFocused(arrivalField);
            return arrivalField.mouseClicked(event, doubleClick);
        }
        if (landingXField != null && landingXField.isMouseOver(event.x(), event.y())) {
            setFocused(landingXField);
            return landingXField.mouseClicked(event, doubleClick);
        }
        if (landingZField != null && landingZField.isMouseOver(event.x(), event.y())) {
            setFocused(landingZField);
            return landingZField.mouseClicked(event, doubleClick);
        }
        if (addonValueField != null && addonValueField.isMouseOver(event.x(), event.y())) {
            setFocused(addonValueField);
            return addonValueField.mouseClicked(event, doubleClick);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (targetList != null) {
            double relativeX = mouseX - leftPos;
            double relativeY = mouseY - topPos;
            if (targetList.isMouseOver(relativeX, relativeY)) {
                return targetList.handleMouseScroll(relativeX, relativeY, scrollY);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (speedAction != null) {
            if (event.isEscape()) closeSpeedEditor();
            else if (event.isConfirmation()) applySpeedLimit();
            else if (speedField != null) speedField.keyPressed(event);
            return true;
        }
        if (targetAction != null) {
            if (event.isEscape()) closeEditors();
            else if (targetSearchField != null) targetSearchField.keyPressed(event);
            return true;
        }
        if (arrivalAction != null) {
            if (event.isEscape()) closeEditors();
            else if (event.isConfirmation()) applyCustomArrival();
            else if (arrivalField != null) arrivalField.keyPressed(event);
            return true;
        }
        if (landingAction != null) {
            if (event.isEscape()) closeEditors();
            else if (event.isConfirmation()) applyLandingCoordinates();
            else if (getFocused() == landingZField && landingZField != null) landingZField.keyPressed(event);
            else if (landingXField != null) landingXField.keyPressed(event);
            return true;
        }
        if (addonAction != null) {
            if (event.isEscape()) closeEditors();
            else if (event.isConfirmation()) applyAddonValue();
            else if (addonValueField != null) addonValueField.keyPressed(event);
            return true;
        }
        if (actionTypeAction != null || mapContextRequest != null) {
            if (event.isEscape()) closeEditors();
            return true;
        }
        return super.keyPressed(event);
    }

    private void addAction(UUID branchId, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null || editableActionCount() >= RocketFlightPlanRules.MAX_ACTIONS) return;
        var action = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(selectedTarget.objectId()).withOrbit(selectedTarget.orbit());
        if (isEarthSurface(action)) {
            var offset = landingOffset(action);
            action = action.withLanding(0, 0, offset[0], offset[1]);
        }
        var actions = withoutGenerated(branch.actions());
        actions.add(action);
        updateBranchActions(branchId, actions, rocket);
    }

    private void removeAction(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null || index < 0 || index >= branch.actions().size()
                || branch.actions().get(index).isGenerated()) return;
        var actions = new ArrayList<>(branch.actions());
        actions.remove(index);
        updateBranchActions(branchId, withoutGenerated(actions), rocket);
    }

    private void moveAction(UUID branchId, int index, int direction, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null || branch.actions().get(index).isGenerated()) return;
        var editable = withoutGenerated(branch.actions());
        var action = branch.actions().get(index);
        int current = editable.indexOf(action);
        int target = current + direction;
        if (target < 0 || target >= editable.size()) return;
        Collections.swap(editable, current, target);
        updateBranchActions(branchId, editable, rocket);
    }

    private void cycleActionParameter(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var action = branch.actions().get(index);
        if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            var objects = currentDraftSnapshot().objects();
            if (objects.isEmpty()) return;
            int current = objects.stream().map(SpaceSimulation.SpaceObjectData::id).toList().indexOf(action.targetId());
            var target = objects.get((current + 1) % objects.size());
            var changed = action.withTarget(target.id()).withOrbit(
                    RocketFlightPlanRules.compatibleOrbit(target.type(), action.orbit()));
            selectedTarget = new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit());
            replaceAction(branchId, action.id(), changed, rocket);
        } else if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            var anchors = RocketFlightPlanRules.asteroidAnchorSegments(rocket);
            if (anchors.isEmpty()) return;
            var current = action.segments().isEmpty() ? null : action.segments().getFirst();
            int anchorIndex = anchors.indexOf(current);
            replaceAction(branchId, action.id(), action.withSegments(
                    List.of(anchors.get((anchorIndex + 1) % anchors.size()))), rocket);
        } else if (action.type() == SpaceSimulation.ActionType.DECOUPLE) {
            if (!action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) return;
            var pairs = connectedPairs(rocket);
            if (pairs.isEmpty()) return;
            int current = pairs.indexOf(action.segments());
            replaceAction(branchId, action.id(), action.withSegments(pairs.get((current + 1) % pairs.size())), rocket);
        }
    }

    private void cycleActionOrbit(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var action = branch.actions().get(index);
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO
                && action.type() != SpaceSimulation.ActionType.CONNECT_ASTEROID) return;
        var target = currentDraftSnapshot().objects().stream()
                .filter(object -> object.id().equals(action.targetId())).findFirst().orElse(null);
        if (target == null) return;
        var bands = RocketFlightPlanRules.availableOrbits(target.type());
        int current = bands.indexOf(action.orbit());
        var changed = action.withOrbit(bands.get((current + 1) % bands.size()));
        if (isEarthSurface(changed)) {
            var offset = landingOffset(changed);
            changed = changed.withLanding(changed.landingX(), changed.landingZ(), offset[0], offset[1]);
        }
        selectedTarget = new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit());
        replaceAction(branchId, action.id(), changed, rocket);
    }

    private void adjustActionVelocity(UUID branchId, int index, int direction, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var action = branch.actions().get(index);
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO
                || action.velocityMode() != SpaceSimulation.ArrivalVelocityMode.CUSTOM) return;
        int step = action.targetVelocity() < 1_000 ? 10 : 100;
        int velocity = Math.clamp(action.targetVelocity() + step * direction, 0, 100_000);
        replaceAction(branchId, action.id(), action.withVelocity(action.velocityMode(), velocity), rocket);
    }

    private void replaceAction(UUID branchId, UUID actionId, SpaceSimulation.FlightPlanAction changed,
                               ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var actions = withoutGenerated(branch.actions());
        int index = actions.stream().map(SpaceSimulation.FlightPlanAction::id).toList().indexOf(actionId);
        if (index < 0) return;
        actions.set(index, changed);
        updateBranchActions(branchId, actions, rocket);
    }

    private void updateBranchActions(UUID branchId, List<SpaceSimulation.FlightPlanAction> actions,
                                     ActiveRocketData rocket) {
        var branches = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        for (var branch : draftPlan.branches()) {
            branches.add(branch.id().equals(branchId)
                    ? branch.withActions(RocketFlightPlanRules.preserveBoosterLinks(branch, actions)) : branch);
        }
        draftPlan = RocketFlightPlanRules.normalize(draftPlan.withBranches(branches));
        menu.setDraftFlightPlan(draftPlan);
        flightPlanRocket = rocket;
        refreshFlightPlanner();
    }

    private void refreshFlightPlanner() {
        if (flightPlanRocket == null) return;
        if (flightPlanActionScroll != null) {
            flightPlanActionScrollX = flightPlanActionScroll.getScrollX();
            flightPlanActionScrollY = flightPlanActionScroll.getScrollY();
            removeComponent(flightPlanActionScroll);
        }
        recalculateAndSynchronize();
        if (activeBranchId == null || draftPlan.branches().stream()
                .noneMatch(branch -> branch.id().equals(activeBranchId))) activeBranchId = draftPlan.root().id();
        if (flightPlanMap != null) {
            flightPlanMap.updateFlightPath(currentDraftSnapshot(), calculatedFlight, activeBranchId);
            flightPlanMap.setSelectedTarget(selectedTarget);
        }
        addFlightPlanEditor(flightPlanRocket);
    }

    private void selectBranch(UUID branchId) {
        activeBranchId = branchId;
        refreshFlightPlanner();
    }

    private SpaceSimulation.FlightPlanBranch findBranch(UUID branchId) {
        return draftPlan.branches().stream().filter(branch -> branch.id().equals(branchId)).findFirst().orElse(null);
    }

    private SpaceSimulation.FlightPlanAction findAction(UUID actionId) {
        return draftPlan.branches().stream().flatMap(branch -> branch.actions().stream())
                .filter(action -> action.id().equals(actionId)).findFirst().orElse(null);
    }

    private static int cardWidth(SpaceSimulation.FlightPlanAction action) {
        return action != null && action.isGenerated() ? GENERATED_CARD_WIDTH : NORMAL_CARD_WIDTH;
    }

    private int editableActionCount() {
        return (int) draftPlan.branches().stream().flatMap(branch -> branch.actions().stream())
                .filter(action -> !action.isGenerated()).count();
    }

    private static ArrayList<SpaceSimulation.FlightPlanAction> withoutGenerated(
            List<SpaceSimulation.FlightPlanAction> actions) {
        var result = new ArrayList<SpaceSimulation.FlightPlanAction>();
        actions.stream().filter(action -> !action.isGenerated()).forEach(result::add);
        return result;
    }

    private SpaceSimulation.FlightPlannerSnapshot currentDraftSnapshot() {
        var source = menu.getFlightPlannerSnapshot();
        if (source == null) throw new IllegalStateException("Flight planner snapshot is not loaded");
        return new SpaceSimulation.FlightPlannerSnapshot(
                source.simulationId(), source.rocketId(), source.objects(), draftPlan);
    }

    private void submitFlightPlanIfDirty() {
        RocketAssemblerClientController.submitFlightPlanIfDirty(menu);
    }

    private Component actionName(SpaceSimulation.ActionType type) {
        return Component.translatable("screen.oritech_space_age.action." + type.name().toLowerCase(Locale.ROOT));
    }

    private Component actionParameter(SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket) {
        if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            return currentDraftSnapshot().objects().stream().filter(object -> object.id().equals(action.targetId()))
                    .findFirst().map(RocketStarMapWidget::objectName)
                    .orElse(Component.translatable("screen.oritech_space_age.action.no_target"));
        }
        if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            var asteroid = objectName(action.targetId());
            var segment = action.segments().isEmpty() ? "?" : segmentName(action.segments().getFirst(), rocket);
            return Component.translatable("screen.oritech_space_age.action.asteroid_connection", asteroid, segment);
        }
        if (action.type() == SpaceSimulation.ActionType.DECOUPLE) {
            if (!action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
                return Component.translatable("screen.oritech_space_age.action.release_asteroid",
                        objectName(action.targetId()));
            }
            if (action.segments().size() != 2) {
                return Component.translatable("screen.oritech_space_age.action.no_segment");
            }
            return Component.literal(segmentName(action.segments().get(0), rocket) + " > "
                    + segmentName(action.segments().get(1), rocket));
        }
        return Component.translatable("screen.oritech_space_age.action.no_parameter");
    }

    private Component actionOrbit(SpaceSimulation.FlightPlanAction action) {
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO
                && action.type() != SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            return Component.translatable("screen.oritech_space_age.action.no_scope");
        }
        if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            return Component.translatable("screen.oritech_space_age.action.asteroid_connection_range",
                    (int) AsteroidImpactRules.MAX_ASTEROID_CONNECTION_SPEED);
        }
        if (isEarthSurface(action)) {
            return Component.translatable("screen.oritech_space_age.action.earth_coordinates",
                    action.landingX(), action.landingZ());
        }
        return RocketStarMapWidget.orbitName(action.orbit());
    }

    private Component actionVelocity(SpaceSimulation.FlightPlanAction action) {
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) {
            return Component.translatable("screen.oritech_space_age.action.no_velocity");
        }
        return switch (action.velocityMode()) {
            case ZERO -> Component.translatable("screen.oritech_space_age.action.velocity_zero");
            case MAXIMUM -> Component.translatable("screen.oritech_space_age.action.velocity_maximum");
            case CUSTOM -> Component.translatable("screen.oritech_space_age.action.velocity_custom",
                    action.targetVelocity());
        };
    }

    private static List<SpaceSimulation.SegmentRef> segmentRefs(ActiveRocketData rocket) {
        return rocket.getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of)
                .sorted(Comparator.comparingInt((SpaceSimulation.SegmentRef ref) -> ref.anchor().getY())
                        .thenComparingInt(ref -> ref.anchor().getX())
                        .thenComparingInt(ref -> ref.anchor().getZ())).toList();
    }

    private String segmentName(SpaceSimulation.SegmentRef ref, ActiveRocketData rocket) {
        String customName = draftPlan.configurationFor(ref).name();
        if (!customName.isBlank()) return customName;
        int index = segmentRefs(rocket).indexOf(ref);
        return index < 0 ? "?" : "S" + (index + 1);
    }

    private static List<List<SpaceSimulation.SegmentRef>> connectedPairs(ActiveRocketData rocket) {
        var refsById = new HashMap<UUID, SpaceSimulation.SegmentRef>();
        rocket.getStaticSegments().forEach((id, segment) -> refsById.put(id, SpaceSimulation.SegmentRef.of(segment)));
        var pairs = new ArrayList<List<SpaceSimulation.SegmentRef>>();
        rocket.getStaticSegments().forEach((id, segment) -> segment.getConnectedSegments().forEach(connected -> {
            var first = refsById.get(id);
            var second = refsById.get(connected);
            if (first != null && second != null) pairs.add(List.of(first, second));
        }));
        pairs.sort(Comparator.comparingLong((List<SpaceSimulation.SegmentRef> pair) -> pair.get(0).anchor().asLong())
                .thenComparingLong(pair -> pair.get(1).anchor().asLong()));
        return pairs;
    }

    private void addMessagePanel(Component message) {
        int contentHeight = panelHeight - 51;
        addComponent(new SurfaceWidget(12, 39, panelWidth - 24, contentHeight, OritechSurface.PANEL_INSET));
        var label = new LabelWidget(32, 39 + contentHeight / 2 - 15, panelWidth - 64, 30, message);
        label.withAlignment(LabelWidget.Alignment.CENTER).withBrightColor().withWrap(true);
        addComponent(label);
    }

    private void switchToRocketScreen() {
        submitFlightPlanIfDirty();
        Minecraft.getInstance().setScreen(new RocketAssemblerScreen(menu, screenInventory, screenTitle));
    }

    @Override
    public void onClose() {
        submitFlightPlanIfDirty();
        super.onClose();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        components.forEach(UIComponent::tick);
        if (flightPlanActionScroll != null) {
            flightPlanActionScrollX = flightPlanActionScroll.getScrollX();
            flightPlanActionScrollY = flightPlanActionScroll.getScrollY();
        }
        if (previewRevision != menu.getPreviewRevision()
                || flightPlannerRevision != menu.getFlightPlannerRevision()) rebuildComponents();
    }

    @Override
    public boolean shouldCreateTitle() {
        return false;
    }

    @Override
    public BlockState getTitleState() {
        return SpaceAgeBlocks.ROCKET_ASSEMBLER.get().defaultBlockState();
    }

    private static final class BranchConnectorWidget extends UIComponent {
        private final int startX;
        private final int startY;
        private final int endX;
        private final int endY;

        private BranchConnectorWidget(int startX, int startY, int endX, int endY) {
            super(Math.min(startX, endX), Math.min(startY, endY),
                    Math.max(1, Math.abs(endX - startX)), Math.max(1, Math.abs(endY - startY)));
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int cornerY = Math.min(endY, startY + 7);
            int color = 0xFFD58A32;
            graphics.fill(startX - 1, startY, startX + 1, cornerY + 1, color);
            graphics.fill(Math.min(startX, endX), cornerY - 1, Math.max(startX, endX) + 1, cornerY + 1, color);
            graphics.fill(endX - 1, cornerY, endX + 1, endY, color);
        }
    }

    private static final class AddonConnectorWidget extends UIComponent {
        private final int lineX;
        private final int startY;
        private final int endY;

        private AddonConnectorWidget(int lineX, int startY, int endY) {
            super(lineX - 1, startY, 2, Math.max(1, endY - startY));
            this.lineX = lineX;
            this.startY = startY;
            this.endY = endY;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(lineX - 1, startY, lineX + 1, endY, 0xFFD58A32);
        }
    }

    private SpaceSimulation.SpaceObjectData selectedObject() {
        return currentDraftSnapshot().objects().stream()
                .filter(object -> object.id().equals(selectedTarget.objectId())).findFirst().orElse(null);
    }

    private String objectName(UUID id) {
        return currentDraftSnapshot().objects().stream().filter(object -> object.id().equals(id)).findFirst()
                .map(object -> RocketStarMapWidget.objectName(object).getString()).orElse("?");
    }

    private static boolean isEarthSurface(SpaceSimulation.FlightPlanAction action) {
        return action != null && action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                && action.targetId().equals(SpaceObjects.EARTH_ID)
                && action.orbit() == SpaceSimulation.OrbitBand.SURFACE;
    }

    private RocketFlightPathCalculator.ArrivalPrediction arrivalPrediction(UUID actionId) {
        return calculatedFlight.arrivalPredictions().stream()
                .filter(prediction -> prediction.actionId().equals(actionId)).findFirst().orElse(null);
    }

    private static List<Component> arrivalTooltip(AsteroidImpactRules.ImpactPrediction prediction) {
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("screen.oritech_space_age.arrival_outcome."
                + prediction.outcome().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD));
        lines.add(Component.translatable("screen.oritech_space_age.arrival_relative_speed",
                String.format(Locale.ROOT, "%.1f", prediction.relativeSpeedMetersPerSecond())));
        lines.add(Component.translatable("screen.oritech_space_age.arrival_position",
                prediction.landingX(), prediction.landingZ()));
        if (prediction.craterRadiusBlocks() > 0) {
            lines.add(Component.translatable("screen.oritech_space_age.arrival_crater",
                    prediction.craterRadiusBlocks()));
        }
        if (prediction.fragmentCount() > 0) {
            lines.add(Component.translatable("screen.oritech_space_age.arrival_fragments."
                    + prediction.fragmentationMode().name().toLowerCase(Locale.ROOT), prediction.fragmentCount()));
            for (int index = 0; index < prediction.fragments().size(); index++) {
                var fragment = prediction.fragments().get(index);
                lines.add(Component.translatable("screen.oritech_space_age.arrival_fragment",
                        index + 1, String.format(Locale.ROOT, "%.0f", fragment.mass() * 1_000),
                        String.format(Locale.ROOT, "%.0f", fragment.radius())));
            }
            if (prediction.remainingTargetMass() > 0) {
                lines.add(Component.translatable("screen.oritech_space_age.arrival_remaining_asteroid",
                        String.format(Locale.ROOT, "%.0f", prediction.remainingTargetMass() * 1_000)));
            }
        }
        if (!prediction.recoverableMaterials().isEmpty()) {
            lines.add(Component.translatable("screen.oritech_space_age.arrival_recoverable"));
            prediction.recoverableMaterials().forEach(material -> lines.add(Component.literal(
                    "• " + material.block() + " × " + material.amount())));
        }
        return lines;
    }

    private static List<Component> releasedAsteroidTooltip(RocketFlightPathCalculator.AsteroidPath path) {
        if (path.earthImpact() == null) {
            return List.of(Component.translatable("screen.oritech_space_age.released_asteroid_miss"),
                    Component.translatable("screen.oritech_space_age.action.predicted_uncertainty",
                            path.landingUncertaintyBlocks()));
        }
        var lines = new ArrayList<>(arrivalTooltip(path.earthImpact()));
        lines.add(Component.translatable("screen.oritech_space_age.action.predicted_uncertainty",
                path.landingUncertaintyBlocks()));
        return lines;
    }

    private boolean canAddNavigationAddon(SpaceSimulation.FlightPlanBranch branch,
                                           SpaceSimulation.FlightPlanAction action) {
        var path = calculatedFlight.paths().stream().filter(item -> item.branchId().equals(branch.id()))
                .findFirst().orElse(null);
        if (path == null) return false;
        var moment = path.actionMoments().stream().filter(item -> item.actionId().equals(action.id()))
                .findFirst().orElse(null);
        return moment != null && (moment.connectedSegments().size() > 1
                || !moment.attachedAsteroidId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET));
    }

    private static Component addonTypeName(SpaceSimulation.ActionAddonType type) {
        return Component.translatable("screen.oritech_space_age.action.condition."
                + type.name().toLowerCase(Locale.ROOT));
    }

    private static Component addonUnit(SpaceSimulation.ActionAddonType type) {
        return Component.translatable("screen.oritech_space_age.action.condition_unit."
                + type.name().toLowerCase(Locale.ROOT));
    }

    private static Component addonSummary(SpaceSimulation.ActionAddon addon) {
        return Component.translatable("screen.oritech_space_age.action.condition_summary."
                + addon.type().name().toLowerCase(Locale.ROOT), addon.value());
    }

    private static String formatAddonValue(SpaceSimulation.ActionAddonType type, double value) {
        return switch (type) {
            case DISTANCE_FROM_TARGET -> String.format(Locale.ROOT, "%.0f m", value);
            case TIME_BEFORE_ARRIVAL -> String.format(Locale.ROOT, "%.0f s", value);
            case DESIRED_UNCERTAINTY -> String.format(Locale.ROOT, "±%.0f blocks", value);
        };
    }

    private SpaceSimulation.FlightPlanAction connectedAsteroidBefore(
            SpaceSimulation.FlightPlanBranch branch, UUID beforeAction) {
        SpaceSimulation.FlightPlanAction connected = null;
        for (var action : branch.actions()) {
            if (action.id().equals(beforeAction)) break;
            if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID) connected = action;
            if (action.type() == SpaceSimulation.ActionType.DECOUPLE
                    && !action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) connected = null;
        }
        return connected;
    }

    private SpaceSimulation.FlightPlanAction asteroidArrivalBefore(
            SpaceSimulation.FlightPlanBranch branch, UUID beforeAction) {
        SpaceSimulation.FlightPlanAction arrival = null;
        for (var action : branch.actions()) {
            if (action.id().equals(beforeAction)) break;
            if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) continue;
            var target = currentDraftSnapshot().objects().stream()
                    .filter(object -> object.id().equals(action.targetId())).findFirst().orElse(null);
            arrival = target != null && target.type() == SpaceObjects.ObjectType.ASTEROID
                    && (action.orbit() == SpaceSimulation.OrbitBand.TIGHT
                    || action.orbit() == SpaceSimulation.OrbitBand.SURFACE)
                    && canConnectAfter(action) ? action : null;
        }
        return arrival;
    }

    private static boolean canConnectAfter(SpaceSimulation.FlightPlanAction action) {
        return switch (action.velocityMode()) {
            case ZERO -> true;
            case CUSTOM -> action.targetVelocity() <= AsteroidImpactRules.MAX_ASTEROID_CONNECTION_SPEED;
            case MAXIMUM -> false;
        };
    }

    private record TargetOption(UIComponent button, String searchText) {
    }
}
