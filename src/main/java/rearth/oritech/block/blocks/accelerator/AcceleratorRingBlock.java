package rearth.oritech.block.blocks.accelerator;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;

import java.util.Objects;
import java.util.function.Consumer;

public class AcceleratorRingBlock extends AcceleratorPassthroughBlock implements TooltipProvider {

    public static final IntegerProperty BENT = IntegerProperty.create("bent", 0, 2);    // 0 = straight, 1 = left, 2 = right
    public static final IntegerProperty REDSTONE_STATE = IntegerProperty.create("redstone_state", 0, 3);    // 0-2 = same as bent, 3 = was never powered

    public AcceleratorRingBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BENT, 0).setValue(REDSTONE_STATE, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BENT, REDSTONE_STATE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BENT, 0).setValue(REDSTONE_STATE, 3);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(2, 0, 2, 14, 12, 14);
    }

    // allow redstone to connect
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @org.jspecify.annotations.Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        if (level.isClientSide()) return;

        var isPowered = level.hasNeighborSignal(pos);
        var lastRedstone = state.getValue(REDSTONE_STATE);
        var lastBent = state.getValue(BENT);

        // straight pipes don't react to redstone
        if (lastBent == 0 && lastRedstone == 3) return;

        // on new redstone signal (redstone stored is not bent)
        if (isPowered && (lastRedstone == 0 || lastRedstone == 3)) {
            // store bent state and set straight
            level.setBlock(pos, state.setValue(REDSTONE_STATE, lastBent).setValue(BENT, 0), Block.UPDATE_CLIENTS, 1);
            AcceleratorParticleLogic.resetCachedGate(pos);
        } else if (!isPowered && lastRedstone != 3 && lastRedstone != 0) {   // on redstone disabled
            // set bent to lastbent, set redstone to straight
            level.setBlock(pos, state.setValue(REDSTONE_STATE, 0).setValue(BENT, lastRedstone), Block.UPDATE_CLIENTS, 1);
            AcceleratorParticleLogic.resetCachedGate(pos);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        var newBent = (state.getValue(BENT) + 1) % 3;
        level.setBlockAndUpdate(pos, state.setValue(BENT, newBent).setValue(REDSTONE_STATE, 3));
        AcceleratorParticleLogic.resetCachedGate(pos);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        var showExtra = Minecraft.getInstance().hasControlDown();
        if (!showExtra) {
            consumer.accept(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        } else {
            consumer.accept(Component.translatable("tooltip.oritech.accelerator_ring").withStyle(ChatFormatting.GRAY));
        }
    }
}
