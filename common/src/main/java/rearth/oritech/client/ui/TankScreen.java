package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.Oritech;

public class TankScreen extends BasicMachineScreen<BasicMachineScreenHandler> {
    
    public static final ResourceLocation TANK_ARROWS = Oritech.id("textures/gui/modular/tank_arrows.png");
    
    public TankScreen(BasicMachineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        var arrowIndicator = Components.texture(TANK_ARROWS, 0, 0, 7, 17, 7, 17);
        overlay.child(arrowIndicator.positioning(Positioning.absolute(95 - 29 - 9, 19 + 20 - 3)).sizing(Sizing.fixed(12), Sizing.fixed(23)));
        
    }
}
