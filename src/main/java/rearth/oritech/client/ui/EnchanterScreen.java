package rearth.oritech.client.ui;

import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class EnchanterScreen extends BasicMachineScreen<EnchanterScreenHandler> {
    
    public EnchanterScreen(EnchanterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    public boolean showExtensionPanel() {
        return false;
    }
    
    @Override
    public void fillOverlay(FlowLayout overlay) {
        super.fillOverlay(overlay);
        
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
    }
}
