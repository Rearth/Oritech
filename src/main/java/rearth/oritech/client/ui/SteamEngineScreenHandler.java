package rearth.oritech.client.ui;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import rearth.oritech.block.entity.machines.generators.SteamEngineEntity;
import rearth.oritech.util.MachineAddonController;

public class SteamEngineScreenHandler extends UpgradableMachineScreenHandler {
    
    protected final SingleVariantStorage<FluidVariant> engineWaterStorage;
    
    public SteamEngineScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity, MachineAddonController.AddonUiData addonUiData, float coreQuality) {
        super(syncId, playerInventory, blockEntity, addonUiData, coreQuality);
        engineWaterStorage = ((SteamEngineEntity) blockEntity).waterStorage;
    }
}
