package rearth.oritech.block.base.block;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.ItemFluidApi;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.processing.PulverizerBlockEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public abstract class MachineBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public MachineBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        
        try {
            return getBlockEntityType().getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(pos, state);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Oritech.LOGGER.error("Unable to create blockEntity for " + getBlockEntityType().getSimpleName() + " at " + this);
            return new PulverizerBlockEntity(pos, state);
        }
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        onBlockRemoved(world, pos);
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    protected void onExplosionHit(BlockState state, Level world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
        onBlockRemoved(world, pos);
        super.onExplosionHit(state, world, pos, explosion, stackMerger);
    }
    
    private static void onBlockRemoved(Level world, BlockPos pos) {
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof MachineBlockEntity entity) {
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
    }
    
    @NotNull
    public abstract Class<? extends BlockEntity> getBlockEntityType();
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide) {
            var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        
        if (ItemFluidApi.tryFluidBlockItemInteraction(stack, world, pos, player, hand)) return ItemInteractionResult.sidedSuccess(true);
        
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        addMachineTooltip(tooltip, this, this);
    }
}