package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.TextureWidget;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;

public class AcceleratorScreen extends OritechMachineScreen<AcceleratorScreenHandler> {
    
    public static final ResourceLocation PARTICLE_OVERLAY = Oritech.id("textures/gui/modular/particle_background_arrow.png");
    
    private LabelWidget titleLabel;
    private LabelWidget speedValueLabel;
    private LabelWidget statusLabel;
    private ItemWidget activeParticleRenderer;
    
    public AcceleratorScreen(AcceleratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    protected void addExtraComponents() {
        var shownItem = ItemStack.EMPTY;
        if (menu.accelerator.lastEvent.activeParticle() != ItemStack.EMPTY)
            shownItem = menu.accelerator.lastEvent.activeParticle();

        activeParticleRenderer = new ItemWidget(7, 34, shownItem);
        addComponent(activeParticleRenderer);
        addComponent(new TextureWidget(3, 27, 24, 30, PARTICLE_OVERLAY, 24, 30));

        titleLabel = new LabelWidget(34, 15, 126, 10, Component.literal("Waiting...").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
        titleLabel.withAlignment(LabelWidget.Alignment.CENTER);
        titleLabel.withDarkColor();

        speedValueLabel = new LabelWidget(37, 40, 133, 10, Component.literal(" "));
        speedValueLabel.withDarkColor();
        speedValueLabel.withAlignment(LabelWidget.Alignment.RIGHT);

        statusLabel = new LabelWidget(37, 52, 133, 30, Component.literal(" "));
        statusLabel.withAlignment(LabelWidget.Alignment.RIGHT);
        statusLabel.withWrap(true);
        statusLabel.withDarkColor();

        addComponent(titleLabel);
        addComponent(speedValueLabel);
        addComponent(statusLabel);
    }
    
    private void updateItemParticle() {
        var shownItem = ItemStack.EMPTY;
        if (menu.accelerator.lastEvent.lastEvent().equals(AcceleratorControllerBlockEntity.ParticleEvent.ACCELERATING) && menu.accelerator.lastEvent.activeParticle() != ItemStack.EMPTY)
            shownItem = menu.accelerator.lastEvent.activeParticle();
        
        activeParticleRenderer.setStack(shownItem);
    }
    
    @Override
    protected void tickExtra() {
        var event = menu.accelerator.lastEvent;
        
        switch (event.lastEvent()) {
            case IDLE -> {
                var text = Component.translatable("text.oritech.accelerator.ui.waiting.0");
                var time = menu.blockEntity.getLevel().getGameTime();
                if ((time / 20) % 3 == 1) text = Component.translatable("text.oritech.accelerator.ui.waiting.1");
                if ((time / 20) % 3 == 2) text = Component.translatable("text.oritech.accelerator.ui.waiting.2");
                text = text.withStyle(ChatFormatting.BOLD, ChatFormatting.BLACK);
                titleLabel.withAlignment(LabelWidget.Alignment.LEFT);
                titleLabel.setText(text);
                speedValueLabel.setText(Component.translatable("text.oritech.accelerator.ui.waiting").withStyle(ChatFormatting.BLACK));
                statusLabel.setText(Component.literal(" "));
            }
            case ERROR -> {
                titleLabel.withAlignment(LabelWidget.Alignment.CENTER);
                titleLabel.setText(Component.translatable("text.oritech.accelerator.ui.error").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedValueLabel.setText(Component.translatable("text.oritech.accelerator.ui.nogate").withStyle(ChatFormatting.BLACK));
                statusLabel.setText(Component.translatable("text.oritech.accelerator.ui.nogate.more").withStyle(ChatFormatting.DARK_GRAY));
            }
            case ACCELERATING -> {
                var speed = event.lastEventSpeed();
                var gateDist = format(AcceleratorParticleLogic.getMaxGateDist(speed), 1);
                var curveDist = format(AcceleratorParticleLogic.getRequiredBendDist(speed), 1);
                titleLabel.withAlignment(LabelWidget.Alignment.CENTER);
                titleLabel.setText(Component.translatable("text.oritech.accelerator.ui.accelerating").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedValueLabel.setText(Component.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).withStyle(ChatFormatting.BLACK));
                statusLabel.setText(Component.translatable("text.oritech.accelerator.ui.accelerating.stats", gateDist, curveDist).withStyle(ChatFormatting.DARK_GRAY));
            }
            case COLLIDED -> {
                titleLabel.withAlignment(LabelWidget.Alignment.CENTER);
                titleLabel.setText(Component.translatable("text.oritech.accelerator.ui.collision").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedValueLabel.setText(Component.translatable("text.oritech.accelerator.ui.collision.stats", format(event.lastEventSpeed(), 0)).withStyle(ChatFormatting.BLACK));
                statusLabel.setText(Component.translatable("text.oritech.accelerator.ui.collision.position", event.lastEventPosition().toShortString()).withStyle(ChatFormatting.DARK_GRAY));
            }
            case EXITED_FAST -> {
                var speed = event.lastEventSpeed();
                var curveDist = format(AcceleratorParticleLogic.getRequiredBendDist(speed), 1);
                titleLabel.withAlignment(LabelWidget.Alignment.CENTER);
                titleLabel.setText(Component.translatable("text.oritech.accelerator.ui.exited").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedValueLabel.setText(Component.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).withStyle(ChatFormatting.BLACK));
                statusLabel.setText(Component.translatable("text.oritech.accelerator.ui.exited.stats", event.lastEventPosition().toShortString(), curveDist, format(event.minBendDist(), 1)).withStyle(ChatFormatting.DARK_GRAY));
            }
            case EXITED_NO_GATE -> {
                var speed = event.lastEventSpeed();
                var gateDist = format(AcceleratorParticleLogic.getMaxGateDist(speed), 1);
                titleLabel.withAlignment(LabelWidget.Alignment.CENTER);
                titleLabel.setText(Component.translatable("text.oritech.accelerator.ui.exited").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedValueLabel.setText(Component.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).withStyle(ChatFormatting.BLACK));
                statusLabel.setText(Component.translatable("text.oritech.accelerator.ui.exited.nogate", event.lastEventPosition().toShortString(), gateDist).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        
        updateItemParticle();
    }
    
    private static String format(float number, int decimal) {
        if (decimal <= 0) return String.valueOf((int) number);
        var format = "%." + decimal + "f";
        return String.format(format, number);
    }
}
