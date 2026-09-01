package rearth.oritech.spaceage.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.BlockPreviewWidget;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.RocketNetworking;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.RocketSimulationController;
import rearth.oritech.spaceage.simulation.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;

import java.util.*;

public class RocketAssemblerScreen extends OritechWidgetScreen<RocketAssemblerMenu> {

    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 285;
    private static final float SEGMENT_HIGHLIGHT = 0.25f;
    private static final float BLOCK_HIGHLIGHT = 0.5f;

    private Tab activeTab = Tab.ROCKET;
    private int previewRevision = -1;
    private int flightPlannerRevision = -1;
    private ScrollWidget flightPlanActionScroll;
    private float flightPlanActionScrollX;

    public RocketAssemblerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    protected void buildComponents() {
        previewRevision = menu.getPreviewRevision();
        flightPlannerRevision = menu.getFlightPlannerRevision();
        addComponent(new SurfaceWidget(0, 0, PANEL_WIDTH, PANEL_HEIGHT, OritechSurface.PANEL));

        var rocketTab = ButtonWidget.panel(9, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.rocket"), ignored -> switchTab(Tab.ROCKET));
        rocketTab.withDisabledSurface(OritechSurface.PANEL_PRESSED);
        rocketTab.setActive(activeTab != Tab.ROCKET);
        addComponent(rocketTab);

        var flightPlanTab = ButtonWidget.panel(9 + 92, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.flight_plan"), ignored -> switchTab(Tab.FLIGHT_PLAN));
        flightPlanTab.setActive(activeTab != Tab.FLIGHT_PLAN);
        flightPlanTab.withDisabledSurface(OritechSurface.PANEL_PRESSED);
        addComponent(flightPlanTab);

        if (activeTab == Tab.FLIGHT_PLAN) {
            buildFlightPlanTab();
        } else {
            buildRocketTab();
        }
    }

    private void buildRocketTab() {
        if (!menu.isPreviewLoaded()) {
            addMessagePanel(Component.translatable("screen.oritech_space_age.scanning"));
            return;
        }

        var rocket = menu.getRocket();
        if (rocket == null) {
            addMessagePanel(Component.translatable("screen.oritech_space_age.invalid_rocket"));
            return;
        }

        var preview = new RocketPreviewWidget(12, 39, 304, 232, rocket);
        preview.withSurface(OritechSurface.PANEL_INSET);
        preview.withPadding(Insets.of(3));
        preview.withRotationSpeed(0.18f);
        preview.withDragRotation();
        addComponent(preview);

        addComponent(new SurfaceWidget(323, 36, 145, 232 + 6, OritechSurface.PANEL_INSET));
        addComponent(new LabelWidget(334, 48, 126,
                Component.translatable("screen.oritech_space_age.general_stats").withStyle(ChatFormatting.BOLD)));

        var performance = RocketSimulationController.calculatePerformance(rocket);
        var blockCount = rocket.getStaticSegments().values().stream().mapToInt(segment -> segment.blocks().size()).sum();
        addStatLines(334, 68, 126, 15, List.of(
                stat("segments", rocket.getStaticSegments().size()),
                stat("blocks", blockCount),
                stat("wet_mass", format(performance.wetMassKilograms())),
                stat("engines", performance.engineCount()),
                stat("thrust", format(performance.thrustNewtons())),
                stat("burn_time", format(performance.availableBurnSeconds())),
                stat("delta_v", format(performance.availableDeltaVMetersPerSecond())),
                stat("acceleration", format(performance.liftoffAccelerationMetersPerSecondSquared()))
        ));

        var readiness = RocketSimulationController.getLaunchReadiness(rocket);
        if (readiness == RocketSimulationController.LaunchReadiness.READY) {
            var launchButton = ButtonWidget.orangePanel(339, 220, 110, 40,
                    Component.translatable("screen.oritech_space_age.launch").withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE), ignored -> launch());
            launchButton.withTextShadow(true);
            addComponent(launchButton);
        } else {
            var warningPanel = ButtonWidget.panel(334, 198, 126, 61, Component.empty(), ignored -> {
            });
            warningPanel.setActive(false);
            addComponent(warningPanel);

            addComponent(new LabelWidget(342, 205, 110,
                    Component.translatable("screen.oritech_space_age.launch_warning")
                            .withStyle(ChatFormatting.BOLD)));

            var warning = new LabelWidget(342, 220, 110, 32,
                    Component.translatable("screen.oritech_space_age.warning."
                            + readiness.name().toLowerCase(Locale.ROOT)));
            warning.withWrap(true);
            addComponent(warning);
        }
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

        var deltaV = RocketSimulationController.calculatePerformance(rocket).availableDeltaVMetersPerSecond();
        addComponent(new StarMapWidget(12, 39, 456, 145, snapshot, deltaV));
        addFlightPlanEditor(snapshot, rocket);
    }

    private void addFlightPlanEditor(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket) {
        if (flightPlanActionScroll != null) {
            flightPlanActionScrollX = flightPlanActionScroll.getScrollX();
        }
        var scroll = new ScrollWidget(12, 190, 456, 81)
                .withVerticalScroll(false)
                .withHorizontalScroll(true)
                .withScrollSpeed(24)
                .withDragScrolling(true);

        var actions = snapshot.actions();
        int cardWidth = 126;
        int cursorX = 4;
        for (int index = 0; index < actions.size(); index++) {
            var action = actions.get(index);
            int actionIndex = index;
            scroll.addChild(new SurfaceWidget(cursorX, 4, cardWidth - 8, 65, OritechSurface.PANEL_INSET));
            scroll.addChild(new LabelWidget(cursorX + 5, 8, 18,
                    Component.literal(Integer.toString(index + 1)).withStyle(ChatFormatting.BOLD)));

            scroll.addChild(ButtonWidget.darkPanel(cursorX + 22, 7, 84, 17,
                    actionName(action.type()), ignored -> cycleActionType(snapshot, rocket, actionIndex)));

            var parameter = actionParameter(action, snapshot, rocket);
            var parameterButton = ButtonWidget.panel(cursorX + 5, 29, 101, 18, parameter,
                    ignored -> cycleActionTarget(snapshot, rocket, actionIndex));
            parameterButton.setActive(action.type() == SpaceSimulation.ActionType.SET_NAVIGATION_TARGET
                    || action.type() == SpaceSimulation.ActionType.DISABLE_COUPLINGS
                    || action.type() == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE
                    || action.type() == SpaceSimulation.ActionType.WAIT_FOR_EVENT);
            scroll.addChild(parameterButton);

            scroll.addChild(ButtonWidget.darkPanel(cursorX + 5, 51, 18, 13, Component.literal("<"),
                    ignored -> moveAction(snapshot, rocket, actionIndex, -1)));
            scroll.addChild(ButtonWidget.darkPanel(cursorX + 25, 51, 18, 13, Component.literal(">"),
                    ignored -> moveAction(snapshot, rocket, actionIndex, 1)));
            var decrease = ButtonWidget.darkPanel(cursorX + 49, 51, 18, 13, Component.literal("−"),
                    ignored -> adjustActionValue(snapshot, rocket, actionIndex, -1));
            decrease.setActive(hasNumericValue(action.type()));
            scroll.addChild(decrease);
            var increase = ButtonWidget.darkPanel(cursorX + 69, 51, 18, 13, Component.literal("+"),
                    ignored -> adjustActionValue(snapshot, rocket, actionIndex, 1));
            increase.setActive(hasNumericValue(action.type()));
            scroll.addChild(increase);
            scroll.addChild(ButtonWidget.darkPanel(cursorX + 89, 51, 17, 13, Component.literal("×"),
                    ignored -> removeAction(snapshot, rocket, actionIndex)));

            cursorX += cardWidth;
        }

        int addX = cursorX + 2;
        scroll.addChild(ButtonWidget.orangePanel(addX, 20, 58, 32,
                Component.translatable("screen.oritech_space_age.action.add"),
                ignored -> addAction(snapshot, rocket)));
        scroll.setContentDimensions(addX + 66, 69);
        scroll.setScrollPosition(flightPlanActionScrollX, 0);
        flightPlanActionScroll = scroll;
        addComponent(scroll);
    }

    private void addAction(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket) {
        var updated = new ArrayList<>(snapshot.actions());
        updated.add(SpaceSimulation.FlightPlanAction.create(SpaceSimulation.ActionType.START_ENGINE_BURN));
        updateFlightPlan(rocket, updated);
    }

    private void removeAction(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket, int index) {
        var updated = new ArrayList<>(snapshot.actions());
        updated.remove(index);
        updateFlightPlan(rocket, updated);
    }

    private void moveAction(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket,
                            int index, int direction) {
        int target = index + direction;
        if (target < 0 || target >= snapshot.actions().size()) return;
        var updated = new ArrayList<>(snapshot.actions());
        Collections.swap(updated, index, target);
        updateFlightPlan(rocket, updated);
    }

    private void cycleActionType(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket, int index) {
        var updated = new ArrayList<>(snapshot.actions());
        var current = updated.get(index);
        var values = SpaceSimulation.ActionType.values();
        var type = values[(current.type().ordinal() + 1) % values.length];
        var changed = current.withType(type);
        if (type == SpaceSimulation.ActionType.SET_NAVIGATION_TARGET && !snapshot.objects().isEmpty()) {
            changed = changed.withTarget(snapshot.objects().getFirst().id());
        } else if (type == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE) {
            changed = changed.withTarget(SpaceSimulation.FlightPlanAction.CURRENT_TARGET);
        } else if (type == SpaceSimulation.ActionType.DISABLE_COUPLINGS && !rocket.getStaticSegments().isEmpty()) {
            changed = changed.withTarget(rocket.getStaticSegments().keySet().iterator().next());
        }
        updated.set(index, changed);
        updateFlightPlan(rocket, updated);
    }

    private void cycleActionTarget(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket, int index) {
        var updated = new ArrayList<>(snapshot.actions());
        var action = updated.get(index);
        List<UUID> targets = switch (action.type()) {
            case SET_NAVIGATION_TARGET -> snapshot.objects().stream()
                    .map(SpaceSimulation.SpaceObjectData::id).toList();
            case WAIT_UNTIL_DISTANCE -> List.of(
                    SpaceSimulation.FlightPlanAction.CURRENT_TARGET,
                    SpaceObjects.EARTH_ID);
            case DISABLE_COUPLINGS -> new ArrayList<>(rocket.getStaticSegments().keySet());
            default -> List.of();
        };
        if (action.type() == SpaceSimulation.ActionType.WAIT_FOR_EVENT) {
            var events = SpaceSimulation.WaitEvent.values();
            updated.set(index, action.withValue((action.value() + 1) % events.length));
            updateFlightPlan(rocket, updated);
            return;
        }
        if (targets.isEmpty()) return;
        int current = targets.indexOf(action.targetId());
        updated.set(index, action.withTarget(targets.get((current + 1) % targets.size())));
        updateFlightPlan(rocket, updated);
    }

    private void adjustActionValue(SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket,
                                   int index, int direction) {
        var updated = new ArrayList<>(snapshot.actions());
        var action = updated.get(index);
        long step = switch (action.type()) {
            case WAIT_TICKS -> 20;
            case WAIT_SECONDS -> action.value() < 60 ? 1 : action.value() < 600 ? 10 : 60;
            case WAIT_UNTIL_DISTANCE -> action.value() < 10_000 ? 1_000
                    : action.value() < 100_000 ? 10_000
                    : action.value() < 1_000_000 ? 100_000 : 1_000_000;
            default -> 0;
        };
        if (step == 0) return;
        long minimum = action.type() == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE ? 0 : 1;
        updated.set(index, action.withValue(Math.max(minimum, action.value() + step * direction)));
        updateFlightPlan(rocket, updated);
    }

    private static boolean hasNumericValue(SpaceSimulation.ActionType type) {
        return type == SpaceSimulation.ActionType.WAIT_TICKS
                || type == SpaceSimulation.ActionType.WAIT_SECONDS
                || type == SpaceSimulation.ActionType.WAIT_UNTIL_DISTANCE;
    }

    private void updateFlightPlan(ActiveRocketData rocket, List<SpaceSimulation.FlightPlanAction> actions) {
        var current = menu.getFlightPlannerSnapshot();
        if (current != null && current.rocketId().equals(rocket.getRocketId())) {
            menu.setFlightPlannerSnapshot(new SpaceSimulation.FlightPlannerSnapshot(
                    current.simulationId(), current.rocketId(), current.objects(), List.copyOf(actions)));
        }
        ClientPacketDistributor.sendToServer(new RocketNetworking.UpdateFlightPlanPayload(
                menu.blockPos, rocket.getRocketId(), actions));
    }

    private Component actionName(SpaceSimulation.ActionType type) {
        return Component.translatable("screen.oritech_space_age.action." + type.name().toLowerCase(Locale.ROOT));
    }

    private Component actionParameter(SpaceSimulation.FlightPlanAction action,
                                      SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket) {
        if (action.type() == SpaceSimulation.ActionType.SET_NAVIGATION_TARGET) {
            return snapshot.objects().stream().filter(object -> object.id().equals(action.targetId()))
                    .findFirst().map(object -> objectName(object.type()))
                    .orElse(Component.translatable("screen.oritech_space_age.action.no_target"));
        }
        if (action.type() == SpaceSimulation.ActionType.DISABLE_COUPLINGS) {
            var ids = new ArrayList<>(rocket.getStaticSegments().keySet());
            int index = ids.indexOf(action.targetId());
            return index < 0 ? Component.translatable("screen.oritech_space_age.action.no_segment")
                    : Component.translatable("screen.oritech_space_age.action.segment", index + 1);
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
                    : action.targetId().equals(SpaceObjects.EARTH_ID)
                    ? Component.translatable("screen.oritech_space_age.action.start_position").getString()
                    : "?";
            return Component.translatable("screen.oritech_space_age.action.distance", target, compact(action.value()));
        }
        if (action.type() == SpaceSimulation.ActionType.WAIT_FOR_EVENT) {
            var events = SpaceSimulation.WaitEvent.values();
            int index = (int) Math.clamp(action.value(), 0, events.length - 1);
            return Component.translatable("screen.oritech_space_age.event."
                    + events[index].name().toLowerCase(Locale.ROOT));
        }
        return Component.translatable("screen.oritech_space_age.action.no_parameter");
    }

    private static String compact(long value) {
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000d);
        if (value >= 1_000) return String.format(Locale.ROOT, "%.1fk", value / 1_000d);
        return Long.toString(value);
    }

