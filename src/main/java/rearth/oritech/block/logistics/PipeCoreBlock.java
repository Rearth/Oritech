package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCore.PartPosition;

public class PipeCoreBlock extends Block implements EntityBlock {

    public PipeCoreBlock(BlockBehaviour.Properties settings) {
        super(settings.dynamicShape());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe ? pipe.getPipeShape() : PipeCoreShape.CENTER;
    }

    // this is called on normal blockstate changed from the neighboring block(?), e.g. redstone update, block placement, destruction, etc.
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        System.out.println("neighbor changed");

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) {
            pipe.updateConnections();
        }
    }

    // this is a neoforge-added things trigger by blockentities calling setChanges for comparator updates?
    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);

        System.out.println("on neighbor changed");

//        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) {
//            pipe.updateConnections();
//        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, @Nullable Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        if (!(level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) || pipe.isEmpty()) {
            return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
        }

        // let vanilla remove the block and handle harvesting when the last part breaks
        var lastPart = pipe.getOnlyPart();
        if (lastPart != null) {
            var removed = super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
            if (removed && level instanceof ServerLevel serverLevel) {
                updateWrapNeighbors(serverLevel, pos, lastPart);
            }
            return removed;
        }

        // the server chooses the part and syncs the remaining core to the client
        if (level instanceof ServerLevel serverLevel && player != null) {
            var start = player.getEyePosition();
            var end = start.add(player.getViewVector(1).scale(player.blockInteractionRange()));
            var hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                var part = pipe.findHitPart(start, end);
                if (part != null) {
                    pipe.removePart(part);
                    serverLevel.updateNeighborsAt(pos, this);
                    updateWrapNeighbors(serverLevel, pos, part);
                }
            }
        }

        // returning false keeps the block and skips the full-block destroy and harvest callbacks
        return false;
    }

    private void updateWrapNeighbors(ServerLevel level, BlockPos pos, PartPosition part) {
        var surface = PartPosition.ToDirection(part);
        if (surface == null) return;

        // ordinary neighbor updates reach the six adjacent blocks, so refresh wrap targets separately
        var support = pos.relative(surface);
        for (var direction : Direction.values()) {
            if (direction.getAxis() == surface.getAxis()) continue;

            var target = support.relative(direction);
            if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof PipeCoreBlockEntity pipe
                && pipe.hasPart(PartPosition.FromDirection(direction.getOpposite()))) {
                pipe.updateConnections();
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PipeCoreBlockEntity(blockPos, blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
