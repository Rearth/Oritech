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
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorParticleLogic;

public class AcceleratorScreen extends BasicMachineScreen<AcceleratorScreenHandler> {
    
    public static final Identifier PARTICLE_OVERLAY = Oritech.id("textures/gui/modular/particle_background_arrow.png");
    
    private LabelComponent titleLabel;
    private LabelComponent speedLabel;
    private LabelComponent statusLabel;
    private ItemComponent activeParticleRenderer;
    
    public AcceleratorScreen(AcceleratorScreenHandler handler, PlayerInventory inventory, Text title) {
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
        if (handler.accelerator.lastEvent.activeParticle() != ItemStack.EMPTY)
            shownItem = handler.accelerator.lastEvent.activeParticle();
        
        activeParticleRenderer = Components.item(shownItem);
        overlay.child(activeParticleRenderer.positioning(Positioning.absolute(7, 34)));
        
        var particleOverlay = Components.texture(PARTICLE_OVERLAY, 0, 0, 24, 30, 24, 30).sizing(Sizing.fixed(24), Sizing.fixed(30));
        particleOverlay.zIndex(-1);
        overlay.child(particleOverlay.positioning(Positioning.absolute(3, 27)));
        
        titleLabel = Components.label(Text.literal("Waiting...").formatted(Formatting.BLACK, Formatting.BOLD));
        titleLabel.horizontalTextAlignment(HorizontalAlignment.CENTER).horizontalSizing(Sizing.fill()).verticalSizing(Sizing.fixed(10)).margins(Insets.of(1));
        
        speedLabel = Components.label(Text.literal("Speed: 0 blocks/s").formatted(Formatting.BLACK));
        speedLabel.margins(Insets.of(2));
        
        statusLabel = Components.label(Text.literal("Insert item to accelerate\nAnd some more details").formatted(Formatting.BLACK));
        statusLabel.horizontalTextAlignment(HorizontalAlignment.RIGHT).margins(Insets.of(2));
        
        var labelContainer = Containers.verticalFlow(Sizing.fixed(130), Sizing.fixed(100));
        labelContainer.child(titleLabel).child(speedLabel).child(statusLabel);
        labelContainer.horizontalAlignment(HorizontalAlignment.RIGHT);
        labelContainer.positioning(Positioning.relative(82, 30));
        
        overlay.child(labelContainer);
        
    }
    
    private void updateItemParticle() {
        var shownItem = ItemStack.EMPTY;
        if (handler.accelerator.lastEvent.lastEvent().equals(AcceleratorControllerBlockEntity.ParticleEvent.ACCELERATING) && handler.accelerator.lastEvent.activeParticle() != ItemStack.EMPTY)
            shownItem = handler.accelerator.lastEvent.activeParticle();
        
        activeParticleRenderer.stack(shownItem);
    }
    
    @Override
    protected void handledScreenTick() {
        var event = handler.accelerator.lastEvent;
        titleLabel.horizontalTextAlignment(HorizontalAlignment.RIGHT);
        
        switch (event.lastEvent()) {
            case IDLE -> {
                var text = Text.translatable("text.oritech.accelerator.ui.waiting.0");
                var time = handler.blockEntity.getWorld().getTime();
                if ((time / 20) % 3 == 1) text = Text.translatable("text.oritech.accelerator.ui.waiting.1");
                if ((time / 20) % 3 == 2) text = Text.translatable("text.oritech.accelerator.ui.waiting.2");
                text = text.formatted(Formatting.BOLD, Formatting.BLACK);
                titleLabel.horizontalTextAlignment(HorizontalAlignment.LEFT).text(text);
                speedLabel.text(Text.translatable("text.oritech.accelerator.ui.waiting").formatted(Formatting.BLACK));
                statusLabel.text(Text.literal(""));
            }
            case ERROR -> {
                titleLabel.text(Text.translatable("text.oritech.accelerator.ui.error").formatted(Formatting.BLACK, Formatting.BOLD));
                speedLabel.text(Text.translatable("text.oritech.accelerator.ui.nogate").formatted(Formatting.BLACK));
                statusLabel.text(Text.translatable("text.oritech.accelerator.ui.nogate.more").formatted(Formatting.DARK_GRAY));
            }
            case ACCELERATING -> {
                var speed = event.lastEventSpeed();
                var gateDist = format(AcceleratorParticleLogic.getMaxGateDist(speed), 1);
                var curveDist = format(AcceleratorParticleLogic.getRequiredBendDist(speed), 1);
                titleLabel.text(Text.translatable("text.oritech.accelerator.ui.accelerating").formatted(Formatting.BLACK, Formatting.BOLD));
                speedLabel.text(Text.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).formatted(Formatting.BLACK));
                statusLabel.text(Text.translatable("text.oritech.accelerator.ui.accelerating.stats", gateDist, curveDist).formatted(Formatting.DARK_GRAY));
            }
            case COLLIDED -> {
                titleLabel.text(Text.translatable("text.oritech.accelerator.ui.collision").formatted(Formatting.BLACK, Formatting.BOLD));
                speedLabel.text(Text.translatable("text.oritech.accelerator.ui.collision.stats", format(event.lastEventSpeed(), 1)).formatted(Formatting.BLACK));
                statusLabel.text(Text.translatable("text.oritech.accelerator.ui.collision.position", event.lastEventPosition().toShortString()).formatted(Formatting.DARK_GRAY));
            }
            case EXITED_FAST -> {
                var speed = event.lastEventSpeed();
                var curveDist = format(AcceleratorParticleLogic.getRequiredBendDist(speed), 1);
                titleLabel.text(Text.translatable("text.oritech.accelerator.ui.exited").formatted(Formatting.BLACK, Formatting.BOLD));
                speedLabel.text(Text.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).formatted(Formatting.BLACK));
                statusLabel.text(Text.translatable("text.oritech.accelerator.ui.exited.stats", event.lastEventPosition().toShortString(), curveDist, format(event.minBendDist(), 1)).formatted(Formatting.DARK_GRAY));
            }
            case EXITED_NO_GATE -> {
                var speed = event.lastEventSpeed();
                var gateDist = format(AcceleratorParticleLogic.getMaxGateDist(speed), 1);
                titleLabel.text(Text.translatable("text.oritech.accelerator.ui.exited").formatted(Formatting.BLACK, Formatting.BOLD));
                speedLabel.text(Text.translatable("text.oritech.accelerator.ui.accelerating.speed", format(event.lastEventSpeed(), 0)).formatted(Formatting.BLACK));
                statusLabel.text(Text.translatable("text.oritech.accelerator.ui.exited.nogate", event.lastEventPosition().toShortString(), gateDist).formatted(Formatting.DARK_GRAY));
            }
        }
        
        updateItemParticle();
        
        super.handledScreenTick();
    }
    
    private static String format(float number, int decimal) {
        if (decimal <= 0) return String.valueOf((int) number);
        var format = "%." + decimal + "f";
        return String.format(format, number);
    }
}
