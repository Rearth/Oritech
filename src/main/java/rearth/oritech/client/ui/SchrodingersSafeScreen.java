package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.block.entity.storage.SchrodingersSafeBlockEntity;
import rearth.oritech.init.BlockContent;

import java.util.List;

public class SchrodingersSafeScreen extends EnergyStorageScreen<UpgradableOritechScreenHandler> {

    public SchrodingersSafeScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        addSchrodingersSafePanel();
    }

    private void addSchrodingersSafePanel() {
        if (this.menu.blockEntity instanceof SchrodingersSafeBlockEntity schrodingersSafe) {
            var container = schrodingersSafe.getEnergyStorageForMultiblock(null);
            var capacity = container.maxInsert;
            var capacityMultiplier = capacity / (SchrodingersSafeBlockEntity.BASE_CAPACITY * schrodingersSafe.qualityMultiplier);   // in percent, exponential
            var tooltipText = List.of(
                    Component.translatable("tooltip.oritech.unstable_laser_tooltip"),
                    Component.translatable("tooltip.oritech.unstable_laser_tooltip.2"));

            var laserIcon = new ItemWidget(36, 6, 22, new ItemStack(BlockContent.ENDERIC_LASER.asItem()));
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
            var containedIcon = new ItemWidget(38, 46, 20, new ItemStack(schrodingersSafe.capturedBlock.getBlock().asItem()));
            containedIcon.withTooltip(containedTooltipText);
            containedIcon.withTooltipFromStack(false);
            var containedLabel = new LabelWidget(27, 69, 40, 9,
                    Component.literal("x" + schrodingersSafe.qualityMultiplier));
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
