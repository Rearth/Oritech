package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.ItemSlotWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.api.screen.widgets.TextureWidget;
import rearth.oritech.block.entity.interaction.DronePortEntity;

public class DroneScreen extends UpgradableOritechScreen<DroneScreenHandler> {
    
    public static final ResourceLocation CARD_SLOT = Oritech.id("textures/gui/modular/designator_arrow.png");
    private final DronePortEntity dronePort;
    private SurfaceWidget messagePanel;
    private LabelWidget messageLabel;
    
    private String lastMessage = "";

    public DroneScreen(DroneScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        dronePort = (DronePortEntity) handler.blockEntity;
    }
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        addComponent(new ItemSlotWidget(129, 26));
        addComponent(new ItemSlotWidget(129, 62));
        addComponent(new TextureWidget(128, 25, 18, 40, CARD_SLOT, 18, 40));

        messagePanel = new SurfaceWidget(18, 16, 140, 22);
        messagePanel.withSurface(OritechSurface.PANEL);
        messagePanel.withZIndex(200);
        addComponent(messagePanel);

        messageLabel = new LabelWidget(24, 23, 128, 10, Component.empty());
        messageLabel.withAlignment(LabelWidget.Alignment.CENTER);
        messageLabel.withDarkColor();
        messageLabel.withZIndex(201);
        addComponent(messageLabel);

        lastMessage = null;
        updateStatusMessage();
    }

    @Override
    protected void tickExtra() {
        super.tickExtra();
        updateStatusMessage();
    }

    private void updateStatusMessage() {
        var message = dronePort.getStatusMessage();
        if (message.equals(lastMessage)) return;

        lastMessage = message;
        var hasMessage = !message.isBlank();
        messagePanel.setVisible(hasMessage);
        messageLabel.setVisible(hasMessage);

        if (hasMessage) {
            messageLabel.setText(Component.translatable(message).withStyle(ChatFormatting.BLACK));
        }
    }
}
