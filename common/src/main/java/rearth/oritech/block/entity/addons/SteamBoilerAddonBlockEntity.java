package rearth.oritech.block.entity.addons;

import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SteamBoilerAddonBlockEntity extends AddonBlockEntity implements FluidApi.BlockProvider {
    
    private UpgradableGeneratorBlockEntity cachedController;
    
    public SteamBoilerAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.STEAM_BOILER_ADDON_ENTITY, pos, state);
    }
    
    private boolean isConnected() {
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    private MachineAddonController getCachedController() {
        
        if (cachedController != null)
            return cachedController;
        
        var candidate = Objects.requireNonNull(level).getBlockEntity(getControllerPos());
        if (candidate instanceof UpgradableGeneratorBlockEntity generator) {
            cachedController = generator;
        }
        return cachedController;
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(Direction direction) {
        if (!isConnected()) return null;
        return cachedController.boilerStorage;
    }
}
