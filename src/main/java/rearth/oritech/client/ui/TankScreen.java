package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.widgets.TextureWidget;

public class TankScreen extends OritechMachineScreen<OritechScreenHandler> {
    
    public static final Identifier TANK_ARROWS = Oritech.id("textures/gui/modular/tank_arrows.png");
    
    public TankScreen(OritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected void addExtraComponents() {
        addComponent(new TextureWidget(57, 36, 12, 23,
            TANK_ARROWS, 0, 0, 7, 17, 7, 17));

    }
}
