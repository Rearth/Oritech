package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;

import java.util.Objects;

public class RefineryScreenHandler extends UpgradableMachineScreenHandler {
    
    protected final RefineryBlockEntity refinery;
    protected FluidApi.SingleSlotStorage outputAContainer;
    protected FluidApi.SingleSlotStorage outputBContainer;
    protected FluidApi.SingleSlotStorage outputCContainer;
    
    public RefineryScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())));
    }
    
    public RefineryScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        if (!(blockEntity instanceof RefineryBlockEntity refineryEntity)) {
            throw new IllegalStateException("Opened centrifuge screen on non-centrifuge block, this should never happen");
        }
        
        this.refinery = refineryEntity;
        
        this.mainFluidContainer = refineryEntity.ownStorage.getInputContainer();
        this.outputAContainer = refineryEntity.ownStorage.getOutputContainer();
        this.outputBContainer = refineryEntity.nodeA;
        this.outputCContainer = refineryEntity.nodeB;
        
    }
}
