package rearth.oritech.spaceage.client.mission_control;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.OverlayWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.mission_control.MissionControlMenu;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.spaceage.client.SpaceAgeClientRecipes;
import rearth.oritech.spaceage.client.mission_control.FleetCategories.Category;
import rearth.oritech.spaceage.client.planner.RocketFlightPlannerScreen;
import rearth.oritech.spaceage.client.starmap.RocketStarMapWidget;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.mission.MissionForecast;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.survey.SpaceCommunications;
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
import java.util.stream.Stream;

/**
 * Fleet view shares the map and opens the live dashboard for the selected craft.
 */
public class MissionControlScreen extends OritechWidgetScreen<MissionControlMenu> {

    private final Map<UUID, FleetRow> fleetRows = new HashMap<>();
    private final Map<UUID, RocketFlightPathCalculator.CraftPath> fleetPaths = new HashMap<>();
    private final Map<UUID, String> fleetCategories = new HashMap<>();
    private int revision = -1;
    private int telemetryRevision = -1;
    private int telemetryTicks;
    private boolean requestedFleet;
    private RocketStarMapWidget map;
    private ScrollWidget fleetList;
    private float fleetScrollY;
    private ButtonWidget speedButton;
    private OverlayWidget categoryPopup;
    private EditBox categoryNameField;
    private UUID categoryCraft;
    private String deletingCategory;
    private UUID discardingCraft;
    private String redirectingCategory;
    private UUID beamTarget = SpaceObjects.EARTH_ID;
    private String beamX = "0";
    private String beamZ = "0";
    private EditBox beamXField;
    private EditBox beamZField;
    private boolean confirmDeleteCraft;
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
                var missionStructureChanged = menu.updateMission(telemetry, selected.connected());
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
            var current = menu.acceptedPlan.currentAction();
            menu.currentAction = current == null ? null : current.id();
            menu.setPreview(payload.rocket());
            menu.markDraftFlightPlanSaved();
            menu.setFlightPlannerSnapshot(payload.snapshot());
            var title = Component.translatable("block.oritech_space_age.mission_control");
            Minecraft.getInstance().setScreen(Minecraft.getInstance().screen instanceof RocketFlightPlannerScreen
                    ? new RocketFlightPlannerScreen(menu, player.getInventory(), title)
                    : new CraftStatusScreen(menu, player.getInventory(), title));
        }
    }

    public static Component location(MissionState.Telemetry telemetry, List<SpaceSimulation.SpaceObjectData> objects) {

        var action = telemetry.plan().currentAction();
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

    public static int progressPercent(MissionState.Telemetry telemetry,
                                      RocketFlightPathCalculator.CraftPath path, long extraTicks) {

        var action = telemetry.plan().currentAction();
        if (action == null) return -1;
        var totalTicks = telemetry.actionDurationTicks();
        if (totalTicks <= 0 && path != null
                && action.type() != SpaceSimulation.ActionType.TRANSMIT_INFORMATION
                && !action.type().stationKeeping()) {
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

    public static Component compactPower(MissionState.Telemetry telemetry, int simulationSpeed) {

        var power = telemetry.power().perWorldTick(simulationSpeed);
        var net = power.net();
        return Component.translatable("screen.oritech_space_age.power.compact", TooltipHelper.getEnergyText((long) power.solar()),
                (net < 0 ? "−" : "+") + TooltipHelper.getEnergyText((long) Math.abs(net)));
    }

    public static List<Component> serviceDetails(MissionState.Telemetry telemetry, int simulationSpeed) {

        var details = new ArrayList<Component>();
        var power = telemetry.power().perWorldTick(simulationSpeed);
        if (simulationSpeed > 1)
            details.add(Component.translatable("screen.oritech_space_age.power.world_tick_rates", simulationSpeed));
        if (telemetry.position().orbit() != SpaceSimulation.OrbitBand.SURFACE && power.solar() == 0)
            details.add(Component.translatable("screen.oritech_space_age.power.fuel_upkeep"));
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
        var action = telemetry.plan().currentAction();
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
            if (power.overflow() > .001 && !power.reason().isEmpty())
                details.add(Component.translatable(power.reason()));
        }
        if (power.receiving().incident() > 0) {
            details.add(Component.translatable("screen.oritech_space_age.power.receive_collection", String.format(Locale.ROOT, "%.1f", power.receiving().coverage() * 100)));
            details.add(Component.translatable("screen.oritech_space_age.power.beam_losses", TooltipHelper.getEnergyText((long) power.receiving().missed()),
                    TooltipHelper.getEnergyText((long) power.receiving().capacityLoss()), TooltipHelper.getEnergyText((long) power.receiving().storageLoss())));
        }
        var curtailed = power.solar() + power.incoming() - power.services() - power.sent() - power.net();
        if (curtailed > .001)
            details.add(Component.translatable("screen.oritech_space_age.power.curtailed", TooltipHelper.getEnergyText((long) curtailed)));
        if (power.incoming() > 0 && power.net() < 0)
            details.add(Component.translatable("screen.oritech_space_age.beam.relay_draining"));
        telemetry.rocket().getDynamicSegments().values().forEach(r -> {
            if (r.fuelCapacity > 0)
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
                                    RocketFlightPathCalculator.CraftPath path, long extraTicks) {

        Component result = MissionState.isRecovered(telemetry.status())
                ? Component.translatable("screen.oritech_space_age.mission.recovered_earth")
                : MissionState.statusComponent(telemetry.status());
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

    private static Component categoryName(Category category) {

        return switch (category.id()) {
            case FleetCategories.UNCATEGORIZED ->
                    Component.translatable("screen.oritech_space_age.fleet.uncategorized");
            case FleetCategories.SUN -> Component.translatable("screen.oritech_space_age.fleet.sun");
            default -> Component.literal(category.name());
        };
    }

    private static boolean validCoordinate(String value) {

        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean inSunOrbit(MissionState.Position position) {

        return position.target().equals(SpaceSimulation.SUN_ID) && position.orbit() == SpaceSimulation.OrbitBand.SOLAR;
    }

    private static Component coverageDistance(double altitude) {

        return altitude < 0 ? Component.translatable("screen.oritech_space_age.mission.system_range")
                : Component.literal(distance(altitude));
    }

    @Override
    protected void init() {

        super.init();
        if (!requestedFleet) {
            requestedFleet = true;
            ClientPacketDistributor.sendToServer(MissionNetworking.FleetRequest.POLL);
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
        var list = new ScrollWidget(8, 30, listWidth, h - 40) {

            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

                graphics.fill(x, y, x + width, y + height, 0xFF2A3036);
                super.renderContent(graphics, mouseX, mouseY, delta);
            }
        }.withVerticalScroll(true).withRenderCulling(4);
        list.setSurface(OritechSurface.NONE);
        fleetList = list;
        categoryPopup = null;
        categoryNameField = null;
        beamXField = null;
        beamZField = null;
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
            var sectionY = y;
            list.addChild(FleetListWidgets.section(2, y, listWidth - 12, categoryName(category), members.size(),
                    category.collapsed(), () -> {
                        FleetViewPreferences.CATEGORIES.toggle(category.id());
                        rebuildComponents();
                    }, () -> {
                        deletingCategory = category.id();
                        rebuildComponents();
                    }));
            y += 27;
            if (!category.collapsed()) {
                for (var entry : members) {
                    if (shown(entry)) y = addFleetRow(list, listWidth, y, entry, numbers.get(entry.id()), presentation);
                }
                var filtered = members.stream().filter(entry -> !shown(entry)).count();
                if (filtered > 0) {
                    list.addChild(new LabelWidget(20, y + 5, listWidth - 36,
                            Component.translatable("screen.oritech_space_age.fleet.filtered", filtered)).withColor(0xFF9EAAB5));
                    y += 22;
                }
            }
            if (y > sectionY + 27) list.addChild(FleetListWidgets.guide(7, sectionY + 27, y - sectionY - 29));
            y += 8;
        }
        list.addChild(SpaceAgeButtons.darkPanel(14, y + 4, listWidth - 24, 20,
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
            map.setSunVisibilityListener(this::rebuildComponents);
            map.setHiddenBranches(menu.fleet.stream().filter(this::hidden).map(MissionNetworking.FleetEntry::id).collect(Collectors.toSet()));
            map.setCoverage(nodes);
            map.setShowSummary(false);
            map.setCommandCoverage(network.groundAntennas() == 0 ? -2 : network.coverageAltitude());
            map.setFleetTimes(presentation.times());
            map.setCraftLabels(presentation.labels());
            map.setBranchSegmentNames(presentation.segmentNames());
            addComponent(map);
        }
        buildCategoryPopup(w, h);
    }

    private Component fit(Component label, int width) {

        return font.width(label) <= width ? label
                : Component.literal(font.plainSubstrByWidth(label.getString(), Math.max(1, width - font.width("..."))) + "...");
    }

    private void closeCategoryPopup() {

        categoryCraft = null;
        deletingCategory = null;
        discardingCraft = null;
        redirectingCategory = null;
        confirmDeleteCraft = false;
        creatingCategory = false;
        setFocused(null);
        rebuildComponents();
    }

    private void buildCategoryPopup(int width, int height) {

        if (categoryCraft == null && deletingCategory == null && discardingCraft == null && redirectingCategory == null && !creatingCategory)
            return;
        var popupWidth = Math.min(300, width - 24);
        var popupHeight = creatingCategory ? 98 : discardingCraft != null ? 158 : redirectingCategory != null ? Math.min(300, height - 24) : deletingCategory != null ? 218
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
                : discardingCraft != null ? "screen.oritech_space_age.fleet.discard"
                  : redirectingCategory != null ? "screen.oritech_space_age.fleet.redirect"
                    : deletingCategory != null ? "screen.oritech_space_age.fleet.actions" : "screen.oritech_space_age.fleet.assign");
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
        } else if (discardingCraft != null) {
            categoryPopup.addChild(new LabelWidget(x + 10, y + 33, popupWidth - 20, 70,
                    Component.translatable("screen.oritech_space_age.fleet.discard_warning"))
                    .withColor(0xFFFF7777).withWrap(true));
            categoryPopup.addChild(SpaceAgeButtons.panel(x + 10, y + popupHeight - 28, popupWidth / 2 - 15, 20,
                    Component.translatable("screen.oritech_space_age.fleet.discard").withStyle(ChatFormatting.RED), ignored -> {
                        ClientPacketDistributor.sendToServer(new MissionNetworking.DeleteCrafts(List.of(discardingCraft)));
                        closeCategoryPopup();
                    }));
            categoryPopup.addChild(SpaceAgeButtons.panel(x + popupWidth / 2 + 5, y + popupHeight - 28, popupWidth / 2 - 15, 20,
                    Component.translatable("gui.cancel"), ignored -> closeCategoryPopup()));
        } else if (redirectingCategory != null) {
            buildBeamRedirect(x, y, popupWidth, popupHeight);
        } else if (deletingCategory != null) {
            var members = menu.fleet.stream().filter(entry -> category(entry).equals(deletingCategory))
                    .map(MissionNetworking.FleetEntry::id).toList();
            categoryPopup.addChild(new LabelWidget(x + 10, y + 32, popupWidth - 20, 58,
                    Component.translatable(confirmDeleteCraft ? "screen.oritech_space_age.fleet.delete_craft_confirm"
                            : "screen.oritech_space_age.fleet.delete_help", members.size())).withColor(confirmDeleteCraft ? 0xFFFF7777 : LabelWidget.BRIGHT_TEXT).withWrap(true));
            var deleteCategory = SpaceAgeButtons.panel(x + 10, y + popupHeight - 54, popupWidth - 20, 20,
                    Component.translatable(confirmDeleteCraft
                            ? FleetCategories.permanent(deletingCategory) ? "screen.oritech_space_age.fleet.discard_all_now"
                              : "screen.oritech_space_age.fleet.delete_craft_now" : "screen.oritech_space_age.fleet.delete"), ignored -> {
                        if (confirmDeleteCraft)
                            ClientPacketDistributor.sendToServer(new MissionNetworking.DeleteCrafts(members));
                        FleetViewPreferences.CATEGORIES.delete(deletingCategory, members);
                        closeCategoryPopup();
                    });
            deleteCategory.setActive(confirmDeleteCraft || !FleetCategories.permanent(deletingCategory));
            categoryPopup.addChild(deleteCategory);
            if (!confirmDeleteCraft) {
                var beaming = categoryBeamCraft(deletingCategory);
                var redirect = SpaceAgeButtons.panel(x + 10, y + popupHeight - 108, popupWidth - 20, 20,
                        Component.translatable("screen.oritech_space_age.fleet.redirect_count", beaming.size()), ignored -> {
                            redirectingCategory = deletingCategory;
                            deletingCategory = null;
                            beamTarget = SpaceObjects.EARTH_ID;
                            rebuildComponents();
                        });
                redirect.setActive(!beaming.isEmpty());
                categoryPopup.addChild(redirect);
                var deleteCraft = SpaceAgeButtons.panel(x + 10, y + popupHeight - 80, popupWidth - 20, 20,
                        Component.translatable("screen.oritech_space_age.fleet.delete_with_craft"), ignored -> {
                            confirmDeleteCraft = true;
                            rebuildComponents();
                        });
                deleteCraft.setActive(!members.isEmpty());
                categoryPopup.addChild(deleteCraft);
            }
            categoryPopup.addChild(SpaceAgeButtons.panel(x + popupWidth - 90, y + popupHeight - 28,
                    80, 20, Component.translatable("gui.cancel"), ignored -> closeCategoryPopup()));
        } else {
            var choices = new ScrollWidget(x + 8, y + 29, popupWidth - 16, popupHeight - 62).withVerticalScroll(true);
            var rowY = 0;
            for (var category : FleetViewPreferences.CATEGORIES.categories()) {
                if (category.id().equals(FleetCategories.SUN)) continue;
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

    private List<EditBox> popupFields() {

        return Stream.of(categoryNameField, beamXField, beamZField).filter(Objects::nonNull).toList();
    }

    private List<UUID> categoryBeamCraft(String categoryId) {

        return menu.fleet.stream().filter(entry -> category(entry).equals(categoryId))
                .filter(entry -> entry.telemetry().plan().currentAction() != null
                        && entry.telemetry().plan().currentAction().type() == SpaceSimulation.ActionType.BEAM_POWER)
                .map(MissionNetworking.FleetEntry::id).toList();
    }

    private void buildBeamRedirect(int x, int y, int width, int height) {

        var ids = categoryBeamCraft(redirectingCategory);
        categoryPopup.addChild(new LabelWidget(x + 10, y + 30, width - 20, 30,
                Component.translatable("screen.oritech_space_age.fleet.redirect_help", ids.size())).withBrightColor().withWrap(true));
        var targets = new ScrollWidget(x + 8, y + 64, width - 16, height - 146).withVerticalScroll(true);
        var objects = new ArrayList<SpaceSimulation.SpaceObjectData>();
        menu.fleetSnapshot.objects().stream().filter(object -> object.id().equals(SpaceObjects.EARTH_ID)).forEach(objects::add);
        menu.fleetSnapshot.stations().stream().map(station -> station.point()).forEach(objects::add);
        var row = 0;
        for (var target : objects) {
            var label = RocketStarMapWidget.objectName(target);
            var selected = target.id().equals(beamTarget);
            var button = SpaceAgeButtons.panel(2, row, width - 32, 20,
                    fit(selected ? Component.translatable("screen.oritech_space_age.fleet.current", label) : label, width - 44), ignored -> {
                        beamTarget = target.id();
                        rebuildComponents();
                    });
            button.withTooltip(label);
            targets.addChild(button);
            row += 24;
        }
        targets.setContentDimensions(width - 32, row);
        categoryPopup.addChild(targets);
        if (beamTarget.equals(SpaceObjects.EARTH_ID)) {
            var fieldWidth = (width - 48) / 2;
            categoryPopup.addChild(new LabelWidget(x + 10, y + height - 70, 12, Component.literal("X")).withBrightColor());
            categoryPopup.addChild(new LabelWidget(x + 30 + fieldWidth, y + height - 70, 12, Component.literal("Z")).withBrightColor());
            beamXField = addWidget(new EditBox(font, leftPos + x + 22, topPos + y + height - 74, fieldWidth, 20, Component.literal("X")));
            beamZField = addWidget(new EditBox(font, leftPos + x + 42 + fieldWidth, topPos + y + height - 74, fieldWidth, 20, Component.literal("Z")));
            beamXField.setValue(beamX);
            beamZField.setValue(beamZ);
        }
        var apply = SpaceAgeButtons.panel(x + 10, y + height - 28, width / 2 - 15, 20,
                Component.translatable("screen.oritech_space_age.fleet.redirect_apply"), ignored -> {
                    ClientPacketDistributor.sendToServer(new MissionNetworking.RedirectBeams(ids, beamTarget,
                            beamTarget.equals(SpaceObjects.EARTH_ID) ? Integer.parseInt(beamX) : 0,
                            beamTarget.equals(SpaceObjects.EARTH_ID) ? Integer.parseInt(beamZ) : 0));
                    closeCategoryPopup();
                });
        Runnable validate = () -> apply.setActive(!ids.isEmpty() && (!beamTarget.equals(SpaceObjects.EARTH_ID)
                || validCoordinate(beamX) && validCoordinate(beamZ)));
        if (beamXField != null) beamXField.setResponder(value -> {
            beamX = value;
            validate.run();
        });
        if (beamZField != null) beamZField.setResponder(value -> {
            beamZ = value;
            validate.run();
        });
        validate.run();
        categoryPopup.addChild(apply);
        categoryPopup.addChild(SpaceAgeButtons.panel(x + width / 2 + 5, y + height - 28, width / 2 - 15, 20,
                Component.translatable("gui.cancel"), ignored -> closeCategoryPopup()));
    }

    @Override
    protected void renderOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        super.renderOverlays(graphics, mouseX, mouseY, partialTick);
        for (var field : popupFields()) {
            graphics.nextStratum();
            field.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        for (var field : popupFields()) {
            if (!field.isMouseOver(event.x(), event.y())) continue;
            setFocused(field);
            return field.mouseClicked(event, doubleClick);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {

        if (categoryPopup != null) {
            for (var field : popupFields())
                if (getFocused() == field) field.mouseDragged(event, dragX, dragY);
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
        if (categoryPopup != null) {
            for (var field : popupFields())
                if (getFocused() == field) field.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    private int addFleetRow(ScrollWidget list, int listWidth, int y, MissionNetworking.FleetEntry entry,
                            int craftNumber, FleetPresentation presentation) {

        var telemetry = entry.telemetry();
        var collapsed = FleetViewPreferences.collapsed(entry.id());
        var working = telemetry.rocket().getDynamicSegments().values().stream().anyMatch(r -> !r.reserved.isEmpty()
                || r.crafters.values().stream().anyMatch(c -> c.job != null));
        list.addChild(FleetListWidgets.row(14, y, listWidth - 24, collapsed ? 24 : working ? 120 : 100, () -> {
            FleetViewPreferences.toggle(entry.id());
            rebuildComponents();
        }, () -> {
            categoryCraft = entry.id();
            rebuildComponents();
        }).withZIndex(-1).withTooltip(Component.translatable("screen.oritech_space_age.fleet.assign_hint")));
        var titleWidth = listWidth - (!collapsed ? 58 : 40);
        var titleLabel = new LabelWidget(20, y + 6, titleWidth, 18, Component.empty()).withBrightColor();
        list.addChild(titleLabel);
        if (!collapsed) list.addChild(SpaceAgeButtons.darkPanel(listWidth - 34, y + 5, 16, 16,
                        Component.literal("×"), ignored -> {
                            discardingCraft = entry.id();
                            rebuildComponents();
                        })
                .withTooltip(Component.translatable("screen.oritech_space_age.fleet.discard")));
        var statusLabel = new LabelWidget(20, y + 25, listWidth - 36, 18, Component.empty()).withBrightColor().withWrap(true);
        var locationLabel = new LabelWidget(20, y + 43, listWidth - 36, 18, Component.empty()).withBrightColor().withWrap(true);
        var radio = new LabelWidget(20, y + 61, listWidth - 36, Component.empty());
        var edit = SpaceAgeButtons.panel(20, y + 78, listWidth - 36, 17,
                Component.translatable("screen.oritech_space_age.craft.open"), ignored ->
                        ClientPacketDistributor.sendToServer(new MissionNetworking.FleetRequest(entry.id(), true)));
        for (var detail : List.of(statusLabel, locationLabel, radio, edit)) {
            detail.setVisible(!collapsed);
            list.addChild(detail);
        }
        var row = new FleetRow(craftNumber, titleLabel, statusLabel, locationLabel, radio, edit);
        fleetRows.put(entry.id(), row);
        updateRow(row, entry, presentation == null ? null : presentation.paths().get(entry.id()), 0);
        if (working && !collapsed) {
            var cancel = SpaceAgeButtons.panel(20, y + 97, listWidth - 36, 17, Component.translatable("screen.oritech_space_age.service.cancel_crafting"),
                    ignored -> ClientPacketDistributor.sendToServer(new MissionNetworking.CancelCrafting(entry.id())));
            cancel.setActive(entry.connected());
            cancel.withTooltip(Component.translatable("screen.oritech_space_age.service.cancel_crafting_help"));
            list.addChild(cancel);
        }
        return y + (collapsed ? 26 : working ? 122 : 102);
    }

    /**
     * Fills the row's labels from live telemetry; shared by building and in-place refreshes.
     */
    private void updateRow(FleetRow row, MissionNetworking.FleetEntry entry, RocketFlightPathCalculator.CraftPath path, long extraTicks) {

        var telemetry = entry.telemetry();
        var link = entry.communication();
        var title = craftTitle(telemetry, row.number());
        row.title().setText(fit(title, row.title().getWidth()));
        row.title().withTooltip(title);
        row.status().setText(status(telemetry, path, extraTicks));
        row.status().withTooltip(serviceDetails(telemetry, menu.debugSpeed));
        row.location().setText(MissionState.isRecovered(telemetry.status())
                ? Component.translatable("screen.oritech_space_age.mission.science_delivered")
                : location(telemetry, menu.fleetSnapshot.objects()));
        row.radio().setText(Component.translatable("screen.oritech_space_age.mission.radio_status", availability(entry.connected())));
        row.radio().withColor(entry.connected() ? 0xFF9FE58A : 0xFFFFC46B);
        row.radio().setTooltip(List.of(Component.translatable(link.reason()),
                Component.translatable("screen.oritech_space_age.mission.radio_tooltip"),
                Component.translatable("screen.oritech_space_age.mission.coverage_distance",
                        coverageDistance(link.coverageAltitude()), distance(link.altitude()))));
        row.edit().setTooltip(List.of(Component.translatable("screen.oritech_space_age.craft.open_help"),
                Component.translatable(link.reason()),
                Component.translatable("screen.oritech_space_age.mission.completed_actions", telemetry.completed().size())));
    }

    private String category(MissionNetworking.FleetEntry entry) {

        return FleetViewPreferences.CATEGORIES.category(entry.id(), inSunOrbit(entry.telemetry().position()), entry.telemetry().plan().category());
    }

    private boolean shown(MissionNetworking.FleetEntry entry) {

        return FleetViewPreferences.showSun() || !inSunOrbit(entry.telemetry().position());
    }

    private boolean hidden(MissionNetworking.FleetEntry entry) {

        return FleetViewPreferences.collapsed(entry.id()) || !visible(entry);
    }

    private boolean visible(MissionNetworking.FleetEntry entry) {

        return shown(entry) && !FleetViewPreferences.CATEGORIES.collapsed(category(entry));
    }

    private FleetPresentation createFleetPresentation() {

        if (menu.fleetSnapshot == null) return null;
        var paths = new ArrayList<RocketFlightPathCalculator.CraftPath>();
        var branches = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        var aborts = new ArrayList<RocketFlightPathCalculator.NavigationAbortMoment>();
        var pathsByCraft = new HashMap<UUID, RocketFlightPathCalculator.CraftPath>();
        var times = new HashMap<UUID, Double>();
        var labels = new HashMap<UUID, Component>();
        var segmentNames = new HashMap<UUID, Map<SpaceSimulation.SegmentRef, String>>();
        var craftNumber = 0;
        for (var entry : menu.fleet) {
            var telemetry = entry.telemetry();
            craftNumber++;
            if (!shown(entry)) continue;
            var names = new HashMap<SpaceSimulation.SegmentRef, String>();
            var segments = SpaceSimulation.SegmentRef.displayOrder(telemetry.rocket());
            for (int index = 0; index < segments.size(); index++) {
                var segment = segments.get(index);
                var name = telemetry.plan().configurationFor(segment).name();
                names.put(segment, name.isBlank() ? "S" + (index + 1) : name);
            }
            segmentNames.put(entry.id(), names);
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
                new UUID(0, 0), menu.fleetSnapshot.objects(), new SpaceSimulation.FlightPlan(branches, List.of()),
                menu.fleetSnapshot.stations().stream()
                        .filter(station -> FleetViewPreferences.showSun() || !inSunOrbit(station.position())).toList());
        var flight = new RocketFlightPathCalculator.FlightPath(paths, List.of(), 0,
                List.of(), List.of(), aborts);
        return new FleetPresentation(snapshot, flight, pathsByCraft, times, labels, segmentNames);
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
            updateRow(row, entry, presentation.paths().get(entry.id()), 0);
        }

        map.updateFlightPath(presentation.snapshot(), presentation.flight(), null);
        map.setCoverage(menu.networkNodes);
        map.setCommandCoverage(menu.network.groundAntennas() == 0 ? -2 : menu.network.coverageAltitude());
        map.setFleetTimes(presentation.times());
        map.setCraftLabels(presentation.labels());
        map.setBranchSegmentNames(presentation.segmentNames());
    }

    private void advanceTelemetry() {

        if (map == null || Minecraft.getInstance().level == null) return;
        var elapsedTicks = Math.max(0, Minecraft.getInstance().level.getGameTime() - menu.receivedWorldTick)
                * menu.debugSpeed;
        var elapsedSeconds = elapsedTicks / 20.0;
        var times = new HashMap<UUID, Double>();
        for (var entry : menu.fleet) {
            if (!shown(entry)) continue;
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
        var lowSlots = SpaceBalance.slots(SpaceSimulation.OrbitBand.LOW);
        var highSlots = SpaceBalance.slots(SpaceSimulation.OrbitBand.HIGH);
        addNetworkIndicator(start, chipWidth, SpaceAgeBlocks.MISSION_CONTROL.get().asItem(),
                Component.translatable("screen.oritech_space_age.mission.ground_short", network.groundAntennas()), network.groundAntennas() > 0,
                List.of(Component.translatable("screen.oritech_space_age.mission.ground_antennas", network.groundAntennas()),
                        Component.translatable("screen.oritech_space_age.mission.ground_tooltip")));
        start += chipWidth + 3;
        addNetworkIndicator(start, chipWidth, SpaceAgeBlocks.ANTENNA.get().asItem(),
                Component.translatable("screen.oritech_space_age.mission.low_short", network.lowSlots(), lowSlots), network.lowComplete(),
                List.of(Component.translatable("screen.oritech_space_age.mission.low_ring", network.lowSlots(), lowSlots),
                        Component.translatable(network.lowComplete() ? "screen.oritech_space_age.mission.low_complete"
                                : "screen.oritech_space_age.mission.low_incomplete")));
        start += chipWidth + 3;
        addNetworkIndicator(start, chipWidth, SpaceAgeBlocks.ANTENNA.get().asItem(),
                Component.translatable("screen.oritech_space_age.mission.high_short", network.highSlots(), highSlots), network.highComplete(),
                List.of(Component.translatable("screen.oritech_space_age.mission.high_ring", network.highSlots(), highSlots),
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
            ClientPacketDistributor.sendToServer(MissionNetworking.FleetRequest.POLL);
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
                                     Map<UUID, Double> times, Map<UUID, Component> labels,
                                     Map<UUID, Map<SpaceSimulation.SegmentRef, String>> segmentNames) {

    }
}
