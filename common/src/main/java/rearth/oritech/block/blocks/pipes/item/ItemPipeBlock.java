package rearth.oritech.block.blocks.pipes.item;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import java.util.HashMap;

public class ItemPipeBlock extends GenericPipeBlock {
    
    public static HashMap<Identifier, GenericPipeInterfaceEntity.PipeNetworkData> ITEM_PIPE_DATA = new HashMap<>();

    public ItemPipeBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public TriFunction<World, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((world, pos, direction) -> ItemStorage.SIDED.find(world, pos, direction) != null);
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
    protected VoxelShape[] createShapes() {
        VoxelShape inner = Block.createCuboidShape(6, 6, 6, 10, 10, 10);
        VoxelShape north = Block.createCuboidShape(6, 6, 0, 10, 10, 6);
        VoxelShape east = Block.createCuboidShape(0, 6, 6, 6, 10, 10);
        VoxelShape south = Block.createCuboidShape(6, 6, 10, 10, 10, 16);
        VoxelShape west = Block.createCuboidShape(10, 6, 6, 16, 10, 10);
        VoxelShape up = Block.createCuboidShape(6, 10, 6, 10, 16, 10);
        VoxelShape down = Block.createCuboidShape(6, 0, 6, 10, 6, 10);
        
        return new VoxelShape[]{inner, north, west, south, east, up, down};
    }
    
    @Override
    public String getPipeTypeName() {
        return "item";
    }
    
    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof ItemPipeBlock || block instanceof ItemPipeConnectionBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return ITEM_PIPE_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
}
