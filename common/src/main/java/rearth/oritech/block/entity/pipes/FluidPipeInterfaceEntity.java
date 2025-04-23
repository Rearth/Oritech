package rearth.oritech.block.entity.pipes;

import com.google.common.collect.Streams;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeConnectionBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.api.fluid.FluidApi;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FluidPipeInterfaceEntity extends ExtractablePipeInterfaceEntity {
    
    public static final int MAX_TRANSFER_RATE = (int) (FluidStackHooks.bucketAmount() * Oritech.CONFIG.fluidPipeExtractAmountBuckets());
    private static final int TRANSFER_PERIOD = Oritech.CONFIG.fluidPipeExtractIntervalDuration();
    
    private List<FluidApi.FluidStorage> filteredFluidTargetsCached;
    
    public FluidPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FLUID_PIPE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        var block = (ExtractablePipeConnectionBlock) state.getBlock();
        if (world.isClient || !block.isExtractable(state)) return;
        
        // boosted pipe works every tick, otherwise only every N tick
        if (world.getTime() % TRANSFER_PERIOD != 0 && !isBoostAvailable())
            return;
        
        var data = FluidPipeBlock.FLUID_PIPE_DATA.getOrDefault(world.getRegistryKey().getValue(), new PipeNetworkData());
        
        // try to get fluid to transfer
        // one transaction for each side
        var stackToMove = FluidStack.empty();
        FluidApi.FluidStorage takenFrom = null;
        var sources = data.machineInterfaces.getOrDefault(pos, new HashSet<>());
        
        for (var sourcePos : sources) {
            var offset = pos.subtract(sourcePos);
            var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());
            if (!block.isSideExtractable(state, direction.getOpposite())) continue;
            var sourceContainer = FluidApi.BLOCK.find(world, sourcePos, direction);
            if (sourceContainer == null || !sourceContainer.supportsExtraction()) continue;
            
            var contents = sourceContainer.getContent();
            var extractionCandidate = Streams.stream(contents)
                                        .filter(candidate -> !candidate.isEmpty())
                                        .filter(candidate -> sourceContainer.extract(candidate, true) > 0)
                                        .findFirst();
            
            if (extractionCandidate.isPresent()) {
                var extractionTest = extractionCandidate.get().copyWithAmount(MAX_TRANSFER_RATE);
                var movedAmount = sourceContainer.extract(extractionTest, true);
                stackToMove = extractionTest;
                stackToMove.setAmount(movedAmount);
                takenFrom = sourceContainer;
                break;
            }
        }
        
        // if one (or more) of connected blocks has fluid available (of first found type, only transfer one type per tick)
        // gather all connection targets supporting insertion
        // shuffle em
        // insert until no more fluid to output is available
        if (stackToMove.isEmpty() || takenFrom == null) return;
        
        var targets = findNetworkTargets(pos, data);
        
        if (targets == null) {
            System.err.println("Yeah your pipe network likely is too long. At: " + this.getPos());
            return;
        }
        
        var netHash = targets.hashCode();
        
        if (netHash != filteredTargetsNetHash || filteredFluidTargetsCached == null) {
            filteredFluidTargetsCached = targets.stream()
                                           .filter(target -> {
                                               var direction = target.getRight();
                                               var pipePos = target.getLeft().add(direction.getVector());
                                               var pipeState = world.getBlockState(pipePos);
                                               if (!(pipeState.getBlock() instanceof FluidPipeConnectionBlock fluidBlock))
                                                   return true;   // edge case, this should never happen
                                               var extracting = fluidBlock.isSideExtractable(pipeState, target.getRight().getOpposite());
                                               return !extracting;
                                           })
                                           .map(target -> FluidApi.BLOCK.find(world, target.getLeft(), target.getRight()))
                                           .filter(obj -> Objects.nonNull(obj) && obj.supportsInsertion())
                                           .collect(Collectors.toList());
            
            filteredTargetsNetHash = netHash;
        }
        
        Collections.shuffle(filteredFluidTargetsCached);
        
        var availableFluid = stackToMove.getAmount();
        
        for (var targetStorage : filteredFluidTargetsCached) {
            var transferred = targetStorage.insert(stackToMove, false);
            stackToMove.shrink(transferred);
            targetStorage.update();
            
            if (stackToMove.getAmount() <= 0) break;
        }
        
        var moved = availableFluid - stackToMove.getAmount();
        if (moved > 0) {
            stackToMove.setAmount(moved);
            takenFrom.extract(stackToMove, false);
            onBoostUsed();
            takenFrom.update();
        }
        
    }
}
