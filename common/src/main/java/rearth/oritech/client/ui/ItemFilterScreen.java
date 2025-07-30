package rearth.oritech.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.*;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity.FilterData;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static rearth.oritech.client.ui.BasicMachineScreen.ITEM_SLOT;


public class ItemFilterScreen extends BaseOwoHandledScreen<FlowLayout, ItemFilterScreenHandler> {

    public static final int FILTER_SIZE = 12;

    private ButtonComponent whiteListButton;
    private ButtonComponent nbtButton;
    private ButtonComponent componentButton;
    private final FlowLayout[] gridContainers = new FlowLayout[FILTER_SIZE];
    private Map<Integer, ItemStack> cachedItems;
    
    public ItemFilterScreen(ItemFilterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    public void updateItemFilters() {
        
        cachedItems = menu.blockEntity.getFilterSettings().items();
        Oritech.LOGGER.debug("loading item filters: " + cachedItems);
        
        for (int i = 0; i < FILTER_SIZE; i++) {
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
        var data = menu.blockEntity.getFilterSettings();
        
        var textWhitelistTooltip = data.useWhitelist() ?
                                     Component.translatable("tooltip.oritech.item_filter.whitelist")
                                     : Component.translatable("tooltip.oritech.item_filter.blacklist");
        
        var textNbtTooltip = data.useNbt() ?
                                     Component.translatable("tooltip.oritech.item_filter.nbt")
                                     : Component.translatable("tooltip.oritech.item_filter.no_nbt");
        
        var textNbtComponent = data.useComponents() ?
                                     Component.translatable("tooltip.oritech.item_filter.component")
                                     : Component.translatable("tooltip.oritech.item_filter.no_component");
        
        whiteListButton.tooltip(textWhitelistTooltip);
        nbtButton.tooltip(textNbtTooltip);
        componentButton.tooltip(textNbtComponent);
        
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        
        rootComponent
          .surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        var overlay = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(166));
        
        var gridContainer = Containers.grid(Sizing.content(0), Sizing.content(0), 3, 4);
        
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                
                var slotContainer = Containers.horizontalFlow(Sizing.fixed(18), Sizing.fixed(18));
                var background = Components.texture(ITEM_SLOT, 0, 0, 18, 18, 18, 18).positioning(Positioning.absolute(0, 0));
                
                int finalX = x;
                int finalY = y;
                background.mouseDown().subscribe(((mouseX, mouseY, button) -> onItemFrameBackgroundClicked(slotContainer, finalX, finalY)));
                
                slotContainer.child(background);
                var idIndex = y * 4 + x;
                gridContainers[idIndex] = slotContainer;
                gridContainer.child(slotContainer.margins(Insets.of(0, 2, 0, 1)), y, x);
                
            }
        }
        
        overlay.child(gridContainer.positioning(Positioning.absolute(5, 18)));
        
        var buttonWidth = 60;
        
        // sorry to whoever is reading this for this cursed section
        whiteListButton = Components.button(Component.literal("            ").append(Component.translatable("title.oritech.item_filter.whitelist").withColor(BasicMachineScreen.GRAY_TEXT_COLOR)), buttonComponent -> toggleWhitelist());
        whiteListButton.horizontalSizing(Sizing.fixed(buttonWidth));
        whiteListButton.renderer(createToggleRenderer(ignored -> ItemFilterScreen.this.menu.blockEntity.getFilterSettings().useWhitelist()));
        whiteListButton.textShadow(false);
        overlay.child(whiteListButton.positioning(Positioning.absolute(83, 18)));
        
        nbtButton = Components.button(Component.literal("      ").append(Component.translatable("title.oritech.item_filter.nbt").withColor(BasicMachineScreen.GRAY_TEXT_COLOR)), buttonComponent -> toggleNbt());
        nbtButton.horizontalSizing(Sizing.fixed(buttonWidth));
        nbtButton.renderer(createToggleRenderer(ignored -> ItemFilterScreen.this.menu.blockEntity.getFilterSettings().useNbt()));
        nbtButton.textShadow(false);
        overlay.child(nbtButton.positioning(Positioning.absolute(83, 38)));
        
        componentButton = Components.button(Component.literal("                ").append(Component.translatable("title.oritech.item_filter.component").withColor(BasicMachineScreen.GRAY_TEXT_COLOR)), buttonComponent -> toggleComponent());
        componentButton.horizontalSizing(Sizing.fixed(buttonWidth));
        componentButton.renderer(createToggleRenderer(ignored -> ItemFilterScreen.this.menu.blockEntity.getFilterSettings().useComponents()));
        componentButton.textShadow(false);
        overlay.child(componentButton.positioning(Positioning.absolute(83, 58)));
        
        addTitle(overlay);
        
        rootComponent.child(
          Components.texture(BasicMachineScreen.BACKGROUND, 0, 0, 176, 166, 176, 166)
        ).child(
          overlay.positioning(Positioning.relative(50, 50))
        );
        
