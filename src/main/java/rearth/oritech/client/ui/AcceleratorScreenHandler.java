package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorControllerBlockEntity;

import java.util.Objects;

public class AcceleratorScreenHandler extends BasicMachineScreenHandler {
    
    protected final AcceleratorControllerBlockEntity accelerator;
    
    public AcceleratorScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())));
    }
    
    public AcceleratorScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        accelerator = (AcceleratorControllerBlockEntity) this.blockEntity;
    }
}
