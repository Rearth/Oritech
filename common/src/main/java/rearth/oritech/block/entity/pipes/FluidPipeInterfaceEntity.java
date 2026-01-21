package rearth.oritech.block.entity.pipes;

import com.google.common.collect.Streams;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.fluid.FluidPipeConnectionBlock;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FluidPipeInterfaceEntity extends ExtractablePipeInterfaceEntity {
    
    public static final int MAX_TRANSFER_RATE = (int) (FluidStackHooks.bucketAmount() * Oritech.CONFIG.fluidPipeExtractAmountBuckets());
    private static final int TRANSFER_PERIOD = Oritech.CONFIG.fluidPipeExtractIntervalDuration();
    
    private List<FluidApi.FluidStorage> filteredFluidTargetsCached;
    
    public FluidPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FLUID_PIPE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        var block = (ExtractablePipeConnectionBlock) state.getBlock();
        if (world.isClientSide || !block.isExtractable(state)) return;
        
        var boosted = isBoostAvailable();
        
        // boosted pipe works every tick, otherwise only every N tick
        if (world.getGameTime() % TRANSFER_PERIOD != 0 && !boosted)
            return;
        
        var data = FluidPipeBlock.FLUID_PIPE_DATA.getOrDefault(world.dimension().location(), new PipeNetworkData());
        var transferAmount = boosted ? MAX_TRANSFER_RATE * 100 : MAX_TRANSFER_RATE;
        
        // try to get fluid to transfer
        // one transaction for each side
        var stackToMove = FluidStack.empty();
        FluidApi.FluidStorage takenFrom = null;
        var sources = data.machineInterfaces.getOrDefault(pos, new HashSet<>());
        
        for (var sourcePos : sources) {
            var offset = pos.subtract(sourcePos);
            var direction = Direction.fromDelta(offset.getX(), offset.getY(), offset.getZ());
            if (!block.isSideExtractable(state, direction.getOpposite())) continue;
            
            var sourceBlock = world.getBlockState(sourcePos);
            
            if (sourceBlock.is(BlockTags.CAULDRONS))
                transferAmount = (int) FluidStackHooks.bucketAmount();
            
            var sourceContainer = FluidApi.BLOCK.find(world, sourcePos, sourceBlock, null, direction);
            if (sourceContainer == null || !sourceContainer.supportsExtraction()) continue;
            
            var contents = sourceContainer.getContent();
            var extractionCandidate = Streams.stream(contents)
                                        .filter(candidate -> !candidate.isEmpty())
                                        .filter(candidate -> sourceContainer.extract(candidate, true) > 0)
                                        .findFirst();
            
            if (extractionCandidate.isPresent()) {
                var extractionTest = extractionCandidate.get().copyWithAmount(transferAmount);
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
            System.err.println("Yeah your pipe network likely is too long. At: " + this.getBlockPos());
            return;
        }
        
        var netHash = targets.hashCode();
        
        if (netHash != filteredTargetsNetHash || filteredFluidTargetsCached == null) {
            filteredFluidTargetsCached = targets.stream()
                                           .filter(target -> {
                                               var direction = target.getB();
                                               var pipePos = target.getA().offset(direction.getNormal());
                                               var pipeState = world.getBlockState(pipePos);
                                               if (!(pipeState.getBlock() instanceof FluidPipeConnectionBlock fluidBlock))
                                                   return true;   // edge case, this should never happen
                                               var extracting = fluidBlock.isSideExtractable(pipeState, target.getB().getOpposite());
                                               return !extracting;
                                           })
                                           .map(target -> FluidApi.BLOCK.find(world, target.getA(), target.getB()))
                                           .filter(obj -> Objects.nonNull(obj) && obj.supportsInsertion())
                                           .collect(Collectors.toList());
            
            filteredTargetsNetHash = netHash;
        }
        
        Collections.shuffle(filteredFluidTargetsCached);
        
        var availableFluid = stackToMove.getAmount();
        
        for (var targetStorage : filteredFluidTargetsCached) {
            
            var maxInsert = targetStorage.insert(stackToMove, true);
            var taken = takenFrom.extract(stackToMove.copyWithAmount(maxInsert), false);
            var inserted = targetStorage.insert(stackToMove.copyWithAmount(taken), false);
            
            stackToMove.shrink(inserted);
            targetStorage.update();
            
            if (stackToMove.getAmount() <= 0) break;
        }
        
        var moved = availableFluid - stackToMove.getAmount();
        if (moved > 0) {
            stackToMove.setAmount(moved);
            onBoostUsed();
            takenFrom.update();
        }
        
    }
}
