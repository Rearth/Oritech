package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import rearth.oritech.OritechClient;
import rearth.oritech.block.entity.interaction.PlayerModifierTestEntity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AugmentSelectionScreen extends BaseOwoScreen<FlowLayout> {
    
    private Component lastFocused;
    
    private final Set<ButtonComponent> augments = new HashSet<>();
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        addAugments(rootComponent);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        if (augments.isEmpty()) return;
        
        var mousingOver = augments.stream().findFirst().get();
        var minDist = Integer.MAX_VALUE;
        
        for (var augment : augments) {
            var mousePos = new Vector2i(mouseX, mouseY);
            var augmentPos = new Vector2i(augment.getX() + augment.getWidth() / 2, augment.getY() + augment.getHeight() / 2);
            // System.out.println(augment.getMessage() + " | " + mousePos.toString(NumberFormat.getCompactNumberInstance()) + " | " + augmentPos.toString(NumberFormat.getCompactNumberInstance()));
            
            augment.setFocused(false);
            
            var dist = mousePos.distanceSquared(augmentPos);
            if (dist < minDist) {
                minDist = (int) dist;
                mousingOver = augment;
            }
        }
        
        mousingOver.setFocused(true);
        mousingOver.drawFocusHighlight(OwoUIDrawContext.of(context), mouseX, mouseY, 0, delta);
        lastFocused = mousingOver;
        System.out.println("res: " + mousingOver.getMessage());
        
    }
    
    private void addAugments(FlowLayout parent) {
        
        var player = Objects.requireNonNull(this.client).player;
        
        for (var augment : PlayerModifierTestEntity.allAugments.values()) {
            var isInstalled = augment.isInstalled(player);
            var isToggleable = augment.toggleable;
            var isEnabled = augment.isEnabled(player);
            
            if (!isInstalled || !isToggleable) continue;
            
            System.out.println(augment.id);
            
            var label = Components.button(Text.literal(augment.id.getPath() +": " + isEnabled), button -> System.out.println(button.getMessage()));
            augments.add(label);
            
            parent.child(label);
            
        }
        
    }
    
    @Override
    public void close() {
        OritechClient.activeScreen = null;
        super.close();
    }
}
