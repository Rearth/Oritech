package rearth.oritech.block.entity.addons;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.energy.containers.DelegatingEnergyStorage;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

public class EnergyAcceptorAddonBlockEntity extends AddonBlockEntity implements EnergyApi.BlockProvider {
    private final DelegatingEnergyStorage delegatedStorage = new DelegatingEnergyStorage(this::getMainStorage, this::isConnected);
    
    private MachineAddonController cachedController;
    
    public EnergyAcceptorAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_ACCEPTOR_ADDON_ENTITY, pos, state);
    }
    
    private boolean isConnected() {
        var isUsed = this.getCachedState().get(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    private EnergyApi.EnergyContainer getMainStorage() {
        
        var isUsed = this.getCachedState().get(MachineAddonBlock.ADDON_USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        return controllerEntity.getStorageForAddon();
    }
    
    private MachineAddonController getCachedController() {
        
        if (cachedController != null)
            return cachedController;
        
        cachedController = (MachineAddonController) Objects.requireNonNull(world).getBlockEntity(getControllerPos());
        return cachedController;
    }
    
    @Override
    public EnergyApi.EnergyContainer getStorage(Direction direction) {
        return delegatedStorage;
    }
}
