package rearth.oritech.block.blocks.pipes.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.entity.pipes.FluidPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock.FLUID_PIPE_DATA;

public class FluidPipeConnectionBlock extends ExtractablePipeConnectionBlock {
    
    public FluidPipeConnectionBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public TriFunction<World, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((world, pos, direction) -> FluidStorage.SIDED.find(world, pos, direction) != null);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeInterfaceEntity(pos, state);
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.FLUID_PIPE_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.FLUID_PIPE.getDefaultState();
    }
    
    @Override
    public String getPipeTypeName() {
        return "fluid";
    }

    // to disconnect when a neighboring block emits a block update (e.g. the centrifuge losing a fluid addon)
    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);

        world.setBlockState(pos, getStateForNeighborUpdate(state, Direction.getFacing(Vec3d.of(sourcePos.subtract(pos))), world.getBlockState(sourcePos), world, pos, sourcePos), Block.NOTIFY_LISTENERS, 0);
    }
    
    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof FluidPipeBlock || block instanceof FluidPipeConnectionBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return FLUID_PIPE_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
}
