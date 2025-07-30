package rearth.oritech.client.ui;

import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RefineryScreenHandler extends UpgradableMachineScreenHandler {
    
    protected final RefineryBlockEntity refinery;
    protected FluidApi.SingleSlotStorage outputAContainer;
    protected FluidApi.SingleSlotStorage outputBContainer;
    protected FluidApi.SingleSlotStorage outputCContainer;
    
    public RefineryScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public RefineryScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
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
