package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
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
import java.util.UUID;

/** Edits mission intent while the analytical calculator supplies paths and automatic booster events. */
public class RocketFlightPlannerScreen extends FlightPlannerEditors {

    // Keep a small border around the resizable screen panel.
    private static final int WINDOW_PADDING = 6;
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
    // Inventory passed back when switching to the assembler screen.
    private final Inventory screenInventory;
    // Title retained when switching to the assembler screen.
    private final Component screenTitle;
    // Current screen panel width in GUI pixels.
    private int panelWidth;
    // Current screen panel height in GUI pixels.
    private int panelHeight;
    // Top of the branch-card area below the map.
    private int editorY;
    // Height available to scroll through branch cards.
    private int editorHeight;

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

    public RocketFlightPlannerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        screenInventory = inventory;
        screenTitle = title;
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
        flightPlanActionScroll = new FlightPlannerCards(this).build(rocket, editorY, panelWidth, editorHeight,
                flightPlanActionScrollX, flightPlanActionScrollY);
        addComponent(flightPlanActionScroll);
    }

    @Override
    protected void addAction(UUID branchId, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null || editableActionCount() >= RocketFlightPlanRules.MAX_ACTIONS) return;
        var action = SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.NAVIGATE_TO)
                .withTarget(selectedTarget.objectId()).withOrbit(selectedTarget.orbit());
        if (FlightPlannerLabels.isEarthSurface(action)) {
            var offset = landingOffset(action);
            action = action.withLanding(0, 0, offset[0], offset[1]);
        }
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
        int current = editable.indexOf(action);
        int target = current + direction;
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

    @Override
    protected void cycleActionOrbit(UUID branchId, int index, ActiveRocketData rocket) {
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
        if (FlightPlannerLabels.isEarthSurface(changed)) {
            var offset = landingOffset(changed);
            changed = changed.withLanding(changed.landingX(), changed.landingZ(), offset[0], offset[1]);
        }
        selectedTarget = new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit());
        replaceAction(branchId, action.id(), changed, rocket);
    }

    void adjustActionVelocity(UUID branchId, int index, int direction, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var action = branch.actions().get(index);
        if (action.type() != SpaceSimulation.ActionType.NAVIGATE_TO
                || action.velocityMode() != SpaceSimulation.ArrivalVelocityMode.CUSTOM) return;
        int step = action.targetVelocity() < 1_000 ? 10 : 100;
        int velocity = Math.clamp(action.targetVelocity() + step * direction, 0, 100_000);
        replaceAction(branchId, action.id(), action.withVelocity(action.velocityMode(), velocity), rocket);
    }

    @Override
    protected void replaceAction(UUID branchId, UUID actionId, SpaceSimulation.FlightPlanAction changed,
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

    private static ArrayList<SpaceSimulation.FlightPlanAction> withoutGenerated(
            List<SpaceSimulation.FlightPlanAction> actions) {
        var result = new ArrayList<SpaceSimulation.FlightPlanAction>();
        actions.stream().filter(action -> !action.isGenerated()).forEach(result::add);
        return result;
    }

    @Override
    protected SpaceSimulation.FlightPlannerSnapshot currentDraftSnapshot() {
        var source = menu.getFlightPlannerSnapshot();
        if (source == null) throw new IllegalStateException("Flight planner snapshot is not loaded");
        return new SpaceSimulation.FlightPlannerSnapshot(
                source.simulationId(), source.rocketId(), source.objects(), draftPlan);
    }

    private void submitFlightPlanIfDirty() {
        RocketAssemblerClientController.submitFlightPlanIfDirty(menu);
    }

    @Override
    protected Component actionParameter(SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket) {
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
        if (FlightPlannerLabels.isEarthSurface(action)) {
            return Component.translatable("screen.oritech_space_age.action.earth_coordinates",
                    action.landingX(), action.landingZ());
        }
        return RocketStarMapWidget.orbitName(action.orbit());
    }

    private static List<SpaceSimulation.SegmentRef> segmentRefs(ActiveRocketData rocket) {
        return rocket.getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of)
                .sorted(Comparator.comparingInt((SpaceSimulation.SegmentRef ref) -> ref.anchor().getY())
                        .thenComparingInt(ref -> ref.anchor().getX())
                        .thenComparingInt(ref -> ref.anchor().getZ())).toList();
    }

    String segmentName(SpaceSimulation.SegmentRef ref, ActiveRocketData rocket) {
        String customName = draftPlan.configurationFor(ref).name();
        if (!customName.isBlank()) return customName;
        int index = segmentRefs(rocket).indexOf(ref);
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

    @Override
    protected SpaceSimulation.SpaceObjectData selectedObject() {
        return currentDraftSnapshot().objects().stream()
                .filter(object -> object.id().equals(selectedTarget.objectId())).findFirst().orElse(null);
    }

    private String objectName(UUID id) {
        return currentDraftSnapshot().objects().stream().filter(object -> object.id().equals(id)).findFirst()
                .map(object -> RocketStarMapWidget.objectName(object).getString()).orElse("?");
    }

    RocketFlightPathCalculator.ArrivalPrediction arrivalPrediction(UUID actionId) {
        return calculatedFlight.arrivalPredictions().stream()
                .filter(prediction -> prediction.actionId().equals(actionId)).findFirst().orElse(null);
    }

    @Override
    protected boolean canAddNavigationAddon(SpaceSimulation.FlightPlanBranch branch,
                                             SpaceSimulation.FlightPlanAction action) {
        var path = calculatedFlight.paths().stream().filter(item -> item.branchId().equals(branch.id()))
                .findFirst().orElse(null);
        if (path == null) return false;
        var moment = path.actionMoments().stream().filter(item -> item.actionId().equals(action.id()))
                .findFirst().orElse(null);
        return moment != null && (moment.connectedSegments().size() > 1
                || !moment.attachedAsteroidId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET));
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

}
