package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static rearth.oritech.block.blocks.decorative.TechDoorBlock.OPENED;

// this is the upper section of the tech door
public class TechDoorBlockHinge extends HorizontalDirectionalBlock {
    
    public TechDoorBlockHinge(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(OPENED, false).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPENED);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        
        // forward the event to bottom block
        if (world.isClientSide) return;
        world.neighborChanged(pos.below(), sourceBlock, sourcePos);
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide) {
            var belowState = world.getBlockState(pos.below());
            if (!player.isCreative())
                Block.dropResources(belowState, world, pos.below());
            world.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return TechDoorBlock.getClosedShape(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPENED))
            return Shapes.empty();
        return super.getCollisionShape(state, world, pos, context);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
}
