package rearth.oritech.block.blocks.interaction;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.interaction.DeepDrillEntity;

import rearth.oritech.util.MultiblockMachineController;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.util.TooltipHelper.addMachineTooltip;


public class DeepDrillBlock extends Block implements EntityBlock {
    
    public DeepDrillBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLED).add(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof DeepDrillEntity deepDrill)) {
                return InteractionResult.SUCCESS;
            }
            
            var wasAssembled = state.getValue(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = deepDrill.tryPlaceNextCore(player);
                if (corePlaced) return InteractionResult.SUCCESS;
            }
            
            var isAssembled = deepDrill.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                deepDrill.triggerSetupAnimation();
                return InteractionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
                return InteractionResult.SUCCESS;
            }
            
            if (!deepDrill.init(true)) {
                player.sendSystemMessage(Component.translatable("message.oritech.deep_drill.ore_placement"));
                return InteractionResult.SUCCESS;
            }
            
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide() && state.getValue(ASSEMBLED)) {
            
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
            
            if (entity instanceof DeepDrillEntity storageBlock) {
                var stacks = storageBlock.inventory.heldStacks;
                for (var heldStack : stacks) {
                    if (!heldStack.isEmpty()) {
                        var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), heldStack);
                        world.addFreshEntity(itemEntity);
                    }
                }
                
                storageBlock.inventory.heldStacks.clear();
                storageBlock.inventory.setChanged();
            }
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeepDrillEntity(pos, state);
    }
    
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
        addMachineTooltip(tooltip, this, this);
    }
}
