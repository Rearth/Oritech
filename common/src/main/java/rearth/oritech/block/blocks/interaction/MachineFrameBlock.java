package rearth.oritech.block.blocks.interaction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.init.TagContent;
import java.util.List;
import java.util.Objects;

public class MachineFrameBlock extends Block {
    
    private static final Boolean USE_ACCURATE_OUTLINES = Oritech.CONFIG.tightMachineFrameHitboxes();
    
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;      // south and west are only needed for voxel shapes
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;        // used to connect to pillars to make things look nice
    
    protected final VoxelShape[] boundingShapes;
    
    public MachineFrameBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
        boundingShapes = createShapes();
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        tooltip.add(Component.translatable("tooltip.oritech.machine_frame").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.oritech.machine_frame.1", Oritech.CONFIG.processingMachines.machineFrameMaxLength(), Oritech.CONFIG.processingMachines.machineFrameMaxLength()).withStyle(ChatFormatting.GRAY));
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    private VoxelShape getShape(BlockState state) {
        return boundingShapes[packStates(state)];
    }

    private static int packStates(BlockState state) {
        int i = 0;
        if (state.getValue(NORTH)) i |= 1;
        if (state.getValue(EAST)) i |= 2;
        if (state.getValue(SOUTH)) i |= 4;
        if (state.getValue(WEST)) i |= 8;
        if (state.getValue(UP)) i |= 16;
        if (state.getValue(DOWN)) i |= 32;
        return i;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (!USE_ACCURATE_OUTLINES)
            return super.getShape(state, world, pos, context);
        return getShape(state);
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state);
    }
    
    protected VoxelShape[] createShapes() {
        return GenericPipeBlock.createShapes(
                Block.box(5, 5, 5, 11, 11, 11),
                Block.box(5, 5, 0, 11, 11, 5),
                Block.box(0, 5, 5, 5, 11, 11),
                Block.box(5, 5, 11, 11, 11, 16),
                Block.box(11, 5, 5, 16, 11, 11),
                Block.box(5, 5, 5, 11, 16, 11),
                Block.box(5, 0, 5, 11, 5, 11)
        );
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var baseState = super.getStateForPlacement(ctx);
        
        var northConnected = ctx.getLevel().getBlockState(ctx.getClickedPos().north()).getBlock() == this;
        var eastConnected = ctx.getLevel().getBlockState(ctx.getClickedPos().east()).getBlock() == this;
        var southConnected = ctx.getLevel().getBlockState(ctx.getClickedPos().south()).getBlock() == this;
        var westConnected = ctx.getLevel().getBlockState(ctx.getClickedPos().west()).getBlock() == this;
        var upConnected = ctx.getLevel().getBlockState(ctx.getClickedPos().above()).is(TagContent.MACHINE_FRAME_SUPPORT);
        var downConnected = ctx.getLevel().getBlockState(ctx.getClickedPos().below()).is(TagContent.MACHINE_FRAME_SUPPORT);
        
        return Objects.requireNonNull(baseState).setValue(NORTH, northConnected).setValue(EAST, eastConnected).setValue(SOUTH, southConnected).setValue(WEST, westConnected).setValue(UP, upConnected).setValue(DOWN, downConnected);
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        
        var northConnected = world.getBlockState(pos.north()).getBlock() == this;
        var eastConnected = world.getBlockState(pos.east()).getBlock() == this;
        var southConnected = world.getBlockState(pos.south()).getBlock() == this;
        var westConnected = world.getBlockState(pos.west()).getBlock() == this;
        var upConnected = world.getBlockState(pos.above()).is(TagContent.MACHINE_FRAME_SUPPORT);
        var downConnected = world.getBlockState(pos.below()).is(TagContent.MACHINE_FRAME_SUPPORT);
        
        return state.setValue(NORTH, northConnected).setValue(EAST, eastConnected).setValue(SOUTH, southConnected).setValue(WEST, westConnected).setValue(UP, upConnected).setValue(DOWN, downConnected);
        
    }
}
