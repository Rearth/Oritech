package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;

public class CatalystScreenHandler extends BasicMachineScreenHandler {
    
    public final EnchantmentCatalystBlockEntity catalyst;
    
    public CatalystScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        if (this.blockEntity instanceof EnchantmentCatalystBlockEntity catalystEntity) {
            this.catalyst = catalystEntity;
        } else {
            this.catalyst = null;
        }
        
    }
}
