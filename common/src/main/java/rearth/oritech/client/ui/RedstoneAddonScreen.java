package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static rearth.oritech.client.ui.BasicMachineScreen.ORITECH_PANEL;
import static rearth.oritech.client.ui.BasicMachineScreen.getItemFrame;


public class RedstoneAddonScreen extends BaseOwoHandledScreen<FlowLayout, RedstoneAddonScreenHandler> {
    
    private LabelComponent activeLabel;
    private FlowLayout overlay;
    private final List<ButtonComponent> buttons = new ArrayList<>();
    private FlowLayout buttonContainer;
    private LabelComponent descriptionLabel;
    
    public RedstoneAddonScreen(RedstoneAddonScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
          .surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        overlay = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(100));
        overlay.padding(Insets.of(3)).margins(Insets.of(3));
        var spacer = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(166 - 100 + 5));
        
        rootComponent.child(overlay.surface(ORITECH_PANEL));
        rootComponent.child(spacer);
        
        var modes = RedstoneAddonBlockEntity.RedstoneMode.values();
        var activeMode = menu.blockEntity.activeMode;
        
        activeLabel = Components.label(getModeText(activeMode));
        activeLabel.horizontalTextAlignment(HorizontalAlignment.CENTER);
        activeLabel.sizing(Sizing.fill(), Sizing.content(2));
        activeLabel.positioning(Positioning.absolute(0, 15));
        overlay.child(activeLabel);
        
        descriptionLabel = Components.label(getDescriptionText(activeMode));
        descriptionLabel.horizontalTextAlignment(HorizontalAlignment.CENTER);
        descriptionLabel.sizing(Sizing.fill(), Sizing.content(2));
        descriptionLabel.positioning(Positioning.absolute(0, 25));
        overlay.child(descriptionLabel);
        
        var slider = Components.discreteSlider(Sizing.fixed(160), 0, modes.length - 1);
        slider.positioning(Positioning.absolute(8, 63));
        slider.snap(true);
        slider.onChanged().subscribe(this::onModeSelected);
        slider.setFromDiscreteValue(activeMode.ordinal());
        overlay.child(slider);
        
        addTitle(overlay);
    }
    
    private void onModeSelected(double mode) {
        var modes = RedstoneAddonBlockEntity.RedstoneMode.values();
        if (mode >= modes.length) return;
        var activeMode = modes[(int) mode];
        activeLabel.text(getModeText(activeMode));
        descriptionLabel.text(getDescriptionText(activeMode));
        
        menu.blockEntity.activeMode = activeMode;
        
        if (activeMode.equals(RedstoneAddonBlockEntity.RedstoneMode.OUTPUT_SLOT)) {
            overlay.verticalSizing().animate(250, Easing.CUBIC, Sizing.fixed(194)).forwards();
            addSlotSelector();
        } else if (overlay.verticalSizing().get().value != 100) {
            overlay.verticalSizing().animate(270, Easing.CUBIC, Sizing.fixed(100)).forwards();
            removeSlotSelector();
        }
        
        triggerServerUpdate();
    }
    
    private void addSlotSelector() {
        removeSlotSelector();   // in case the user is moving really fast and the call order is messed up
        
        var controller = menu.blockEntity.getCachedController();
        if (!(controller instanceof ScreenProvider screenProvider)) return;
        
        var slots = screenProvider.getGuiSlots();
        buttonContainer = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(100));
        buttonContainer.positioning(Positioning.absolute(0, 100));
        
        // small separator line
        buttonContainer.child(Components.box(Sizing.fixed(160), Sizing.fixed(1)).color(new Color(0.1f, 0.1f, 0.1f)).positioning(Positioning.absolute(8, 0)));
        
        var title = Components.label(Component.translatable("title.oritech.redstone_addon"));
        title.horizontalTextAlignment(HorizontalAlignment.CENTER);
        buttonContainer.child(title.positioning(Positioning.relative(50, 5)));
        
        for (var slot : slots) {
            var button = Components.button(Component.literal(" "), elem -> {
                setActiveSlot(slot.index());
            });
            buttons.add(button);
            buttonContainer.child(getItemFrame(slot.x(), slot.y()));
            buttonContainer.child(button.sizing(Sizing.fixed(10)).positioning(Positioning.absolute(slot.x() + 3, slot.y() + 3)));
        }
        
        setActiveSlot(menu.blockEntity.monitoredSlot);
        
        if (screenProvider.showProgress()) {
            var arrowConfig = screenProvider.getIndicatorConfiguration();
            var arrow = Components.texture(arrowConfig.full(), 0, 0, arrowConfig.width(), arrowConfig.height(), arrowConfig.width(), arrowConfig.height());
            buttonContainer.child(arrow.positioning(Positioning.absolute(arrowConfig.x(), arrowConfig.y())));
        }
        
        overlay.child(buttonContainer);
        
    }
    
    private void removeSlotSelector() {
        if (buttonContainer == null) return;
        overlay.removeChild(buttonContainer);
        buttons.clear();
        buttonContainer = null;
    }
    
    private void setActiveSlot(int slot) {
        for (int i = 0; i < buttons.size(); i++) {
            var button = buttons.get(i);
            button.active = i != slot;
        }
        
        menu.blockEntity.monitoredSlot = slot;
        triggerServerUpdate();
    }
    
    private Component getModeText(RedstoneAddonBlockEntity.RedstoneMode mode) {
        return Component.translatable("title.oritech.redstone_" + mode.toString().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GRAY);
    }
    
    private Component getDescriptionText(RedstoneAddonBlockEntity.RedstoneMode mode) {
        return Component.translatable("tooltip.oritech.redstone_" + mode.toString().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.BLACK);
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
    
    private void triggerServerUpdate() {
        menu.blockEntity.sendDataToServer();
    }
}
