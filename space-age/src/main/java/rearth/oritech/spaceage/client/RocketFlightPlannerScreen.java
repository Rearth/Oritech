package rearth.oritech.spaceage.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.RocketNetworking;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.*;

/** Edits and previews the flight plan without mixing planner state into the rocket overview screen. */
public class RocketFlightPlannerScreen extends OritechWidgetScreen<RocketAssemblerMenu> {

    private static final int WINDOW_PADDING = 6;

    private int previewRevision = -1;
    private int flightPlannerRevision = -1;
    private int draftSourceRevision = -1;
    private SpaceSimulation.FlightPlan draftPlan = SpaceSimulation.FlightPlan.empty();
    private boolean flightPlanDirty;
    private RocketStarMapWidget flightPlanMap;
    private ActiveRocketData flightPlanRocket;
    private ScrollWidget flightPlanActionScroll;
    private float flightPlanActionScrollX;
    private float flightPlanActionScrollY;
    // Surface destinations need two numbers, while the existing card has one +/- control. Clicking the parameter
    // switches which coordinate those controls edit without adding another row to every action card.
    private final Set<UUID> editingSurfaceZ = new HashSet<>();
    private UUID activeBranchId;
    private final Inventory screenInventory;
    private final Component screenTitle;
    private int panelWidth;
    private int panelHeight;
    private int editorY;
    private int editorHeight;

