package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.network.NetworkContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static rearth.oritech.client.ui.BasicMachineScreen.getItemFrame;

public class InventoryProxyScreen extends BaseOwoHandledScreen<FlowLayout, InventoryProxyScreenHandler> {
    
    private final List<ButtonComponent> buttons = new ArrayList<>();
    
    public InventoryProxyScreen(InventoryProxyScreenHandler handler, PlayerInventory inventory, Text title) {
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
        
        var overlay = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(100));
        var spacer = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(166 - 100 + 5));
        
        rootComponent.child(overlay.surface(Surface.PANEL));
        rootComponent.child(spacer);
        
        for (var slot : Objects.requireNonNull(handler.controllerScreen).getGuiSlots()) {
            var button = Components.button(Text.literal(""), elem -> {
                setActiveSlot(slot.index());
            });
            buttons.add(button);
            overlay.child(getItemFrame(slot.x(), slot.y()));
            overlay.child(button.sizing(Sizing.fixed(10)).positioning(Positioning.absolute(slot.x() + 2, slot.y() + 2)));
        }
        
        for (int i = 0; i < buttons.size(); i++) {
            var button = buttons.get(i);
            button.active = i != handler.selectedSlot;
        }
        
        addTitle(overlay);
        
        var hint = Components.label(Text.translatable("tooltip.oritech.addon_proxy_select"));
        hint.horizontalTextAlignment(HorizontalAlignment.CENTER);
        hint.color(new Color(64 / 255f, 64 / 255f, 64 / 255f));
        hint.sizing(Sizing.fixed(176), Sizing.content(2));
        overlay.child(hint.positioning(Positioning.relative(50, 90)));
    }
    
    private void addTitle(FlowLayout overlay) {
        var blockTitle = handler.addonEntity.getCachedState().getBlock().getName();
        var label = Components.label(blockTitle);
        label.color(new Color(64 / 255f, 64 / 255f, 64 / 255f));
        label.sizing(Sizing.fixed(176), Sizing.content(2));
        label.horizontalTextAlignment(HorizontalAlignment.CENTER);
        label.zIndex(1);
        overlay.child(label.positioning(Positioning.relative(50, 2)));
    }
    
    private void setActiveSlot(int slot) {
        
        handler.selectedSlot = slot;
        
        for (int i = 0; i < buttons.size(); i++) {
            var button = buttons.get(i);
            button.active = i != slot;
        }
        
        // sync to client entity
        handler.addonEntity.setTargetSlot(slot);
        
        // sync to server entity
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.InventoryProxySlotSelectorPacket(handler.blockPos, slot));
    }
}
