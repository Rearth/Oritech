package rearth.oritech.spaceage.block.launch_ramp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.util.CollisionShapes;
import rearth.oritech.util.TooltipHelper;

import java.util.function.Consumer;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

/** The launcher points along the player's placement direction. */
public class LaunchRampBlock extends Block implements EntityBlock, TooltipProvider {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public LaunchRampBlock(Properties properties) {

        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(ASSEMBLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(FACING, ASSEMBLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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

        return new LaunchRampBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {

        if (type != SpaceAgeBlockEntities.LAUNCH_RAMP.get()) return null;
        return (world, pos, block, entity) -> ((LaunchRampBlockEntity) entity).tick(world, pos, block, (LaunchRampBlockEntity) entity);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof LaunchRampBlockEntity launcher))
            return InteractionResult.SUCCESS;
        if (!launcher.isAssembled(state)) {
            if (launcher.tryPlaceNextCore(player)) return InteractionResult.SUCCESS;
            if (!launcher.initMultiblock(state))
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
            return InteractionResult.SUCCESS;
        }
        serverPlayer.openMenu(launcher, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        var shape = CollisionShapes.getShape(state, level, pos);
        return shape != null ? shape : super.getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.INVISIBLE;
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {

        TooltipHelper.addMachineTooltip(consumer, this, this);
    }
}
