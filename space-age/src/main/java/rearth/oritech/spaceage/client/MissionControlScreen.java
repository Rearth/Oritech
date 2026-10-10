package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.MissionControlMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.MissionForecast;
import rearth.oritech.spaceage.simulation.MissionState;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.SpaceCommunications;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fleet view shares the map and opens the existing mission-card editor for the selected craft.
 */
public class MissionControlScreen extends OritechWidgetScreen<MissionControlMenu> {

    private final Map<UUID, FleetRow> fleetRows = new HashMap<>();
    private final Map<UUID, RocketFlightPathCalculator.CraftPath> fleetPaths = new HashMap<>();
    private int revision = -1;
    private int telemetryRevision = -1;
    private int telemetryTicks;
    private boolean requestedFleet;
    private RocketStarMapWidget map;
    private ScrollWidget fleetList;
    private float fleetScrollY;
    private ButtonWidget speedButton;

    public MissionControlScreen(MissionControlMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 0, 0);
    }

    public static void receive(MissionNetworking.FleetPayload payload) {

        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof MissionControlMenu menu) {
            var fleetStructureChanged = !sameFleetStructure(menu.fleet, payload.fleet())
                    || !menu.network.equals(payload.network());
            menu.fleet = payload.fleet();
            menu.telemetryRevision++;
            if (fleetStructureChanged) menu.fleetRevision++;
            menu.network = payload.network();
            menu.networkNodes = payload.nodes();
            menu.simulationTick = payload.simulationTick();
            menu.debugSpeed = payload.debugSpeed();
            menu.receivedWorldTick = Minecraft.getInstance().level.getGameTime();
            menu.fleetSnapshot = new SpaceSimulation.FlightPlannerSnapshot(payload.system(), new UUID(0, 0), payload.objects(),
                    SpaceSimulation.FlightPlan.empty(), payload.stations());
            var selected = menu.fleet.stream().filter(entry -> entry.id().equals(menu.selected)).findFirst().orElse(null);
            if (selected != null) {
                var telemetry = selected.telemetry();
                var currentAction = telemetry.plan().root().actions().stream().filter(action -> !action.isGenerated())
                        .map(SpaceSimulation.FlightPlanAction::id).findFirst().orElse(null);
                var missionStructureChanged = menu.selectedTelemetry == null
                        || !Objects.equals(menu.currentAction, currentAction)
                        || !menu.completed.equals(telemetry.completed())
                        || menu.connected != selected.connected()
                        || menu.acceptedPlan != null && !menu.acceptedPlan.equals(telemetry.plan())
                        || !menu.selectedTelemetry.rocket().getStaticSegments().keySet()
                        .equals(telemetry.rocket().getStaticSegments().keySet());
                menu.selectedTelemetry = telemetry;
                menu.completed = telemetry.completed();
                menu.currentAction = currentAction;
                menu.missionStatus = telemetry.status();
                menu.acceptedPlan = telemetry.plan();
                menu.knownPosition = telemetry.position();
                menu.connected = selected.connected();
                if (Minecraft.getInstance().screen instanceof RocketFlightPlannerScreen planner) {
                    planner.receiveLiveMission(new SpaceSimulation.FlightPlannerSnapshot(payload.system(),
                                    telemetry.rocket().getRocketId(), payload.objects(), telemetry.plan(), payload.stations()),
                            telemetry.rocket(), missionStructureChanged);
                }
            }
            if (selected == null && menu.selected != null) menu.selected = null;
            menu.connected = selected != null && selected.connected();
        }
    }

    private static boolean sameFleetStructure(List<MissionNetworking.FleetEntry> current,
                                              List<MissionNetworking.FleetEntry> updated) {

        if (current.size() != updated.size()) return false;
        var dismissible = new HashMap<UUID, Boolean>();
        current.forEach(entry -> dismissible.put(entry.id(), entry.dismissible()));
        return updated.stream().allMatch(entry -> Objects.equals(dismissible.get(entry.id()), entry.dismissible()));
    }

    public static void select(MissionNetworking.SelectedPayload payload) {

        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof MissionControlMenu menu) {
            menu.selected = payload.rocket().getRocketId();
            menu.knownPosition = payload.position();
            menu.completed = payload.completed();
            menu.missionStatus = payload.status();
            menu.connected = payload.connected();
            menu.selectedTelemetry = menu.fleet.stream().filter(entry -> entry.id().equals(menu.selected))
                    .map(MissionNetworking.FleetEntry::telemetry).findFirst().orElse(null);
            menu.acceptedPlan = payload.snapshot().plan();
            menu.currentAction = menu.acceptedPlan.root().actions().stream().filter(action -> !action.isGenerated())
                    .map(SpaceSimulation.FlightPlanAction::id).findFirst().orElse(null);
            menu.setPreview(payload.rocket());
            menu.markDraftFlightPlanSaved();
            menu.setFlightPlannerSnapshot(payload.snapshot());
            Minecraft.getInstance().setScreen(new RocketFlightPlannerScreen(menu, player.getInventory(),
                    Component.translatable("block.oritech_space_age.mission_control")));
        }
    }

    static Component location(MissionState.Telemetry telemetry, List<SpaceSimulation.SpaceObjectData> objects) {

        var action = telemetry.plan().root().actions().stream().filter(a -> !a.isGenerated()).findFirst().orElse(null);
        var navigating = action != null && action.type() == SpaceSimulation.ActionType.NAVIGATE_TO;
        var target = navigating ? action.targetId() : telemetry.position().target();
        var name = objects.stream().filter(o -> o.id().equals(target)).findFirst()
                .map(RocketStarMapWidget::objectName).orElse(Component.translatable("screen.oritech_space_age.object.space"));
        var orbit = navigating ? action.orbit() : telemetry.position().orbit();
        return Component.translatable(navigating ? "screen.oritech_space_age.mission.en_route"
                : "screen.oritech_space_age.mission.at", name, RocketStarMapWidget.orbitName(orbit));
    }

    static String distance(double metres) {

        return String.format(Locale.ROOT, "%,.0f km", metres / 1000);
    }

    private static Component availability(boolean available) {

        return Component.translatable(available ? "screen.oritech_space_age.available" : "screen.oritech_space_age.unavailable");
    }

    static int progressPercent(MissionState.Telemetry telemetry, RocketFlightPathCalculator.CraftPath path) {

        return progressPercent(telemetry, path, 0);
    }

    static int progressPercent(MissionState.Telemetry telemetry,
                               RocketFlightPathCalculator.CraftPath path, long extraTicks) {

        var action = telemetry.plan().root().actions().stream().filter(item -> !item.isGenerated()).findFirst().orElse(null);
        if (action == null) return -1;
        var totalTicks = telemetry.actionDurationTicks();
        if (totalTicks <= 0 && path != null
                && action.type() != SpaceSimulation.ActionType.TRANSMIT_INFORMATION
                && action.type() != SpaceSimulation.ActionType.MAINTAIN_POSITION) {
            var previous = path.samples().isEmpty() ? 0 : path.samples().getFirst().timeSeconds();
            for (var moment : path.actionMoments()) {
                if (moment.actionId().equals(action.id())) {
                    totalTicks = telemetry.actionTicks() + Math.max(0, Math.round((moment.timeSeconds() - previous) * 20));
                    break;
                }
                previous = moment.timeSeconds();
            }
        }
        if (totalTicks <= 0) return -1;
        return Math.clamp((telemetry.actionTicks() + extraTicks) * 100 / totalTicks, 0, 99);
    }

    private static List<Component> serviceDetails(MissionState.Telemetry telemetry) {

        var details = new ArrayList<Component>();
        telemetry.rocket().getDynamicSegments().values().forEach(r -> {
            details.add(Component.translatable("screen.oritech_space_age.resource_pool", "RF", r.availableRF, r.rfCapacity));
            details.add(Component.translatable("screen.oritech_space_age.resource_pool", "Fuel", r.availableFuelBurnTimeTicks, r.fuelCapacity));
            if (r.lastExchange != null)
                details.add(Component.translatable("screen.oritech_space_age.service.exchange_report", r.lastExchange.moved(), r.lastExchange.remaining(),
                        r.lastExchange.fuel(), r.lastExchange.rf()));
            r.crafters.forEach((pos, m) -> {
                if (m.job != null)
                    details.add(Component.translatable("screen.oritech_space_age.pair.job_details", m.name.isBlank() ? pos.toShortString() : m.name,
                            m.job.source().controller().position().toShortString(), m.job.elapsed() * 100 / m.job.duration()));
            });
        });
        return details;
    }

    private static Component status(MissionState.Telemetry telemetry,
                                    RocketFlightPathCalculator.CraftPath path) {

        return status(telemetry, path, 0);
    }

    private static Component status(MissionState.Telemetry telemetry,
                                    RocketFlightPathCalculator.CraftPath path, long extraTicks) {

        Component result = MissionState.isRecovered(telemetry.status())
                ? Component.translatable("screen.oritech_space_age.mission.recovered_earth")
                : readableStatus(telemetry.status());
        var progress = progressPercent(telemetry, path, extraTicks);
        return progress < 0 ? result
                : Component.translatable("screen.oritech_space_age.mission.status_progress", result, progress);
    }

    private static Component craftTitle(MissionState.Telemetry telemetry, int number) {

        if (!telemetry.plan().name().isBlank()) return Component.translatable(
                "screen.oritech_space_age.mission.craft_named", number, telemetry.plan().name());
        var names = telemetry.plan().segmentConfigurations().stream().filter(c -> !c.name().isBlank())
                .filter(c -> telemetry.rocket().getStaticSegments().values().stream()
                        .anyMatch(segment -> SpaceSimulation.SegmentRef.of(segment).equals(c.segment())))
                .map(SpaceSimulation.SegmentConfiguration::name).distinct().limit(2).toList();
        return names.isEmpty() ? Component.translatable("screen.oritech_space_age.mission.craft", number)
                : Component.translatable("screen.oritech_space_age.mission.craft_named",
                number, String.join(" / ", names));
    }

    static Component readableStatus(String status) {

        return MissionState.statusComponent(status);
    }

    @Override
    protected void init() {

        super.init();
        if (!requestedFleet) {
            requestedFleet = true;
            ClientPacketDistributor.sendToServer(new MissionNetworking.FleetRequest(new UUID(0, 0), false));
        }
    }

    @Override
    protected void buildComponents() {

        if (fleetList != null) fleetScrollY = fleetList.getScrollY();
        fleetRows.clear();
        fleetPaths.clear();
        var w = width - 12;
        var h = height - 12;
        setPanelSize(w, h);
        revision = menu.fleetRevision;
        telemetryRevision = menu.telemetryRevision;
        addComponent(new SurfaceWidget(0, 0, w, h, OritechSurface.PANEL));
        addComponent(new LabelWidget(12, 10, w - 170, Component.translatable("block.oritech_space_age.mission_control")).withColor(LabelWidget.DARK_TEXT));
        speedButton = SpaceAgeButtons.panel(w - 152, 7, 140, 18,
                Component.translatable("screen.oritech_space_age.mission.debug_speed", menu.debugSpeed), ignored ->
                        ClientPacketDistributor.sendToServer(new MissionNetworking.DebugSpeed(menu.debugSpeed == 1 ? 5 : menu.debugSpeed == 5 ? 10 : 1)));
        speedButton.setActive(Minecraft.getInstance().player.isCreative());
        speedButton.withTooltip(Component.translatable("screen.oritech_space_age.mission.debug_speed_tooltip"));
        addComponent(speedButton);
        addNetworkIndicators(w, menu.network);
        var listWidth = Math.min(235, w / 3);
        var list = new ScrollWidget(8, 30, listWidth, h - 40).withVerticalScroll(true).withRenderCulling(4);
        fleetList = list;
        var y = 0;
        var network = menu.network;

        var presentation = createFleetPresentation();
        if (presentation != null) fleetPaths.putAll(presentation.paths());
        var nodes = menu.networkNodes;
        var craftNumber = 0;
        for (var entry : menu.fleet) {
            craftNumber++;
            var telemetry = entry.telemetry();
            var path = presentation == null ? null : presentation.paths().get(entry.id());
            var recovered = MissionState.isRecovered(telemetry.status());
            Component status = status(telemetry, path);
            var craftName = craftTitle(telemetry, craftNumber);
            var collapsed = FleetViewPreferences.collapsed(entry.id());
            list.addChild(new SurfaceWidget(2, y, listWidth - 12, collapsed ? 28 : 118, OritechSurface.PANEL_DARK));
            var titleWidth = listWidth - (!collapsed && entry.dismissible() ? 65 : 45);
            var titleLabel = new LabelWidget(8, y + 6, titleWidth, 18, craftName).withBrightColor().withWrap(true);
            titleLabel.withTooltip(craftName);
            list.addChild(titleLabel);
            list.addChild(SpaceAgeButtons.collapse(listWidth - 35, y + 5, collapsed, ignored -> {
                FleetViewPreferences.toggle(entry.id());
                rebuildComponents();
            }));
            if (!collapsed && entry.dismissible()) list.addChild(SpaceAgeButtons.darkPanel(listWidth - 54, y + 5, 16, 16,
                            Component.literal("×"), ignored -> ClientPacketDistributor.sendToServer(new MissionNetworking.DismissCraft(entry.id())))
                    .withTooltip(Component.translatable("screen.oritech_space_age.mission.dismiss_tooltip")));
            var statusLabel = new LabelWidget(8, y + 25, listWidth - 24, 18, status).withBrightColor().withWrap(true);
            statusLabel.setVisible(!collapsed);
            statusLabel.withTooltip(serviceDetails(telemetry));
            list.addChild(statusLabel);
            var locationLabel = new LabelWidget(8, y + 43, listWidth - 24, 18,
                    recovered ? Component.translatable("screen.oritech_space_age.mission.science_delivered")
                            : location(telemetry, menu.fleetSnapshot.objects()))
                    .withBrightColor().withWrap(true);
            list.addChild(locationLabel);
            locationLabel.setVisible(!collapsed);
            var link = entry.communication();
            var radio = new LabelWidget(8, y + 61, listWidth - 24,
                    Component.translatable("screen.oritech_space_age.mission.radio_status",
                            availability(entry.connected()), availability(link.upload())))
                    .withColor(entry.connected() && link.upload() ? 0xFF9FE58A : 0xFFFFC46B);
            radio.withTooltip(Component.translatable(link.reason()),
                    Component.translatable("screen.oritech_space_age.mission.radio_tooltip"));
            list.addChild(radio);
            radio.setVisible(!collapsed);
            var upload = new LabelWidget(8, y + 75, listWidth - 24, 18,
                    Component.translatable("screen.oritech_space_age.mission.upload_distance",
                            distance(link.uploadRange()), distance(link.receiverDistance())))
                    .withBrightColor().withWrap(true);
            upload.withTooltip(Component.translatable(link.reason()));
            list.addChild(upload);
            upload.setVisible(!collapsed);
            var edit = SpaceAgeButtons.panel(8, y + 96, listWidth - 24, 17,
                    Component.translatable(entry.connected() ? "screen.oritech_space_age.mission.view_edit"
                            : "screen.oritech_space_age.mission.view_read_only"), ignored ->
                            ClientPacketDistributor.sendToServer(new MissionNetworking.FleetRequest(entry.id(), true)));
            edit.withTooltip(Component.translatable(entry.connected() ? "screen.oritech_space_age.mission.edit_tooltip"
                            : "screen.oritech_space_age.mission.read_only_tooltip"),
                    Component.translatable(link.reason()), Component.translatable("screen.oritech_space_age.mission.completed_actions", telemetry.completed().size()));
            list.addChild(edit);
            edit.setVisible(!collapsed);
            fleetRows.put(entry.id(), new FleetRow(craftNumber, titleLabel, statusLabel, locationLabel, radio, upload, edit));
            var working = telemetry.rocket().getDynamicSegments().values().stream().anyMatch(r -> !r.reserved.isEmpty()
                    || r.crafters.values().stream().anyMatch(c -> c.job != null));
            if (working && !collapsed) {
                var cancel = SpaceAgeButtons.panel(8, y + 115, listWidth - 24, 17, Component.translatable("screen.oritech_space_age.service.cancel_crafting"),
                        ignored -> ClientPacketDistributor.sendToServer(new MissionNetworking.CancelCrafting(entry.id())));
                cancel.setActive(entry.connected());
                cancel.withTooltip(Component.translatable("screen.oritech_space_age.service.cancel_crafting_help"));
                list.addChild(cancel);
            }
            y += collapsed ? 33 : working ? 143 : 123;
        }
        list.setContentDimensions(listWidth - 8, Math.max(h - 48, y));
        list.setScrollPosition(0, fleetScrollY);
        addComponent(list);
        if (presentation != null) {
            var previous = map;
            map = new RocketStarMapWidget(listWidth + 14, 30, w - listWidth - 24, h - 40,
                    presentation.snapshot(), presentation.flight(), new ActiveRocketData(Map.of(), Map.of()),
                    null, null, ignored -> {
            }, ignored -> {
            });
            map.copyViewFrom(previous);
            map.setHiddenBranches(menu.fleet.stream().map(MissionNetworking.FleetEntry::id)
                    .filter(FleetViewPreferences::collapsed).collect(Collectors.toSet()));
            map.setCoverage(nodes);
            map.setShowSummary(false);
            map.setCommandCoverage(network.groundAntennas() == 0 ? -2 : network.commandAltitude());
            map.setFleetTimes(presentation.times());
            map.setCraftLabels(presentation.labels());
            addComponent(map);
        }
    }

    private FleetPresentation createFleetPresentation() {

        if (menu.fleetSnapshot == null) return null;
        var paths = new ArrayList<RocketFlightPathCalculator.CraftPath>();
        var branches = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        var aborts = new ArrayList<RocketFlightPathCalculator.NavigationAbortMoment>();
        var pathsByCraft = new HashMap<UUID, RocketFlightPathCalculator.CraftPath>();
        var times = new HashMap<UUID, Double>();
        var labels = new HashMap<UUID, Component>();
        var craftNumber = 0;
        for (var entry : menu.fleet) {
            var telemetry = entry.telemetry();
            craftNumber++;
            if (!FleetViewPreferences.collapsed(entry.id())) labels.put(entry.id(), craftTitle(telemetry, craftNumber));
            var forecast = MissionForecast.remainingServices(telemetry.plan(), telemetry.actionTicks(), telemetry.serviceProgress());
            var prediction = entry.navigation().paths().isEmpty()
                    ? RocketFlightPathCalculator.calculateFrom(telemetry.rocket(), menu.fleetSnapshot.objects(), forecast, telemetry.position(),
                            SpaceAgeClientRecipes.vacuum(), menu.fleetSnapshot.stations())
                    : entry.navigation().flight();
            var path = prediction.paths().stream()
                    .filter(item -> item.branchId().equals(telemetry.plan().root().id()))
                    .findFirst().orElse(null);
            if (path == null) continue;
            pathsByCraft.put(entry.id(), path);
            paths.add(new RocketFlightPathCalculator.CraftPath(entry.id(), path.segments(), path.samples(),
                    path.actionMoments(), path.durationSeconds(), path.remainingDeltaV(), path.terminalState()));
            branches.add(new SpaceSimulation.FlightPlanBranch(entry.id(),
                    SpaceSimulation.FlightPlanBranch.NO_PARENT, telemetry.plan().root().actions()));
            times.put(entry.id(), entry.navigation().paths().isEmpty() ? 0.0 : telemetry.actionTicks() / 20.0);
            prediction.navigationAborts().stream().filter(abort -> abort.branchId().equals(path.branchId()))
                    .forEach(abort -> aborts.add(new RocketFlightPathCalculator.NavigationAbortMoment(
                            abort.actionId(), entry.id(), abort.addonId(), abort.actualValue(), abort.timeSeconds(),
                            abort.x(), abort.y(), abort.destinationX(), abort.destinationY())));
        }
        var snapshot = new SpaceSimulation.FlightPlannerSnapshot(menu.fleetSnapshot.simulationId(),
                new UUID(0, 0), menu.fleetSnapshot.objects(), new SpaceSimulation.FlightPlan(branches, List.of()), menu.fleetSnapshot.stations());
        var flight = new RocketFlightPathCalculator.FlightPath(paths, List.of(), 0,
                List.of(), List.of(), aborts);
        return new FleetPresentation(snapshot, flight, pathsByCraft, times, labels);
    }

    private void refreshTelemetry() {

        telemetryRevision = menu.telemetryRevision;
        if (menu.fleetSnapshot == null || map == null) return;
        if (speedButton != null) speedButton.setLabel(
                Component.translatable("screen.oritech_space_age.mission.debug_speed", menu.debugSpeed));

        var presentation = createFleetPresentation();
        if (presentation == null) return;
        fleetPaths.clear();
        fleetPaths.putAll(presentation.paths());
        for (var entry : menu.fleet) {
            var row = fleetRows.get(entry.id());
            if (row == null) {
                rebuildComponents();
                return;
            }
            var telemetry = entry.telemetry();
            var path = presentation.paths().get(entry.id());
            var recovered = MissionState.isRecovered(telemetry.status());
            var link = entry.communication();

            row.title().setText(craftTitle(telemetry, row.number()));
            row.status().setText(status(telemetry, path));
            row.status().withTooltip(serviceDetails(telemetry));
            row.location().setText(recovered
                    ? Component.translatable("screen.oritech_space_age.mission.science_delivered")
                    : location(telemetry, menu.fleetSnapshot.objects()));
            row.radio().setText(Component.translatable("screen.oritech_space_age.mission.radio_status",
                    availability(entry.connected()), availability(link.upload())));
            row.radio().withColor(entry.connected() && link.upload() ? 0xFF9FE58A : 0xFFFFC46B);
            row.radio().setTooltip(List.of(Component.translatable(link.reason()),
                    Component.translatable("screen.oritech_space_age.mission.radio_tooltip")));
            row.upload().setText(Component.translatable("screen.oritech_space_age.mission.upload_distance",
                    distance(link.uploadRange()), distance(link.receiverDistance())));
            row.upload().setTooltip(List.of(Component.translatable(link.reason())));
            row.edit().setLabel(Component.translatable(entry.connected()
                    ? "screen.oritech_space_age.mission.view_edit"
                    : "screen.oritech_space_age.mission.view_read_only"));
            row.edit().setTooltip(List.of(Component.translatable(entry.connected()
                            ? "screen.oritech_space_age.mission.edit_tooltip"
                            : "screen.oritech_space_age.mission.read_only_tooltip"),
                    Component.translatable(link.reason()),
                    Component.translatable("screen.oritech_space_age.mission.completed_actions",
                            telemetry.completed().size())));

        }

        map.updateFlightPath(presentation.snapshot(), presentation.flight(), null);
        map.setCoverage(menu.networkNodes);
        map.setCommandCoverage(menu.network.groundAntennas() == 0 ? -2 : menu.network.commandAltitude());
        map.setFleetTimes(presentation.times());
        map.setCraftLabels(presentation.labels());
    }

    private void advanceTelemetry() {

        if (map == null || Minecraft.getInstance().level == null) return;
        var elapsedTicks = Math.max(0, Minecraft.getInstance().level.getGameTime() - menu.receivedWorldTick)
                * menu.debugSpeed;
        var elapsedSeconds = elapsedTicks / 20.0;
        var times = new HashMap<UUID, Double>();
        for (var entry : menu.fleet) {
            var path = fleetPaths.get(entry.id());
            if (path != null)
                times.put(entry.id(), Math.min(elapsedSeconds + (entry.navigation().paths().isEmpty() ? 0 : entry.telemetry().actionTicks() / 20.0), path.durationSeconds()));
            var row = fleetRows.get(entry.id());
            if (row != null) row.status().setText(status(entry.telemetry(), path, elapsedTicks));
        }
        map.setFleetTimes(times);
    }

    private void addNetworkIndicators(int width, SpaceCommunications.NetworkStatus network) {

        var start = 135;
        var end = width - 160;
        var chipWidth = Math.clamp((end - start - 9) / 4, 56, 105);
        addNetworkIndicator(start, chipWidth, SpaceAgeBlocks.MISSION_CONTROL.get().asItem(),
                Component.translatable("screen.oritech_space_age.mission.ground_short", network.groundAntennas()), network.groundAntennas() > 0,
                List.of(Component.translatable("screen.oritech_space_age.mission.ground_antennas", network.groundAntennas()),
                        Component.translatable("screen.oritech_space_age.mission.ground_tooltip")));
        start += chipWidth + 3;
        addNetworkIndicator(start, chipWidth, SpaceAgeBlocks.ANTENNA.get().asItem(),
                Component.translatable("screen.oritech_space_age.mission.low_short", network.lowSlots(), 6), network.lowComplete(),
                List.of(Component.translatable("screen.oritech_space_age.mission.low_ring", network.lowSlots(), 6),
                        Component.translatable(network.lowComplete() ? "screen.oritech_space_age.mission.low_complete"
                                : "screen.oritech_space_age.mission.low_incomplete")));
        start += chipWidth + 3;
        addNetworkIndicator(start, chipWidth, SpaceAgeBlocks.ANTENNA.get().asItem(),
                Component.translatable("screen.oritech_space_age.mission.high_short", network.highSlots(), 36), network.highComplete(),
                List.of(Component.translatable("screen.oritech_space_age.mission.high_ring", network.highSlots(), 36),
                        Component.translatable(network.highComplete() ? "screen.oritech_space_age.mission.high_complete"
                                : !network.lowComplete() ? "screen.oritech_space_age.mission.high_requires_low"
                                  : "screen.oritech_space_age.mission.high_incomplete")));
        start += chipWidth + 3;
        var coverage = network.groundAntennas() == 0 ? Component.translatable("screen.oritech_space_age.mission.no_range")
                : network.commandAltitude() < 0 ? Component.translatable("screen.oritech_space_age.mission.system_range")
                  : Component.literal(distance(network.commandAltitude()));
        addNetworkIndicator(start, chipWidth, SpaceAgeBlocks.ANTENNA.get().asItem(), coverage,
                network.groundAntennas() > 0,
                List.of(Component.translatable("screen.oritech_space_age.mission.command_coverage", coverage),
                        Component.translatable("screen.oritech_space_age.mission.receiver_gain", (int) network.receiverGain()),
                        Component.translatable("screen.oritech_space_age.mission.command_boundary_tooltip")));
    }

    private void addNetworkIndicator(int x, int width, Item item, Component text,
                                     boolean complete, List<Component> tooltip) {

        var surface = new SurfaceWidget(x, 7, width, 18, complete ? OritechSurface.PANEL_PRESSED : OritechSurface.PANEL);
        surface.withTooltip(tooltip);
        addComponent(surface);
        var icon = new ItemWidget(x + 2, 8, 15, new ItemStack(item)).withShowOverlay(false).withTooltipFromStack(false);
        icon.withTooltip(tooltip);
        addComponent(icon);
        addComponent(new LabelWidget(x + 19, 12, width - 22, text)
                .withColor(complete ? LabelWidget.BRIGHT_TEXT : LabelWidget.DARK_TEXT).withTooltip(tooltip));
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        if (++telemetryTicks >= 20) {
            telemetryTicks = 0;
            ClientPacketDistributor.sendToServer(new MissionNetworking.FleetRequest(new UUID(0, 0), false));
        }
        if (revision != menu.fleetRevision) rebuildComponents();
        else if (telemetryRevision != menu.telemetryRevision) refreshTelemetry();
        advanceTelemetry();
    }

    @Override
    public boolean shouldCreateTitle() {

        return false;
    }

    @Override
    public BlockState getTitleState() {

        return SpaceAgeBlocks.MISSION_CONTROL.get().defaultBlockState();
    }

    private record FleetRow(int number, LabelWidget title, LabelWidget status, LabelWidget location,
                            LabelWidget radio, LabelWidget upload, ButtonWidget edit) {

    }

    private record FleetPresentation(SpaceSimulation.FlightPlannerSnapshot snapshot,
                                     RocketFlightPathCalculator.FlightPath flight,
                                     Map<UUID, RocketFlightPathCalculator.CraftPath> paths,
                                     Map<UUID, Double> times, Map<UUID, Component> labels) {

    }
}
