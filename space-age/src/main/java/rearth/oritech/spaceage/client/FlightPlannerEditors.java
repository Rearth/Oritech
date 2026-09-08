package rearth.oritech.spaceage.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Owns modal editor state and input routing; FlightPlannerPopups builds the controls. */
abstract class FlightPlannerEditors extends OritechWidgetScreen<RocketAssemblerMenu> {
    // Generated booster cards cannot be selected as a manual action type.
    protected static final SpaceSimulation.ActionType[] EDITABLE_ACTION_TYPES = {
            SpaceSimulation.ActionType.NAVIGATE_TO, SpaceSimulation.ActionType.CONNECT_ASTEROID,
            SpaceSimulation.ActionType.DECOUPLE, SpaceSimulation.ActionType.MAINTAIN_POSITION,
            SpaceSimulation.ActionType.DISCARD_CRAFT
    };

    // Timeline card currently open in the action configuration popup.
    protected UUID actionEditorAction;
    // Cruise-speed popup: edited card, live field and text retained across rebuilds.
    protected UUID speedAction;
    protected EditBox speedField;
    protected String speedText;
    // Card currently choosing a new action type.
    protected UUID actionTypeAction;
    // Target picker: edited card, search text and the currently visible target buttons.
    protected UUID targetAction;
    protected EditBox targetSearchField;
    protected String targetSearch = "";
    protected ScrollWidget targetList;
    protected final List<TargetOption> targetOptions = new ArrayList<>();
    // Arrival-speed popup: edited card, live field and retained text.
    protected UUID arrivalAction;
    protected EditBox arrivalField;
    protected String arrivalText;
    // Landing popup: edited card and the Overworld X/Z input fields and text.
    protected UUID landingAction;
    protected EditBox landingXField;
    protected EditBox landingZField;
    protected String landingXText;
    protected String landingZText;
    // Completion-condition popup: edited card, input field and retained value.
    protected UUID addonAction;
    protected EditBox addonValueField;
    protected String addonValueText;
    // Target and click position for the map's right-click menu.
    protected RocketStarMapWidget.NavigationContextRequest mapContextRequest;
    // Dropdown origin in panel coordinates after accounting for card scrolling.
    protected int dropdownX;
    protected int dropdownY;
    private int popupLayerOffset;

