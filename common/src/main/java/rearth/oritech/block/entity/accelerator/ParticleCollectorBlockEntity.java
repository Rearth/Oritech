package rearth.oritech.block.entity.accelerator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.init.BlockEntitiesContent;

import static rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity.getOutputPosition;

public class ParticleCollectorBlockEntity extends BlockEntity implements BlockEntityTicker<ParticleCollectorBlockEntity>, EnergyApi.BlockProvider {
    
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(OritechConfig.collectorEnergyStorage.get(), 0, OritechConfig.collectorEnergyStorage.get(), this::setChanged);
    
    public ParticleCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PARTICLE_COLLECTOR_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return energyStorage;
    }
    
    public void onParticleCollided() {
        onParticleCollided(OritechConfig.blackHoleTachyonEnergy.get());
    }
    
    public void onParticleCollided(int amount) {
        energyStorage.amount = Math.min(energyStorage.capacity, energyStorage.amount + amount);
        energyStorage.update();
        triggerAnimation();
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, ParticleCollectorBlockEntity blockEntity) {
        if (world.isClientSide) return;
        
        
        if (energyStorage.amount <= 0) return;
        
        // output energy to back
        var target = getOutputPosition(pos, getBlockState().getValue(DirectionalBlock.FACING).getOpposite());
        var candidate = EnergyApi.BLOCK.find(world, target.getB(), target.getA());
        if (candidate != null) {
            EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        energyStorage.setAmount(nbt.getLong("energy"));
    }
    
    public void triggerAnimation() {
        if (level instanceof ServerLevel serverLevel) {
            var forward = getBlockState().getValue(DirectionalBlock.FACING).getNormal();
            var at = worldPosition.getCenter().add(Vec3.atCenterOf(forward));
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 2, level.random.nextFloat(), level.random.nextFloat(), level.random.nextFloat(), 0.15f);
        }
    }
}
