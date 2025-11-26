package rearth.oritech.block.blocks.pipes.energy;

import rearth.oritech.block.blocks.pipes.GenericPipeDuctBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.energy.EnergyPipeBlock.ENERGY_PIPE_DATA;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EnergyPipeDuctBlock extends GenericPipeDuctBlock {
	public EnergyPipeDuctBlock(Properties settings) {
		super(settings);
	}

	@Override
	public BlockState getConnectionBlock() {
		return getNormalBlock();
	}

	@Override
	public BlockState getNormalBlock() {
		return BlockContent.ENERGY_PIPE_DUCT_BLOCK.defaultBlockState();
	}

	@Override
	public String getPipeTypeName() {
		return "energy";
	}

	@Override
	public boolean connectToOwnBlockType(Block block) {
		return block instanceof EnergyPipeDuctBlock || block instanceof EnergyPipeBlock ||block instanceof EnergyPipeConnectionBlock;
	}

	@Override
	public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level world) {
		return ENERGY_PIPE_DATA.computeIfAbsent(world.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
	}
}
