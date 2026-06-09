package rearth.oritech.block.blocks.pipes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.util.Geometry;

import java.util.function.Consumer;

// how this block works:
// points to block/storage that player was facing when placing (e.g. similar to addons)
// connects via pipes to other nearby inventories
// has a GUI to configure filter options (n amount of slots available, and some buttons to filter based on metadata)
// filter options: whitelist/blacklist, ignore damage, ignore nbt
public class ItemFilterBlock extends Block implements EntityBlock, TooltipProvider {

    private static final VoxelShape[] BOUNDING_SHAPES;

    public ItemFilterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).setValue(BlockStateProperties.FACING, ctx.getClickedFace().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemFilterBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            player.openMenu((MenuProvider) level.getBlockEntity(pos), pos);
        }

        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("rawtypes")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        consumer.accept(Component.translatable("tooltip.oritech.item_filter").withStyle(ChatFormatting.GRAY));

//  todo
//        if (Platform.isModLoaded("ftbfiltersystem")) {
//            consumer.accept(Component.translatable("tooltip.oritech.item_filter_ftb").withStyle(ChatFormatting.GRAY));
//        }

    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BOUNDING_SHAPES[state.getValue(BlockStateProperties.FACING).get3DDataValue()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    static {
        BOUNDING_SHAPES = new VoxelShape[Direction.values().length];
        for (var facing : Direction.values()) {
            BOUNDING_SHAPES[facing.ordinal()] = Shapes.or(
                    Geometry.rotateVoxelShape(Shapes.box(0.25, 0.25, -0.00375, 0.75, 0.75, 0.125), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0, 0.375, 0.375, 0.125, 0.625, 0.625), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.375, 0.875, 0.375, 0.625, 1, 0.625), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.875, 0.375, 0.375, 1, 0.625, 0.625), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.375, 0, 0.375, 0.625, 0.125, 0.625), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.375, 0.375, 0.875, 0.625, 0.625, 1), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.1875, -0.03125, 0.5625, 0.8125, 0.1875), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.1875, 0.4375, -0.03125, 0.8125, 0.5625, 0.1875), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.125, 0.4375, 0.4375, 0.875, 0.5625, 0.5625), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.125, 0.4375, 0.5625, 0.875, 0.5625), facing, AttachFace.FLOOR),
                    Geometry.rotateVoxelShape(Shapes.box(0.4375, 0.4375, 0.8125, 0.5625, 0.5625, 0.875), facing, AttachFace.FLOOR));
        }
    }
}
