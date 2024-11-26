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
import rearth.oritech.util.EnergyApi;
import rearth.oritech.util.SimpleEnergyStorage;

import java.util.Set;

public abstract class PassiveGeneratorBlockEntity extends BlockEntity implements EnergyApi.BlockEnergyApi.EnergyProvider, BlockEntityTicker<PassiveGeneratorBlockEntity> {
    
    protected final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(200_000, 0, 5_000, this::markDirty);
    
    public PassiveGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, PassiveGeneratorBlockEntity blockEntity) {
        if (world.isClient || !isProducing()) return;
        
        var producedAmount = getProductionRate();
        if (energyStorage.insertIgnoringLimit(producedAmount, false) > 0) {
            energyStorage.update();
        }
        
        outputEnergy();
        
    }
    
    private void outputEnergy() {
        if (energyStorage.getAmount() <= 0) return;
        
        // todo caching for targets? Used to be BlockApiCache.create()
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
    public EnergyApi.EnergyContainer getStorage(Direction direction) {
        return energyStorage;
    }
    
    protected abstract Set<Pair<BlockPos, Direction>> getOutputTargets(BlockPos pos, World world);
}
