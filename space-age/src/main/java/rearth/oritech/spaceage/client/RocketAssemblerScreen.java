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
import rearth.oritech.spaceage.simulation.RocketSimulationController;
import rearth.oritech.spaceage.simulation.StaticRocketSegment;

import java.util.*;

public class RocketAssemblerScreen extends OritechWidgetScreen<RocketAssemblerMenu> {

    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 285;
    private static final float SEGMENT_HIGHLIGHT = 0.25f;
    private static final float BLOCK_HIGHLIGHT = 0.5f;

    private Tab activeTab = Tab.ROCKET;
    private int previewRevision = -1;

    public RocketAssemblerScreen(RocketAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    protected void buildComponents() {
        previewRevision = menu.getPreviewRevision();
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
        addComponent(new SurfaceWidget(12, 39, 456, 232, OritechSurface.PANEL_INSET));
        var placeholder = new LabelWidget(24, 145, 432, 20,
                Component.translatable("screen.oritech_space_age.flight_plan_placeholder"));
        placeholder.withAlignment(LabelWidget.Alignment.CENTER).withBrightColor();
        addComponent(placeholder);
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

        if (previewRevision != menu.getPreviewRevision()) {
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
