package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.screen.widgets.BoxWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.block.entity.processing.ArcaneRefineryBlockEntity;

public class ArcaneRefineryScreen extends OritechMachineScreen<ArcaneRefineryScreenHandler> {

    public ArcaneRefineryScreen(ArcaneRefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        var refinery = (ArcaneRefineryBlockEntity) menu.blockEntity;
        
        
        addComponent(BoxWidget.filled(87, 6, 1, 73, LabelWidget.DARK_TEXT));
        
    }
}
