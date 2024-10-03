package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.network.NetworkContent;

import java.util.HashMap;
import java.util.Map;

import static rearth.oritech.client.ui.BasicMachineScreen.ITEM_SLOT;

public class ItemFilterScreen extends BaseOwoHandledScreen<FlowLayout, ItemFilterScreenHandler> {
    
    private ButtonComponent whiteListButton;
    private ButtonComponent nbtButton;
    private final FlowLayout[] gridContainers = new FlowLayout[8];
    private Map<Integer, ItemStack> cachedItems;
    
    public ItemFilterScreen(ItemFilterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    public void updateItemFilters() {
        
        cachedItems = handler.blockEntity.getFilterSettings().items();
        Oritech.LOGGER.debug("loading item filters: " + cachedItems);
        
        for (int i = 0; i < 8; i++) {
            var storedStack = cachedItems.getOrDefault(i, ItemStack.EMPTY);
            
            var container = gridContainers[i];
            // if empty and one is set, remove it
            // otherwise just add a new display
            if (container.children().size() == 2) {
                container.removeChild(container.children().get(1));
            }
            
            if (storedStack.isEmpty()) continue;
            
            var itemComponent = Components.item(storedStack);
            itemComponent.positioning(Positioning.absolute(1, 1));
            itemComponent.showOverlay(true);
            itemComponent.setTooltipFromStack(true);
            container.child(itemComponent);
            
        }
        
    }
    
    private void updateButtons() {
        var data = handler.blockEntity.getFilterSettings();
        
        var textWhitelist = data.useWhitelist() ? Text.translatable("title.oritech.item_filter.whitelist") : Text.translatable("title.oritech.item_filter.blacklist");
        var textWhitelistTooltip = data.useWhitelist() ?
                                     Text.translatable("tooltip.oritech.item_filter.whitelist")
                                     : Text.translatable("tooltip.oritech.item_filter.blacklist");
        
        var textNbt = data.useNbt() ? Text.translatable("title.oritech.item_filter.nbt") : Text.translatable("title.oritech.item_filter.no_nbt");
        var textNbtTooltip = data.useNbt() ?
                                     Text.translatable("tooltip.oritech.item_filter.nbt")
                                     : Text.translatable("tooltip.oritech.item_filter.no_nbt");
        
        whiteListButton.setMessage(textWhitelist);
        whiteListButton.tooltip(textWhitelistTooltip);
        
        nbtButton.setMessage(textNbt);
        nbtButton.tooltip(textNbtTooltip);
        
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        
        rootComponent
          .surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        var overlay = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(166));
        
        var gridContainer = Containers.grid(Sizing.content(0), Sizing.content(0), 2, 4);
        
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 2; y++) {
                
                var slotContainer = Containers.horizontalFlow(Sizing.fixed(19), Sizing.fixed(18));
                var background = Components.texture(ITEM_SLOT, 0, 0, 18, 17, 18, 17).positioning(Positioning.absolute(0, 0));
                
                int finalX = x;
                int finalY = y;
                background.mouseDown().subscribe(((mouseX, mouseY, button) -> onItemFrameBackgroundClicked(slotContainer, finalX, finalY)));
                
                slotContainer.child(background);
                var idIndex = y * 4 + x;
                gridContainers[idIndex] = slotContainer;
                gridContainer.child(slotContainer, y, x);
                
            }
        }
        
        overlay.child(gridContainer.positioning(Positioning.absolute(25, 20)));
        
        var buttonWidth = 50;
        whiteListButton = Components.button(Text.translatable("button.oritech.item_filter.whitelist"), buttonComponent -> toggleWhitelist());
        whiteListButton.horizontalSizing(Sizing.fixed(buttonWidth));
        overlay.child(whiteListButton.positioning(Positioning.absolute(110, 20)));
        
        nbtButton = Components.button(Text.translatable("button.oritech.item_filter.nbt_on"), buttonComponent -> toggleNbt());
        nbtButton.horizontalSizing(Sizing.fixed(buttonWidth));
        overlay.child(nbtButton.positioning(Positioning.absolute(110, 46)));
        
        addTitle(overlay);
        
        rootComponent.child(
          Components.texture(BasicMachineScreen.BACKGROUND, 0, 0, 176, 166, 176, 166)
        ).child(
          overlay.positioning(Positioning.relative(50, 50))
        );
        
        updateButtons();
        updateItemFilters();
    }
    
    
    private void addTitle(FlowLayout overlay) {
        var blockTitle = handler.blockEntity.getCachedState().getBlock().getName();
        var label = Components.label(blockTitle);
        label.color(new Color(64 / 255f, 64 / 255f, 64 / 255f));
        label.sizing(Sizing.fixed(176), Sizing.content(2));
        label.horizontalTextAlignment(HorizontalAlignment.CENTER);
        label.zIndex(1);
        overlay.child(label.positioning(Positioning.relative(50, 2)));
    }
    
    private void sendUpdateToServer() {
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.ItemFilterSyncPacket(handler.blockPos, handler.blockEntity.getFilterSettings()));
    }
    
    private void toggleWhitelist() {
        
        var data = handler.blockEntity.getFilterSettings();
        var whitelist = data.useWhitelist();
        var newWhitelist = !whitelist;
        var newData = new ItemFilterBlockEntity.FilterData(data.useNbt(), newWhitelist, data.items());
        updateFilterSettings(newData); // this is only on client
        
        updateButtons();
        sendUpdateToServer();
    }
    
    private void toggleNbt() {
        
        var data = handler.blockEntity.getFilterSettings();
        var nbt = data.useNbt();
        var newNbt = !nbt;
        var newData = new ItemFilterBlockEntity.FilterData(newNbt, data.useWhitelist(), data.items());
        updateFilterSettings(newData); // this is only on client
        
        updateButtons();
        sendUpdateToServer();
    }
    
    private boolean onItemFrameBackgroundClicked(FlowLayout slotContainer, int x, int y) {
        
        if (slotContainer.children().size() >= 2) {
            slotContainer.removeChild(slotContainer.children().get(1));
        }
        
        var heldItem = this.handler.getCursorStack();
        if (heldItem.isEmpty()) {
            
            var idIndex = y * 4 + x;
            var oldData = handler.blockEntity.getFilterSettings();
            var itemFilters = new HashMap<>(oldData.items());
            itemFilters.remove(idIndex);
            var newData = new ItemFilterBlockEntity.FilterData(oldData.useNbt(), oldData.useWhitelist(), itemFilters);
            updateFilterSettings(newData); // this is only on client
            sendUpdateToServer();
            
            return false;
        }
        
        var displayStack = new ItemStack(heldItem.getItem(), 1);
        
        if (heldItem.getComponents() != null)
            displayStack.applyComponentsFrom(heldItem.getComponents());
        
        var itemComponent = Components.item(displayStack);
        itemComponent.positioning(Positioning.absolute(1, 1));
        itemComponent.showOverlay(true);
        itemComponent.setTooltipFromStack(true);
        slotContainer.child(itemComponent);
        
        var idIndex = y * 4 + x;
        var oldData = handler.blockEntity.getFilterSettings();
        var itemFilters = new HashMap<>(oldData.items());
        itemFilters.put(idIndex, displayStack);
        var newData = new ItemFilterBlockEntity.FilterData(oldData.useNbt(), oldData.useWhitelist(), itemFilters);
        updateFilterSettings(newData); // this is only on client
        
        Oritech.LOGGER.debug("stored map: " + itemFilters);
        sendUpdateToServer();
        
        return true;
    }

    private void updateFilterSettings(ItemFilterBlockEntity.FilterData filterData) {
        handler.blockEntity.setFilterSettings(filterData);
        cachedItems = filterData.items();
    }
}
