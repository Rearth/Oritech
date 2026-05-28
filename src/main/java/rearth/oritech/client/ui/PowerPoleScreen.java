package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.transfer.energy.DynamicStatisticEnergyStorage;
import rearth.oritech.block.entity.interaction.PowerPoleEntity;
import rearth.oritech.init.BlockContent;

public class PowerPoleScreen extends EnergyStorageScreen<UpgradableOritechScreenHandler> {
    
    public PowerPoleScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        
        var powerPoleEntity = (PowerPoleEntity) this.menu.blockEntity;
        var connectionCount = powerPoleEntity.getConnections().size();
        var isConnected = connectionCount > 0;
        var containedTooltipText = Component.translatable("tooltip.oritech.power_pole_connection_" + (isConnected ? "enabled" : "disabled"));
        
        // Note: previously this anonymous BlockWidget subclass overrode appleRotation(PoseStack)
        // to apply a custom -30°/180°/45° XYZ rotation. That hook no longer exists because the
        // BlockWidget itself was rewritten around a PIP renderer (currently stubbed) for 26.1.
        var connectedIcon = new BlockWidget(24, 3, 50, BlockContent.TECH_LEVER.defaultBlockState().setValue(LeverBlock.POWERED, isConnected));
        connectedIcon.withTooltip(containedTooltipText);
        
        var connectedLabel = new LabelWidget(7, 53, 84, 18,
          Component.translatable("title.oritech.power_pole_connection_" + (isConnected ? "enabled" : "disabled"), connectionCount))
                               .withDarkColor()
                               .withAlignment(LabelWidget.Alignment.CENTER)
                               .withTooltip(containedTooltipText);
        
        addComponent(connectedIcon);
        addComponent(connectedLabel);
    }
    
    @Override
    public DynamicStatisticEnergyStorage.EnergyStatistics getStatistics(BlockEntity entity) {
        return ((PowerPoleEntity) entity).currentStats;
    }
}
