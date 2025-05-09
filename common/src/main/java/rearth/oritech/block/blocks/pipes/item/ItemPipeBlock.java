package rearth.oritech.block.blocks.pipes.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import java.util.HashMap;
import java.util.List;

public class ItemPipeBlock extends GenericPipeBlock {
    
    public static HashMap<Identifier, GenericPipeInterfaceEntity.PipeNetworkData> ITEM_PIPE_DATA = new HashMap<>();
    
    public ItemPipeBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public TriFunction<World, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((world, pos, direction) -> ItemApi.BLOCK.find(world, pos, direction) != null);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        
        var showExtra = Screen.hasControlDown();
        if (showExtra) {
            for (int i = 1; i <= 4; i++) {
                tooltip.add(Text.translatable("tooltip.oritech.item_pipe." + i).formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
            }
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
        
        super.appendTooltip(stack, context, tooltip, options);
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
        return block instanceof ItemPipeBlock || block instanceof ItemPipeConnectionBlock || block instanceof ItemPipeDuctBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return ITEM_PIPE_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
    
    public static class FramedItemPipeBlock extends ItemPipeBlock {
        
        public FramedItemPipeBlock(Settings settings) {
            super(settings);
        }
        
        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
            return VoxelShapes.fullCube();
        }
        
        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
            return state.getOutlineShape(world, pos);
        }
        
        @Override
        public BlockState getNormalBlock() {
            return BlockContent.FRAMED_ITEM_PIPE.getDefaultState();
        }
        
        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.FRAMED_ITEM_PIPE_CONNECTION.getDefaultState();
        }
    }
    
    public static class TransparentItemPipe extends ItemPipeBlock {
        
        public TransparentItemPipe(Settings settings) {
            super(settings);
        }
        
        @Override
        protected VoxelShape[] createShapes() {
            VoxelShape inner = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
            VoxelShape north = Block.createCuboidShape(5, 5, 0, 11, 11, 5);
            VoxelShape east = Block.createCuboidShape(0, 5, 5, 5, 11, 11);
            VoxelShape south = Block.createCuboidShape(5, 5, 11, 11, 11, 16);
            VoxelShape west = Block.createCuboidShape(11, 5, 5, 16, 11, 11);
            VoxelShape up = Block.createCuboidShape(5, 11, 5, 11, 16, 11);
            VoxelShape down = Block.createCuboidShape(5, 0, 5, 11, 5, 11);
            
            return new VoxelShape[]{inner, north, west, south, east, up, down};
        }
        
        @Override
        public BlockState getNormalBlock() {
            return BlockContent.TRANSPARENT_ITEM_PIPE.getDefaultState();
        }
        
        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.getDefaultState();
        }
    }
}
