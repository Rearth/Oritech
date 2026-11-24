package rearth.oritech.block.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.block.logistics.PipeCore.PartPosition;

// this class is used to place items on either new blocks or update existing ones
public class PipeCoreBlockItem extends BlockItem {

    public PipeCoreBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    // placement rules:
    // using this part on an empty block places and initializes a new pipe core with a surface or center part
    // clicking a support face that borders an existing core adds a surface part to that core
    // clicking the exposed top face of an installed ribbon adds a center part to that core

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // use the actual hit before BlockPlaceContext offsets it to the adjacent block
        var clickedPos = context.getClickedPos();
        var face = context.getClickedFace();
        if (context.getLevel().getBlockEntity(clickedPos) instanceof PipeCoreBlockEntity pipe
            && pipe.hasPart(PartPosition.FromDirection(face.getOpposite()))
            && PipeCoreShape.isRibbonTop(face, context.getClickLocation().subtract(Vec3.atLowerCornerOf(clickedPos)))) {
            return addPart(context, clickedPos, pipe, PartPosition.CENTER);
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        // a click on a supporting face can aim into a block that already contains a core
        var pos = context.getClickedPos();
        if (context.getLevel().getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) {
            var part = getTargetPosition(context);
            return part == PartPosition.CENTER ? InteractionResult.FAIL : addPart(context, pos, pipe, part);
        }

        return super.place(context);
    }

    private InteractionResult addPart(UseOnContext context, BlockPos pos, PipeCoreBlockEntity pipe, PartPosition part) {
        var level = context.getLevel();
        var player = context.getPlayer();

        // reject occupied slots and check the new part's own hitbox for entity collisions
        if (pipe.hasPart(part) || !getBlock().isEnabled(level.enabledFeatures())
            || !level.mayInteract(player, pos) || !canPlacePart(context, pos, part)) {
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            if (!pipe.addPart(part)) {
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
            && pipe.addPart(part)) {
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
        return context.getLevel().isUnobstructed(null, PipeCoreShape.partShape(part).move(pos));
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
