package rearth.oritech.block.entity.machines.addons;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.FluidProvider;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

public class SteamBoilerAddonBlockEntity extends AddonBlockEntity implements FluidProvider {
    
    private UpgradableGeneratorBlockEntity cachedController;
    
    public SteamBoilerAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.STEAM_BOILER_ADDON_ENTITY, pos, state);
    }
    
    private boolean isConnected() {
        var isUsed = this.getCachedState().get(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    private MachineAddonController getCachedController() {
        
        if (cachedController != null)
            return cachedController;
        
        cachedController = (UpgradableGeneratorBlockEntity) Objects.requireNonNull(world).getBlockEntity(getControllerPos());
        return cachedController;
    }
    
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        if (!isConnected()) return null;
        return cachedController.exposedStorage;
    }
}
