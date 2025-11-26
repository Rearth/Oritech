package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import rearth.oritech.block.blocks.reactor.ReactorAbsorberBlock;
import rearth.oritech.block.blocks.reactor.ReactorHeatPipeBlock;
import rearth.oritech.block.blocks.reactor.ReactorHeatVentBlock;
import rearth.oritech.block.blocks.reactor.ReactorRodBlock;
import rearth.oritech.block.entity.reactor.ReactorAbsorberPortEntity;
import rearth.oritech.block.entity.reactor.ReactorControllerBlockEntity;
import rearth.oritech.block.entity.reactor.ReactorControllerBlockEntity.ComponentStatistics;
import rearth.oritech.block.entity.reactor.ReactorFuelPortEntity;
import rearth.oritech.client.ui.components.ReactorBlockRenderComponent;
import rearth.oritech.client.ui.components.ReactorPreviewContainer;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.ScreenProvider.BarConfiguration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static rearth.oritech.client.ui.BasicMachineScreen.ORITECH_PANEL;


public class ReactorScreen extends BaseOwoHandledScreen<FlowLayout, ReactorScreenHandler> {
    
    private ArrayList<Tuple<Integer, ReactorBlockRenderComponent>> activeComponents;
    private HashSet<ReactorBlockRenderComponent> activeOverlays;
    private LabelComponent tooltipTitle;
    private FlowLayout tooltipContainer;
    private ReactorBlockRenderComponent selectedBlockOverlay;
    private TextureComponent energyIndicator;
    private LabelComponent productionLabel;
    private LabelComponent hottestLabel;
    private LabelComponent sumHeatLabel;
    private LabelComponent statusLabel;
    
    public ReactorScreen(ReactorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
          .surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        tooltipContainer = Containers.verticalFlow(Sizing.content(2), Sizing.content(2));
        tooltipContainer.surface(Surface.VANILLA_TRANSLUCENT);
        tooltipTitle = Components.label(Component.literal("My title!"));
        tooltipContainer.child(tooltipTitle.margins(Insets.of(6)));
        tooltipContainer.zIndex(3000);
        tooltipContainer.padding(Insets.of(3));
        tooltipTitle.zIndex(3001);
        
        var overlay = Containers.horizontalFlow(Sizing.fixed(329), Sizing.fixed(200));
        rootComponent.child(overlay.surface(ORITECH_PANEL));
        
        addReactorComponentPreview(overlay);
        addReactorStats(overlay);
        addEnergyBar(overlay);
        addReactorStatus(overlay);
        
        addTitle(overlay);
        rootComponent.child(tooltipContainer.positioning(Positioning.absolute(0, 0)));
    }
    
    private void addReactorStats(FlowLayout overlay) {
        var container = Containers.verticalFlow(Sizing.fixed(131), Sizing.content(0));
        
        productionLabel = Components.label(Component.translatable("RF Production: %s RF/t", "50").withStyle(ChatFormatting.WHITE));
        sumHeatLabel = Components.label(Component.translatable("Heat Production: %s RF/t", "50").withStyle(ChatFormatting.WHITE));
        hottestLabel = Components.label(Component.translatable("Hottest Part: %s RF/t", "50").withStyle(ChatFormatting.WHITE));
        
        container.child(productionLabel.margins(Insets.of(4)));
        container.child(hottestLabel.margins(Insets.of(4)));
        container.child(sumHeatLabel.margins(Insets.of(4)));
        
        overlay.child(container.margins(Insets.of(8)).surface(Surface.PANEL_INSET).positioning(Positioning.absolute(183, 16)));
    }
    
