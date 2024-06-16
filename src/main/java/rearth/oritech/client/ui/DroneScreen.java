package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.interaction.DronePortEntity;

public class DroneScreen extends BasicMachineScreen<DroneScreenHandler> {
    
    public static final Identifier CARD_SLOT = Oritech.id("textures/gui/modular/designator_arrow.png");
    private final DronePortEntity dronePort;
    
    public DroneScreen(DroneScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        dronePort = (DronePortEntity) handler.blockEntity;
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
        if (dronePort.getStatusMessage() != null) {
            var message = dronePort.getStatusMessage();
            dronePort.setStatusMessage(null);
            
            var label = Components.label(Text.literal(message).formatted(Formatting.BLACK));
            label.horizontalTextAlignment(HorizontalAlignment.CENTER);
            label.verticalTextAlignment(VerticalAlignment.CENTER);
            label.margins(Insets.of(8));
            
            var container = Containers.verticalFlow(Sizing.content(10), Sizing.content(10));
            container.child(label);
            container.sizing(Sizing.content(10));
            container.surface(Surface.PANEL);
            container.positioning(Positioning.relative(50, 30));
            container.zIndex(7000);
            
            var messagePanel = Containers.overlay(container);
            root.child(messagePanel);
            
        }
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        overlay.child(BasicMachineScreen.getItemFrame(129, 26));
        overlay.child(BasicMachineScreen.getItemFrame(129, 62));
        overlay.child(Components.texture(CARD_SLOT, 0, 0, 18, 40, 18, 40).positioning(Positioning.absolute(128, 25)));
        
    }
}
