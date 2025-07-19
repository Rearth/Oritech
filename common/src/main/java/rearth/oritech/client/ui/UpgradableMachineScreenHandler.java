package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

public class UpgradableMachineScreenHandler extends BasicMachineScreenHandler {
    
    protected final World worldAccess;
    protected final MachineAddonController addonController;
    
    public UpgradableMachineScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())));
    }
    
    // on server, also called from client constructor
    public UpgradableMachineScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        // sync speed and efficiency to client entity, so the getProgress method works correctly
        if (blockEntity instanceof MachineAddonController upgradableEntity) {
            addonController = upgradableEntity;
        } else {
            Oritech.LOGGER.debug("Creating Upgrade screen for non-upgradable block: {}", blockEntity);
            addonController = null;
        }
        
        this.worldAccess = playerInventory.player.getWorld();
    }
    
    @Override
    public boolean showRedstoneAddon() {
        return super.showRedstoneAddon() ||
                 addonController.getConnectedAddons().stream().anyMatch(addonPos -> this.worldAccess.getBlockState(addonPos).getBlock().equals(BlockContent.MACHINE_REDSTONE_ADDON));
    }
}
