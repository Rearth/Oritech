package rearth.oritech.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DelegatingEnergyStorage;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.DelegatingFluidStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.DelegatingInventoryStorage;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MultiblockMachineController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MachineCoreEntity extends BlockEntity implements ItemApi.BlockProvider, EnergyApi.BlockProvider, FluidApi.BlockProvider {
    
    private BlockPos controllerPos = BlockPos.ORIGIN;
    private MultiblockMachineController controllerEntity;
    private final Map<Direction, DelegatingEnergyStorage> delegatedEnergy = new HashMap<>(6);
    private final Map<Direction, DelegatingFluidStorage> delegatedFluid = new HashMap<>(6);
    private final Map<Direction, DelegatingInventoryStorage> delegatedItem = new HashMap<>(6);
    
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
    
    @Nullable
    public MultiblockMachineController getCachedController() {
        if (world == null || !this.getCachedState().get(MachineCoreBlock.USED)) return null;
        
        if (controllerEntity == null || ((BlockEntity) controllerEntity).isRemoved()) {
            var candidate = Objects.requireNonNull(world).getBlockEntity(getControllerPos());
            if (candidate instanceof MultiblockMachineController controller) {
                controllerEntity = controller;
            } else {
                controllerEntity = null;
            }
        }
        
        return controllerEntity;
    }
    
    @Nullable
    private EnergyApi.EnergyStorage getMainEnergyStorage(Direction direction) {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (controllerEntity == null) return new SimpleEnergyStorage(0, 0, 0);    // this should never happen
        return controllerEntity.getEnergyStorageForMultiblock(direction);
    }
    
    private FluidApi.FluidStorage getMainFluidStorage(Direction direction) {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (!(controllerEntity instanceof FluidApi.BlockProvider fluidProvider)) return null;
        return fluidProvider.getFluidStorage(direction);
    }
    
    private ItemApi.InventoryStorage getMainItemStorage(Direction direction) {
        
        var isUsed = this.getCachedState().get(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (!(controllerEntity instanceof ItemApi.BlockProvider itemProvider)) return null;
        return itemProvider.getInventoryStorage(direction);
    }
    
    @Nullable
    private EnergyApi.EnergyStorage getEnergyStorageDelegated(Direction direction) {
        return delegatedEnergy.computeIfAbsent(direction, dir -> {
            if (getMainEnergyStorage(dir) == null) return null;
            return new DelegatingEnergyStorage(() -> getMainEnergyStorage(dir), this::isEnabled);
        });
    }
    
    private FluidApi.FluidStorage getFluidStorageDelegated(Direction direction) {
        return delegatedFluid.computeIfAbsent(direction, dir -> {
            if (getMainFluidStorage(dir) == null) return null;
            return new DelegatingFluidStorage(() -> getMainFluidStorage(dir), this::isEnabled);
        });
    }
    
    private ItemApi.InventoryStorage getItemStorageDelegated(Direction direction) {
        return delegatedItem.computeIfAbsent(direction, dir -> {
            if (getMainItemStorage(dir) == null) return null;
            return new DelegatingInventoryStorage(() -> getMainItemStorage(dir), this::isEnabled);
        });
    }
    
    public void resetCaches() {
        delegatedItem.clear();
        delegatedFluid.clear();
        delegatedEnergy.clear();
    }
    
    public boolean isEnabled() {
        return this.getCachedState().get(MachineCoreBlock.USED);
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return getEnergyStorageDelegated(direction);
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return getItemStorageDelegated(direction);
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(Direction direction) {
        return getFluidStorageDelegated(direction);
    }
}
