package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import rearth.oritech.block.entity.machines.interaction.DronePortEntity;

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
}
