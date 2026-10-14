package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.api.screen.data.EnergyDisplayWidget;
import rearth.oritech.api.screen.widgets.ToggleWidget;
import rearth.oritech.block.entity.interaction.DestroyerBlockEntity;
import rearth.oritech.config.OritechConfig;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DestroyerScreen extends UpgradableOritechScreen<UpgradableOritechScreenHandler> {

    private ToggleWidget removeFluidsToggle;

    public DestroyerScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        var destroyer = (DestroyerBlockEntity) menu.blockEntity;
        removeFluidsToggle = ToggleWidget.of(30, 63,
                Component.translatable("title.oritech.destroyer.remove_fluids"), destroyer.removeFluids,
                (button, value) -> {
                    destroyer.removeFluids = value;
                    ClientPacketDistributor.sendToServer(new DestroyerBlockEntity.FluidRemovalPacket(menu.blockPos, value));
                });
        removeFluidsToggle.withTooltip(Component.translatable("tooltip.oritech.destroyer.remove_fluids"));
        removeFluidsToggle.setVisible(destroyer.range > 1);
        addComponent(removeFluidsToggle);
        updateEnergyTooltips();
    }

    @Override
    protected void tickExtra() {
        super.tickExtra();
        var destroyer = (DestroyerBlockEntity) menu.blockEntity;
        removeFluidsToggle.setValue(destroyer.removeFluids);
        updateEnergyTooltips();
    }

    private void updateEnergyTooltips() {
        var destroyer = (DestroyerBlockEntity) menu.blockEntity;
        var miningCost = destroyer.getOperationEnergyUsagePerTick();
        var movingCost = destroyer.getMoveEnergyUsagePerTick();
        var peakCost = (long) Math.max(miningCost, movingCost);
        var minimumStored = (long) Math.ceil(Math.max(miningCost, movingCost));
        var storage = destroyer.energyStorage;
        var addonData = destroyer.getBaseAddonData();
        var speedFactor = 1 / addonData.speed();
        var costFactor = addonData.efficiency();

        var rates = Component.translatable("tooltip.oritech.destroyer.energy_rates",
                formatNumber(peakCost), formatNumber((long) miningCost), formatNumber((long) movingCost),
                formatNumber(minimumStored), formatNumber(storage.maxInsert));
        var tooltip = DisplayDataSource.getEnergyTooltip(storage.energy, storage.capacity, 0, 0, false, false).copy()
                .append("\n\n").append(rates)
                .append(Component.translatable("tooltip.oritech.destroyer.energy_formula",
                        formatNumber(destroyer.getOperationEnergyUsage()), formatNumber(speedFactor), formatNumber(costFactor),
                        formatNumber(destroyer.getMoveEnergyUsage()), formatNumber(speedFactor), formatNumber(costFactor)));

        if (destroyer.range > 1) {
            tooltip.append(Component.translatable("tooltip.oritech.destroyer.quarry_energy",
                    formatNumber(OritechConfig.destroyerConfig.workEnergyUsage.get()), formatNumber(destroyer.getOperationEnergyUsage()),
                    formatNumber(OritechConfig.destroyerConfig.moveEnergyUsage.get()), formatNumber(destroyer.getMoveEnergyUsage())));
        }
        if (storage.maxInsert < peakCost) {
            tooltip.append(Component.translatable("tooltip.oritech.destroyer.energy_input_shortfall"));
        }
        for (var component : components) {
            if (component instanceof EnergyDisplayWidget) component.setTooltip(List.of(tooltip));
        }
        if (speedLabel != null) {
            speedLabel.withTooltip(Component.translatable("tooltip.oritech.destroyer.speed", formatNumber(speedFactor)));
        }
        if (efficiencyLabel != null) {
            efficiencyLabel.withTooltip(Component.translatable("tooltip.oritech.destroyer.efficiency", formatNumber(costFactor)));
        }
    }

    private static String formatNumber(double value) {
        var format = NumberFormat.getNumberInstance(Locale.ROOT);
        format.setMaximumFractionDigits(4);
        return format.format(value);
    }
}
