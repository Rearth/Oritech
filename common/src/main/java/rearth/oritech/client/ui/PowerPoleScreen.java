package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.api.energy.containers.DynamicStatisticEnergyStorage;
import rearth.oritech.block.entity.interaction.PowerPoleEntity;
import rearth.oritech.client.ui.components.CustomBlockComponent;
import rearth.oritech.init.BlockContent;

public class PowerPoleScreen extends EnergyStorageScreen {
    
    public PowerPoleScreen(UpgradableMachineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        var powerPoleEntity = (PowerPoleEntity) this.menu.blockEntity;
        var connectionCount = powerPoleEntity.getConnections().size();
        var isConnected = connectionCount > 0;
        
        // show connection status
        var connectedIcon = new CustomBlockComponent(BlockContent.TECH_LEVER.defaultBlockState().setValue(LeverBlock.POWERED, isConnected), null);
        var connectedLabel = Components.label(Component.translatable("title.oritech.power_pole_connection_" + (isConnected ? "enabled" : "disabled"), connectionCount).withColor(BasicMachineScreen.GRAY_TEXT_COLOR));
        var containedTooltipText = Component.translatable("tooltip.oritech.power_pole_connection_" + (isConnected ? "enabled" : "disabled"));
        connectedIcon.tooltip(containedTooltipText);
        connectedLabel.tooltip(containedTooltipText);
        
        connectedIcon.sizing(Sizing.fixed(40), Sizing.fixed(40));
        
        var connectedContainer = Containers.verticalFlow(Sizing.fixed(44), Sizing.content());
        connectedContainer.child(connectedIcon.margins(Insets.of(4, 2, 4, 4)));
        connectedContainer.child(connectedLabel.margins(Insets.of(4, 2, 4, 4)));
        connectedContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        
        overlay.child(connectedContainer.positioning(Positioning.absolute(27, 5)));
        
    }
    
    @Override
    public DynamicStatisticEnergyStorage.EnergyStatistics getStatistics(BlockEntity entity) {
        return ((PowerPoleEntity) entity).currentStats;
    }
}
