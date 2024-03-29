package rearth.oritech.client.ui;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.processing.CentrifugeBlockEntity;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

import static rearth.oritech.util.MachineAddonController.ADDON_UI_ENDEC;

public class CentrifugeScreenHandler extends UpgradableMachineScreenHandler{
    
    public final SingleVariantStorage<FluidVariant> inputTank;
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())), buf.read(ADDON_UI_ENDEC), buf.readFloat());
    }
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, MachineAddonController.AddonUiData addonUiData, float coreQuality) {
        super(syncId, playerInventory, blockEntity, addonUiData, coreQuality);
        
        if (!(blockEntity instanceof CentrifugeBlockEntity centrifugeEntity)) {
            inputTank = null;
            Oritech.LOGGER.error("Opened centrifuge screen on non-centrifuge block, this should never happen");
            return;
        }
        
        inputTank = centrifugeEntity.inputStorage;
        
    }
}
