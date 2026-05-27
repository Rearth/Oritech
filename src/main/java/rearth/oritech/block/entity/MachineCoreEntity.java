package rearth.oritech.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.energy.DelegatingEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.fluid.DelegatingFluidStorage;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.item.DelegatingInventoryStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MultiblockMachineController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MachineCoreEntity extends BlockEntity implements ItemProvider, FluidProvider, EnergyProvider {
    
    private BlockPos controllerPos = BlockPos.ZERO;
    private MultiblockMachineController controllerEntity;
    private final Map<Direction, DelegatingEnergyStorage> delegatedEnergy = new HashMap<>(6);
    private final Map<Direction, DelegatingFluidStorage> delegatedFluid = new HashMap<>(6);
    private final Map<Direction, DelegatingInventoryStorage> delegatedItem = new HashMap<>(6);
    
    public MachineCoreEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.MACHINE_CORE_ENTITY.get(), pos, state);
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("controller", BlockPos.CODEC, controllerPos);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        controllerPos = input.read("controller", BlockPos.CODEC)
                          .orElse(new BlockPos(input.getIntOr("controller_x", 0), input.getIntOr("controller_y", 0), input.getIntOr("controller_z", 0)));
    }
    
    public BlockPos getControllerPos() {
        return controllerPos;
    }
    
    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        this.controllerEntity = null;    // forces cache reload
        this.setChanged();
    }
    
    @Nullable
    public MultiblockMachineController getCachedController() {
        if (level == null || !this.getBlockState().getValue(MachineCoreBlock.USED)) return null;
        
        if (controllerEntity == null || ((BlockEntity) controllerEntity).isRemoved()) {
            var candidate = Objects.requireNonNull(level).getBlockEntity(getControllerPos());
            if (candidate instanceof MultiblockMachineController controller) {
                controllerEntity = controller;
            } else {
                controllerEntity = null;
            }
        }
        
        return controllerEntity;
    }
    
    public void resetCaches() {
        delegatedItem.clear();
        delegatedFluid.clear();
        delegatedEnergy.clear();
    }
    
    public boolean isEnabled() {
        return this.getBlockState().getValue(MachineCoreBlock.USED);
    }
    
    @Nullable
    private EnergyHandler getMainEnergyStorage(Direction direction) {
        
        var isUsed = this.getBlockState().getValue(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (controllerEntity == null) return null;
        return controllerEntity.getEnergyStorageForMultiblock(direction);
    }
    
    private ResourceHandler<FluidResource> getMainFluidStorage(Direction direction) {
        
        var isUsed = this.getBlockState().getValue(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (!(controllerEntity instanceof FluidProvider fluidProvider)) return null;
        return fluidProvider.getFluidLookup(direction);
    }
    
    private ResourceHandler<ItemResource> getMainItemStorage(Direction direction) {
        
        var isUsed = this.getBlockState().getValue(MachineCoreBlock.USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (!(controllerEntity instanceof ItemProvider itemProvider)) return null;
        return itemProvider.getItemLookup(direction);
    }
    
    @Nullable
    private EnergyHandler getEnergyStorageDelegated(Direction direction) {
        return delegatedEnergy.computeIfAbsent(direction, dir -> {
            if (getMainEnergyStorage(dir) == null) return null;
            return new DelegatingEnergyStorage(() -> getMainEnergyStorage(dir), this::isEnabled);
        });
    }
    
    private ResourceHandler<FluidResource> getFluidStorageDelegated(Direction direction) {
        return delegatedFluid.computeIfAbsent(direction, dir -> {
            if (getMainFluidStorage(dir) == null) return null;
            return new DelegatingFluidStorage(() -> getMainFluidStorage(dir), this::isEnabled);
        });
    }
    
    private ResourceHandler<ItemResource> getItemStorageDelegated(Direction direction) {
        return delegatedItem.computeIfAbsent(direction, dir -> {
            if (getMainItemStorage(dir) == null) return null;
            return new DelegatingInventoryStorage(() -> getMainItemStorage(dir), this::isEnabled);
        });
    }
    
    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return getEnergyStorageDelegated(direction);
    }
    
    @Override
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        return getFluidStorageDelegated(direction);
    }
    
    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return getItemStorageDelegated(direction);
    }
}
