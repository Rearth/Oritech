package rearth.oritech.client.ui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.api.screen.widgets.TextureWidget;
import rearth.oritech.block.blocks.reactor.ReactorAbsorberBlock;
import rearth.oritech.block.blocks.reactor.ReactorHeatPipeBlock;
import rearth.oritech.block.blocks.reactor.ReactorHeatVentBlock;
import rearth.oritech.block.blocks.reactor.ReactorRodBlock;
import rearth.oritech.block.entity.reactor.ReactorAbsorberPortEntity;
import rearth.oritech.block.entity.reactor.ReactorControllerBlockEntity;
import rearth.oritech.block.entity.reactor.ReactorFuelPortEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReactorScreen extends OritechWidgetScreen<ReactorScreenHandler> {

    private static final float PREVIEW_DRAW_WIDTH = 170f;
    private static final float PREVIEW_HORIZONTAL_MARGIN = 10f;
    private static final float PREVIEW_BASELINE_Y = 100f;
    private static final float PREVIEW_ISO_X = 0.43f;
    private static final float PREVIEW_ISO_Y = 0.224f;
    private static final float PREVIEW_ISO_HEIGHT = 0.5f;

    private LabelWidget productionLabel;
    private LabelWidget hottestLabel;
    private LabelWidget sumHeatLabel;
    private LabelWidget statusLabel;
    private TextureWidget energyIndicator;
    private ReactorPreviewWidget previewWidget;

    public ReactorScreen(ReactorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 340, 200);
    }

    @Override
    protected void buildComponents() {
        var panel = new SurfaceWidget(0, 0, imageWidth, imageHeight);
        panel.withSurface(OritechSurface.PANEL);
        panel.withZIndex(-10);
        addComponent(panel);

        previewWidget = new ReactorPreviewWidget(6, 16, 175, 164);
        addComponent(previewWidget);
        addStatsPanel();
        addStatusPanel();
        addEnergyBar();

        var title = new LabelWidget(0, 4, imageWidth, 10, menu.reactorEntity.getBlockState().getBlock().getName());
        title.withAlignment(LabelWidget.Alignment.CENTER);
        title.withDarkColor();
        addComponent(title);
    }

    private void addStatsPanel() {
        var panel = new SurfaceWidget(183, 16, 148, 54);
        panel.withSurface(OritechSurface.PANEL_INSET);
        panel.withZIndex(-8);
        addComponent(panel);

        productionLabel = new LabelWidget(189, 22, 129, 10,
            Component.translatable("text.oritech.reactor.rf_production", TooltipHelper.getEnergyText(0)));
        productionLabel.withBrightColor();
        addComponent(productionLabel);

        hottestLabel = new LabelWidget(189, 36, 129, 10,
            Component.translatable("text.oritech.reactor.hottest_part", 0));
        hottestLabel.withBrightColor();
        addComponent(hottestLabel);

        sumHeatLabel = new LabelWidget(189, 50, 129, 10,
            Component.translatable("text.oritech.reactor.heat_production", 0));
        sumHeatLabel.withBrightColor();
        addComponent(sumHeatLabel);
    }

    private void addStatusPanel() {
        statusLabel = new LabelWidget(183, 80, 87, 10, Component.translatable("text.oritech.reactor.idle").withStyle(ChatFormatting.BOLD));
        statusLabel.withAlignment(LabelWidget.Alignment.CENTER);
        statusLabel.withSurface(OritechSurface.PANEL_INSET).withPadding(Insets.of(6, 0, 4, 0));
        statusLabel.withBrightColor();
        addComponent(statusLabel);
    }

    private void addEnergyBar() {
        int x = 294;
        int y = 75;
        int width = 36;
        int height = 110;

        var frame = new SurfaceWidget(x - 1, y - 1, width + 2, height + 2);
        frame.withSurface(OritechSurface.PANEL_INSET);
        frame.withZIndex(-8);
        addComponent(frame);

        addComponent(new TextureWidget(x, y, width, height,
            OritechMachineScreen.GUI_COMPONENTS, 24, 0, 24, 96, 98, 96));

        energyIndicator = new TextureWidget(x, y, width, height,
            OritechMachineScreen.GUI_COMPONENTS, 0, 0, 24, 96, 98, 96);
        addComponent(energyIndicator);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (menu.reactorEntity.componentStats.isEmpty()) return;

        int stackHeight = menu.reactorEntity.areaMax.getY() - menu.reactorEntity.areaMin.getY() - 1;

        int sumProducedEnergy = menu.reactorEntity.componentStats.values().stream()
            .mapToInt(data -> data.receivedPulses() * ReactorControllerBlockEntity.RF_PER_PULSE * stackHeight)
            .sum();

        int sumProducedHeat = menu.reactorEntity.componentStats.values().stream()
            .filter(data -> data.receivedPulses() > 0)
            .mapToInt(ReactorControllerBlockEntity.ComponentStatistics::heatChanged)
            .sum();

        int hottestComponent = menu.reactorEntity.componentStats.values().stream()
            .mapToInt(ReactorControllerBlockEntity.ComponentStatistics::storedHeat)
            .max()
            .orElse(0);

        productionLabel.setText(Component.translatable("text.oritech.reactor.rf_production", TooltipHelper.getEnergyText(sumProducedEnergy)));
        hottestLabel.setText(Component.translatable("text.oritech.reactor.hottest_part", hottestComponent));
        sumHeatLabel.setText(Component.translatable("text.oritech.reactor.heat_production", sumProducedHeat));

        boolean isActive = sumProducedEnergy + sumProducedHeat > 0;
        String activeLabel = "idle";
        ChatFormatting color = ChatFormatting.WHITE;

        if (isActive) {
            if (hottestComponent < 100) {
                activeLabel = "stable";
            } else if (hottestComponent < 1200) {
                activeLabel = "heating_up";
                color = ChatFormatting.YELLOW;
            } else if (hottestComponent < 1700) {
                activeLabel = "unstable";
                color = ChatFormatting.RED;
            } else {
                activeLabel = "explosion_imminent";
                color = ChatFormatting.DARK_RED;
            }
        }

        statusLabel.setText(Component.translatable("text.oritech.reactor." + activeLabel).withStyle(ChatFormatting.BOLD).withStyle(color));
        updateEnergyBar();
    }

    private void updateEnergyBar() {
        long capacity = menu.reactorEntity.energyStorage.getCapacity();
        long amount = menu.reactorEntity.energyStorage.getAmount();
        float fillAmount = capacity <= 0 ? 0f : (float) amount / capacity;

        energyIndicator.withTooltip(Component.translatable("tooltip.oritech.energy_indicator", amount, capacity));
        energyIndicator.setVisibleArea(0, 108 - (int) (108 * fillAmount), 36, (int) (108 * fillAmount));
    }

    private List<Component> getStatsTooltip(BlockPos pos, BlockState state) {
        var tooltip = new ArrayList<Component>();
        tooltip.add(state.getBlock().getName().copy().withStyle(ChatFormatting.BOLD));

        var stats = getStatsAtPosition(pos);
        if (stats.storedHeat() == -1) return tooltip;

        int stackHeight = menu.reactorEntity.areaMax.getY() - menu.reactorEntity.areaMin.getY() - 1;
        var portPosition = pos.offset(0, stackHeight, 0);
        var portEntity = menu.world.getBlockEntity(portPosition);
        if (portEntity != null && portEntity.isRemoved()) return tooltip;

        if (state.getBlock() instanceof ReactorRodBlock rodBlock) {
            int rodCount = rodBlock.getRodCount();
            int totalPulses = stats.receivedPulses();
            int createdPulses = totalPulses == 0 ? 0 : rodBlock.getInternalPulseCount();
            int externalPulses = totalPulses - createdPulses;
            int generatedEnergy = ReactorControllerBlockEntity.RF_PER_PULSE * totalPulses;
            int generatedHeat = stats.heatChanged();
            int heat = stats.storedHeat();

            if (portEntity instanceof ReactorFuelPortEntity fuelPortEntity) {
                tooltip.add(Component.translatable("text.oritech.reactor.rod_count", rodCount));
                tooltip.add(Component.translatable("text.oritech.reactor.generated_pulses", createdPulses));
                tooltip.add(Component.translatable("text.oritech.reactor.received_pulses", externalPulses));
                tooltip.add(Component.translatable("text.oritech.reactor.generated_heat", generatedHeat));
                tooltip.add(Component.translatable("text.oritech.reactor.generated_energy", generatedEnergy));
                tooltip.add(Component.translatable("text.oritech.reactor.heat", heat));
                tooltip.add(Component.translatable("text.oritech.reactor.fuel", fuelPortEntity.availableFuel, fuelPortEntity.currentFuelOriginalCapacity));
            }
        } else if (state.getBlock() instanceof ReactorHeatPipeBlock) {
            tooltip.add(Component.translatable("text.oritech.reactor.collected_heat", stats.heatChanged()));
            tooltip.add(Component.translatable("text.oritech.reactor.heat", stats.storedHeat()));
        } else if (state.getBlock() instanceof ReactorHeatVentBlock) {
            tooltip.add(Component.translatable("text.oritech.reactor.removed_heat", stats.heatChanged()));
        } else if (state.getBlock() instanceof ReactorAbsorberBlock) {
            if (portEntity instanceof ReactorAbsorberPortEntity absorberPortEntity) {
                tooltip.add(Component.translatable("text.oritech.reactor.absorbed_heat", stats.heatChanged()));
                tooltip.add(Component.translatable("text.oritech.reactor.absorbant", absorberPortEntity.availableFuel, absorberPortEntity.currentFuelOriginalCapacity));
            }
        }

        return tooltip;
    }

    public ReactorControllerBlockEntity.ComponentStatistics getStatsAtPosition(BlockPos pos) {
        if (menu.reactorEntity.componentStats.isEmpty()) {
            return ReactorControllerBlockEntity.ComponentStatistics.EMPTY;
        }

        var reactorMin = menu.reactorEntity.areaMin;
        for (var entry : menu.reactorEntity.componentStats.entrySet()) {
            var localPos = entry.getKey();
            var worldPos = reactorMin.offset(localPos.x + 1, 1, localPos.y + 1);
            if (worldPos.equals(pos)) return entry.getValue();
        }

        return ReactorControllerBlockEntity.ComponentStatistics.EMPTY;
    }

    @Override
    public boolean shouldCreateTitle() {
        return false;
    }

    @Override
    public BlockState getTitleState() {
        return menu.reactorEntity.getBlockState();
    }


    private final class ReactorPreviewWidget extends UIComponent {
        private static final Comparator<PreviewEntry> PREVIEW_RENDER_ORDER = Comparator
            .comparingDouble(PreviewEntry::zIndex)
            .thenComparingInt(PreviewEntry::drawY)
            .thenComparingInt(PreviewEntry::drawX);

        private final List<PreviewEntry> blockEntries = new ArrayList<>();
        private final List<PreviewEntry> heatEntries = new ArrayList<>();
        private @Nullable PreviewEntry hoveredEntry;
        private final int blockSize;

        private ReactorPreviewWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.surface = OritechSurface.PANEL_INSET;

            var previewMax = menu.reactorEntity.areaMax.atY(menu.reactorEntity.areaMin.getY() + 1);
            var totalSize = previewMax.subtract(menu.reactorEntity.areaMin);
            int leftCount = totalSize.getZ();
            int rightCount = totalSize.getX();
            int totalWidth = leftCount + rightCount + 3;
            float middlePercentage = leftCount / (float) totalWidth;
            float xOffset = middlePercentage * PREVIEW_DRAW_WIDTH + PREVIEW_HORIZONTAL_MARGIN;

            this.blockSize = Math.max(1, Math.round(PREVIEW_DRAW_WIDTH / totalWidth * 2.2f));

            BlockPos.betweenClosedStream(menu.reactorEntity.areaMin, previewMax).forEach(pos -> {
                var entry = createPreviewEntry(pos, xOffset);
                if (entry == null) {
                    return;
                }

                var offset = pos.subtract(menu.reactorEntity.areaMin);
                blockEntries.add(entry);

                var state = menu.world.getBlockState(pos);
                if (state.getBlock() instanceof ReactorRodBlock || state.getBlock() instanceof ReactorHeatPipeBlock) {
                    heatEntries.add(new PreviewEntry(pos.immutable(), null, entry.zIndex() + 0.5f, entry.drawX(), entry.drawY()));
                }
            });

            blockEntries.sort(PREVIEW_RENDER_ORDER);
            heatEntries.sort(PREVIEW_RENDER_ORDER);
        }

        @Override
        protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            hoveredEntry = findHoveredEntry(mouseX, mouseY);

            for (var entry : blockEntries) {
                renderBlock(graphics, entry.drawX(), entry.drawY(), blockSize, entry.zIndex(), null, entry.blockEntity(), entry.pos(), delta);
            }

            for (var entry : heatEntries) {
                var state = resolveHeatState(entry.pos());
                if (!state.isAir()) {
                    renderBlock(graphics, entry.drawX(), entry.drawY(), blockSize, entry.zIndex(), state, null, entry.pos(), delta);
                }
            }

            if (hoveredEntry != null) {
                renderBlock(graphics, hoveredEntry.drawX(), hoveredEntry.drawY(), blockSize, hoveredEntry.zIndex() + 0.6f,
                    BlockContent.ADDON_INDICATOR_BLOCK.defaultBlockState(), null, hoveredEntry.pos(), delta);
                renderTooltipPanel(graphics, hoveredEntry.drawX(), hoveredEntry.drawY(), getStatsTooltip(hoveredEntry.pos(), menu.world.getBlockState(hoveredEntry.pos())));
            }
        }

        private BlockState resolveHeatState(BlockPos pos) {
            var data = getStatsAtPosition(pos);
            if (data.storedHeat() <= 10) {
                return Blocks.AIR.defaultBlockState();
            }

            if (data.storedHeat() > 1000) {
                return BlockContent.REACTOR_HOT_INDICATOR_BLOCK.defaultBlockState();
            }
            if (data.storedHeat() > 200) {
                return BlockContent.REACTOR_MEDIUM_INDICATOR_BLOCK.defaultBlockState();
            }
            return BlockContent.REACTOR_COLD_INDICATOR_BLOCK.defaultBlockState();
        }

        @Override
        public boolean hasTooltip() {
            return false;
        }

        private @Nullable PreviewEntry createPreviewEntry(BlockPos pos, float xOffset) {
            var state = menu.world.getBlockState(pos);
            if (state.isAir()) {
                return null;
            }

            var offset = pos.subtract(menu.reactorEntity.areaMin);
            float projectedPosX = offset.getX() * PREVIEW_ISO_X - offset.getZ() * PREVIEW_ISO_X;
            float projectedPosY = offset.getX() * PREVIEW_ISO_Y + offset.getZ() * PREVIEW_ISO_Y + offset.getY() * PREVIEW_ISO_HEIGHT;
            float zIndex = offset.getY() - offset.getX() - offset.getZ();
            int drawX = Math.round(projectedPosX * blockSize + xOffset);
            int drawY = Math.round(PREVIEW_BASELINE_Y - projectedPosY * blockSize);

            return new PreviewEntry(
                pos.immutable(),
                menu.world.getBlockEntity(pos),
                zIndex,
                drawX,
                drawY
            );
        }

        private @Nullable PreviewEntry findHoveredEntry(int mouseX, int mouseY) {
            if (!isMouseOver(mouseX, mouseY)) {
                return null;
            }

            float localMouseX = (float) mouseX - x;
            float localMouseY = (float) mouseY - y;
            for (int i = blockEntries.size() - 1; i >= 0; i--) {
                var entry = blockEntries.get(i);
                var yLevel = entry.pos.getY() - menu.reactorEntity.areaMin.getY();
                if (yLevel == 1 && localMouseX >= entry.drawX() && localMouseX <= entry.drawX() + blockSize &&
                    localMouseY >= entry.drawY() && localMouseY <= entry.drawY() + blockSize) {
                    return entry;
                }
            }
            return null;
        }

        private void renderTooltipPanel(GuiGraphics graphics, int blockX, int blockY, List<Component> tooltip) {
            if (tooltip.isEmpty()) return;

            var font = Minecraft.getInstance().font;
            int maxWidth = 0;
            for (var line : tooltip) {
                maxWidth = Math.max(maxWidth, font.width(line));
            }

            int panelWidth = maxWidth + 10;
            int panelHeight = tooltip.size() * font.lineHeight + 8;
            int tooltipX = Math.max(4, Math.min(width - panelWidth - 4, blockX - 24));
            int tooltipY = Math.max(4, blockY - panelHeight - 6);
            
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 3000);
            graphics.fill(tooltipX, tooltipY, tooltipX + panelWidth, tooltipY + panelHeight, 0xCC101418);
            graphics.fill(tooltipX, tooltipY, tooltipX + panelWidth, tooltipY + 1, 0xFF9DB4C7);
            graphics.fill(tooltipX, tooltipY + panelHeight - 1, tooltipX + panelWidth, tooltipY + panelHeight, 0xFF9DB4C7);
            graphics.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + panelHeight, 0xFF9DB4C7);
            graphics.fill(tooltipX + panelWidth - 1, tooltipY, tooltipX + panelWidth, tooltipY + panelHeight, 0xFF9DB4C7);

            for (int index = 0; index < tooltip.size(); index++) {
                graphics.drawString(font, tooltip.get(index), tooltipX + 5, tooltipY + 4 + index * font.lineHeight, 0xFFF2F6FA, false);
            }
            graphics.pose().popPose();
        }

        private void renderBlock(GuiGraphics graphics, int drawX, int drawY, int size, float zIndex,
                                 @Nullable BlockState overrideState, @Nullable BlockEntity overrideEntity,
                                 BlockPos worldPos, float delta) {
            var client = Minecraft.getInstance();
            var usedState = overrideState != null ? overrideState : menu.world.getBlockState(worldPos);
            var usedEntity = overrideEntity;

            graphics.pose().pushPose();
            graphics.pose().translate(x + drawX + size / 2f, y + drawY + size / 2f, zIndex * 25 + 1000);
            graphics.pose().scale(40 * size / 64f, -40 * size / 64f, 40);
            graphics.pose().mulPose(Axis.XP.rotationDegrees(30));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(225));
            graphics.pose().translate(-0.5f, -0.5f, -0.5f);

            RenderSystem.runAsFancy(() -> {
                var vertexConsumers = client.renderBuffers().bufferSource();
                if (usedState.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                    client.getBlockRenderer().renderSingleBlock(
                        usedState, graphics.pose(), vertexConsumers,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                    );
                }

                if (usedEntity != null) {
                    var renderer = client.getBlockEntityRenderDispatcher().getRenderer(usedEntity);
                    if (renderer != null) {
                        renderer.render(usedEntity, delta, graphics.pose(), vertexConsumers,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    }
                }

                RenderSystem.setShaderLights(new Vector3f(-1.5f, -0.5f, 0), new Vector3f(0, -1, 0));
                vertexConsumers.endBatch();
                Lighting.setupFor3DItems();
            });

            graphics.pose().popPose();
        }
    }

    private record PreviewEntry(BlockPos pos, @Nullable BlockEntity blockEntity, float zIndex, int drawX, int drawY) {}
}