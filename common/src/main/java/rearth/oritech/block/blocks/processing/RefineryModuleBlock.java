package rearth.oritech.block.blocks.processing;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.fluid.ItemFluidApi;
import rearth.oritech.block.entity.processing.RefineryModuleBlockEntity;

import rearth.oritech.util.MultiblockMachineController;

import java.util.List;
import java.util.Objects;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.util.TooltipHelper.addMachineTooltip;


public class RefineryModuleBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public RefineryModuleBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(ASSEMBLED, false));
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING, ASSEMBLED);
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
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof MultiblockMachineController machineEntity)) {
                return InteractionResult.SUCCESS;
            }
            
            var wasAssembled = state.getValue(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = machineEntity.tryPlaceNextCore(player);
                if (corePlaced) return InteractionResult.SUCCESS;
            }
            
            var isAssembled = machineEntity.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                machineEntity.triggerSetupAnimation();
                return InteractionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
                return InteractionResult.SUCCESS;
            }
            
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        
        if (ItemFluidApi.tryFluidBlockItemInteraction(stack, world, pos, player, hand)) return ItemInteractionResult.sidedSuccess(true);
        
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide() && state.getValue(ASSEMBLED)) {
            
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefineryModuleBlockEntity(pos, state);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        tooltip.add(Component.translatable("tooltip.oritech.refinery_module").withStyle(ChatFormatting.GRAY));
        addMachineTooltip(tooltip, this, this);
    }
}
