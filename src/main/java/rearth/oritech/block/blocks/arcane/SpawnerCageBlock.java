package rearth.oritech.block.blocks.arcane;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpawnerCageBlock extends Block {
    
    public static BooleanProperty UP = BooleanProperty.of("up");
    public static BooleanProperty DOWN = BooleanProperty.of("down");
    public static BooleanProperty NORTH = BooleanProperty.of("north");
    public static BooleanProperty EAST = BooleanProperty.of("east");
    public static BooleanProperty SOUTH = BooleanProperty.of("south");
    public static BooleanProperty WEST = BooleanProperty.of("west");
    
    public SpawnerCageBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(UP, false).with(DOWN, false).with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST,false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var world = ctx.getWorld();
        var pos = ctx.getBlockPos();
        return getTargetState(world, pos);
    }
    
    private BlockState getTargetState(WorldAccess world, BlockPos pos) {
        
        var state = getDefaultState();
        
        if (world.getBlockState(pos.up()).isOf(this.asBlock()))
            state = state.with(UP, true);
        if (world.getBlockState(pos.down()).isOf(this.asBlock()))
            state = state.with(DOWN, true);
        if (world.getBlockState(pos.north()).isOf(this.asBlock()))
            state = state.with(NORTH, true);
        if (world.getBlockState(pos.east()).isOf(this.asBlock()))
            state = state.with(EAST, true);
        if (world.getBlockState(pos.south()).isOf(this.asBlock()))
            state = state.with(SOUTH, true);
        if (world.getBlockState(pos.west()).isOf(this.asBlock()))
            state = state.with(WEST, true);
        
        return state;
    }
    
    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return getTargetState(world, pos);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        tooltip.add(Text.translatable("tooltip.oritech.spawner_cage").formatted(Formatting.GRAY));
    }
}
