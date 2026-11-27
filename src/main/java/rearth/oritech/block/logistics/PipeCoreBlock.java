package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import rearth.oritech.init.ItemContent;

import java.util.function.Consumer;

public class PipeCoreBlock extends Block implements EntityBlock {

    public PipeCoreBlock(BlockBehaviour.Properties settings) {
        super(settings.dynamicShape().pushReaction(PushReaction.DESTROY));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe ? pipe.getPipeShape() : PipeCoreShape.CENTER;
    }

    public static boolean canToggleExtraction(Player player, BlockPos pos) {
        if (player.isSpectator() || player.isHolding(stack -> stack.is(ItemContent.WRENCH) || stack.getItem() instanceof PipeCoreBlockItem)) return false;

        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        return !player.isSecondaryUseActive() || (main.isEmpty() && off.isEmpty())
            || (main.doesSneakBypassUse(player.level(), pos, player) && off.doesSneakBypassUse(player.level(), pos, player));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!canToggleExtraction(player, pos) || !(level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe)) return InteractionResult.PASS;

        var target = pipe.findExtractionTarget(hit);
        if (target == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS_SERVER;
        if (!pipe.toggleExtraction(target.port())) return InteractionResult.PASS;

        level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.9f, 1.2f);
        return InteractionResult.SUCCESS_SERVER;
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
        // let vanilla remove the block and handle harvesting when the last part breaks
        if (!(level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) || !pipe.hasMultipleParts()) {
            return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
        }

        // the server chooses the part and syncs the remaining core to the client
        if (level instanceof ServerLevel serverLevel && player != null) {
            var start = player.getEyePosition();
            var end = start.add(player.getViewVector(1).scale(player.blockInteractionRange()));
            var hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                var part = pipe.findHitPart(start, end);
                if (part != null) {
                    pipe.removePart(part.kind(), part.position());
                    serverLevel.updateNeighborsAt(pos, this);
                    refreshNearbyPipes(serverLevel, pos);
                }
            }
        }

        // returning false keeps the block and skips the full-block destroy and harvest callbacks
        return false;
    }

    static void refreshNearbyPipes(ServerLevel level, BlockPos pos) {
        PipeNetworks.get(level).refreshNearby(level, pos);
    }

    static @Nullable PipeCoreBlockEntity getLoadedPipe(ServerLevel level, BlockPos pos) {
        var chunk = getLoadedChunk(level, pos);
        return chunk != null && chunk.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe ? pipe : null;
    }

    static @Nullable LevelChunk getLoadedChunk(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    static void forEachNearbyPipePosition(BlockPos pos, Consumer<BlockPos> action) {
        // straight connections reach face neighbors and outside wraps reach edge neighbors
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) > 2) continue;

                    action.accept(pos.offset(x, y, z));
                }
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PipeCoreBlockEntity(blockPos, blockState);
    }
}
