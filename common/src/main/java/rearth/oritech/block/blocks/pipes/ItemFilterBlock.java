package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.util.Geometry;

import java.util.List;

// how this block works:
// points to block/storage that player was facing when placing (e.g. similar to addons)
// connects via pipes to other nearby inventories
// has a GUI to configure filter options (n amount of slots available, and some buttons to filter based on metadata)
// filter options: whitelist/blacklist, ignore damage, ignore nbt
public class ItemFilterBlock extends Block implements BlockEntityProvider {
    
    public static final DirectionProperty TARGET_DIR = DirectionProperty.of("target_dir");

    private static final VoxelShape[] BOUNDING_SHAPES;
    
    public ItemFilterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getDefaultState().with(TARGET_DIR, Direction.NORTH));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(TARGET_DIR);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(TARGET_DIR, ctx.getSide().getOpposite());
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemFilterBlockEntity(pos, state);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
        }
        
        return ActionResult.SUCCESS;
    }
    
    @SuppressWarnings("rawtypes")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.oritech.item_filter").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, options);
    }

        @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BOUNDING_SHAPES[state.get(TARGET_DIR).getId()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    static {
        BOUNDING_SHAPES = new VoxelShape[Direction.values().length];
        for (var facing : Direction.values()) {
            BOUNDING_SHAPES[facing.ordinal()] = VoxelShapes.union(
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.25, -0.00375, 0.75, 0.75, 0.125), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.375, 0.375, 0.125, 0.625, 0.625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.875, 0.375, 0.625, 1, 0.625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.875, 0.375, 0.375, 1, 0.625, 0.625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0, 0.375, 0.625, 0.125, 0.625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.375, 0.875, 0.625, 0.625, 1), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.1875, -0.03125, 0.5625, 0.8125, 0.1875), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.4375, -0.03125, 0.8125, 0.5625, 0.1875), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.4375, 0.4375, 0.875, 0.5625, 0.5625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.125, 0.4375, 0.5625, 0.875, 0.5625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.4375, 0.4375, 0.8125, 0.5625, 0.5625, 0.875), facing, BlockFace.FLOOR));
        }
    }
}
