package rearth.oritech.block.blocks.pipes.energy;

import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.block.blocks.pipes.GenericPipeConnectionBlock;
import rearth.oritech.block.entity.pipes.EnergyPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock.ENERGY_PIPE_DATA;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EnergyPipeConnectionBlock extends GenericPipeConnectionBlock {
    
    public EnergyPipeConnectionBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((world, pos, direction) -> EnergyApi.BLOCK.find(world, pos, direction) != null);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyPipeInterfaceEntity(pos, state);
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.ENERGY_PIPE_CONNECTION.defaultBlockState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.ENERGY_PIPE.defaultBlockState();
    }

    @Override
    protected VoxelShape[] createShapes() {
        return THIN_SHAPES;
    }
    
    @Override
    public String getPipeTypeName() {
        return "energy";
    }
    
    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof EnergyPipeBlock || block instanceof EnergyPipeConnectionBlock || block instanceof EnergyPipeDuctBlock;
    }
    
    @Override
    public boolean isCompatibleTarget(Block block) {
        return !block.equals(BlockContent.SUPERCONDUCTOR_CONNECTION);
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level world) {
        return ENERGY_PIPE_DATA.computeIfAbsent(world.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }

	public static class FramedEnergyPipeConnectionBlock extends EnergyPipeConnectionBlock {

		public FramedEnergyPipeConnectionBlock(Properties settings) {
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
			return BlockContent.FRAMED_ENERGY_PIPE.defaultBlockState();
		}

		@Override
		public BlockState getConnectionBlock() {
			return BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.defaultBlockState();
		}
	}
}
