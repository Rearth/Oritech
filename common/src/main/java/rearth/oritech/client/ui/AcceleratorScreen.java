package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;

public class AcceleratorScreen extends BasicMachineScreen<AcceleratorScreenHandler> {
    
    public static final ResourceLocation PARTICLE_OVERLAY = Oritech.id("textures/gui/modular/particle_background_arrow.png");
    
    private LabelComponent titleLabel;
    private LabelComponent speedLabel;
    private LabelComponent statusLabel;
    private ItemComponent activeParticleRenderer;
    
    public AcceleratorScreen(AcceleratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        var shownItem = ItemStack.EMPTY;
        if (menu.accelerator.lastEvent.activeParticle() != ItemStack.EMPTY)
            shownItem = menu.accelerator.lastEvent.activeParticle();
        
        activeParticleRenderer = Components.item(shownItem);
        overlay.child(activeParticleRenderer.positioning(Positioning.absolute(7, 34)));
        
        var particleOverlay = Components.texture(PARTICLE_OVERLAY, 0, 0, 24, 30, 24, 30).sizing(Sizing.fixed(24), Sizing.fixed(30));
        particleOverlay.zIndex(-1);
        overlay.child(particleOverlay.positioning(Positioning.absolute(3, 27)));
        
        titleLabel = Components.label(Component.literal("Waiting...").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
        titleLabel.horizontalTextAlignment(HorizontalAlignment.CENTER).horizontalSizing(Sizing.fill()).verticalSizing(Sizing.fixed(10)).margins(Insets.of(1));
        
        speedLabel = Components.label(Component.literal("Speed: 0 blocks/s").withStyle(ChatFormatting.BLACK));
        speedLabel.margins(Insets.of(2));
        
        statusLabel = Components.label(Component.literal("Insert item to accelerate\nAnd some more details").withStyle(ChatFormatting.BLACK));
        statusLabel.horizontalTextAlignment(HorizontalAlignment.RIGHT).margins(Insets.of(2));
        
        var labelContainer = Containers.verticalFlow(Sizing.fixed(130), Sizing.fixed(100));
        labelContainer.child(titleLabel).child(speedLabel).child(statusLabel);
        labelContainer.horizontalAlignment(HorizontalAlignment.RIGHT);
        labelContainer.positioning(Positioning.relative(82, 30));
        
        overlay.child(labelContainer);
        
    }
    
    private void updateItemParticle() {
        var shownItem = ItemStack.EMPTY;
        if (menu.accelerator.lastEvent.lastEvent().equals(AcceleratorControllerBlockEntity.ParticleEvent.ACCELERATING) && menu.accelerator.lastEvent.activeParticle() != ItemStack.EMPTY)
            shownItem = menu.accelerator.lastEvent.activeParticle();
        
        activeParticleRenderer.stack(shownItem);
    }
    
    @Override
    protected void containerTick() {
        var event = menu.accelerator.lastEvent;
        titleLabel.horizontalTextAlignment(HorizontalAlignment.RIGHT);
        
        switch (event.lastEvent()) {
            case IDLE -> {
                var text = Component.translatable("text.oritech.accelerator.ui.waiting.0");
                var time = menu.blockEntity.getLevel().getGameTime();
                if ((time / 20) % 3 == 1) text = Component.translatable("text.oritech.accelerator.ui.waiting.1");
                if ((time / 20) % 3 == 2) text = Component.translatable("text.oritech.accelerator.ui.waiting.2");
                text = text.withStyle(ChatFormatting.BOLD, ChatFormatting.BLACK);
                titleLabel.horizontalTextAlignment(HorizontalAlignment.LEFT).text(text);
                speedLabel.text(Component.translatable("text.oritech.accelerator.ui.waiting").withStyle(ChatFormatting.BLACK));
                statusLabel.text(Component.literal(" "));
            }
            case ERROR -> {
                titleLabel.text(Component.translatable("text.oritech.accelerator.ui.error").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedLabel.text(Component.translatable("text.oritech.accelerator.ui.nogate").withStyle(ChatFormatting.BLACK));
                statusLabel.text(Component.translatable("text.oritech.accelerator.ui.nogate.more").withStyle(ChatFormatting.DARK_GRAY));
            }
            case ACCELERATING -> {
                var speed = event.lastEventSpeed();
                var gateDist = format(AcceleratorParticleLogic.getMaxGateDist(speed), 1);
                var curveDist = format(AcceleratorParticleLogic.getRequiredBendDist(speed), 1);
                titleLabel.text(Component.translatable("text.oritech.accelerator.ui.accelerating").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedLabel.text(Component.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).withStyle(ChatFormatting.BLACK));
                statusLabel.text(Component.translatable("text.oritech.accelerator.ui.accelerating.stats", gateDist, curveDist).withStyle(ChatFormatting.DARK_GRAY));
            }
            case COLLIDED -> {
                titleLabel.text(Component.translatable("text.oritech.accelerator.ui.collision").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedLabel.text(Component.translatable("text.oritech.accelerator.ui.collision.stats", format(event.lastEventSpeed(), 0)).withStyle(ChatFormatting.BLACK));
                statusLabel.text(Component.translatable("text.oritech.accelerator.ui.collision.position", event.lastEventPosition().toShortString()).withStyle(ChatFormatting.DARK_GRAY));
            }
            case EXITED_FAST -> {
                var speed = event.lastEventSpeed();
                var curveDist = format(AcceleratorParticleLogic.getRequiredBendDist(speed), 1);
                titleLabel.text(Component.translatable("text.oritech.accelerator.ui.exited").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedLabel.text(Component.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).withStyle(ChatFormatting.BLACK));
                statusLabel.text(Component.translatable("text.oritech.accelerator.ui.exited.stats", event.lastEventPosition().toShortString(), curveDist, format(event.minBendDist(), 1)).withStyle(ChatFormatting.DARK_GRAY));
            }
            case EXITED_NO_GATE -> {
                var speed = event.lastEventSpeed();
                var gateDist = format(AcceleratorParticleLogic.getMaxGateDist(speed), 1);
                titleLabel.text(Component.translatable("text.oritech.accelerator.ui.exited").withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
                speedLabel.text(Component.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).withStyle(ChatFormatting.BLACK));
                statusLabel.text(Component.translatable("text.oritech.accelerator.ui.exited.nogate", event.lastEventPosition().toShortString(), gateDist).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        
        updateItemParticle();
        
        super.containerTick();
    }
    
    private static String format(float number, int decimal) {
        if (decimal <= 0) return String.valueOf((int) number);
        var format = "%." + decimal + "f";
        return String.format(format, number);
    }
}