    private void addReactorStatus(FlowLayout overlay) {
        
        var container = Containers.verticalFlow(Sizing.fixed(90), Sizing.content(1));
        
        statusLabel = Components.label(Component.translatable("Stable").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        
        container.child(statusLabel.horizontalTextAlignment(HorizontalAlignment.CENTER).horizontalSizing(Sizing.fill()).margins(Insets.of(4)));
        
        overlay.child(container.margins(Insets.of(4)).surface(Surface.PANEL_INSET).positioning(Positioning.absolute(187, 75)));
        
    }
    
    private BlockPos getPreviewMax() {
        return menu.reactorEntity.areaMax.atY(menu.reactorEntity.areaMin.getY() + 1);
    }
    
    private void addReactorComponentPreview(FlowLayout overlay) {
        
        var holoPreviewContainer = new ReactorPreviewContainer(Sizing.fixed(180), Sizing.fixed(164), FlowLayout.Algorithm.HORIZONTAL, this::onContainerMouseMove);
        holoPreviewContainer.surface(Surface.PANEL_INSET);
        holoPreviewContainer.margins(Insets.of(8));
        
        var totalSize = getPreviewMax().subtract(menu.reactorEntity.areaMin);
        var leftCount = totalSize.getZ();
        var rightCount = totalSize.getX();
        var totalWidth = leftCount + rightCount + 3;
        var middlePercentage = leftCount / (float) totalWidth;
        var xOffset = middlePercentage * 170 + 10;
        
        var size = (int) (170 / (float) totalWidth * 2.2f);
        
        activeComponents = new ArrayList<>();
        activeOverlays = new HashSet<>();
        
        BlockPos.betweenClosedStream(menu.reactorEntity.areaMin, getPreviewMax()).forEach(pos -> {
            var state = menu.world.getBlockState(pos);
            if (state.isAir()) return;
            var offset = pos.subtract(menu.reactorEntity.areaMin);
            var projectedPosX = offset.getX() * 0.43f - offset.getZ() * 0.43f;
            var projectedPosY = offset.getX() * 0.224f + offset.getZ() * 0.224f + offset.getY() * 0.5f;
            var zIndex = offset.getY() - offset.getX() - offset.getZ();
            var preview = new ReactorBlockRenderComponent(null, menu.world.getBlockEntity(pos), zIndex, pos.immutable())
                            .sizing(Sizing.fixed(size))
                            .positioning(Positioning.absolute((int) (projectedPosX * size + xOffset), (int) (-projectedPosY * size) + 100));
            if (offset.getY() == 1) {
                activeComponents.add(new Tuple<>(-zIndex, (ReactorBlockRenderComponent) preview));
            }
            holoPreviewContainer.child(preview);
            
            if (state.getBlock() instanceof ReactorRodBlock || state.getBlock() instanceof ReactorHeatPipeBlock) {
                var heatOverlay = new ReactorBlockRenderComponent(Blocks.AIR.defaultBlockState(), null, zIndex + 0.5f, pos.immutable())
                                    .sizing(Sizing.fixed(size))
                                    .positioning(Positioning.absolute((int) (projectedPosX * size + xOffset), (int) (-projectedPosY * size) + 100));
                
                holoPreviewContainer.child(heatOverlay);
                activeOverlays.add((ReactorBlockRenderComponent) heatOverlay);
            }
            
        });
        
        selectedBlockOverlay = (ReactorBlockRenderComponent) new ReactorBlockRenderComponent(Blocks.AIR.defaultBlockState(), null, 10 + 0.5f, BlockPos.ZERO)
                                                               .sizing(Sizing.fixed(size))
                                                               .positioning(Positioning.absolute(0, 0));
        holoPreviewContainer.child(selectedBlockOverlay);
        
        activeComponents.sort(Comparator.comparingInt(Tuple::getA));
        
        overlay.child(holoPreviewContainer.positioning(Positioning.absolute(0, 16)));
        
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        if (menu.reactorEntity.componentStats.isEmpty()) return;
        
        for (var overlay : activeOverlays) {
            var data = getStatsAtPosition(overlay.pos);
            
            var isEmpty = data.storedHeat() <= 10;
            if (isEmpty) {
                overlay.state = Blocks.AIR.defaultBlockState();
                continue;
            }
            
            var res = BlockContent.REACTOR_COLD_INDICATOR_BLOCK.defaultBlockState();
            
            if (data.storedHeat() > 1000) {
                res = BlockContent.REACTOR_HOT_INDICATOR_BLOCK.defaultBlockState();
            } else if (data.storedHeat() > 200) {
                res = BlockContent.REACTOR_MEDIUM_INDICATOR_BLOCK.defaultBlockState();
            }
            
            overlay.state = res;
        }
        
        var stackHeight = menu.reactorEntity.areaMax.getY() - menu.reactorEntity.areaMin.getY() - 1;
        
        // gather stats
        var sumProducedEnergy = menu.reactorEntity.componentStats.values().stream()
                                  .mapToInt(data -> data.receivedPulses() * ReactorControllerBlockEntity.RF_PER_PULSE * stackHeight).sum();
        
        var sumProducedHeat = menu.reactorEntity.componentStats.values().stream()
                                .filter(elem -> elem.receivedPulses() > 0)
                                .mapToInt(ReactorControllerBlockEntity.ComponentStatistics::heatChanged).sum();
        
        var hottestComponent = menu.reactorEntity.componentStats.values().stream()
                                 .mapToInt(ReactorControllerBlockEntity.ComponentStatistics::storedHeat)
                                 .max().orElse(0);
        
        productionLabel.text(Component.translatable("text.oritech.reactor.rf_production", sumProducedEnergy));
        hottestLabel.text(Component.translatable("text.oritech.reactor.hottest_part", hottestComponent));
        sumHeatLabel.text(Component.translatable("text.oritech.reactor.heat_production", sumProducedHeat));
        
        // update status
        var isActive = sumProducedEnergy + sumProducedHeat > 0;
        var activeLabel = "idle";
        var color = ChatFormatting.WHITE;
        
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
        
        statusLabel.text(Component.translatable("text.oritech.reactor." + activeLabel).withStyle(ChatFormatting.BOLD).withStyle(color));
        
        updateEnergyBar();
        
    }
    
    private void onContainerMouseMove(int mouseX, int mouseY) {
        
        var posX = mouseX;
        var posY = mouseY;
        
        // check if self is on top of activeComponents
        for (var component : activeComponents) {
            var hit = component.getB().isInBoundingBox(mouseX, mouseY);
            if (hit) {
                var pos = component.getB().pos;
                addStatsToTooltip(pos, menu.world.getBlockState(pos), tooltipContainer);
                posX = component.getB().x();
                posY = component.getB().y();
                
                selectedBlockOverlay.state = BlockContent.ADDON_INDICATOR_BLOCK.defaultBlockState();
                selectedBlockOverlay.pos = pos;
                selectedBlockOverlay.zIndex = component.getB().zIndex + 0.6f;
                selectedBlockOverlay.positioning(component.getB().positioning().get());
                
                break;
            }
        }
        
        if (posX == mouseX) {   // move out of visible area
            tooltipContainer.positioning(Positioning.absolute(-100, -500));
            selectedBlockOverlay.state = Blocks.AIR.defaultBlockState();
            return;
        }
        
        var containerHeight = tooltipContainer.height();
        
        tooltipContainer.positioning(Positioning.absolute(posX - 30, posY - 5 - containerHeight));
        
    }
    
    public void addStatsToTooltip(BlockPos pos, BlockState state, FlowLayout container) {
        
        container.clearChildren();
        
        var blockname = state.getBlock().getName();
        container.child(Components.label(blockname.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)).margins(Insets.of(0, 3, 0, 0)));
        
        var stats = getStatsAtPosition(pos);
        if (stats.storedHeat() == -1) return;
        
        var stackHeight = menu.reactorEntity.areaMax.getY() - menu.reactorEntity.areaMin.getY() - 1;
        var portPosition = pos.offset(0, stackHeight, 0);
        var portEntity = menu.world.getBlockEntity(portPosition);
        if (portEntity != null && portEntity.isRemoved()) return;
        
        
        if (state.getBlock() instanceof ReactorRodBlock rodBlock) {
            var rodCount = rodBlock.getRodCount();
            var totalPulses = stats.receivedPulses();
            var createdPulses = rodBlock.getInternalPulseCount();
            var externalPulses = totalPulses - createdPulses;
            var generatedEnergy = ReactorControllerBlockEntity.RF_PER_PULSE * totalPulses;
            var generatedHeat = stats.heatChanged();
            var heat = stats.storedHeat();
            
            if (totalPulses == 0) { // probably no fuel
                createdPulses = 0;
                externalPulses = 0;
            }
            
            if (!(portEntity instanceof ReactorFuelPortEntity fuelPortEntity)) return;
            var availableFuel = fuelPortEntity.availableFuel;
            var maxFuel = fuelPortEntity.currentFuelOriginalCapacity;
            
            container.child(Components.label(Component.translatable("text.oritech.reactor.rod_count", rodCount).withStyle(ChatFormatting.WHITE)));
            container.child(Components.label(Component.translatable("text.oritech.reactor.generated_pulses", createdPulses).withStyle(ChatFormatting.WHITE)));
            container.child(Components.label(Component.translatable("text.oritech.reactor.received_pulses", externalPulses).withStyle(ChatFormatting.WHITE)));
            container.child(Components.label(Component.translatable("text.oritech.reactor.generated_heat", generatedHeat).withStyle(ChatFormatting.WHITE)));
            container.child(Components.label(Component.translatable("text.oritech.reactor.generated_energy", generatedEnergy).withStyle(ChatFormatting.WHITE)));
            container.child(Components.label(Component.translatable("text.oritech.reactor.heat", heat).withStyle(ChatFormatting.WHITE)));
            container.child(Components.label(Component.translatable("text.oritech.reactor.fuel", availableFuel, maxFuel).withStyle(ChatFormatting.WHITE)));
        } else if (state.getBlock() instanceof ReactorHeatPipeBlock pipeBlock) {
            container.child(Components.label(Component.translatable("text.oritech.reactor.collected_heat", stats.heatChanged()).withStyle(ChatFormatting.WHITE)));
            container.child(Components.label(Component.translatable("text.oritech.reactor.heat", stats.storedHeat()).withStyle(ChatFormatting.WHITE)));
        } else if (state.getBlock() instanceof ReactorHeatVentBlock pipeBlock) {
            container.child(Components.label(Component.translatable("text.oritech.reactor.removed_heat", stats.heatChanged()).withStyle(ChatFormatting.WHITE)));
        } else if (state.getBlock() instanceof ReactorAbsorberBlock absorberBlock) {
            
            if (!(portEntity instanceof ReactorAbsorberPortEntity absorberPortEntity)) return;
            var availableFuel = absorberPortEntity.availableFuel;
            var maxFuel = absorberPortEntity.currentFuelOriginalCapacity;
            
            container.child(Components.label(Component.translatable("text.oritech.reactor.absorbed_heat", stats.heatChanged()).withStyle(ChatFormatting.WHITE)));
            container.child(Components.label(Component.translatable("text.oritech.reactor.absorbant", availableFuel, maxFuel).withStyle(ChatFormatting.WHITE)));
        }
        
    }
    
