package rearth.oritech.block.blocks.arcane;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SpawnerCageBlock extends Block implements TooltipProvider {

    public static BooleanProperty UP = BooleanProperty.create("up");
    public static BooleanProperty DOWN = BooleanProperty.create("down");
    public static BooleanProperty NORTH = BooleanProperty.create("north");
    public static BooleanProperty EAST = BooleanProperty.create("east");
    public static BooleanProperty SOUTH = BooleanProperty.create("south");
    public static BooleanProperty WEST = BooleanProperty.create("west");

    public SpawnerCageBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(UP, false).setValue(DOWN, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var level = ctx.getLevel();
        var pos = ctx.getClickedPos();
        return getTargetState(level, pos);
    }

    private BlockState getTargetState(LevelReader level, BlockPos pos) {

        var state = defaultBlockState();

        if (level.getBlockState(pos.above()).is(this.asBlock()))
            state = state.setValue(UP, true);
        if (level.getBlockState(pos.below()).is(this.asBlock()))
            state = state.setValue(DOWN, true);
        if (level.getBlockState(pos.north()).is(this.asBlock()))
            state = state.setValue(NORTH, true);
        if (level.getBlockState(pos.east()).is(this.asBlock()))
            state = state.setValue(EAST, true);
        if (level.getBlockState(pos.south()).is(this.asBlock()))
            state = state.setValue(SOUTH, true);
        if (level.getBlockState(pos.west()).is(this.asBlock()))
            state = state.setValue(WEST, true);

        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {

        return getTargetState(level, pos);
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        consumer.accept(Component.translatable("tooltip.oritech.spawner_cage").withStyle(ChatFormatting.GRAY));
    }
}
