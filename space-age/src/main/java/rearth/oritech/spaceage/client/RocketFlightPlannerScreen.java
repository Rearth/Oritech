package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.spaceage.block.MissionControlMenu;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.network.RocketNetworking;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.MissionForecast;
import rearth.oritech.spaceage.simulation.MissionState;
import rearth.oritech.spaceage.simulation.NavigationComputerRules;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Edits mission intent while the analytical calculator supplies paths and automatic booster events.
 */
public class RocketFlightPlannerScreen extends FlightPlannerEditors {

    // Keep a small border around the resizable screen panel.
    private static final int WINDOW_PADDING = 6;
    // Inventory passed back when switching to the assembler screen.
    private final Inventory screenInventory;
    // Title retained when switching to the assembler screen.
    private final Component screenTitle;
    private final List<LabelWidget> currentActionLabels = new ArrayList<>();
    // Last assembler preview displayed; a new revision replaces the rocket model.
    private int previewRevision = -1;
    // Last server planner snapshot displayed.
    private int flightPlannerRevision = -1;
    // Revision which initialized the local draft, so rebuilding does not overwrite edits.
    private int draftSourceRevision = -1;
    // Mission state stays with the screen because the map and cards share it.
    // Mission currently being edited and shared with the assembler menu.
    private SpaceSimulation.FlightPlan draftPlan = SpaceSimulation.FlightPlan.empty();
    // Current preview used by the map, cards and editor availability checks.
    private RocketFlightPathCalculator.FlightPath calculatedFlight =
            new RocketFlightPathCalculator.FlightPath(List.of(), List.of(), 0);
    private List<RocketFlightPlanRules.Issue> validationIssues = List.of();
    // Map widget whose view is retained across component rebuilds.
    private RocketStarMapWidget flightPlanMap;
    // Assembly used to validate and calculate the current draft.
    private ActiveRocketData flightPlanRocket;
    // Branch-card area; popup positioning uses its content coordinates.
    private ScrollWidget flightPlanActionScroll;
    // Saved horizontal card scroll when components are rebuilt.
    private float flightPlanActionScrollX;
    // Saved vertical card scroll when components are rebuilt.
    private float flightPlanActionScrollY;
    // Branch selected for new cards and highlighted on the map.
    private UUID activeBranchId;
    // Map target used as the default for new navigation cards.
    private RocketStarMapWidget.NavigationSelection selectedTarget =
            new RocketStarMapWidget.NavigationSelection(SpaceObjects.EARTH_ID, SpaceSimulation.OrbitBand.LOW);
    // Current screen panel width in GUI pixels.
    private int panelWidth;
    // Current screen panel height in GUI pixels.
    private int panelHeight;
    // Top of the branch-card area below the map.
    private int editorY;
    // Height available to scroll through branch cards.
    private int editorHeight;
    private ButtonWidget sendMissionButton;
    private LabelWidget missionStateLabel;
    private LabelWidget capacityLabel;
    private int fleetTicks;
    private int recipeRevision = -1;
    private boolean pendingMissionRefresh;

    private SpaceSimulation.FlightPlan disconnectedDraft;

