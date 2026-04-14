package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.init.BlockContent;

import java.util.List;

public class UnstableContainerScreen extends EnergyStorageScreen<UpgradableOritechScreenHandler> {
    
    public UnstableContainerScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        
        addUnstableContainerPanel();
    }
    
    private void addUnstableContainerPanel() {
        if (this.menu.blockEntity instanceof UnstableContainerBlockEntity unstableContainer) {
            var container = (DynamicEnergyStorage) unstableContainer.getEnergyStorageForMultiblock(null);
            var capacity = container.maxInsert;
            var capacityMultiplier = capacity / (UnstableContainerBlockEntity.BASE_CAPACITY * unstableContainer.qualityMultiplier);   // in percent, exponential
            var tooltipText = List.of(
              Component.translatable("tooltip.oritech.unstable_laser_tooltip"),
              Component.translatable("tooltip.oritech.unstable_laser_tooltip.2"));
            
            var laserIcon = new ItemWidget(36, 6, 22, new ItemStack(BlockContent.LASER_ARM_BLOCK.asItem()));
            laserIcon.withTooltip(tooltipText.toArray(Component[]::new));
            laserIcon.withShowOverlay(false);
            laserIcon.withTooltipFromStack(false);
            var laserLabel = new LabelWidget(27, 30, 40, 9,
              Component.literal("x" + String.format("%.1f", capacityMultiplier)));
            laserLabel.withTooltip(tooltipText.toArray(Component[]::new));
            laserLabel.withAlignment(LabelWidget.Alignment.CENTER);
            
            addInsetInfoBox(27, 5,
              laserIcon,
              laserLabel);
            
            var containedTooltipText = Component.translatable("tooltip.oritech.unstable_contained_tooltip");
            var containedIcon = new ItemWidget(38, 46, 20, new ItemStack(unstableContainer.capturedBlock.getBlock().asItem()));
            containedIcon.withTooltip(containedTooltipText);
            containedIcon.withTooltipFromStack(false);
            var containedLabel = new LabelWidget(27, 69, 40, 9,
              Component.literal("x" + unstableContainer.qualityMultiplier));
            containedLabel.withTooltip(containedTooltipText);
            containedLabel.withAlignment(LabelWidget.Alignment.CENTER);
            addInsetInfoBox(27, 44,
              containedIcon,
              containedLabel);
        }
    }
    
    private void addInsetInfoBox(int x, int y, UIComponent icon, UIComponent label) {
        var panel = new SurfaceWidget(x, y, 40, 35);
        panel.withSurface(OritechSurface.PANEL_INSET);
        addComponent(panel);
        addComponent(icon);
        addComponent(label);
    }
}
