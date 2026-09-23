package rearth.oritech.spaceage.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
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
        if (editors.actionEditorAction != null) buildActionEditor();
        if (editors.actionEditorAction != null && editors.hasNestedEditor()) editors.setPopupLayerOffset(100);
        if (editors.speedAction != null) buildSpeedEditor();
        else if (editors.actionTypeAction != null || editors.newActionBranch != null) buildActionTypeMenu();
        else if (editors.targetAction != null) buildTargetMenu();
        else if (editors.arrivalAction != null) buildArrivalEditor();
        else if (editors.destinationAction != null) buildDestinationMenu();
        else if (editors.addonAction != null) buildAddonEditor();
        else if (editors.decoupleAction != null) buildDecoupleEditor();
        else if (editors.actionEditorAction == null && editors.mapContextRequest != null) buildMapContextMenu();
        editors.setPopupLayerOffset(0);
    }

    private void buildActionEditor() {
        var action = editors.findAction(editors.actionEditorAction);
        var branch = editors.findBranchContaining(action);
        if (branch == null) return;
        var target = editors.currentDraftSnapshot().objects().stream().filter(o -> o.id().equals(action.targetId())).findFirst().orElse(null);
        boolean survey = target != null && target.type() == SpaceObjects.ObjectType.SURVEY_REGION;
        boolean deployment = action.targetId().equals(SpaceObjects.EARTH_ID)
                && rearth.oritech.spaceage.simulation.SpaceBalance.hasSlots(action.orbit());
        int settingCount = switch (action.type()) {
            case NAVIGATE_TO -> 4 + (survey ? 0 : 1) + (deployment ? 1 : 0);
            case SCAN -> 2;
            case RELAY, TRANSMIT_INFORMATION -> 2;
            case CONNECT_ASTEROID, DECOUPLE -> 2;
            case MAINTAIN_POSITION, DISCARD_CRAFT -> 1;
            case DISCONNECT_BOOSTER -> 0;
        };
        var popupWidth = Math.min(380, editors.panelWidth() - 12);
        var popupHeight = 66 + settingCount * 25;
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 48,
                Component.translatable("screen.oritech_space_age.action.configuration",
                        FlightPlannerLabels.actionName(action.type())))
                .withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 6,
                ignored -> editors.closeActionEditor()).withZIndex(9_001));

        int rowY = py + 29;
        int buttonX = px + 92;
        int buttonWidth = popupWidth - 102;
        addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.type");
        int typeMenuY = rowY + 16;
        editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                FlightPlannerLabels.actionName(action.type()), ignored ->
                        editors.openActionTypeMenuFromEditor(action.id(), buttonX, typeMenuY))
                .withTooltip(FlightPlannerLabels.actionTooltip(action.type())).withZIndex(9_001));
        rowY += 25;

        if (action.type() == SpaceSimulation.ActionType.RELAY
                || action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION) {
            var settings = action.service();
            var text = action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION
                    ? settings.timeoutTicks() == 0 ? Component.translatable("screen.oritech_space_age.action.timeout_wait")
                    : Component.translatable("screen.oritech_space_age.action.timeout_seconds", settings.timeoutTicks() / 20)
                    : Component.translatable("screen.oritech_space_age.action.duration_seconds", settings.durationTicks() / 20);
            editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18, text,
                    ignored -> editors.editServiceNumber(action))
                    .withTooltip(Component.translatable(action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION
                            ? "screen.oritech_space_age.action.timeout_tooltip" : "screen.oritech_space_age.action.duration_tooltip"))
                    .withZIndex(9_001));
            rowY += 25;
        }
        if (action.type() == SpaceSimulation.ActionType.SCAN) {
            var estimate = FlightPlannerLabels.scanEstimate(editors.calculatedFlight(), action.id());
            editors.addPopupComponent(new LabelWidget(px + 10, rowY, popupWidth - 20,
                    FlightPlannerLabels.scanEnergyLabel(estimate)).withBrightColor()
                    .withTooltip(FlightPlannerLabels.scanEnergyTooltip(estimate)).withZIndex(9_001));
            rowY += 25;
        }
        if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.target");
            int targetMenuY = rowY + 16;
            editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                    editors.actionParameter(action, editors.flightPlanRocket()), ignored ->
                            editors.openTargetMenuFromEditor(action.id(), buttonX, targetMenuY))
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.target_tooltip"))
                    .withZIndex(9_001));
            rowY += 25;

            if (!survey) {
                addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.orbit");
                int destinationMenuY = rowY + 16;
                editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                        editors.actionOrbit(action), ignored ->
                                editors.openDestinationMenuFromEditor(action.id(), buttonX, destinationMenuY))
                        .withTooltip(Component.translatable("screen.oritech_space_age.action.destination_tooltip"))
                        .withZIndex(9_001));
                rowY += 25;

            }

            addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.arrival");
            var arrival = SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                    FlightPlannerLabels.actionVelocity(action), ignored -> editors.openArrivalEditor(action.id()));
            var prediction = editors.calculatedFlight().arrivalPredictions().stream()
                    .filter(item -> item.actionId().equals(action.id())).findFirst().orElse(null);
            var arrivalTooltip = new ArrayList<Component>();
            arrivalTooltip.add(Component.translatable("screen.oritech_space_age.action.arrival_tooltip"));
            if (prediction != null) arrivalTooltip.addAll(FlightPlannerLabels.arrivalTooltip(prediction.impact()));
            arrival.withTooltip(arrivalTooltip);
            editors.addPopupComponent(arrival.withZIndex(9_001));
            rowY += 25;

            if (deployment) {
                var settings = action.service();
                editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                        Component.translatable("screen.oritech_space_age.action.deployment_slot",
                                settings.slot() < 0 ? Component.translatable("screen.oritech_space_age.action.slot_auto") : settings.slot()), ignored -> editors.changeService(action,
                                new SpaceSimulation.ServiceSettings(settings.durationTicks(), settings.timeoutTicks(),
                                        settings.slot() + 1 >= rearth.oritech.spaceage.simulation.SpaceBalance.slots(action.orbit()) ? -1 : settings.slot() + 1)))
                        .withTooltip(Component.translatable("screen.oritech_space_age.action.slot_tooltip")).withZIndex(9_001));
                rowY += 25;
            }
            addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.cruise");
            editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                    Component.translatable("screen.oritech_space_age.action.speed_limit",
                            action.maxSpeed() == 0 ? Component.translatable("screen.oritech_space_age.action.speed_maximum")
                                    : Component.translatable("screen.oritech_space_age.action.speed_metres_per_second", action.maxSpeed())),
                    ignored -> editors.openSpeedEditor(action))
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.cruise_tooltip"))
                    .withZIndex(9_001));
            rowY += 25;
        } else if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                || action.type() == SpaceSimulation.ActionType.DECOUPLE) {
            addSettingLabel(px, rowY, action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                    ? "screen.oritech_space_age.action.setting.anchor"
                    : "screen.oritech_space_age.action.setting.connection");
            Component parameterLabel = editors.actionParameter(action, editors.flightPlanRocket());
            if (action.type() == SpaceSimulation.ActionType.DECOUPLE && action.segments().size() == 2
                    && action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
                parameterLabel = Component.translatable("screen.oritech_space_age.action.decouple_connection",
                        editors.segmentName(action.segments().get(0), editors.flightPlanRocket()),
                        editors.segmentName(action.segments().get(1), editors.flightPlanRocket()));
            }
            var parameter = SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18, parameterLabel, ignored -> {
                if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
                    editors.cycleEditedParameter(action.id());
                } else editors.openDecoupleEditor(action.id());
            });
            parameter.setActive(action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET));
            editors.addPopupComponent(parameter.withZIndex(9_001));
            rowY += 25;
        }

        editors.addPopupComponent(SpaceAgeButtons.panel(px + popupWidth - 110, rowY, 100, 18,
                Component.translatable("gui.done"), ignored -> editors.closeActionEditor()).withZIndex(9_001));
    }

    private void buildDecoupleEditor() {
        var popupWidth = Math.min(560, editors.panelWidth() - 12);
        var popupHeight = Math.min(270, editors.panelHeight() - 12);
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 44,
                Component.translatable("screen.oritech_space_age.action.decouple_editor"))
                .withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 6,
                ignored -> editors.closeEditors()).withZIndex(9_001));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 24, popupWidth - 20, 28,
                Component.translatable("screen.oritech_space_age.action.decouple_help"))
                .withBrightColor().withWrap(true).withZIndex(9_001));

        int listWidth = (popupWidth - 34) / 2;
        int listY = py + 68;
        int listHeight = popupHeight - 106;
        editors.addPopupComponent(new LabelWidget(px + 10, py + 55, listWidth,
                Component.translatable("screen.oritech_space_age.action.decouple_keep"))
                .withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(new LabelWidget(px + 24 + listWidth, py + 55, listWidth,
                Component.translatable("screen.oritech_space_age.action.decouple_new_branch"))
                .withBrightColor().withZIndex(9_001));

        var retainedList = new ScrollWidget(px + 10, listY, listWidth, listHeight)
                .withVerticalScroll(true).withHorizontalScroll(false).withScrollSpeed(20);
        retainedList.setZIndex(9_001);
        var retained = editors.decoupleCandidates();
        for (int index = 0; index < retained.size(); index++) {
            var segment = retained.get(index);
            var button = SpaceAgeButtons.panel(4, 4 + index * 22, listWidth - 16, 18,
                    Component.literal(editors.segmentName(segment, editors.flightPlanRocket())),
                    ignored -> editors.selectDecoupleRetained(segment));
            selectListButton(button, segment.equals(editors.decoupleRetained));
            retainedList.addChild(button);
        }
        retainedList.setContentDimensions(listWidth - 8, Math.max(listHeight - 8, 8 + retained.size() * 22));
        editors.addPopupComponent(retainedList);

        var detachedList = new ScrollWidget(px + 24 + listWidth, listY, listWidth, listHeight)
                .withVerticalScroll(true).withHorizontalScroll(false).withScrollSpeed(20);
        detachedList.setZIndex(9_001);
        var detached = editors.decoupleNeighbors(editors.decoupleRetained);
        for (int index = 0; index < detached.size(); index++) {
            var segment = detached.get(index);
            var button = SpaceAgeButtons.panel(4, 4 + index * 22, listWidth - 16, 18,
                    Component.literal(editors.segmentName(segment, editors.flightPlanRocket())),
                    ignored -> editors.selectDecoupleDetached(segment));
            selectListButton(button, segment.equals(editors.decoupleDetached));
            detachedList.addChild(button);
        }
        detachedList.setContentDimensions(listWidth - 8, Math.max(listHeight - 8, 8 + detached.size() * 22));
        editors.addPopupComponent(detachedList);

        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + popupHeight - 28, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(9_001));
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, py + popupHeight - 28, 100, 18,
                Component.translatable("gui.done"), ignored -> editors.applyDecoupleSelection());
        apply.setActive(editors.decoupleRetained != null && editors.decoupleDetached != null);
        editors.addPopupComponent(apply.withZIndex(9_001));
    }

    private static void selectListButton(rearth.oritech.api.screen.widgets.ButtonWidget button, boolean selected) {
        button.withDisabledSurface(OritechSurface.PANEL_PRESSED)
                .withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(selected);
        button.setActive(!selected);
    }

    private void addSettingLabel(int px, int rowY, String translationKey) {
        editors.addPopupComponent(new LabelWidget(px + 10, rowY, 78, Component.translatable(translationKey))
                .withBrightColor().withZIndex(9_001));
    }

    private void buildSpeedEditor() {
        var action = editors.findAction(editors.speedAction);
        if (action == null) return;
        var transmissionTimeout = editors.editingServiceTime
                && action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION;
        var numberLabel = editors.editingServiceTime ? Component.translatable("screen.oritech_space_age.action.seconds")
                : Component.translatable("screen.oritech_space_age.action.speed_custom_title");
        var popupWidth = Math.min(280, editors.panelWidth() - 12);
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - 132) / 2);
        addEditorBackdrop(px, py, popupWidth, 132);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 30,
                editors.editingServiceTime ? Component.translatable(transmissionTimeout ? "screen.oritech_space_age.action.timeout_help"
                        : "screen.oritech_space_age.action.duration_help") : Component.translatable("screen.oritech_space_age.action.speed_help"))
                .withWrap(true).withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + 35, popupWidth - 20, 18,
                editors.editingServiceTime ? Component.translatable(transmissionTimeout ? "screen.oritech_space_age.action.wait_until_delivered"
                        : "screen.oritech_space_age.action.one_day") : Component.translatable("screen.oritech_space_age.action.speed_maximum"),
                ignored -> editors.applySpeedLimit(editors.editingServiceTime && !transmissionTimeout ? 1200 : 0)).withZIndex(9_001));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 64, 70,
                numberLabel)
                .withBrightColor().withZIndex(9_001));
        editors.speedField = editors.addPopupField(new EditBox(font, leftPos + px + 82, topPos + py + 59,
                popupWidth - 92, 18, numberLabel));
        if (editors.editingServiceTime) editors.speedField.setTooltip(Tooltip.create(Component.translatable(transmissionTimeout
                ? "screen.oritech_space_age.action.timeout_tooltip" : "screen.oritech_space_age.action.duration_tooltip")));
        editors.speedField.setMaxLength(20);
        editors.speedField.setValue(editors.speedText);
        editors.speedField.setTextColor(editors.parseSpeedLimit(editors.speedText) != null ? 0xFFFFFFFF : 0xFFFF6666);
        editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 6,
                ignored -> editors.closeSpeedEditor()).withZIndex(9_001));
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, py + 104, 100, 18, Component.translatable("gui.done"),
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
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + 104, 100, 18,
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
        editors.addPopupComponent(new LabelWidget(px + 10, py + 7, popupWidth - 42,
                Component.translatable("screen.oritech_space_age.action.choose_type"))
                .withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 4,
                ignored -> editors.closeEditors()).withZIndex(9_001));
        var editedAction = editors.findAction(editors.actionTypeAction);
        var editedBranch = editors.newActionBranch == null
                ? editors.draftPlan().branches().stream()
                .filter(branch -> branch.actions().stream().anyMatch(action -> action.id().equals(editors.actionTypeAction)))
                .findFirst().orElse(null)
                : editors.findBranch(editors.newActionBranch);
        var beforeAction = editedAction == null ? null : editedAction.id();
        boolean canConnectAsteroid = editedBranch != null
                && !RocketFlightPlanRules.asteroidAnchorSegments(editors.flightPlanRocket()).isEmpty()
                && editors.asteroidArrivalBefore(editedBranch, beforeAction) != null
                && editors.connectedAsteroidBefore(editedBranch, beforeAction) == null;
        var hardware = editors.flightPlanRocket().getStaticSegments().values().stream()
                .map(rearth.oritech.spaceage.simulation.RocketHardware::of).toList();
        boolean hasScanner = hardware.stream().anyMatch(item -> item.scanners() > 0);
        boolean hasAntenna = hardware.stream().anyMatch(item -> item.antennas() > 0);
        for (int index = 0; index < editors.EDITABLE_ACTION_TYPES.length; index++) {
            var type = editors.EDITABLE_ACTION_TYPES[index];
            var button = SpaceAgeButtons.panel(px + 10, py + 18 + index * 22, popupWidth - 20, 18,
                    FlightPlannerLabels.actionName(type), ignored -> editors.selectActionType(type));
            button.withTooltip(FlightPlannerLabels.actionTooltip(type));
            var available = type != SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || editedAction != null && editedAction.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || canConnectAsteroid;
            if (type == SpaceSimulation.ActionType.SCAN) available &= hasScanner;
            if (type == SpaceSimulation.ActionType.RELAY || type == SpaceSimulation.ActionType.TRANSMIT_INFORMATION)
                available &= hasAntenna;
            if (editedAction != null && type == editedAction.type()) selectListButton(button, true);
            else button.setActive(available);
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
        editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 6,
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
                .sorted(Comparator.comparingInt((SpaceSimulation.SpaceObjectData object) ->
                                object.type() == SpaceObjects.ObjectType.ASTEROID ? 1 : 0)
                        .thenComparing(object -> RocketStarMapWidget.objectName(object).getString(),
                                String.CASE_INSENSITIVE_ORDER)).toList();
        for (var object : objects) {
            var button = SpaceAgeButtons.panel(4, 0, popupWidth - 36, 18,
                    RocketStarMapWidget.objectName(object), ignored -> editors.selectNavigationTarget(object));
            var action = editors.findAction(editors.targetAction);
            selectListButton(button, action != null && object.id().equals(action.targetId()));
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
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 48, 24,
                Component.translatable("screen.oritech_space_age.action.arrival_help"))
                .withWrap(true).withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 6,
                ignored -> editors.closeEditors()).withZIndex(9_001));
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
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, py + 104, 100, 18, Component.translatable("gui.done"),
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
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + 104, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(9_001));
        editors.focusPopup(editors.arrivalField);
    }

    private void buildDestinationMenu() {
        var action = editors.findAction(editors.destinationAction);
        var target = action == null ? null : editors.currentDraftSnapshot().objects().stream()
                .filter(object -> object.id().equals(action.targetId())).findFirst().orElse(null);
        if (action == null || target == null) return;
        var orbits = RocketFlightPlanRules.availableOrbits(target.type());
        var surfaceCoordinates = action.targetId().equals(SpaceObjects.EARTH_ID)
                && editors.destinationOrbit == SpaceSimulation.OrbitBand.SURFACE;
        var popupWidth = Math.min(300, editors.panelWidth() - 12);
        var popupHeight = 34 + orbits.size() * 22 + (surfaceCoordinates ? 74 : 30);
        var px = Math.clamp(editors.dropdownX, 6, Math.max(6, editors.panelWidth() - popupWidth - 6));
        var py = Math.clamp(editors.dropdownY, 6, Math.max(6, editors.panelHeight() - popupHeight - 6));
        addContextMenuBackdrop();
        editors.addPopupComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(9_000));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 42,
                Component.translatable("screen.oritech_space_age.action.choose_destination"))
                .withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 6,
                ignored -> editors.closeEditors()).withZIndex(9_001));
        int rowY = py + 28;
        for (var orbit : orbits) {
            var button = SpaceAgeButtons.panel(px + 10, rowY, popupWidth - 20, 18,
                    RocketStarMapWidget.orbitName(orbit), ignored -> editors.selectDestinationOrbit(orbit));
            selectListButton(button, orbit == editors.destinationOrbit);
            editors.addPopupComponent(button.withZIndex(9_001));
            rowY += 22;
        }
        if (surfaceCoordinates) {
            editors.addPopupComponent(new LabelWidget(px + 10, rowY + 5, popupWidth - 20,
                    Component.translatable("screen.oritech_space_age.action.setting.coordinates"))
                    .withBrightColor().withZIndex(9_001));
            rowY += 20;
            editors.addPopupComponent(new LabelWidget(px + 10, rowY + 5, 18, Component.literal("X"))
                .withBrightColor().withZIndex(9_001));
            editors.landingXField = editors.addPopupField(new EditBox(font, leftPos + px + 28, topPos + rowY,
                100, 18, Component.literal("X")));
            editors.landingXField.setValue(editors.landingXText);
            editors.addPopupComponent(new LabelWidget(px + 150, rowY + 5, 18, Component.literal("Z"))
                .withBrightColor().withZIndex(9_001));
            editors.landingZField = editors.addPopupField(new EditBox(font, leftPos + px + 168, topPos + rowY,
                100, 18, Component.literal("Z")));
            editors.landingZField.setValue(editors.landingZText);
        }
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + popupHeight - 24, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(9_001));
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, py + popupHeight - 24, 100, 18,
                Component.translatable("gui.done"), ignored -> editors.applyDestination());
        apply.setZIndex(9_001);
        apply.setActive(!surfaceCoordinates || editors.parseCoordinate(editors.landingXText) != null
                && editors.parseCoordinate(editors.landingZText) != null);
        editors.addPopupComponent(apply);
        if (surfaceCoordinates) {
            editors.landingXField.setResponder(value -> {
                editors.landingXText = value;
                var valid = editors.parseCoordinate(value) != null;
                editors.landingXField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
                apply.setActive(valid && editors.parseCoordinate(editors.landingZText) != null);
            });
            editors.landingZField.setResponder(value -> {
                editors.landingZText = value;
                var valid = editors.parseCoordinate(value) != null;
                editors.landingZField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
                apply.setActive(valid && editors.parseCoordinate(editors.landingXText) != null);
            });
            editors.focusPopup(editors.landingXField);
        }
    }

    private void buildAddonEditor() {
        var action = editors.findAction(editors.addonAction);
        if (action == null || action.addons().isEmpty()) return;
        var addon = action.addons().getFirst();
        var types = java.util.Arrays.stream(SpaceSimulation.ActionAddonType.values())
                .filter(type -> action.type() == SpaceSimulation.ActionType.MAINTAIN_POSITION
                        ? type.isMaintainPositionCondition() : type.isNavigationCondition()).toList();
        int columns = Math.min(3, types.size());
        int typeRows = (types.size() + columns - 1) / columns;
        boolean stationKeeping = action.type() == SpaceSimulation.ActionType.MAINTAIN_POSITION;
        int stationKeepingDetailsHeight = stationKeeping ? 40 : 0;
        var popupWidth = Math.min(350, editors.panelWidth() - 12);
        var popupHeight = 123 + typeRows * 32 + stationKeepingDetailsHeight;
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 48, 24,
                Component.translatable(action.type() == SpaceSimulation.ActionType.MAINTAIN_POSITION
                        ? "screen.oritech_space_age.action.maintain_abort_help"
                        : "screen.oritech_space_age.action.completion_help"))
                .withWrap(true).withBrightColor().withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 6,
                ignored -> editors.cancelAddonEditor()).withZIndex(9_001));
        LabelWidget stationKeepingDuration = stationKeeping ? new LabelWidget(px + 10, py + 38, popupWidth - 20,
                Component.empty()).withBrightColor() : null;
        LabelWidget stationKeepingEnd = stationKeeping ? new LabelWidget(px + 10, py + 56, popupWidth - 20,
                Component.empty()).withBrightColor() : null;
        if (stationKeeping) {
            updateStationKeepingLabels(action, stationKeepingDuration, stationKeepingEnd);
            stationKeepingDuration.setZIndex(9_001);
            stationKeepingEnd.setZIndex(9_001);
            editors.addPopupComponent(stationKeepingDuration);
            editors.addPopupComponent(stationKeepingEnd);
        }
        int typeWidth = (popupWidth - 20 - (columns - 1) * 4) / columns;
        for (int index = 0; index < types.size(); index++) {
            var type = types.get(index);
            int column = index % columns;
            int row = index / columns;
            var button = SpaceAgeButtons.panel(px + 10 + column * (typeWidth + 4),
                    py + 38 + stationKeepingDetailsHeight + row * 32, typeWidth, 28,
                    FlightPlannerLabels.addonTypeName(type), ignored -> editors.selectAddonType(type));
            button.setActive(type != addon.type());
            editors.addPopupComponent(button.withTooltip(FlightPlannerLabels.addonTooltip(type)).withZIndex(9_001));
        }
        int valueY = py + 44 + stationKeepingDetailsHeight + typeRows * 32;
        editors.addPopupComponent(new LabelWidget(px + 10, valueY + 5, 72,
                Component.translatable("screen.oritech_space_age.action.condition_value"))
                .withBrightColor().withZIndex(9_001));
        editors.addonValueField = editors.addPopupField(new EditBox(font, leftPos + px + 82, topPos + valueY,
                popupWidth - 172, 18, Component.translatable("screen.oritech_space_age.action.condition_value")));
        editors.addonValueField.setMaxLength(10);
        editors.addonValueField.setValue(editors.addonValueText);
        editors.addPopupComponent(new LabelWidget(px + popupWidth - 82, valueY + 5, 72,
                FlightPlannerLabels.addonUnit(addon.type())).withBrightColor().withZIndex(9_001));
        int footerY = py + popupHeight - 28;
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, footerY, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.cancelAddonEditor()).withZIndex(9_001));
        editors.addPopupComponent(SpaceAgeButtons.darkPanel(px + (popupWidth - 100) / 2, footerY, 100, 18,
                Component.translatable("screen.oritech_space_age.action.remove_condition"),
                ignored -> editors.removeEditedNavigationAddon()).withZIndex(9_001));
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, footerY, 100, 18, Component.translatable("gui.done"),
                ignored -> editors.applyAddonValue());
        apply.setZIndex(9_001);
        apply.setActive(editors.parseAddonValue(addon.type(), editors.addonValueText) != null);
        editors.addPopupComponent(apply);
        editors.addonValueField.setTextColor(editors.parseAddonValue(addon.type(), editors.addonValueText) == null
                ? 0xFFFF6666 : 0xFFFFFFFF);
        editors.addonValueField.setResponder(value -> {
            editors.addonValueText = value;
            var valid = editors.parseAddonValue(addon.type(), value) != null;
            editors.addonValueField.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF6666);
            apply.setActive(valid);
            if (valid && editors.previewAddonValue(value)) {
                updateStationKeepingLabels(action, stationKeepingDuration, stationKeepingEnd);
            }
        });
        editors.focusPopup(editors.addonValueField);
    }

    private void updateStationKeepingLabels(SpaceSimulation.FlightPlanAction action,
                                            LabelWidget duration, LabelWidget end) {
        if (duration == null || end == null) return;
        var estimate = FlightPlannerLabels.stationKeepingEstimate(editors.calculatedFlight(), action.id());
        duration.setText(FlightPlannerLabels.stationKeepingDuration(estimate));
        end.setText(FlightPlannerLabels.stationKeepingEnd(estimate));
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
