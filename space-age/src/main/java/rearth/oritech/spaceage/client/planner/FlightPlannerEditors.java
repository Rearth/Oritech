package rearth.oritech.spaceage.client.planner;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.block.mission_control.MissionControlMenu;
import rearth.oritech.spaceage.client.mission_control.FleetViewPreferences;
import rearth.oritech.spaceage.client.starmap.RocketStarMapWidget;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.simulation.AsteroidTarget;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.flight.NavigationWaypoint;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.FlightPlanHardware;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.mission.DockingTarget;
import rearth.oritech.spaceage.simulation.mission.RocketDocking;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketPayloads;
import rearth.oritech.spaceage.simulation.service.RocketProcessingService;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings.Docking;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings.HardwareRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Owns modal editor state and input routing; FlightPlannerPopups builds the controls.
 */
abstract class FlightPlannerEditors extends OritechWidgetScreen<RocketAssemblerMenu> {

    // Generated booster cards cannot be selected as a manual action type.
    protected static final SpaceSimulation.ActionType[] EDITABLE_ACTION_TYPES = {
            SpaceSimulation.ActionType.NAVIGATE_TO, SpaceSimulation.ActionType.CONNECT_ASTEROID,
            SpaceSimulation.ActionType.DECOUPLE, SpaceSimulation.ActionType.DEPLOY_CUBESAT, SpaceSimulation.ActionType.MAINTAIN_POSITION,
            SpaceSimulation.ActionType.BEAM_POWER,
            SpaceSimulation.ActionType.DISCARD_CRAFT, SpaceSimulation.ActionType.SCAN,
            SpaceSimulation.ActionType.PROCESS, SpaceSimulation.ActionType.DOCK,
            SpaceSimulation.ActionType.UNDOCK, SpaceSimulation.ActionType.TRANSMIT_INFORMATION
    };
    protected final List<TargetOption> targetOptions = new ArrayList<>();
    // Timeline card currently open in the action configuration popup.
    protected UUID actionEditorAction;
    // Shared numeric popup: edited card, edited value, live field and text retained across rebuilds.
    protected UUID numberAction;
    protected NumberKind numberKind = NumberKind.SPEED;
    protected EditBox numberField;
    protected String numberText;
    // Card currently choosing a new action type.
    protected UUID actionTypeAction;
    // Branch receiving a new card after its action type is chosen.
    protected UUID newActionBranch;
    // Target picker: edited card, search text and the currently visible target buttons.
    protected UUID targetAction;
    protected EditBox targetSearchField;
    protected String targetSearch = "";
    protected ScrollWidget targetList;
    // Modal lists receive wheel input before the map and timeline behind them.
    private final List<ScrollWidget> popupScrolls = new ArrayList<>();
    // Arrival-speed popup: edited card, live field and retained text.
    protected UUID arrivalAction;
    protected EditBox arrivalField;
    protected String arrivalText;
    // Destination popup: edited card and optional Earth-surface X/Z fields.
    protected UUID destinationAction;
    protected SpaceSimulation.OrbitBand destinationOrbit;
    protected EditBox landingXField;
    protected EditBox landingZField;
    protected String landingXText;
    protected String landingZText;
    // Completion-condition popup: edited card, input field and retained value.
    protected UUID addonAction;
    protected SpaceSimulation.ActionAddon addonOriginal;
    protected EditBox addonValueField;
    protected String addonValueText;
    // Decouple editor keeps the ordered coupling sides locally until Done is pressed.
    protected UUID decoupleAction;
    protected SpaceSimulation.SegmentRef decoupleRetained;
    protected SpaceSimulation.SegmentRef decoupleDetached;
    // Target and click position for the map's right-click menu.
    protected RocketStarMapWidget.NavigationContextRequest mapContextRequest;
    // Dropdown origin in panel coordinates after accounting for card scrolling.
    protected int dropdownX;
    protected int dropdownY;

