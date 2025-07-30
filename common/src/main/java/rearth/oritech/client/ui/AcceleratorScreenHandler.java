package rearth.oritech.client.ui;

import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AcceleratorScreenHandler extends BasicMachineScreenHandler {
    
    protected final AcceleratorControllerBlockEntity accelerator;
    
    public AcceleratorScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public AcceleratorScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        accelerator = (AcceleratorControllerBlockEntity) this.blockEntity;
    }
}
