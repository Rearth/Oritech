package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.splicer.SplicerPreview;
import rearth.oritech.block.entity.interaction.AddonSplicerBlockEntity;

public class AddonSplicerScreen extends UpgradableOritechScreen<UpgradableOritechScreenHandler> {

    private ButtonWidget shrinkButton;
    private LabelWidget statusLabel;
    private ItemWidget resultPreview;

    // last preview applied to the button tooltip, to only rebuild it when the synced preview changes
    private SplicerPreview shownPreview;

    public AddonSplicerScreen(UpgradableOritechScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void addExtraComponents() {
        super.addExtraComponents();

        shrinkButton = ButtonWidget.darkPanel(74, 38, 60, 14,
                        Component.translatable("text.oritech.shrink"),
                        event -> onShrinkPressed())
                .withTextColor(LabelWidget.BRIGHT_TEXT);
        shrinkButton.withSurfacePadding(Insets.of(3, 1, 5, 1));

        statusLabel = new LabelWidget(40, 22, 120, 10, Component.translatable("text.oritech.shrink_ready"))
                .withAlignment(LabelWidget.Alignment.CENTER);

        // faded preview of the result, shown in the empty output slot
        var outputSlot = menu.screenData.getGuiSlots().getFirst();
        resultPreview = new ItemWidget(outputSlot.x(), outputSlot.y(), ItemStack.EMPTY).withGhost(true);

        addComponent(shrinkButton);
        addComponent(statusLabel);
        addComponent(resultPreview);

        updateState();
    }

    @Override
    protected void tickExtra() {
        super.tickExtra();
        updateState();
    }

    private void updateState() {
        if (!(this.menu.addonController instanceof AddonSplicerBlockEntity splicer)) return;

        var preview = splicer.currentPreview;

        if (preview != shownPreview) {
            shownPreview = preview;
            shrinkButton.withTooltip(preview.tooltip());
            resultPreview.setStack(preview.result());
        }

        resultPreview.setVisible(preview.hasOperation() && !this.menu.getSlot(0).hasItem());

        if (!preview.hasOperation()) {
            statusLabel.setText(Component.translatable("text.oritech.shrink_no_target").withStyle(ChatFormatting.RED));
        } else if (preview.blockingReason().isPresent()) {
            statusLabel.setText(preview.blockingReason().get().copy().withStyle(ChatFormatting.RED));
        } else if (splicer.getEnergyLookup(null).getAmountAsLong() < preview.energyCost()) {
            statusLabel.setText(Component.translatable("text.oritech.shrink_no_energy").withStyle(ChatFormatting.RED));
        } else {
            statusLabel.setText(Component.translatable("text.oritech.shrink_ready"));
        }

        shrinkButton.setActive(preview.hasOperation() && preview.blockingReason().isEmpty());
    }

    private void onShrinkPressed() {
        ClientPacketDistributor.sendToServer(new AddonSplicerBlockEntity.AddonSplicerPlayerUsePacket(this.menu.blockPos));
    }
}
