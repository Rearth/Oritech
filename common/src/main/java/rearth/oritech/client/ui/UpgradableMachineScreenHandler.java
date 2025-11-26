package rearth.oritech.client.ui;

import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class UpgradableMachineScreenHandler extends BasicMachineScreenHandler {
    
    protected final Level worldAccess;
    protected final MachineAddonController addonController;
    
    public UpgradableMachineScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    // on server, also called from client constructor
    public UpgradableMachineScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        // sync speed and efficiency to client entity, so the getProgress method works correctly
        if (blockEntity instanceof MachineAddonController upgradableEntity) {
            addonController = upgradableEntity;
        } else {
            Oritech.LOGGER.debug("Creating Upgrade screen for non-upgradable block: {}", blockEntity);
            addonController = null;
        }
        
        this.worldAccess = playerInventory.player.level();
    }
    
    @Override
    public boolean showRedstoneAddon() {
        return super.showRedstoneAddon() ||
                 addonController.getConnectedAddons().stream().anyMatch(addonPos -> this.worldAccess.getBlockState(addonPos).getBlock().equals(BlockContent.MACHINE_REDSTONE_ADDON));
    }
}
