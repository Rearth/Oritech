package rearth.oritech.block.entity.addons;

import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DelegatingEnergyStorage;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyAcceptorAddonBlockEntity extends AddonBlockEntity implements EnergyApi.BlockProvider {
    private final DelegatingEnergyStorage delegatedStorage = new DelegatingEnergyStorage(this::getMainStorage, this::isConnected);
    
    private MachineAddonController cachedController;
    
    public EnergyAcceptorAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_ACCEPTOR_ADDON_ENTITY, pos, state);
    }
    
    private boolean isConnected() {
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    private EnergyApi.EnergyStorage getMainStorage() {
        
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        return controllerEntity.getStorageForAddon();
    }
    
    private MachineAddonController getCachedController() {
        
        if (cachedController != null)
            return cachedController;
        
        cachedController = (MachineAddonController) Objects.requireNonNull(level).getBlockEntity(getControllerPos());
        return cachedController;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return delegatedStorage;
    }
}
