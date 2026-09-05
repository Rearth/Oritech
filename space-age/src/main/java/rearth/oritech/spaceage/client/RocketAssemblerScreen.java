package rearth.oritech.spaceage.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.BlockPreviewWidget;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.RocketNetworking;
import rearth.oritech.spaceage.simulation.ActiveRocketData;
import rearth.oritech.spaceage.simulation.RocketPerformanceCalculator;
import rearth.oritech.spaceage.simulation.RocketFlightPlanRules;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.StaticRocketSegment;

import java.util.*;

/** Shows the assembled rocket. The flight planner uses a separate screen class to keep both screens manageable. */
public class RocketAssemblerScreen extends OritechWidgetScreen<RocketAssemblerMenu> {

    private static final int WINDOW_PADDING = 6;
    private static final float SEGMENT_HIGHLIGHT = 0.25f;
    private static final float BLOCK_HIGHLIGHT = 0.5f;

    private final Inventory screenInventory;
    private final Component screenTitle;
    private int previewRevision = -1;
    private int flightPlannerRevision = -1;
    private int panelWidth;
    private int panelHeight;
    private boolean requestedFlightPlanner;
    private UUID selectedSegment;
    private UUID pendingSegment;
    private boolean segmentStagesExpanded;
    private EditBox segmentNameField;
    private RocketPreviewWidget rocketPreview;
    private BlockPreviewWidget.ViewState previewViewState;

