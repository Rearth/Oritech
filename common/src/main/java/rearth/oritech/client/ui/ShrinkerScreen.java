package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Positioning;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.blocks.addons.CombiAddonBlock;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;

public class ShrinkerScreen extends UpgradableMachineScreen<UpgradableMachineScreenHandler> {
    
    public ShrinkerScreen(UpgradableMachineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    public LabelComponent statusLabel;
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        var shrinkButton = Components.button(Component.translatable("text.oritech.shrink"), event -> {
            onShrinkPressed();
        });
        shrinkButton.renderer(ORITECH_BUTTON_DARK);
        shrinkButton.textShadow(false);
        
        if (this.menu.addonController.getConnectedAddons().isEmpty()) {
            shrinkButton.active(false);
        }
        
        statusLabel = Components.label(Component.translatable("text.oritech.shrink_ready"));
        
        if (this.menu.addonController instanceof ShrinkerBlockEntity shrinkerBlockEntity && shrinkerBlockEntity.currentCandidate != null) {
            var previewText = CombiAddonBlock.getShrinkTooltip(shrinkerBlockEntity.currentCandidate);
            previewText.add(0, Component.translatable("tooltip.oritech.shrinker_action"));
            shrinkButton.tooltip(previewText);
        }
        
        overlay.child(shrinkButton.positioning(Positioning.absolute(74, 38)));
        overlay.child(statusLabel.positioning(Positioning.absolute(40, 22)));
        
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        if (this.menu.addonController.getConnectedAddons().isEmpty()) {
            statusLabel.text(Component.translatable("text.oritech.shrink_no_addon").withStyle(ChatFormatting.RED));
        } else if (this.menu.addonController instanceof ShrinkerBlockEntity shrinker && shrinker.getEnergyStorage(null).getAmount() < shrinker.getDefaultCapacity()) {
            statusLabel.text(Component.translatable("text.oritech.shrink_no_energy").withStyle(ChatFormatting.RED));
        } else {
            statusLabel.text(Component.translatable("text.oritech.shrink_ready"));
        }
        
    }
    
    private void onShrinkPressed() {
        NetworkManager.sendToServer(new ShrinkerBlockEntity.ShrinkerPlayerUsePacket(this.menu.getBlockPos()));
    }
}
