package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.energy.containers.DynamicStatisticEnergyStorage.EnergyStatistics;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.api.screen.widgets.ToggleWidget;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.List;

public abstract class EnergyStorageScreen<T extends UpgradableOritechScreenHandler> extends UpgradableOritechScreen<T> {
    
    private LabelWidget inAvgSecond;
    private LabelWidget inLastTick;
    private LabelWidget inSources;
    private LabelWidget inPeak;
    private LabelWidget outAvgSecond;
    private LabelWidget outLastTick;
    private LabelWidget outPeak;
    private final List<UIComponent> insertionWidgets = new ArrayList<>();
    private final List<UIComponent> extractionWidgets = new ArrayList<>();
    private boolean showingOutput;
    
    public EnergyStorageScreen(T handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        
        var panelXPos = 74;
        
        addStatsPanel(insertionWidgets, panelXPos, 23, 94, 56);
        inLastTick = createPanelLabel(panelXPos + 6, 29, "title.oritech.energy.inLastTick.tooltip");
        inAvgSecond = createPanelLabel(panelXPos + 6, 41, "title.oritech.energy.inAvgSecond.tooltip");
        inPeak = createPanelLabel(panelXPos + 6, 53, "title.oritech.energy.inPeak.tooltip");
        inSources = createPanelLabel(panelXPos + 6, 65, "title.oritech.energy.inSources.tooltip");
        addGrouped(insertionWidgets, inLastTick, inAvgSecond, inPeak, inSources);
        
        addStatsPanel(extractionWidgets, panelXPos, 23, 94, 44);
        outLastTick = createPanelLabel(panelXPos + 6, 29, "title.oritech.energy.outLastTick.tooltip");
        outAvgSecond = createPanelLabel(panelXPos + 6, 41, "title.oritech.energy.outAvgSecond.tooltip");
        outPeak = createPanelLabel(panelXPos + 6, 53, "title.oritech.energy.outPeak.tooltip");
        addGrouped(extractionWidgets, outLastTick, outAvgSecond, outPeak);
        
        var toggleButton = ToggleWidget.of(panelXPos, 5,
            Component.translatable("title.oritech.item_filter.toggle_energy_statistics").withColor(LabelWidget.DARK_TEXT),
            showingOutput,
            (button, value) -> {
                showingOutput = value;
                updatePanelVisibility();
            })
                             .withTextColor(LabelWidget.DARK_TEXT)
                             .withTextShadow(false);
        addComponent(toggleButton);
        
        updatePanelVisibility();
    }
    
    @Override
    protected void tickExtra() {
        super.tickExtra();
        
        var entity = this.menu.blockEntity;
        var statistics = getStatistics(entity);
        if (statistics == null) return;
        
        var updateAll = this.menu.worldAccess.getGameTime() % 4 == 0;
        
        if (updateAll) {
            inAvgSecond.setText(Component.translatable("title.oritech.energy.inAvgSecond", TooltipHelper.getEnergyText((long) statistics.avgInsertSecond())));
            inSources.setText(Component.translatable("title.oritech.energy.inSources", statistics.insertionCountLastTick()));
            inPeak.setText(Component.translatable("title.oritech.energy.inPeak", TooltipHelper.getEnergyText(statistics.maxInsertSecond())));
            outAvgSecond.setText(Component.translatable("title.oritech.energy.outAvgSecond", TooltipHelper.getEnergyText((long) statistics.avgExtractSecond())));
            outPeak.setText(Component.translatable("title.oritech.energy.outPeak", TooltipHelper.getEnergyText(statistics.maxExtractSecond())));
        }
        
        inLastTick.setText(Component.translatable("title.oritech.energy.inLastTick", TooltipHelper.getEnergyText(statistics.insertedLastTickTotal())));
        outLastTick.setText(Component.translatable("title.oritech.energy.outLastTick", TooltipHelper.getEnergyText(statistics.extractedLastTickTotal())));
    }
    
    protected EnergyStatistics getStatistics(BlockEntity entity) {
        return (entity instanceof ExpandableEnergyStorageBlockEntity) ? ((ExpandableEnergyStorageBlockEntity) entity).currentStats : ((UnstableContainerBlockEntity) entity).currentStats;
    }
    
    @Override
    public ItemStack getTitleIcon() {
        if (this.menu.blockEntity instanceof UnstableContainerBlockEntity) {
            return new ItemStack(ItemContent.UNSTABLE_CONTAINER);
        }
        return super.getTitleIcon();
    }
    
    private void addStatsPanel(List<UIComponent> group, int x, int y, int width, int height) {
        var panel = new SurfaceWidget(x, y, width, height);
        panel.withSurface(OritechSurface.PANEL_INSET);
        group.add(panel);
        addComponent(panel);
    }
    
    private LabelWidget createPanelLabel(int x, int y, String tooltipKey) {
        var label = new LabelWidget(x, y, 84, 9, Component.literal(""));
        label.withTooltip(Component.translatable(tooltipKey));
        return label;
    }
    
    private void addGrouped(List<UIComponent> group, UIComponent... widgets) {
        for (var widget : widgets) {
            group.add(widget);
            addComponent(widget);
        }
    }
    
    private void updatePanelVisibility() {
        for (var widget : insertionWidgets)
            widget.setVisible(!showingOutput);
        for (var widget : extractionWidgets)
            widget.setVisible(showingOutput);
    }
}