    protected FlightPlannerEditors(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 0, 0);
    }

    // The screen retains mission and map state; the editor only requests it through these hooks.
    protected abstract SpaceSimulation.FlightPlan draftPlan();
    protected abstract RocketFlightPathCalculator.FlightPath calculatedFlight();
    protected abstract ActiveRocketData flightPlanRocket();
    protected abstract ScrollWidget flightPlanActionScroll();
    protected abstract UUID activeBranchId();
    protected abstract RocketStarMapWidget.NavigationSelection selectedTarget();
    protected abstract void setSelectedTarget(RocketStarMapWidget.NavigationSelection target);
    protected abstract int panelWidth();
    protected abstract int panelHeight();

    protected abstract SpaceSimulation.FlightPlanAction findAction(UUID actionId);
    protected abstract SpaceSimulation.FlightPlanBranch findBranch(UUID branchId);
    protected abstract SpaceSimulation.FlightPlannerSnapshot currentDraftSnapshot();
    protected abstract SpaceSimulation.SpaceObjectData selectedObject();
    protected abstract SpaceSimulation.FlightPlanAction connectedAsteroidBefore(SpaceSimulation.FlightPlanBranch branch, UUID actionId);
    protected abstract SpaceSimulation.FlightPlanAction asteroidArrivalBefore(SpaceSimulation.FlightPlanBranch branch, UUID actionId);
    protected abstract List<List<SpaceSimulation.SegmentRef>> connectedPairs(ActiveRocketData rocket);
    protected abstract void replaceAction(UUID branchId, UUID actionId, SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket);
    protected abstract void cycleActionOrbit(UUID branchId, int index, ActiveRocketData rocket);
    protected abstract void cycleActionParameter(UUID branchId, int index, ActiveRocketData rocket);
    protected abstract Component actionParameter(SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket);
    protected abstract Component actionOrbit(SpaceSimulation.FlightPlanAction action);
    protected abstract boolean canAddNavigationAddon(SpaceSimulation.FlightPlanBranch branch,
                                                      SpaceSimulation.FlightPlanAction action);
    protected abstract void addAction(UUID branchId, ActiveRocketData rocket);

    protected void openActionEditor(UUID actionId) {
        closeEditorState();
        actionEditorAction = actionId;
        rebuildComponents();
    }

    protected void openSpeedEditor(SpaceSimulation.FlightPlanAction action) {
        closeNestedEditorState();
        speedAction = action.id();
        speedText = action.maxSpeed() == 0 ? "maximum" : Integer.toString(action.maxSpeed());
        rebuildComponents();
    }

    protected boolean hasOpenPopup() {
        return actionEditorAction != null || speedAction != null || actionTypeAction != null || targetAction != null
                || arrivalAction != null || landingAction != null || addonAction != null
                || mapContextRequest != null;
    }

    protected void buildEditors() {
        // Rebuilding the screen drops vanilla widgets. Keep their text, but replace the widget references.
        speedField = null;
        targetSearchField = null;
        targetList = null;
        targetOptions.clear();
        arrivalField = null;
        landingXField = null;
        landingZField = null;
        addonValueField = null;
        new FlightPlannerPopups(this, font, leftPos, topPos).build();
    }

    void addPopupComponent(UIComponent component) {
        if (popupLayerOffset != 0) component.setZIndex(component.getZIndex() + popupLayerOffset);
        addComponent(component);
    }

    void setPopupLayerOffset(int offset) {
        popupLayerOffset = offset;
    }

    EditBox addPopupField(EditBox field) {
        return addRenderableWidget(field);
    }

    void focusPopup(EditBox field) {
        setFocused(field);
    }

    protected static Integer parseSpeedLimit(String text) {
        return FlightPlannerLabels.parseSpeedLimit(text);
    }

    protected void applySpeedLimit() {
        var value = parseSpeedLimit(speedField.getValue());
        var action = findAction(speedAction);
        if (value == null || action == null) return;
        var branch = draftPlan().branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElseThrow();
        speedAction = null;
        replaceAction(branch.id(), action.id(), action.withMaxSpeed(value), flightPlanRocket());
        rebuildComponents();
    }

    protected void closeSpeedEditor() {
        closeEditors();
    }

    protected static Integer parseArrivalVelocity(String text) {
        return FlightPlannerLabels.parseArrivalVelocity(text);
    }

    protected void openActionTypeMenu(UUID actionId, int contentX, int contentY) {
        closeNestedEditorState();
        actionTypeAction = actionId;
        setDropdownPosition(contentX, contentY);
        rebuildComponents();
    }

    protected void openActionTypeMenuFromEditor(UUID actionId, int panelX, int panelY) {
        closeNestedEditorState();
        actionTypeAction = actionId;
        dropdownX = panelX;
        dropdownY = panelY;
        rebuildComponents();
    }

    protected void selectActionType(SpaceSimulation.ActionType type) {
        var action = findAction(actionTypeAction);
        if (action == null) return;
        var branch = draftPlan().branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        var changed = action.withType(type);
        if (type == SpaceSimulation.ActionType.NAVIGATE_TO) {
            changed = changed.withTarget(selectedTarget().objectId()).withOrbit(selectedTarget().orbit());
            if (FlightPlannerLabels.isEarthSurface(changed)) {
                var offset = landingOffset(changed);
                changed = changed.withLanding(0, 0, offset[0], offset[1]);
            }
        } else if (type == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            var anchors = RocketFlightPlanRules.asteroidAnchorSegments(flightPlanRocket());
            var arrival = asteroidArrivalBefore(branch, action.id());
            var asteroid = arrival == null ? selectedObject() : currentDraftSnapshot().objects().stream()
                    .filter(object -> object.id().equals(arrival.targetId())).findFirst().orElse(null);
            if (anchors.isEmpty()) return;
            if (asteroid == null || asteroid.type() != SpaceObjects.ObjectType.ASTEROID) {
                asteroid = currentDraftSnapshot().objects().stream()
                        .filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID).findFirst().orElse(null);
            }
            if (asteroid == null) return;
            var orbit = arrival == null ? RocketFlightPlanRules.compatibleOrbit(asteroid.type(), selectedTarget().orbit())
                    : arrival.orbit();
            changed = changed.withTarget(asteroid.id()).withOrbit(orbit).withSegments(List.of(anchors.getFirst()));
        } else if (type == SpaceSimulation.ActionType.DECOUPLE) {
            var connected = connectedAsteroidBefore(branch, action.id());
            if (connected != null) {
                changed = changed.withTarget(connected.targetId()).withOrbit(connected.orbit())
                        .withSegments(connected.segments());
            } else {
                var pairs = connectedPairs(flightPlanRocket());
                if (!pairs.isEmpty()) changed = changed.withSegments(pairs.getFirst());
            }
        }
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket());
        rebuildComponents();
    }

    protected void openTargetMenu(UUID actionId, int contentX, int contentY) {
        closeNestedEditorState();
        targetAction = actionId;
        targetSearch = "";
        setDropdownPosition(contentX, contentY);
        rebuildComponents();
    }

    protected void openTargetMenuFromEditor(UUID actionId, int panelX, int panelY) {
        closeNestedEditorState();
        targetAction = actionId;
        targetSearch = "";
        dropdownX = panelX;
        dropdownY = panelY;
        rebuildComponents();
    }

    protected void setDropdownPosition(int contentX, int contentY) {
        dropdownX = flightPlanActionScroll().getX() + 4 + contentX - Math.round(flightPlanActionScroll().getScrollX());
        dropdownY = flightPlanActionScroll().getY() + 4 + contentY - Math.round(flightPlanActionScroll().getScrollY());
    }

    protected void selectNavigationTarget(SpaceSimulation.SpaceObjectData target) {
        var action = findAction(targetAction);
        if (action == null) return;
        var branch = draftPlan().branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        var changed = action.withTarget(target.id()).withOrbit(
                RocketFlightPlanRules.compatibleOrbit(target.type(), action.orbit()));
        if (FlightPlannerLabels.isEarthSurface(changed)) {
            var offset = landingOffset(changed);
            changed = changed.withLanding(changed.landingX(), changed.landingZ(), offset[0], offset[1]);
        }
        setSelectedTarget(new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit()));
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket());
        rebuildComponents();
    }

    protected void openLandingEditor(UUID actionId) {
        var action = findAction(actionId);
        if (!FlightPlannerLabels.isEarthSurface(action)) return;
        closeNestedEditorState();
        landingAction = actionId;
        landingXText = Integer.toString(action.landingX());
        landingZText = Integer.toString(action.landingZ());
        rebuildComponents();
    }

    protected void applyLandingCoordinates() {
        var action = findAction(landingAction);
        var x = parseCoordinate(landingXField == null ? "" : landingXField.getValue());
        var z = parseCoordinate(landingZField == null ? "" : landingZField.getValue());
        if (action == null || x == null || z == null) return;
        var branch = draftPlan().branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        var offset = landingOffset(action);
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), action.withLanding(x, z, offset[0], offset[1]), flightPlanRocket());
        rebuildComponents();
    }

    protected void nextLandingOrbit() {
        var action = findAction(landingAction);
        var branch = action == null ? null : draftPlan().branches().stream()
                .filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        closeNestedEditorState();
        cycleActionOrbit(branch.id(), branch.actions().indexOf(action), flightPlanRocket());
        rebuildComponents();
    }

    protected static Integer parseCoordinate(String value) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    protected void addNavigationAddon(UUID branchId, UUID actionId, ActiveRocketData rocket) {
        var action = findAction(actionId);
        if (action == null || !action.addons().isEmpty()) return;
        var addon = SpaceSimulation.ActionAddon.create(SpaceSimulation.ActionAddonType.DISTANCE_FROM_TARGET);
        replaceAction(branchId, actionId, action.withAddons(List.of(addon)), rocket);
        addonAction = actionId;
        addonValueText = Integer.toString(addon.value());
        rebuildComponents();
    }

    protected void openAddonEditor(UUID actionId) {
        var action = findAction(actionId);
        if (action == null || action.addons().isEmpty()) return;
        closeNestedEditorState();
        addonAction = actionId;
        addonValueText = Integer.toString(action.addons().getFirst().value());
        rebuildComponents();
    }

    protected void selectAddonType(SpaceSimulation.ActionAddonType type) {
        var action = findAction(addonAction);
        if (action == null || action.addons().isEmpty()) return;
        var changed = action.addons().getFirst().withType(type);
        var branch = draftPlan().branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        addonValueText = Integer.toString(changed.value());
        replaceAction(branch.id(), action.id(), action.withAddons(List.of(changed)), flightPlanRocket());
        rebuildComponents();
    }

    protected void applyAddonValue() {
        var action = findAction(addonAction);
        var value = action == null || action.addons().isEmpty() || addonValueField == null ? null
                : parseAddonValue(action.addons().getFirst().type(), addonValueField.getValue());
        if (action == null || action.addons().isEmpty() || value == null) return;
        var branch = draftPlan().branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        var changed = action.addons().getFirst().withValue(value);
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), action.withAddons(List.of(changed)), flightPlanRocket());
        rebuildComponents();
    }

    protected void removeNavigationAddon(UUID branchId, UUID actionId, ActiveRocketData rocket) {
        var action = findAction(actionId);
        if (action == null) return;
        replaceAction(branchId, actionId, action.withAddons(List.of()), rocket);
    }

    protected void editNavigationAddon(UUID actionId) {
        var action = findAction(actionId);
        var branch = action == null ? null : draftPlan().branches().stream()
                .filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        if (action.addons().isEmpty()) addNavigationAddon(branch.id(), action.id(), flightPlanRocket());
        else openAddonEditor(action.id());
    }

    protected void removeEditedNavigationAddon() {
        var action = findAction(addonAction);
        var branch = action == null ? null : draftPlan().branches().stream()
                .filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        closeNestedEditorState();
        removeNavigationAddon(branch.id(), action.id(), flightPlanRocket());
        rebuildComponents();
    }

    protected void cycleEditedOrbit(UUID actionId) {
        var action = findAction(actionId);
        var branch = action == null ? null : draftPlan().branches().stream()
                .filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        cycleActionOrbit(branch.id(), branch.actions().indexOf(action), flightPlanRocket());
        rebuildComponents();
    }

    protected void cycleEditedParameter(UUID actionId) {
        var action = findAction(actionId);
        var branch = action == null ? null : draftPlan().branches().stream()
                .filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        cycleActionParameter(branch.id(), branch.actions().indexOf(action), flightPlanRocket());
        rebuildComponents();
    }

    protected static Integer parseAddonValue(SpaceSimulation.ActionAddonType type, String value) {
        return FlightPlannerLabels.parseAddonValue(type, value);
    }

    protected static int[] landingOffset(SpaceSimulation.FlightPlanAction action) {
        if (action.landingOffsetX() != 0 || action.landingOffsetZ() != 0) {
            return new int[]{action.landingOffsetX(), action.landingOffsetZ()};
        }
        var random = new java.util.Random(action.id().getMostSignificantBits() ^ action.id().getLeastSignificantBits());
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = Math.sqrt(random.nextDouble()) * AsteroidImpactRules.LANDING_UNCERTAINTY_BLOCKS;
        return new int[]{(int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius)};
    }

    protected void openArrivalEditor(UUID actionId) {
        var action = findAction(actionId);
        if (action == null || action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) return;
        closeNestedEditorState();
        arrivalAction = actionId;
        arrivalText = Integer.toString(Math.max(0, action.targetVelocity()));
        rebuildComponents();
    }

    protected void applyCustomArrival() {
        var velocity = arrivalField == null ? null : parseArrivalVelocity(arrivalField.getValue());
        if (velocity != null) applyArrival(SpaceSimulation.ArrivalVelocityMode.CUSTOM, velocity);
    }

    protected void applyArrival(SpaceSimulation.ArrivalVelocityMode mode, int velocity) {
        var action = findAction(arrivalAction);
        if (action == null) return;
        var branch = draftPlan().branches().stream().filter(item -> item.actions().contains(action)).findFirst().orElse(null);
        if (branch == null) return;
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), action.withVelocity(mode, velocity), flightPlanRocket());
        rebuildComponents();
    }

    protected void addNavigationTargetFromMap() {
        if (mapContextRequest == null || activeBranchId() == null) return;
        setSelectedTarget(mapContextRequest.selection());
        closeEditorState();
        addAction(activeBranchId(), flightPlanRocket());
        rebuildComponents();
    }

    protected void closeEditorState() {
        actionEditorAction = null;
        closeNestedEditorState();
        mapContextRequest = null;
    }

    private void closeNestedEditorState() {
        speedAction = null;
        actionTypeAction = null;
        targetAction = null;
        arrivalAction = null;
        landingAction = null;
        addonAction = null;
    }

    protected void closeEditors() {
        if (actionEditorAction != null && (speedAction != null || actionTypeAction != null || targetAction != null
                || arrivalAction != null || landingAction != null || addonAction != null)) {
            closeNestedEditorState();
        } else closeEditorState();
        rebuildComponents();
    }

    protected void closeActionEditor() {
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
        if (actionEditorAction != null) {
            if (event.isEscape()) closeActionEditor();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Target button and its normalized search text, rebuilt together with the popup. */
    protected record TargetOption(UIComponent button, String searchText) { }
}
