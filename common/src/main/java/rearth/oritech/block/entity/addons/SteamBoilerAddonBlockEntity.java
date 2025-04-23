package rearth.oritech.block.entity.addons;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.api.fluid.FluidApi;

import java.util.Objects;

public class SteamBoilerAddonBlockEntity extends AddonBlockEntity implements FluidApi.BlockProvider {
    
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
    public FluidApi.FluidStorage getFluidStorage(Direction direction) {
        if (!isConnected()) return null;
        return cachedController.boilerStorage;
    }
}
