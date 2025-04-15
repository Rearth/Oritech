package rearth.oritech.client.ui;

import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;
import rearth.oritech.util.ScreenProvider;

public class CentrifugeScreen extends UpgradableMachineScreen<CentrifugeScreenHandler> {
    private final FluidDisplay inFluidDisplay;
    
    private static final ScreenProvider.BarConfiguration inputConfig = new ScreenProvider.BarConfiguration(28, 6, 21, 74);
    
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
}
