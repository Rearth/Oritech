package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.network.NetworkContent;

public class PlayerModifierScreen extends BaseOwoHandledScreen<FlowLayout, PlayerModifierScreenHandler> {
    
    public PlayerModifierScreen(PlayerModifierScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
          .surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        addAvailableAugments(rootComponent);
        
    }
    
    private void addAvailableAugments(FlowLayout parent) {
        
        for (var augment : this.handler.blockEntity.allAugments.keySet()) {
            var isResearched = this.handler.blockEntity.researchedAugments.contains(augment);
            var isApplied = this.handler.blockEntity.hasPlayerAugment(augment, this.handler.player);
            
            var text = augment.getPath();
            var operation = AugmentOperation.RESEARCH;
            if (isApplied) {
                text += " remove";
                operation = AugmentOperation.REMOVE;
            } else if (isResearched) {
                text += " apply";
                operation = AugmentOperation.ADD;
            } else {
                text += " research";
            }
            
            var finalText = text;
            var finalOperation = operation;
            var button = Components.button(Text.literal(text), elem -> {
                System.out.println(finalText + "pressed");
                triggerAugmentOperation(augment, finalOperation);
            });
            
            parent.child(button);
            
        }
        
    }
    
    private void triggerAugmentOperation(Identifier id, AugmentOperation operation) {
        var operationId = operation.ordinal();
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.AugmentInstallTriggerPacket(this.handler.blockPos, id, operationId));
    }
    
    public enum AugmentOperation {
        RESEARCH, ADD, REMOVE
    }
    
}
