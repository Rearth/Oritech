package rearth.oritech.client.ui;

import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CatalystScreenHandler extends BasicMachineScreenHandler {
    
    public final EnchantmentCatalystBlockEntity catalyst;
    
    public CatalystScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public CatalystScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        if (this.blockEntity instanceof EnchantmentCatalystBlockEntity catalystEntity) {
            this.catalyst = catalystEntity;
        } else {
            this.catalyst = null;
        }
        
    }

    // Won't affect player dragging items into slots, but quick-move will only allow enchanted books in the first slot
    public int getMachineInvStartSlot(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) ? 0 : 1;
    }
}
