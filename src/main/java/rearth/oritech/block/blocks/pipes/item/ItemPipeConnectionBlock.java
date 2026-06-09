package rearth.oritech.block.blocks.pipes.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.blocks.pipes.item.ItemPipeBlock.ITEM_PIPE_DATA;

public class ItemPipeConnectionBlock extends ExtractablePipeConnectionBlock {

    public ItemPipeConnectionBlock(Properties settings) {
        super(settings);
    }

    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((level, pos, direction) -> level.getCapability(Capabilities.Item.BLOCK, pos, direction) != null);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeInterfaceEntity(pos, state);
    }

    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.ITEM_PIPE_CONNECTION.get().defaultBlockState();
    }

    @Override
    public BlockState getNormalBlock() {
        return BlockContent.ITEM_PIPE.get().defaultBlockState();
    }

    @Override
    protected VoxelShape[] createShapes() {
        return THIN_SHAPES;
    }

    @Override
    public SavedDataType<GenericPipeInterfaceEntity.PipeNetworkData> getNetworkDataType() {
        return GenericPipeInterfaceEntity.PipeNetworkData.ITEM_TYPE;
    }


    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof ItemPipeBlock || block instanceof ItemPipeConnectionBlock || block instanceof ItemPipeDuctBlock;
    }

    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return ITEM_PIPE_DATA.computeIfAbsent(level.dimension().identifier(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }

    public static class FramedItemPipeConnectionBlock extends ItemPipeConnectionBlock {

        public FramedItemPipeConnectionBlock(Properties settings) {
            super(settings);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.block();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getShape(level, pos);
        }

        @Override
        public BlockState getNormalBlock() {
            return BlockContent.FRAMED_ITEM_PIPE.get().defaultBlockState();
        }

        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get().defaultBlockState();
        }
    }

    public static class TransparentItemPipeConnectionBlock extends ItemPipeConnectionBlock {

        public TransparentItemPipeConnectionBlock(Properties settings) {
            super(settings);
        }

        @Override
        public BlockState getNormalBlock() {
            return BlockContent.TRANSPARENT_ITEM_PIPE.get().defaultBlockState();
        }

        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get().defaultBlockState();
        }

        @Override
        protected VoxelShape[] createShapes() {
            return THICK_SHAPES;
        }
    }
}
