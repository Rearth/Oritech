package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.util.ColorHelper;

public class EnchanterScreen extends OritechMachineScreen<EnchanterScreenHandler> {

    private ItemStack currentItem = ItemStack.EMPTY;
    private ButtonWidget chooseButton;
    private LabelWidget statisticsLabel;
    private OverlayWidget selectionOverlay;

    public EnchanterScreen(EnchanterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public boolean showExtensionPanel() {
        return false;
    }

    @Override
    protected void addExtraComponents() {

        chooseButton = ButtonWidget.darkPanel(40, 33, 110, 20,
            Component.translatable("message.oritech.enchanter.insert_item"),
            button -> onOpenClicked()).withTextColor(LabelWidget.BRIGHT_TEXT);

        statisticsLabel = new LabelWidget(40, 20, 110, 20, Component.literal(" "));
        statisticsLabel.withAlignment(LabelWidget.Alignment.CENTER);
        statisticsLabel.withWrap(true);
        statisticsLabel.withDarkColor();

        addComponent(chooseButton);
        addComponent(statisticsLabel);
    }

    @Override
    protected void tickExtra() {
        var stack = menu.enchanter.inventory.getItem(0);
        if (!ItemStack.isSameItemSameComponents(currentItem, stack)) {
            currentItem = stack.copy();
            onStackChanged();
        }

        Component description = Component.translatable("tooltip.oritech.enchanter_selection");
        if (stack.isEmpty()) description = Component.translatable("message.oritech.enchanter.insert_item");
        var selection = menu.enchanter.getSelectedEnchantment();
        var hasSelection = selection != null;
        if (hasSelection) {
            description = selection.value().description();
        }

        var registry = menu.enchanter.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var canBeEnchanted = registry.stream().anyMatch(elem -> elem.canEnchant(stack));
        chooseButton.setActive(canBeEnchanted);
        chooseButton.setLabel(description);

        var statistics = menu.enchanter.statistics;
        if (statistics.equals(EnchanterBlockEntity.EnchanterStatistics.EMPTY)) {
            statisticsLabel.setText(Component.literal(" "));
        } else {
            statisticsLabel.setText(Component.translatable("title.oritech.enchanter.catalysts", statistics.availableCatalysts(), statistics.requiredCatalysts()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private void onOpenClicked() {
        menu.enchanter.selectedEnchantment = EnchanterBlockEntity.NONE_SELECTED;
        sendEnchantmentToServer(EnchanterBlockEntity.NONE_SELECTED);
        openSelectionPanel();
    }

    private void onStackChanged() {
        if (!currentItem.isEmpty() && menu.enchanter.getSelectedEnchantment() == null) {
            openSelectionPanel();
        } else if (currentItem.isEmpty()) {
            closeSelectionOverlay();
        }
    }

    private void openSelectionPanel() {
        closeSelectionOverlay();

        var registry = menu.enchanter.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var all = registry.stream().map(registry::wrapAsHolder).filter(entry -> entry.value().canEnchant(currentItem)).toList();
        if (all.isEmpty()) return;

        selectionOverlay = new OverlayWidget(imageWidth, imageHeight);
        selectionOverlay.withBackgroundColor(ColorHelper.argb(0f, 0f, 0f, 0.45f));
        selectionOverlay.withDismissHandler(this::closeSelectionOverlay);

        int panelX = 10;
        int panelY = 8;
        int panelWidth = imageWidth - 20;
        int panelHeight = imageHeight - 16;

        var panel = new SurfaceWidget(panelX, panelY, panelWidth, panelHeight);
        panel.withSurface(OritechSurface.PANEL);
        selectionOverlay.addChild(panel);

        var title = new LabelWidget(panelX + 8, panelY + 8, panelWidth - 16, 10, Component.translatable("tooltip.oritech.enchanter_selection"));
        title.withAlignment(LabelWidget.Alignment.CENTER);
        title.withDarkColor();
        selectionOverlay.addChild(title);

        var scroll = new ScrollWidget(panelX + 8, panelY + 24, panelWidth - 16, panelHeight - 32);
        scroll.setContentDimensions(panelWidth - 24, all.size() * 26);
        scroll.withSurface(OritechSurface.PANEL_DARK);
        selectionOverlay.addChild(scroll);

        int y = 0;
        for (var entry : all) {
            var button = ButtonWidget.darkPanel(0, y, panelWidth - 28, 22,
                entry.value().description().copy().withColor(LabelWidget.BRIGHT_TEXT),
                ignored -> onEnchantmentSelected(entry));
            scroll.addChild(button);
            y += 26;
        }

        addComponent(selectionOverlay);
    }

    private void onEnchantmentSelected(Holder<Enchantment> entry) {
        var selected = ResourceLocation.parse(entry.getRegisteredName());
        menu.enchanter.selectedEnchantment = selected;
        sendEnchantmentToServer(selected);
        closeSelectionOverlay();
    }

    private void closeSelectionOverlay() {
        if (selectionOverlay != null) {
            removeComponent(selectionOverlay);
            selectionOverlay = null;
        }
    }

    private void sendEnchantmentToServer(ResourceLocation selected) {
        NetworkManager.sendToServer(new EnchanterBlockEntity.SelectEnchantingPacket(menu.blockPos, selected));
    }
}
