package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import static rearth.oritech.block.blocks.decorative.TechDoorBlock.OPENED;

// this is the upper section of the tech door
public class TechDoorBlockHinge extends HorizontalFacingBlock {
    
    public TechDoorBlockHinge(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(OPENED, false).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPENED);
        builder.add(Properties.HORIZONTAL_FACING);
    }
    
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }
    
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        
        // forward the event to bottom block
        if (world.isClient) return;
        world.updateNeighbor(pos.down(), sourceBlock, sourcePos);
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            var belowState = world.getBlockState(pos.down());
            if (!player.isCreative())
                Block.dropStacks(belowState, world, pos.down());
            world.setBlockState(pos.down(), Blocks.AIR.getDefaultState());
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return TechDoorBlock.getClosedShape(state.get(Properties.HORIZONTAL_FACING));
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(OPENED))
            return VoxelShapes.empty();
        return super.getCollisionShape(state, world, pos, context);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }
    
    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }
}
