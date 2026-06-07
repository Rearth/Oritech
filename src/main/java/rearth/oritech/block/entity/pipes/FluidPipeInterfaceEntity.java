package rearth.oritech.block.entity.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeConnectionBlock;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FluidPipeInterfaceEntity extends ExtractablePipeInterfaceEntity {

    public static final int MAX_TRANSFER_RATE = (int) (FluidType.BUCKET_VOLUME * OritechConfig.fluidPipeExtractAmountBuckets.get());
    private static final int TRANSFER_PERIOD = OritechConfig.fluidPipeExtractIntervalDuration.get();

    private List<BlockCapabilityCache<ResourceHandler<FluidResource>, Direction>> filteredFluidTargetsCached;

    public FluidPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FLUID_PIPE_ENTITY.get(), pos, state);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        var block = (ExtractablePipeConnectionBlock) state.getBlock();
        if (level.isClientSide() || !block.isExtractable(state)) return;

        var boosted = isBoostAvailable();

        // boosted pipe works every tick, otherwise only every N tick
        if (level.getGameTime() % TRANSFER_PERIOD != 0 && !boosted)
            return;

        var data = FluidPipeBlock.FLUID_PIPE_DATA.get(level.dimension().identifier());
        if (data == null) return;   // this should never happen

        var targets = findNetworkTargets(pos, data);

        if (targets == null) {
            System.err.println("Yeah your pipe network likely is too long (or something else errored. At: " + this.getBlockPos());
            return;
        }

        refreshTargetCaches(level, targets);

        Collections.shuffle(filteredFluidTargetsCached);

        // do the whole thing for each direction (neighboring fluid container) fluid is taken from (usually just 1, but could be multiple)
        // tries to extract from each side (for each slot on that source machine) and then tries to insert it to any matching containers
        for (var machineDirection : data.getMachineDirections(pos)) {
            if (!block.isSideExtracting(state, machineDirection)) continue;

            var sourcePos = pos.relative(machineDirection);
            var accessDirection = machineDirection.getOpposite();

            var sourceBlock = level.getBlockState(sourcePos);

            var transferAmount = boosted ? MAX_TRANSFER_RATE * 100 : MAX_TRANSFER_RATE;
            if (sourceBlock.is(BlockTags.CAULDRONS))
                transferAmount = FluidType.BUCKET_VOLUME;

            var sourceContainer = level.getCapability(Capabilities.Fluid.BLOCK, sourcePos, sourceBlock, null, accessDirection);
            if (sourceContainer == null) continue;

            // one transaction per machine that is being extracted from
            try (var transaction = Transaction.openRoot()) {
                var moved = 0;
                // do the whole thing for each slot in the source container
                for (int i = 0; i < sourceContainer.size(); i++) {
                    var extractedResource = sourceContainer.getResource(i);
                    if (extractedResource.isEmpty()) continue;

                    // with directly canceled transaction just to figure out how much can be moved / extracted
                    var availableAmount = 0;
                    try (var simulation = Transaction.open(transaction)) {
                        availableAmount = sourceContainer.extract(i, extractedResource, transferAmount, simulation);
                        if (availableAmount <= 0) continue;
                    }


                    // go through all targets and try to insert
                    for (var cachedTarget : filteredFluidTargetsCached) {
                        var targetContainer = cachedTarget.getCapability();
                        if (targetContainer == null) continue;

                        var inserted = targetContainer.insert(extractedResource, availableAmount, transaction);
                        var taken = sourceContainer.extract(i, extractedResource, inserted, transaction);

                        if (taken != inserted) {
                            Oritech.LOGGER.warn("Fluid Pipe Insertion Error! Handler Misbehaving. At: {}, inserted: {}, extracted: {},  amount: {}. From pipe at: {}",
                                    cachedTarget.pos(), inserted, taken, availableAmount, worldPosition);
                            return;  // this should never happen
                        }

                        availableAmount -= inserted;
                        moved += inserted;

                        if (availableAmount <= 0) break;

                    }
                }
                if (moved > 0) {
                    transaction.commit();
                    onBoostUsed();
                }

            }
        }
    }

    private void refreshTargetCaches(Level level, Set<GenericPipeInterfaceEntity.PipeNetworkTarget> targets) {

        var netHash = targets.hashCode();
        if (netHash == filteredTargetsNetHash && filteredFluidTargetsCached != null) {
            return;
        }

        filteredFluidTargetsCached = targets.stream()
                .filter(target -> {
                    var pipeState = level.getBlockState(target.getPipePos());
                    if (!(pipeState.getBlock() instanceof FluidPipeConnectionBlock fluidBlock))
                        return true;

                    // A machine connected through an extracting pipe interface should act as a source only,
                    // otherwise fluid can get pushed back into a block that the network is configured to drain.
                    var extracting = fluidBlock.isSideExtracting(pipeState, target.getPipeFacing());
                    return !extracting;
                })
                .map(target -> BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, (ServerLevel) level, target.machinePos(), target.insertedFrom()))
                .collect(Collectors.toList());

        filteredTargetsNetHash = netHash;
    }
}
