package rearth.oritech.block.entity.machines;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.block.base.entity.UpgradableMachineBlockEntity;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.*;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.DelegatingEnergyStorage;

import java.util.Objects;

public class MachineCoreEntity extends BlockEntity implements InventoryProvider, EnergyProvider {
    
    private BlockPos controllerPos = BlockPos.ORIGIN;
    private MultiblockMachineController controllerEntity;
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
    
    private MultiblockMachineController getCachedController() {
        if (!this.getCachedState().get(MachineCoreBlock.USED)) return null;
        
        if (controllerEntity == null)
            controllerEntity = (MultiblockMachineController) Objects.requireNonNull(world).getBlockEntity(getControllerPos());
        
        return controllerEntity;
    }
    
    private EnergyStorage getMainStorage() {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        return Objects.requireNonNull(controllerEntity).getEnergyStorageForLink();
    }
    
    public boolean isEnabled() {
        return this.getCachedState().get(MachineCoreBlock.USED);
    }
    
    @Override
    public EnergyStorage getStorage() {
        return delegatedStorage;
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed || getCachedController() == null) return null;
        
        return getCachedController().getInventoryForLink().getInventory(direction);
    }
}
