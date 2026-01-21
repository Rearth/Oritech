package rearth.oritech.block.base.entity;

import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class PassiveGeneratorBlockEntity extends BlockEntity implements EnergyApi.BlockProvider, BlockEntityTicker<PassiveGeneratorBlockEntity> {
    
    protected final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(0, 5_000, 200_000, this::setChanged);
    
    public PassiveGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, PassiveGeneratorBlockEntity blockEntity) {
        if (world.isClientSide) return;
        
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
        
        for (var target : getOutputTargets(worldPosition, level)) {
            var candidate = EnergyApi.BLOCK.find(level, target.getA(), target.getB());
            if (candidate != null)
                EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
    }
    
    public abstract int getProductionRate();
    
    public abstract boolean isProducing();
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        energyStorage.setAmount(nbt.getLong("energy"));
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    protected abstract Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level world);
}
