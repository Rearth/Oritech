package rearth.oritech.block.blocks.pipes.item;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.util.Geometry;

import java.util.List;

// how this block works:
// points to block/storage that player was facing when placing (e.g. similar to addons)
// connects via pipes to other nearby inventories
// has a GUI to configure filter options (n amount of slots available, and some buttons to filter based on metadata)
// filter options: whitelist/blacklist, ignore damage, ignore nbt
public class ItemFilterBlock extends Block implements EntityBlock {
    
    public static final DirectionProperty TARGET_DIR = DirectionProperty.create("target_dir");

    private static final VoxelShape[] BOUNDING_SHAPES;
    
    public ItemFilterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(TARGET_DIR, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TARGET_DIR);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).setValue(TARGET_DIR, ctx.getClickedFace().getOpposite());
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemFilterBlockEntity(pos, state);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @SuppressWarnings("rawtypes")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        tooltip.add(Component.translatable("tooltip.oritech.item_filter").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, options);
    }

        @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return BOUNDING_SHAPES[state.getValue(TARGET_DIR).get3DDataValue()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide) {
            var entity = (ItemFilterBlockEntity) world.getBlockEntity(pos);
            var stacks = entity.inventory.heldStacks;
            for (var stack : stacks) {
                if (!stack.isEmpty()) {
                    var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                    world.addFreshEntity(itemEntity);
                }
            }
            
            entity.inventory.heldStacks.clear();
            entity.inventory.setChanged();
        }
        
        return super.playerWillDestroy(world, pos, state, player);
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
