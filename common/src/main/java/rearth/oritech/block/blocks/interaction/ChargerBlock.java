package rearth.oritech.block.blocks.interaction;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.interaction.ChargerBlockEntity;

import java.util.List;
import java.util.Objects;

public class ChargerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public ChargerBlock(Properties settings) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 8, 16);
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargerBlockEntity(pos, state);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof ChargerBlockEntity machine) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, machine);
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide) {
            var entity = (ChargerBlockEntity) world.getBlockEntity(pos);
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        var showExtra = Screen.hasControlDown();
        if (!showExtra) {
            tooltip.add(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.charger").withStyle(ChatFormatting.GRAY));
        }
    }
}
