package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.block.blocks.addons.HeartOfTheMachineAddonBlock;
import rearth.oritech.block.entity.interaction.AddonSplicerBlockEntity;

public class AddonSplicerScreen extends UpgradableOritechScreen<UpgradableOritechScreenHandler> {

    private ButtonWidget shrinkButton;

    public AddonSplicerScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    private LabelWidget statusLabel;

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        shrinkButton = ButtonWidget.darkPanel(74, 38, 60, 14,
                        Component.translatable("text.oritech.shrink"),
                        event -> onShrinkPressed())
                .withTextColor(LabelWidget.BRIGHT_TEXT);
        shrinkButton.withSurfacePadding(Insets.of(3, 1, 5, 1));

        if (this.menu.addonController.getConnectedAddons().isEmpty()) {
            shrinkButton.setActive(false);
        }

        statusLabel = new LabelWidget(40, 22, 120, 10, Component.translatable("text.oritech.shrink_ready"))
                .withAlignment(LabelWidget.Alignment.CENTER);

        if (this.menu.addonController instanceof AddonSplicerBlockEntity addon_splicerBlockEntity && addon_splicerBlockEntity.currentCandidate != null) {
            var previewText = HeartOfTheMachineAddonBlock.getShrinkTooltip(addon_splicerBlockEntity.currentCandidate);
            previewText.add(0, Component.translatable("tooltip.oritech.addon_splicer_action"));
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
        } else if (this.menu.addonController instanceof AddonSplicerBlockEntity addon_splicer && addon_splicer.getEnergyLookup(null).getAmountAsInt() < addon_splicer.getDefaultCapacity()) {
            statusLabel.setText(Component.translatable("text.oritech.shrink_no_energy").withStyle(ChatFormatting.RED));
        } else {
            statusLabel.setText(Component.translatable("text.oritech.shrink_ready"));
        }

        if (this.menu.addonController.getConnectedAddons().isEmpty()) {
            shrinkButton.setActive(false);
        }

    }

    private void onShrinkPressed() {
        ClientPacketDistributor.sendToServer(new AddonSplicerBlockEntity.AddonSplicerPlayerUsePacket(this.menu.blockPos));
    }
}
