package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.block.entity.interaction.DronePortEntity;
import rearth.oritech.util.ColorHelper;

public class DroneScreen extends UpgradableOritechScreen<DroneScreenHandler> {
    
    public static final ResourceLocation CARD_SLOT = Oritech.id("textures/gui/modular/designator_arrow.png");

    private final DronePortEntity dronePort;
    private OverlayWidget messageOverlay;
    private LabelWidget messageLabel;
    
    private String lastMessage = "";

    public DroneScreen(DroneScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        dronePort = (DronePortEntity) handler.blockEntity;
    }
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        addComponent(new ItemSlotWidget(130, 26));
        addComponent(new ItemSlotWidget(130, 62));
        addComponent(new TextureWidget(129, 26, 18, 40, CARD_SLOT, 18, 40));

        messageOverlay = new OverlayWidget(width, height);
        messageOverlay.setPosition(-leftPos, -topPos);
        messageOverlay.withBackgroundColor(ColorHelper.argb(0.3f, 0.3f, 0.3f, 0.5f));
        messageOverlay.withDismissHandler(() -> removeComponent(messageOverlay));

        messageLabel = new LabelWidget(10, 23, 150, 40, Component.empty());
        messageLabel.withAlignment(LabelWidget.Alignment.CENTER);
        messageLabel.withSurface(OritechSurface.PANEL);
        messageLabel.withPadding(Insets.of(6));
        messageLabel.withWrap(true);
        messageLabel.withDarkColor();
        messageLabel.withZIndex(201);
        messageOverlay.addChild(messageLabel);

        lastMessage = dronePort.getStatusMessage();
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

        if (hasMessage) {
            addComponent(messageOverlay);
            var translated = Component.translatable(message).withStyle(ChatFormatting.BLACK);
            messageLabel.setText(translated);

            var lineCount = Math.max(1, Minecraft.getInstance().font.split(translated, messageLabel.getWidth()).size());
            var textHeight = lineCount * Minecraft.getInstance().font.lineHeight;
            var panelHeight = Math.max(22, textHeight + 14);

            messageLabel.setHeight(textHeight);
            messageLabel.setY(16 + Math.max(7, (panelHeight - textHeight) / 2));
        }
    }
}
