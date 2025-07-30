package rearth.oritech.client.ui;

import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.DronePortEntity;
import rearth.oritech.item.tools.LaserTargetDesignator;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DroneScreenHandler extends UpgradableMachineScreenHandler {
    
    private final SimpleContainer cardInventory;
    
    public DroneScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public DroneScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);

        if (!(blockEntity instanceof DronePortEntity dronePortEntity)) {
            cardInventory = null;
            Oritech.LOGGER.error("Opened drone screen on non-drone block, this should never happen");
            return;
        }
        
        cardInventory = dronePortEntity.getCardInventory();
        cardInventory.startOpen(playerInventory.player);
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
