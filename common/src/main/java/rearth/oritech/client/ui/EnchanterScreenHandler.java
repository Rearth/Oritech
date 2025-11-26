package rearth.oritech.client.ui;

import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EnchanterScreenHandler extends BasicMachineScreenHandler {
    
    protected final EnchanterBlockEntity enchanter;
    
    public EnchanterScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public EnchanterScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        enchanter = (EnchanterBlockEntity) this.blockEntity;
    }
}
