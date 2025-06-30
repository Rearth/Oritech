package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;

import java.util.Objects;

public class CentrifugeScreenHandler extends UpgradableMachineScreenHandler {
    
    public final FluidApi.SingleSlotStorage inputTank;
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())));
    }
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        if (!(blockEntity instanceof CentrifugeBlockEntity centrifugeEntity)) {
            inputTank = null;
            Oritech.LOGGER.error("Opened centrifuge screen on non-centrifuge block, this should never happen");
            return;
        }
        
        if (centrifugeEntity.hasFluidAddon) {
            inputTank = centrifugeEntity.fluidContainer.getInputContainer();
            this.mainFluidContainer = centrifugeEntity.fluidContainer.getOutputContainer();
        } else {
            inputTank = null;
        }
    }
}
