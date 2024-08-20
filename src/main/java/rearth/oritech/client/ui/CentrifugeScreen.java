package rearth.oritech.client.ui;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.processing.CentrifugeBlockEntity;
import rearth.oritech.util.ScreenProvider;

public class CentrifugeScreen extends UpgradableMachineScreen<CentrifugeScreenHandler> {
    private final FluidDisplay inFluidDisplay;
    
    private static final ScreenProvider.BarConfiguration inputConfig = new ScreenProvider.BarConfiguration(28, 6, 21, 74);
    public static final Identifier BUCKET_SLOT = Oritech.id("textures/gui/modular/bucket_indicator.png");
    
    public CentrifugeScreen(CentrifugeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        
        if (((CentrifugeBlockEntity) handler.blockEntity).hasFluidAddon) {
            inFluidDisplay = initFluidDisplay(handler.inputTank, inputConfig);
        } else {
            inFluidDisplay = null;
        }
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
        System.out.println(inFluidDisplay);
        if (inFluidDisplay != null) {
            addFluidDisplay(overlay, inFluidDisplay);
            updateFluidDisplay(inFluidDisplay);
        }
        
    }
    
    @Override
    protected void handledScreenTick() {
        
        if (inFluidDisplay != null)
            updateFluidDisplay(inFluidDisplay);
        
        super.handledScreenTick();
    }
    
    @Override
    public void addExtensionComponents(FlowLayout container) {
        super.addExtensionComponents(container);
        
        if (!((CentrifugeBlockEntity) handler.blockEntity).hasFluidAddon) return;
        
        var childLayout = Containers.horizontalFlow(Sizing.fixed(60), Sizing.fixed(20));
        childLayout.margins(Insets.of(4, 1, 1, 1));
        childLayout.padding(Insets.of(1));
        
        childLayout.child(BasicMachineScreen.getItemFrame(2, 0));
        childLayout.child(BasicMachineScreen.getItemFrame(34, 0));
        childLayout.child(Components.texture(BUCKET_SLOT, 0, 0, 27, 18, 27, 18).positioning(Positioning.absolute(2, -1)));
        
        // ids: 9 * 4 + 3 = 39 (count of slot from playinv + centrifuge normal inv)
        childLayout.child(this.slotAsComponent(39).positioning(Positioning.absolute(2, 0)));
        childLayout.child(this.slotAsComponent(40).positioning(Positioning.absolute(34, 0)));
        
        container.child(Components.box(Sizing.fixed(73), Sizing.fixed(1)).color(new Color(0.8f, 0.8f, 0.8f)));
        container.child(childLayout);
    }
}