    public RocketFlightPlannerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title);
        screenInventory = inventory;
        screenTitle = title;
    }

    private static ArrayList<SpaceSimulation.FlightPlanAction> withoutGenerated(
            List<SpaceSimulation.FlightPlanAction> actions) {

        var result = new ArrayList<SpaceSimulation.FlightPlanAction>();
        actions.stream().filter(action -> !action.isGenerated()).forEach(result::add);
        return result;
    }

    private static List<SpaceSimulation.SegmentRef> segmentRefs(ActiveRocketData rocket) {

        return rocket.getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of)
                .sorted(Comparator.comparingInt((SpaceSimulation.SegmentRef ref) -> ref.anchor().getY())
                        .thenComparingInt(ref -> ref.anchor().getX())
                        .thenComparingInt(ref -> ref.anchor().getZ())).toList();
    }

    private static boolean canConnectAfter(SpaceSimulation.FlightPlanAction action) {

        return switch (action.velocityMode()) {
            case ZERO -> true;
            case CUSTOM -> action.targetVelocity() <= AsteroidImpactRules.MAX_ASTEROID_CONNECTION_SPEED;
            case MAXIMUM -> false;
        };
    }

    boolean isReadOnly() {

        return menu instanceof MissionControlMenu control && !control.connected;
    }

    Component currentActionLabel() {

        return currentActionLabel(0);
    }

    private Component currentActionLabel(long extraTicks) {

        if (!(menu instanceof MissionControlMenu control)
                || control.selectedTelemetry == null)
            return Component.translatable("screen.oritech_space_age.mission.now");
        var path = calculatedFlight.paths().stream().filter(item -> item.branchId().equals(draftPlan.root().id()))
                .findFirst().orElse(null);
        var progress = MissionControlScreen.progressPercent(control.selectedTelemetry, path, extraTicks);
        return progress < 0 ? Component.translatable("screen.oritech_space_age.mission.now")
                : Component.translatable("screen.oritech_space_age.mission.percent", progress);
    }

    List<SpaceSimulation.FlightPlanAction> completedActions() {

        return menu instanceof MissionControlMenu control ? control.completed : List.of();
    }

    boolean isCurrentAction(UUID id) {

        return menu instanceof MissionControlMenu control && id.equals(control.currentAction);
    }

    void refreshCurrentMission(SpaceSimulation.FlightPlannerSnapshot snapshot, boolean commandsAvailable) {

        var edited = hasMissionChanges();
        if (!commandsAvailable) {
            if (!isReadOnly() && edited) disconnectedDraft = draftPlan;
            closeEditorState();
            draftPlan = snapshot.plan();
        } else if (disconnectedDraft != null) {
            draftPlan = disconnectedDraft;
            disconnectedDraft = null;
        } else if (!edited) {
            draftPlan = snapshot.plan();
        } else if (menu instanceof MissionControlMenu control) {
            var completed = control.completed.stream().map(SpaceSimulation.FlightPlanAction::id)
                    .collect(Collectors.toSet());
            draftPlan = RocketFlightPlanRules.normalize(draftPlan.withBranches(draftPlan.branches().stream()
                    .map(branch -> branch.withActions(branch.actions().stream()
                            .filter(action -> !completed.contains(action.id())).toList()))
                    .toList()));
        }
        menu.setDraftFlightPlan(draftPlan);
        menu.setFlightPlannerSnapshot(snapshot);
    }

    void receiveLiveMission(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket,
                            boolean structureChanged) {

        menu.updatePreviewData(rocket);
        menu.updateFlightPlannerSnapshotData(snapshot);
        flightPlanRocket = rocket;
        if (structureChanged || pendingMissionRefresh) {
            pendingMissionRefresh = true;
            updateMissionStateLabel();
            return;
        }
        refreshLivePresentation();
    }

    private void refreshLivePresentation() {

        if (flightPlanRocket == null || flightPlanMap == null) return;
        calculatedFlight = calculateDraft(currentDraftSnapshot());
        updateValidation();
        flightPlanMap.updateFlightPath(currentDraftSnapshot(), calculatedFlight, activeBranchId);
        if (menu instanceof MissionControlMenu) {
            flightPlanMap.setFleetTimes(Map.of(draftPlan.root().id(), livePathTime()));
            flightPlanMap.setCraftLabels(Map.of(draftPlan.root().id(),
                    Component.translatable("screen.oritech_space_age.mission.current_position")));
        }
        updateMissionStateLabel();
        var progress = currentActionLabel();
        currentActionLabels.forEach(label -> label.setText(progress));
        updateSendButton();
    }

    private void advanceLivePresentation() {

        if (!(menu instanceof MissionControlMenu control)
                || control.selectedTelemetry == null || flightPlanMap == null
                || Minecraft.getInstance().level == null) return;
        var elapsedTicks = Math.max(0,
                Minecraft.getInstance().level.getGameTime() - control.receivedWorldTick) * control.debugSpeed;
        var path = calculatedFlight.paths().stream()
                .filter(item -> item.branchId().equals(draftPlan.root().id())).findFirst().orElse(null);
        if (path != null) flightPlanMap.setFleetTimes(Map.of(draftPlan.root().id(),
                // Cached navigation starts at the action's beginning, not the latest telemetry update.
                Math.min(livePathTime() + elapsedTicks / 20.0, path.durationSeconds())));
        var progress = currentActionLabel(elapsedTicks);
        currentActionLabels.forEach(label -> label.setText(progress));
    }

    private Component missionStateText(MissionControlMenu control) {

        var description = MissionState.isRecovered(control.missionStatus)
                ? Component.translatable("screen.oritech_space_age.mission.recovered_summary")
                : Component.translatable("screen.oritech_space_age.mission.state_location",
                MissionControlScreen.readableStatus(control.missionStatus),
                MissionControlScreen.location(control.selectedTelemetry, currentDraftSnapshot().objects()));
        return Component.translatable(isReadOnly() ? "screen.oritech_space_age.mission.read_only_state"
                : "screen.oritech_space_age.mission.connected_state", description);
    }

    private void updateMissionStateLabel() {

        if (missionStateLabel == null
                || !(menu instanceof MissionControlMenu control)
                || control.selectedTelemetry == null) return;
        missionStateLabel.setText(missionStateText(control));
        missionStateLabel.withColor(isReadOnly() ? 0xFF65532C : 0xFF285522);
        missionStateLabel.setTooltip(List.of(Component.translatable(isReadOnly()
                ? "screen.oritech_space_age.mission.read_only_state_tooltip"
                : "screen.oritech_space_age.mission.connected_state_tooltip")));
    }

    void registerCurrentActionLabel(LabelWidget label) {

        currentActionLabels.add(label);
    }

    List<RocketFlightPlanRules.Issue> computerIssues() {

        return validationIssues;
    }

    Component capacityText() {

        return Component.translatable("screen.oritech_space_age.computer.capacity", editableActionCount(),
                NavigationComputerRules.capacity(flightPlanRocket));
    }

    private void updateCapacityLabel() {

        if (capacityLabel == null || flightPlanRocket == null) return;
        var computers = NavigationComputerRules.computers(flightPlanRocket,
                NavigationComputerRules.allSegments(flightPlanRocket));
        capacityLabel.setText(capacityText());
        capacityLabel.withColor(computerIssues().isEmpty() ? LabelWidget.DARK_TEXT : 0xFFB53525);
        var tooltip = new ArrayList<Component>();
        tooltip.add(Component.translatable("screen.oritech_space_age.computer.capacity_help", editableActionCount(),
                NavigationComputerRules.capacity(flightPlanRocket), computers));
        tooltip.add(Component.translatable("screen.oritech_space_age.computer.automation_help"));
        computerIssues().stream().map(RocketFlightPlanRules.Issue::description).forEach(tooltip::add);
        capacityLabel.setTooltip(tooltip);
    }

    boolean canAddStep() {

        return flightPlanRocket != null && editableActionCount() < NavigationComputerRules.capacity(flightPlanRocket);
    }

    private boolean hasMissionChanges() {

        if (!(menu instanceof MissionControlMenu control) || control.acceptedPlan == null)
            return false;
        return !editableProgram(draftPlan).equals(editableProgram(control.acceptedPlan));
    }

    private SpaceSimulation.FlightPlan editableProgram(SpaceSimulation.FlightPlan plan) {

        // Derived empty booster branches and already detached segment settings are not user edits.
        var normalized = RocketFlightPlanRules.normalize(plan);
        var branches = normalized.branches().stream().map(branch -> branch.withActions(branch.actions().stream()
                        .filter(action -> !action.isGenerated()).toList()))
                .filter(branch -> branch.isRoot() || !branch.actions().isEmpty()).toList();
        var segments = flightPlanRocket.getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of).toList();
        return new SpaceSimulation.FlightPlan(branches, plan.segmentConfigurations().stream()
                .filter(configuration -> segments.contains(configuration.segment())).toList(), plan.name());
    }

    private void updateSendButton() {

        if (sendMissionButton == null || !(menu instanceof MissionControlMenu control))
            return;
        var issues = computerIssues();
        sendMissionButton.setActive(control.connected && hasMissionChanges() && issues.isEmpty());
        if (!issues.isEmpty()) {
            sendMissionButton.withTooltip(issues.getFirst().description());
            return;
        }
        sendMissionButton.withTooltip(Component.translatable(!control.connected ? "screen.oritech_space_age.mission.send_unavailable"
                : hasMissionChanges() ? "screen.oritech_space_age.mission.send_tooltip" : "screen.oritech_space_age.mission.no_changes"));
    }

    @Override
    protected SpaceSimulation.FlightPlan draftPlan() {

        return draftPlan;
    }

    @Override
    protected RocketFlightPathCalculator.FlightPath calculatedFlight() {

        return calculatedFlight;
    }

    @Override
    protected ActiveRocketData flightPlanRocket() {

        return flightPlanRocket;
    }

    @Override
    protected ScrollWidget flightPlanActionScroll() {

        return flightPlanActionScroll;
    }

    @Override
    protected UUID activeBranchId() {

        return activeBranchId;
    }

    @Override
    protected RocketStarMapWidget.NavigationSelection selectedTarget() {

        return selectedTarget;
    }

    @Override
    protected void setSelectedTarget(RocketStarMapWidget.NavigationSelection target) {

        selectedTarget = target;
    }

    @Override
    protected int panelWidth() {

        return panelWidth;
    }

    @Override
    protected int panelHeight() {

        return panelHeight;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        if (doubleClick && !hasOpenPopup() && flightPlanMap != null
                && flightPlanMap.handleDoubleClick(event.x() - leftPos, event.y() - topPos, event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void buildComponents() {

        missionStateLabel = null;
        capacityLabel = null;
        panelWidth = width - WINDOW_PADDING * 2;
        panelHeight = height - WINDOW_PADDING * 2;
        setPanelSize(panelWidth, panelHeight);
        previewRevision = menu.getPreviewRevision();
        flightPlannerRevision = menu.getFlightPlannerRevision();
        addComponent(new SurfaceWidget(0, 0, panelWidth, panelHeight, OritechSurface.PANEL));

        var rocketTab = SpaceAgeButtons.panel(9, 9, 92, 20,
                menu instanceof MissionControlMenu ? Component.translatable("screen.oritech_space_age.mission.fleet") : Component.translatable("screen.oritech_space_age.rocket"), ignored -> switchToRocketScreen());
        rocketTab.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT);
        addComponent(rocketTab);
        var flightPlanTab = SpaceAgeButtons.panel(101, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.flight_plan"), ignored -> {
                });
        flightPlanTab.setActive(false);
        flightPlanTab.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
        addComponent(flightPlanTab);
        addComponent(SpaceAgeButtons.panel(200, 9, 70, 20, Component.translatable("screen.oritech_space_age.mission.save_card"), ignored -> card(true)));
        var loadCard = SpaceAgeButtons.panel(273, 9, 70, 20, Component.translatable("screen.oritech_space_age.mission.load_card"), ignored -> card(false));
        loadCard.setActive(!isReadOnly());
        addComponent(loadCard);
        if (menu instanceof MissionControlMenu control) {
            sendMissionButton = SpaceAgeButtons.orangePanel(346, 9, 95, 20, Component.translatable("screen.oritech_space_age.mission.send_changes"), ignored -> {
                ClientPacketDistributor.sendToServer(
                        new RocketNetworking.SubmitFlightPlanPayload(menu.blockPos, flightPlanRocket.getRocketId(), draftPlan));
            });
            addComponent(sendMissionButton);

        }
        buildFlightPlanTab();
        updateSendButton();
        if (menu instanceof MissionControlMenu control && control.selectedTelemetry != null) {
            missionStateLabel = new LabelWidget(12, 36, panelWidth - 24, 22,
                    missionStateText(control)).withWrap(true)
                    .withColor(isReadOnly() ? 0xFF65532C : 0xFF285522);
            missionStateLabel.withTooltip(Component.translatable(isReadOnly()
                    ? "screen.oritech_space_age.mission.read_only_state_tooltip"
                    : "screen.oritech_space_age.mission.connected_state_tooltip"));
            addComponent(missionStateLabel);
        }
        if (flightPlanMap != null) flightPlanMap.setTooltipsEnabled(!hasOpenPopup());
        buildEditors();
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

        var mapY = menu instanceof MissionControlMenu ? 65 : 39;
        capacityLabel = new LabelWidget(panelWidth - 70, panelHeight - 17, 58, capacityText())
                .withDarkColor().withAlignment(LabelWidget.Alignment.RIGHT);
        updateCapacityLabel();
        addComponent(capacityLabel);
        var availableHeight = panelHeight - mapY - 24;
        editorHeight = Math.min(Math.clamp(availableHeight / 3, 160, 270), Math.max(64, availableHeight - 112));
        var mapHeight = availableHeight - editorHeight - 6;
        editorY = mapY + mapHeight + 6;
        var previousMap = flightPlanMap;
        flightPlanMap = new RocketStarMapWidget(12, mapY, panelWidth - 24, mapHeight,
                currentDraftSnapshot(), calculatedFlight, rocket, activeBranchId, selectedTarget,
                this::selectMapTarget, this::openMapContextMenu);
        flightPlanMap.copyViewFrom(previousMap);
        if (menu instanceof MissionControlMenu) {
            flightPlanMap.setShowSummary(false);
            flightPlanMap.setFleetTimes(Map.of(draftPlan.root().id(), livePathTime()));
            flightPlanMap.setCraftLabels(Map.of(draftPlan.root().id(),
                    Component.translatable("screen.oritech_space_age.mission.current_position")));
        }
        addComponent(flightPlanMap);
        addFlightPlanEditor(rocket);
    }

    private double livePathTime() {

        if (menu instanceof MissionControlMenu control && control.selectedTelemetry != null
                && draftPlan.equals(control.selectedTelemetry.plan())
                && control.fleet.stream().anyMatch(entry -> entry.id().equals(control.selected) && !entry.navigation().paths().isEmpty()))
            return control.selectedTelemetry.actionTicks() / 20.0;
        return 0;
    }

    private RocketFlightPathCalculator.FlightPath calculateDraft(SpaceSimulation.FlightPlannerSnapshot snapshot) {

        if (menu instanceof MissionControlMenu control) {
            var report = control.selectedTelemetry;
            var fleetEntry = control.fleet.stream().filter(entry -> entry.id().equals(control.selected)).findFirst().orElse(null);
            if (report != null && draftPlan.equals(report.plan()) && fleetEntry != null && !fleetEntry.navigation().paths().isEmpty())
                return fleetEntry.navigation().flight();
            var current = draftPlan.root().actions().stream().filter(action -> !action.isGenerated()).findFirst();
            var reported = report == null ? Optional.<SpaceSimulation.FlightPlanAction>empty()
                    : report.plan().root().actions().stream().filter(action -> !action.isGenerated()).findFirst();
            var serviceTicks = report != null && current.equals(reported) ? report.actionTicks() : 0;
            var forecastPlan = MissionForecast.remainingServices(draftPlan, serviceTicks, report != null && current.equals(reported) ? report.serviceProgress() : 0);
            return RocketFlightPathCalculator.calculateFrom(flightPlanRocket, snapshot.objects(), forecastPlan, control.knownPosition,
                    SpaceAgeClientRecipes.vacuum(), snapshot.stations());
        }
        return RocketFlightPathCalculator.calculateFrom(flightPlanRocket, snapshot.objects(), draftPlan, null, SpaceAgeClientRecipes.vacuum(), snapshot.stations());
    }

    private void card(boolean save) {

        if (!save && isReadOnly()) return;
        ClientPacketDistributor.sendToServer(
                new MissionNetworking.CardRequest(save, draftPlan));
    }

    private void recalculateAndSynchronize() {

        var snapshot = currentDraftSnapshot();
        calculatedFlight = calculateDraft(snapshot);
        var synchronizedPlan = RocketFlightPlanRules.synchronizeBoosterEvents(
                draftPlan, calculatedFlight.boosterEvents());
        if (!synchronizedPlan.equals(draftPlan)) {
            draftPlan = synchronizedPlan;
            menu.setDraftFlightPlan(draftPlan);
            calculatedFlight = calculateDraft(snapshot);
        }
        updateValidation();
    }

    private void updateValidation() {

        validationIssues = RocketFlightPlanRules.inspect(draftPlan, flightPlanRocket, currentDraftSnapshot().objects(),
                !(menu instanceof MissionControlMenu), menu instanceof MissionControlMenu c ? c.knownPosition : null,
                SpaceAgeClientRecipes.vacuum(), currentDraftSnapshot().stations(), calculatedFlight).issues();
    }

    private void selectMapTarget(RocketStarMapWidget.NavigationSelection selection) {

        selectedTarget = selection;
    }

    private void openMapContextMenu(RocketStarMapWidget.NavigationContextRequest request) {

        if (isReadOnly()) return;
        closeEditorState();
        mapContextRequest = request;
        rebuildComponents();
    }

    private void addFlightPlanEditor(ActiveRocketData rocket) {

        currentActionLabels.clear();
        flightPlanActionScroll = new FlightPlannerCards(this).build(rocket, editorY, panelWidth, editorHeight,
                flightPlanActionScrollX, flightPlanActionScrollY);
        addComponent(flightPlanActionScroll);
    }

    @Override
    protected void addAction(UUID branchId, ActiveRocketData rocket, SpaceSimulation.ActionType type) {

        var branch = findBranch(branchId);
        if (branch == null || !canAddStep()) return;
        var action = SpaceSimulation.FlightPlanAction.create(type);
        action = configureAction(action, type, branch, action.id());
        if (action == null) return;
        var actions = withoutGenerated(branch.actions());
        actions.add(action);
        updateBranchActions(branchId, actions, rocket);
    }

    void removeAction(UUID branchId, int index, ActiveRocketData rocket) {

        var branch = findBranch(branchId);
        if (branch == null || index < 0 || index >= branch.actions().size()
                || branch.actions().get(index).isGenerated()) return;
        var actions = new ArrayList<>(branch.actions());
        actions.remove(index);
        updateBranchActions(branchId, withoutGenerated(actions), rocket);
    }

    void moveAction(UUID branchId, int index, int direction, ActiveRocketData rocket) {

        var branch = findBranch(branchId);
        if (branch == null || branch.actions().get(index).isGenerated()) return;
        var editable = withoutGenerated(branch.actions());
        var action = branch.actions().get(index);
        var current = editable.indexOf(action);
        var target = current + direction;
        if (target < 0 || target >= editable.size()) return;
        Collections.swap(editable, current, target);
        updateBranchActions(branchId, editable, rocket);
    }

    @Override
    protected void cycleActionParameter(UUID branchId, int index, ActiveRocketData rocket) {

        var branch = findBranch(branchId);
        if (branch == null) return;
        var action = branch.actions().get(index);
        if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            var objects = currentDraftSnapshot().objects();
            if (objects.isEmpty()) return;
            var current = objects.stream().map(SpaceSimulation.SpaceObjectData::id).toList().indexOf(action.targetId());
            var target = objects.get((current + 1) % objects.size());
            var changed = action.withTarget(target.id()).withOrbit(
                    RocketFlightPlanRules.compatibleOrbit(target.type(), action.orbit()));
            selectedTarget = new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit());
            replaceAction(branchId, action.id(), changed, rocket);
        } else if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            var anchors = RocketFlightPlanRules.asteroidAnchorSegments(rocket);
            if (anchors.isEmpty()) return;
            var current = action.segments().isEmpty() ? null : action.segments().getFirst();
            var anchorIndex = anchors.indexOf(current);
            replaceAction(branchId, action.id(), action.withSegments(
                    List.of(anchors.get((anchorIndex + 1) % anchors.size()))), rocket);
        } else if (action.type() == SpaceSimulation.ActionType.DECOUPLE) {
            if (!action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) return;
            var pairs = connectedPairs(rocket);
            if (pairs.isEmpty()) return;
            var current = pairs.indexOf(action.segments());
            replaceAction(branchId, action.id(), action.withSegments(pairs.get((current + 1) % pairs.size())), rocket);
        }
    }

    void adjustActionVelocity(UUID branchId, int index, int direction, ActiveRocketData rocket) {

        var branch = findBranch(branchId);
        if (branch == null) return;
        var action = branch.actions().get(index);
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO
                || action.velocityMode() != SpaceSimulation.ArrivalVelocityMode.CUSTOM) return;
        var step = action.targetVelocity() < 1_000 ? 10 : 100;
        var velocity = Math.clamp(action.targetVelocity() + (long) step * direction, 0, 100_000);
        replaceAction(branchId, action.id(), action.withVelocity(action.velocityMode(), velocity), rocket);
    }

    @Override
    protected void replaceAction(UUID branchId, UUID actionId, SpaceSimulation.FlightPlanAction changed,
                                 ActiveRocketData rocket) {

        var branch = findBranch(branchId);
        if (branch == null) return;
        var actions = withoutGenerated(branch.actions());
        var index = actions.stream().map(SpaceSimulation.FlightPlanAction::id).toList().indexOf(actionId);
        if (index < 0) return;
        actions.set(index, RocketFlightPlanRules.applyLandingUncertainty(changed));
        updateBranchActions(branchId, actions, rocket);
    }

    private void updateBranchActions(UUID branchId, List<SpaceSimulation.FlightPlanAction> actions,
                                     ActiveRocketData rocket) {

        if (isReadOnly()) return;
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
        updateCapacityLabel();
        updateSendButton();
    }

    void selectBranch(UUID branchId) {

        activeBranchId = branchId;
        refreshFlightPlanner();
    }

    @Override
    protected SpaceSimulation.FlightPlanBranch findBranch(UUID branchId) {

        return draftPlan.branches().stream().filter(branch -> branch.id().equals(branchId)).findFirst().orElse(null);
    }

    @Override
    protected SpaceSimulation.FlightPlanAction findAction(UUID actionId) {

        return draftPlan.branches().stream().flatMap(branch -> branch.actions().stream())
                .filter(action -> action.id().equals(actionId)).findFirst().orElse(null);
    }

    private int editableActionCount() {

        return (int) draftPlan.branches().stream().flatMap(branch -> branch.actions().stream())
                .filter(action -> !action.isGenerated()).count();
    }

    @Override
    protected SpaceSimulation.FlightPlannerSnapshot currentDraftSnapshot() {

        var source = menu.getFlightPlannerSnapshot();
        if (source == null) throw new IllegalStateException("Flight planner snapshot is not loaded");
        return new SpaceSimulation.FlightPlannerSnapshot(
                source.simulationId(), source.rocketId(), source.objects(), draftPlan, source.stations());
    }

    private void submitFlightPlanIfDirty() {

        RocketAssemblerClientController.submitFlightPlanIfDirty(menu);
    }

    @Override
    protected Component actionParameter(SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket) {

        if (action.type() == SpaceSimulation.ActionType.PROCESS)
            return Component.translatable("screen.oritech_space_age.service.processing_summary", action.settings().processing().modules().size());
        if (action.type() == SpaceSimulation.ActionType.DOCK)
            return currentDraftSnapshot().stations().stream().filter(t -> t.id().equals(action.settings().docking().host())).findFirst()
                    .map(t -> Component.literal(t.name())).orElse(Component.translatable("screen.oritech_space_age.service.choose_host"));
        if (action.type() == SpaceSimulation.ActionType.UNDOCK)
            return Component.translatable("screen.oritech_space_age.service.undock_help");
        if (action.type() == SpaceSimulation.ActionType.SCAN)
            return Component.translatable("screen.oritech_space_age.action.scan_instant");
        if (action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION)
            return Component.translatable("screen.oritech_space_age.action.upload_survey_data");
        if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            var target = findObject(action.targetId());
            return target == null ? Component.translatable("screen.oritech_space_age.action.no_target")
                    : RocketStarMapWidget.objectName(target);
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
            var retained = segmentName(action.segments().get(0), rocket);
            var detached = segmentName(action.segments().get(1), rocket);
            return Component.translatable("screen.oritech_space_age.action.decouple_summary",
                    retained, detached, retained, detached);
        }
        return Component.translatable("screen.oritech_space_age.action.no_parameter");
    }

    @Override
    protected Component actionOrbit(SpaceSimulation.FlightPlanAction action) {

        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO
                && action.type() != SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            return Component.translatable("screen.oritech_space_age.action.no_scope");
        }
        if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            return Component.translatable("screen.oritech_space_age.action.asteroid_connection_range",
                    (int) AsteroidImpactRules.MAX_ASTEROID_CONNECTION_SPEED);
        }
        var target = findObject(action.targetId());
        if (target != null && target.type() == SpaceObjects.ObjectType.SURVEY_REGION) {
            return Component.translatable("screen.oritech_space_age.region_boundary");
        }
        if (target != null && target.type() == SpaceObjects.ObjectType.CRAFT)
            return Component.translatable("screen.oritech_space_age.action.rendezvous");
        return RocketStarMapWidget.orbitName(action.orbit());
    }

    @Override
    protected String segmentName(SpaceSimulation.SegmentRef ref, ActiveRocketData rocket) {

        var customName = draftPlan.configurationFor(ref).name();
        if (!customName.isBlank()) return customName;
        var index = segmentRefs(rocket).indexOf(ref);
        return index < 0 ? "?" : "S" + (index + 1);
    }

    @Override
    protected List<List<SpaceSimulation.SegmentRef>> connectedPairs(ActiveRocketData rocket) {

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

        var contentHeight = panelHeight - 51;
        addComponent(new SurfaceWidget(12, 39, panelWidth - 24, contentHeight, OritechSurface.PANEL_INSET));
        var label = new LabelWidget(32, 39 + contentHeight / 2 - 15, panelWidth - 64, 30, message);
        label.withAlignment(LabelWidget.Alignment.CENTER).withBrightColor().withWrap(true);
        addComponent(label);
    }

    private void switchToRocketScreen() {

        submitFlightPlanIfDirty();
        Minecraft.getInstance().setScreen(menu instanceof MissionControlMenu control
                ? new MissionControlScreen(control, screenInventory, screenTitle) : new RocketAssemblerScreen(menu, screenInventory, screenTitle));
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
        if (menu instanceof MissionControlMenu control) {
            if (++fleetTicks >= 20) {
                fleetTicks = 0;
                ClientPacketDistributor.sendToServer(
                        new MissionNetworking.FleetRequest(new UUID(0, 0), false));
            }
            if (pendingMissionRefresh && !hasOpenPopup() && menu.getFlightPlannerSnapshot() != null) {
                pendingMissionRefresh = false;
                refreshCurrentMission(menu.getFlightPlannerSnapshot(), control.connected);
                rebuildComponents();
                return;
            }
            if (!pendingMissionRefresh) advanceLivePresentation();
        }
        if (recipeRevision != SpaceAgeClientRecipes.revision) {
            recipeRevision = SpaceAgeClientRecipes.revision;
            rebuildComponents();
            return;
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

    @Override
    protected SpaceSimulation.SpaceObjectData selectedObject() {

        return findObject(selectedTarget.objectId());
    }

    private String objectName(UUID id) {

        var object = findObject(id);
        return object == null ? "?" : RocketStarMapWidget.objectName(object).getString();
    }

    private SpaceSimulation.SpaceObjectData findObject(UUID id) {

        return currentDraftSnapshot().mapObjects().stream().filter(object -> object.id().equals(id))
                .findFirst().orElse(null);
    }

    RocketFlightPathCalculator.ArrivalPrediction arrivalPrediction(UUID actionId) {

        return calculatedFlight.arrivalPredictions().stream()
                .filter(prediction -> prediction.actionId().equals(actionId)).findFirst().orElse(null);
    }

    double actionDurationSeconds(UUID actionId) {

        for (var path : calculatedFlight.paths()) {
            var previousTime = path.samples().isEmpty() ? 0 : path.samples().getFirst().timeSeconds();
            for (var moment : path.actionMoments()) {
                if (moment.actionId().equals(actionId)) return Math.max(0, moment.timeSeconds() - previousTime);
                previousTime = moment.timeSeconds();
            }
        }
        return Double.NaN;
    }

    @Override
    protected boolean canAddNavigationAddon(SpaceSimulation.FlightPlanBranch branch,
                                            SpaceSimulation.FlightPlanAction action) {

        var path = calculatedFlight.paths().stream().filter(item -> item.branchId().equals(branch.id()))
                .findFirst().orElse(null);
        if (path == null) return false;
        var moment = path.actionMoments().stream().filter(item -> item.actionId().equals(action.id()))
                .findFirst().orElse(null);
        return NavigationComputerRules.computers(flightPlanRocket,
                moment == null ? NavigationComputerRules.allSegments(flightPlanRocket)
                        : moment.connectedSegments()) > 0;
    }

    @Override
    protected SpaceSimulation.FlightPlanAction connectedAsteroidBefore(
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

    @Override
    protected SpaceSimulation.FlightPlanAction asteroidArrivalBefore(
            SpaceSimulation.FlightPlanBranch branch, UUID beforeAction) {

        SpaceSimulation.FlightPlanAction arrival = null;
        for (var action : branch.actions()) {
            if (action.id().equals(beforeAction)) break;
            if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) continue;
            var target = findObject(action.targetId());
            arrival = target != null && target.type() == SpaceObjects.ObjectType.ASTEROID
                    && (action.orbit() == SpaceSimulation.OrbitBand.TIGHT
                    || action.orbit() == SpaceSimulation.OrbitBand.SURFACE)
                    && canConnectAfter(action) ? action : null;
        }
        return arrival;
    }

}
