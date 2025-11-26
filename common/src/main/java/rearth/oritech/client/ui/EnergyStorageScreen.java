package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.energy.containers.DynamicStatisticEnergyStorage.EnergyStatistics;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.TooltipHelper;

import java.util.Collection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EnergyStorageScreen extends UpgradableMachineScreen<UpgradableMachineScreenHandler> {
    
    private LabelComponent inAvgSecond;
    private LabelComponent inLastTick;
    private LabelComponent inSources;
    private LabelComponent inPeak;
    private LabelComponent outAvgSecond;
    private LabelComponent outLastTick;
    private LabelComponent outPeak;
    private boolean showingOutput;
    
    public EnergyStorageScreen(UpgradableMachineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        var panelXPos = 74;
        
        var insertionContainer = Containers.verticalFlow(Sizing.fixed(94), Sizing.content());
        insertionContainer.surface(Surface.PANEL_INSET);
        insertionContainer.padding(Insets.of(2, 2, 4, 8));
        insertionContainer.positioning(Positioning.absolute(panelXPos, 23));
        
        inAvgSecond = Components.label(Component.literal("X RF/t"));
        inAvgSecond.tooltip(Component.translatable("title.oritech.energy.inAvgSecond.tooltip"));
        inLastTick = Components.label(Component.literal("X RF/t"));
        inLastTick.tooltip(Component.translatable("title.oritech.energy.inLastTick.tooltip"));
        inSources = Components.label(Component.literal("X"));
        inSources.tooltip(Component.translatable("title.oritech.energy.inSources.tooltip"));
        inPeak = Components.label(Component.literal("X RF/t"));
        inPeak.tooltip(Component.translatable("title.oritech.energy.inPeak.tooltip"));
        
        insertionContainer.child(inLastTick.margins(Insets.of(2)));
        insertionContainer.child(inAvgSecond.margins(Insets.of(2)));
        insertionContainer.child(inPeak.margins(Insets.of(2)));
        insertionContainer.child(inSources.margins(Insets.of(2)));
        
        var extractionContainer = Containers.verticalFlow(Sizing.fixed(94), Sizing.content());
        extractionContainer.surface(Surface.PANEL_INSET);
        extractionContainer.padding(Insets.of(2, 2, 4, 8));
        extractionContainer.positioning(Positioning.absolute(panelXPos, 23));
        
        outAvgSecond = Components.label(Component.literal("X RF/t"));
        outAvgSecond.tooltip(Component.translatable("title.oritech.energy.outAvgSecond.tooltip"));
        outLastTick = Components.label(Component.literal("X RF/t"));
        outLastTick.tooltip(Component.translatable("title.oritech.energy.outLastTick.tooltip"));
        outPeak = Components.label(Component.literal("X RF/t"));
        outPeak.tooltip(Component.translatable("title.oritech.energy.outPeak.tooltip"));
        
        extractionContainer.child(outLastTick.margins(Insets.of(2)));
        extractionContainer.child(outAvgSecond.margins(Insets.of(2)));
        extractionContainer.child(outPeak.margins(Insets.of(2)));
        
        overlay.child(insertionContainer);
        
        var toggleButton = Components.button(Component.literal("                  ").append(Component.translatable("title.oritech.item_filter.toggle_energy_statistics").withColor(BasicMachineScreen.GRAY_TEXT_COLOR)), buttonComponent -> {
            showingOutput = !showingOutput;
            if (showingOutput) {
                overlay.removeChild(insertionContainer);
                overlay.child(extractionContainer);
            } else {
                overlay.removeChild(extractionContainer);
                overlay.child(insertionContainer);
            }
        });
        toggleButton.horizontalSizing(Sizing.fixed(60));
        toggleButton.renderer(ItemFilterScreen.createToggleRenderer(ignored -> showingOutput));
        toggleButton.textShadow(false);
        overlay.child(toggleButton.positioning(Positioning.absolute(panelXPos, 5)));
        
        if (this.menu.blockEntity instanceof UnstableContainerBlockEntity unstableContainer) {
            var container = (DynamicEnergyStorage) unstableContainer.getEnergyStorageForMultiblock(null);
            var capacity = container.maxInsert;
            var capacityMultiplier = capacity / (UnstableContainerBlockEntity.BASE_CAPACITY * unstableContainer.qualityMultiplier);   // in percent, exponential
            var laserIcon = Components.item(new ItemStack(BlockContent.LASER_ARM_BLOCK.asItem()));
            var laserLabel = Components.label(Component.literal("x" + String.format("%.1f", capacityMultiplier)));
            
            Collection<Component> tooltipText = List.of(Component.translatable("tooltip.oritech.unstable_laser_tooltip"), Component.translatable("tooltip.oritech.unstable_laser_tooltip.2"));
            
            laserIcon.tooltip(tooltipText);
            laserLabel.tooltip(tooltipText);
            
            var laserContainer = Containers.verticalFlow(Sizing.fixed(44), Sizing.fixed(37));
            laserContainer.surface(Surface.PANEL_INSET);
            laserContainer.child(laserIcon.margins(Insets.of(2, 0, 0, 0)));
            laserContainer.child(laserLabel.margins(Insets.of(4, 2, 4, 4)));
            laserContainer.horizontalAlignment(HorizontalAlignment.CENTER);
            
            overlay.child(laserContainer.positioning(Positioning.absolute(27, 5)));
            
            var containedIcon = Components.item(new ItemStack(unstableContainer.capturedBlock.getBlock().asItem()));
            var containedLabel = Components.label(Component.literal("x" + unstableContainer.qualityMultiplier));
            var containedTooltipText = Component.translatable("tooltip.oritech.unstable_contained_tooltip");
            containedIcon.tooltip(containedTooltipText);
            containedLabel.tooltip(containedTooltipText);
            
            var containedContainer = Containers.verticalFlow(Sizing.fixed(44), Sizing.content());
            containedContainer.surface(Surface.PANEL_INSET);
            containedContainer.child(containedIcon.margins(Insets.of(2, 0, 0, 0)));
            containedContainer.child(containedLabel.margins(Insets.of(4, 2, 4, 4)));
            containedContainer.horizontalAlignment(HorizontalAlignment.CENTER);
            
            overlay.child(containedContainer.positioning(Positioning.absolute(27, 11 + 33 + 2)));
        }
        
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        var entity = this.menu.blockEntity;
        var statistics = (entity instanceof ExpandableEnergyStorageBlockEntity) ? ((ExpandableEnergyStorageBlockEntity) entity).currentStats : ((UnstableContainerBlockEntity) entity).currentStats;
        if (statistics == null) return;
        
        var updateAll = this.menu.worldAccess.getGameTime() % 4 == 0;
        
        if (updateAll) {
            inAvgSecond.text(Component.translatable("title.oritech.energy.inAvgSecond", TooltipHelper.getEnergyText((long) statistics.avgInsertSecond())));
            inSources.text(Component.translatable("title.oritech.energy.inSources", statistics.insertionCountLastTick()));
            inPeak.text(Component.translatable("title.oritech.energy.inPeak", TooltipHelper.getEnergyText(statistics.maxInsertSecond())));
            outAvgSecond.text(Component.translatable("title.oritech.energy.outAvgSecond", TooltipHelper.getEnergyText((long) statistics.avgExtractSecond())));
            outPeak.text(Component.translatable("title.oritech.energy.outPeak", TooltipHelper.getEnergyText(statistics.maxExtractSecond())));
        }
        
        inLastTick.text(Component.translatable("title.oritech.energy.inLastTick", TooltipHelper.getEnergyText(statistics.insertedLastTickTotal())));
        outLastTick.text(Component.translatable("title.oritech.energy.outLastTick", TooltipHelper.getEnergyText(statistics.extractedLastTickTotal())));
        
        
    }
    
    @Override
    public boolean useHighTitle() {
        return true;
    }
    
    @Override
    public ItemStack getTitleIcon() {
        if (this.menu.blockEntity instanceof UnstableContainerBlockEntity) {
            return new ItemStack(ItemContent.UNSTABLE_CONTAINER);
        }
        return super.getTitleIcon();
    }
}
