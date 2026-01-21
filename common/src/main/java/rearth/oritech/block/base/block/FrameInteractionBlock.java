package rearth.oritech.block.base.block;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.util.MachineAddonController;

import java.util.List;
import java.util.Objects;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;


public abstract class FrameInteractionBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public static final BooleanProperty HAS_FRAME = BooleanProperty.create("has_frame");
    
    public FrameInteractionBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(HAS_FRAME, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, HAS_FRAME);
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
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof FrameInteractionBlockEntity machineEntity)) {
                return InteractionResult.SUCCESS;
            }
            
            var frameValid = machineEntity.tryFindFrame();
            world.setBlockAndUpdate(pos, state.setValue(HAS_FRAME, frameValid));
            
            if (frameValid) {
                if (entity instanceof MachineAddonController addonController)
                    addonController.initAddons();
                
                var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
            } else {
                player.sendSystemMessage(Component.translatable("message.oritech.machine_frame.missing_frame"));
            }
            
        }
        return InteractionResult.SUCCESS;
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
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide() && state.getValue(HAS_FRAME)) {
            
            var ownEntity = (FrameInteractionBlockEntity) world.getBlockEntity(pos);
            ownEntity.cleanup();
            
            if (ownEntity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
            
            if (ownEntity instanceof ItemEnergyFrameInteractionBlockEntity itemContainer) {
                var stacks = itemContainer.inventory.heldStacks;
                for (var stack : stacks) {
                    if (!stack.isEmpty()) {
                        var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                        world.addFreshEntity(itemEntity);
                    }
                }
                
                itemContainer.inventory.heldStacks.clear();
                itemContainer.inventory.setChanged();
            }
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        addMachineTooltip(tooltip, this, this);
    }
}
