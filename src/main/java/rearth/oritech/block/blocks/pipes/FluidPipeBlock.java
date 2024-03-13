package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

public class FluidPipeBlock extends GenericPipeBlock {
    
    public static GenericPipeInterfaceEntity.PipeNetworkData FLUID_PIPE_DATA = new GenericPipeInterfaceEntity.PipeNetworkData();
    
    public FluidPipeBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public BlockApiLookup<?, Direction> getSidesLookup() {
        return FluidStorage.SIDED;
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.FLUID_PIPE_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.FLUID_PIPE.getDefaultState();
    }
    
    @Override
    public String getPipeTypeName() {
        return "fluid";
    }
    
    @Override
    public boolean connectToBlockType(Block block) {
        return block instanceof FluidPipeBlock || block instanceof FluidPipeConnectionBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData() {
        return FLUID_PIPE_DATA;
    }
}
