package rearth.oritech.client.ui;

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

import java.util.Arrays;

public class EnchanterScreen extends BasicMachineScreen<EnchanterScreenHandler> {
    
    private ItemStack currentItem = ItemStack.EMPTY;
    private LabelComponent selectedEnchantment;
    private FlowLayout detailsScrollPane;
    
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
        
        selectedEnchantment = Components.label(Text.literal("Nothing selected"));
        selectedEnchantment.sizing(Sizing.fill(100), Sizing.fixed(20));
        selectedEnchantment.positioning(Positioning.relative(50, 15));
        selectedEnchantment.horizontalTextAlignment(HorizontalAlignment.CENTER);
        
        overlay.child(selectedEnchantment);
        
        detailsScrollPane = Containers.verticalFlow(Sizing.content(2), Sizing.content(2));
        detailsScrollPane.padding(Insets.of(2));
        detailsScrollPane.surface(Surface.DARK_PANEL);
        
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
        var stack = this.handler.enchanter.inventory.getStack(0);
        if (stack != currentItem) {
            currentItem = stack;
            onStackChanged();
        }
        
        Text description = Text.literal("Nothing selected");
        if (this.handler.enchanter.selectedEnchantment != null) {
            description = this.handler.enchanter.selectedEnchantment.value().description();
        }
        selectedEnchantment.text(description);
        
    }
    
    private void onStackChanged() {
        System.out.println("got new stack: " + currentItem);
        
        // find enchantments
        var registry = handler.enchanter.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var all = registry.stream().map(registry::getEntry).filter(entry -> currentItem.canBeEnchantedWith(entry, EnchantingContext.ACCEPTABLE)).toList();
        System.out.println(Arrays.toString(all.toArray()));
        
        if (all.isEmpty()) return;
        
        detailsScrollPane.clearChildren();
        
        var scrollPane = Containers.verticalScroll(Sizing.fixed(184), Sizing.fixed(200), detailsScrollPane);
        var floatingPanel = Containers.overlay(scrollPane);
        
        // refresh gui
        for (var entry : all) {
            var candidate = entry.value();
            var button = Components.button(candidate.description(), data -> onEnchantmentSelected(entry, floatingPanel));
            button.sizing(Sizing.fill(), Sizing.fixed(25));
            detailsScrollPane.child(button);
        }
        
        
        floatingPanel.zIndex(9800);
        floatingPanel
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        this.root.child(floatingPanel);
        
    }
    
    private void onEnchantmentSelected(RegistryEntry<Enchantment> entry, OverlayContainer<ScrollContainer<FlowLayout>> floatingPanel) {
        System.out.println(entry);
        this.handler.enchanter.selectedEnchantment = entry;
        floatingPanel.remove();
    }
}
