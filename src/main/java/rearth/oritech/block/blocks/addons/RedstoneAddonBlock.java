package rearth.oritech.block.blocks.addons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.util.ComparatorOutputProvider;

public class RedstoneAddonBlock extends MachineAddonBlock {

    public RedstoneAddonBlock(Properties settings, AddonSettings addonSettings) {
        super(settings, addonSettings);
        this.registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWERED);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return ((ComparatorOutputProvider) level.getBlockEntity(pos)).getComparatorOutput();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);


        if (level.isClientSide()) return;

        var isPowered = level.hasNeighborSignal(pos);

        var addonEntity = (RedstoneAddonBlockEntity) level.getBlockEntity(pos);
        addonEntity.setRedstonePowered(isPowered);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {

        var isPowered = level.hasNeighborSignal(pos);
        var poweredState = state.setValue(BlockStateProperties.POWERED, isPowered);

        return super.updateShape(poweredState, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            player.openMenu((MenuProvider) level.getBlockEntity(pos), pos);

        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return RedstoneAddonBlockEntity.class;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }

}
