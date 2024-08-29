package rearth.oritech.block.blocks.machines.interaction;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.interaction.TreefellerBlockEntity;
import rearth.oritech.util.Geometry;

import java.util.List;
import java.util.Objects;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public class TreefellerBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    private static final VoxelShape[] BOUNDING_SHAPES;
    
    public TreefellerBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TreefellerBlockEntity(pos, state);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient) {
            var entity = (TreefellerBlockEntity) world.getBlockEntity(pos);
            var stacks = entity.inventory.heldStacks;
            for (var stack : stacks) {
                if (!stack.isEmpty()) {
                    var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                    world.spawnEntity(itemEntity);
                }
            }
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
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
        super.appendTooltip(stack, context, tooltip, options);
        addMachineTooltip(tooltip, this, this);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BOUNDING_SHAPES[state.get(FACING).ordinal()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    static {
        BOUNDING_SHAPES = new VoxelShape[Direction.values().length];
        for (var facing : Direction.values()) {
            if (!facing.getAxis().isHorizontal()) {
                BOUNDING_SHAPES[facing.ordinal()] = VoxelShapes.fullCube();
            } else {
                BOUNDING_SHAPES[facing.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.9375), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.875, 0.3125, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0.3125, 0.0625, 0.9375, 0.4375, 0.9375), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.4375, 0.125, 0.875, 0.5625, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0.625, 0.0625, 0.9375, 0.75, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.5625, 0.125, 0.875, 0.625, 0.6875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0, 0.375, 0.1875, 0.8125, 0.625), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.8125, 0, 0.375, 1, 0.8125, 0.625), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0, 0.875, 0.625, 0.5, 1), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0, 0, 0.625, 0.8125, 0.1875), facing, BlockFace.FLOOR));
            }
        }
    }
}
