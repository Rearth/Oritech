package rearth.oritech.block.blocks.pipes.energy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import rearth.oritech.block.blocks.pipes.GenericPipeDuctBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

public class SuperConductorDuctBlock extends GenericPipeDuctBlock {
	public SuperConductorDuctBlock(Settings settings) {
		super(settings);
	}

	@Override
	public BlockState getConnectionBlock() {
		return getNormalBlock();
	}

	@Override
	public BlockState getNormalBlock() {
		return BlockContent.SUPERCONDUCTOR_DUCT_BLOCK.getDefaultState();
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
	public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
		return SuperConductorBlock.SUPERCONDUCTOR_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
	}
}
