package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import rearth.oritech.block.entity.machines.interaction.DronePortEntity;
import rearth.oritech.item.tools.LaserTargetDesignator;

import java.util.Objects;

public class DroneScreenHandler extends BasicMachineScreenHandler {
    
    private final SimpleInventory cardInventory;
    
    public DroneScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())));
    }
    
    public DroneScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        cardInventory = ((DronePortEntity) blockEntity).getCardInventory();
        cardInventory.onOpen(playerInventory.player);
        addCardSlots();
    }
    
    private void addCardSlots() {
        addSlot(new Slot(cardInventory, 0, 130, 26));
        addSlot(new Slot(cardInventory, 1, 130, 62));
    }
    
    // card slots are appended at end, so order is: machine inv - player inv - bucket inv
    @Override
    public int getPlayerInvEndSlot(ItemStack stack) {
        return super.getPlayerInvEndSlot(stack) - 2;
    }
    
    @Override
    public int getMachineInvStartSlot(ItemStack stack) {
        
        if (stack.getItem() instanceof LaserTargetDesignator)
            return this.slots.size() - 2;
        
        return super.getMachineInvStartSlot(stack);
    }
    
    @Override
    public int getMachineInvEndSlot(ItemStack stack) {
        
        if (stack.getItem() instanceof LaserTargetDesignator)
            return this.slots.size();
        
        return super.getMachineInvEndSlot(stack);
    }
}
