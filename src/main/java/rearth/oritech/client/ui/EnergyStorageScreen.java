package rearth.oritech.client.ui;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.api.screen.widgets.ToggleWidget;
import rearth.oritech.api.transfer.energy.DynamicStatisticEnergyStorage.EnergyStatistics;
import rearth.oritech.api.transfer.energy.EnergyHistory;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.entity.interaction.EnergyTransmissionPoleEntity;
import rearth.oritech.block.entity.storage.LargeStorageBlockEntity;
import rearth.oritech.block.entity.storage.SchrodingersSafeBlockEntity;
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
    private EnergyHistoryPopup historyPopup;

    public EnergyStorageScreen(T handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    public EnergyStorageScreen(T handler, Inventory inventory, Component title, int width, int height) {
        super(handler, inventory, title, width, height);
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        historyPopup = null;

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

        if (getEnergyHistory() != null) {
            // Share the existing header row with History without overlapping the output-limit slider below.
            toggleButton.setLabel(Component.empty());
            toggleButton.setWidth(34);
            toggleButton.withTooltip(Component.translatable("title.oritech.item_filter.toggle_energy_statistics"));
            addComponent(ButtonWidget.panel(panelXPos + 40, 5, 54, 16,
                    Component.translatable("title.oritech.energy.history"), button -> openHistory()));
        }
        updatePanelVisibility();
    }

    // The portable and creative blocks share this screen but intentionally don't collect long-term history.
    private EnergyHistory getEnergyHistory() {
        return switch (menu.blockEntity) {
            case LargeStorageBlockEntity storage -> storage.energyHistory;
            case SchrodingersSafeBlockEntity safe -> safe.energyHistory;
            case EnergyTransmissionPoleEntity pole -> pole.energyHistory;
            default -> null;
        };
    }

    private void openHistory() {
        closeHistory();
        historyPopup = new EnergyHistoryPopup(width, height, -leftPos, -topPos, this::getEnergyHistory,
                menu.blockEntity instanceof EnergyTransmissionPoleEntity, this::closeHistory);
        addComponent(historyPopup);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (historyPopup != null) {
            // Escape dismisses the popup first; other keys must not operate inventory slots behind it.
            if (event.isEscape()) closeHistory();
            return true;
        }
        return super.keyPressed(event);
    }

    private void closeHistory() {
        if (historyPopup != null) removeComponent(historyPopup);
        historyPopup = null;
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
        return (entity instanceof ExpandableEnergyStorageBlockEntity) ? ((ExpandableEnergyStorageBlockEntity) entity).currentStats : ((SchrodingersSafeBlockEntity) entity).currentStats;
    }

    @Override
    public ItemStack getTitleIcon() {
        if (this.menu.blockEntity instanceof SchrodingersSafeBlockEntity) {
            return new ItemStack(ItemContent.SCHRODINGERS_SAFE.asItem());
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
