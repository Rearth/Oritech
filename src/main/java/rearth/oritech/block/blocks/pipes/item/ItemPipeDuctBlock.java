package rearth.oritech.block.blocks.pipes.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;
import rearth.oritech.block.blocks.pipes.GenericPipeDuctBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.item.ItemPipeBlock.ITEM_PIPE_DATA;

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
        return BlockContent.ITEM_PIPE_DUCT.get().defaultBlockState();
    }

    @Override
    public SavedDataType<GenericPipeInterfaceEntity.PipeNetworkData> getNetworkDataType() {
        return GenericPipeInterfaceEntity.PipeNetworkData.ITEM_TYPE;
    }


    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof ItemPipeDuctBlock || block instanceof ItemPipeBlock || block instanceof ItemPipeConnectionBlock;
    }

    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return ITEM_PIPE_DATA.computeIfAbsent(level.dimension().identifier(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
}
