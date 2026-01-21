package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.DronePortEntity;

public class DroneScreen extends UpgradableMachineScreen<DroneScreenHandler> {
    
    public static final ResourceLocation CARD_SLOT = Oritech.id("textures/gui/modular/designator_arrow.png");
    private final DronePortEntity dronePort;
    
    private String lastMessage = "";

    public DroneScreen(DroneScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        dronePort = (DronePortEntity) handler.blockEntity;
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        super.build(rootComponent);
        lastMessage = dronePort.getStatusMessage();
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        if (!dronePort.getStatusMessage().equals(lastMessage)) {
            var message = dronePort.getStatusMessage();
            lastMessage = message;
            
            var label = Components.label(Component.translatable(message).withStyle(ChatFormatting.BLACK));
            label.horizontalTextAlignment(HorizontalAlignment.CENTER);
            label.verticalTextAlignment(VerticalAlignment.CENTER);
            label.margins(Insets.of(8));
            
            var container = Containers.verticalFlow(Sizing.content(10), Sizing.content(10));
            container.child(label);
            container.sizing(Sizing.content(10));
            container.surface(ORITECH_PANEL);
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
