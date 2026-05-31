package rearth.oritech.block.blocks.pipes.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import java.util.HashMap;

public class FluidPipeBlock extends GenericPipeBlock {

    public static HashMap<Identifier, GenericPipeInterfaceEntity.PipeNetworkData> FLUID_PIPE_DATA = new HashMap<>();

    public FluidPipeBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((level, pos, direction) -> FluidApi.BLOCK.find(level, pos, direction) != null);
    }

    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.FLUID_PIPE_CONNECTION.defaultBlockState();
    }

    @Override
    public BlockState getNormalBlock() {
        return BlockContent.FLUID_PIPE.defaultBlockState();
    }

    @Override
    public String getPipeTypeName() {
        return "fluid";
    }

    // to connect when a neighboring block emits a block update (e.g. the centrifuge getting a fluid addon)
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, level, pos, sourceBlock, sourcePos, notify);

        level.setBlock(pos, updateShape(state, Direction.getNearest(Vec3.atLowerCornerOf(sourcePos.subtract(pos))), level.getBlockState(sourcePos), level, pos, sourcePos), Block.UPDATE_CLIENTS, 0);
    }

    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof FluidPipeBlock || block instanceof FluidPipeConnectionBlock || block instanceof FluidPipeDuctBlock;
    }

    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return FLUID_PIPE_DATA.computeIfAbsent(level.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
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
            return BlockContent.FRAMED_FLUID_PIPE.defaultBlockState();
        }

        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.FRAMED_FLUID_PIPE_CONNECTION.defaultBlockState();
        }
    }
}
