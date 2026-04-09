package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.block.blocks.addons.CombiAddonBlock;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;

public class ShrinkerScreen extends UpgradableOritechScreen<UpgradableOritechScreenHandler> {
    
    private ButtonWidget shrinkButton;
    
    public ShrinkerScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    private LabelWidget statusLabel;
    
    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();
        
        shrinkButton = ButtonWidget.darkPanel(74, 38, 60, 14,
            Component.translatable("text.oritech.shrink"),
            event -> onShrinkPressed())
                                      .withTextColor(LabelWidget.DARK_TEXT);
        shrinkButton.withSurfacePadding(Insets.of(3, 1, 5, 1));

        if (this.menu.addonController.getConnectedAddons().isEmpty()) {
            shrinkButton.setActive(false);
        }

        statusLabel = new LabelWidget(40, 22, 120, 10, Component.translatable("text.oritech.shrink_ready"))
            .withAlignment(LabelWidget.Alignment.CENTER);

        if (this.menu.addonController instanceof ShrinkerBlockEntity shrinkerBlockEntity && shrinkerBlockEntity.currentCandidate != null) {
            var previewText = CombiAddonBlock.getShrinkTooltip(shrinkerBlockEntity.currentCandidate);
            previewText.add(0, Component.translatable("tooltip.oritech.shrinker_action"));
            shrinkButton.withTooltip(previewText);
        }

        addComponent(shrinkButton);
        addComponent(statusLabel);
    }
    
    @Override
    protected void tickExtra() {
        super.tickExtra();

        if (this.menu.addonController.getConnectedAddons().isEmpty()) {
            statusLabel.setText(Component.translatable("text.oritech.shrink_no_addon").withStyle(ChatFormatting.RED));
        } else if (this.menu.addonController instanceof ShrinkerBlockEntity shrinker && shrinker.getEnergyStorage(null).getAmount() < shrinker.getDefaultCapacity()) {
            statusLabel.setText(Component.translatable("text.oritech.shrink_no_energy").withStyle(ChatFormatting.RED));
        } else {
            statusLabel.setText(Component.translatable("text.oritech.shrink_ready"));
        }
        
        if (this.menu.addonController.getConnectedAddons().isEmpty()) {
            shrinkButton.setActive(false);
        }
        
    }
    
    private void onShrinkPressed() {
        NetworkManager.sendToServer(new ShrinkerBlockEntity.ShrinkerPlayerUsePacket(this.menu.blockPos));
    }
}
