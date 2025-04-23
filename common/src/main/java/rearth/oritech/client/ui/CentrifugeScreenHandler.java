package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.api.fluid.FluidApi;

public class CentrifugeScreenHandler extends UpgradableMachineScreenHandler {
    
    public final FluidApi.SingleSlotStorage inputTank;
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, MachineAddonController.AddonUiData addonUiData, float coreQuality) {
        super(syncId, playerInventory, blockEntity, addonUiData, coreQuality);
        
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
