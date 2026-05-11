package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import rearth.oritech.util.ColorHelper;

public class RefineryScreen extends OritechMachineScreen<RefineryScreenHandler> {

    public RefineryScreen(RefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        var refinery = (RefineryBlockEntity) menu.blockEntity;
        var moduleCount = refinery.getModuleCount();

        if (moduleCount < 1) {
            var blocker = new SurfaceWidget(92 + 27, 6, 21, 74, OritechSurface.PANEL_DARK);
            blocker.withTooltip(Component.translatable("tooltip.oritech.refinery_module_missing")).withZIndex(1);
            addComponent(blocker);
            
            var icon = new LabelWidget(92 + 27, 6 + 30, 21, Component.literal("❌"))
                         .withAlignment(LabelWidget.Alignment.CENTER)
                         .withColor(ColorHelper.argb(0.1f, 0.1f, 0.1f))
                         .withZIndex(1);
            addComponent(icon);
        }
        
        if (moduleCount < 2) {
            var blocker = new SurfaceWidget(92 + 27 * 2, 6, 21, 74, OritechSurface.PANEL_DARK);
            blocker.withTooltip(Component.translatable("tooltip.oritech.refinery_module_missing")).withZIndex(1);
            addComponent(blocker);
            
            var icon = new LabelWidget(92 + 27 * 2, 6 + 30, 21, Component.literal("❌"))
                         .withAlignment(LabelWidget.Alignment.CENTER)
                         .withColor(ColorHelper.argb(0.1f, 0.1f, 0.1f))
                         .withZIndex(1);
            addComponent(icon);
        }
    }
}
