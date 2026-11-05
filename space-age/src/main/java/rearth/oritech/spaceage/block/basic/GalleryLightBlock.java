package rearth.oritech.spaceage.block.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class GalleryLightBlock extends Block {

    public static final EnumProperty<Connection> CONNECTION = EnumProperty.create("connection", Connection.class);

    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
            Block.box(6, 10, 14, 10, 12, 16),
            Block.box(7, 11, 13, 9, 12, 14),
            Block.box(2, 11, 12, 14, 12, 13),
            Block.box(0, 3, 12.4375, 16, 11, 12.5625)));

    public GalleryLightBlock(Properties properties) {

        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(CONNECTION, Connection.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(BlockStateProperties.HORIZONTAL_FACING, CONNECTION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        var facing = context.getClickedFace();
        if (!facing.getAxis().isHorizontal()) return null;
        var state = defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? connections(state, context.getLevel(), context.getClickedPos()) : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {

        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {

        if (direction == state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite() && !state.canSurvive(level, pos))
            return Blocks.AIR.defaultBlockState();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (direction == facing.getClockWise() || direction == facing.getCounterClockWise()) return connections(state, level, pos);
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    private BlockState connections(BlockState state, LevelReader level, BlockPos pos) {

        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var left = connects(level.getBlockState(pos.relative(facing.getClockWise())), facing);
        var right = connects(level.getBlockState(pos.relative(facing.getCounterClockWise())), facing);
        // End names describe their position in the row, viewed from the front.
        return state.setValue(CONNECTION, left && right ? Connection.CENTER : left ? Connection.RIGHT : right ? Connection.LEFT : Connection.SINGLE);
    }

    private boolean connects(BlockState state, Direction facing) {

        return state.is(this) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {

        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {

        var mirrored = state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
        if (mirror == Mirror.NONE) return mirrored;
        return mirrored.setValue(CONNECTION, switch (state.getValue(CONNECTION)) {
            case LEFT -> Connection.RIGHT;
            case RIGHT -> Connection.LEFT;
            default -> state.getValue(CONNECTION);
        });
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return SHAPES.get(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    public enum Connection implements StringRepresentable {
        SINGLE("single"), LEFT("left"), RIGHT("right"), CENTER("center");

        private final String name;

        Connection(String name) {

            this.name = name;
        }

        @Override
        public String getSerializedName() {

            return name;
        }
    }

}
