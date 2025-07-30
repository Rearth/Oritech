package rearth.oritech.block.blocks.pipes.fluid;

import rearth.oritech.block.blocks.pipes.GenericPipeDuctBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.fluid.FluidPipeBlock.FLUID_PIPE_DATA;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class FluidPipeDuctBlock extends GenericPipeDuctBlock {
	public FluidPipeDuctBlock(Properties settings) {
		super(settings);
	}

	@Override
	public BlockState getConnectionBlock() {
		return getNormalBlock();
	}

	@Override
	public BlockState getNormalBlock() {
		return BlockContent.FLUID_PIPE_DUCT_BLOCK.defaultBlockState();
	}

	@Override
	public String getPipeTypeName() {
		return "fluid";
	}

	@Override
	public boolean connectToOwnBlockType(Block block) {
		return block instanceof FluidPipeDuctBlock || block instanceof FluidPipeBlock || block instanceof FluidPipeConnectionBlock;
	}

	@Override
	public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level world) {
		return FLUID_PIPE_DATA.computeIfAbsent(world.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
	}
}
