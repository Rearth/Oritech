package rearth.oritech.block.blocks.storage;

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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.ComparatorOutputProvider;

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
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return ((ComparatorOutputProvider) level.getBlockEntity(pos)).getComparatorOutput();
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!level.isClientSide()) {
            var handler = (ExtendedMenuProvider) level.getBlockEntity(pos);
            MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
            
        }
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SmallTankEntity tankEntity) {
            var usedStack = stack;
            if (stack.getCount() > 1) {
                usedStack = stack.copyWithCount(1);
            }
            
            var candidate = usedStack.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(usedStack));
            if (candidate != null) {
                if (!level.isClientSide()) {
                    int moved = 0;
                    
                    try (var transaction = Transaction.openRoot()) {
                        var itemResource = candidate.getResource(0);
                        if (itemResource.isEmpty()) { // from tank to item
                            var tankResource = tankEntity.fluidStorage.getResource(0);
                            if (!tankResource.isEmpty()) {
                                var inserted = candidate.insert(tankResource, tankEntity.fluidStorage.getAmount(), transaction);
                                if (inserted > 0) {
                                    var extracted = tankEntity.fluidStorage.extract(0, tankResource, inserted, transaction);
                                    if (extracted == inserted) {
                                        transaction.commit();
                                        moved = inserted;
                                    }
                                }
                            }
                        } else {    // from item to tank
                            var maxTaken = Math.min(candidate.getAmountAsLong(0), tankEntity.fluidStorage.getCapacity() - tankEntity.fluidStorage.getAmount());
                            var taken = candidate.extract(0, itemResource, (int) maxTaken, transaction);
                            if (taken > 0) {
                                var inserted = tankEntity.fluidStorage.insert(itemResource, taken, transaction);
                                if (inserted == taken) {
                                    transaction.commit();
                                    moved = taken;
                                }
                            }
                        }
                    }
                    
                    if (moved > 0) {
                        if (stack.getCount() > 1) {
                            stack.shrink(1);
                            if (!player.getInventory().add(usedStack)) {
                                player.drop(usedStack, true);
                            }
                        } else {
                            player.setItemInHand(hand, usedStack);
                        }
                        
                        level.playSound(null, pos, SoundEvents.AXOLOTL_SPLASH, SoundSource.PLAYERS, 0.8f, 1.4f);
                    }
                }
                
                return ItemInteractionResult.sidedSuccess(true);
            }
        }
        
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
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
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return getStackWithData(level, pos);
    }
    
    @NotNull
    private static ItemStack getStackWithData(LevelReader level, BlockPos pos) {
        var tankEntity = (SmallTankEntity) level.getBlockEntity(pos);
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, placer, itemStack);
        
        if (itemStack.has(FluidApi.ITEM.getFluidComponent())) {
            var tankEntity = (SmallTankEntity) level.getBlockEntity(pos);
            tankEntity.fluidStorage.setStack(itemStack.get(FluidApi.ITEM.getFluidComponent()).copy());
            tankEntity.setChanged();
        }
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
}
