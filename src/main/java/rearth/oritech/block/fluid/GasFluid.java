package rearth.oritech.block.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public abstract class GasFluid extends FlowableFluid {
    
    @Override
    public boolean isInfinite(World world) {
        return false;
    }
    
    @Override
    protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        final BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
        Block.dropStacks(state, world, pos, blockEntity);
    }
    
    @Override
    public void onScheduledTick(World world, BlockPos pos, FluidState state) {
        System.out.println("here");
        var doDisappear = world.random.nextFloat() < 0.1;
        if (doDisappear) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            System.out.println("disappeared");
            return;
        }
        super.onScheduledTick(world, pos, state);
    }
    
    @Override
    protected boolean canFlow(BlockView world, BlockPos fluidPos, BlockState fluidBlockState, Direction flowDirection, BlockPos flowTo, BlockState flowToBlockState, FluidState fluidState, Fluid fluid) {
        return false;
    }
    
    @Override
    protected int getFlowSpeed(WorldView world) {
        return 5;
    }
    
    @Override
    public boolean matchesType(Fluid fluid) {
        return fluid == getStill() || fluid == getFlowing();
    }
    
    @Override
    protected int getLevelDecreasePerBlock(WorldView world) {
        return 3;
    }
    
    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
        return false;
    }
    
    @Override
    public int getTickRate(WorldView world) {
        return 15;
    }
    
    @Override
    protected float getBlastResistance() {
        return 10f;
    }
}
