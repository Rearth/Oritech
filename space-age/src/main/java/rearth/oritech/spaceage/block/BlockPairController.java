package rearth.oritech.spaceage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * The marked work faces define both cargo and machine-local recipe cells.
 */
public class BlockPairController extends Block implements EntityBlock {

    public BlockPairController(Properties properties) {

        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    public static Direction left(BlockState state) {

        var facing = state.getValue(BlockStateProperties.FACING);
        // Vertical placement has no yaw; east/west stays consistent with the marked model faces.
        return facing.getAxis().isVertical() ? Direction.WEST : facing.getCounterClockWise();
    }

    public static List<BlockPos> cells(BlockPos pos, BlockState state) {

        var left = left(state);
        return List.of(pos.relative(left), pos.relative(left.getOpposite()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        return defaultBlockState().setValue(BlockStateProperties.FACING, context.getNearestLookingDirection().getOpposite());
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new VacuumCrafterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof VacuumCrafterBlockEntity controller)
            server.openMenu(controller, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeUtf(controller.name());
            });
        return InteractionResult.SUCCESS;
    }
}
