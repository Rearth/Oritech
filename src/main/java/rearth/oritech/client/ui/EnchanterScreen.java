package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.network.NetworkContent;

public class EnchanterScreen extends BasicMachineScreen<EnchanterScreenHandler> {
    
    private ItemStack currentItem = null;
    private FlowLayout detailsScrollPane;
    private ButtonComponent openEnchantmentSelection;
    private LabelComponent statisticsLabel;
    
    public EnchanterScreen(EnchanterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        openEnchantmentSelection = Components.button(Text.literal("Bane of long names"), this::onOpenClicked);
        openEnchantmentSelection.positioning(Positioning.relative(54, 13));
        openEnchantmentSelection.active(false);
        overlay.child(openEnchantmentSelection);
        
        detailsScrollPane = Containers.verticalFlow(Sizing.content(2), Sizing.content(2));
        detailsScrollPane.padding(Insets.of(2));
        detailsScrollPane.margins(Insets.of(3));
        
        statisticsLabel = Components.label(Text.literal("1 / 4 catalysts available"));
        statisticsLabel.positioning(Positioning.relative(54, 29));
        overlay.child(statisticsLabel);
    }
    
    private void onOpenClicked(ButtonComponent event) {
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.EnchanterSelectionPacket(this.handler.blockPos, ""));
        this.handler.enchanter.selectedEnchantment = null;
        openSelectionPanel();
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
        var stack = this.handler.enchanter.inventory.getStack(0);
        if (currentItem == null)
            currentItem = stack;
        
        if (stack.getItem() != currentItem.getItem()) {
            currentItem = stack;
            onStackChanged();
        }
        
        Text description = Text.translatable("tooltip.oritech.enchanter_item_needed");
        var hasSelection = this.handler.enchanter.selectedEnchantment != null;
        if (hasSelection) {
            description = this.handler.enchanter.selectedEnchantment.value().description();
        }
        openEnchantmentSelection.setMessage(description);
        
        
        var registry = handler.enchanter.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var canBeEnchanted = registry.stream().anyMatch(elem -> stack.canBeEnchantedWith(registry.getEntry(elem), EnchantingContext.ACCEPTABLE));
        
        openEnchantmentSelection.active(hasSelection && canBeEnchanted);
        
        var statistics = handler.enchanter.statistics;
        if (statistics.equals(EnchanterBlockEntity.EnchanterStatistics.EMPTY)) {
            statisticsLabel.text(Text.literal(""));
        } else {
            statisticsLabel.text(Text.literal(statistics.availableCatalysts() + "/" + statistics.requiredCatalysts()).append(Text.translatable("tooltip.oritech.enchanter_catalysts_ready")).formatted(Formatting.DARK_GRAY));
        }
        
        this.progress_indicator.tooltip(Text.literal(handler.enchanter.progress + "/" + handler.enchanter.maxProgress).append(Text.translatable("tooltip.oritech.enchanter_souls_used")));
        
    }
    
    private void onStackChanged() {
        if (handler.enchanter.selectedEnchantment != null) return;
        openSelectionPanel();
        
    }
    
    private void openSelectionPanel() {
        // find enchantments
        var registry = handler.enchanter.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var all = registry.stream().map(registry::getEntry).filter(entry -> currentItem.canBeEnchantedWith(entry, EnchantingContext.ACCEPTABLE)).toList();
        
        if (all.isEmpty()) return;
        
        detailsScrollPane.clearChildren();
        
        var title = Components.label(Text.translatable("tooltip.oritech.enchanter_selection"));
        detailsScrollPane.child(title);
        
        var scrollPane = Containers.verticalScroll(Sizing.fixed(184), Sizing.fixed(200), detailsScrollPane);
        scrollPane.padding(Insets.of(2));
        var floatingPanel = Containers.overlay(scrollPane);
        
        // refresh gui
        for (var entry : all) {
            var candidate = entry.value();
            var button = Components.button(candidate.description(), data -> onEnchantmentSelected(entry, floatingPanel));
            button.sizing(Sizing.fill(), Sizing.fixed(25));
            button.margins(Insets.of(1, 1, 0, 8));
            detailsScrollPane.child(button);
        }
        
        scrollPane.surface(Surface.DARK_PANEL);
        
        
        floatingPanel.zIndex(9800);
        floatingPanel
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        this.root.child(floatingPanel);
    }
    
    private void onEnchantmentSelected(RegistryEntry<Enchantment> entry, OverlayContainer<ScrollContainer<FlowLayout>> floatingPanel) {
        this.handler.enchanter.selectedEnchantment = entry;
        floatingPanel.remove();
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.EnchanterSelectionPacket(this.handler.blockPos, entry.getIdAsString()));
    }
}