    public RocketAssemblerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 0, 0);
        this.screenInventory = inventory;
        this.screenTitle = title;
    }

    @Override
    protected void buildComponents() {
        segmentNameField = null;
        panelWidth = width - WINDOW_PADDING * 2;
        panelHeight = height - WINDOW_PADDING * 2;
        setPanelSize(panelWidth, panelHeight);
        previewRevision = menu.getPreviewRevision();
        flightPlannerRevision = menu.getFlightPlannerRevision();
        if (!requestedFlightPlanner && menu.getFlightPlannerSnapshot() == null) {
            requestedFlightPlanner = true;
            ClientPacketDistributor.sendToServer(new RocketNetworking.RequestFlightPlannerPayload(menu.blockPos));
        }
        addComponent(new SurfaceWidget(0, 0, panelWidth, panelHeight, OritechSurface.PANEL));

        var rocketTab = ButtonWidget.panel(9, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.rocket"), ignored -> {});
        rocketTab.withDisabledSurface(OritechSurface.PANEL_PRESSED);
        rocketTab.setActive(false);
        addComponent(rocketTab);

        var flightPlanTab = ButtonWidget.panel(101, 9, 92, 20,
                Component.translatable("screen.oritech_space_age.flight_plan"), ignored -> openFlightPlanner());
        flightPlanTab.withDisabledSurface(OritechSurface.PANEL_PRESSED);
        addComponent(flightPlanTab);

        buildRocketPanel();
        if (selectedSegment != null) addSegmentConfigurationPopup();
    }

    private void buildRocketPanel() {
        if (!menu.isPreviewLoaded()) {
            addMessagePanel(Component.translatable("screen.oritech_space_age.scanning"));
            return;
        }

        var rocket = menu.getRocket();
        if (rocket == null) {
            addMessagePanel(Component.translatable("screen.oritech_space_age.invalid_rocket"));
            return;
        }

        int contentTop = 39;
        int contentHeight = panelHeight - contentTop - 12;
        int statsWidth = Math.clamp(panelWidth / 3, 130, 240);
        int statsX = panelWidth - statsWidth - 12;
        int previewWidth = statsX - 19;

        var preview = new RocketPreviewWidget(12, contentTop, previewWidth, contentHeight, rocket);
        preview.withSurface(OritechSurface.PANEL_INSET);
        preview.withPadding(Insets.of(3));
        preview.withRotationSpeed(0.18f);
        preview.withDragRotation();
        preview.withViewState(previewViewState);
        rocketPreview = preview;
        addComponent(preview);

        addComponent(new SurfaceWidget(statsX, contentTop, statsWidth, contentHeight, OritechSurface.PANEL_INSET));
        addComponent(new LabelWidget(statsX + 11, contentTop + 12, statsWidth - 19,
                Component.translatable("screen.oritech_space_age.general_stats").withStyle(ChatFormatting.BOLD)));

        var performance = RocketPerformanceCalculator.calculate(rocket);
        var blockCount = rocket.getStaticSegments().values().stream().mapToInt(segment -> segment.blocks().size()).sum();
        int statsSpacing = Math.clamp((contentHeight - 115) / 8, 9, 15);
        addStatLines(statsX + 11, contentTop + 32, statsWidth - 19, statsSpacing, List.of(
                stat("segments", rocket.getStaticSegments().size()),
                stat("stages", RocketFlightPlanRules.stageCount(currentPlan(), rocket.getStaticSegments().size())),
                stat("blocks", blockCount),
                stat("wet_mass", format(performance.wetMassKilograms())),
                stat("engines", performance.engineCount()),
                stat("thrust", format(performance.thrustNewtons())),
                stat("burn_time", format(performance.availableBurnSeconds())),
                stat("delta_v", format(performance.availableDeltaVMetersPerSecond())),
                stat("acceleration", format(performance.liftoffAccelerationMetersPerSecondSquared()))
        ));

        var readiness = RocketPerformanceCalculator.getLaunchReadiness(rocket);
        if (readiness == RocketPerformanceCalculator.LaunchReadiness.READY) {
            var launchButton = ButtonWidget.orangePanel(statsX + 16, contentTop + contentHeight - 55,
                    statsWidth - 32, 40,
                    Component.translatable("screen.oritech_space_age.launch").withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE),
                    ignored -> launch());
            launchButton.withTextShadow(true);
            addComponent(launchButton);
        } else {
            int warningX = statsX + 11;
            int warningY = contentTop + contentHeight - 74;
            var warningPanel = ButtonWidget.panel(warningX, warningY, statsWidth - 22, 61,
                    Component.empty(), ignored -> {});
            warningPanel.setActive(false);
            addComponent(warningPanel);
            addComponent(new LabelWidget(warningX + 8, warningY + 7, statsWidth - 38,
                    Component.translatable("screen.oritech_space_age.launch_warning").withStyle(ChatFormatting.BOLD)));
            var warning = new LabelWidget(warningX + 8, warningY + 22, statsWidth - 38, 32,
                    Component.translatable("screen.oritech_space_age.warning."
                            + readiness.name().toLowerCase(Locale.ROOT)));
            warning.withWrap(true);
            addComponent(warning);
        }
    }

    private void openFlightPlanner() {
        if (menu.getFlightPlannerSnapshot() == null) {
            ClientPacketDistributor.sendToServer(new RocketNetworking.RequestFlightPlannerPayload(menu.blockPos));
        }
        Minecraft.getInstance().setScreen(new RocketFlightPlannerScreen(menu, screenInventory, screenTitle));
    }

    private void addSegmentConfigurationPopup() {
        var rocket = menu.getRocket();
        if (rocket == null || menu.getFlightPlannerSnapshot() == null) return;
        var segment = rocket.getStaticSegments().get(selectedSegment);
        if (segment == null) {
            selectedSegment = null;
            return;
        }

        var ref = SpaceSimulation.SegmentRef.of(segment);
        var configuration = currentPlan().configurationFor(ref);
        int popupWidth = Math.min(330, panelWidth - 36);
        var segmentRefs = rocket.getStaticSegments().values().stream()
                .map(SpaceSimulation.SegmentRef::of).toList();
        int stageCount = RocketFlightPlanRules.editableStageCount(currentPlan(), segmentRefs);
        int stageRows = Math.max(1, (stageCount + 4) / 5);
        int popupHeight = segmentStagesExpanded ? 143 + stageRows * 23 : 119;
        int popupX = (panelWidth - popupWidth) / 2;
        int popupY = Math.max(34, (panelHeight - popupHeight) / 2);

        addComponent(new SegmentPopupPanel(panelWidth, panelHeight, popupX, popupY, popupWidth, popupHeight,
                this::closeSegmentConfiguration));
        addPopupComponent(new LabelWidget(popupX + 10, popupY + 9, popupWidth - 20, 14,
                Component.translatable("screen.oritech_space_age.segment_configuration").withStyle(ChatFormatting.BOLD)));
        addPopupComponent(ButtonWidget.darkPanel(popupX + popupWidth - 27, popupY + 7, 18, 16,
                Component.literal("×"), ignored -> closeSegmentConfiguration()));

        segmentNameField = addRenderableWidget(new EditBox(font, leftPos + popupX + 56, topPos + popupY + 27,
                popupWidth - 67, 18, Component.translatable("screen.oritech_space_age.segment_name")));
        segmentNameField.setMaxLength(RocketFlightPlanRules.MAX_SEGMENT_NAME_LENGTH);
        segmentNameField.setValue(configuration.name().isBlank() ? defaultSegmentName(ref, rocket) : configuration.name());
        // Popup buttons rebuild the screen. Always read the latest draft here so an older captured configuration
        // cannot overwrite a name which was typed since the popup was opened.
        segmentNameField.setResponder(name -> updateSegmentConfiguration(
                currentPlan().configurationFor(ref).withName(name), false));
        addPopupComponent(new LabelWidget(popupX + 10, popupY + 31, 42, 12,
                Component.translatable("screen.oritech_space_age.name")));

        int presetY = popupY + 51;
        addPopupComponent(ButtonWidget.darkPanel(popupX + 10, presetY, 64, 18,
                Component.translatable("screen.oritech_space_age.segment_preset.core"), ignored -> setSegmentNamePreset("Core")));
        addPopupComponent(ButtonWidget.darkPanel(popupX + 78, presetY, 76, 18,
                Component.translatable("screen.oritech_space_age.segment_preset.booster"), ignored -> setSegmentNamePreset("Booster")));
        addPopupComponent(ButtonWidget.darkPanel(popupX + 158, presetY, 70, 18,
                Component.translatable("screen.oritech_space_age.segment_preset.payload"), ignored -> setSegmentNamePreset("Payload")));

        addPopupComponent(ButtonWidget.panel(popupX + 10, popupY + 75, 106, 20,
                checkboxLabel("screen.oritech_space_age.booster", configuration.booster()),
                ignored -> {
                    var current = currentPlan().configurationFor(ref);
                    updateSegmentConfiguration(current.withBooster(!current.booster()), true);
                }));
        addPopupComponent(ButtonWidget.panel(popupX + 122, popupY + 75, popupWidth - 132, 20,
                Component.translatable(segmentStagesExpanded
                        ? "screen.oritech_space_age.engine_stages.collapse"
                        : "screen.oritech_space_age.engine_stages.expand"), ignored -> {
                    segmentStagesExpanded = !segmentStagesExpanded;
                    rebuildComponents();
                }));

        if (segmentStagesExpanded) {
            addPopupComponent(new LabelWidget(popupX + 10, popupY + 103, popupWidth - 20, 12,
                    Component.translatable("screen.oritech_space_age.engine_stages")));
            boolean hasEngines = segment.engineCount() > 0;
            for (int stage = 1; stage <= stageCount; stage++) {
                int currentStage = stage;
                int stageX = popupX + 10 + ((stage - 1) % 5) * 61;
                int stageY = popupY + 117 + ((stage - 1) / 5) * 23;
                var stageButton = ButtonWidget.darkPanel(stageX, stageY, 56, 18,
                        checkboxLabel("screen.oritech_space_age.stage_short", configuration.usesEnginesDuring(stage), stage),
                        ignored -> toggleEngineStage(ref, currentStage));
                stageButton.setActive(hasEngines);
                if (!hasEngines) stageButton.withTooltip(Component.translatable("screen.oritech_space_age.segment_no_engines"));
                addPopupComponent(stageButton);
            }
        }
    }

    private void addPopupComponent(UIComponent component) {
        component.withZIndex(9_001);
        addComponent(component);
    }

    private Component checkboxLabel(String key, boolean checked, Object... arguments) {
        return Component.literal(checked ? "☑ " : "☐ ").append(Component.translatable(key, arguments));
    }

    private void setSegmentNamePreset(String name) {
        if (segmentNameField != null) segmentNameField.setValue(name);
    }

    private void toggleEngineStage(SpaceSimulation.SegmentRef segment, int stage) {
        var configuration = currentPlan().configurationFor(segment);
        var stages = new ArrayList<>(configuration.engineStages());
        if (!stages.remove(Integer.valueOf(stage))) stages.add(stage);
        stages.sort(Integer::compareTo);
        updateSegmentConfiguration(configuration.withEngineStages(stages), true);
    }

    private void updateSegmentConfiguration(SpaceSimulation.SegmentConfiguration configuration, boolean rebuild) {
        var plan = currentPlan();
        var configurations = new ArrayList<>(plan.segmentConfigurations());
        configurations.removeIf(existing -> existing.segment().equals(configuration.segment()));
        configurations.add(configuration);
        configurations.sort(Comparator.comparingLong(item -> item.segment().anchor().asLong()));
        var rocket = menu.getRocket();
        var updatedPlan = plan.withSegmentConfigurations(configurations);
        if (rocket != null) {
            var segmentRefs = rocket.getStaticSegments().values().stream()
                    .map(SpaceSimulation.SegmentRef::of).toList();
            updatedPlan = RocketFlightPlanRules.trimEngineStageGaps(updatedPlan, segmentRefs);
        }
        menu.setDraftFlightPlan(updatedPlan);
        if (rebuild) rebuildComponents();
    }

    private void closeSegmentConfiguration() {
        selectedSegment = null;
        segmentStagesExpanded = false;
        rebuildComponents();
    }

    private SpaceSimulation.FlightPlan currentPlan() {
        var draft = menu.getDraftFlightPlan();
        return draft == null ? SpaceSimulation.FlightPlan.empty() : draft;
    }

    private void launch() {
        RocketAssemblerClientController.submitFlightPlanIfDirty(menu);
        ClientPacketDistributor.sendToServer(new RocketNetworking.LaunchRocketPayload(menu.blockPos));
    }

    private void addMessagePanel(Component message) {
        int contentHeight = panelHeight - 51;
        addComponent(new SurfaceWidget(12, 39, panelWidth - 24, contentHeight, OritechSurface.PANEL_INSET));
        var label = new LabelWidget(32, 39 + contentHeight / 2 - 15, panelWidth - 64, 30, message);
        label.withAlignment(LabelWidget.Alignment.CENTER).withBrightColor().withWrap(true);
        addComponent(label);
    }

    private void addStatLines(int x, int y, int width, int spacing, List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            addComponent(new LabelWidget(x, y + i * spacing, width, lines.get(i)));
        }
    }

    private List<Component> getSegmentStats(UUID segmentId, ActiveRocketData rocket) {
        var staticSegment = rocket.getStaticSegments().get(segmentId);
        var dynamicSegment = rocket.getDynamicSegments().get(segmentId);
        if (staticSegment == null || dynamicSegment == null) return List.of();

        var segmentRocket = new ActiveRocketData(Map.of(segmentId, staticSegment), Map.of(segmentId, dynamicSegment));
        var performance = RocketPerformanceCalculator.calculate(segmentRocket);
        var configuration = currentPlan().configurationFor(SpaceSimulation.SegmentRef.of(staticSegment));
        String engineStages = configuration.engineStages().isEmpty()
                ? Component.translatable("screen.oritech_space_age.segment_engine_stages.none").getString()
                : configuration.engineStages().stream().sorted()
                .map(stage -> "S" + stage).collect(java.util.stream.Collectors.joining(", "));
        return List.of(
                Component.translatable("screen.oritech_space_age.segment_stats",
                        segmentName(SpaceSimulation.SegmentRef.of(staticSegment), rocket)).withStyle(ChatFormatting.BOLD),
                Component.translatable("screen.oritech_space_age.segment_role",
                        Component.translatable(configuration.booster()
                                ? "screen.oritech_space_age.segment_role.booster"
                                : "screen.oritech_space_age.segment_role.attached")),
                Component.translatable("screen.oritech_space_age.segment_engine_stages", engineStages),
                stat("blocks", staticSegment.blocks().size()),
                stat("dry_mass", format(performance.dryMassKilograms())),
                stat("fuel_mass", format(performance.fuelMassKilograms())),
                stat("energy", format(dynamicSegment.availableRF)),
                stat("engines", staticSegment.engineCount()),
                stat("burn_time", format(performance.availableBurnSeconds()))
        );
    }

    private String segmentName(SpaceSimulation.SegmentRef ref, ActiveRocketData rocket) {
        String customName = currentPlan().configurationFor(ref).name();
        if (!customName.isBlank()) return customName;
        return defaultSegmentName(ref, rocket);
    }

    private static String defaultSegmentName(SpaceSimulation.SegmentRef ref, ActiveRocketData rocket) {
        var refs = rocket.getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of)
                .sorted(Comparator.comparingInt((SpaceSimulation.SegmentRef item) -> item.anchor().getY())
                        .thenComparingInt(item -> item.anchor().getX())
                        .thenComparingInt(item -> item.anchor().getZ()))
                .toList();
        int index = refs.indexOf(ref);
        return index < 0 ? "?" : "S" + (index + 1);
    }

    private static Component stat(String name, Object value) {
        return Component.translatable("screen.oritech_space_age.stat." + name, value);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    @Override
    protected void rebuildComponents() {
        // Segment controls rebuild the popup because their stage count can change. Keep the preview camera separate
        // from that short-lived widget tree so editing a segment does not snap the rocket back to its default view.
        if (rocketPreview != null) previewViewState = rocketPreview.getViewState();
        rocketPreview = null;
        super.rebuildComponents();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        components.forEach(component -> component.tick());
        if (previewRevision != menu.getPreviewRevision()
                || flightPlannerRevision != menu.getFlightPlannerRevision()) {
            if (menu.getFlightPlannerSnapshot() == null) requestedFlightPlanner = false;
            rebuildComponents();
        }
        if (pendingSegment != null) {
            selectedSegment = pendingSegment;
            pendingSegment = null;
            segmentStagesExpanded = false;
            rebuildComponents();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (segmentNameField != null && segmentNameField.isMouseOver(event.x(), event.y())) {
            setFocused(segmentNameField);
            return segmentNameField.mouseClicked(event, doubleClick);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (selectedSegment != null && event.isEscape()) {
            closeSegmentConfiguration();
            return true;
        }
        if (segmentNameField != null && segmentNameField.isFocused()) {
            // Container screens normally interpret E as the inventory shortcut before charTyped reaches a field.
            segmentNameField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        RocketAssemblerClientController.submitFlightPlanIfDirty(menu);
        super.onClose();
    }

    @Override
    public boolean shouldCreateTitle() {
        return false;
    }

    @Override
    public BlockState getTitleState() {
        return SpaceAgeBlocks.ROCKET_ASSEMBLER.get().defaultBlockState();
    }

    private final class RocketPreviewWidget extends BlockPreviewWidget {

        private final ActiveRocketData rocket;
        private final Map<BlockPos, UUID> segmentsByPosition = new HashMap<>();
        private final Map<UUID, Set<BlockPos>> positionsBySegment = new HashMap<>();
        private UUID clickedSegment;
        private boolean draggedSinceClick;

        private RocketPreviewWidget(int x, int y, int width, int height, ActiveRocketData rocket) {
            super(x, y, width, height);
            this.rocket = rocket;
            var addedPositions = new HashSet<BlockPos>();
            for (var entry : rocket.getStaticSegments().entrySet()) {
                var segmentId = entry.getKey();
                var segmentPositions = positionsBySegment.computeIfAbsent(segmentId, ignored -> new HashSet<>());
                for (var block : entry.getValue().blocks()) {
                    addPreviewBlock(block.relativePos(), block.state(), segmentId, segmentPositions, addedPositions);
                }
                for (var couplings : entry.getValue().originalCouplings().values()) {
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
            if (addedPositions.add(immutablePosition)) addBlock(state, null, immutablePosition);
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
            if (entry.offset().equals(hovered.offset())) return OverlayTexture.pack(BLOCK_HIGHLIGHT, false);
            if (positionsBySegment.getOrDefault(segmentId, Set.of()).contains(asBlockPos(entry.offset()))) {
                return OverlayTexture.pack(SEGMENT_HIGHLIGHT, false);
            }
            return OverlayTexture.NO_OVERLAY;
        }

        @Override
        public boolean handleClick(double mouseX, double mouseY, int button) {
            if (!super.handleClick(mouseX, mouseY, button)) return false;
            var clicked = findBlockAt(mouseX, mouseY);
            clickedSegment = clicked == null ? null : segmentsByPosition.get(asBlockPos(clicked.offset()));
            draggedSinceClick = false;
            return true;
        }

        @Override
        public boolean handleDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
            if (Math.abs(deltaX) + Math.abs(deltaY) > 0.5) draggedSinceClick = true;
            return super.handleDrag(mouseX, mouseY, deltaX, deltaY, button);
        }

        @Override
        public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
            boolean handled = super.handleMouseRelease(mouseX, mouseY, button);
            if (button == 0 && clickedSegment != null && !draggedSinceClick) {
                // Rebuilding here would mutate the screen's component list while it dispatches mouse release.
                pendingSegment = clickedSegment;
            }
            clickedSegment = null;
            return handled;
        }

        private void renderTooltipPanel(GuiGraphicsExtractor graphics, int blockX, int blockY, List<Component> tooltip) {
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

    /** Blocks the preview while the compact editor is open without requiring a separate screen class. */
    private static final class SegmentPopupPanel extends UIComponent {
        private final int popupX;
        private final int popupY;
        private final int popupWidth;
        private final int popupHeight;
        private final Runnable dismiss;

        private SegmentPopupPanel(int screenWidth, int screenHeight, int popupX, int popupY,
                                  int popupWidth, int popupHeight, Runnable dismiss) {
            super(0, 0, screenWidth, screenHeight);
            this.popupX = popupX;
            this.popupY = popupY;
            this.popupWidth = popupWidth;
            this.popupHeight = popupHeight;
            this.dismiss = dismiss;
            this.zIndex = 9_000;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(0, 0, width, height, 0x66000000);
            OritechSurface.PANEL_DARK.render(graphics, popupX, popupY, popupWidth, popupHeight);
        }

        @Override
        public boolean handleClick(double mouseX, double mouseY, int button) {
            boolean inside = mouseX >= popupX && mouseX < popupX + popupWidth
                    && mouseY >= popupY && mouseY < popupY + popupHeight;
            if (!inside && button == 0) dismiss.run();
            return true;
        }
    }
}
