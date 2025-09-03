package rearth.oritech.block.blocks.storage;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.ComparatorOutputProvider;
import rearth.oritech.util.StackContext;

import java.util.List;

public class SmallFluidTank extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    
    public SmallFluidTank(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmallTankEntity(pos, state, false);
    }
    
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
    
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        return ((ComparatorOutputProvider) world.getBlockEntity(pos)).getComparatorOutput();
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SmallTankEntity tankEntity) {
            var usedStack = stack;
            if (stack.getCount() > 1) {
                usedStack = stack.copyWithCount(1);
            }
            var stackRef = new StackContext(usedStack, updated -> {
                if (stack.getCount() > 1) {
                    stack.shrink(1);
                    if (!player.getInventory().add(updated)) {
                        player.drop(updated, true);
                    }
                } else {
                    player.setItemInHand(hand, updated);
                }
            });
            
            var candidate = FluidApi.ITEM.find(stackRef);
            if (candidate != null) {
                
                if (!world.isClientSide) {
                    if (candidate.getContent().getFirst().isEmpty()) { // from tank to item
                        var moved = FluidApi.transferFirst(tankEntity.fluidStorage, candidate, tankEntity.fluidStorage.getCapacity(), false);
                        Oritech.LOGGER.debug("moved to item {} {}", moved, stackRef.getValue());
                    } else {    // from item to tank
                        var moved = FluidApi.transferFirst(candidate, tankEntity.fluidStorage, tankEntity.fluidStorage.getCapacity(), false);
                        Oritech.LOGGER.debug("moved from item {} {}", moved, stackRef.getValue());
                    }
                }
                
                world.playLocalSound(pos, SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 0.8f, 1.4f, true);
                
                return ItemInteractionResult.sidedSuccess(true);
            }
        }
        
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }
    
    @Override
    protected @NotNull List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var droppedStacks = super.getDrops(state, builder);
        
        var blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof SmallTankEntity tankEntity) {
            droppedStacks.addAll(tankEntity.inventory.getHeldStacks());
            tankEntity.inventory.clearContent();
        }
        
        return droppedStacks;
    }
    
    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state) {
        return getStackWithData(world, pos);
    }
    
    @NotNull
    private static ItemStack getStackWithData(LevelReader world, BlockPos pos) {
        var tankEntity = (SmallTankEntity) world.getBlockEntity(pos);
        var stack = getBasePickStack(tankEntity.isCreative);
        
        if (tankEntity.fluidStorage.getAmount() > 0) {
            var fluidStack = tankEntity.fluidStorage.getStack().copy();
            stack.set(FluidApi.ITEM.getFluidComponent(), fluidStack);
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }
        
        return stack;
    }
    
    public static ItemStack getBasePickStack(boolean creative) {
        return new ItemStack(creative ? BlockContent.CREATIVE_TANK_BLOCK.asItem() : BlockContent.SMALL_TANK_BLOCK.asItem());
    }
    
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        
        if (itemStack.has(FluidApi.ITEM.getFluidComponent())) {
            var tankEntity = (SmallTankEntity) world.getBlockEntity(pos);
            tankEntity.fluidStorage.setStack(itemStack.get(FluidApi.ITEM.getFluidComponent()).copy());
        }
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
    
}
