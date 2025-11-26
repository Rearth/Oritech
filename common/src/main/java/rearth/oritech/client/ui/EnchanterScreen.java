package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity.EnchanterStatistics;


public class EnchanterScreen extends BasicMachineScreen<EnchanterScreenHandler> {
    
    private ItemStack currentItem = null;
    private FlowLayout detailsScrollPane;
    private ButtonComponent openEnchantmentSelection;
    private LabelComponent statisticsLabel;
    
    public EnchanterScreen(EnchanterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        openEnchantmentSelection = Components.button(Component.translatable("button.oritech.enchanter.bane_of_long_names"), this::onOpenClicked);
        openEnchantmentSelection.positioning(Positioning.relative(54, 13));
        openEnchantmentSelection.active(false);
        openEnchantmentSelection.renderer(ORITECH_BUTTON_DARK);
        overlay.child(openEnchantmentSelection);
        
        detailsScrollPane = Containers.verticalFlow(Sizing.content(2), Sizing.content(2));
        detailsScrollPane.padding(Insets.of(2));
        detailsScrollPane.margins(Insets.of(3));
        
        statisticsLabel = Components.label(Component.translatable("title.oritech.enchanter.catalysts_available", 1, 4));
        statisticsLabel.positioning(Positioning.relative(54, 29));
        overlay.child(statisticsLabel);
    }
    
    private void onOpenClicked(ButtonComponent event) {
        sendEnchantmentToServer(EnchanterBlockEntity.NONE_SELECTED);
        openSelectionPanel();
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        var stack = this.menu.enchanter.inventory.getItem(0);
        if (currentItem == null)
            currentItem = stack;
        
        if (stack.getItem() != currentItem.getItem()) {
            currentItem = stack;
            onStackChanged();
        }
        
        Component description = Component.translatable("message.oritech.enchanter.insert_item");
        var hasSelection = this.menu.enchanter.getSelectedEnchantment() != null;
        if (hasSelection) {
            description = this.menu.enchanter.getSelectedEnchantment().value().description();
        }
        openEnchantmentSelection.setMessage(description);
        
        
        var registry = menu.enchanter.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var canBeEnchanted = registry.stream().anyMatch(elem -> elem.canEnchant(stack));
        
        openEnchantmentSelection.active(hasSelection && canBeEnchanted);
        
        var statistics = menu.enchanter.statistics;
        if (statistics.equals(EnchanterBlockEntity.EnchanterStatistics.EMPTY)) {
            statisticsLabel.text(Component.literal(" "));
        } else {
            statisticsLabel.text(Component.translatable("title.oritech.enchanter.catalysts", statistics.availableCatalysts(), statistics.requiredCatalysts()).withStyle(ChatFormatting.DARK_GRAY));
        }
        
        this.progress_indicator.tooltip(Component.translatable("title.oritech.enchanter.souls_used", menu.enchanter.progress, menu.enchanter.maxProgress));
        
    }
    
    private void onStackChanged() {
        if (menu.enchanter.getSelectedEnchantment() != null) return;
        openSelectionPanel();
        
    }
    
    private void openSelectionPanel() {
        
        var slotCount = this.menu.slots.size();
        
        for (int i = 0; i < slotCount; i++) {
            this.disableSlot(i);
        }
        
        // find enchantments
        var registry = menu.enchanter.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var all = registry.stream().map(registry::wrapAsHolder).filter(entry -> entry.value().canEnchant(currentItem)).toList();
        
        if (all.isEmpty()) return;
        
        detailsScrollPane.clearChildren();
        
        var title = Components.label(Component.translatable("tooltip.oritech.enchanter_selection"));
        detailsScrollPane.child(title);
        
        var scrollPane = Containers.verticalScroll(Sizing.fixed(184), Sizing.fixed(200), detailsScrollPane);
        scrollPane.padding(Insets.of(2));
        var floatingPanel = new OverlayContainer<>(scrollPane) {
            @Override
            public void remove() {
                super.remove();
                for (int i = 0; i < slotCount; i++) {
                    EnchanterScreen.this.enableSlot(i);
                }
            }
        };
        
        // refresh gui
        for (var entry : all) {
            var candidate = entry.value();
            var button = Components.button(candidate.description().copy().withColor(BasicMachineScreen.GRAY_TEXT_COLOR), data -> onEnchantmentSelected(entry, floatingPanel));
            button.sizing(Sizing.fill(), Sizing.fixed(25));
            button.margins(Insets.of(1, 1, 0, 8));
            button.renderer(ORITECH_BUTTON);
            button.textShadow(false);
            detailsScrollPane.child(button);
        }
        
        scrollPane.surface(Surface.DARK_PANEL);
        
        
        floatingPanel.zIndex(9800);
        floatingPanel
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        this.root.child(floatingPanel);
    }
    
    private void onEnchantmentSelected(Holder<Enchantment> entry, OverlayContainer<ScrollContainer<FlowLayout>> floatingPanel) {
        this.menu.enchanter.selectedEnchantment = ResourceLocation.parse(entry.getRegisteredName());
        sendEnchantmentToServer(ResourceLocation.parse(entry.getRegisteredName()));
        floatingPanel.remove();
    }
    
    private void sendEnchantmentToServer(ResourceLocation selected) {
        NetworkManager.sendToServer(new EnchanterBlockEntity.SelectEnchantingPacket(this.menu.blockPos, selected));
    }
    
    
}
