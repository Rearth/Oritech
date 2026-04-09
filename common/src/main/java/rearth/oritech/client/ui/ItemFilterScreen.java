package rearth.oritech.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.widgets.ItemSlotWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ToggleWidget;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;

import java.util.HashMap;

public class ItemFilterScreen extends OritechWidgetScreen<ItemFilterScreenHandler> {
    
    public static final int FILTER_SIZE = 12;
    
    private ToggleWidget whitelistButton;
    private ToggleWidget nbtButton;
    private ToggleWidget componentButton;
    
    public ItemFilterScreen(ItemFilterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 166, OritechMachineScreen.BACKGROUND);
    }
    
    @Override
    protected void buildComponents() {
        addFilterGrid();
        addToggleButtons();
        addPlayerInventorySlots();
        updateButtons();
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        for (var component : components) {
            component.tick();
        }
        updateButtons();
    }
    
    public void updateItemFilters() {
    }
    
    private void addFilterGrid() {
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                int index = y * 4 + x;
                int posX = 5 + x * 20;
                int posY = 18 + y * 20;
                addComponent(new ItemSlotWidget(posX, posY));
                addComponent(new FilterSlotWidget(posX - 1, posY - 1, index));
            }
        }
    }
    
    private void addToggleButtons() {
        var data = menu.blockEntity.getFilterSettings();
        
        whitelistButton = ToggleWidget.of(83, 18,
          Component.translatable("title.oritech.item_filter.whitelist"), data.useWhitelist(),
          (button, state) -> toggleWhitelist()).withTextColor(LabelWidget.DARK_TEXT);
        
        nbtButton = ToggleWidget.of(83, 38,
          Component.translatable("title.oritech.item_filter.nbt"), data.useNbt(),
          (button, state) -> toggleNbt()).withTextColor(LabelWidget.DARK_TEXT);
        
        componentButton = ToggleWidget.of(83, 58,
          Component.translatable("title.oritech.item_filter.component"), data.useComponents(),
          (button, state) -> toggleComponent()).withTextColor(LabelWidget.DARK_TEXT);
        
        addComponent(whitelistButton);
        addComponent(nbtButton);
        addComponent(componentButton);
    }
    
    private void addPlayerInventorySlots() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addComponent(new ItemSlotWidget(8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addComponent(new ItemSlotWidget(8 + col * 18, 142));
        }
    }
    
    private void updateButtons() {
        var data = menu.blockEntity.getFilterSettings();
        
        whitelistButton.withTooltip(data.useWhitelist()
                                      ? Component.translatable("tooltip.oritech.item_filter.whitelist")
                                      : Component.translatable("tooltip.oritech.item_filter.blacklist"));
        
        nbtButton.withTooltip(data.useNbt()
                                ? Component.translatable("tooltip.oritech.item_filter.nbt")
                                : Component.translatable("tooltip.oritech.item_filter.no_nbt"));
        
        componentButton.withTooltip(data.useComponents()
                                      ? Component.translatable("tooltip.oritech.item_filter.component")
                                      : Component.translatable("tooltip.oritech.item_filter.no_component"));
    }
    
    private void sendUpdateToServer() {
        NetworkManager.sendToServer(new ItemFilterBlockEntity.ItemFilterPayload(menu.blockPos, menu.blockEntity.getFilterSettings()));
    }
    
    private void toggleWhitelist() {
        var data = menu.blockEntity.getFilterSettings();
        updateFilterSettings(new ItemFilterBlockEntity.FilterData(data.useNbt(), !data.useWhitelist(), data.useComponents(), data.items()));
        sendUpdateToServer();
    }
    
    private void toggleNbt() {
        var data = menu.blockEntity.getFilterSettings();
        updateFilterSettings(new ItemFilterBlockEntity.FilterData(!data.useNbt(), data.useWhitelist(), data.useComponents(), data.items()));
        sendUpdateToServer();
    }
    
    private void toggleComponent() {
        var data = menu.blockEntity.getFilterSettings();
        var newComponents = !data.useComponents();
        var nbt = newComponents || data.useNbt();
        updateFilterSettings(new ItemFilterBlockEntity.FilterData(nbt, data.useWhitelist(), newComponents, data.items()));
        sendUpdateToServer();
    }
    
    private void updateFilterSettings(ItemFilterBlockEntity.FilterData filterData) {
        menu.blockEntity.setFilterSettings(filterData);
    }
    
    @Override
    public BlockState getTitleState() {
        return menu.blockEntity.getBlockState();
    }
    
    private final class FilterSlotWidget extends rearth.oritech.api.screen.UIComponent {
        
        private final int index;
        
        private FilterSlotWidget(int x, int y, int index) {
            super(x, y, 18, 18);
            this.index = index;
            this.zIndex = 1;
        }
        
        @Override
        protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            var stack = getDisplayedStack(index);
            if (stack.isEmpty()) return;
            
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(net.minecraft.client.Minecraft.getInstance().font, stack, x + 1, y + 1);
        }
        
        @Override
        public boolean handleClick(double mouseX, double mouseY, int button) {
            return acceptItemStack(menu.getCarried(), index);
        }
        
        @Override
        public boolean hasTooltip() {
            return !getDisplayedStack(index).isEmpty();
        }
        
        @Override
        public java.util.List<Component> getTooltip() {
            var stack = getDisplayedStack(index);
            if (stack.isEmpty()) {
                return super.getTooltip();
            }
            return Screen.getTooltipFromItem(net.minecraft.client.Minecraft.getInstance(), stack);
        }
    }
    
    public boolean acceptItemStack(ItemStack itemStack, int index) {
        var oldData = menu.blockEntity.getFilterSettings();
        var itemFilters = new HashMap<>(oldData.items());
        
        if (itemStack.isEmpty()) {
            itemFilters.remove(index);
            updateFilterSettings(new ItemFilterBlockEntity.FilterData(oldData.useNbt(), oldData.useWhitelist(), oldData.useComponents(), itemFilters));
            sendUpdateToServer();
            return false;
        }
        
        var displayStack = new ItemStack(itemStack.getItem(), 1);
        displayStack.applyComponents(itemStack.getComponents());
        itemFilters.put(index, displayStack);
        updateFilterSettings(new ItemFilterBlockEntity.FilterData(oldData.useNbt(), oldData.useWhitelist(), oldData.useComponents(), itemFilters));
        sendUpdateToServer();
        return true;
    }
    
    private ItemStack getDisplayedStack(int index) {
        return menu.blockEntity.getFilterSettings().items().getOrDefault(index, ItemStack.EMPTY);
    }
    
    public FilterSlotBounds getItemContainer(int index) {
        int x = 5 + (index % 4) * 20;
        int y = 18 + (index / 4) * 20;
        return new FilterSlotBounds(leftPos + x, topPos + y, 18, 18);
    }
    
    public record FilterSlotBounds(int x, int y, int width, int height) {
        public boolean isInBoundingBox(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
