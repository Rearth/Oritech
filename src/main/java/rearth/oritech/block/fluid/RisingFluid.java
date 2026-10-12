package rearth.oritech.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** A finite steam column that spreads upward and stops at obstacles. */
public abstract class RisingFluid extends BaseFlowingFluid {

    protected RisingFluid(Properties properties) {
        super(properties);
    }

    @Override
    protected void spread(ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (fluidState.isEmpty()) {
            return;
        }

        var abovePos = pos.above();
        var aboveState = level.getBlockState(abovePos);
        var aboveFluid = aboveState.getFluidState();
        int nextAmount = fluidState.getAmount() - getDropOff(level);
        if (nextAmount <= 0) {
            return;
        }

        var newAboveFluid = getFlowing(nextAmount, false);
        if (canPassThrough(level, pos, state, Direction.UP, abovePos, aboveState, aboveFluid)
            && aboveFluid.canBeReplacedWith(level, abovePos, newAboveFluid.getType(), Direction.UP)) {
            spreadTo(level, abovePos, aboveState, Direction.UP, newAboveFluid);
        }
    }

    @Override
    protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
        // Only the block below can feed steam. Losing that supply clears the column from bottom to top.
        var belowPos = pos.below();
        var belowState = level.getBlockState(belowPos);
        var belowFluid = belowState.getFluidState();
        if (belowFluid.getType().isSame(this)
            && canPassThroughWall(Direction.UP, level, belowPos, belowState, pos, state)) {
            int amount = belowFluid.getAmount() - getDropOff(level);
            if (amount > 0) {
                return getFlowing(amount, false);
            }
        }
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluidState) {
        return new Vec3(0, 1, 0);
    }

    @Override
    public float getOwnHeight(FluidState state) {
        // Gas fills its cell. Liquid corner heights produce sloped shelves in an upward column.
        // LEVEL controls how far steam rises (seven blocks by default), not its rendered height.
        return 1;
    }

    private boolean canPassThrough(BlockGetter level, BlockPos sourcePos, BlockState sourceState, Direction direction,
                                   BlockPos targetPos, BlockState targetState, FluidState targetFluid) {
        return !(targetFluid.getType().isSame(this) && targetFluid.isSource())
            && canHoldFluid(level, targetPos, targetState, getFlowing())
            && canPassThroughWall(direction, level, sourcePos, sourceState, targetPos, targetState);
    }

    // Same face-occlusion check as FlowingFluid.canPassThroughWall, without its cache.
    private static boolean canPassThroughWall(Direction direction, BlockGetter level, BlockPos sourcePos,
                                              BlockState sourceState, BlockPos targetPos, BlockState targetState) {
        var sourceShape = sourceState.getCollisionShape(level, sourcePos);
        var targetShape = targetState.getCollisionShape(level, targetPos);
        return sourceShape != Shapes.block() && targetShape != Shapes.block()
            && !Shapes.mergedFaceOccludes(sourceShape, targetShape, direction);
    }

    // Based on FlowingFluid.canHoldAnyFluid/canHoldSpecificFluid; these helpers are private in vanilla.
    private static boolean canHoldFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        Block block = state.getBlock();
        boolean canHoldAny = block instanceof LiquidBlockContainer
            || (!state.blocksMotion()
                && !(block instanceof DoorBlock)
                && !state.is(BlockTags.SIGNS)
                && !state.is(Blocks.LADDER)
                && !state.is(Blocks.SUGAR_CANE)
                && !state.is(Blocks.BUBBLE_COLUMN)
                && !state.is(Blocks.NETHER_PORTAL)
                && !state.is(Blocks.END_PORTAL)
                && !state.is(Blocks.END_GATEWAY)
                && !state.is(Blocks.STRUCTURE_VOID));
        return canHoldAny && (!(block instanceof LiquidBlockContainer container)
            || container.canPlaceLiquid(null, level, pos, state, fluid));
    }

    public static class Flowing extends RisingFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends RisingFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
