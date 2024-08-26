package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;

import java.util.Objects;

public class EnchanterScreenHandler extends BasicMachineScreenHandler {
    
    protected final EnchanterBlockEntity enchanter;
    
    public EnchanterScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())));
    }
    
    public EnchanterScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        enchanter = (EnchanterBlockEntity) this.blockEntity;
    }
}
