package rearth.oritech.spaceage.client.planner;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.api.screen.widgets.ToggleWidget;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.spaceage.client.mission_control.FleetViewPreferences;
import rearth.oritech.spaceage.client.mission_control.FleetCategories;
import rearth.oritech.spaceage.client.planner.FlightPlannerEditors.NumberKind;
import rearth.oritech.spaceage.client.starmap.RocketStarMapWidget;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.NavigationComputerRules;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.mission.RocketDocking;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.RocketPayloads;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Builds popup controls; their text, selected card and input handling stay in the editor.
 */
final class FlightPlannerPopups {

    // Popups render above the map and timeline: dismiss layer, then surfaces, then controls.
    static final int DISMISS_Z = 8_999;
    static final int SURFACE_Z = 9_000;
    static final int CONTROL_Z = 9_001;
    private static final int VALID_TEXT = 0xFFFFFFFF;
    private static final int INVALID_TEXT = 0xFFFF6666;
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

    /**
     * Keeps a field's retained text, error colour and Done button in sync with its parser.
     */
    private static void validateField(EditBox field, ButtonWidget apply, Predicate<String> valid, Consumer<String> changed) {

        var initial = valid.test(field.getValue());
        field.setTextColor(initial ? VALID_TEXT : INVALID_TEXT);
        apply.setActive(initial);
        field.setResponder(value -> {
            var accepted = valid.test(value);
            field.setTextColor(accepted ? VALID_TEXT : INVALID_TEXT);
            apply.setActive(accepted);
            changed.accept(value);
        });
    }

    private static void selectListButton(ButtonWidget button, boolean selected) {

        button.withDisabledSurface(OritechSurface.PANEL_PRESSED)
                .withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(selected);
        button.setActive(!selected);
    }

    void build() {

        if (editors.categoryEditor) {
            buildCategoryMenu();
            return;
        }
        if (editors.actionEditorAction != null && !editors.hasNestedEditor()) buildActionEditor();
        if (editors.numberAction != null) buildNumberEditor();
        else if (editors.actionTypeAction != null || editors.newActionBranch != null) buildActionTypeMenu();
        else if (editors.targetAction != null) buildTargetMenu();
        else if (editors.arrivalAction != null) buildArrivalEditor();
        else if (editors.destinationAction != null) buildDestinationMenu();
        else if (editors.addonAction != null) buildAddonEditor();
        else if (editors.decoupleAction != null) buildDecoupleEditor();
        else if (editors.actionEditorAction == null && editors.mapContextRequest != null) buildMapContextMenu();
    }

    private void buildCategoryMenu() {

        var categories = FleetViewPreferences.CATEGORIES;
        var popupWidth = Math.min(260, editors.panelWidth() - 12);
        var popupHeight = Math.min(editors.panelHeight() - 12, 44 + categories.categories().size() * 24);
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = (editors.panelHeight() - popupHeight) / 2;
        addDropdown(px, py, popupWidth, popupHeight, "screen.oritech_space_age.fleet.launch_category", true);
        var list = new ScrollWidget(px + 8, py + 29, popupWidth - 16, popupHeight - 37).withVerticalScroll(true);
        list.setZIndex(CONTROL_Z);
        var row = 0;
        for (var category : categories.categories()) {
            if (category.id().equals(FleetCategories.SUN)) continue;
            var button = SpaceAgeButtons.panel(2, row, popupWidth - 32, 20, categories.label(category.id()), ignored -> {
                editors.changeLaunchCategory(category.id());
                editors.closeEditors();
            });
            var selected = editors.draftPlan().category().isBlank() ? FleetCategories.UNCATEGORIZED : editors.draftPlan().category();
            selectListButton(button, selected.equals(category.id()));
            list.addChild(button);
            row += 24;
        }
        list.setContentDimensions(popupWidth - 32, row);
        editors.addPopupComponent(list);
    }

