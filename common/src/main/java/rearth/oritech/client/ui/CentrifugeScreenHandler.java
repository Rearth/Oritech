package rearth.oritech.client.ui;

import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CentrifugeScreenHandler extends UpgradableMachineScreenHandler {
    
    public final FluidApi.SingleSlotStorage inputTank;
    
    public CentrifugeScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public CentrifugeScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
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
