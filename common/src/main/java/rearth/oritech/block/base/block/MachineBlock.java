package rearth.oritech.block.base.block;

import com.mojang.serialization.MapCodec;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
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
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.processing.PulverizerBlockEntity;
import rearth.oritech.util.StackContext;
import rearth.oritech.util.fluid.FluidApi;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public abstract class MachineBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    
    public MachineBlock(Settings settings) {
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
        
        try {
            return getBlockEntityType().getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(pos, state);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Oritech.LOGGER.error("Unable to create blockEntity for " + getBlockEntityType().getSimpleName() + " at " + this);
            return new PulverizerBlockEntity(pos, state);
        }
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        onBlockRemoved(world, pos);
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    protected void onExploded(BlockState state, World world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
        onBlockRemoved(world, pos);
        super.onExploded(state, world, pos, explosion, stackMerger);
    }
    
    private static void onBlockRemoved(World world, BlockPos pos) {
        if (!world.isClient) {
            var entity = (MachineBlockEntity) world.getBlockEntity(pos);
            var stacks = entity.inventory.heldStacks;
            for (var stack : stacks) {
                if (!stack.isEmpty()) {
                    var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                    world.spawnEntity(itemEntity);
                }
            }
        }
    }
    
    @NotNull
    public abstract Class<? extends BlockEntity> getBlockEntityType();
    
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FluidApi.BlockProvider tankEntity) {
            var usedStack = stack;
            if (stack.getCount() > 1) {
                usedStack = stack.copyWithCount(1);
            }
            var stackRef = new StackContext(usedStack, updated -> {
                if (stack.getCount() > 1) {
                    stack.decrement(1);
                    if (!player.getInventory().insertStack(updated)) {
                        player.dropItem(updated, true);
                    }
                } else {
                    player.setStackInHand(hand, updated);
                }
            });
            
            var candidate = FluidApi.ITEM.find(stackRef);
            if (candidate != null) {
                
                var fluidStorage = tankEntity.getFluidStorage(null);
                
                if (!world.isClient) {
                    if (candidate.getContent().getFirst().isEmpty()) { // from tank to item
                        var moved = FluidApi.transferFirst(fluidStorage, candidate, FluidStackHooks.bucketAmount() * 8, false);
                        if (moved == 0) {   // attempt last if first did not work
                            moved = FluidApi.transferLast(fluidStorage, candidate, FluidStackHooks.bucketAmount() * 8, false);
                        }
                        Oritech.LOGGER.debug("moved to item {} {}", moved, stackRef.getValue());
                    } else {    // from item to tank
                        var moved = FluidApi.transferFirst(candidate, fluidStorage, FluidStackHooks.bucketAmount() * 8, false);
                        Oritech.LOGGER.debug("moved from item {} {}", moved, stackRef.getValue());
                    }
                }
                
                return ItemActionResult.success(true);
            }
        }
        
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        addMachineTooltip(tooltip, this, this);
    }
}