package rearth.oritech.block.blocks.pipes.fluid;

import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FluidPipeBlock extends GenericPipeBlock {
    
    public static HashMap<ResourceLocation, GenericPipeInterfaceEntity.PipeNetworkData> FLUID_PIPE_DATA = new HashMap<>();

    public FluidPipeBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((world, pos, direction) -> FluidApi.BLOCK.find(world, pos, direction) != null);
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
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);

        world.setBlock(pos, updateShape(state, Direction.getNearest(Vec3.atLowerCornerOf(sourcePos.subtract(pos))), world.getBlockState(sourcePos), world, pos, sourcePos), Block.UPDATE_CLIENTS, 0);
    }
    
    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof FluidPipeBlock || block instanceof FluidPipeConnectionBlock || block instanceof FluidPipeDuctBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level world) {
        return FLUID_PIPE_DATA.computeIfAbsent(world.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }

	public static class FramedFluidPipeBlock extends FluidPipeBlock {

		public FramedFluidPipeBlock(Properties settings) {
			super(settings);
		}

		@Override
		public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
			return Shapes.block();
		}

		@Override
		public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
			return state.getShape(world, pos);
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
