package rearth.oritech.block.entity.machines.accelerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.SimpleEnergyStorage;

public class AcceleratorMotorBlockEntity extends BlockEntity implements EnergyApi.BlockProvider {
    
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(5_000_000, 5_000_000, 5_000_000, this::markDirty);
    
    public AcceleratorMotorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_MOTOR_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public EnergyApi.EnergyContainer getStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        energyStorage.setAmount(nbt.getLong("energy"));
    }
}