    protected FlightPlannerEditors(RocketAssemblerMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 0, 0);
    }

    protected static int[] landingOffset(SpaceSimulation.FlightPlanAction action) {

        var adjusted = RocketFlightPlanRules.applyLandingUncertainty(action);
        return new int[]{adjusted.landingOffsetX(), adjusted.landingOffsetZ()};
    }

    protected ActiveRocketData hardwareBefore(SpaceSimulation.FlightPlanBranch branch, UUID actionId) {

        return FlightPlanHardware.before(draftPlan(), branch == null ? draftPlan().root() : branch, actionId,
                flightPlanRocket(), calculatedFlight());
    }

    protected ActiveRocketData hardwareFor(SpaceSimulation.FlightPlanAction action) {

        return hardwareBefore(findBranchContaining(action), action.id());
    }

    // The screen retains mission and map state; the editor only requests it through these hooks.
    protected abstract SpaceSimulation.FlightPlan draftPlan();

    protected abstract RocketFlightPathCalculator.FlightPath calculatedFlight();

    protected abstract ActiveRocketData flightPlanRocket();

    protected abstract ScrollWidget flightPlanActionScroll();

    protected abstract UUID activeBranchId();

    protected abstract RocketStarMapWidget.NavigationSelection selectedTarget();

    protected abstract void setSelectedTarget(RocketStarMapWidget.NavigationSelection target);

    protected abstract void showBeamTarget(UUID target);

    protected abstract int panelWidth();

    protected abstract int panelHeight();

    protected abstract SpaceSimulation.FlightPlanAction findAction(UUID actionId);

    protected abstract SpaceSimulation.FlightPlanBranch findBranch(UUID branchId);

    protected abstract SpaceSimulation.FlightPlannerSnapshot currentDraftSnapshot();

    protected abstract SpaceSimulation.SpaceObjectData selectedObject();

    protected abstract SpaceSimulation.FlightPlanAction connectedAsteroidBefore(SpaceSimulation.FlightPlanBranch branch, UUID actionId);

    protected abstract SpaceSimulation.FlightPlanAction asteroidArrivalBefore(SpaceSimulation.FlightPlanBranch branch, UUID actionId);

    protected abstract List<List<SpaceSimulation.SegmentRef>> connectedPairs(ActiveRocketData rocket);

    protected abstract String segmentName(SpaceSimulation.SegmentRef ref, ActiveRocketData rocket);

    protected abstract void replaceAction(UUID branchId, UUID actionId, SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket);

    protected abstract void cycleActionParameter(UUID branchId, int index, ActiveRocketData rocket);

    protected abstract Component actionParameter(SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket);

    protected abstract Component actionOrbit(SpaceSimulation.FlightPlanAction action);

    protected abstract boolean canAddNavigationAddon(SpaceSimulation.FlightPlanBranch branch,
                                                     SpaceSimulation.FlightPlanAction action);

    protected abstract void addAction(UUID branchId, ActiveRocketData rocket, SpaceSimulation.ActionType type);

    protected SpaceSimulation.FlightPlanBranch findBranchContaining(SpaceSimulation.FlightPlanAction action) {

        if (action == null) return null;
        return draftPlan().branches().stream().filter(branch -> branch.actions().contains(action))
                .findFirst().orElse(null);
    }

    protected void openActionEditor(UUID actionId) {

        closeEditorState();
        actionEditorAction = actionId;
        rebuildComponents();
    }

    private void openNumberEditor(SpaceSimulation.FlightPlanAction action, NumberKind kind, String text) {

        closeNestedEditorState();
        numberAction = action.id();
        numberKind = kind;
        numberText = text;
        rebuildComponents();
    }

    protected void openSpeedEditor(SpaceSimulation.FlightPlanAction action) {

        openNumberEditor(action, NumberKind.SPEED, action.maxSpeed() == 0 ? "maximum" : Integer.toString(action.maxSpeed()));
    }

    void editServiceNumber(SpaceSimulation.FlightPlanAction action) {

        var ticks = action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION
                ? action.service().timeoutTicks() : action.service().durationTicks();
        openNumberEditor(action, NumberKind.SERVICE_TIME, ticks == 0 ? "maximum" : Integer.toString(ticks / 20));
    }

    void toggleSunStations() {

        FleetViewPreferences.toggleSun();
        rebuildComponents();
    }

    void editBeamReserve(SpaceSimulation.FlightPlanAction action) {

        openNumberEditor(action, NumberKind.BEAM_RESERVE, Integer.toString(action.settings().beam().reservePercent()));
    }

    void editResourceAmount(SpaceSimulation.FlightPlanAction action, boolean fuel, boolean reserve) {

        var kind = fuel ? reserve ? NumberKind.FUEL_RESERVE : NumberKind.FUEL_LIMIT
                : reserve ? NumberKind.RF_RESERVE : NumberKind.RF_LIMIT;
        var exchange = fuel ? action.settings().exchange().fuel() : action.settings().exchange().rf();
        var amount = reserve ? exchange.reserve() : exchange.limit();
        openNumberEditor(action, kind, amount == Long.MAX_VALUE ? "0" : Long.toString(amount));
    }

    void editAsteroidRadius(SpaceSimulation.FlightPlanAction action, boolean minimum) {

        openNumberEditor(action, minimum ? NumberKind.MIN_RADIUS : NumberKind.MAX_RADIUS,
                Integer.toString(minimum ? action.asteroidTarget().minRadius() : action.asteroidTarget().maxRadius()));
    }

    Integer parseEditorNumber(String text) {

        if (numberKind == NumberKind.SPEED || numberKind == NumberKind.SERVICE_TIME) return FlightPlannerLabels.parseSpeedLimit(text);
        var value = FlightPlannerLabels.parseInteger(text);
        return value != null && value >= 0 && (numberKind != NumberKind.BEAM_RESERVE || value <= 100) ? value : null;
    }

    void changeService(SpaceSimulation.FlightPlanAction action, SpaceSimulation.ServiceSettings settings) {

        var branch = findBranchContaining(action);
        if (branch == null) return;
        replaceAction(branch.id(), action.id(), action.withService(settings), flightPlanRocket());
        rebuildComponents();
    }

    void changeServiceSettings(SpaceSimulation.FlightPlanAction action, RocketServiceSettings settings) {

        var branch = findBranchContaining(action);
        if (branch == null) return;
        replaceAction(branch.id(), action.id(), action.withSettings(settings), flightPlanRocket());
        rebuildComponents();
    }

    protected boolean hasOpenPopup() {

        return actionEditorAction != null || numberAction != null || actionTypeAction != null || newActionBranch != null || targetAction != null
                || arrivalAction != null || destinationAction != null || addonAction != null || decoupleAction != null
                || mapContextRequest != null;
    }

    protected void buildEditors() {

        // Rebuilding the screen drops vanilla widgets. Keep their text, but replace the widget references.
        numberField = null;
        targetSearchField = null;
        targetList = null;
        popupScrolls.clear();
        targetOptions.clear();
        arrivalField = null;
        landingXField = null;
        landingZField = null;
        addonValueField = null;
        new FlightPlannerPopups(this, font, leftPos, topPos).build();
    }

    void addPopupComponent(UIComponent component) {

        if (component instanceof ScrollWidget scroll) popupScrolls.add(scroll);
        addComponent(component);
    }

    EditBox addPopupField(EditBox field) {

        return addRenderableWidget(field);
    }

    void focusPopup(EditBox field) {

        setFocused(field);
    }

    protected void applyNumber() {

        var value = parseEditorNumber(numberField.getValue());
        if (value != null) applyNumber(value);
    }

    protected void applyNumber(int value) {

        var action = findAction(numberAction);
        if (action == null) return;
        var branch = findBranchContaining(action);
        if (branch == null) return;
        numberAction = null;
        var edited = switch (numberKind) {
            case SPEED -> action.withMaxSpeed(value);
            case SERVICE_TIME -> {
                var settings = action.service();
                var transmit = action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION;
                yield action.withService(new SpaceSimulation.ServiceSettings(transmit ? settings.durationTicks() : Math.max(20, value * 20),
                        transmit ? value * 20 : settings.timeoutTicks(), settings.slot()));
            }
            case BEAM_RESERVE -> action.withSettings(action.settings().withBeam(new RocketServiceSettings.Beam(value)));
            case MIN_RADIUS, MAX_RADIUS -> {
                var selection = action.asteroidTarget();
                yield action.withAsteroidTarget(new AsteroidTarget(true, numberKind == NumberKind.MIN_RADIUS ? value : selection.minRadius(),
                        numberKind == NumberKind.MAX_RADIUS ? value : selection.maxRadius()));
            }
            case FUEL_LIMIT, FUEL_RESERVE, RF_LIMIT, RF_RESERVE -> withExchangeAmount(action, value);
        };
        replaceAction(branch.id(), action.id(), edited, flightPlanRocket());
        rebuildComponents();
    }

    private SpaceSimulation.FlightPlanAction withExchangeAmount(SpaceSimulation.FlightPlanAction action, int value) {

        var exchange = action.settings().exchange();
        var fuel = numberKind == NumberKind.FUEL_LIMIT || numberKind == NumberKind.FUEL_RESERVE;
        var reserve = numberKind == NumberKind.FUEL_RESERVE || numberKind == NumberKind.RF_RESERVE;
        var current = fuel ? exchange.fuel() : exchange.rf();
        // A zero limit means unlimited.
        var changed = new RocketServiceSettings.ResourceExchange(current.direction(),
                reserve ? current.limit() : value == 0 ? Long.MAX_VALUE : value, reserve ? value : current.reserve());
        return action.withSettings(action.settings().withExchange(new RocketServiceSettings.Exchange(exchange.cargo(), exchange.local(),
                exchange.remote(), exchange.filter(), fuel ? changed : exchange.fuel(), fuel ? exchange.rf() : changed)));
    }

    boolean hasNestedEditor() {

        return numberAction != null || actionTypeAction != null || newActionBranch != null || targetAction != null
                || arrivalAction != null || destinationAction != null || addonAction != null || decoupleAction != null;
    }

    protected void openAddActionMenu(UUID branchId, int contentX, int contentY) {

        closeEditorState();
        newActionBranch = branchId;
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

        if (newActionBranch != null) {
            var branchId = newActionBranch;
            closeNestedEditorState();
            addAction(branchId, flightPlanRocket(), type);
            rebuildComponents();
            return;
        }
        var action = findAction(actionTypeAction);
        if (action == null) return;
        var branch = findBranchContaining(action);
        if (branch == null) return;
        var changed = configureAction(action.withType(type), type, branch, action.id());
        if (changed == null) return;
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket());
        rebuildComponents();
    }

    protected SpaceSimulation.FlightPlanAction configureAction(SpaceSimulation.FlightPlanAction changed,
                                                               SpaceSimulation.ActionType type,
                                                               SpaceSimulation.FlightPlanBranch branch,
                                                               UUID beforeAction) {

        var hardware = hardwareBefore(branch, beforeAction);
        if (type == SpaceSimulation.ActionType.BEAM_POWER) {
            changed = changed.withTarget(SpaceObjects.EARTH_ID).withOrbit(SpaceSimulation.OrbitBand.SURFACE);
        } else if (type == SpaceSimulation.ActionType.DOCK) {
            var ports = RocketDocking.ports(hardware);
            var host = dockingTargetBefore(branch, beforeAction);
            if (host != null && !host.ports().isEmpty() && !ports.isEmpty())
                changed = changed.withSettings(changed.settings().withDocking(host.rendezvous(ports.getFirst(), host.ports().getFirst())));
        } else if (type == SpaceSimulation.ActionType.NAVIGATE_TO) {
            changed = changed.withTarget(selectedTarget().objectId()).withOrbit(selectedTarget().orbit());
            if (selectedTarget().waypoint() != null) changed = changed.withWaypoint(selectedTarget().waypoint());
            changed = freezeNavigationTarget(changed);
            if (changed.settings().waypoint().isPresent())
                setSelectedTarget(new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit()));
            if (FlightPlannerLabels.isEarthSurface(changed)) {
                var offset = landingOffset(changed);
                changed = changed.withLanding(0, 0, offset[0], offset[1]);
            }
        } else if (type == SpaceSimulation.ActionType.PROCESS) {
            var modules = RocketProcessingService.modules(hardware);
            if (!modules.isEmpty()) changed = changed.withSettings(changed.settings().withProcessing(
                    new RocketServiceSettings.Processing(List.of(modules.getFirst()), RocketServiceSettings.SourceScope.BOTH, SpaceSimulation.FlightPlanAction.NO_TARGET)));
        } else if (type == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
            var anchors = RocketFlightPlanRules.asteroidAnchorSegments(hardware);
            var arrival = asteroidArrivalBefore(branch, beforeAction);
            var asteroid = arrival == null ? selectedObject() : currentDraftSnapshot().objects().stream()
                    .filter(object -> object.id().equals(arrival.targetId())).findFirst().orElse(null);
            if (anchors.isEmpty()) return null;
            if (asteroid == null || asteroid.type() != SpaceObjects.ObjectType.ASTEROID) {
                asteroid = currentDraftSnapshot().objects().stream()
                        .filter(object -> object.type() == SpaceObjects.ObjectType.ASTEROID).findFirst().orElse(null);
            }
            if (arrival != null && arrival.asteroidTarget().closest()) {
                return changed.withTarget(SpaceSimulation.FlightPlanAction.CLOSEST_ASTEROID).withOrbit(arrival.orbit())
                        .withSegments(List.of(anchors.getFirst()));
            }
            if (asteroid == null) return null;
            var orbit = arrival == null ? RocketFlightPlanRules.compatibleOrbit(asteroid.type(), selectedTarget().orbit())
                    : arrival.orbit();
            changed = changed.withTarget(asteroid.id()).withOrbit(orbit).withSegments(List.of(anchors.getFirst()));
        } else if (type == SpaceSimulation.ActionType.DEPLOY_CUBESAT) {
            var payloads = RocketPayloads.all(hardware);
            if (payloads.isEmpty()) return null;
            changed = changed.withSegments(List.of(new SpaceSimulation.SegmentRef(payloads.keySet().iterator().next())));
        } else if (type == SpaceSimulation.ActionType.DECOUPLE) {
            var connected = connectedAsteroidBefore(branch, beforeAction);
            if (connected != null) {
                changed = changed.withTarget(connected.targetId()).withOrbit(connected.orbit())
                        .withSegments(connected.segments());
            } else {
                var pairs = connectedPairs(hardware);
                if (!pairs.isEmpty()) changed = changed.withSegments(pairs.getFirst());
            }
        }
        return changed;
    }

    private SpaceSimulation.FlightPlanAction freezeNavigationTarget(SpaceSimulation.FlightPlanAction action) {

        var point = currentDraftSnapshot().mapObjects().stream().filter(target -> target.id().equals(action.targetId())
                && target.type() == SpaceObjects.ObjectType.WAYPOINT).findFirst().orElse(null);
        var navigation = point == null ? action : action.withWaypoint(new NavigationWaypoint(point.x(), point.y()));
        var station = currentDraftSnapshot().stations().stream().filter(target -> target.id().equals(action.targetId())).findFirst().orElse(null);
        var docking = station == null ? Docking.DEFAULT : station.rendezvous(HardwareRef.NONE, HardwareRef.NONE);
        var changed = navigation.withSettings(navigation.settings().withDocking(docking));
        return station == null ? changed : changed.withOrbit(SpaceSimulation.OrbitBand.SURFACE)
                .withVelocity(SpaceSimulation.ArrivalVelocityMode.ZERO, 0);
    }

    DockingTarget dockingTargetBefore(SpaceSimulation.FlightPlanBranch branch, UUID beforeAction) {

        if (branch == null) return null;
        var targetId = menu instanceof MissionControlMenu control && control.knownPosition != null
                && Math.hypot(control.knownPosition.vx(), control.knownPosition.vy()) <= .01
                ? control.knownPosition.target() : SpaceSimulation.FlightPlanAction.NO_TARGET;
        for (var action : branch.actions()) {
            if (action.id().equals(beforeAction)) break;
            if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
                // A flyby cannot attach ports, and an aborted transfer never reaches the host.
                var stopped = action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.ZERO
                        || action.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM && action.targetVelocity() == 0;
                var arrived = calculatedFlight().paths().stream().filter(path -> path.branchId().equals(branch.id()))
                        .flatMap(path -> path.actionMoments().stream()).anyMatch(moment -> moment.actionId().equals(action.id()) && moment.completed());
                var aborted = calculatedFlight().navigationAborts().stream().anyMatch(moment -> moment.actionId().equals(action.id()));
                targetId = stopped && arrived && !aborted ? action.targetId() : SpaceSimulation.FlightPlanAction.NO_TARGET;
            } else if (action.type() == SpaceSimulation.ActionType.DOCK) targetId = SpaceSimulation.FlightPlanAction.NO_TARGET;
        }
        var id = targetId;
        return currentDraftSnapshot().stations().stream().filter(target -> target.id().equals(id)
                && !target.id().equals(flightPlanRocket().getRocketId())).findFirst().orElse(null);
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

    void selectClosestAsteroid() {

        var action = findAction(targetAction);
        if (action == null) return;
        var branch = findBranchContaining(action);
        if (branch == null) return;
        var changed = action.withTarget(SpaceSimulation.FlightPlanAction.CLOSEST_ASTEROID)
                .withAsteroidTarget(new AsteroidTarget(true, action.asteroidTarget().minRadius(), action.asteroidTarget().maxRadius()))
                .withOrbit(RocketFlightPlanRules.compatibleOrbit(SpaceObjects.ObjectType.ASTEROID, action.orbit()));
        changed = freezeNavigationTarget(changed);
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket());
        rebuildComponents();
    }

    protected void selectNavigationTarget(SpaceSimulation.SpaceObjectData target) {

        if (menu instanceof MissionControlMenu && target.type() == SpaceObjects.ObjectType.CRAFT)
            ClientPacketDistributor.sendToServer(new MissionNetworking.FleetRequest(target.id(), false));

        var action = findAction(targetAction);
        if (action == null) return;
        var branch = findBranchContaining(action);
        if (branch == null) return;
        var changed = action.withAsteroidTarget(AsteroidTarget.DEFAULT).withTarget(target.id()).withOrbit(
                RocketFlightPlanRules.compatibleOrbit(target.type(), action.orbit()));
        if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) changed = freezeNavigationTarget(changed);
        if (FlightPlannerLabels.isEarthSurface(changed)) {
            var offset = landingOffset(changed);
            changed = changed.withLanding(changed.landingX(), changed.landingZ(), offset[0], offset[1]);
        }
        setSelectedTarget(new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit()));
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket());
        rebuildComponents();
    }

    protected void openDestinationMenuFromEditor(UUID actionId, int panelX, int panelY) {

        var action = findAction(actionId);
        if (action == null || action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) return;
        closeNestedEditorState();
        destinationAction = actionId;
        destinationOrbit = action.orbit();
        landingXText = Integer.toString(action.landingX());
        landingZText = Integer.toString(action.landingZ());
        dropdownX = panelX;
        dropdownY = panelY;
        rebuildComponents();
    }

    protected void selectDestinationOrbit(SpaceSimulation.OrbitBand orbit) {

        var action = findAction(destinationAction);
        if (action == null || action.type() != SpaceSimulation.ActionType.NAVIGATE_TO) return;
        destinationOrbit = orbit;
        rebuildComponents();
    }

    protected void useLaunchPadCoordinates() {

        var centre = flightPlanRocket().getLaunchPadCenter();
        landingXField.setValue(Integer.toString(centre.getX()));
        landingZField.setValue(Integer.toString(centre.getZ()));
    }

    protected void applyDestination() {

        var action = findAction(destinationAction);
        if (action == null || destinationOrbit == null) return;
        var branch = findBranchContaining(action);
        if (branch == null) return;
        var changed = action.withOrbit(destinationOrbit);
        if (FlightPlannerLabels.isEarthSurface(changed)) {
            var x = FlightPlannerLabels.parseInteger(landingXField == null ? "" : landingXField.getValue());
            var z = FlightPlannerLabels.parseInteger(landingZField == null ? "" : landingZField.getValue());
            if (x == null || z == null) return;
            changed = changed.withLanding(x, z, 0, 0);
            var offset = landingOffset(changed);
            changed = changed.withLanding(x, z, offset[0], offset[1]);
        }
        setSelectedTarget(new RocketStarMapWidget.NavigationSelection(changed.targetId(), changed.orbit()));
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket());
        rebuildComponents();
    }

    protected void addNavigationAddon(UUID branchId, UUID actionId, ActiveRocketData rocket) {

        var action = findAction(actionId);
        if (action == null || !action.addons().isEmpty()) return;
        var addon = SpaceSimulation.ActionAddon.create(action.type().stationKeeping()
                ? SpaceSimulation.ActionAddonType.LOW_RF
                : SpaceSimulation.ActionAddonType.DISTANCE_FROM_TARGET);
        replaceAction(branchId, actionId, action.withAddons(List.of(addon)), rocket);
        addonAction = actionId;
        addonOriginal = null;
        addonValueText = Integer.toString(addon.value());
        rebuildComponents();
    }

    protected void openAddonEditor(UUID actionId) {

        var action = findAction(actionId);
        if (action == null || action.addons().isEmpty()) return;
        closeNestedEditorState();
        addonAction = actionId;
        addonOriginal = action.addons().getFirst();
        addonValueText = Integer.toString(action.addons().getFirst().value());
        rebuildComponents();
    }

    protected void selectAddonType(SpaceSimulation.ActionAddonType type) {

        var action = findAction(addonAction);
        if (action == null || action.addons().isEmpty()) return;
        var changed = action.addons().getFirst().withType(type);
        var branch = findBranchContaining(action);
        if (branch == null) return;
        addonValueText = Integer.toString(changed.value());
        replaceAction(branch.id(), action.id(), action.withAddons(List.of(changed)), flightPlanRocket());
        rebuildComponents();
    }

    protected void applyAddonValue() {

        var action = findAction(addonAction);
        var value = action == null || action.addons().isEmpty() || addonValueField == null ? null
                : FlightPlannerLabels.parseAddonValue(action.addons().getFirst().type(), addonValueField.getValue());
        if (action == null || action.addons().isEmpty() || value == null) return;
        if (!updateAddonValue(action, value)) return;
        closeNestedEditorState();
        rebuildComponents();
    }

    protected void removeNavigationAddon(UUID branchId, UUID actionId, ActiveRocketData rocket) {

        var action = findAction(actionId);
        if (action == null) return;
        replaceAction(branchId, actionId, action.withAddons(List.of()), rocket);
    }

    protected void removeEditedNavigationAddon() {

        var action = findAction(addonAction);
        var branch = findBranchContaining(action);
        if (branch == null) return;
        closeNestedEditorState();
        removeNavigationAddon(branch.id(), action.id(), flightPlanRocket());
        rebuildComponents();
    }

    protected boolean previewAddonValue(String text) {

        var action = findAction(addonAction);
        var value = action == null || action.addons().isEmpty() ? null
                : FlightPlannerLabels.parseAddonValue(action.addons().getFirst().type(), text);
        return action != null && !action.addons().isEmpty() && value != null && updateAddonValue(action, value);
    }

    private boolean updateAddonValue(SpaceSimulation.FlightPlanAction action, int value) {

        var branch = findBranchContaining(action);
        if (branch == null) return false;
        var addon = action.addons().getFirst();
        if (addon.value() != value) {
            replaceAction(branch.id(), action.id(), action.withAddons(List.of(addon.withValue(value))), flightPlanRocket());
        }
        return true;
    }

    protected void cancelAddonEditor() {

        var action = findAction(addonAction);
        var branch = findBranchContaining(action);
        if (branch != null) {
            var restored = addonOriginal == null ? List.<SpaceSimulation.ActionAddon>of() : List.of(addonOriginal);
            var actionId = action.id();
            closeNestedEditorState();
            replaceAction(branch.id(), actionId, action.withAddons(restored), flightPlanRocket());
        } else closeNestedEditorState();
        rebuildComponents();
    }

    protected void cycleEditedParameter(UUID actionId) {

        var action = findAction(actionId);
        var branch = findBranchContaining(action);
        if (branch == null) return;
        cycleActionParameter(branch.id(), branch.actions().indexOf(action), flightPlanRocket());
        rebuildComponents();
    }

    protected void openDecoupleEditor(UUID actionId) {

        var action = findAction(actionId);
        if (action == null || action.type() != SpaceSimulation.ActionType.DECOUPLE
                || !action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) return;
        var pairs = connectedPairs(hardwareFor(action));
        if (pairs.isEmpty()) return;
        closeNestedEditorState();
        decoupleAction = actionId;
        if (action.segments().size() == 2) {
            decoupleRetained = action.segments().get(0);
            decoupleDetached = action.segments().get(1);
        } else {
            decoupleRetained = pairs.getFirst().get(0);
            decoupleDetached = pairs.getFirst().get(1);
        }
        rebuildComponents();
    }

    protected void selectDecoupleRetained(SpaceSimulation.SegmentRef retained) {

        decoupleRetained = retained;
        var neighbors = decoupleNeighbors(retained);
        if (!neighbors.contains(decoupleDetached)) decoupleDetached = neighbors.isEmpty() ? null : neighbors.getFirst();
        rebuildComponents();
    }

    protected void selectDecoupleDetached(SpaceSimulation.SegmentRef detached) {

        decoupleDetached = detached;
        rebuildComponents();
    }

    protected List<SpaceSimulation.SegmentRef> decoupleCandidates() {

        return connectedPairs(hardwareFor(findAction(decoupleAction))).stream().flatMap(List::stream).distinct()
                .sorted(Comparator.comparing(ref -> segmentName(ref, flightPlanRocket()), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    protected List<SpaceSimulation.SegmentRef> decoupleNeighbors(SpaceSimulation.SegmentRef retained) {

        if (retained == null) return List.of();
        return connectedPairs(hardwareFor(findAction(decoupleAction))).stream().filter(pair -> pair.contains(retained))
                .map(pair -> pair.get(0).equals(retained) ? pair.get(1) : pair.get(0)).distinct()
                .sorted(Comparator.comparing(ref -> segmentName(ref, flightPlanRocket()), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    protected void applyDecoupleSelection() {

        var action = findAction(decoupleAction);
        var branch = findBranchContaining(action);
        if (branch == null || decoupleRetained == null || decoupleDetached == null
                || !decoupleNeighbors(decoupleRetained).contains(decoupleDetached)) return;
        var changed = action.withSegments(List.of(decoupleRetained, decoupleDetached));
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), changed, flightPlanRocket());
        rebuildComponents();
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

        var velocity = arrivalField == null ? null : FlightPlannerLabels.parseArrivalVelocity(arrivalField.getValue());
        if (velocity != null) applyArrival(SpaceSimulation.ArrivalVelocityMode.CUSTOM, velocity);
    }

    protected void applyArrival(SpaceSimulation.ArrivalVelocityMode mode, int velocity) {

        var action = findAction(arrivalAction);
        if (action == null) return;
        var branch = findBranchContaining(action);
        if (branch == null) return;
        closeNestedEditorState();
        replaceAction(branch.id(), action.id(), action.withVelocity(mode, velocity), flightPlanRocket());
        rebuildComponents();
    }

    protected void addNavigationTargetFromMap() {

        if (mapContextRequest == null || activeBranchId() == null) return;
        setSelectedTarget(mapContextRequest.selection());
        closeEditorState();
        addAction(activeBranchId(), flightPlanRocket(), SpaceSimulation.ActionType.NAVIGATE_TO);
        rebuildComponents();
    }

    protected void closeEditorState() {

        actionEditorAction = null;
        closeNestedEditorState();
        mapContextRequest = null;
    }

    private void closeNestedEditorState() {

        numberAction = null;
        actionTypeAction = null;
        newActionBranch = null;
        targetAction = null;
        arrivalAction = null;
        destinationAction = null;
        destinationOrbit = null;
        addonAction = null;
        addonOriginal = null;
        decoupleAction = null;
        decoupleRetained = null;
        decoupleDetached = null;
    }

    protected void closeEditors() {

        if (actionEditorAction != null && hasNestedEditor()) {
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

        if (clickField(numberField, event, doubleClick) || clickField(targetSearchField, event, doubleClick)
                || clickField(arrivalField, event, doubleClick) || clickField(landingXField, event, doubleClick)
                || clickField(landingZField, event, doubleClick) || clickField(addonValueField, event, doubleClick))
            return true;
        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickField(EditBox field, MouseButtonEvent event, boolean doubleClick) {

        if (field == null || !field.isMouseOver(event.x(), event.y())) return false;
        setFocused(field);
        return field.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {

        if (hasOpenPopup()) {
            var relativeX = mouseX - leftPos;
            var relativeY = mouseY - topPos;
            for (var index = popupScrolls.size() - 1; index >= 0; index--) {
                var scroll = popupScrolls.get(index);
                if (scroll.handleMouseScroll(relativeX, relativeY, scrollY)) return true;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        if (numberAction != null) {
            if (event.isEscape()) closeEditors();
            else if (event.isConfirmation()) applyNumber();
            else if (numberField != null) numberField.keyPressed(event);
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
        if (destinationAction != null) {
            if (event.isEscape()) closeEditors();
            else if (event.isConfirmation()) applyDestination();
            else if (getFocused() == landingZField && landingZField != null) landingZField.keyPressed(event);
            else if (landingXField != null) landingXField.keyPressed(event);
            return true;
        }
        if (addonAction != null) {
            if (event.isEscape()) cancelAddonEditor();
            else if (event.isConfirmation()) applyAddonValue();
            else if (addonValueField != null) addonValueField.keyPressed(event);
            return true;
        }
        if (actionTypeAction != null || newActionBranch != null || mapContextRequest != null) {
            if (event.isEscape()) closeEditors();
            return true;
        }
        if (decoupleAction != null) {
            if (event.isEscape()) closeEditors();
            else if (event.isConfirmation()) applyDecoupleSelection();
            return true;
        }
        if (actionEditorAction != null) {
            if (event.isEscape()) closeActionEditor();
            else if (getFocused() == landingXField && landingXField != null) landingXField.keyPressed(event);
            else if (getFocused() == landingZField && landingZField != null) landingZField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    /** Values edited through the shared numeric popup. */
    protected enum NumberKind {

        SPEED,
        SERVICE_TIME,
        BEAM_RESERVE,
        MIN_RADIUS,
        MAX_RADIUS,
        FUEL_LIMIT,
        FUEL_RESERVE,
        RF_LIMIT,
        RF_RESERVE;

        boolean resourceAmount() {

            return this == FUEL_LIMIT || this == FUEL_RESERVE || this == RF_LIMIT || this == RF_RESERVE;
        }
    }

    /**
     * Target button and its normalized search text, rebuilt together with the popup.
     */
    protected record TargetOption(UIComponent button, String searchText) {

    }
}
