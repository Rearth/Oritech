package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;

import java.util.Set;

public abstract class PassiveGeneratorBlockEntity extends BlockEntity implements EnergyApi.BlockProvider, BlockEntityTicker<PassiveGeneratorBlockEntity> {
    
    protected final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(0, 5_000, 200_000, this::markDirty);
    
    public PassiveGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, PassiveGeneratorBlockEntity blockEntity) {
        if (world.isClient) return;
        
        if (isProducing()) {
            var producedAmount = getProductionRate();
            if (energyStorage.insertIgnoringLimit(producedAmount, false) > 0) {
                energyStorage.update();
            }
        }
        
        outputEnergy();
        
    }
    
    private void outputEnergy() {
        if (energyStorage.getAmount() <= 0) return;
        
        for (var target : getOutputTargets(pos, world)) {
            var candidate = EnergyApi.BLOCK.find(world, target.getLeft(), target.getRight());
            if (candidate != null)
                EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
    }
    
    public abstract int getProductionRate();
    
    public abstract boolean isProducing();
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        energyStorage.setAmount(nbt.getLong("energy"));
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    protected abstract Set<Pair<BlockPos, Direction>> getOutputTargets(BlockPos pos, World world);
}
