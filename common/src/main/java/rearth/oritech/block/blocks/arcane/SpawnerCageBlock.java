package rearth.oritech.block.blocks.arcane;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class SpawnerCageBlock extends Block {
    
    public static BooleanProperty UP = BooleanProperty.create("up");
    public static BooleanProperty DOWN = BooleanProperty.create("down");
    public static BooleanProperty NORTH = BooleanProperty.create("north");
    public static BooleanProperty EAST = BooleanProperty.create("east");
    public static BooleanProperty SOUTH = BooleanProperty.create("south");
    public static BooleanProperty WEST = BooleanProperty.create("west");
    
    public SpawnerCageBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(UP, false).setValue(DOWN, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST,false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        return getTargetState(world, pos);
    }
    
    private BlockState getTargetState(LevelAccessor world, BlockPos pos) {
        
        var state = defaultBlockState();
        
        if (world.getBlockState(pos.above()).is(this.asBlock()))
            state = state.setValue(UP, true);
        if (world.getBlockState(pos.below()).is(this.asBlock()))
            state = state.setValue(DOWN, true);
        if (world.getBlockState(pos.north()).is(this.asBlock()))
            state = state.setValue(NORTH, true);
        if (world.getBlockState(pos.east()).is(this.asBlock()))
            state = state.setValue(EAST, true);
        if (world.getBlockState(pos.south()).is(this.asBlock()))
            state = state.setValue(SOUTH, true);
        if (world.getBlockState(pos.west()).is(this.asBlock()))
            state = state.setValue(WEST, true);
        
        return state;
    }
    
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return getTargetState(world, pos);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        tooltip.add(Component.translatable("tooltip.oritech.spawner_cage").withStyle(ChatFormatting.GRAY));
    }
}
