package rearth.oritech.block.blocks.accelerator;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class AcceleratorPassthroughBlock extends HorizontalDirectionalBlock {
    public AcceleratorPassthroughBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }
    
    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, world, pos, newState, moved);
        AcceleratorParticleLogic.resetCachedGate(pos);
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        AcceleratorParticleLogic.resetCachedGate(pos);
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClientSide) {
            AcceleratorParticleLogic.resetNearbyCache(pos);
        }
    }
    
    @Override
    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
        super.wasExploded(world, pos, explosion);
        AcceleratorParticleLogic.resetCachedGate(pos);
    }
    
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
}
