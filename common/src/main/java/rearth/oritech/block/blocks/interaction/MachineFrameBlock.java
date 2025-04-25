package rearth.oritech.block.blocks.interaction;

import net.minecraft.block.*;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.init.TagContent;

import java.util.List;
import java.util.Objects;

public class MachineFrameBlock extends Block {
    
    private static final Boolean USE_ACCURATE_OUTLINES = Oritech.CONFIG.tightMachineFrameHitboxes();
    
    public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
    public static final BooleanProperty EAST = ConnectingBlock.EAST;
    public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;      // south and west are only needed for voxel shapes
    public static final BooleanProperty WEST = ConnectingBlock.WEST;
    public static final BooleanProperty UP = ConnectingBlock.UP;
    public static final BooleanProperty DOWN = ConnectingBlock.DOWN;        // used to connect to pillars to make things look nice
    
    protected final VoxelShape[] boundingShapes;
    
    public MachineFrameBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(UP, false).with(DOWN, false));
        boundingShapes = createShapes();
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        tooltip.add(Text.translatable("tooltip.oritech.machine_frame").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("tooltip.oritech.machine_frame.1", Oritech.CONFIG.processingMachines.machineFrameMaxLength(), Oritech.CONFIG.processingMachines.machineFrameMaxLength()).formatted(Formatting.GRAY));
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    private VoxelShape getShape(BlockState state) {
        var shape = boundingShapes[0];
        
        if (state.get(NORTH))
            shape = VoxelShapes.union(shape, boundingShapes[1]);
        if (state.get(EAST))
            shape = VoxelShapes.union(shape, boundingShapes[2]);
        if (state.get(SOUTH))
            shape = VoxelShapes.union(shape, boundingShapes[3]);
        if (state.get(WEST))
            shape = VoxelShapes.union(shape, boundingShapes[4]);
        if (state.get(UP))
            shape = VoxelShapes.union(shape, boundingShapes[5]);
        if (state.get(DOWN))
            shape = VoxelShapes.union(shape, boundingShapes[6]);
        
        return shape;
    }
    
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!USE_ACCURATE_OUTLINES)
            return super.getOutlineShape(state, world, pos, context);
        return getShape(state);
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShape(state);
    }
    
    protected VoxelShape[] createShapes() {
        VoxelShape inner = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
        VoxelShape north = Block.createCuboidShape(5, 5, 0, 11, 11, 5);
        VoxelShape east = Block.createCuboidShape(0, 5, 5, 5, 11, 11);
        VoxelShape south = Block.createCuboidShape(5, 5, 11, 11, 11, 16);
        VoxelShape west = Block.createCuboidShape(11, 5, 5, 16, 11, 11);
        VoxelShape up = Block.createCuboidShape(5, 5, 5, 11, 16, 11);
        VoxelShape down = Block.createCuboidShape(5, 0, 5, 11, 5, 11);
        
        return new VoxelShape[] {inner, north, west, south, east, up, down};
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var baseState = super.getPlacementState(ctx);
        
        var northConnected = ctx.getWorld().getBlockState(ctx.getBlockPos().north()).getBlock() == this;
        var eastConnected = ctx.getWorld().getBlockState(ctx.getBlockPos().east()).getBlock() == this;
        var southConnected = ctx.getWorld().getBlockState(ctx.getBlockPos().south()).getBlock() == this;
        var westConnected = ctx.getWorld().getBlockState(ctx.getBlockPos().west()).getBlock() == this;
        var upConnected = ctx.getWorld().getBlockState(ctx.getBlockPos().up()).isIn(TagContent.MACHINE_FRAME_SUPPORT);
        var downConnected = ctx.getWorld().getBlockState(ctx.getBlockPos().down()).isIn(TagContent.MACHINE_FRAME_SUPPORT);
        
        return Objects.requireNonNull(baseState).with(NORTH, northConnected).with(EAST, eastConnected).with(SOUTH, southConnected).with(WEST, westConnected).with(UP, upConnected).with(DOWN, downConnected);
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        
        var northConnected = world.getBlockState(pos.north()).getBlock() == this;
        var eastConnected = world.getBlockState(pos.east()).getBlock() == this;
        var southConnected = world.getBlockState(pos.south()).getBlock() == this;
        var westConnected = world.getBlockState(pos.west()).getBlock() == this;
        var upConnected = world.getBlockState(pos.up()).isIn(TagContent.MACHINE_FRAME_SUPPORT);
        var downConnected = world.getBlockState(pos.down()).isIn(TagContent.MACHINE_FRAME_SUPPORT);
        
        return state.with(NORTH, northConnected).with(EAST, eastConnected).with(SOUTH, southConnected).with(WEST, westConnected).with(UP, upConnected).with(DOWN, downConnected);
        
    }
}
