package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.decorative.HangarDoorBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.SoundContent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HangarDoorBlock extends Block implements EntityBlock {

    public static final DirectionProperty SURFACE = BlockStateProperties.FACING;
    public static final BooleanProperty OPENED = BooleanProperty.create("open");
    public static final BooleanProperty ROTATED = BooleanProperty.create("rotated");

    private static final VoxelShape FULL_BLOCK_SHAPE = Shapes.block();
    private static final VoxelShape SUPPORT_SHAPE_Z = Block.box(0, 0, 3, 16, 16, 13);    // for vertical rotated, facing north-south (e.g. Z axis)
    private static final VoxelShape SUPPORT_SHAPE_X = Block.box(3, 0, 0, 13, 16, 16);    // for vertical non-rotated, facing east-west (e.g. X axis)
    private static final VoxelShape SUPPORT_SHAPE_Y = Block.box(0, 3, 0, 16, 13, 16);    // for horizontal non-rotated, facing up-down (e.g. Y axis)

    public HangarDoorBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(SURFACE, Direction.NORTH).setValue(OPENED, false).setValue(ROTATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SURFACE, OPENED, ROTATED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var surface = ctx.getClickedFace();
        var rotated = chooseRotated(surface, ctx);
        var state = defaultBlockState().setValue(SURFACE, surface).setValue(ROTATED, rotated);
        return canPlaceDoor(ctx.getLevel(), ctx.getClickedPos(), state) ? state : null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (world.isClientSide) return;

        // create segment blocks
        var segmentDirection = getSegmentDirection(state);
        for (int part = 1; part <= 2; part++) {
            world.setBlockAndUpdate(pos.relative(segmentDirection, part), BlockContent.HANGAR_DOOR_HELPER.defaultBlockState()
              .setValue(HangarDoorHelperBlock.PART, part)
              .setValue(SURFACE, state.getValue(SURFACE))
              .setValue(OPENED, state.getValue(OPENED))
              .setValue(ROTATED, state.getValue(ROTATED)));
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);

        if (world.isClientSide) return;
        updateDoorState(world, pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return FULL_BLOCK_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return FULL_BLOCK_SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide) {
            removeHelpers(world, pos, state);
        }

        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !world.isClientSide) {
            removeHelpers(world, pos, state);
        }

        super.onRemove(state, world, pos, newState, movedByPiston);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HangarDoorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        tooltip.add(Component.translatable("tooltip.oritech.hangar_door.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.oritech.hangar_door.2").withStyle(ChatFormatting.GRAY));
    }

    public static VoxelShape getClosedShape(Direction surface, boolean rotated) {
        
        // on floor/ceiling
        if (surface.getAxis().isVertical()) {
            return rotated ? SUPPORT_SHAPE_X : SUPPORT_SHAPE_Z;
        }
        
        // rotated on walls always points up
        if (!rotated) return SUPPORT_SHAPE_Y;
        
        return switch (surface) {
            case NORTH, SOUTH -> SUPPORT_SHAPE_X;
            case EAST, WEST -> SUPPORT_SHAPE_Z;
            default -> null;    // should never happen
        };
    }

    public static Direction getSegmentDirection(BlockState state) {
        return getSegmentDirection(state.getValue(SURFACE), state.getValue(ROTATED));
    }

    public static Direction getSegmentDirection(Direction surface, boolean rotated) {
        return surface;
    }

    public static BlockPos getAnchorPos(BlockPos helperPos, BlockState helperState) {
        return helperPos.relative(getSegmentDirection(helperState), -helperState.getValue(HangarDoorHelperBlock.PART));
    }

    public static boolean isStructureValid(LevelReader world, BlockPos anchorPos, BlockState anchorState) {
        var surface = anchorState.getValue(SURFACE);
        var opened = anchorState.getValue(OPENED);
        var rotated = anchorState.getValue(ROTATED);
        var segmentDirection = getSegmentDirection(anchorState);

        for (int part = 1; part <= 2; part++) {
            var helperPos = anchorPos.relative(segmentDirection, part);
            var helperState = world.getBlockState(helperPos);
            if (!helperState.is(BlockContent.HANGAR_DOOR_HELPER)) return false;
            if (helperState.getValue(HangarDoorHelperBlock.PART) != part) return false;
            if (helperState.getValue(SURFACE) != surface) return false;
            if (helperState.getValue(OPENED) != opened) return false;
            if (helperState.getValue(ROTATED) != rotated) return false;
        }

        return true;
    }

    public static void updateDoorState(Level world, BlockPos anchorPos, BlockState anchorState) {
        if (!anchorState.is(BlockContent.HANGAR_DOOR)) return;

        if (!isStructureValid(world, anchorPos, anchorState)) {
            return;
        }

        var connectedAnchors = collectConnectedAnchors(world, anchorPos);
        var powered = isAnyDoorPowered(world, connectedAnchors);

        if (powered == anchorState.getValue(OPENED)) return;

        var blockEntity = world.getBlockEntity(anchorPos);
        if (blockEntity instanceof HangarDoorBlockEntity hangarDoor && hangarDoor.shouldPlaySoundAgain()) {
            world.playSound(null, anchorPos, SoundContent.PRESS, SoundSource.BLOCKS, OritechConfig.machineVolumeMultiplier.get().floatValue() * 0.18f, 1.15f);
        }

        for (var connectedAnchorPos : connectedAnchors) {
            var connectedAnchorState = world.getBlockState(connectedAnchorPos);
            if (!connectedAnchorState.is(BlockContent.HANGAR_DOOR) || !isStructureValid(world, connectedAnchorPos, connectedAnchorState)) {
                continue;
            }

            setDoorOpenState(world, connectedAnchorPos, connectedAnchorState, powered);
        }
    }

    private static Set<BlockPos> collectConnectedAnchors(Level world, BlockPos startPos) {
        var connectedAnchors = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(startPos);

        while (!queue.isEmpty()) {
            var currentPos = queue.removeFirst();
            if (!connectedAnchors.add(currentPos)) continue;

            for (var direction : Direction.values()) {
                var neighborPos = currentPos.relative(direction);
                var neighborState = world.getBlockState(neighborPos);
                if (neighborState.is(BlockContent.HANGAR_DOOR)) {
                    queue.add(neighborPos);
                }
            }
        }

        return connectedAnchors;
    }

    private static boolean isAnyDoorPowered(Level world, Set<BlockPos> connectedAnchors) {
        for (var anchorPos : connectedAnchors) {
            var anchorState = world.getBlockState(anchorPos);
            if (!anchorState.is(BlockContent.HANGAR_DOOR) || !isStructureValid(world, anchorPos, anchorState)) {
                continue;
            }

            if (world.hasNeighborSignal(anchorPos)) {
                return true;
            }
        }

        return false;
    }

    private static void setDoorOpenState(Level world, BlockPos anchorPos, BlockState anchorState, boolean opened) {
        world.setBlockAndUpdate(anchorPos, anchorState.setValue(OPENED, opened));

        var segmentDirection = getSegmentDirection(anchorState);
        for (int part = 1; part <= 2; part++) {
            var helperPos = anchorPos.relative(segmentDirection, part);
            var helperState = world.getBlockState(helperPos);
            if (helperState.is(BlockContent.HANGAR_DOOR_HELPER)) {
                world.setBlockAndUpdate(helperPos, helperState.setValue(OPENED, opened));
            }
        }
    }
    
    public static void removeHelpers(Level world, BlockPos anchorPos, BlockState anchorState) {
        var segmentDirection = getSegmentDirection(anchorState);
        for (int part = 1; part <= 2; part++) {
            var helperPos = anchorPos.relative(segmentDirection, part);
            if (world.getBlockState(helperPos).is(BlockContent.HANGAR_DOOR_HELPER)) {
                world.setBlock(helperPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
    
    public static void removeFullStructure(Level world, BlockPos anchorPos, BlockState anchorState, boolean dropAnchor) {
        removeHelpers(world, anchorPos, anchorState);
        
        if (dropAnchor) {
            Block.dropResources(anchorState, world, anchorPos, world.getBlockEntity(anchorPos));
        }
        
        world.setBlock(anchorPos, Blocks.AIR.defaultBlockState(), 3);
    }

    private boolean canPlaceDoor(LevelReader world, BlockPos anchorPos, BlockState anchorState) {
        var segmentDirection = getSegmentDirection(anchorState);
        for (int part = 1; part <= 2; part++) {
            var candidateState = world.getBlockState(anchorPos.relative(segmentDirection, part));
            if (!candidateState.canBeReplaced()) return false;
        }

        return true;
    }

    private static boolean chooseRotated(Direction surface, BlockPlaceContext ctx) {
        var player = ctx.getPlayer();
        if (player == null) {
            return surface.getAxis().isVertical() && ctx.getHorizontalDirection().getAxis() == Direction.Axis.X;
        }

        var horizontalAxis = ctx.getHorizontalDirection().getAxis();

        // on floors or ceilings
        if (surface.getAxis().isVertical()) {
            return horizontalAxis == Direction.Axis.X;
        }

        // on walls
        var look = player.getLookAngle();
        var horizontalStrength = Math.max(Math.abs(look.x), Math.abs(look.z));
        return Math.abs(look.y) < horizontalStrength;
    }
}