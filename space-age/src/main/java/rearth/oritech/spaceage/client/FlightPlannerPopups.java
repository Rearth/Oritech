package rearth.oritech.spaceage.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.Comparator;
import java.util.Locale;

/** Builds popup controls; their text, selected card and input handling stay in the editor. */
final class FlightPlannerPopups {
    // Persistent editor state and the operations invoked by the popup buttons.
    private final FlightPlannerEditors editors;
    // Current font and panel origin; vanilla text fields use absolute screen coordinates.
    private final Font font;
    private final int leftPos;
    private final int topPos;

    FlightPlannerPopups(FlightPlannerEditors editors, Font font, int leftPos, int topPos) {
        this.editors = editors;
        this.font = font;
        this.leftPos = leftPos;
        this.topPos = topPos;
    }

    void build() {
        if (editors.speedAction != null) buildSpeedEditor();
        else if (editors.actionTypeAction != null) buildActionTypeMenu();
        else if (editors.targetAction != null) buildTargetMenu();
        else if (editors.arrivalAction != null) buildArrivalEditor();
        else if (editors.landingAction != null) buildLandingEditor();
        else if (editors.addonAction != null) buildAddonEditor();
        else if (editors.mapContextRequest != null) buildMapContextMenu();
    }

    private void buildSpeedEditor() {
        var popupWidth = Math.min(280, editors.panelWidth() - 12);
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - 112) / 2);
        addEditorBackdrop(px, py, popupWidth, 112);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 30,
                Component.translatable("screen.oritech_space_age.action.speed_help"))
                .withWrap(true).withZIndex(9_001));
        editors.speedField = editors.addPopupField(new EditBox(font, leftPos + px + 10, topPos + py + 42,
                popupWidth - 20, 18, Component.translatable("screen.oritech_space_age.action.speed_title")));
        editors.speedField.setMaxLength(20);
        editors.speedField.setValue(editors.speedText);
        editors.speedField.setTextColor(editors.parseSpeedLimit(editors.speedText) != null ? 0xFFFFFFFF : 0xFFFF6666);
        var apply = SpaceAgeButtons.panel(px + 10, py + 84, 100, 18, Component.translatable("gui.done"),
                ignored -> editors.applySpeedLimit());
        apply.setZIndex(9_001);
        apply.setActive(editors.parseSpeedLimit(editors.speedText) != null);
        editors.addPopupComponent(apply);
        editors.speedField.setResponder(value -> {
            editors.speedText = value;
            var valid = editors.parseSpeedLimit(value) != null;
            editors.speedField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
            apply.setActive(valid);
        });
        editors.addPopupComponent(SpaceAgeButtons.panel(px + popupWidth - 110, py + 84, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeSpeedEditor()).withZIndex(9_001));
        editors.focusPopup(editors.speedField);
    }

    private void buildActionTypeMenu() {
        var popupWidth = Math.min(210, editors.panelWidth() - 12);
        int popupHeight = 18 + editors.EDITABLE_ACTION_TYPES.length * 22;
        var px = Math.clamp(editors.dropdownX, 6, Math.max(6, editors.panelWidth() - popupWidth - 6));
        var py = Math.clamp(editors.dropdownY, 6, Math.max(6, editors.panelHeight() - popupHeight - 6));
        addContextMenuBackdrop();
        editors.addPopupComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(9_000));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 7, popupWidth - 20,
                Component.translatable("screen.oritech_space_age.action.choose_type"))
                .withBrightColor().withZIndex(9_001));
        var editedAction = editors.findAction(editors.actionTypeAction);
        var editedBranch = editors.draftPlan().branches().stream()
                .filter(branch -> branch.actions().stream().anyMatch(action -> action.id().equals(editors.actionTypeAction)))
                .findFirst().orElse(null);
        boolean canConnectAsteroid = editedAction != null && editedBranch != null
                && !RocketFlightPlanRules.asteroidAnchorSegments(editors.flightPlanRocket()).isEmpty()
                && editors.asteroidArrivalBefore(editedBranch, editedAction.id()) != null
                && editors.connectedAsteroidBefore(editedBranch, editedAction.id()) == null;
        for (int index = 0; index < editors.EDITABLE_ACTION_TYPES.length; index++) {
            var type = editors.EDITABLE_ACTION_TYPES[index];
            var button = SpaceAgeButtons.panel(px + 10, py + 18 + index * 22, popupWidth - 20, 18,
                    FlightPlannerLabels.actionName(type), ignored -> editors.selectActionType(type));
            button.setActive(type != SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || editedAction != null && editedAction.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || canConnectAsteroid);
            editors.addPopupComponent(button.withZIndex(9_001));
        }
    }

    private void buildTargetMenu() {
        var popupWidth = Math.min(310, editors.panelWidth() - 12);
        var popupHeight = Math.min(250, editors.panelHeight() - 12);
        var px = Math.clamp(editors.dropdownX, 6, Math.max(6, editors.panelWidth() - popupWidth - 6));
        var py = Math.clamp(editors.dropdownY, 6, Math.max(6, editors.panelHeight() - popupHeight - 6));
        addContextMenuBackdrop();
        editors.addPopupComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(9_000));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 42,
                Component.translatable("screen.oritech_space_age.action.choose_target"))
                .withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.darkPanel(px + popupWidth - 28, py + 6, 18, 16, Component.literal("×"),
                ignored -> editors.closeEditors()).withZIndex(9_001));
        editors.targetSearchField = editors.addPopupField(new EditBox(font, leftPos + px + 10, topPos + py + 27,
                popupWidth - 20, 18, Component.translatable("screen.oritech_space_age.action.search_targets")));
        editors.targetSearchField.setMaxLength(40);
        editors.targetSearchField.setValue(editors.targetSearch);
        editors.targetList = new ScrollWidget(px + 10, py + 50, popupWidth - 20, popupHeight - 60)
                .withVerticalScroll(true).withHorizontalScroll(false).withScrollSpeed(20);
        editors.targetList.setZIndex(9_001);
        editors.targetOptions.clear();
        var objects = editors.currentDraftSnapshot().objects().stream()
                .filter(object -> {
                    var action = editors.findAction(editors.targetAction);
                    return action == null || action.type() != SpaceSimulation.ActionType.CONNECT_ASTEROID
                            || object.type() == SpaceObjects.ObjectType.ASTEROID;
                })
                .sorted(Comparator.comparing(object -> RocketStarMapWidget.objectName(object).getString(),
                        String.CASE_INSENSITIVE_ORDER)).toList();
        for (var object : objects) {
            var button = SpaceAgeButtons.panel(4, 0, popupWidth - 36, 18,
                    RocketStarMapWidget.objectName(object), ignored -> editors.selectNavigationTarget(object));
            editors.targetList.addChild(button);
            editors.targetOptions.add(new FlightPlannerEditors.TargetOption(button,
                    RocketStarMapWidget.objectName(object).getString().toLowerCase(Locale.ROOT)));
        }
        filterTargetOptions();
        editors.addPopupComponent(editors.targetList);
        editors.targetSearchField.setResponder(value -> {
            editors.targetSearch = value;
            filterTargetOptions();
        });
        editors.focusPopup(editors.targetSearchField);
    }

    private void filterTargetOptions() {
        if (editors.targetList == null) return;
        var query = editors.targetSearch.strip().toLowerCase(Locale.ROOT);
        int y = 4;
        for (var option : editors.targetOptions) {
            boolean visible = query.isEmpty() || option.searchText().contains(query);
            option.button().setVisible(visible);
            if (visible) {
                option.button().setY(y);
                y += 21;
            }
        }
        editors.targetList.setContentDimensions(editors.targetList.getWidth() - 8, Math.max(editors.targetList.getHeight() - 8, y + 3));
    }

    private void buildArrivalEditor() {
        var popupWidth = Math.min(290, editors.panelWidth() - 12);
        var popupHeight = 132;
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 24,
                Component.translatable("screen.oritech_space_age.action.arrival_help"))
                .withWrap(true).withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + 35, (popupWidth - 24) / 2, 18,
                Component.translatable("screen.oritech_space_age.action.velocity_zero"),
                ignored -> editors.applyArrival(SpaceSimulation.ArrivalVelocityMode.ZERO, 0)).withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 14 + (popupWidth - 24) / 2, py + 35,
                (popupWidth - 24) / 2, 18,
                Component.translatable("screen.oritech_space_age.action.velocity_maximum"),
                ignored -> editors.applyArrival(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0)).withZIndex(9_001));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 64, 70,
                Component.translatable("screen.oritech_space_age.action.velocity_custom_title"))
                .withBrightColor().withZIndex(9_001));
        editors.arrivalField = editors.addPopupField(new EditBox(font, leftPos + px + 82, topPos + py + 59,
                popupWidth - 92, 18, Component.translatable("screen.oritech_space_age.action.velocity_custom_title")));
        editors.arrivalField.setMaxLength(10);
        editors.arrivalField.setValue(editors.arrivalText);
        var apply = SpaceAgeButtons.panel(px + 10, py + 104, 100, 18, Component.translatable("gui.done"),
                ignored -> editors.applyCustomArrival());
        apply.setZIndex(9_001);
        apply.setActive(editors.parseArrivalVelocity(editors.arrivalText) != null);
        editors.addPopupComponent(apply);
        editors.arrivalField.setResponder(value -> {
            editors.arrivalText = value;
            var valid = editors.parseArrivalVelocity(value) != null;
            editors.arrivalField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
            apply.setActive(valid);
        });
        editors.addPopupComponent(SpaceAgeButtons.panel(px + popupWidth - 110, py + 104, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(9_001));
        editors.focusPopup(editors.arrivalField);
    }

    private void buildLandingEditor() {
        var popupWidth = Math.min(300, editors.panelWidth() - 12);
        var popupHeight = 126;
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 24,
                Component.translatable("screen.oritech_space_age.action.landing_help"))
                .withWrap(true).withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 43, 18, Component.literal("X"))
                .withBrightColor().withZIndex(9_001));
        editors.landingXField = editors.addPopupField(new EditBox(font, leftPos + px + 28, topPos + py + 38,
                100, 18, Component.literal("X")));
        editors.landingXField.setValue(editors.landingXText);
        editors.addPopupComponent(new LabelWidget(px + 150, py + 43, 18, Component.literal("Z"))
                .withBrightColor().withZIndex(9_001));
        editors.landingZField = editors.addPopupField(new EditBox(font, leftPos + px + 168, topPos + py + 38,
                100, 18, Component.literal("Z")));
        editors.landingZField.setValue(editors.landingZText);
        var apply = SpaceAgeButtons.panel(px + 10, py + 94, 88, 18, Component.translatable("gui.done"),
                ignored -> editors.applyLandingCoordinates());
        apply.setZIndex(9_001);
        editors.addPopupComponent(apply);
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 106, py + 94, 88, 18,
                Component.translatable("screen.oritech_space_age.action.next_orbit"), ignored -> editors.nextLandingOrbit())
                .withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 202, py + 94, 88, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(9_001));
        editors.landingXField.setResponder(value -> editors.landingXText = value);
        editors.landingZField.setResponder(value -> editors.landingZText = value);
        editors.focusPopup(editors.landingXField);
    }

    private void buildAddonEditor() {
        var action = editors.findAction(editors.addonAction);
        if (action == null || action.addons().isEmpty()) return;
        var addon = action.addons().getFirst();
        var popupWidth = Math.min(350, editors.panelWidth() - 12);
        var popupHeight = 144;
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 24,
                Component.translatable("screen.oritech_space_age.action.completion_help"))
                .withWrap(true).withBrightColor().withZIndex(9_001));
        int typeWidth = (popupWidth - 28) / 3;
        var types = SpaceSimulation.ActionAddonType.values();
        for (int index = 0; index < types.length; index++) {
            var type = types[index];
            var button = SpaceAgeButtons.panel(px + 10 + index * (typeWidth + 4), py + 38, typeWidth, 28,
                    FlightPlannerLabels.addonTypeName(type), ignored -> editors.selectAddonType(type));
            button.setActive(type != addon.type());
            editors.addPopupComponent(button.withZIndex(9_001));
        }
        editors.addPopupComponent(new LabelWidget(px + 10, py + 79, 72,
                Component.translatable("screen.oritech_space_age.action.condition_value"))
                .withBrightColor().withZIndex(9_001));
        editors.addonValueField = editors.addPopupField(new EditBox(font, leftPos + px + 82, topPos + py + 74,
                popupWidth - 172, 18, Component.translatable("screen.oritech_space_age.action.condition_value")));
        editors.addonValueField.setMaxLength(10);
        editors.addonValueField.setValue(editors.addonValueText);
        editors.addPopupComponent(new LabelWidget(px + popupWidth - 82, py + 79, 72, FlightPlannerLabels.addonUnit(addon.type()))
                .withBrightColor().withZIndex(9_001));
        var apply = SpaceAgeButtons.panel(px + 10, py + 116, 100, 18, Component.translatable("gui.done"),
                ignored -> editors.applyAddonValue());
        apply.setZIndex(9_001);
        apply.setActive(editors.parseAddonValue(addon.type(), editors.addonValueText) != null);
        editors.addPopupComponent(apply);
        editors.addPopupComponent(SpaceAgeButtons.panel(px + popupWidth - 110, py + 116, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(9_001));
        editors.addonValueField.setTextColor(editors.parseAddonValue(addon.type(), editors.addonValueText) == null
                ? 0xFFFF6666 : 0xFFFFFFFF);
        editors.addonValueField.setResponder(value -> {
            editors.addonValueText = value;
            var valid = editors.parseAddonValue(addon.type(), value) != null;
            editors.addonValueField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
            apply.setActive(valid);
        });
        editors.focusPopup(editors.addonValueField);
    }

    private void buildMapContextMenu() {
        int popupWidth = 176;
        int popupHeight = 30;
        int px = Math.clamp(editors.mapContextRequest.mouseX(), 6, Math.max(6, editors.panelWidth() - popupWidth - 6));
        int py = Math.clamp(editors.mapContextRequest.mouseY(), 6, Math.max(6, editors.panelHeight() - popupHeight - 6));
        addContextMenuBackdrop();
        editors.addPopupComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(9_000));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 6, py + 6, popupWidth - 12, 18,
                Component.translatable("screen.oritech_space_age.action.add_navigation_target"),
                ignored -> editors.addNavigationTargetFromMap()).withZIndex(9_001));
    }

    private void addEditorBackdrop(int px, int py, int popupWidth, int popupHeight) {
        var backdrop = new UIComponent(0, 0, editors.panelWidth(), editors.panelHeight()) {
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
        editors.addPopupComponent(backdrop);
    }

    private void addContextMenuBackdrop() {
        var backdrop = new UIComponent(0, 0, editors.panelWidth(), editors.panelHeight()) {
            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            }

            @Override
            public boolean handleClick(double mouseX, double mouseY, int button) {
                editors.closeEditors();
                return true;
            }
        };
        backdrop.setZIndex(8_999);
        editors.addPopupComponent(backdrop);
    }

}
