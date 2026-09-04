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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
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
    private int panelWidth;
    private int panelHeight;

    public RocketAssemblerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
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
        addComponent(preview);

        addComponent(new SurfaceWidget(statsX, contentTop, statsWidth, contentHeight, OritechSurface.PANEL_INSET));
        addComponent(new LabelWidget(statsX + 11, contentTop + 12, statsWidth - 19,
                Component.translatable("screen.oritech_space_age.general_stats").withStyle(ChatFormatting.BOLD)));

        var performance = RocketPerformanceCalculator.calculate(rocket);
        var blockCount = rocket.getStaticSegments().values().stream().mapToInt(segment -> segment.blocks().size()).sum();
        int statsSpacing = Math.clamp((contentHeight - 115) / 7, 9, 15);
        addStatLines(statsX + 11, contentTop + 32, statsWidth - 19, statsSpacing, List.of(
                stat("segments", rocket.getStaticSegments().size()),
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
        ClientPacketDistributor.sendToServer(new RocketNetworking.RequestFlightPlannerPayload(menu.blockPos));
        Minecraft.getInstance().setScreen(new RocketFlightPlannerScreen(menu, screenInventory, screenTitle));
    }

    private void launch() {
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
        return List.of(
                Component.translatable("screen.oritech_space_age.segment_stats",
                        segmentName(SpaceSimulation.SegmentRef.of(staticSegment), rocket)).withStyle(ChatFormatting.BOLD),
                stat("blocks", staticSegment.blocks().size()),
                stat("dry_mass", format(performance.dryMassKilograms())),
                stat("fuel_mass", format(performance.fuelMassKilograms())),
                stat("energy", format(dynamicSegment.availableRF)),
                stat("engines", staticSegment.engineCount()),
                stat("burn_time", format(performance.availableBurnSeconds()))
        );
    }

    private static String segmentName(SpaceSimulation.SegmentRef ref, ActiveRocketData rocket) {
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
    protected void containerTick() {
        super.containerTick();
        components.forEach(component -> component.tick());
        if (previewRevision != menu.getPreviewRevision()) rebuildComponents();
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
}
