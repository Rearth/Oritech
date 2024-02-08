package rearth.oritech.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.block.custom.MachineAddonBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.EnergyProvider;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.DelegatingEnergyStorage;

import java.util.Objects;

public class CapacitorAddonBlockEntity extends AddonBlockEntity implements EnergyProvider {
    private final DelegatingEnergyStorage delegatedStorage = new DelegatingEnergyStorage(this::getMainStorage, this::isConnected);
    
    private UpgradableMachineBlockEntity cachedController;
    
    public CapacitorAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CAPACITOR_ADDON_ENTITY, pos, state);
    }
    
    private boolean isConnected() {
        var isUsed = this.getCachedState().get(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    private EnergyStorage getMainStorage() {
        
        var isUsed = this.getCachedState().get(MachineAddonBlock.ADDON_USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        return controllerEntity.getEnergyStorage();
    }
    
    private UpgradableMachineBlockEntity getCachedController() {
        
        if (cachedController != null && !cachedController.isRemoved())
            return cachedController;
        
        cachedController = (UpgradableMachineBlockEntity) Objects.requireNonNull(world).getBlockEntity(getControllerPos());
        return cachedController;
    }
    
    @Override
    public EnergyStorage getStorage() {
        return delegatedStorage;
    }
}
