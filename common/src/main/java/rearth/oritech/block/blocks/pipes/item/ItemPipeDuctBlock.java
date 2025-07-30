package rearth.oritech.block.blocks.pipes.item;

import rearth.oritech.block.blocks.pipes.GenericPipeDuctBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.item.ItemPipeBlock.ITEM_PIPE_DATA;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ItemPipeDuctBlock extends GenericPipeDuctBlock {
	public ItemPipeDuctBlock(Properties settings) {
		super(settings);
	}

	@Override
	public BlockState getConnectionBlock() {
		return getNormalBlock();
	}

	@Override
	public BlockState getNormalBlock() {
		return BlockContent.ITEM_PIPE_DUCT_BLOCK.defaultBlockState();
	}

	@Override
	public String getPipeTypeName() {
		return "item";
	}

	@Override
	public boolean connectToOwnBlockType(Block block) {
		return block instanceof ItemPipeDuctBlock || block instanceof ItemPipeBlock || block instanceof ItemPipeConnectionBlock;
	}

	@Override
	public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level world) {
		return ITEM_PIPE_DATA.computeIfAbsent(world.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
	}
}