    private static Component objectName(SpaceObjects.ObjectType type) {
        return Component.translatable("screen.oritech_space_age.object." + type.name().toLowerCase(Locale.ROOT));
    }

    private void addMessagePanel(Component message) {
        addComponent(new SurfaceWidget(12, 39, 456, 232, OritechSurface.PANEL_INSET));
        var label = new LabelWidget(32, 142, 416, 30, message);
        label.withAlignment(LabelWidget.Alignment.CENTER).withBrightColor().withWrap(true);
        addComponent(label);
    }

    private List<LabelWidget> addStatLines(int x, int y, int width, int spacing, List<Component> lines) {
        var labels = new ArrayList<LabelWidget>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            var label = new LabelWidget(x, y + i * spacing, width, lines.get(i));
            labels.add(label);
            addComponent(label);
        }
        return labels;
    }

    private void switchTab(Tab tab) {
        if (activeTab == tab) return;
        activeTab = tab;
        if (tab == Tab.FLIGHT_PLAN) {
            ClientPacketDistributor.sendToServer(new RocketNetworking.RequestFlightPlannerPayload(menu.blockPos));
        }
        rebuildComponents();
    }

    private void launch() {
        ClientPacketDistributor.sendToServer(new RocketNetworking.LaunchRocketPayload(menu.blockPos));
    }

    private List<Component> getSegmentStats(UUID segmentId, ActiveRocketData rocket) {
        var staticSegment = rocket.getStaticSegments().get(segmentId);
        var dynamicSegment = rocket.getDynamicSegments().get(segmentId);
        if (staticSegment == null || dynamicSegment == null) return List.of();

        var segmentRocket = new ActiveRocketData(
                Map.of(segmentId, staticSegment),
                Map.of(segmentId, dynamicSegment));
        var performance = RocketSimulationController.calculatePerformance(segmentRocket);
        return List.of(
                Component.translatable("screen.oritech_space_age.segment_stats",
                        segmentId.toString().substring(0, 8)).withStyle(ChatFormatting.BOLD),
                stat("blocks", staticSegment.blocks().size()),
                stat("dry_mass", format(performance.dryMassKilograms())),
                stat("fuel_mass", format(performance.fuelMassKilograms())),
                stat("energy", format(dynamicSegment.availableRF)),
                stat("engines", staticSegment.engineCount()),
                stat("burn_time", format(performance.availableBurnSeconds()))
        );
    }

    private static Component stat(String name, Object value) {
        return Component.translatable("screen.oritech_space_age.stat." + name, value);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        components.forEach(component -> component.tick());
        if (flightPlanActionScroll != null) {
            flightPlanActionScrollX = flightPlanActionScroll.getScrollX();
        }

        if (previewRevision != menu.getPreviewRevision()) {
            rebuildComponents();
        } else if (activeTab == Tab.FLIGHT_PLAN && flightPlannerRevision != menu.getFlightPlannerRevision()) {
            rebuildComponents();
        }
    }

    @Override
    public BlockState getTitleState() {
        return SpaceAgeBlocks.ROCKET_ASSEMBLER.get().defaultBlockState();
    }

    private enum Tab {
        ROCKET,
        FLIGHT_PLAN
    }

    private static final class StarMapWidget extends UIComponent {

        private static final int CONTENT_HEIGHT = 550;
        private static final DistanceBand[] DISTANCE_BANDS = {
                new DistanceBand(100_000, 130),
                new DistanceBand(1_000_000, 90),
                new DistanceBand(10_000_000, 120),
                new DistanceBand(25_000_000, 90),
                new DistanceBand(100_000_000, 80)
        };
        private static final OrbitMarker[] ORBIT_MARKERS = {
                new OrbitMarker(1_000, "low_orbit"),
                new OrbitMarker(20_000, "medium_orbit"),
                new OrbitMarker(40_000, "high_orbit"),
                new OrbitMarker(100_000, "outer_space"),
                new OrbitMarker(1_000_000, "deep_space"),
                new OrbitMarker(10_000_000, "inner_system"),
                new OrbitMarker(20_000_000, "asteroid_belt")
        };

        private final List<MapObject> objects = new ArrayList<>();
        private final List<TrajectoryLeg> trajectory = new ArrayList<>();
        private final ItemWidget rocketMarker;
        private float scrollY;
        private float renderedScrollY;
        private boolean dragging;
        private MapObject hovered;

        private StarMapWidget(int x, int y, int width, int height,
                              SpaceSimulation.FlightPlannerSnapshot snapshot, double availableDeltaV) {
            super(x, y, width, height);
            this.surface = OritechSurface.PANEL_INSET;

            var horizontalRanges = new HashMap<Integer, float[]>();
            for (var object : snapshot.objects()) {
                horizontalRanges.compute(distanceBand(object.y()), (ignored, range) -> {
                    if (range == null) return new float[]{object.x(), object.x()};
                    range[0] = Math.min(range[0], object.x());
                    range[1] = Math.max(range[1], object.x());
                    return range;
                });
            }

            for (var object : snapshot.objects()) {
                int objectSize = object.type() == SpaceObjects.ObjectType.ASTEROID ? 14 : 22;
                var range = horizontalRanges.get(distanceBand(object.y()));
                float normalizedX = range[1] > range[0]
                        ? (object.x() - range[0]) / (range[1] - range[0]) - 0.5f
                        : 0;
                int objectX = width / 2 - objectSize / 2
                        + Math.round(normalizedX * (width - 68));
                int objectY = contentBottom() - distancePixels(object.y()) - objectSize / 2;
                var widget = new BlockWidget(objectX, objectY, objectSize,
                        placeholderBlock(object.type()));
                objects.add(new MapObject(object, widget, objectX + widget.getWidth() / 2, objectY + widget.getHeight() / 2));
            }

            var earth = objects.stream().filter(object -> object.data.type() == SpaceObjects.ObjectType.EARTH)
                    .findFirst().orElse(objects.isEmpty() ? null : objects.getFirst());
            var current = earth;
            double remaining = availableDeltaV;
            for (var action : snapshot.actions()) {
                if (action.type() != SpaceSimulation.ActionType.SET_NAVIGATION_TARGET || current == null) continue;
                var target = objects.stream().filter(object -> object.data.id().equals(action.targetId())).findFirst().orElse(null);
                if (target == null) continue;
                double distance = Math.hypot(target.data.x() - current.data.x(), target.data.y() - current.data.y());
                double cost = 500 + Math.sqrt(distance) * 2;
                boolean reachable = remaining >= cost;
                trajectory.add(new TrajectoryLeg(current.centerX, current.centerY, target.centerX, target.centerY, reachable));
                if (!reachable) break;
                remaining -= cost;
                current = target;
            }
            int markerX = current == null ? width / 2 : current.centerX - 6;
            int markerY = current == null ? contentBottom() - 18 : current.centerY - 18;
            rocketMarker = new ItemWidget(markerX, markerY, 12, new ItemStack(Items.FIREWORK_ROCKET));
            rocketMarker.withShowOverlay(false).withTooltipFromStack(false);

            scrollY = maxScroll();
            renderedScrollY = scrollY;
        }

        private int maxScroll() {
            return Math.max(0, CONTENT_HEIGHT - (height - 10));
        }

        private static int contentBottom() {
            return CONTENT_HEIGHT - 24;
        }

        private static int distancePixels(double distance) {
            double remaining = Math.max(0, distance);
            double previousLimit = 0;
            int pixels = 0;
            for (var band : DISTANCE_BANDS) {
                double bandDistance = band.maxDistance - previousLimit;
                if (remaining <= bandDistance) {
                    return pixels + (int) Math.round(remaining / bandDistance * band.pixels);
                }
                remaining -= bandDistance;
                previousLimit = band.maxDistance;
                pixels += band.pixels;
            }
            return pixels;
        }

        private static int distanceBand(double distance) {
            for (int index = 0; index < DISTANCE_BANDS.length; index++) {
                if (distance <= DISTANCE_BANDS[index].maxDistance) return index;
            }
            return DISTANCE_BANDS.length;
        }

        private static BlockState placeholderBlock(SpaceObjects.ObjectType type) {
            return switch (type) {
                case EARTH -> Blocks.GRASS_BLOCK.defaultBlockState();
                case SUN -> Blocks.GOLD_BLOCK.defaultBlockState();
                case MARS -> Blocks.RED_SAND.defaultBlockState();
                case ASTEROID -> Blocks.IRON_ORE.defaultBlockState();
            };
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xFF080D18);
            renderedScrollY += (scrollY - renderedScrollY) * 0.22f;

            int viewportX = x + 5;
            int viewportY = y + 5;
            int viewportWidth = width - 10;
            int viewportHeight = height - 10;
            float localMouseX = mouseX - viewportX;
            float localMouseY = mouseY - viewportY + renderedScrollY;

            graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(viewportX, viewportY - renderedScrollY);

            var font = Minecraft.getInstance().font;
            for (var marker : ORBIT_MARKERS) {
                int markerY = contentBottom() - distancePixels(marker.distance);
                drawDashedLine(graphics, 5, markerY, viewportWidth - 8, 0xFF354657);
                graphics.text(font, Component.translatable("screen.oritech_space_age.orbit." + marker.translation),
                        8, markerY - 10, 0xFF71879A, false);
            }
            for (var leg : trajectory) {
                drawLine(graphics, leg.fromX, leg.fromY, leg.toX, leg.toY,
                        leg.reachable ? 0xFFFF8A20 : 0xFF5A6068);
            }
            hovered = null;
            for (var object : objects) {
                object.widget.render(graphics, (int) localMouseX, (int) localMouseY, delta);
                if (isMouseOver(mouseX, mouseY) && object.widget.isMouseOver(localMouseX, localMouseY)) hovered = object;
            }
            rocketMarker.render(graphics, (int) localMouseX, (int) localMouseY, delta);
            pose.popMatrix();
            graphics.disableScissor();

            graphics.fill(x + 6, y + 6, x + width - 7, y + 21, 0xD8080D18);
            graphics.text(font, Component.translatable("screen.oritech_space_age.star_system"),
                    x + 10, y + 9, 0xFFCAD8E5, true);
            renderScrollbar(graphics, viewportX + viewportWidth - 2, viewportY, viewportHeight);
        }

        private void renderScrollbar(GuiGraphicsExtractor graphics, int barX, int barY, int trackHeight) {
            int thumbHeight = Math.max(12, trackHeight * trackHeight / CONTENT_HEIGHT);
            int thumbY = barY + Math.round((trackHeight - thumbHeight) * renderedScrollY / Math.max(1, maxScroll()));
            graphics.fill(barX, barY, barX + 2, barY + trackHeight, 0x443B4652);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, 0xCC8192A3);
        }

        private static void drawDashedLine(GuiGraphicsExtractor graphics, int fromX, int y, int toX, int color) {
            for (int lineX = fromX; lineX < toX; lineX += 8) {
                graphics.fill(lineX, y, Math.min(lineX + 4, toX), y + 1, color);
            }
        }

        private static void drawLine(GuiGraphicsExtractor graphics, int fromX, int fromY, int toX, int toY, int color) {
            int steps = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
            for (int i = 0; i <= steps; i++) {
                int px = fromX + (toX - fromX) * i / Math.max(1, steps);
                int py = fromY + (toY - fromY) * i / Math.max(1, steps);
                graphics.fill(px, py, px + 2, py + 2, color);
            }
        }

        @Override
        public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
            if (!isMouseOver(mouseX, mouseY)) return false;
            scrollY = Math.clamp(scrollY - (float) scrollDelta * 28, 0, maxScroll());
            return true;
        }

        @Override
        public boolean handleClick(double mouseX, double mouseY, int button) {
            if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
            dragging = true;
            return true;
        }

        @Override
        public boolean handleDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
            if (!dragging || button != 0) return false;
            scrollY = Math.clamp(scrollY - (float) deltaY, 0, maxScroll());
            return true;
        }

        @Override
        public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
            if (button != 0 || !dragging) return false;
            dragging = false;
            return true;
        }

        @Override
        public boolean hasTooltip() {
            return hovered != null;
        }

        @Override
        public List<Component> getTooltip() {
            if (hovered == null) return List.of();
            return List.of(objectName(hovered.data.type()).copy().withStyle(ChatFormatting.BOLD),
                    Component.translatable("screen.oritech_space_age.object.position",
                            format(hovered.data.x()), format(hovered.data.y())),
                    Component.translatable("screen.oritech_space_age.object.detection",
                            hovered.data.detectionState().name().toLowerCase(Locale.ROOT)));
        }

        private record MapObject(SpaceSimulation.SpaceObjectData data, BlockWidget widget,
                                 int centerX, int centerY) {
        }

        private record TrajectoryLeg(int fromX, int fromY, int toX, int toY, boolean reachable) {
        }

        private record DistanceBand(double maxDistance, int pixels) {
        }

        private record OrbitMarker(double distance, String translation) {
        }
    }

    private final class RocketPreviewWidget extends BlockPreviewWidget {

        private final ActiveRocketData rocket;
        private final Map<BlockPos, UUID> segmentsByPosition = new HashMap<>();
        private final Map<UUID, Set<BlockPos>> positionsBySegment = new HashMap<>();

        private RocketPreviewWidget(int x, int y, int width, int height, ActiveRocketData rocket) {
            super(x, y, width, height);
            this.rocket = rocket;

            var addedPositions = new HashSet<BlockPos>();
            for (var entry : rocket.getStaticSegments().entrySet()) {
                var segmentId = entry.getKey();
                var segment = entry.getValue();
                var segmentPositions = positionsBySegment.computeIfAbsent(segmentId, ignored -> new HashSet<>());

                for (var block : segment.blocks()) {
                    addPreviewBlock(block.relativePos(), block.state(), segmentId, segmentPositions, addedPositions);
                }

                for (var couplings : segment.originalCouplings().values()) {
                    for (StaticRocketSegment.CouplingData coupling : couplings) {
                        addPreviewBlock(coupling.relativePos(), SpaceAgeBlocks.ROCKET_COUPLING.get().defaultBlockState(),
                                segmentId, segmentPositions, addedPositions);
                    }
                }
            }
        }

        private void addPreviewBlock(BlockPos position, BlockState state, UUID segmentId,
                                     Set<BlockPos> segmentPositions, Set<BlockPos> addedPositions) {
            var immutablePosition = position.immutable();
            segmentPositions.add(immutablePosition);
            segmentsByPosition.putIfAbsent(immutablePosition, segmentId);
            if (addedPositions.add(immutablePosition)) {
                addBlock(state, null, immutablePosition);
            }
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.renderContent(graphics, mouseX, mouseY, delta);

            var hovered = getHoveredBlock();
            var hoveredSegment = hovered == null ? null : segmentsByPosition.get(asBlockPos(hovered.offset()));
            if (hoveredSegment != null) {
                renderTooltipPanel(graphics, mouseX - x, mouseY - y, getSegmentStats(hoveredSegment, rocket));
            }
        }

        @Override
        protected int getOverlayCoords(BlockEntry entry) {
            var hovered = getHoveredBlock();
            if (hovered == null) return OverlayTexture.NO_OVERLAY;

            var segmentId = segmentsByPosition.get(asBlockPos(hovered.offset()));
            if (segmentId == null) return OverlayTexture.NO_OVERLAY;

            if (entry.offset().equals(hovered.offset())) {
                return OverlayTexture.pack(BLOCK_HIGHLIGHT, false);
            }
            if (positionsBySegment.getOrDefault(segmentId, Set.of()).contains(asBlockPos(entry.offset()))) {
                return OverlayTexture.pack(SEGMENT_HIGHLIGHT, false);
            }
            return OverlayTexture.NO_OVERLAY;
        }

        private void renderTooltipPanel(GuiGraphicsExtractor graphics, int blockX, int blockY,
                                        List<Component> tooltip) {
            if (tooltip.isEmpty()) return;

            var font = Minecraft.getInstance().font;
            int maxWidth = tooltip.stream().mapToInt(font::width).max().orElse(0);
            int panelWidth = maxWidth + 12;
            int panelHeight = tooltip.size() * font.lineHeight + 10;
            int tooltipX = Math.max(5, Math.min(width - panelWidth - 5, blockX + 12));
            int tooltipY = Math.max(5, Math.min(height - panelHeight - 5, blockY - panelHeight - 8));

            graphics.nextStratum();
            graphics.fill(tooltipX, tooltipY, tooltipX + panelWidth, tooltipY + panelHeight, 0xE0101418);
            graphics.fill(tooltipX, tooltipY, tooltipX + panelWidth, tooltipY + 1, 0xFF9DB4C7);
            graphics.fill(tooltipX, tooltipY + panelHeight - 1, tooltipX + panelWidth, tooltipY + panelHeight, 0xFF9DB4C7);
            graphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + panelHeight, 0xFF9DB4C7);
            graphics.fill(tooltipX + panelWidth - 1, tooltipY, tooltipX + panelWidth, tooltipY + panelHeight, 0xFF9DB4C7);

            for (int index = 0; index < tooltip.size(); index++) {
                graphics.text(font, tooltip.get(index), tooltipX + 6,
                        tooltipY + 5 + index * font.lineHeight, 0xFFF2F6FA, false);
            }
        }

        private BlockPos asBlockPos(Vec3i position) {
            return new BlockPos(position.getX(), position.getY(), position.getZ());
        }
    }
}
