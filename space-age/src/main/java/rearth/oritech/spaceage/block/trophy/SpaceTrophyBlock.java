package rearth.oritech.spaceage.block.trophy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.BlockContent;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public final class SpaceTrophyBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SpaceTrophyBlock(Properties properties) {

        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ASSEMBLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(FACING, ASSEMBLED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {

        var pos = context.getClickedPos().above();
        if (context.getLevel().isOutsideBuildHeight(pos) || !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {

        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {

        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new SpaceTrophyBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {

        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SpaceTrophyBlockEntity trophy)) return;
        for (var offset : trophy.getCorePositions())
            level.setBlockAndUpdate(pos.offset(offset), BlockContent.COMPLEX_PLATING.get().defaultBlockState());
        trophy.initMultiblock(state);
        trophy.setChanged();
    }
}
