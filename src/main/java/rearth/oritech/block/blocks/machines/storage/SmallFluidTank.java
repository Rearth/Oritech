package rearth.oritech.block.blocks.machines.storage;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.storage.SmallFluidTankEntity;
import rearth.oritech.init.BlockContent;

public class SmallFluidTank extends Block implements BlockEntityProvider {
    
    public SmallFluidTank(Settings settings) {
        super(settings);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SmallFluidTankEntity(pos, state);
    }
    
    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }
    
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ((SmallFluidTankEntity) world.getBlockEntity(pos)).getComparatorOutput();
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
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient) {
            var stack = getStackWithData(world, pos);
            var tankEntity = (SmallFluidTankEntity) world.getBlockEntity(pos);
            
            if (!player.isCreative())
                Block.dropStack(world, pos, stack);
            
            var stacks = tankEntity.inventory.heldStacks;
            for (var heldStack : stacks) {
                if (!heldStack.isEmpty()) {
                    var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), heldStack);
                    world.spawnEntity(itemEntity);
                }
            }
            
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return getStackWithData(world, pos);
    }
    
    @NotNull
    private static ItemStack getStackWithData(WorldView world, BlockPos pos) {
        var stack = new ItemStack(BlockContent.SMALL_TANK_BLOCK.asItem());
        var tankEntity = (SmallFluidTankEntity) world.getBlockEntity(pos);
        
        if (tankEntity.getForDirectFluidAccess().amount > 0) {
            var nbt = new NbtCompound();
            tankEntity.writeNbt(nbt, world.getRegistryManager());
            nbt.remove("Items");
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            var fluidName = FluidVariantAttributes.getName(tankEntity.getForDirectFluidAccess().variant);
            stack.set(DataComponentTypes.CUSTOM_NAME, fluidName.copy().append(" ").append(Text.translatable("block.oritech.small_tank_block")));
        }
        
        return stack;
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        
        if (!itemStack.contains(DataComponentTypes.CUSTOM_DATA)) return;
        
        var tankEntity = (SmallFluidTankEntity) world.getBlockEntity(pos);
        var nbt = itemStack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();
        if (nbt != null)
            tankEntity.readNbt(nbt, world.getRegistryManager());
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
