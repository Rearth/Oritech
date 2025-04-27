package rearth.oritech.block.entity.accelerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.init.BlockEntitiesContent;

public class AcceleratorMotorBlockEntity extends BlockEntity implements EnergyApi.BlockProvider {
    
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(Oritech.CONFIG.acceleratorMotorRFCapacity(), Oritech.CONFIG.acceleratorMotorRFCapacity(), Oritech.CONFIG.acceleratorMotorRFCapacity(), this::markDirty);
    
    public AcceleratorMotorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_MOTOR_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
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
