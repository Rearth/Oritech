package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.widgets.BoxWidget;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ProgressBarWidget;
import rearth.oritech.block.entity.processing.TaintedRefineryBlockEntity;
import rearth.oritech.util.ColorHelper;
import rearth.oritech.util.TooltipHelper;

import java.util.List;
import java.util.Locale;

public class TaintedRefineryScreen extends OritechMachineScreen<TaintedRefineryScreenHandler> {
    
    private ProgressBarWidget energyIntakeBar;
    private ProgressBarWidget sculkBar;
    private ProgressBarWidget arcaneBar;
    
    public TaintedRefineryScreen(TaintedRefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        
        var refinery = (TaintedRefineryBlockEntity) menu.blockEntity;
        
        var panelX = 92;
        var panelY = 8;
        var panelWidth = 76;
        var barHeight = 18;
        var barSpacing = 7;
        
        energyIntakeBar = new ProgressBarWidget(
          panelX,
          panelY,
          panelWidth,
          barHeight,
          Component.translatable("title.oritech.tainted_refinery.energy_intake"),
          this::getEnergyProgress,
          () -> {
              var energyProgress = this.getEnergyProgress();
              if (energyProgress > 0.5) return ProgressBarWidget.PRESET_GREEN;
              if (energyProgress > 0.2) return ProgressBarWidget.PRESET_ORANGE;
              return ProgressBarWidget.PRESET_RED;
          }).withTextColor(LabelWidget.DARK_TEXT).withPulsing(true);
        
        sculkBar = new ProgressBarWidget(
          panelX,
          panelY + barHeight + barSpacing,
          panelWidth,
          barHeight,
          Component.translatable("title.oritech.tainted_refinery.sculk_meter"),
          () -> refinery.sculkFactor.result(),
          () -> ColorHelper.argb(144 / 255f, 22 / 255f, 181 / 255f)).withTextColor(LabelWidget.DARK_TEXT);
        
        arcaneBar = new ProgressBarWidget(
          panelX,
          panelY + (barHeight + barSpacing) * 2,
          panelWidth,
          barHeight,
          Component.translatable("title.oritech.tainted_refinery.arcane_meter"),
          () -> refinery.arcaneFactor.result(),
          () -> ProgressBarWidget.PRESET_BLUE).withTextColor(LabelWidget.DARK_TEXT);
        
        addComponent(energyIntakeBar);
        addComponent(sculkBar);
        addComponent(arcaneBar);
        updateBarTooltips(refinery);
        
        addComponent(BoxWidget.filled(87, 6, 1, 73, LabelWidget.DARK_TEXT));
        
        // todo tooltip
        var currentActiveTank = refinery.selectedOutput;
        var cycleSlotButton = ButtonWidget.darkPanel(26, 54, 40, 20, Component.translatable("label.oritech.tainted_refinery.output_slot", currentActiveTank + 1), widget -> {
            var tankIndex = refinery.selectedOutput;
            var newIndex = (tankIndex + 1) % 3;
            widget.setLabel(Component.translatable("label.oritech.tainted_refinery.output_slot", newIndex + 1));
            NetworkManager.sendToServer(new TaintedRefineryBlockEntity.RefineryTankSelectorPacket(menu.blockPos, newIndex));
        }).withTextColor(LabelWidget.BRIGHT_TEXT).withTooltip(Component.translatable("tooltip.oritech.tainted_refinery.output_slot"));
        
        addComponent(cycleSlotButton);
        
    }
    
    private float getEnergyProgress() {
        // maps input RF/t to refinery bar
        // formula is 0.2 * (amount^0.5)
        // 64 = 1.6
        // 256 = 3.2
        // 1024 = 6.4
        // 10000 = 20
        // 100000 = 63
        // 1M = 200
        // 50M = 1414
        
        var refinery = (TaintedRefineryBlockEntity) menu.blockEntity;
        var energy = refinery.lastTickRFUsed;
        var progressTicks = refinery.getEnergyFactor(energy);
        
        return (float) Math.min(1, Math.log10(progressTicks) / 2.5);
    }
    
    @Override
    protected void tickExtra() {
        updateBarTooltips((TaintedRefineryBlockEntity) menu.blockEntity);
    }
    
    private void updateBarTooltips(TaintedRefineryBlockEntity refinery) {
        energyIntakeBar.setTooltip(List.of(
          Component.translatable("tooltip.oritech.tainted_refinery.energy_intake"),
          Component.translatable(
            "tooltip.oritech.tainted_refinery.energy_intake.rate",
            // "%s %s %s",
            TooltipHelper.getEnergyText(refinery.lastTickRFUsed),
            TooltipHelper.getEnergyText(refinery.getEnergyInputMapped(refinery.lastTickRFUsed)),
            TooltipHelper.getEnergyText(refinery.getEnergyInputMapped((long) (refinery.getArcaneEnergyMultiplier() * refinery.lastTickRFUsed))))));
        
        var sculkFactor = refinery.sculkFactor;
        sculkBar.setTooltip(List.of(
          Component.translatable(
            "tooltip.oritech.tainted_refinery.factor_strength",
            Component.translatable("title.oritech.tainted_refinery.sculk_meter"),
            formatPercent(sculkFactor.result())),
          Component.translatable("tooltip.oritech.tainted_refinery.factor_blocks", sculkFactor.sources().size()),
          Component.translatable("tooltip.oritech.tainted_refinery.factor_variants", sculkFactor.variants()),
          Component.translatable("tooltip.oritech.tainted_refinery.sculk_output", refinery.getOutputMultiplier())));
        
        var arcaneFactor = refinery.arcaneFactor;
        arcaneBar.setTooltip(List.of(
          Component.translatable(
            "tooltip.oritech.tainted_refinery.factor_strength",
            Component.translatable("title.oritech.tainted_refinery.arcane_meter"),
            formatPercent(arcaneFactor.result())),
          Component.translatable("tooltip.oritech.tainted_refinery.factor_blocks", arcaneFactor.sources().size()),
          Component.translatable("tooltip.oritech.tainted_refinery.factor_variants", arcaneFactor.variants()),
          Component.translatable("tooltip.oritech.tainted_refinery.arcane_boost", formatMultiplier(refinery.getArcaneEnergyMultiplier()))));
    }
    
    private static String formatPercent(float value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100f);
    }
    
    private static String formatMultiplier(float value) {
        return String.format(Locale.ROOT, "x%.2f", value);
    }
}
