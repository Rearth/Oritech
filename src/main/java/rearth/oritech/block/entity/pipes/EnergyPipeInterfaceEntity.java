package rearth.oritech.block.entity.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock;
import rearth.oritech.block.blocks.pipes.energy.SuperConductorBlock;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.ArrayList;
import java.util.List;

public class EnergyPipeInterfaceEntity extends GenericPipeInterfaceEntity implements EnergyProvider {

    private final SimpleEnergyHandler energyStorage;
    private final boolean isSuperConductor;

    private final List<BlockCapabilityCache<EnergyHandler, Direction>> cachedTargets = new ArrayList<>();
    private int nextTargetIndex;

    public EnergyPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_PIPE.get(), pos, state);

        isSuperConductor = state.getBlock().equals(BlockContent.SUPERCONDUCTOR_CONNECTION.get()) || state.getBlock().equals(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get());

        if (isSuperConductor) {
            energyStorage = new SimpleEnergyHandler(Math.toIntExact(OritechConfig.superConductorTransferRate.get()));
        } else {
            energyStorage = new SimpleEnergyHandler(Math.toIntExact(OritechConfig.energyPipeTransferRate.get()));
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

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        // if energy is available
        // gather all connection targets supporting insertion
        // rotate starting target each transfer attempt
        // insert until no more energy is available

        if (level.isClientSide()) return;
        if (energyStorage.getAmountAsInt() <= 0) {
            transferBackoff.reset();
            return;
        }
        if (!transferBackoff.shouldAttempt(level.getGameTime(), 1)) return;

        var dataSource = isSuperConductor ? SuperConductorBlock.SUPERCONDUCTOR_DATA : EnergyPipeBlock.ENERGY_PIPE_DATA;

        var data = dataSource.get(level.dimension().identifier());
        if (data == null) return;   // this should also never happen
        var targets = findNetworkTargets(pos, data);    // list of connected machine positions

        if (targets != cachedNetworkTargets) {
            cachedTargets.clear();
            for (var target : targets) {
                cachedTargets.add(BlockCapabilityCache.create(Capabilities.Energy.BLOCK, (ServerLevel) level, target.machinePos(), target.insertedFrom()));
            }
            cachedNetworkTargets = targets;
            nextTargetIndex = cachedTargets.isEmpty() ? 0 : Math.floorMod(worldPosition.hashCode(), cachedTargets.size());
        }

        if (this.cachedTargets.isEmpty()) {
            transferBackoff.recordAttempt(level.getGameTime(), 1, false);
            return;
        }

        var targetCount = this.cachedTargets.size();
        var startTargetIndex = nextTargetIndex;
        // If nobody accepts energy, still rotate the starting target for the next attempt.
        nextTargetIndex = (nextTargetIndex + 1) % targetCount;
        var lastSuccessfulTargetIndex = -1;
        var totalMoved = 0;
        try (var transaction = Transaction.openRoot()) {

            for (int i = 0; i < targetCount; i++) {
                if (energyStorage.getAmountAsLong() <= 0) break;

                var targetIndex = (startTargetIndex + i) % targetCount;
                var cachedTarget = this.cachedTargets.get(targetIndex);

                var targetStorage = cachedTarget.getCapability();
                if (targetStorage == null) continue;

                var prev = energyStorage.getAmountAsLong();
                var inserted = targetStorage.insert(energyStorage.getAmountAsInt(), transaction);
                var extracted = energyStorage.extract(inserted, transaction);

                totalMoved += extracted;

                if (inserted != extracted) {
                    Oritech.LOGGER.warn("Energy Pipe Insertion Error! Handler Misbehaving. At: {}, inserted: {}, extracted: {},  amount: {}. From pipe at: {}",
                            cachedTarget.pos(), inserted, extracted, prev, worldPosition);
                    transferBackoff.recordAttempt(level.getGameTime(), 1, false);
                    return;  // this should never happen
                }

                if (extracted > 0) lastSuccessfulTargetIndex = targetIndex;

            }

            if (totalMoved > 0) {
                transaction.commit();
                // Rejected targets must not give the following receiver an extra turn.
                nextTargetIndex = (lastSuccessfulTargetIndex + 1) % targetCount;
            }
        }

        transferBackoff.recordAttempt(level.getGameTime(), 1, totalMoved > 0);
    }

    @Override
    public void setChanged() {
        if (this.level != null) level.blockEntityChanged(worldPosition);
    }
}
