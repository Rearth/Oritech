package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.transfer.energy.DynamicStatisticEnergyStorage;
import rearth.oritech.block.entity.interaction.EnergyTransmissionPoleEntity;
import rearth.oritech.init.BlockContent;

public class EnergyTransmissionPoleScreen extends EnergyStorageScreen<UpgradableOritechScreenHandler> {

    public EnergyTransmissionPoleScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        var energyTransmissionPoleEntity = (EnergyTransmissionPoleEntity) this.menu.blockEntity;
        var connectionCount = energyTransmissionPoleEntity.getConnections().size();
        var isConnected = connectionCount > 0;
        var containedTooltipText = Component.translatable("tooltip.oritech.energy_transmission_pole_connection_" + (isConnected ? "enabled" : "disabled"));

        var connectedIcon = new BlockWidget(28, 3, 50, BlockContent.INDUSTRIAL_LEVER.get().defaultBlockState().setValue(LeverBlock.POWERED, isConnected));
        connectedIcon.withTooltip(containedTooltipText);
        connectedIcon.withRotation(30 - 45, 225 - 45 + 20);

        var connectedLabel = new LabelWidget(7, 53, 84, 18,
                Component.translatable("title.oritech.energy_transmission_pole_connection_" + (isConnected ? "enabled" : "disabled"), connectionCount))
                .withDarkColor()
                .withAlignment(LabelWidget.Alignment.CENTER)
                .withTooltip(containedTooltipText);

        addComponent(connectedIcon);
        addComponent(connectedLabel);
    }

    @Override
    public DynamicStatisticEnergyStorage.EnergyStatistics getStatistics(BlockEntity entity) {
        return ((EnergyTransmissionPoleEntity) entity).currentStats;
    }
}
