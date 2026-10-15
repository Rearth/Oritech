package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.OverlayWidget;
import rearth.oritech.spaceage.client.FleetCategories.Category;
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
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final Map<UUID, String> fleetCategories = new HashMap<>();
    private OverlayWidget categoryPopup;
    private EditBox categoryNameField;
    private UUID categoryCraft;
    private String deletingCategory;
    private boolean creatingCategory;
    private String categoryNameDraft = "";

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
                && action.type() != SpaceSimulation.ActionType.MAINTAIN_POSITION
                && action.type() != SpaceSimulation.ActionType.BEAM_POWER) {
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

    static Component compactPower(MissionState.Telemetry telemetry, int simulationSpeed) {

        var power = telemetry.power().perWorldTick(simulationSpeed);
        var net = power.net();
        return Component.translatable("screen.oritech_space_age.power.compact", TooltipHelper.getEnergyText((long) power.solar()),
                (net < 0 ? "−" : "+") + TooltipHelper.getEnergyText((long) Math.abs(net)));
    }

    static List<Component> serviceDetails(MissionState.Telemetry telemetry, int simulationSpeed) {

        var details = new ArrayList<Component>();
        var power = telemetry.power().perWorldTick(simulationSpeed);
        if (simulationSpeed > 1) details.add(Component.translatable("screen.oritech_space_age.power.world_tick_rates", simulationSpeed));
        if (telemetry.position().orbit() != SpaceSimulation.OrbitBand.SURFACE && power.solar() == 0) details.add(Component.translatable("screen.oritech_space_age.power.fuel_upkeep"));
        var stored = telemetry.rocket().getDynamicSegments().values().stream().mapToDouble(r -> r.rf()).sum();
        var capacity = telemetry.rocket().getDynamicSegments().values().stream().mapToDouble(r -> r.rfCapacity).sum();
        var launch = telemetry.rocket().getOriginalSegments().values().stream().mapToLong(segment -> segment.initialRF()).sum();
        details.add(Component.translatable("screen.oritech_space_age.power.stored", TooltipHelper.getEnergyText((long) stored),
                TooltipHelper.getEnergyText((long) capacity), String.format(Locale.ROOT, "%.1f", capacity > 0 ? stored * 100 / capacity : 0)));
        details.add(Component.translatable("screen.oritech_space_age.power.launch", TooltipHelper.getEnergyText(launch)));
        details.add(Component.translatable("screen.oritech_space_age.power.sources", TooltipHelper.getEnergyText((long) power.solar()),
                TooltipHelper.getEnergyText((long) power.incoming())));
        details.add(Component.translatable("screen.oritech_space_age.power.uses", TooltipHelper.getEnergyText((long) power.services()),
                TooltipHelper.getEnergyText((long) power.sent())));
        details.add(Component.translatable(Math.abs(power.net()) < .001 ? "screen.oritech_space_age.power.steady"
                : power.net() > 0 ? "screen.oritech_space_age.power.charging" : "screen.oritech_space_age.power.draining",
                TooltipHelper.getEnergyText((long) Math.abs(power.net()))));
        var action = telemetry.plan().root().actions().stream().filter(item -> !item.isGenerated()).findFirst().orElse(null);
        if (action != null && action.type() == SpaceSimulation.ActionType.BEAM_POWER) {
            var reserve = action.settings().beam().reservePercent();
            details.add(Component.translatable("screen.oritech_space_age.power.reserve", reserve,
                    TooltipHelper.getEnergyText((long) (capacity * reserve / 100d))));
        }
        if (power.sent() > 0 && power.outgoing().incident() > 0) {
            details.add(Component.translatable("screen.oritech_space_age.power.beam_collection",
                    String.format(Locale.ROOT, "%.1f", power.outgoing().coverage() * 100), TooltipHelper.getEnergyText((long) power.outgoing().accepted())));
            details.add(Component.translatable("screen.oritech_space_age.power.beam_losses", TooltipHelper.getEnergyText((long) power.outgoing().missed()),
                    TooltipHelper.getEnergyText((long) power.outgoing().capacityLoss()), TooltipHelper.getEnergyText((long) power.outgoing().storageLoss())));
            if (power.overflow() > .001 && !power.reason().isEmpty()) details.add(Component.translatable(power.reason()));
        }
        if (power.receiving().incident() > 0) {
            details.add(Component.translatable("screen.oritech_space_age.power.receive_collection", String.format(Locale.ROOT, "%.1f", power.receiving().coverage() * 100)));
            details.add(Component.translatable("screen.oritech_space_age.power.beam_losses", TooltipHelper.getEnergyText((long) power.receiving().missed()),
                    TooltipHelper.getEnergyText((long) power.receiving().capacityLoss()), TooltipHelper.getEnergyText((long) power.receiving().storageLoss())));
        }
        var curtailed = power.solar() + power.incoming() - power.services() - power.sent() - power.net();
        if (curtailed > .001) details.add(Component.translatable("screen.oritech_space_age.power.curtailed", TooltipHelper.getEnergyText((long) curtailed)));
        if (power.incoming() > 0 && power.net() < 0) details.add(Component.translatable("screen.oritech_space_age.beam.relay_draining"));
        telemetry.rocket().getDynamicSegments().values().forEach(r -> {
            if (r.fuelCapacity > 0) details.add(Component.translatable("screen.oritech_space_age.resource_pool", "Fuel", r.availableFuelBurnTimeTicks, r.fuelCapacity));
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
        categoryPopup = null;
        categoryNameField = null;
        var y = 0;
        fleetCategories.clear();
        menu.fleet.forEach(entry -> fleetCategories.put(entry.id(), category(entry)));
        var network = menu.network;

        var presentation = createFleetPresentation();
        if (presentation != null) fleetPaths.putAll(presentation.paths());
        var nodes = menu.networkNodes;
        var numbers = new HashMap<UUID, Integer>();
        for (int i = 0; i < menu.fleet.size(); i++) numbers.put(menu.fleet.get(i).id(), i + 1);
        for (var category : FleetViewPreferences.CATEGORIES.categories()) {
            var members = menu.fleet.stream().filter(entry -> category(entry).equals(category.id())).toList();
            var heading = categoryName(category).copy().append(" (" + members.size() + ")");
            list.addChild(SpaceAgeButtons.darkPanel(2, y, listWidth - 61, 22,
                    fit(heading, listWidth - 73), ignored -> {
                        FleetViewPreferences.CATEGORIES.toggle(category.id());
                        rebuildComponents();
                    }).withTooltip(heading));
            list.addChild(SpaceAgeButtons.collapse(listWidth - 35, y + 3, category.collapsed(), ignored -> {
                FleetViewPreferences.CATEGORIES.toggle(category.id());
                rebuildComponents();
            }));
            if (!category.id().equals(FleetCategories.UNCATEGORIZED))
                list.addChild(SpaceAgeButtons.darkPanel(listWidth - 54, y + 3, 16, 16, Component.literal("×"), ignored -> {
                    deletingCategory = category.id();
                    rebuildComponents();
                }).withTooltip(Component.translatable("screen.oritech_space_age.fleet.delete")));
            y += 27;
            if (!category.collapsed()) {
                for (var entry : members) y = addFleetRow(list, listWidth, y, entry, numbers.get(entry.id()), presentation);
            }
        }
        list.addChild(SpaceAgeButtons.panel(2, y + 4, listWidth - 12, 22,
                Component.translatable("screen.oritech_space_age.fleet.create"), ignored -> {
                    creatingCategory = true;
                    categoryNameDraft = "";
                    rebuildComponents();
                }));
        y += 32;
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
            map.setHiddenBranches(menu.fleet.stream().filter(this::hidden).map(MissionNetworking.FleetEntry::id).collect(Collectors.toSet()));
            map.setCoverage(nodes);
            map.setShowSummary(false);
            map.setCommandCoverage(network.groundAntennas() == 0 ? -2 : network.coverageAltitude());
            map.setFleetTimes(presentation.times());
            map.setCraftLabels(presentation.labels());
            addComponent(map);
        }
        buildCategoryPopup(w, h);
    }

    private static Component categoryName(Category category) {

        return switch (category.id()) {
            case FleetCategories.UNCATEGORIZED -> Component.translatable("screen.oritech_space_age.fleet.uncategorized");
            case FleetCategories.SUN -> Component.translatable("screen.oritech_space_age.fleet.sun");
            default -> Component.literal(category.name());
        };
    }

    private Component fit(Component label, int width) {

        return font.width(label) <= width ? label
                : Component.literal(font.plainSubstrByWidth(label.getString(), Math.max(1, width - font.width("..."))) + "...");
    }

    private void closeCategoryPopup() {

        categoryCraft = null;
        deletingCategory = null;
        creatingCategory = false;
        setFocused(null);
        rebuildComponents();
    }

    private void buildCategoryPopup(int width, int height) {

        if (categoryCraft == null && deletingCategory == null && !creatingCategory) return;
        var popupWidth = Math.min(300, width - 24);
        var popupHeight = creatingCategory ? 98 : deletingCategory != null ? 112
                : Math.min(height - 24, 70 + FleetViewPreferences.CATEGORIES.categories().size() * 24);
        var x = (width - popupWidth) / 2;
        var y = (height - popupHeight) / 2;
        categoryPopup = new OverlayWidget(width, height) {

            @Override
            public UIComponent getTopmostHovered(double mouseX, double mouseY) {

                for (var child : getChildren()) {
                    if (child instanceof ScrollWidget scroll && scroll.isMouseOver(mouseX, mouseY))
                        return scroll.getTopmostHovered(mouseX, mouseY);
                }
                return super.getTopmostHovered(mouseX, mouseY);
            }
        }.withDismissHandler(this::closeCategoryPopup);
        categoryPopup.addChild(new SurfaceWidget(x, y, popupWidth, popupHeight, OritechSurface.PANEL_DARK));
        var title = Component.translatable(creatingCategory ? "screen.oritech_space_age.fleet.create"
                : deletingCategory != null ? "screen.oritech_space_age.fleet.delete" : "screen.oritech_space_age.fleet.assign");
        categoryPopup.addChild(new LabelWidget(x + 10, y + 10, popupWidth - 20, fit(title, popupWidth - 20)).withBrightColor());
        if (creatingCategory) {
            var create = SpaceAgeButtons.panel(x + 10, y + popupHeight - 28, popupWidth / 2 - 15, 20,
                    Component.translatable("gui.done"), ignored -> {
                        if (FleetViewPreferences.CATEGORIES.create(categoryNameDraft) != null) closeCategoryPopup();
                    });
            create.setActive(!categoryNameDraft.isBlank());
            categoryPopup.addChild(create);
            categoryNameField = addWidget(new EditBox(font, leftPos + x + 10, topPos + y + 33,
                    popupWidth - 20, 20, Component.translatable("screen.oritech_space_age.fleet.name")));
            categoryNameField.setMaxLength(48);
            categoryNameField.setValue(categoryNameDraft);
            categoryNameField.setResponder(value -> {
                categoryNameDraft = value;
                create.setActive(!value.isBlank());
            });
            setFocused(categoryNameField);
            categoryPopup.addChild(SpaceAgeButtons.panel(x + popupWidth / 2 + 5, y + popupHeight - 28,
                    popupWidth / 2 - 15, 20, Component.translatable("gui.cancel"), ignored -> closeCategoryPopup()));
        } else if (deletingCategory != null) {
            categoryPopup.addChild(new LabelWidget(x + 10, y + 34, popupWidth - 20, 35,
                    Component.translatable("screen.oritech_space_age.fleet.delete_help")).withBrightColor().withWrap(true));
            categoryPopup.addChild(SpaceAgeButtons.panel(x + 10, y + popupHeight - 28, popupWidth / 2 - 15, 20,
                    Component.translatable("screen.oritech_space_age.fleet.delete"), ignored -> {
                        var members = menu.fleet.stream().filter(entry -> category(entry).equals(deletingCategory))
                                .map(MissionNetworking.FleetEntry::id).toList();
                        FleetViewPreferences.CATEGORIES.delete(deletingCategory, members);
                        closeCategoryPopup();
                    }));
            categoryPopup.addChild(SpaceAgeButtons.panel(x + popupWidth / 2 + 5, y + popupHeight - 28,
                    popupWidth / 2 - 15, 20, Component.translatable("gui.cancel"), ignored -> closeCategoryPopup()));
        } else {
            var choices = new ScrollWidget(x + 8, y + 29, popupWidth - 16, popupHeight - 62).withVerticalScroll(true);
            var rowY = 0;
            for (var category : FleetViewPreferences.CATEGORIES.categories()) {
                var label = categoryName(category);
                var selected = menu.fleet.stream().anyMatch(entry -> entry.id().equals(categoryCraft) && category(entry).equals(category.id()));
                var choiceLabel = selected ? Component.translatable("screen.oritech_space_age.fleet.current", label) : label;
                var choice = SpaceAgeButtons.panel(2, rowY, popupWidth - 32, 20, fit(choiceLabel, popupWidth - 44), ignored -> {
                    FleetViewPreferences.CATEGORIES.assign(categoryCraft, category.id());
                    closeCategoryPopup();
                });
                choice.withTooltip(label);
                choices.addChild(choice);
                rowY += 24;
            }
            choices.setContentDimensions(popupWidth - 32, rowY);
            categoryPopup.addChild(choices);
            categoryPopup.addChild(SpaceAgeButtons.panel(x + popupWidth - 90, y + popupHeight - 26, 80, 18,
                    Component.translatable("gui.cancel"), ignored -> closeCategoryPopup()));
        }
        addComponent(categoryPopup);
    }

    @Override
    protected void renderOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        super.renderOverlays(graphics, mouseX, mouseY, partialTick);
        if (categoryNameField != null) {
            graphics.nextStratum();
            categoryNameField.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        if (categoryNameField != null && categoryNameField.isMouseOver(event.x(), event.y())) {
            setFocused(categoryNameField);
            return categoryNameField.mouseClicked(event, doubleClick);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {

        if (categoryPopup != null) {
            if (categoryNameField != null) categoryNameField.mouseDragged(event, dragX, dragY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {

        if (categoryPopup != null) return categoryPopup.handleMouseScroll(mouseX - leftPos, mouseY - topPos, scrollY);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        if (categoryPopup != null && event.isEscape()) {
            closeCategoryPopup();
            return true;
        }
        if (categoryNameField != null) {
            if (event.isConfirmation()) {
                if (FleetViewPreferences.CATEGORIES.create(categoryNameDraft) != null) closeCategoryPopup();
            } else categoryNameField.keyPressed(event);
            return true;
        }
        if (categoryPopup != null) return true;
        return super.keyPressed(event);
    }

    private int addFleetRow(ScrollWidget list, int listWidth, int y, MissionNetworking.FleetEntry entry,
                            int craftNumber, FleetPresentation presentation) {

        var telemetry = entry.telemetry();
        var path = presentation == null ? null : presentation.paths().get(entry.id());
        var recovered = MissionState.isRecovered(telemetry.status());
        Component status = status(telemetry, path);
        var craftName = craftTitle(telemetry, craftNumber);
        var collapsed = FleetViewPreferences.collapsed(entry.id());
        var working = telemetry.rocket().getDynamicSegments().values().stream().anyMatch(r -> !r.reserved.isEmpty()
                || r.crafters.values().stream().anyMatch(c -> c.job != null));
        list.addChild(new SurfaceWidget(2, y, listWidth - 12, collapsed ? 28 : working ? 120 : 100, OritechSurface.PANEL_DARK) {

            @Override
            public boolean handleClick(double mouseX, double mouseY, int button) {

                if (button != 1) return false;
                categoryCraft = entry.id();
                rebuildComponents();
                return true;
            }
        }.withTooltip(Component.translatable("screen.oritech_space_age.fleet.assign_hint")));
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
        statusLabel.withTooltip(serviceDetails(telemetry, menu.debugSpeed));
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
                        availability(entry.connected())))
                .withColor(entry.connected() ? 0xFF9FE58A : 0xFFFFC46B);
        radio.withTooltip(Component.translatable(link.reason()),
                Component.translatable("screen.oritech_space_age.mission.radio_tooltip"),
                Component.translatable("screen.oritech_space_age.mission.coverage_distance", coverageDistance(link.coverageAltitude()), distance(link.altitude())));
        list.addChild(radio);
        radio.setVisible(!collapsed);
        var edit = SpaceAgeButtons.panel(8, y + 78, listWidth - 24, 17,
                Component.translatable(entry.connected() ? "screen.oritech_space_age.mission.view_edit"
                        : "screen.oritech_space_age.mission.view_read_only"), ignored ->
                        ClientPacketDistributor.sendToServer(new MissionNetworking.FleetRequest(entry.id(), true)));
        edit.withTooltip(Component.translatable(entry.connected() ? "screen.oritech_space_age.mission.edit_tooltip"
                        : "screen.oritech_space_age.mission.read_only_tooltip"),
                Component.translatable(link.reason()), Component.translatable("screen.oritech_space_age.mission.completed_actions", telemetry.completed().size()));
        list.addChild(edit);
        edit.setVisible(!collapsed);
        fleetRows.put(entry.id(), new FleetRow(craftNumber, titleLabel, statusLabel, locationLabel, radio, edit));
        if (working && !collapsed) {
            var cancel = SpaceAgeButtons.panel(8, y + 97, listWidth - 24, 17, Component.translatable("screen.oritech_space_age.service.cancel_crafting"),
                    ignored -> ClientPacketDistributor.sendToServer(new MissionNetworking.CancelCrafting(entry.id())));
            cancel.setActive(entry.connected());
            cancel.withTooltip(Component.translatable("screen.oritech_space_age.service.cancel_crafting_help"));
            list.addChild(cancel);
        }
        return y + (collapsed ? 33 : working ? 125 : 105);
    }

    private String category(MissionNetworking.FleetEntry entry) {

        var position = entry.telemetry().position();
        return FleetViewPreferences.CATEGORIES.category(entry.id(), position.target().equals(SpaceSimulation.SUN_ID)
                && position.orbit() == SpaceSimulation.OrbitBand.SOLAR);
    }

    private boolean hidden(MissionNetworking.FleetEntry entry) {

        return FleetViewPreferences.collapsed(entry.id()) || !visible(entry);
    }

    private boolean visible(MissionNetworking.FleetEntry entry) {

        return !FleetViewPreferences.CATEGORIES.collapsed(category(entry));
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
            if (!hidden(entry)) labels.put(entry.id(), craftTitle(telemetry, craftNumber));
            var forecast = MissionForecast.remainingServices(telemetry.plan(), telemetry.actionTicks(), telemetry.serviceProgress());
            if (!telemetry.fullLayout() && entry.navigation().paths().isEmpty()) {
                var p = telemetry.position();
                var sample = new RocketFlightPathCalculator.PathSample(0, p.x(), p.y(), Math.hypot(p.vx(), p.vy()), p.vx(), p.vy(),
                        RocketFlightPathCalculator.PathPhase.COAST, p.target(), SpaceSimulation.FlightPlanAction.NO_TARGET, p.stage(), Set.of(), Set.of(), p.asteroid());
                var path = new RocketFlightPathCalculator.CraftPath(entry.id(), Set.of(), List.of(sample), List.of(), 0, 0,
                        RocketFlightPathCalculator.TerminalState.READY);
                paths.add(path);
                pathsByCraft.put(entry.id(), path);
                branches.add(new SpaceSimulation.FlightPlanBranch(entry.id(), SpaceSimulation.FlightPlanBranch.NO_PARENT, telemetry.plan().root().actions()));
                times.put(entry.id(), 0d);
                continue;
            }
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
                    path.actionMoments(), path.durationSeconds(), path.remainingDeltaV(), path.terminalState(), path.failure()));
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

        if (menu.fleet.stream().anyMatch(entry -> !Objects.equals(fleetCategories.get(entry.id()), category(entry)))
                || !fleetRows.keySet().equals(menu.fleet.stream().filter(this::visible).map(MissionNetworking.FleetEntry::id).collect(Collectors.toSet()))) {
            rebuildComponents();
            return;
        }
        var presentation = createFleetPresentation();
        if (presentation == null) return;
        fleetPaths.clear();
        fleetPaths.putAll(presentation.paths());
        for (var entry : menu.fleet) {
            if (!visible(entry)) continue;
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
            row.status().withTooltip(serviceDetails(telemetry, menu.debugSpeed));
            row.location().setText(recovered
                    ? Component.translatable("screen.oritech_space_age.mission.science_delivered")
                    : location(telemetry, menu.fleetSnapshot.objects()));
            row.radio().setText(Component.translatable("screen.oritech_space_age.mission.radio_status",
                    availability(entry.connected())));
            row.radio().withColor(entry.connected() ? 0xFF9FE58A : 0xFFFFC46B);
            row.radio().setTooltip(List.of(Component.translatable(link.reason()),
                    Component.translatable("screen.oritech_space_age.mission.radio_tooltip")));
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
        map.setCommandCoverage(menu.network.groundAntennas() == 0 ? -2 : menu.network.coverageAltitude());
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

    private static Component coverageDistance(double altitude) {

        return altitude < 0 ? Component.translatable("screen.oritech_space_age.mission.system_range")
                : Component.literal(distance(altitude));
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
                : network.coverageAltitude() < 0 ? Component.translatable("screen.oritech_space_age.mission.system_range")
                  : Component.literal(distance(network.coverageAltitude()));
        addNetworkIndicator(start, chipWidth, SpaceAgeBlocks.ANTENNA.get().asItem(), coverage,
                network.groundAntennas() > 0,
                List.of(Component.translatable("screen.oritech_space_age.mission.command_coverage", coverage),
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
                            LabelWidget radio, ButtonWidget edit) {

    }

    private record FleetPresentation(SpaceSimulation.FlightPlannerSnapshot snapshot,
                                     RocketFlightPathCalculator.FlightPath flight,
                                     Map<UUID, RocketFlightPathCalculator.CraftPath> paths,
                                     Map<UUID, Double> times, Map<UUID, Component> labels) {

    }
}
