package rearth.oritech.client.ui;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.processing.CentrifugeBlockEntity;
import rearth.oritech.util.MachineAddonController;

public class CentrifugeScreenHandler extends UpgradableMachineScreenHandler {
    
    public final SingleVariantStorage<FluidVariant> inputTank;
    public final SimpleInventory bucketInventory;
    
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
    
    // bucket slots are appended at end, so order is: machine inv - player inv - bucket inv
    @Override
    public int getPlayerInvEndSlot(ItemStack stack) {
        return super.getPlayerInvEndSlot(stack) - 2;
    }
    
    @Override
    public int getMachineInvStartSlot(ItemStack stack) {
        
        if (stack.getItem() instanceof BucketItem && inputTank != null)
            return this.slots.size() - 2;
        
        return super.getMachineInvStartSlot(stack);
    }
    
    @Override
    public int getMachineInvEndSlot(ItemStack stack) {
        
        if (stack.getItem() instanceof BucketItem && inputTank != null)
            return this.slots.size();
        
        return super.getMachineInvEndSlot(stack);
    }
}
