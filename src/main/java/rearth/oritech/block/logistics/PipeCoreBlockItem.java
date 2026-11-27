package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.logistics.PipeCore.PartPosition;
import rearth.oritech.block.logistics.PipeCore.PipeKind;
import rearth.oritech.block.logistics.PipeNetworks.Node;

import java.util.Map;

// this class is used to place items on either new blocks or update existing ones
public class PipeCoreBlockItem extends BlockItem {

    private final PipeKind kind;

    public PipeCoreBlockItem(Block block, PipeKind kind, Properties properties) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        // keep the item pipe as the default for the shared block
        if (kind == PipeKind.ITEM) {
            super.registerBlocks(map, item);
        }
    }

    // placement rules:
    // using this part on an empty block places and initializes a new pipe core with a surface or center part
    // clicking a support face that borders an existing core adds a surface part to that core
    // clicking the exposed top face of a ribbon adds a center part to that core
    // extending a floating pipe into an existing core works if that kind's center slot is free

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var placementContext = new BlockPlaceContext(context);
        return place(placementContext, findPlacement(placementContext, context.getClickedPos()));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return place(context, findPlacement(context, null));
    }

    private record Placement(BlockPos pos, PartPosition part, @Nullable PipeCoreBlockEntity pipe) {}

    public @Nullable Node getPlacementTarget(UseOnContext context) {
        var placement = findPlacement(new BlockPlaceContext(context), context.getClickedPos());
        return placement == null ? null : new Node(placement.pos(), kind, placement.part());
    }

    private @Nullable Placement findPlacement(BlockPlaceContext context, @Nullable BlockPos hitPos) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (context.getItemInHand().isEmpty() || !getBlock().isEnabled(level.enabledFeatures())) return null;
        if (hitPos != null && player != null && (player.isSpectator() || player.getCooldowns().isOnCooldown(context.getItemInHand())
            || !player.getAbilities().mayBuild && !context.getItemInHand().canPlaceOnBlockInAdventureMode(new BlockInWorld(level, hitPos, false)))) {
            return null;
        }

        // Use the original hit for a ribbon top, before the normal placement context offsets it.
        if (hitPos != null && level.getBlockEntity(hitPos) instanceof PipeCoreBlockEntity pipe) {
            var localHit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(hitPos));
            if (pipe.isRibbonTop(context.getClickedFace().getOpposite(), localHit)) {
                return existingPartPlacement(context, hitPos, pipe, PartPosition.CENTER);
            }
        }

        var pos = context.getClickedPos();
        var part = getTargetPosition(context);
        if (level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) {
            return existingPartPlacement(context, pos, pipe, part);
        }

        return canInteract(context, pos) && context.canPlace() && getPlacementState(context) != null
            ? new Placement(pos, part, null) : null;
    }

    private @Nullable Placement existingPartPlacement(BlockPlaceContext context, BlockPos pos, PipeCoreBlockEntity pipe, PartPosition part) {
        return !pipe.hasPart(kind, part) && canInteract(context, pos) && canPlacePart(context, pos, part)
            ? new Placement(pos, part, pipe) : null;
    }

    private boolean canInteract(BlockPlaceContext context, BlockPos pos) {
        var level = context.getLevel();
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
            && (context.getPlayer() == null || level.mayInteract(context.getPlayer(), pos));
    }

    private InteractionResult place(BlockPlaceContext context, @Nullable Placement placement) {
        if (placement == null) return InteractionResult.FAIL;
        var pipe = placement.pipe();
        if (pipe == null) return super.place(context);

        var pos = placement.pos();
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level instanceof ServerLevel serverLevel) {
            if (!pipe.addPart(kind, placement.part())) {
                return InteractionResult.FAIL;
            }

            var state = pipe.getBlockState();
            // the core's block state stays the same, so send the usual neighbor update explicitly
            serverLevel.updateNeighborsAt(pos, state.getBlock());
            PipeCoreBlock.refreshNearbyPipes(serverLevel, pos);

            var sound = state.getSoundType(level, pos, player);
            level.playSound(null, pos, getPlaceSound(state, level, pos, player), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            context.getItemInHand().consume(1, player);
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        var pos = context.getClickedPos();
        return (!mustSurvive() || state.canSurvive(context.getLevel(), pos))
            && canPlacePart(context, pos, getTargetPosition(context));
    }

    // vanilla handles placing the block and consuming the item for the first part
    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState placementState) {
        var part = getTargetPosition(context);
        if (!super.placeBlock(context, placementState)) {
            return false;
        }

        var pos = context.getClickedPos();
        if (context.getLevel() instanceof ServerLevel level && level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe
            && pipe.addPart(kind, part)) {
            // NeoForge defers the placement update while capturing, so it will already see the first part
            if (!level.captureBlockSnapshots) {
                level.updateNeighborsAt(pos, placementState.getBlock());
            }

            // wait until placement can no longer be rolled back by the place event
            var server = level.getServer();
            server.schedule(server.wrapRunnable(() -> {
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) == pipe) {
                    PipeCoreBlock.refreshNearbyPipes(level, pos);
                }
            }));
        }

        return true;
    }

    private boolean canPlacePart(UseOnContext context, BlockPos pos, PartPosition part) {
        return context.getLevel().isUnobstructed(null, PipeCoreShape.partShape(kind, part).move(pos));
    }

    private PartPosition getTargetPosition(BlockPlaceContext context) {
        // the backing block lies opposite the clicked face
        var level = context.getLevel();
        var face = context.getClickedFace();
        var supportDirection = face.getOpposite();
        var supportPos = context.getClickedPos().relative(supportDirection);
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, face)
            ? PartPosition.FromDirection(supportDirection)
            : PartPosition.CENTER;
    }
}