        updateButtons();
        updateItemFilters();
    }
    
    public static ButtonComponent.Renderer createToggleRenderer(Predicate<ButtonComponent> activeSupplier) {
        return (owoUIDrawContext, button, v) -> {
            RenderSystem.enableDepthTest();
            var isOn = activeSupplier.test(button);
            var normalTexture = isOn ? Oritech.id("textures/gui/modular/toggle_on.png") : Oritech.id("textures/gui/modular/toggle_off.png");
            var hoverTexture = isOn ? Oritech.id("textures/gui/modular/toggle_on_hover.png") : Oritech.id("textures/gui/modular/toggle_off_hover.png");
            owoUIDrawContext.blit(button.isHovered() ? hoverTexture : normalTexture, button.x(), button.y(), 30, 16, 0, 0, 30, 16, 30, 16);
        };
    }
    
    
    private void addTitle(FlowLayout overlay) {
        var blockTitle = menu.blockEntity.getBlockState().getBlock().getName();
        var label = Components.label(blockTitle);
        label.color(new Color(64 / 255f, 64 / 255f, 64 / 255f));
        label.sizing(Sizing.fixed(176), Sizing.content(2));
        label.horizontalTextAlignment(HorizontalAlignment.CENTER);
        label.zIndex(1);
        overlay.child(label.positioning(Positioning.relative(50, 2)));
    }
    
    private void sendUpdateToServer() {
        NetworkManager.sendToServer(new ItemFilterBlockEntity.ItemFilterPayload(menu.blockPos, menu.blockEntity.getFilterSettings()));
    }
    
    private void toggleWhitelist() {
        
        var data = menu.blockEntity.getFilterSettings();
        var whitelist = data.useWhitelist();
        var newWhitelist = !whitelist;
        var newData = new ItemFilterBlockEntity.FilterData(data.useNbt(), newWhitelist, data.useComponents(), data.items());
        updateFilterSettings(newData); // this is only on client
        
        updateButtons();
        sendUpdateToServer();
    }
    
    private void toggleNbt() {
        
        var data = menu.blockEntity.getFilterSettings();
        var nbt = data.useNbt();
        var newNbt = !nbt;
        var newData = new ItemFilterBlockEntity.FilterData(newNbt, data.useWhitelist(), data.useComponents(), data.items());
        updateFilterSettings(newData); // this is only on client
        
        updateButtons();
        sendUpdateToServer();
    }
    
    private void toggleComponent() {
        
        var data = menu.blockEntity.getFilterSettings();
        var component = data.useComponents();
        var nbt = data.useNbt();
        var newComponent = !component;
        if (newComponent)
            nbt = true;
        var newData = new ItemFilterBlockEntity.FilterData(nbt, data.useWhitelist(), newComponent, data.items());
        updateFilterSettings(newData); // this is only on client
        
        updateButtons();
        sendUpdateToServer();
    }
    
    private boolean onItemFrameBackgroundClicked(FlowLayout slotContainer, int x, int y) {
        return acceptItemStack(slotContainer, this.menu.getCarried(), y * 4 + x);
    }

    public boolean acceptItemStack(ItemStack itemStack, int index) {
        return acceptItemStack(getItemContainer(index), itemStack, index);
    }

    public boolean acceptItemStack(FlowLayout slotContainer, ItemStack itemStack, int index) {
        if (slotContainer.children().size() >= 2) {
            slotContainer.removeChild(slotContainer.children().get(1));
        }

        if (itemStack.isEmpty()) {

            var oldData = menu.blockEntity.getFilterSettings();
            var itemFilters = new HashMap<>(oldData.items());
            itemFilters.remove(index);
            var newData = new ItemFilterBlockEntity.FilterData(oldData.useNbt(), oldData.useWhitelist(), oldData.useComponents(), itemFilters);
            updateFilterSettings(newData); // this is only on client
            sendUpdateToServer();

            return false;
        }

        var displayStack = new ItemStack(itemStack.getItem(), 1);

        if (itemStack.getComponents() != null)
            displayStack.applyComponents(itemStack.getComponents());

        var itemComponent = Components.item(displayStack);
        itemComponent.positioning(Positioning.absolute(1, 1));
        itemComponent.showOverlay(true);
        itemComponent.setTooltipFromStack(true);
        slotContainer.child(itemComponent);

        var oldData = menu.blockEntity.getFilterSettings();
        var itemFilters = new HashMap<>(oldData.items());
        itemFilters.put(index, displayStack);
        var newData = new ItemFilterBlockEntity.FilterData(oldData.useNbt(), oldData.useWhitelist(), oldData.useComponents(), itemFilters);
        updateFilterSettings(newData); // this is only on client

        Oritech.LOGGER.debug("stored map: " + itemFilters);
        sendUpdateToServer();

        return true;
    }

    public FlowLayout getItemContainer(int index) {
        return gridContainers[index];
    }

    private void updateFilterSettings(ItemFilterBlockEntity.FilterData filterData) {
        menu.blockEntity.setFilterSettings(filterData);
        cachedItems = filterData.items();
    }
}
