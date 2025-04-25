package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.DronePortEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.item.tools.LaserTargetDesignator;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

public class DroneScreenHandler extends UpgradableMachineScreenHandler {
    
    private final SimpleInventory cardInventory;
    
    public DroneScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, ModScreens.UpgradableData.PACKET_CODEC.decode(buf));
    }
    
    public DroneScreenHandler(int syncId, PlayerInventory inventory, ModScreens.UpgradableData data) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(data.pos())), data.addonUiData(), data.coreQuality());
    }
    
    public DroneScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, MachineAddonController.AddonUiData addonUiData, float coreQuality) {
        super(syncId, playerInventory, blockEntity, addonUiData, coreQuality);

        if (!(blockEntity instanceof DronePortEntity dronePortEntity)) {
            cardInventory = null;
            Oritech.LOGGER.error("Opened drone screen on non-drone block, this should never happen");
            return;
        }
        
        cardInventory = dronePortEntity.getCardInventory();
        cardInventory.onOpen(playerInventory.player);
        addCardSlots();
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