    private void buildActionEditor() {

        var action = editors.findAction(editors.actionEditorAction);
        var branch = editors.findBranchContaining(action);
        if (branch == null) return;
        var target = editors.currentDraftSnapshot().mapObjects().stream().filter(o -> o.id().equals(action.targetId())).findFirst().orElse(null);
        var survey = target != null && target.type() == SpaceObjects.ObjectType.SURVEY_REGION;
        var craft = target != null && target.type() == SpaceObjects.ObjectType.CRAFT;
        var waypoint = action.settings().waypoint().isPresent();
        var deployment = SpaceBalance.hasSlots(action.targetId(), action.orbit());
        var settingCount = switch (action.type()) {
            case NAVIGATE_TO ->
                    4 + (survey || craft || waypoint ? 0 : 1) + (deployment ? 1 : 0) + (action.asteroidTarget().closest() ? 2 : 0);
            case SCAN -> 2;
            case TRANSMIT_INFORMATION -> 2;
            case CONNECT_ASTEROID, DECOUPLE, DEPLOY_CUBESAT -> 2;
            case MAINTAIN_POSITION, DISCARD_CRAFT -> 1;
            case BEAM_POWER -> action.targetId().equals(SpaceObjects.EARTH_ID) ? 4 : 3;
            case PROCESS -> 7;
            case DOCK -> 9;
            case UNDOCK -> 6;
            case DISCONNECT_BOOSTER -> 0;
        };
        var popupWidth = Math.min(380, editors.panelWidth() - 12);
        var beamStats = action.type() == SpaceSimulation.ActionType.BEAM_POWER
                ? FlightPlannerLabels.beamPreview(action, editors.hardwareFor(action), editors.currentDraftSnapshot(), editors.calculatedFlight()) : List.<Component>of();
        var statsHeight = beamStats.stream().mapToInt(line -> font.split(line, popupWidth - 20).size() * font.lineHeight + 4).sum();
        var rowSpacing = Math.clamp((editors.panelHeight() - 68 - statsHeight) / Math.max(1, settingCount), 18, 25);
        var popupHeight = 56 + settingCount * rowSpacing + statsHeight;
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 48,
                Component.translatable("screen.oritech_space_age.action.configuration",
                        FlightPlannerLabels.actionName(action.type())))
                .withBrightColor().withZIndex(CONTROL_Z));

        var rowY = py + 29;
        var buttonX = px + 92;
        var buttonWidth = popupWidth - 102;
        addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.type");
        var typeMenuY = rowY + 16;
        editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                        FlightPlannerLabels.actionName(action.type()), ignored ->
                                editors.openActionTypeMenuFromEditor(action.id(), buttonX, typeMenuY))
                .withTooltip(FlightPlannerLabels.actionTooltip(action.type())).withZIndex(CONTROL_Z));
        rowY += rowSpacing;

        if (action.type() == SpaceSimulation.ActionType.BEAM_POWER) {
            for (var line : beamStats) {
                var lineHeight = font.split(line, popupWidth - 20).size() * font.lineHeight;
                editors.addPopupComponent(new LabelWidget(px + 10, rowY, popupWidth - 20, lineHeight, line)
                        .withBrightColor().withWrap(true).withTooltip(Component.translatable("screen.oritech_space_age.beam.ideal_note")).withZIndex(CONTROL_Z));
                rowY += lineHeight + 4;
            }
            var targetY = rowY + 16;
            editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, rowY - 4, popupWidth - 94, 18,
                    editors.actionParameter(action, editors.flightPlanRocket()), ignored ->
                            editors.openTargetMenuFromEditor(action.id(), buttonX, targetY)).withZIndex(CONTROL_Z));
            editors.addPopupComponent(SpaceAgeButtons.panel(px + popupWidth - 80, rowY - 4, 70, 18,
                    Component.translatable("screen.oritech_space_age.beam.show_target"), ignored -> editors.showBeamTarget(action.targetId())).withZIndex(CONTROL_Z));
            rowY += rowSpacing;
            editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, rowY - 4, popupWidth - 20, 18,
                    Component.translatable("screen.oritech_space_age.beam.reserve", action.settings().beam().reservePercent()),
                    ignored -> editors.editBeamReserve(action)).withZIndex(CONTROL_Z));
            rowY += rowSpacing;
            if (action.targetId().equals(SpaceObjects.EARTH_ID)) {
                addSettingLabel(px, rowY, "screen.oritech_space_age.beam.coordinates");
                var fieldWidth = (buttonWidth - 4) / 2;
                var xField = editors.landingXField = editors.addPopupField(new EditBox(font, leftPos + buttonX, topPos + rowY - 4,
                        fieldWidth, 18, Component.literal("X")));
                var zField = editors.landingZField = editors.addPopupField(new EditBox(font, leftPos + buttonX + fieldWidth + 4, topPos + rowY - 4,
                        fieldWidth, 18, Component.literal("Z")));
                xField.setValue(Integer.toString(action.landingX()));
                zField.setValue(Integer.toString(action.landingZ()));
                xField.setResponder(value -> updateBeamTarget(action, value, true));
                zField.setResponder(value -> updateBeamTarget(action, value, false));
                rowY += rowSpacing;
            }
        }
        if (action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION) {
            var timeout = action.service().timeoutTicks();
            var text = timeout == 0 ? Component.translatable("screen.oritech_space_age.action.timeout_wait")
                    : Component.translatable("screen.oritech_space_age.action.timeout_seconds", timeout / 20);
            var timeoutButton = SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18, text,
                    ignored -> editors.editServiceNumber(action));
            timeoutButton.withTooltip(Component.translatable("screen.oritech_space_age.action.timeout_tooltip"));
            timeoutButton.setZIndex(CONTROL_Z);
            var computer = NavigationComputerRules.computers(editors.hardwareFor(action), NavigationComputerRules.allSegments(editors.hardwareFor(action))) > 0;
            timeoutButton.setActive(computer);
            if (!computer)
                timeoutButton.withTooltip(Component.translatable("screen.oritech_space_age.validation.computer_required"));
            editors.addPopupComponent(timeoutButton);
            rowY += rowSpacing;
        }
        if (action.type() == SpaceSimulation.ActionType.SCAN) {
            var estimate = FlightPlannerLabels.scanEstimate(editors.calculatedFlight(), action.id());
            editors.addPopupComponent(new LabelWidget(px + 10, rowY, popupWidth - 20,
                    FlightPlannerLabels.scanEnergyLabel(estimate)).withBrightColor()
                    .withTooltip(FlightPlannerLabels.scanEnergyTooltip(estimate)).withZIndex(CONTROL_Z));
            rowY += rowSpacing;
        }
        if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.target");
            var targetMenuY = rowY + 16;
            editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                            editors.actionParameter(action, editors.flightPlanRocket()), ignored ->
                                    editors.openTargetMenuFromEditor(action.id(), buttonX, targetMenuY))
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.target_tooltip"))
                    .withZIndex(CONTROL_Z));
            rowY += rowSpacing;

            if (!survey && !craft && !waypoint) {
                addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.orbit");
                var destinationMenuY = rowY + 16;
                editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                                editors.actionOrbit(action), ignored ->
                                        editors.openDestinationMenuFromEditor(action.id(), buttonX, destinationMenuY))
                        .withTooltip(Component.translatable("screen.oritech_space_age.action.destination_tooltip"))
                        .withZIndex(CONTROL_Z));
                rowY += rowSpacing;
            }

            if (action.asteroidTarget().closest()) {
                for (var minimum : new boolean[]{true, false}) {
                    var radius = minimum ? action.asteroidTarget().minRadius() : action.asteroidTarget().maxRadius();
                    addSettingLabel(px, rowY, minimum ? "screen.oritech_space_age.action.min_radius" : "screen.oritech_space_age.action.max_radius");
                    editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                            Component.translatable("screen.oritech_space_age.action.radius_limit", radius),
                            ignored -> editors.editAsteroidRadius(action, minimum)).withZIndex(CONTROL_Z));
                    rowY += rowSpacing;
                }
            }

            addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.arrival");
            var arrival = SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                    FlightPlannerLabels.actionVelocity(action), ignored -> editors.openArrivalEditor(action.id()));
            var prediction = editors.calculatedFlight().arrivalPredictions().stream()
                    .filter(item -> item.actionId().equals(action.id())).findFirst().orElse(null);
            var arrivalTooltip = new ArrayList<Component>();
            arrivalTooltip.add(Component.translatable("screen.oritech_space_age.action.arrival_tooltip"));
            if (prediction != null) {
                arrivalTooltip.addAll(FlightPlannerLabels.arrivalTooltip(prediction.impact()));
                arrivalTooltip.addAll(FlightPlannerLabels.parachuteTooltip(prediction.parachutes()));
            }
            arrival.withTooltip(arrivalTooltip);
            editors.addPopupComponent(arrival.withZIndex(CONTROL_Z));
            rowY += rowSpacing;

            if (deployment) {
                var settings = action.service();
                editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                                Component.translatable("screen.oritech_space_age.action.deployment_slot",
                                        settings.slot() < 0 ? Component.translatable("screen.oritech_space_age.action.slot_auto") : settings.slot()),
                                ignored -> editors.changeService(action,
                                        new SpaceSimulation.ServiceSettings(settings.durationTicks(), settings.timeoutTicks(),
                                                settings.slot() + 1 >= SpaceBalance.slots(action.targetId(), action.orbit()) ? -1 : settings.slot() + 1)))
                        .withTooltip(Component.translatable("screen.oritech_space_age.action.slot_tooltip")).withZIndex(CONTROL_Z));
                rowY += rowSpacing;
            }
            addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.cruise");
            editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                            Component.translatable("screen.oritech_space_age.action.speed_limit",
                                    action.maxSpeed() == 0 ? Component.translatable("screen.oritech_space_age.action.speed_maximum")
                                            : Component.translatable("screen.oritech_space_age.action.speed_metres_per_second", action.maxSpeed())),
                            ignored -> editors.openSpeedEditor(action))
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.cruise_tooltip"))
                    .withZIndex(CONTROL_Z));
            rowY += rowSpacing;
        } else if (action.type() == SpaceSimulation.ActionType.DEPLOY_CUBESAT) {
            addSettingLabel(px, rowY, "screen.oritech_space_age.action.setting.cube_sat");
            editors.addPopupComponent(SpaceAgeButtons.panel(buttonX, rowY - 4, buttonWidth, 18,
                            editors.actionParameter(action, editors.flightPlanRocket()), ignored -> editors.cycleEditedParameter(action.id()))
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.cube_sat_tooltip")).withZIndex(CONTROL_Z));
            rowY += rowSpacing;
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
            editors.addPopupComponent(parameter.withZIndex(CONTROL_Z));
            rowY += rowSpacing;
        }

        rowY = new FlightPlannerServices(editors).build(action, px, rowY, popupWidth, buttonX, buttonWidth, rowSpacing);

        editors.addPopupComponent(SpaceAgeButtons.panel(px + popupWidth - 110, rowY, 100, 18,
                Component.translatable("gui.done"), ignored -> editors.closeActionEditor()).withZIndex(CONTROL_Z));
    }

    private void updateBeamTarget(SpaceSimulation.FlightPlanAction action, String value, boolean x) {

        var coordinate = FlightPlannerLabels.parseInteger(value);
        var current = editors.findAction(action.id());
        var branch = editors.findBranchContaining(current);
        if (coordinate == null || branch == null) return;
        editors.replaceAction(branch.id(), current.id(), current.withLanding(x ? coordinate : current.landingX(),
                x ? current.landingZ() : coordinate, 0, 0), editors.flightPlanRocket());
    }

    private void buildDecoupleEditor() {

        var popupWidth = Math.min(560, editors.panelWidth() - 12);
        var popupHeight = Math.min(270, editors.panelHeight() - 12);
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 44,
                Component.translatable("screen.oritech_space_age.action.decouple_editor"))
                .withBrightColor().withZIndex(CONTROL_Z));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 24, popupWidth - 20, 28,
                Component.translatable("screen.oritech_space_age.action.decouple_help"))
                .withBrightColor().withWrap(true).withZIndex(CONTROL_Z));

        var listWidth = (popupWidth - 34) / 2;
        var listY = py + 68;
        var listHeight = popupHeight - 106;
        editors.addPopupComponent(new LabelWidget(px + 10, py + 55, listWidth,
                Component.translatable("screen.oritech_space_age.action.decouple_keep"))
                .withBrightColor().withZIndex(CONTROL_Z));
        editors.addPopupComponent(new LabelWidget(px + 24 + listWidth, py + 55, listWidth,
                Component.translatable("screen.oritech_space_age.action.decouple_new_branch"))
                .withBrightColor().withZIndex(CONTROL_Z));

        addSegmentList(px + 10, listY, listWidth, listHeight, editors.decoupleCandidates(), editors.decoupleRetained,
                editors::selectDecoupleRetained);
        addSegmentList(px + 24 + listWidth, listY, listWidth, listHeight, editors.decoupleNeighbors(editors.decoupleRetained),
                editors.decoupleDetached, editors::selectDecoupleDetached);

        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + popupHeight - 28, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(CONTROL_Z));
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, py + popupHeight - 28, 100, 18,
                Component.translatable("gui.done"), ignored -> editors.applyDecoupleSelection());
        apply.setActive(editors.decoupleRetained != null && editors.decoupleDetached != null);
        editors.addPopupComponent(apply.withZIndex(CONTROL_Z));
    }

    private void addSegmentList(int x, int y, int width, int height, List<SpaceSimulation.SegmentRef> segments,
                                SpaceSimulation.SegmentRef selected, Consumer<SpaceSimulation.SegmentRef> select) {

        var list = new ScrollWidget(x, y, width, height).withVerticalScroll(true).withHorizontalScroll(false).withScrollSpeed(20);
        list.setZIndex(CONTROL_Z);
        for (int index = 0; index < segments.size(); index++) {
            var segment = segments.get(index);
            var button = SpaceAgeButtons.panel(4, 4 + index * 22, width - 16, 18,
                    Component.literal(editors.segmentName(segment, editors.flightPlanRocket())), ignored -> select.accept(segment));
            selectListButton(button, segment.equals(selected));
            list.addChild(button);
        }
        list.setContentDimensions(width - 8, Math.max(height - 8, 8 + segments.size() * 22));
        editors.addPopupComponent(list);
    }

    /**
     * Dropdown surface and title; only dropdowns without Done/Cancel get a close button.
     */
    private void addDropdown(int px, int py, int popupWidth, int popupHeight, String titleKey, boolean closeButton) {

        addContextMenuBackdrop();
        editors.addPopupComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(SURFACE_Z));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 42, Component.translatable(titleKey))
                .withBrightColor().withZIndex(CONTROL_Z));
        if (closeButton) editors.addPopupComponent(SpaceAgeButtons.close(px + popupWidth - 28, py + 5,
                ignored -> editors.closeEditors()).withZIndex(CONTROL_Z));
    }

    private void addSettingLabel(int px, int rowY, String translationKey) {

        editors.addPopupComponent(new LabelWidget(px + 10, rowY, 78, Component.translatable(translationKey))
                .withBrightColor().withZIndex(CONTROL_Z));
    }

    private void buildNumberEditor() {

        var action = editors.findAction(editors.numberAction);
        if (action == null) return;
        var kind = editors.numberKind;
        var transmissionTimeout = kind == NumberKind.SERVICE_TIME
                && action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION;
        // The same numeric editor serves speeds, service times and resource exchange amounts.
        String labelKey;
        String helpKey;
        String presetKey;
        var presetValue = 0;
        if (kind == NumberKind.MIN_RADIUS || kind == NumberKind.MAX_RADIUS) {
            labelKey = "screen.oritech_space_age.action.radius_metres";
            helpKey = "screen.oritech_space_age.action.radius_help";
            presetKey = "screen.oritech_space_age.action.radius_unrestricted";
        } else if (kind == NumberKind.BEAM_RESERVE) {
            labelKey = "screen.oritech_space_age.beam.reserve_title";
            helpKey = "screen.oritech_space_age.beam.reserve_help";
            presetKey = "screen.oritech_space_age.beam.no_reserve";
        } else if (kind.resourceAmount()) {
            labelKey = "screen.oritech_space_age.service.resource_amount_title";
            helpKey = "screen.oritech_space_age.service.resource_amount_help";
            presetKey = "screen.oritech_space_age.service.resource_amount_zero";
        } else if (kind == NumberKind.SERVICE_TIME) {
            labelKey = "screen.oritech_space_age.action.seconds";
            helpKey = transmissionTimeout ? "screen.oritech_space_age.action.timeout_help" : "screen.oritech_space_age.action.duration_help";
            presetKey = transmissionTimeout ? "screen.oritech_space_age.action.wait_until_delivered" : "screen.oritech_space_age.action.one_day";
            presetValue = transmissionTimeout ? 0 : 1200;
        } else {
            labelKey = "screen.oritech_space_age.action.speed_custom_title";
            helpKey = "screen.oritech_space_age.action.speed_help";
            presetKey = "screen.oritech_space_age.action.speed_maximum";
        }
        var numberLabel = Component.translatable(labelKey);
        var defaultValue = presetValue;
        var popupWidth = Math.min(280, editors.panelWidth() - 12);
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - 132) / 2);
        addEditorBackdrop(px, py, popupWidth, 132);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 20, 30,
                Component.translatable(helpKey))
                .withWrap(true).withZIndex(CONTROL_Z));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + 35, popupWidth - 20, 18,
                Component.translatable(presetKey), ignored -> editors.applyNumber(defaultValue)).withZIndex(CONTROL_Z));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 64, 70,
                numberLabel)
                .withBrightColor().withZIndex(CONTROL_Z));
        editors.numberField = editors.addPopupField(new EditBox(font, leftPos + px + 82, topPos + py + 59,
                popupWidth - 92, 18, numberLabel));
        if (kind == NumberKind.SERVICE_TIME)
            editors.numberField.setTooltip(Tooltip.create(Component.translatable(transmissionTimeout
                    ? "screen.oritech_space_age.action.timeout_tooltip" : "screen.oritech_space_age.action.duration_tooltip")));
        editors.numberField.setMaxLength(20);
        editors.numberField.setValue(editors.numberText);
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, py + 104, 100, 18, Component.translatable("gui.done"),
                ignored -> editors.applyNumber());
        apply.setZIndex(CONTROL_Z);
        editors.addPopupComponent(apply);
        validateField(editors.numberField, apply, value -> editors.parseEditorNumber(value) != null, value -> editors.numberText = value);
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + 104, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(CONTROL_Z));
        editors.focusPopup(editors.numberField);
    }

    private void buildActionTypeMenu() {

        var popupWidth = Math.min(210, editors.panelWidth() - 12);
        var popupHeight = Math.min(190, editors.panelHeight() - 12);
        var px = Math.clamp(editors.dropdownX, 6, Math.max(6, editors.panelWidth() - popupWidth - 6));
        var py = Math.clamp(editors.dropdownY, 6, Math.max(6, editors.panelHeight() - popupHeight - 6));
        addDropdown(px, py, popupWidth, popupHeight, "screen.oritech_space_age.action.choose_type", true);
        var editedAction = editors.findAction(editors.actionTypeAction);
        var editedBranch = editors.newActionBranch == null
                ? editors.draftPlan().branches().stream()
                .filter(branch -> branch.actions().stream().anyMatch(action -> action.id().equals(editors.actionTypeAction)))
                .findFirst().orElse(null)
                : editors.findBranch(editors.newActionBranch);
        var beforeAction = editedAction == null ? null : editedAction.id();
        var craft = editors.hardwareBefore(editedBranch, beforeAction);
        var canConnectAsteroid = editedBranch != null
                && !RocketFlightPlanRules.asteroidAnchorSegments(craft).isEmpty()
                && editors.asteroidArrivalBefore(editedBranch, beforeAction) != null
                && editors.connectedAsteroidBefore(editedBranch, beforeAction) == null;
        var hardware = craft.getStaticSegments().values().stream()
                .map(RocketHardware::of).toList();
        var hasScanner = hardware.stream().anyMatch(item -> item.scanners() > 0);
        var hasAntenna = hardware.stream().anyMatch(item -> item.antennas() > 0);
        var dockTarget = editors.dockingTargetBefore(editedBranch, beforeAction);
        var canDock = dockTarget != null && !dockTarget.ports().isEmpty() && !RocketDocking.ports(craft).isEmpty();
        var typeList = new ScrollWidget(px + 6, py + 26, popupWidth - 12, popupHeight - 32).withVerticalScroll(true).withHorizontalScroll(false).withScrollSpeed(20);
        typeList.withSurface(OritechSurface.NONE);
        typeList.setZIndex(CONTROL_Z);
        var hasAperture = craft.powerHardware().throughput() > 0;
        var types = Arrays.stream(FlightPlannerEditors.EDITABLE_ACTION_TYPES)
                .filter(type -> type != SpaceSimulation.ActionType.BEAM_POWER || hasAperture).toList();
        for (int index = 0; index < types.size(); index++) {
            var type = types.get(index);
            var button = SpaceAgeButtons.panel(4, 4 + index * 22, popupWidth - 30, 18,
                    FlightPlannerLabels.actionName(type), ignored -> editors.selectActionType(type));
            button.withTooltip(FlightPlannerLabels.actionTooltip(type));
            var available = type != SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || editedAction != null && editedAction.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                    || canConnectAsteroid;
            if (type == SpaceSimulation.ActionType.SCAN) available &= hasScanner;
            if (type == SpaceSimulation.ActionType.DOCK) {
                available &= canDock;
                if (!canDock)
                    button.withTooltip(Component.translatable("screen.oritech_space_age.action.dock_requires_arrival"));
            }
            if (type == SpaceSimulation.ActionType.TRANSMIT_INFORMATION)
                available &= hasAntenna;
            if (type == SpaceSimulation.ActionType.DEPLOY_CUBESAT)
                available &= !RocketPayloads.all(craft).isEmpty();
            if (editedAction != null && type == editedAction.type()) selectListButton(button, true);
            else button.setActive(available);
            typeList.addChild(button);
        }
        typeList.setContentDimensions(popupWidth - 22, 8 + types.size() * 22);
        editors.addPopupComponent(typeList);
    }

    private void buildTargetMenu() {

        var popupWidth = Math.min(310, editors.panelWidth() - 12);
        var popupHeight = Math.min(250, editors.panelHeight() - 12);
        var px = Math.clamp(editors.dropdownX, 6, Math.max(6, editors.panelWidth() - popupWidth - 6));
        var py = Math.clamp(editors.dropdownY, 6, Math.max(6, editors.panelHeight() - popupHeight - 6));
        addDropdown(px, py, popupWidth, popupHeight, "screen.oritech_space_age.action.choose_target", true);
        editors.targetSearchField = editors.addPopupField(new EditBox(font, leftPos + px + 10, topPos + py + 27,
                popupWidth - 20, 18, Component.translatable("screen.oritech_space_age.action.search_targets")));
        editors.targetSearchField.setMaxLength(40);
        editors.targetSearchField.setValue(editors.targetSearch);
        editors.addPopupComponent(new ToggleWidget(px + 10, py + 49, popupWidth - 20,
                Component.translatable("screen.oritech_space_age.sun_filter"), FleetViewPreferences.showSun(), (toggle, value) ->
                editors.toggleSunStations()).withTextColor(LabelWidget.BRIGHT_TEXT).withZIndex(CONTROL_Z));
        editors.targetList = new ScrollWidget(px + 10, py + 71, popupWidth - 20, popupHeight - 81)
                .withVerticalScroll(true).withHorizontalScroll(false).withScrollSpeed(20);
        editors.targetList.setZIndex(CONTROL_Z);
        editors.targetOptions.clear();
        var editedAction = editors.findAction(editors.targetAction);
        if (editedAction != null && editedAction.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            var label = Component.translatable("screen.oritech_space_age.action.closest_asteroid");
            var button = SpaceAgeButtons.panel(4, 0, popupWidth - 36, 18, label, ignored -> editors.selectClosestAsteroid());
            selectListButton(button, editedAction.asteroidTarget().closest());
            editors.targetList.addChild(button);
            editors.targetOptions.add(new FlightPlannerEditors.TargetOption(button, label.getString().toLowerCase(Locale.ROOT)));
        }
        var objects = editors.currentDraftSnapshot().mapObjects().stream()
                .filter(object -> {
                    if (editedAction != null && editedAction.type() == SpaceSimulation.ActionType.BEAM_POWER)
                        return object.id().equals(SpaceObjects.EARTH_ID) || object.type() == SpaceObjects.ObjectType.CRAFT;
                    return editedAction == null || editedAction.type() != SpaceSimulation.ActionType.CONNECT_ASTEROID
                            || object.type() == SpaceObjects.ObjectType.ASTEROID;
                })
                .sorted(Comparator.comparingInt((SpaceSimulation.SpaceObjectData object) ->
                                object.type() == SpaceObjects.ObjectType.ASTEROID ? 1 : 0)
                        .thenComparing(object -> RocketStarMapWidget.objectName(object).getString(),
                                String.CASE_INSENSITIVE_ORDER)).toList();
        for (var object : objects) {
            var hiddenSun = !FleetViewPreferences.showSun() && editors.currentDraftSnapshot().stations().stream()
                    .anyMatch(station -> station.id().equals(object.id()) && station.position().target().equals(SpaceSimulation.SUN_ID));
            if (hiddenSun && (editedAction == null || !object.id().equals(editedAction.targetId()))) continue;
            var button = SpaceAgeButtons.panel(4, 0, popupWidth - 36, 18,
                    RocketStarMapWidget.objectName(object), ignored -> editors.selectNavigationTarget(object));
            selectListButton(button, editedAction != null && !editedAction.asteroidTarget().closest() && object.id().equals(editedAction.targetId()));
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
        var y = 4;
        for (var option : editors.targetOptions) {
            var visible = query.isEmpty() || option.searchText().contains(query);
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
                .withWrap(true).withBrightColor().withZIndex(CONTROL_Z));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + 35, (popupWidth - 24) / 2, 18,
                Component.translatable("screen.oritech_space_age.action.velocity_zero"),
                ignored -> editors.applyArrival(SpaceSimulation.ArrivalVelocityMode.ZERO, 0)).withZIndex(CONTROL_Z));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 14 + (popupWidth - 24) / 2, py + 35,
                (popupWidth - 24) / 2, 18,
                Component.translatable("screen.oritech_space_age.action.velocity_maximum"),
                ignored -> editors.applyArrival(SpaceSimulation.ArrivalVelocityMode.MAXIMUM, 0)).withZIndex(CONTROL_Z));
        editors.addPopupComponent(new LabelWidget(px + 10, py + 64, 70,
                Component.translatable("screen.oritech_space_age.action.velocity_custom_title"))
                .withBrightColor().withZIndex(CONTROL_Z));
        editors.arrivalField = editors.addPopupField(new EditBox(font, leftPos + px + 82, topPos + py + 59,
                popupWidth - 92, 18, Component.translatable("screen.oritech_space_age.action.velocity_custom_title")));
        editors.arrivalField.setMaxLength(10);
        editors.arrivalField.setValue(editors.arrivalText);
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, py + 104, 100, 18, Component.translatable("gui.done"),
                ignored -> editors.applyCustomArrival());
        apply.setZIndex(CONTROL_Z);
        editors.addPopupComponent(apply);
        validateField(editors.arrivalField, apply, value -> FlightPlannerLabels.parseArrivalVelocity(value) != null,
                value -> editors.arrivalText = value);
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + 104, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(CONTROL_Z));
        editors.focusPopup(editors.arrivalField);
    }

    private void buildDestinationMenu() {

        var action = editors.findAction(editors.destinationAction);
        var target = action == null ? null : editors.currentDraftSnapshot().objects().stream()
                .filter(object -> object.id().equals(action.targetId())).findFirst().orElse(null);
        if (action == null || target == null && !action.asteroidTarget().closest()) return;
        var orbits = RocketFlightPlanRules.availableOrbits(action.asteroidTarget().closest() ? SpaceObjects.ObjectType.ASTEROID : target.type());
        var surfaceCoordinates = action.targetId().equals(SpaceObjects.EARTH_ID)
                && editors.destinationOrbit == SpaceSimulation.OrbitBand.SURFACE;
        var popupWidth = Math.min(300, editors.panelWidth() - 12);
        var popupHeight = 34 + orbits.size() * 22 + (surfaceCoordinates ? 74 : 30);
        var px = Math.clamp(editors.dropdownX, 6, Math.max(6, editors.panelWidth() - popupWidth - 6));
        var py = Math.clamp(editors.dropdownY, 6, Math.max(6, editors.panelHeight() - popupHeight - 6));
        addDropdown(px, py, popupWidth, popupHeight, "screen.oritech_space_age.action.choose_destination", false);
        var rowY = py + 28;
        for (var orbit : orbits) {
            var button = SpaceAgeButtons.panel(px + 10, rowY, popupWidth - 20, 18,
                    RocketStarMapWidget.orbitName(orbit), ignored -> editors.selectDestinationOrbit(orbit));
            selectListButton(button, orbit == editors.destinationOrbit);
            editors.addPopupComponent(button.withZIndex(CONTROL_Z));
            rowY += 22;
        }
        if (surfaceCoordinates) {
            editors.addPopupComponent(new LabelWidget(px + 10, rowY + 5, popupWidth - 150,
                    Component.translatable("screen.oritech_space_age.action.setting.coordinates"))
                    .withBrightColor().withZIndex(CONTROL_Z));
            editors.addPopupComponent(SpaceAgeButtons.panel(px + popupWidth - 130, rowY, 120, 18,
                            Component.translatable("screen.oritech_space_age.action.launch_pad"), ignored -> editors.useLaunchPadCoordinates())
                    .withTooltip(Component.translatable("screen.oritech_space_age.action.launch_pad_tooltip")).withZIndex(CONTROL_Z));
            rowY += 20;
            editors.addPopupComponent(new LabelWidget(px + 10, rowY + 5, 18, Component.literal("X"))
                    .withBrightColor().withZIndex(CONTROL_Z));
            editors.landingXField = editors.addPopupField(new EditBox(font, leftPos + px + 28, topPos + rowY,
                    100, 18, Component.literal("X")));
            editors.landingXField.setValue(editors.landingXText);
            editors.addPopupComponent(new LabelWidget(px + 150, rowY + 5, 18, Component.literal("Z"))
                    .withBrightColor().withZIndex(CONTROL_Z));
            editors.landingZField = editors.addPopupField(new EditBox(font, leftPos + px + 168, topPos + rowY,
                    100, 18, Component.literal("Z")));
            editors.landingZField.setValue(editors.landingZText);
        }
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, py + popupHeight - 24, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.closeEditors()).withZIndex(CONTROL_Z));
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, py + popupHeight - 24, 100, 18,
                Component.translatable("gui.done"), ignored -> editors.applyDestination());
        apply.setZIndex(CONTROL_Z);
        editors.addPopupComponent(apply);
        if (surfaceCoordinates) {
            // Done needs both coordinates, so each field also checks the other's retained text.
            validateField(editors.landingXField, apply, value -> FlightPlannerLabels.parseInteger(value) != null
                    && FlightPlannerLabels.parseInteger(editors.landingZText) != null, value -> editors.landingXText = value);
            validateField(editors.landingZField, apply, value -> FlightPlannerLabels.parseInteger(value) != null
                    && FlightPlannerLabels.parseInteger(editors.landingXText) != null, value -> editors.landingZText = value);
            editors.focusPopup(editors.landingXField);
        }
    }

    private void buildAddonEditor() {

        var action = editors.findAction(editors.addonAction);
        if (action == null || action.addons().isEmpty()) return;
        var addon = action.addons().getFirst();
        var stationKeeping = action.type().stationKeeping();
        var types = Arrays.stream(SpaceSimulation.ActionAddonType.values())
                .filter(type -> stationKeeping ? type.isStationaryCondition() : type.isNavigationCondition()).toList();
        var columns = Math.min(3, types.size());
        var typeRows = (types.size() + columns - 1) / columns;
        var stationKeepingDetailsHeight = stationKeeping ? 40 : 0;
        var popupWidth = Math.min(350, editors.panelWidth() - 12);
        var popupHeight = 123 + typeRows * 32 + stationKeepingDetailsHeight;
        var px = (editors.panelWidth() - popupWidth) / 2;
        var py = Math.max(4, (editors.panelHeight() - popupHeight) / 2);
        addEditorBackdrop(px, py, popupWidth, popupHeight);
        editors.addPopupComponent(new LabelWidget(px + 10, py + 8, popupWidth - 48, 24,
                Component.translatable(stationKeeping
                        ? "screen.oritech_space_age.action.maintain_abort_help"
                        : "screen.oritech_space_age.action.completion_help"))
                .withWrap(true).withBrightColor().withZIndex(CONTROL_Z));
        LabelWidget stationKeepingDuration = stationKeeping ? new LabelWidget(px + 10, py + 38, popupWidth - 20,
                Component.empty()).withBrightColor() : null;
        LabelWidget stationKeepingEnd = stationKeeping ? new LabelWidget(px + 10, py + 56, popupWidth - 20,
                Component.empty()).withBrightColor() : null;
        if (stationKeeping) {
            updateStationKeepingLabels(action, stationKeepingDuration, stationKeepingEnd);
            stationKeepingDuration.setZIndex(CONTROL_Z);
            stationKeepingEnd.setZIndex(CONTROL_Z);
            editors.addPopupComponent(stationKeepingDuration);
            editors.addPopupComponent(stationKeepingEnd);
        }
        var typeWidth = (popupWidth - 20 - (columns - 1) * 4) / columns;
        for (int index = 0; index < types.size(); index++) {
            var type = types.get(index);
            var column = index % columns;
            var row = index / columns;
            var button = SpaceAgeButtons.panel(px + 10 + column * (typeWidth + 4),
                    py + 38 + stationKeepingDetailsHeight + row * 32, typeWidth, 28,
                    FlightPlannerLabels.addonTypeName(type), ignored -> editors.selectAddonType(type));
            button.setActive(type != addon.type());
            editors.addPopupComponent(button.withTooltip(FlightPlannerLabels.addonTooltip(type)).withZIndex(CONTROL_Z));
        }
        var valueY = py + 44 + stationKeepingDetailsHeight + typeRows * 32;
        editors.addPopupComponent(new LabelWidget(px + 10, valueY + 5, 72,
                Component.translatable("screen.oritech_space_age.action.condition_value"))
                .withBrightColor().withZIndex(CONTROL_Z));
        editors.addonValueField = editors.addPopupField(new EditBox(font, leftPos + px + 82, topPos + valueY,
                popupWidth - 172, 18, Component.translatable("screen.oritech_space_age.action.condition_value")));
        editors.addonValueField.setMaxLength(10);
        editors.addonValueField.setValue(editors.addonValueText);
        editors.addPopupComponent(new LabelWidget(px + popupWidth - 82, valueY + 5, 72,
                FlightPlannerLabels.addonUnit(addon.type())).withBrightColor().withZIndex(CONTROL_Z));
        var footerY = py + popupHeight - 28;
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 10, footerY, 100, 18,
                Component.translatable("gui.cancel"), ignored -> editors.cancelAddonEditor()).withZIndex(CONTROL_Z));
        editors.addPopupComponent(SpaceAgeButtons.darkPanel(px + (popupWidth - 100) / 2, footerY, 100, 18,
                Component.translatable("screen.oritech_space_age.action.remove_condition"),
                ignored -> editors.removeEditedNavigationAddon()).withZIndex(CONTROL_Z));
        var apply = SpaceAgeButtons.panel(px + popupWidth - 110, footerY, 100, 18, Component.translatable("gui.done"),
                ignored -> editors.applyAddonValue());
        apply.setZIndex(CONTROL_Z);
        editors.addPopupComponent(apply);
        validateField(editors.addonValueField, apply, value -> FlightPlannerLabels.parseAddonValue(addon.type(), value) != null, value -> {
            editors.addonValueText = value;
            // Valid values preview immediately so the station-keeping estimate follows the input.
            if (editors.previewAddonValue(value))
                updateStationKeepingLabels(action, stationKeepingDuration, stationKeepingEnd);
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

        var popupWidth = 176;
        var popupHeight = 30;
        var px = Math.clamp(editors.mapContextRequest.mouseX(), 6, Math.max(6, editors.panelWidth() - popupWidth - 6));
        var py = Math.clamp(editors.mapContextRequest.mouseY(), 6, Math.max(6, editors.panelHeight() - popupHeight - 6));
        addContextMenuBackdrop();
        editors.addPopupComponent(new SurfaceWidget(px, py, popupWidth, popupHeight, OritechSurface.PANEL_DARK)
                .withZIndex(SURFACE_Z));
        editors.addPopupComponent(SpaceAgeButtons.panel(px + 6, py + 6, popupWidth - 12, 18,
                Component.translatable(editors.currentDraftSnapshot().stations().stream().anyMatch(t -> t.id().equals(editors.mapContextRequest.selection().objectId()))
                        ? "screen.oritech_space_age.service.add_dock" : "screen.oritech_space_age.action.add_navigation_target"),
                ignored -> editors.addNavigationTargetFromMap()).withZIndex(CONTROL_Z));
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
        backdrop.setZIndex(SURFACE_Z);
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
        backdrop.setZIndex(DISMISS_Z);
        editors.addPopupComponent(backdrop);
    }

}
