package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.DronePortEntity;
import rearth.oritech.item.tools.LaserTargetDesignator;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.api.fluid.FluidApi;

public class DroneScreenHandler extends UpgradableMachineScreenHandler {
    
    private final SimpleInventory cardInventory;
    private final FluidApi.SingleSlotStorage fluidTank;
    
    public DroneScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, MachineAddonController.AddonUiData addonUiData, float coreQuality) {
        super(syncId, playerInventory, blockEntity, addonUiData, coreQuality);

        if (!(blockEntity instanceof DronePortEntity dronePortEntity)) {
            cardInventory = null;
            fluidTank = null;
            Oritech.LOGGER.error("Opened drone screen on non-drone block, this should never happen");
            return;
        }
        
        cardInventory = dronePortEntity.getCardInventory();
        cardInventory.onOpen(playerInventory.player);
        addCardSlots();

        if (dronePortEntity.hasFluidAddon) {
            fluidTank = dronePortEntity.fluidStorage;
        } else {
            fluidTank = null;
        }
    }
    
    private void addCardSlots() {
        addSlot(new Slot(cardInventory, 0, 130, 26));
        addSlot(new Slot(cardInventory, 1, 130, 62));
    }
    
    // card slots are appended at end, so order is: machine inv - player inv - bucket inv
    @Override
    public int getPlayerInvEndSlot(ItemStack stack) {
        return super.getPlayerInvEndSlot(stack) - 2;
    }
    
    @Override
    public int getMachineInvStartSlot(ItemStack stack) {
        
        if (stack.getItem() instanceof LaserTargetDesignator)
            return this.slots.size() - 2;
        
        return super.getMachineInvStartSlot(stack);
    }
    
    @Override
    public int getMachineInvEndSlot(ItemStack stack) {
        
        if (stack.getItem() instanceof LaserTargetDesignator)
            return this.slots.size();
        
        return super.getMachineInvEndSlot(stack);
    }
}
