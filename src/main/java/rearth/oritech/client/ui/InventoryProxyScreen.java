package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.ItemSlotWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.block.entity.addons.InventoryProxyAddonBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InventoryProxyScreen extends OritechWidgetScreen<InventoryProxyScreenHandler> {
    
    private final List<ButtonWidget> buttons = new ArrayList<>();
    
    public InventoryProxyScreen(InventoryProxyScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 100);
    }

    @Override
    protected void buildComponents() {
        buttons.clear();

        var panel = new SurfaceWidget(0, 0, 176, 100);
        panel.withSurface(OritechSurface.PANEL);
        panel.withZIndex(-10);
        addComponent(panel);

        for (var slot : Objects.requireNonNull(menu.controllerScreen).getGuiSlots()) {
            addComponent(new ItemSlotWidget(slot.x(), slot.y()));

            var button = ButtonWidget.panel(slot.x() + 3, slot.y() + 3, 10, 10,
                Component.literal(""),
                elem -> setActiveSlot(slot.index()));
            buttons.add(button);
            addComponent(button);
        }
        
        for (int i = 0; i < buttons.size(); i++) {
            var button = buttons.get(i);
            button.setActive(i != menu.selectedSlot);
        }

        var hint = new LabelWidget(0, 85, 176, 10,
            Component.translatable("tooltip.oritech.addon_proxy_select"));
        hint.withAlignment(LabelWidget.Alignment.CENTER);
        hint.withDarkColor();
        addComponent(hint);
    }
    
    @Override
    public BlockState getTitleState() {
        return menu.addonEntity.getBlockState();
    }
    
    private void setActiveSlot(int slot) {
        
        menu.selectedSlot = slot;
        
        for (int i = 0; i < buttons.size(); i++) {
            var button = buttons.get(i);
            button.setActive(i != slot);
        }
        
        // sync to client entity
        menu.addonEntity.setTargetSlot(slot);
        
        // sync to server entity
        NetworkManager.sendToServer(new InventoryProxyAddonBlockEntity.InventoryProxySlotSelectorPacket(menu.blockPos, slot));
    }
}
