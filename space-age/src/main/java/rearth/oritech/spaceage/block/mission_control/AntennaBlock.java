package rearth.oritech.spaceage.block.mission_control;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/** Passive ground antenna; spacecraft radio upkeep is handled by the simulation. */
public class AntennaBlock extends Block {

    private static final VoxelShape UPRIGHT_SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 1, 13),
            Block.box(5, 1, 5, 11, 3, 11),
            Block.box(3.5, .5, 3.5, 4.5, 1.5, 4.5),
            Block.box(11.5, .5, 3.5, 12.5, 1.5, 4.5),
            Block.box(3.5, .5, 11.5, 4.5, 1.5, 12.5),
            Block.box(11.5, .5, 11.5, 12.5, 1.5, 12.5),
            Block.box(7, 3, 7, 9, 16, 9),
            Block.box(0, 14, 7.75, 16, 16, 8.25),
            Block.box(0, 10, 7.75, 16, 12, 8.25),
            Block.box(0, 6, 7.75, 16, 8, 8.25));

    private static final Map<Direction, VoxelShape> SHAPES =
            Shapes.rotateAll(Shapes.rotate(UPRIGHT_SHAPE, OctahedralGroup.BLOCK_ROT_X_90));

    public AntennaBlock(Properties properties) {

        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        var facing = context.getClickedFace();
        if (facing.getAxis().isVertical()) return defaultBlockState();
        var level = context.getLevel();
        var supportPos = context.getClickedPos().relative(facing.getOpposite());
        // Support sets the orientation at placement. Rocket assembly moves blocks independently.
        if (level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing))
            return defaultBlockState().setValue(BlockStateProperties.FACING, facing);
        return defaultBlockState();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {

        return state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {

        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return SHAPES.get(state.getValue(BlockStateProperties.FACING));
    }
}
