package rearth.oritech.block.blocks.pipes.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.entity.pipes.FluidPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock.FLUID_PIPE_DATA;

public class FluidPipeConnectionBlock extends ExtractablePipeConnectionBlock {

    public FluidPipeConnectionBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((level, pos, direction) -> level.getCapability(Capabilities.Fluid.BLOCK, pos, direction) != null);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeInterfaceEntity(pos, state);
    }

    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.FLUID_PIPE_CONNECTION.get().defaultBlockState();
    }

    @Override
    public BlockState getNormalBlock() {
        return BlockContent.FLUID_PIPE.get().defaultBlockState();
    }

    @Override
    public SavedDataType<GenericPipeInterfaceEntity.PipeNetworkData> getNetworkDataType() {
        return GenericPipeInterfaceEntity.PipeNetworkData.FLUID_TYPE;
    }

    // to disconnect when a neighboring block emits a block update (e.g. the centrifuge losing a fluid addon)
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        // todo
        // level.setBlock(pos, updateShape(state, Direction.getNearest(Vec3.atLowerCornerOf(sourcePos.subtract(pos))), level.getBlockState(sourcePos), level, pos, sourcePos), Block.UPDATE_CLIENTS, 0);
    }

    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof FluidPipeBlock || block instanceof FluidPipeConnectionBlock || block instanceof FluidPipeDuctBlock;
    }

    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return FLUID_PIPE_DATA.computeIfAbsent(level.dimension().identifier(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }

    public static class FramedFluidPipeConnectionBlock extends FluidPipeConnectionBlock {

        public FramedFluidPipeConnectionBlock(Properties settings) {
            super(settings);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.block();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getShape(level, pos);
        }

        @Override
        public BlockState getNormalBlock() {
            return BlockContent.FRAMED_FLUID_PIPE.get().defaultBlockState();
        }

        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get().defaultBlockState();
        }
    }
}
