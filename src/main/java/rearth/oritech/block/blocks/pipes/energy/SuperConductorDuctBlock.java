package rearth.oritech.block.blocks.pipes.energy;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.pipes.GenericPipeDuctBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

public class SuperConductorDuctBlock extends GenericPipeDuctBlock {
	public SuperConductorDuctBlock(Properties settings) {
		super(settings);
	}

	@Override
	public BlockState getConnectionBlock() {
		return getNormalBlock();
	}

	@Override
	public BlockState getNormalBlock() {
		return BlockContent.SUPERCONDUCTOR_DUCT_BLOCK.defaultBlockState();
	}

	@Override
	public String getPipeTypeName() {
		return "superconductor";
	}

	@Override
	public boolean connectToOwnBlockType(Block block) {
		return block instanceof SuperConductorDuctBlock || block instanceof SuperConductorBlock || block instanceof SuperConductorConnectionBlock;
	}

	@Override
	public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level world) {
		return SuperConductorBlock.SUPERCONDUCTOR_DATA.computeIfAbsent(world.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
	}
}
