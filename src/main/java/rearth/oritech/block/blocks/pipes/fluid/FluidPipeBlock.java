package rearth.oritech.block.blocks.pipes.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import javax.annotation.Nullable;
import java.util.HashMap;

public class FluidPipeBlock extends GenericPipeBlock {

    public static HashMap<Identifier, GenericPipeInterfaceEntity.PipeNetworkData> FLUID_PIPE_DATA = new HashMap<>();

    public FluidPipeBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((level, pos, direction) -> level.getCapability(Capabilities.Fluid.BLOCK, pos, direction) != null);
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

    // to connect when a neighboring block emits a block update (e.g. the centrifuge getting a fluid addon)
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        // todo:
        // level.setBlock(pos, updateShape(state, level, Direction.getNearest(Vec3.atLowerCornerOf(pos.subtract(pos))), level.getBlockState(pos), level, pos, pos), Block.UPDATE_CLIENTS, 0);
    }

    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof FluidPipeBlock || block instanceof FluidPipeConnectionBlock || block instanceof FluidPipeDuctBlock;
    }

    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return FLUID_PIPE_DATA.computeIfAbsent(level.dimension().identifier(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }

    public static class FramedFluidPipeBlock extends FluidPipeBlock {

        public FramedFluidPipeBlock(Properties settings) {
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
