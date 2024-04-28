package rearth.oritech.block.blocks.machines.storage;

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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (!world.isClient) {
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
            
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            var stack = new ItemStack(BlockContent.SMALL_TANK_BLOCK.asItem());
            var tankEntity = (SmallFluidTankEntity) world.getBlockEntity(pos);
            
            if (tankEntity.getForDirectFluidAccess().amount > 0) {
                var nbt = new NbtCompound();
                tankEntity.writeNbt(nbt);
                stack.setNbt(nbt);
                Block.dropStack(world, pos, stack);
            }
            
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        
        var tankEntity = (SmallFluidTankEntity) world.getBlockEntity(pos);
        var nbt = itemStack.getNbt();
        if (nbt != null)
            tankEntity.readNbt(nbt);
        
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
