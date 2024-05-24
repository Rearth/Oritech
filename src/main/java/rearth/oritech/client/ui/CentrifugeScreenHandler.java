package rearth.oritech.client.ui;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.processing.CentrifugeBlockEntity;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

import static rearth.oritech.util.MachineAddonController.ADDON_UI_ENDEC;

public class CentrifugeScreenHandler extends UpgradableMachineScreenHandler {
    
    public final SingleVariantStorage<FluidVariant> inputTank;
    public final SimpleInventory bucketInventory;
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())), buf.read(ADDON_UI_ENDEC), buf.readFloat());
    }
    
    public CentrifugeScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, MachineAddonController.AddonUiData addonUiData, float coreQuality) {
        super(syncId, playerInventory, blockEntity, addonUiData, coreQuality);
        
        if (!(blockEntity instanceof CentrifugeBlockEntity centrifugeEntity)) {
            inputTank = null;
            bucketInventory = null;
            Oritech.LOGGER.error("Opened centrifuge screen on non-centrifuge block, this should never happen");
            return;
        }
        
        if (centrifugeEntity.hasFluidAddon) {
            inputTank = centrifugeEntity.inputStorage;
            bucketInventory = centrifugeEntity.bucketInventory;
            bucketInventory.onOpen(playerInventory.player);
            addBucketSlots();
        } else {
            inputTank = null;
            bucketInventory = null;
        }
    }
    
    private void addBucketSlots() {
        addSlot(new Slot(bucketInventory, 0, 130, -30));
        addSlot(new Slot(bucketInventory, 1, 130, -10));
    }
}
