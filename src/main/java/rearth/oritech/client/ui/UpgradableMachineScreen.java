package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;


public class UpgradableMachineScreen extends BasicMachineScreen<UpgradableMachineScreenHandler> {
    public UpgradableMachineScreen(UpgradableMachineScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    public void addExtensionComponents(FlowLayout container) {
        super.addExtensionComponents(container);
        
        var speed = String.format("%.0f", 1 / handler.addonUiData.speed() * 100);
        var efficiency = String.format("%.0f", 1 / handler.addonUiData.efficiency() * 100);
        
        container.child(Components.box(Sizing.fixed(73), Sizing.fixed(1)).color(new Color(0.8f, 0.8f, 0.8f)));
        container.child(Components.label(Text.literal("⌛ " + speed + "%")).tooltip(Text.literal("Processing Speed")).margins(Insets.of(3)));
        container.child(Components.label(Text.literal("⚡ " + efficiency + "%")).tooltip(Text.literal("Energy Efficiency")).margins(Insets.of(3)));
        
    }
}
