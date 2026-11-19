package rearth.oritech.spaceage.block.mission_control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.function.Function;

public class MissionControlBlock extends SurveyModuleBlock {

    // Approximate the tilted screens in the idle model with boxes inside the block footprint.
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
            Block.box(4, 0, 4, 12, 13, 12),
            Block.box(6, 13, 5.75, 10, 17, 9.75),
            Block.box(6, 3, 11.75, 10, 12, 14.75),
            Block.box(3, 13.5, 3.5, 13, 14.5, 12.5),
            Block.box(3, 10, 0, 13, 12.5, 2),
            Block.box(3, 12.5, 1, 13, 14.5, 3.5),
            Block.box(7, 7, 0, 9, 10.5, 4.75),
            Block.box(0, 9.5, 3, 2.5, 13, 13),
            Block.box(1.5, 13, 3, 4, 15.5, 13),
            Block.box(3, 15.5, 3, 5.5, 18, 13),
            Block.box(13.5, 9.5, 3, 16, 13, 13),
            Block.box(12, 13, 3, 14.5, 15.5, 13),
            Block.box(10.5, 15.5, 3, 13, 18, 13)));

    private static final Map<Direction, VoxelShape> CONNECTION_SHAPES = Shapes.rotateHorizontal(Shapes.or(
            Block.box(7, 3, 2, 9, 5, 4),
            Block.box(6, 3, 0, 10, 5, 2)));

    private final Function<BlockState, VoxelShape> shapesByState = getShapeForEachState(MissionControlBlock::buildShape);

    public MissionControlBlock(Properties properties) {

        super(properties);
        var state = defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        for (var direction : Direction.Plane.HORIZONTAL) state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.NORTH, BlockStateProperties.EAST,
                BlockStateProperties.SOUTH, BlockStateProperties.WEST);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return shapesByState.apply(state);
    }

    private static VoxelShape buildShape(BlockState state) {

        var shape = SHAPES.get(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
        for (var direction : Direction.Plane.HORIZONTAL) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction)))
                shape = Shapes.or(shape, CONNECTION_SHAPES.get(direction));
        }
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        var state = defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
        for (var direction : Direction.Plane.HORIZONTAL)
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                    context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(SpaceAgeBlocks.ANTENNA));
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {

        if (direction.getAxis().isHorizontal())
            return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), neighborState.is(SpaceAgeBlocks.ANTENNA));
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof GroundStationBlockEntity station) {
            serverPlayer.openMenu(station, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
