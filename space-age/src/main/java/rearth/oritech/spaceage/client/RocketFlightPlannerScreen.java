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
        speedField = null;
        if (speedAction != null) buildSpeedEditor();
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
                this::selectMapTarget);
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
        var rowHeight = 138;
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
                branchX = parentX + cardWidth(parentAction) / 2;
                int guideY = branchStartY + parentRow * rowHeight
                        + (parentAction != null && parentAction.isGenerated() ? 68 : 130);
                scroll.addChild(new BranchConnectorWidget(branchX, guideY, branchX - 2, rowY + 17));
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
                actionName(action.type()), ignored -> cycleActionType(branch.id(), index, rocket)));

        var parameter = SpaceAgeButtons.panel(cursorX + 5, rowY + 27, 120, 17,
                actionParameter(action, rocket), ignored -> cycleActionParameter(branch.id(), index, rocket));
        parameter.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                || action.type() == SpaceSimulation.ActionType.DECOUPLE);
        scroll.addChild(parameter);
        var orbit = SpaceAgeButtons.panel(cursorX + 5, rowY + 48, 120, 17,
                actionOrbit(action), ignored -> cycleActionOrbit(branch.id(), index, rocket));
        orbit.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO);
        scroll.addChild(orbit);

        var velocity = SpaceAgeButtons.panel(cursorX + 5, rowY + 69, 120, 17,
                actionVelocity(action), ignored -> cycleActionVelocity(branch.id(), index, rocket));
        velocity.setActive(action.type() == SpaceSimulation.ActionType.NAVIGATE_TO);
        scroll.addChild(velocity);

        var speed = SpaceAgeButtons.panel(cursorX + 5, rowY + 91, 120, 17,
                Component.translatable("screen.oritech_space_age.action.speed_limit",
                        action.maxSpeed() == 0 ? "max" : action.maxSpeed() + " m/s"), ignored -> {
                    speedAction = action.id();
                    speedText = action.maxSpeed() == 0 ? "max" : Integer.toString(action.maxSpeed());
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

    private static Integer parseSpeedLimit(String text) {
        var value = text.strip().toLowerCase(Locale.ROOT);
        if (value.equals("max")) return 0;
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
        speedAction = null;
        rebuildComponents();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (speedField != null && speedField.isMouseOver(event.x(), event.y())) {
            setFocused(speedField);
            return speedField.mouseClicked(event, doubleClick);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (speedAction != null) {
            if (event.isEscape()) closeSpeedEditor();
            else if (event.isConfirmation()) applySpeedLimit();
            else if (speedField != null) speedField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    private void addAction(UUID branchId, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null || editableActionCount() >= RocketFlightPlanRules.MAX_ACTIONS) return;
        var action = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(selectedTarget.objectId()).withOrbit(selectedTarget.orbit());
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

    private void cycleActionType(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var current = branch.actions().get(index);
        if (current.isGenerated()) return;
        int currentType = java.util.Arrays.asList(EDITABLE_ACTION_TYPES).indexOf(current.type());
        var changed = current.withType(EDITABLE_ACTION_TYPES[(currentType + 1) % EDITABLE_ACTION_TYPES.length]);
        if (changed.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            changed = changed.withTarget(selectedTarget.objectId()).withOrbit(selectedTarget.orbit());
        } else if (changed.type() == SpaceSimulation.ActionType.DECOUPLE) {
            var pairs = connectedPairs(rocket);
            if (!pairs.isEmpty()) changed = changed.withSegments(pairs.getFirst());
        }
        replaceAction(branchId, current.id(), changed, rocket);
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
        } else if (action.type() == SpaceSimulation.ActionType.DECOUPLE) {
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
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) return;
        var target = currentDraftSnapshot().objects().stream()
                .filter(object -> object.id().equals(action.targetId())).findFirst().orElse(null);
        if (target == null) return;
        var bands = RocketFlightPlanRules.availableOrbits(target.type());
        int current = bands.indexOf(action.orbit());
        var changed = action.withOrbit(bands.get((current + 1) % bands.size()));
        selectedTarget = new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit());
        replaceAction(branchId, action.id(), changed, rocket);
    }

    private void cycleActionVelocity(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var action = branch.actions().get(index);
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) return;
        var modes = SpaceSimulation.ArrivalVelocityMode.values();
        var mode = modes[(action.velocityMode().ordinal() + 1) % modes.length];
        int velocity = mode == SpaceSimulation.ArrivalVelocityMode.CUSTOM
                ? Math.max(100, action.targetVelocity()) : 0;
        replaceAction(branchId, action.id(), action.withVelocity(mode, velocity), rocket);
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
                    .findFirst().map(object -> RocketStarMapWidget.objectName(object.type()))
                    .orElse(Component.translatable("screen.oritech_space_age.action.no_target"));
        }
        if (action.type() == SpaceSimulation.ActionType.DECOUPLE) {
            if (action.segments().size() != 2) {
                return Component.translatable("screen.oritech_space_age.action.no_segment");
            }
            return Component.literal(segmentName(action.segments().get(0), rocket) + " > "
                    + segmentName(action.segments().get(1), rocket));
        }
        return Component.translatable("screen.oritech_space_age.action.no_parameter");
    }

    private Component actionOrbit(SpaceSimulation.FlightPlanAction action) {
        return action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                ? RocketStarMapWidget.orbitName(action.orbit())
                : Component.translatable("screen.oritech_space_age.action.no_scope");
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
            int cornerY = Math.min(endY, startY + 5);
            graphics.fill(startX, startY, startX + 1, cornerY, 0xFF405468);
            graphics.fill(Math.min(startX, endX), cornerY - 1, Math.max(startX, endX), cornerY, 0xFF405468);
            graphics.fill(endX, cornerY, endX + 1, endY, 0xFF405468);
        }
    }
}
