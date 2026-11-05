package rearth.oritech.spaceage.block.mission_control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class MissionControlBlock extends SurveyModuleBlock {

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
