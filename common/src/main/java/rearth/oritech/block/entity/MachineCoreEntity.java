package rearth.oritech.block.entity;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.*;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.DelegatingEnergyStorage;
import rearth.oritech.util.energy.containers.SimpleEnergyStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MachineCoreEntity extends BlockEntity implements InventoryProvider, EnergyApi.BlockProvider, FluidProvider {
    
    private BlockPos controllerPos = BlockPos.ORIGIN;
    private MultiblockMachineController controllerEntity;
    private final DelegatingEnergyStorage delegatedEnergy = new DelegatingEnergyStorage(this::getMainEnergyStorage, this::isEnabled);
    private final Map<Direction, DelegatingFluidStorage> delegatedFluid = new HashMap<>(6);
    private final Map<Direction, DelegatingItemStorage> delegatedItem = new HashMap<>(6);
    
    public MachineCoreEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.MACHINE_CORE_ENTITY, pos, state);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("controller_x", controllerPos.getX());
        nbt.putInt("controller_y", controllerPos.getY());
        nbt.putInt("controller_z", controllerPos.getZ());
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        controllerPos = new BlockPos(nbt.getInt("controller_x"), nbt.getInt("controller_y"), nbt.getInt("controller_z"));
    }
    
    public BlockPos getControllerPos() {
        return controllerPos;
    }
    
    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        this.controllerEntity = null;    // forces cache reload
        this.markDirty();
    }
    
    public MultiblockMachineController getCachedController() {
        if (!this.getCachedState().get(MachineCoreBlock.USED)) return null;
        
        if (controllerEntity == null || ((BlockEntity) controllerEntity).isRemoved())
            controllerEntity = (MultiblockMachineController) Objects.requireNonNull(world).getBlockEntity(getControllerPos());
        
        return controllerEntity;
    }
    
    private EnergyApi.EnergyContainer getMainEnergyStorage() {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (controllerEntity == null) return new SimpleEnergyStorage(100, 0, 0);    // this should never happen
        return controllerEntity.getEnergyStorageForLink();
    }
    
    private Storage<FluidVariant> getMainFluidStorage(Direction direction) {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (!(controllerEntity instanceof FluidProvider fluidProvider)) return null;
        return fluidProvider.getFluidStorage(direction);
    }
    
    private Storage<ItemVariant> getMainItemStorage(Direction direction) {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (!(controllerEntity instanceof InventoryProvider itemProvider)) return null;
        return itemProvider.getInventory(direction);
    }
    
    private Storage<FluidVariant> getFluidStorageDelegated(Direction direction) {
        return delegatedFluid.computeIfAbsent(direction, dir -> new DelegatingFluidStorage(() -> getMainFluidStorage(dir), this::isEnabled));
    }
    
    private Storage<ItemVariant> getItemStorageDelegated(Direction direction) {
        return delegatedItem.computeIfAbsent(direction, dir -> new DelegatingItemStorage(() -> getMainItemStorage(dir), this::isEnabled));
    }
    
    public boolean isEnabled() {
        return this.getCachedState().get(MachineCoreBlock.USED);
    }
    
    @Override
    public EnergyApi.EnergyContainer getStorage(Direction direction) {
        if (getCachedController() == null || getCachedController().getEnergyStorageForLink() == null) {
            return null;
        } else {
            return delegatedEnergy;
        }
    }
    
    @Override
    public Storage<ItemVariant> getInventory(Direction direction) {
        return getItemStorageDelegated(direction);
    }
    
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        return getFluidStorageDelegated(direction);
    }
}
