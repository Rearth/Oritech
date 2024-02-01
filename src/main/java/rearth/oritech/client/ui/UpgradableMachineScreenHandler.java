package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;

import java.util.Objects;

import static rearth.oritech.block.base.entity.UpgradableMachineBlockEntity.ADDON_UI_ENDEC;
import static rearth.oritech.block.base.entity.UpgradableMachineBlockEntity.AddonUiData;

public class UpgradableMachineScreenHandler extends BasicMachineScreenHandler {
    
    protected final AddonUiData addonUiData;
    
    // on client, receiving data from writeScreenOpeningData
    public UpgradableMachineScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())), buf.read(ADDON_UI_ENDEC));
    }
    
    // on server, also called from client constructor
    public UpgradableMachineScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, AddonUiData addonUiData) {
        super(syncId, playerInventory, blockEntity);
        this.addonUiData = addonUiData;
        
        // sync speed and efficiency to client entity, so the getProgress method works correctly
        if (playerInventory.player.getWorld().isClient() && blockEntity instanceof UpgradableMachineBlockEntity upgradableEntity) {
            upgradableEntity.setCombinedSpeed(addonUiData.speed());
            upgradableEntity.setCombinedEfficiency(addonUiData.efficiency());
        }
    }
    
}
