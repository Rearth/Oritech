package rearth.oritech.block.blocks.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.TagContent;

public class MetalBeamBlock extends Block {
    
    // 0 = foot, 1 = inner, 2 = head
    private static final IntegerProperty BEAM_STATE = IntegerProperty.create("beam", 0, 2);
    
    private static final VoxelShape BEAM_SHAPE = Block.box(4, 0, 4, 12, 16, 12);
    
    public MetalBeamBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(BEAM_STATE, 0));
    }
    
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BEAM_STATE);
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return BEAM_SHAPE;
    }
    
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return BEAM_SHAPE;
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        return getTargetState(world, pos);
    }
    
    private BlockState getTargetState(LevelAccessor world, BlockPos pos) {
        var isFrameSupport = world.getBlockState(pos).is(TagContent.MACHINE_FRAME_SUPPORT);
        var blockBelow = world.getBlockState(pos.below()).getBlock();
        var beamBelow = blockBelow.equals(BlockContent.METAL_BEAM_BLOCK) || (isFrameSupport && blockBelow.equals(BlockContent.MACHINE_FRAME_BLOCK));
        var blockAbove = world.getBlockState(pos.above()).getBlock();
        var beamAbove = blockAbove.equals(BlockContent.METAL_BEAM_BLOCK) || (isFrameSupport && blockAbove.equals(BlockContent.MACHINE_FRAME_BLOCK));
        
        var state = defaultBlockState();
        
        if (beamBelow && beamAbove)
            return state.setValue(BEAM_STATE, 1);
        
        if (beamBelow)
            return state.setValue(BEAM_STATE, 2);
        
        return state.setValue(BEAM_STATE, 0);
    }
    
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return getTargetState(world, pos);
    }
}