    public ReactorControllerBlockEntity.ComponentStatistics getStatsAtPosition(BlockPos pos) {
        
        if (menu.reactorEntity.componentStats.isEmpty())
            return ReactorControllerBlockEntity.ComponentStatistics.EMPTY;
        
        var reactorMin = menu.reactorEntity.areaMin;
        
        for (var entry : menu.reactorEntity.componentStats.entrySet()) {
            var localPos = entry.getKey();
            var worldPos = reactorMin.offset(localPos.x + 1, 1, localPos.y + 1);
            if (worldPos.equals(pos)) return entry.getValue();
        }
        
        return ReactorControllerBlockEntity.ComponentStatistics.EMPTY;
    }
    
    private void addTitle(FlowLayout overlay) {
        var blockTitle = menu.reactorEntity.getBlockState().getBlock().getName();
        var label = Components.label(blockTitle);
        label.color(new Color(64 / 255f, 64 / 255f, 64 / 255f));
        label.sizing(Sizing.fixed(176), Sizing.content(2));
        label.horizontalTextAlignment(HorizontalAlignment.CENTER);
        label.zIndex(1);
        overlay.child(label.positioning(Positioning.relative(50, 3)));
    }
    
    private void addEnergyBar(FlowLayout panel) {
        
        var config = new ScreenProvider.BarConfiguration(285, 80, 36, 108);
        var insetSize = 1;
        var tooltipText = Component.translatable("tooltip.oritech.energy_indicator", 10, 50);
        
        var frame = Containers.horizontalFlow(Sizing.fixed(config.width() + insetSize * 2), Sizing.fixed(config.height() + insetSize * 2));
        frame.surface(Surface.PANEL_INSET);
        frame.padding(Insets.of(insetSize));
        frame.positioning(Positioning.absolute(config.x() - insetSize, config.y() - insetSize));
        panel.child(frame);
        
        var indicator_background = Components.texture(BasicMachineScreen.GUI_COMPONENTS, 24, 0, 24, 96, 98, 96);
        indicator_background.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        
        energyIndicator = Components.texture(BasicMachineScreen.GUI_COMPONENTS, 0, 0, 24, (96), 98, 96);
        energyIndicator.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        energyIndicator.positioning(Positioning.absolute(0, 0));
        energyIndicator.tooltip(tooltipText);
        
        frame
          .child(indicator_background)
          .child(energyIndicator);
    }
    
    protected void updateEnergyBar() {
        
        var capacity = menu.reactorEntity.energyStorage.getCapacity();
        var amount = menu.reactorEntity.energyStorage.getAmount();
        
        var fillAmount = (float) amount / capacity;
        var tooltipText = BasicMachineScreen.getEnergyTooltip(amount, capacity, 0, 0);
        
        energyIndicator.tooltip(tooltipText);
        energyIndicator.visibleArea(PositionedRectangle.of(0, 96 - ((int) (96 * (fillAmount))), 24, (int) (96 * fillAmount)));
    }
}
