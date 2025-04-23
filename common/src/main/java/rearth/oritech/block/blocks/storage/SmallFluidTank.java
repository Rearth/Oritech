package rearth.oritech.block.blocks.storage;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.ComparatorOutputProvider;
import rearth.oritech.util.StackContext;
import rearth.oritech.api.fluid.FluidApi;

import java.util.List;

public class SmallFluidTank extends Block implements BlockEntityProvider {
    public static final BooleanProperty LIT = Properties.LIT;
    
    public SmallFluidTank(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(LIT, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SmallTankEntity(pos, state, false);
    }
    
    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }
    
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ((ComparatorOutputProvider) world.getBlockEntity(pos)).getComparatorOutput();
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
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SmallTankEntity tankEntity) {
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
                
                if (!world.isClient) {
                    if (candidate.getContent().getFirst().isEmpty()) { // from tank to item
                        var moved = FluidApi.transferFirst(tankEntity.fluidStorage, candidate, tankEntity.fluidStorage.getCapacity(), false);
                        Oritech.LOGGER.debug("moved to item {} {}", moved, stackRef.getValue());
                    } else {    // from item to tank
                        var moved = FluidApi.transferFirst(candidate, tankEntity.fluidStorage, tankEntity.fluidStorage.getCapacity(), false);
                        Oritech.LOGGER.debug("moved from item {} {}", moved, stackRef.getValue());
                    }
                }
                
                return ItemActionResult.success(true);
            }
        }
        
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }
    
    protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        var droppedStacks = super.getDroppedStacks(state, builder);
        
        var blockEntity = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (blockEntity instanceof SmallTankEntity tankEntity)
            droppedStacks.addAll(tankEntity.inventory.getHeldStacks());
        
        return droppedStacks;
    }
    
    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return getStackWithData(world, pos);
    }
    
    @NotNull
    private static ItemStack getStackWithData(WorldView world, BlockPos pos) {
        var tankEntity = (SmallTankEntity) world.getBlockEntity(pos);
        var stack = getBasePickStack(tankEntity.isCreative);
        
        if (tankEntity.fluidStorage.getAmount() > 0) {
            var fluidStack = tankEntity.fluidStorage.getStack();
            stack.set(FluidApi.ITEM.getFluidComponent(), fluidStack);
        }
        
        return stack;
    }
    
    public static ItemStack getBasePickStack(boolean creative) {
        return new ItemStack(creative ? BlockContent.CREATIVE_TANK_BLOCK.asItem() : BlockContent.SMALL_TANK_BLOCK.asItem());
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        
        if (itemStack.contains(FluidApi.ITEM.getFluidComponent())) {
            var tankEntity = (SmallTankEntity) world.getBlockEntity(pos);
            tankEntity.fluidStorage.setStack(itemStack.get(FluidApi.ITEM.getFluidComponent()));
        }
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
    
}
