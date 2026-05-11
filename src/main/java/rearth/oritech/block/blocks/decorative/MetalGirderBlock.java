package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.BlockContent;

public class MetalGirderBlock extends HorizontalDirectionalBlock {
    
    public static final BooleanProperty HEADING = BooleanProperty.create("heading");
    
    private static final VoxelShape NORTH_SHAPE = Block.box(0, 4, 4, 16, 12, 12);
    private static final VoxelShape EAST_SHAPE = Block.box(4, 4, 0, 12, 12, 16);
    
    public MetalGirderBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(HEADING, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEADING);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (facing.getAxis().equals(Direction.Axis.X)) return NORTH_SHAPE;
        return EAST_SHAPE;
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getCollisionShape(state, world, pos, context);
    }
    
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var blockInBack = world.getBlockState(pos.offset(facing.getNormal())).is(BlockContent.METAL_GIRDER_BLOCK);
        var blockInFront = world.getBlockState(pos.offset(facing.getOpposite().getNormal())).is(BlockContent.METAL_GIRDER_BLOCK);
        var straight = false;
        if (blockInFront && !blockInBack) {
            facing = facing.getOpposite();
        } else if (blockInFront && blockInBack) {
            straight = true;
        }
        
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(HEADING, !straight);
        
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        
        // if placed on ceiling/wall, rotate with player facing. Otherwise get surface facing.
        // if block behind is other girder, use straight model. Otherwise it's an endstop.
        
        var facing = ctx.getClickedFace();
        
        if (ctx.getClickedFace().getAxis().equals(Direction.Axis.Y)) {
            facing = ctx.getHorizontalDirection().getOpposite();
        }
        
        var blockInBack = world.getBlockState(pos.offset(facing.getNormal())).is(BlockContent.METAL_GIRDER_BLOCK);
        var blockInFront = world.getBlockState(pos.offset(facing.getOpposite().getNormal())).is(BlockContent.METAL_GIRDER_BLOCK);
        var straight = false;
        if (blockInFront && !blockInBack) {
            facing = facing.getOpposite();
        } else if (blockInFront && blockInBack) {
            straight = true;
        }
        
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(HEADING, !straight);
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
}
