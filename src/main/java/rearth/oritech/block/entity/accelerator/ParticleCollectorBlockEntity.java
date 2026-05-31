package rearth.oritech.block.entity.accelerator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;

import static rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity.getOutputPosition;

public class ParticleCollectorBlockEntity extends BlockEntity implements BlockEntityTicker<ParticleCollectorBlockEntity>, EnergyProvider {

    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(OritechConfig.collectorEnergyStorage.get(), 0, OritechConfig.collectorEnergyStorage.get(), 0, this::setChanged, false);

    public ParticleCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PARTICLE_COLLECTOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    public void onParticleCollided() {
        onParticleCollided(OritechConfig.blackHoleTachyonEnergy.get());
    }

    public void onParticleCollided(int amount) {

        try (var transaction = Transaction.openRoot()) {
            energyStorage.internalInsert(amount, transaction);
            transaction.commit();
        }

        triggerAnimation();
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, ParticleCollectorBlockEntity blockEntity) {
        if (level.isClientSide()) return;

        if (energyStorage.energy <= 0) return;

        // output energy to back
        // todo caching?
        var target = getOutputPosition(pos, getBlockState().getValue(DirectionalBlock.FACING).getOpposite());
        var candidate = level.getCapability(Capabilities.Energy.BLOCK, target.getB(), target.getA());
        if (candidate != null) {
            try (var transaction = Transaction.openRoot()) {
                var inserted = candidate.insert((int) Math.min(Integer.MAX_VALUE, energyStorage.energy), transaction);
                if (inserted > 0) {
                    energyStorage.internalExtract(inserted, transaction);
                    transaction.commit();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energyStorage.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.deserialize(input);
    }

    public void triggerAnimation() {
        if (level instanceof ServerLevel serverLevel) {
            var forward = getBlockState().getValue(DirectionalBlock.FACING).getUnitVec3i();
            var at = worldPosition.getCenter().add(Vec3.atCenterOf(forward));
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 2, level.getRandom().nextFloat(), level.getRandom().nextFloat(), level.getRandom().nextFloat(), 0.15f);
        }
    }
}
