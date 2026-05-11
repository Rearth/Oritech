package rearth.oritech.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.util.ScreenProvider;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class RedstoneAddonScreen extends OritechWidgetScreen<RedstoneAddonScreenHandler> {
    
    private final Map<Integer, ButtonWidget> slotButtons = new LinkedHashMap<>();
    
    public RedstoneAddonScreen(RedstoneAddonScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176,
          handler.blockEntity.activeMode == RedstoneAddonBlockEntity.RedstoneMode.OUTPUT_SLOT ? 194 : 100);
    }
    
    @Override
    protected void buildComponents() {
        var activeMode = menu.blockEntity.activeMode;
        setPanelSize(176, activeMode == RedstoneAddonBlockEntity.RedstoneMode.OUTPUT_SLOT ? 194 : 100);
        slotButtons.clear();
        
        var panel = new SurfaceWidget(0, 0, imageWidth, imageHeight);
        panel.withSurface(OritechSurface.PANEL);
        panel.withZIndex(-10);
        addComponent(panel);
        
        var activeLabel = new LabelWidget(8, 16, 160, 10, getModeText(activeMode));
        activeLabel.withAlignment(LabelWidget.Alignment.CENTER);
        addComponent(activeLabel);
        
        var descriptionLabel = new LabelWidget(8, 28, 160, 18, getDescriptionText(activeMode));
        descriptionLabel.withAlignment(LabelWidget.Alignment.CENTER);
        descriptionLabel.withDarkColor();
        descriptionLabel.withWrap(true);
        addComponent(descriptionLabel);
        
        addComponent(ButtonWidget.darkPanel(20, 58, 18, 14, Component.literal("<"),
          btn -> shiftMode(-1)).withTextColor(OritechWidgetScreen.SEPARATOR_COLOR).withSurfacePadding(Insets.of(1, 2, 1, 2)));
        addComponent(ButtonWidget.darkPanel(138, 58, 18, 14, Component.literal(">"),
          btn -> shiftMode(1)).withTextColor(OritechWidgetScreen.SEPARATOR_COLOR).withSurfacePadding(Insets.of(1, 0, 1, 3)));
        
        var modeIndicator = new LabelWidget(40, 61, 96, 10,
          Component.literal((activeMode.ordinal() + 1) + " / " + RedstoneAddonBlockEntity.RedstoneMode.values().length));
        modeIndicator.withAlignment(LabelWidget.Alignment.CENTER);
        modeIndicator.withDarkColor();
        addComponent(modeIndicator);
        
        if (activeMode == RedstoneAddonBlockEntity.RedstoneMode.OUTPUT_SLOT) {
            addSlotSelector();
        }
    }
    
    private void shiftMode(int delta) {
        var modes = RedstoneAddonBlockEntity.RedstoneMode.values();
        var nextIndex = Math.floorMod(menu.blockEntity.activeMode.ordinal() + delta, modes.length);
        menu.blockEntity.activeMode = modes[nextIndex];
        triggerServerUpdate();
        rebuildComponents();
    }
    
    private void addSlotSelector() {
        var controller = menu.blockEntity.getCachedController();
        if (!(controller instanceof ScreenProvider screenProvider)) return;
        
        var slots = screenProvider.getGuiSlots();
        
        addComponent(BoxWidget.filled(8, 100, 160, 1, LabelWidget.DARK_TEXT));
        
        var title = new LabelWidget(0, 116, 176, 10, Component.translatable("title.oritech.redstone_addon"));
        title.withAlignment(LabelWidget.Alignment.CENTER);
        title.withDarkColor();
        addComponent(title);
        
        for (var slot : slots) {
            addComponent(new ItemSlotWidget(slot.x(), 120 + slot.y()));
            
            var button = ButtonWidget.panel(slot.x() + 3, 120 + slot.y() + 3, 10, 10,
              Component.literal(""),
              elem -> setActiveSlot(slot.index()));
            slotButtons.put(slot.index(), button);
            addComponent(button);
        }
        
        setActiveSlot(menu.blockEntity.monitoredSlot);
        
        if (screenProvider.showProgress()) {
            var arrowConfig = screenProvider.getIndicatorConfiguration();
            addComponent(new TextureWidget(
              arrowConfig.x(), 120 + arrowConfig.y(),
              arrowConfig.width(), arrowConfig.height(),
              arrowConfig.full(), 0, 0,
              arrowConfig.width(), arrowConfig.height(),
              arrowConfig.width(), arrowConfig.height()));
        }
    }
    
    private void setActiveSlot(int slot) {
        for (var entry : slotButtons.entrySet()) {
            entry.getValue().setActive(entry.getKey() != slot);
        }
        
        menu.blockEntity.monitoredSlot = slot;
        triggerServerUpdate();
    }
    
    private Component getModeText(RedstoneAddonBlockEntity.RedstoneMode mode) {
        return Component.translatable("title.oritech.redstone_" + mode.toString().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GRAY);
    }
    
    @Override
    public BlockState getTitleState() {
        return menu.blockEntity.getBlockState();
    }
    
    private Component getDescriptionText(RedstoneAddonBlockEntity.RedstoneMode mode) {
        return Component.translatable("tooltip.oritech.redstone_" + mode.toString().toLowerCase(Locale.ROOT));
    }
    
    private void triggerServerUpdate() {
        menu.blockEntity.sendDataToServer();
    }
}
