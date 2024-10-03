package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

    // Won't affect player dragging items into slots, but quick-move will only allow enchanted books in the first slot
    public int getMachineInvStartSlot(ItemStack stack) {
        return stack.isOf(Items.ENCHANTED_BOOK) ? 0 : 1;
    }
}