    public RocketFlightPlannerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 0, 0);
        this.screenInventory = inventory;
        this.screenTitle = title;
    }

    @Override
    protected void buildComponents() {
        panelWidth = width - WINDOW_PADDING * 2;
        panelHeight = height - WINDOW_PADDING * 2;
        setPanelSize(panelWidth, panelHeight);
        previewRevision = menu.getPreviewRevision();
        flightPlannerRevision = menu.getFlightPlannerRevision();
        addComponent(new SurfaceWidget(0, 0, panelWidth, panelHeight, OritechSurface.PANEL));

        var rocketTab = ButtonWidget.panel(9, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.rocket"), ignored -> switchToRocketScreen());
        rocketTab.withDisabledSurface(OritechSurface.PANEL_PRESSED);
        addComponent(rocketTab);

        var flightPlanTab = ButtonWidget.panel(9 + 92, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.flight_plan"), ignored -> {});
        flightPlanTab.setActive(false);
        flightPlanTab.withDisabledSurface(OritechSurface.PANEL_PRESSED);
        addComponent(flightPlanTab);

        buildFlightPlanTab();
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
            draftPlan = RocketFlightPlanRules.normalize(snapshot.plan());
            draftSourceRevision = menu.getFlightPlannerRevision();
            flightPlanDirty = false;
        }

        flightPlanRocket = rocket;
        var draft = currentDraftSnapshot();
        if (activeBranchId == null || draft.plan().branches().stream().noneMatch(branch -> branch.id().equals(activeBranchId))) {
            activeBranchId = draft.plan().root().id();
        }
        int availableHeight = panelHeight - 51;
        editorHeight = Math.clamp(availableHeight / 3, 96, 240);
        int mapHeight = availableHeight - editorHeight - 6;
        editorY = 39 + mapHeight + 6;
        flightPlanMap = new RocketStarMapWidget(12, 39, panelWidth - 24, mapHeight, draft, rocket, activeBranchId);
        addComponent(flightPlanMap);
        addFlightPlanEditor(draft.plan(), rocket);
    }

    private void addFlightPlanEditor(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket) {
        if (flightPlanActionScroll != null) {
            flightPlanActionScrollX = flightPlanActionScroll.getScrollX();
            flightPlanActionScrollY = flightPlanActionScroll.getScrollY();
        }
        var scroll = new ScrollWidget(12, editorY, panelWidth - 24, editorHeight)
                .withVerticalScroll(true)
                .withHorizontalScroll(true)
                .withScrollSpeed(24)
                .withDragScrolling(true);

        var actionOwners = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        var actionsById = new HashMap<UUID, SpaceSimulation.FlightPlanAction>();
        plan.branches().forEach(branch -> branch.actions().forEach(action -> actionOwners.put(action.id(), branch)));
        plan.branches().forEach(branch -> branch.actions().forEach(action -> actionsById.put(action.id(), action)));
        var rowByBranch = new HashMap<UUID, Integer>();
        var xByBranch = new HashMap<UUID, Integer>();
        int contentWidth = 0;
        int rowHeight = 96;
        int cardWidth = 142;
        for (int row = 0; row < plan.branches().size(); row++) {
            var branch = plan.branches().get(row);
            rowByBranch.put(branch.id(), row);
            int rowY = 4 + row * rowHeight;
            int branchX = 5;

            if (!branch.isRoot()) {
                var parent = actionOwners.get(branch.parentSeparationAction());
                int parentRow = parent == null ? Math.max(0, row - 1) : rowByBranch.getOrDefault(parent.id(), row - 1);
                int parentAction = parent == null ? 0 : Math.max(0, parent.actions().stream()
                        .map(SpaceSimulation.FlightPlanAction::id).toList().indexOf(branch.parentSeparationAction()));
                int parentX = parent == null ? 5 : xByBranch.getOrDefault(parent.id(), 5);
                int parentActionX = parentX + 78 + parentAction * cardWidth;

                // Start below the separation card instead of returning to the left edge.
                branchX = parentActionX + cardWidth / 2;
                int guideX = branchX;
                int guideY = 4 + parentRow * rowHeight + 88;
                scroll.addChild(new BranchConnectorWidget(guideX, guideY, branchX - 2, rowY + 17));
            }
            xByBranch.put(branch.id(), branchX);

            Component branchName = branch.isRoot()
                    ? Component.translatable("screen.oritech_space_age.branch.root")
                    : Component.translatable("screen.oritech_space_age.branch.number", row + 1);
            var branchButton = ButtonWidget.panel(branchX, rowY + 5, 70, 24, branchName,
                    ignored -> selectBranch(branch.id()));
            branchButton.withDisabledSurface(OritechSurface.PANEL_PRESSED);
            branchButton.setActive(!branch.id().equals(activeBranchId));
            var sourceAction = actionsById.get(branch.parentSeparationAction());
            var detachedSegment = sourceAction != null && sourceAction.segments().size() == 2
                    ? sourceAction.segments().get(1) : null;
            branchButton.withTooltip(branch.isRoot()
                    ? Component.translatable("screen.oritech_space_age.branch.all_segments")
                    : Component.translatable("screen.oritech_space_age.branch.controls",
                    detachedSegment == null ? "?" : segmentName(detachedSegment, rocket)));
            scroll.addChild(branchButton);

            int cursorX = branchX + 78;
            for (int index = 0; index < branch.actions().size(); index++) {
                var action = branch.actions().get(index);
                int actionIndex = index;
                scroll.addChild(new SurfaceWidget(cursorX, rowY, cardWidth - 8, 88, OritechSurface.PANEL_INSET));
                scroll.addChild(new LabelWidget(cursorX + 5, rowY + 5, 18,
                        Component.literal(Integer.toString(index + 1)).withStyle(ChatFormatting.BOLD)));
                scroll.addChild(ButtonWidget.darkPanel(cursorX + 22, rowY + 4, 103, 17,
                        actionName(action.type()), ignored -> cycleActionType(branch.id(), actionIndex, rocket)));

                var parameterButton = ButtonWidget.panel(cursorX + 5, rowY + 27, 120, 17,
                        actionParameter(action, rocket),
                        ignored -> cycleActionParameter(branch.id(), actionIndex, rocket));
                parameterButton.setActive(hasActionParameter(action.type()));
                scroll.addChild(parameterButton);

                var scopeButton = ButtonWidget.panel(cursorX + 5, rowY + 48, 120, 17,
                        actionScope(action, rocket),
                        ignored -> cycleActionScope(branch.id(), actionIndex, rocket));
                scopeButton.setActive(hasSegmentScope(action));
                scroll.addChild(scopeButton);

                scroll.addChild(ButtonWidget.darkPanel(cursorX + 5, rowY + 69, 18, 13, Component.literal("<"),
                        ignored -> moveAction(branch.id(), actionIndex, -1, rocket)));
                scroll.addChild(ButtonWidget.darkPanel(cursorX + 25, rowY + 69, 18, 13, Component.literal(">"),
                        ignored -> moveAction(branch.id(), actionIndex, 1, rocket)));
                var decrease = ButtonWidget.darkPanel(cursorX + 49, rowY + 69, 18, 13, Component.literal("−"),
                        ignored -> adjustActionValue(branch.id(), actionIndex, -1, rocket));
                decrease.setActive(hasNumericValue(action.type()));
                scroll.addChild(decrease);
                var increase = ButtonWidget.darkPanel(cursorX + 69, rowY + 69, 18, 13, Component.literal("+"),
                        ignored -> adjustActionValue(branch.id(), actionIndex, 1, rocket));
                increase.setActive(hasNumericValue(action.type()));
                scroll.addChild(increase);
                scroll.addChild(ButtonWidget.darkPanel(cursorX + 108, rowY + 69, 17, 13, Component.literal("×"),
                        ignored -> removeAction(branch.id(), actionIndex, rocket)));
                cursorX += cardWidth;
            }

            scroll.addChild(ButtonWidget.orangePanel(cursorX + 2, rowY + 27, 58, 32,
                    Component.translatable("screen.oritech_space_age.action.add"),
                    ignored -> addAction(branch.id(), rocket)));
            contentWidth = Math.max(contentWidth, cursorX + 68);
        }

        scroll.setContentDimensions(contentWidth, Math.max(96, plan.branches().size() * rowHeight));
        scroll.setScrollPosition(flightPlanActionScrollX, flightPlanActionScrollY);
        flightPlanActionScroll = scroll;
        addComponent(scroll);
    }

    private void addAction(UUID branchId, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        int actionCount = draftPlan.branches().stream().mapToInt(item -> item.actions().size()).sum();
        if (branch == null || actionCount >= RocketFlightPlanRules.MAX_ACTIONS) return;
        var updated = new ArrayList<>(branch.actions());
        updated.add(SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.START_ENGINE_BURN));
        updateBranchActions(branchId, updated, rocket);
    }

    private void removeAction(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null || index < 0 || index >= branch.actions().size()) return;
        var updated = new ArrayList<>(branch.actions());
        updated.remove(index);
        updateBranchActions(branchId, updated, rocket);
    }

    private void moveAction(UUID branchId, int index, int direction, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        int target = index + direction;
        if (target < 0 || target >= branch.actions().size()) return;
        var updated = new ArrayList<>(branch.actions());
        Collections.swap(updated, index, target);
        updateBranchActions(branchId, updated, rocket);
    }

    private void cycleActionType(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        var snapshot = currentDraftSnapshot();
        if (branch == null) return;
        var updated = new ArrayList<>(branch.actions());
        var current = updated.get(index);
        var values = SpaceSimulation.ActionType.values();
        var type = values[(current.type().ordinal() + 1) % values.length];
        var changed = current.withType(type);
        if (type == SpaceSimulation.ActionType.SET_NAVIGATION_TARGET && !snapshot.objects().isEmpty()) {
            changed = changed.withTarget(snapshot.objects().getFirst().id());
        } else if (type == SpaceSimulation.ActionType.SET_SURFACE_DESTINATION) {
            changed = changed.withValue(snapshot.launchX()).withSecondaryValue(snapshot.launchZ());
        } else if (type == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE) {
            changed = changed.withTarget(SpaceSimulation.FlightPlanAction.CURRENT_TARGET);
        } else if (type == SpaceSimulation.ActionType.DISABLE_COUPLINGS) {
            var pairs = connectedPairs(rocket);
            if (!pairs.isEmpty()) changed = changed.withSegments(pairs.getFirst());
        }
        editingSurfaceZ.remove(current.id());
        updated.set(index, changed);
        updateBranchActions(branchId, updated, rocket);
    }

    private void cycleActionParameter(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        var snapshot = currentDraftSnapshot();
        if (branch == null) return;
        var updated = new ArrayList<>(branch.actions());
        var action = updated.get(index);
        if (action.type() == SpaceSimulation.ActionType.SET_SURFACE_DESTINATION) {
            if (!editingSurfaceZ.add(action.id())) editingSurfaceZ.remove(action.id());
            refreshFlightPlanner();
            return;
        }
        if (action.type() == SpaceSimulation.ActionType.WAIT_FOR_EVENT) {
            var events = SpaceSimulation.WaitEvent.values();
            updated.set(index, action.withValue((action.value() + 1) % events.length));
            updateBranchActions(branchId, updated, rocket);
            return;
        }
        if (action.type() == SpaceSimulation.ActionType.DISABLE_COUPLINGS) {
            var pairs = connectedPairs(rocket);
            if (pairs.isEmpty()) return;
            int current = pairs.indexOf(action.segments());
            updated.set(index, action.withSegments(pairs.get((current + 1) % pairs.size())));
            updateBranchActions(branchId, updated, rocket);
            return;
        }
        List<UUID> targets = action.type() == SpaceSimulation.ActionType.SET_NAVIGATION_TARGET
                ? snapshot.objects().stream().map(SpaceSimulation.SpaceObjectData::id).toList()
                : action.type() == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE
                ? List.of(SpaceSimulation.FlightPlanAction.CURRENT_TARGET, SpaceObjects.EARTH_ID)
                : List.of();
        if (targets.isEmpty()) return;
        int current = targets.indexOf(action.targetId());
        updated.set(index, action.withTarget(targets.get((current + 1) % targets.size())));
        updateBranchActions(branchId, updated, rocket);
    }

    private void cycleActionScope(UUID branchId, int index, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var updated = new ArrayList<>(branch.actions());
        var action = updated.get(index);
        var refs = segmentRefs(rocket);
        if (refs.isEmpty()) return;
        int current = action.segments().isEmpty() ? -1 : refs.indexOf(action.segments().getFirst());
        var next = current + 1 >= refs.size() ? List.<SpaceSimulation.SegmentRef>of()
                : List.of(refs.get(current + 1));
        updated.set(index, action.withSegments(next));
        updateBranchActions(branchId, updated, rocket);
    }

    private void adjustActionValue(UUID branchId, int index, int direction, ActiveRocketData rocket) {
        var branch = findBranch(branchId);
        if (branch == null) return;
        var updated = new ArrayList<>(branch.actions());
        var action = updated.get(index);
        if (action.type() == SpaceSimulation.ActionType.SET_SURFACE_DESTINATION) {
            boolean editZ = editingSurfaceZ.contains(action.id());
            long coordinate = editZ ? action.secondaryValue() : action.value();
            long step = coordinateStep(coordinate);
            long changed = Math.clamp(coordinate + step * direction, -30_000_000, 30_000_000);
            updated.set(index, editZ ? action.withSecondaryValue(changed) : action.withValue(changed));
            updateBranchActions(branchId, updated, rocket);
            return;
        }
        long step = switch (action.type()) {
            case WAIT_TICKS -> 20;
            case WAIT_SECONDS -> action.value() < 60 ? 1 : action.value() < 600 ? 10 : 60;
            case WAIT_UNTIL_DISTANCE -> action.value() < 10_000 ? 1_000
                    : action.value() < 100_000 ? 10_000
                    : action.value() < 1_000_000 ? 100_000 : 1_000_000;
            case SET_ARRIVAL_VELOCITY -> action.value() < 1_000 ? 10 : 100;
            default -> 0;
        };
        if (step == 0) return;
        long minimum = action.type() == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE
                || action.type() == SpaceSimulation.ActionType.SET_ARRIVAL_VELOCITY ? 0 : 1;
        updated.set(index, action.withValue(Math.max(minimum, action.value() + step * direction)));
        updateBranchActions(branchId, updated, rocket);
    }

    private static long coordinateStep(long coordinate) {
        long absolute = Math.abs(coordinate);
        if (absolute < 1_000) return 100;
        if (absolute < 10_000) return 1_000;
        if (absolute < 100_000) return 10_000;
        return 100_000;
    }

    private static boolean hasNumericValue(SpaceSimulation.ActionType type) {
        return type == SpaceSimulation.ActionType.WAIT_TICKS
                || type == SpaceSimulation.ActionType.WAIT_SECONDS
                || type == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE
                || type == SpaceSimulation.ActionType.SET_SURFACE_DESTINATION
                || type == SpaceSimulation.ActionType.SET_ARRIVAL_VELOCITY;
    }

    private static boolean hasActionParameter(SpaceSimulation.ActionType type) {
        return type == SpaceSimulation.ActionType.SET_NAVIGATION_TARGET
                || type == SpaceSimulation.ActionType.DISABLE_COUPLINGS
                || type == SpaceSimulation.ActionType.WAIT_TICKS
                || type == SpaceSimulation.ActionType.WAIT_SECONDS
                || type == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE
                || type == SpaceSimulation.ActionType.WAIT_FOR_EVENT
                || type == SpaceSimulation.ActionType.SET_SURFACE_DESTINATION
                || type == SpaceSimulation.ActionType.SET_ARRIVAL_VELOCITY;
    }

    private static boolean hasSegmentScope(SpaceSimulation.FlightPlanAction action) {
        return action.type() == SpaceSimulation.ActionType.START_ENGINE_BURN
                || action.type() == SpaceSimulation.ActionType.START_BRAKING_BURN
                || action.type() == SpaceSimulation.ActionType.STOP_ENGINE_BURN
                || action.type() == SpaceSimulation.ActionType.OPEN_PARACHUTES
                || action.type() == SpaceSimulation.ActionType.WAIT_FOR_EVENT
                || action.type() == SpaceSimulation.ActionType.WAIT_UNTIL_BRAKING_POINT
                || action.type() == SpaceSimulation.ActionType.MAINTAIN_ORBIT;
    }

    private SpaceSimulation.FlightPlanBranch findBranch(UUID branchId) {
        return draftPlan.branches().stream().filter(branch -> branch.id().equals(branchId)).findFirst().orElse(null);
    }

    private void selectBranch(UUID branchId) {
        activeBranchId = branchId;
        refreshFlightPlanner();
    }

    private void updateBranchActions(UUID branchId, List<SpaceSimulation.FlightPlanAction> actions,
                                     ActiveRocketData rocket) {
        var branches = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        for (var branch : draftPlan.branches()) {
            branches.add(branch.id().equals(branchId) ? branch.withActions(actions) : branch);
        }
        var candidate = RocketFlightPlanRules.normalize(new SpaceSimulation.FlightPlan(branches));
        int actionCount = candidate.branches().stream().mapToInt(branch -> branch.actions().size()).sum();
        if (candidate.branches().size() > RocketFlightPlanRules.MAX_BRANCHES
                || actionCount > RocketFlightPlanRules.MAX_ACTIONS) return;
        draftPlan = candidate;
        flightPlanDirty = true;
        flightPlanRocket = rocket;
        refreshFlightPlanner();
    }

    private void refreshFlightPlanner() {
        if (flightPlanRocket == null) return;
        var draft = currentDraftSnapshot();
        // Keep the star-map widget so its vertical scroll position survives edits. Action cards are cheap to rebuild
        // and doing so keeps their callbacks and indices in sync after moving or removing actions.
        if (flightPlanMap != null) flightPlanMap.updateFlightPath(draft, flightPlanRocket, activeBranchId);
        if (flightPlanActionScroll != null) {
            flightPlanActionScrollX = flightPlanActionScroll.getScrollX();
            flightPlanActionScrollY = flightPlanActionScroll.getScrollY();
            removeComponent(flightPlanActionScroll);
        }
        addFlightPlanEditor(draft.plan(), flightPlanRocket);
    }

    private SpaceSimulation.FlightPlannerSnapshot currentDraftSnapshot() {
        var source = menu.getFlightPlannerSnapshot();
        if (source == null) throw new IllegalStateException("Flight planner snapshot is not loaded");
        return new SpaceSimulation.FlightPlannerSnapshot(
                source.simulationId(), source.rocketId(), source.launchX(), source.launchZ(), source.objects(), draftPlan);
    }

    private void submitFlightPlanIfDirty() {
        var rocket = menu.getRocket();
        if (!flightPlanDirty || rocket == null) return;
        ClientPacketDistributor.sendToServer(new RocketNetworking.SubmitFlightPlanPayload(
                menu.blockPos, rocket.getRocketId(), draftPlan));
        flightPlanDirty = false;
    }

    private static List<SpaceSimulation.SegmentRef> segmentRefs(ActiveRocketData rocket) {
        return rocket.getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of)
                .sorted(Comparator.comparingInt((SpaceSimulation.SegmentRef ref) -> ref.anchor().getY())
                        .thenComparingInt(ref -> ref.anchor().getX())
                        .thenComparingInt(ref -> ref.anchor().getZ()))
                .toList();
    }

    private static List<List<SpaceSimulation.SegmentRef>> connectedPairs(ActiveRocketData rocket) {
        var refsById = new HashMap<UUID, SpaceSimulation.SegmentRef>();
        rocket.getStaticSegments().forEach((id, segment) -> refsById.put(id, SpaceSimulation.SegmentRef.of(segment)));
        var pairs = new ArrayList<List<SpaceSimulation.SegmentRef>>();
        rocket.getStaticSegments().forEach((id, segment) -> {
            for (var connected : segment.getConnectedSegments()) {
                var first = refsById.get(id);
                var second = refsById.get(connected);
                if (first != null && second != null) pairs.add(List.of(first, second));
            }
        });
        // Keep both A > B and B > A. Their order selects which side remains on the current branch after separation.
        pairs.sort(Comparator.comparingLong((List<SpaceSimulation.SegmentRef> pair) -> pair.get(0).anchor().asLong())
                .thenComparingLong(pair -> pair.get(1).anchor().asLong()));
        return pairs;
    }

    private Component actionName(SpaceSimulation.ActionType type) {
        return Component.translatable("screen.oritech_space_age.action." + type.name().toLowerCase(Locale.ROOT));
    }

    private Component actionParameter(SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket) {
        var snapshot = currentDraftSnapshot();
        if (action.type() == SpaceSimulation.ActionType.SET_NAVIGATION_TARGET) {
            return snapshot.objects().stream().filter(object -> object.id().equals(action.targetId()))
                    .findFirst().map(object -> objectName(object.type()))
                    .orElse(Component.translatable("screen.oritech_space_age.action.no_target"));
        }
        if (action.type() == SpaceSimulation.ActionType.SET_SURFACE_DESTINATION) {
            String selectedX = editingSurfaceZ.contains(action.id()) ? "X" : "[X]";
            String selectedZ = editingSurfaceZ.contains(action.id()) ? "[Z]" : "Z";
            return Component.literal(selectedX + ": " + compact(action.value()) + "  "
                    + selectedZ + ": " + compact(action.secondaryValue()));
        }
        if (action.type() == SpaceSimulation.ActionType.SET_ARRIVAL_VELOCITY) {
            return Component.translatable("screen.oritech_space_age.action.velocity", compact(action.value()));
        }
        if (action.type() == SpaceSimulation.ActionType.DISABLE_COUPLINGS) {
            if (action.segments().size() != 2) return Component.translatable("screen.oritech_space_age.action.no_segment");
            return Component.literal(segmentName(action.segments().get(0), rocket) + " > "
                    + segmentName(action.segments().get(1), rocket));
        }
        if (action.type() == SpaceSimulation.ActionType.WAIT_TICKS) {
            return Component.translatable("screen.oritech_space_age.action.ticks", action.value());
        }
        if (action.type() == SpaceSimulation.ActionType.WAIT_SECONDS) {
            return Component.translatable("screen.oritech_space_age.action.seconds", action.value());
        }
        if (action.type() == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE) {
            var target = action.targetId().equals(SpaceSimulation.FlightPlanAction.CURRENT_TARGET)
                    ? Component.translatable("screen.oritech_space_age.action.current_target").getString()
                    : Component.translatable("screen.oritech_space_age.action.start_position").getString();
            return Component.translatable("screen.oritech_space_age.action.distance", target, compact(action.value()));
        }
        if (action.type() == SpaceSimulation.ActionType.WAIT_FOR_EVENT) {
            var events = SpaceSimulation.WaitEvent.values();
            int index = (int) Math.clamp(action.value(), 0, events.length - 1);
            return Component.translatable("screen.oritech_space_age.event." + events[index].name().toLowerCase(Locale.ROOT));
        }
        if (action.type() == SpaceSimulation.ActionType.WAIT_UNTIL_BRAKING_POINT) {
            return Component.translatable("screen.oritech_space_age.action.current_arrival_velocity");
        }
        if (action.type() == SpaceSimulation.ActionType.MAINTAIN_ORBIT) {
            return Component.translatable("screen.oritech_space_age.action.current_position");
        }
        return Component.translatable("screen.oritech_space_age.action.no_parameter");
    }

    private Component actionScope(SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket) {
        if (!hasSegmentScope(action)) return Component.translatable("screen.oritech_space_age.action.no_scope");
        if (action.segments().isEmpty()) return Component.translatable("screen.oritech_space_age.action.all_segments");
        return Component.literal(segmentName(action.segments().getFirst(), rocket));
    }

    private static String segmentName(SpaceSimulation.SegmentRef ref, ActiveRocketData rocket) {
        int index = segmentRefs(rocket).indexOf(ref);
        return index < 0 ? "?" : "S" + (index + 1);
    }

    private static String compact(long value) {
        long absolute = Math.abs(value);
        if (absolute >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000d);
        if (absolute >= 1_000) return String.format(Locale.ROOT, "%.1fk", value / 1_000d);
        return Long.toString(value);
    }

    private static Component objectName(SpaceObjects.ObjectType type) {
        return Component.translatable("screen.oritech_space_age.object." + type.name().toLowerCase(Locale.ROOT));
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
        components.forEach(component -> component.tick());
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

    /** Draws the tree line between a separation action and its child branch row. */
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
            graphics.fill(startX, startY, startX + 1, cornerY, 0xFF71879A);
            graphics.fill(Math.min(startX, endX), cornerY - 1, Math.max(startX, endX), cornerY, 0xFF71879A);
            graphics.fill(endX, cornerY, endX + 1, endY, 0xFF71879A);
        }
    }

}
