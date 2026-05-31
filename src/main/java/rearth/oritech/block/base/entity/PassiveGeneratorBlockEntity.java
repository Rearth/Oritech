package rearth.oritech.block.base.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;

import java.util.List;
import java.util.Set;

public abstract class PassiveGeneratorBlockEntity extends BlockEntity implements EnergyProvider, BlockEntityTicker<PassiveGeneratorBlockEntity> {

    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(200_000, 0, 10_000, 0, this::setChanged, false);
    private List<BlockCapabilityCache<EnergyHandler, Direction>> cachedOutputTargets = List.of();

    public PassiveGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, PassiveGeneratorBlockEntity blockEntity) {

        if (level.isClientSide()) return;

        try (var transaction = Transaction.openRoot()) {

            if (isProducing()) {
                var producedAmount = getProductionRate();
                energyStorage.internalInsert(producedAmount, transaction);
            }

            outputEnergy(transaction);

            transaction.commit();
        }

    }

    private void outputEnergy(Transaction transaction) {
        if (energyStorage.getAmountAsLong() <= 0 || !(level instanceof ServerLevel serverLevel)) return;

        if (cachedOutputTargets.isEmpty()) {
            cachedOutputTargets = getOutputTargets(worldPosition, level).stream()
                    .map(target -> BlockCapabilityCache.create(Capabilities.Energy.BLOCK, serverLevel, target.getA(), target.getB()))
                    .toList();
        }

        var available = energyStorage.getAmountAsLong();

        for (var target : cachedOutputTargets) {
            var candidate = target.getCapability();
            if (candidate != null)
                available -= candidate.insert((int) available, transaction);

            if (available <= 0) break;
        }
    }

    public abstract int getProductionRate();

    public abstract boolean isProducing();

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

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    protected abstract Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level);
}
