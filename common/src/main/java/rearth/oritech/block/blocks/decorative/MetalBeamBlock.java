package rearth.oritech.block.blocks.decorative;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.TagContent;

public class MetalBeamBlock extends Block {
    
    // 0 = foot, 1 = inner, 2 = head
    private static final IntProperty BEAM_STATE = IntProperty.of("beam", 0, 2);
    
    private static final VoxelShape BEAM_SHAPE = Block.createCuboidShape(4, 0, 4, 12, 16, 12);
    
    public MetalBeamBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(BEAM_STATE, 0));
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(BEAM_STATE);
    }
    
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BEAM_SHAPE;
    }
    
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BEAM_SHAPE;
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var world = ctx.getWorld();
        var pos = ctx.getBlockPos();
        return getTargetState(world, pos);
    }
    
    private BlockState getTargetState(WorldAccess world, BlockPos pos) {
        var isFrameSupport = world.getBlockState(pos).isIn(TagContent.MACHINE_FRAME_SUPPORT);
        var blockBelow = world.getBlockState(pos.down()).getBlock();
        var beamBelow = blockBelow.equals(BlockContent.METAL_BEAM_BLOCK) || (isFrameSupport && blockBelow.equals(BlockContent.MACHINE_FRAME_BLOCK));
        var blockAbove = world.getBlockState(pos.up()).getBlock();
        var beamAbove = blockAbove.equals(BlockContent.METAL_BEAM_BLOCK) || (isFrameSupport && blockAbove.equals(BlockContent.MACHINE_FRAME_BLOCK));
        
        var state = getDefaultState();
        
        if (beamBelow && beamAbove)
            return state.with(BEAM_STATE, 1);
        
        if (beamBelow)
            return state.with(BEAM_STATE, 2);
        
        return state.with(BEAM_STATE, 0);
    }
    
    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return getTargetState(world, pos);
    }
}
