package rearth.oritech.block.blocks.pipes.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.block.blocks.pipes.ExtractablePipeConnectionBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.SoundContent;

import static rearth.oritech.block.blocks.pipes.item.ItemPipeBlock.ITEM_PIPE_DATA;

public class ItemPipeConnectionBlock extends ExtractablePipeConnectionBlock {
    
    public static BooleanProperty HAS_MOTOR = BooleanProperty.create("has_motor");

    public ItemPipeConnectionBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(HAS_MOTOR, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_MOTOR);
    }
    
    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        
        if (level.isClientSide || !hasExtractingSide(state) || state.getValue(HAS_MOTOR))
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        
        var ownEntity = level.getBlockEntity(pos);
        if (ownEntity instanceof ItemPipeInterfaceEntity && stack.getItem().equals(ItemContent.MOTOR)) {
            level.setBlock(pos, state.setValue(HAS_MOTOR, true), Block.UPDATE_KNOWN_SHAPE, 0);
            stack.shrink(1);
            level.playSound(null, pos, SoundContent.SHORT_SERVO, SoundSource.BLOCKS, 0.9f, 1.2f);
            return ItemInteractionResult.CONSUME;
        }
        
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        
        if (!level.isClientSide && state.getValue(HAS_MOTOR)) {
            level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ItemContent.MOTOR)));
        }
        
        return super.playerWillDestroy(level, pos, state, player);
    }
    
    @Override
    public TriFunction<Level, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((level, pos, direction) -> ItemApi.BLOCK.find(level, pos, direction) != null);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeInterfaceEntity(pos, state);
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.ITEM_PIPE_CONNECTION.defaultBlockState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.ITEM_PIPE.defaultBlockState();
    }

    @Override
    protected VoxelShape[] createShapes() {
        return THIN_SHAPES;
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
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return ITEM_PIPE_DATA.computeIfAbsent(level.dimension().location(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
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
            return BlockContent.FRAMED_ITEM_PIPE.defaultBlockState();
        }

        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.FRAMED_ITEM_PIPE_CONNECTION.defaultBlockState();
        }
    }

    public static class TransparentItemPipeConnectionBlock extends ItemPipeConnectionBlock {

        public TransparentItemPipeConnectionBlock(Properties settings) {
            super(settings);
        }

        @Override
        public BlockState getNormalBlock() {
            return BlockContent.TRANSPARENT_ITEM_PIPE.defaultBlockState();
        }

        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.defaultBlockState();
        }
        
        @Override
        protected VoxelShape[] createShapes() {
            return THICK_SHAPES;
        }
    }
}
