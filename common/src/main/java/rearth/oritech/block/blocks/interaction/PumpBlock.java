package rearth.oritech.block.blocks.interaction;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.interaction.PumpBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;


public class PumpBlock extends Block implements EntityBlock {
    
    public PumpBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpBlockEntity(pos, state);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            var pumpEntity = world.getBlockEntity(pos, BlockEntitiesContent.PUMP_BLOCK);
            pumpEntity.ifPresent(pumpBlockEntity -> pumpBlockEntity.onUsed(player));
        }
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide) {
            // break all trunk blocks below
            var checkPos = pos.below();
            while (world.getBlockState(checkPos).getBlock().equals(BlockContent.PUMP_TRUNK_BLOCK)) {
                world.destroyBlock(checkPos, false);
                checkPos = checkPos.below();
            }
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
        super.appendHoverText(stack, context, tooltip, options);
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            tooltip.add(Component.translatable("tooltip.oritech.pump_redstone"));
        }
        addMachineTooltip(tooltip, this, this);
    }
}
