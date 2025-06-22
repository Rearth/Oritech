package rearth.oritech.block.blocks.pipes.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
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
    
    public static BooleanProperty HAS_MOTOR = BooleanProperty.of("has_motor");

    public ItemPipeConnectionBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(HAS_MOTOR, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_MOTOR);
    }
    
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (world.isClient || !hasExtractingSide(state) || state.get(HAS_MOTOR))
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        
        var ownEntity = world.getBlockEntity(pos);
        if (ownEntity instanceof ItemPipeInterfaceEntity && stack.getItem().equals(ItemContent.MOTOR)) {
            world.setBlockState(pos, state.with(HAS_MOTOR, true), Block.FORCE_STATE, 0);
            stack.decrement(1);
            world.playSound(null, pos, SoundContent.SHORT_SERVO, SoundCategory.BLOCKS, 0.9f, 1.2f);
            return ItemActionResult.CONSUME;
        }
        
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient && state.get(HAS_MOTOR)) {
            world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(ItemContent.MOTOR)));
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    public TriFunction<World, BlockPos, Direction, Boolean> apiValidationFunction() {
        return ((world, pos, direction) -> ItemApi.BLOCK.find(world, pos, direction) != null);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeInterfaceEntity(pos, state);
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
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return ITEM_PIPE_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }

    public static class FramedItemPipeConnectionBlock extends ItemPipeConnectionBlock {

        public FramedItemPipeConnectionBlock(Settings settings) {
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

    public static class TransparentItemPipeConnectionBlock extends ItemPipeConnectionBlock {

        public TransparentItemPipeConnectionBlock(Settings settings) {
            super(settings);
        }

        @Override
        public BlockState getNormalBlock() {
            return BlockContent.TRANSPARENT_ITEM_PIPE.getDefaultState();
        }

        @Override
        public BlockState getConnectionBlock() {
            return BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.getDefaultState();
        }
        
        @Override
        protected VoxelShape[] createShapes() {
            return THICK_SHAPES;
        }
    }
}
