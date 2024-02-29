package rearth.oritech.block.entity.machines;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.DelegatingInventory;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.ImplementedInventory;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.DelegatingEnergyStorage;

import java.util.Objects;

public class MachineCoreEntity extends BlockEntity implements DelegatingInventory, EnergyProvider {
    
    private BlockPos controllerPos = BlockPos.ORIGIN;
    private UpgradableMachineBlockEntity controllerEntity;
    private final DelegatingEnergyStorage delegatedStorage = new DelegatingEnergyStorage(this::getMainStorage, this::isEnabled);
    
    public MachineCoreEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.MACHINE_CORE_ENTITY, pos, state);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("controller_x", controllerPos.getX());
        nbt.putInt("controller_y", controllerPos.getY());
        nbt.putInt("controller_z", controllerPos.getZ());
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        controllerPos = new BlockPos(nbt.getInt("controller_x"), nbt.getInt("controller_y"), nbt.getInt("controller_z"));
    }
    
    public BlockPos getControllerPos() {
        return controllerPos;
    }
    
    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        this.markDirty();
    }
    
    private UpgradableMachineBlockEntity getCachedController() {
        if (!this.getCachedState().get(MachineCoreBlock.USED)) return null;
        
        if (controllerEntity == null)
            controllerEntity = (UpgradableMachineBlockEntity) Objects.requireNonNull(world).getBlockEntity(getControllerPos());
        
        return controllerEntity;
    }
    
    @Override
    public ImplementedInventory getDelegatedInventory() {
        return getCachedController();
    }
    
    private EnergyStorage getMainStorage() {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        return Objects.requireNonNull(controllerEntity).getEnergyStorage();
    }
    
    @Override
    public boolean isEnabled() {
        return this.getCachedState().get(MachineCoreBlock.USED);
    }
    
    @Override
    public EnergyStorage getStorage() {
        return delegatedStorage;
    }
}
