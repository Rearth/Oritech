package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

public class ItemPipeBlock extends GenericPipeBlock {
    
    public static GenericPipeInterfaceEntity.PipeNetworkData ITEM_PIPE_DATA = new GenericPipeInterfaceEntity.PipeNetworkData();
    
    public ItemPipeBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public BlockApiLookup<?, Direction> getSidesLookup() {
        return ItemStorage.SIDED;
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.ITEM_PIPE_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.ITEM_PIPE.getDefaultState();
    }
    
    @Override
    public String getPipeTypeName() {
        return "item";
    }
    
    @Override
    public boolean connectToBlockType(Block block) {
        return block instanceof ItemPipeBlock || block instanceof ItemPipeConnectionBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData() {
        return ITEM_PIPE_DATA;
    }
}
