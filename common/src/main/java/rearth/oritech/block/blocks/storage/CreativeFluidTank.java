package rearth.oritech.block.blocks.storage;

import dev.architectury.fluid.FluidStack;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.storage.SmallTankEntity;

import java.util.List;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public class CreativeFluidTank extends SmallFluidTank {

    public CreativeFluidTank(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        addMachineTooltip(tooltip, this, this);
        if (Screen.hasControlDown())
            tooltip.add(Text.translatable("tooltip.oritech.creative_tank").formatted(Formatting.GRAY));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SmallTankEntity(pos, state, true);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if(world.isClient || !(world.getBlockEntity(pos) instanceof SmallTankEntity blockEntity)) return super.onUse(state, world, pos, player, hit);
        
        // todo use proper api here
        var mainHandStack = player.getMainHandStack();
        if (mainHandStack.isOf(Items.BUCKET)) {
            blockEntity.fluidStorage.setStack(FluidStack.empty());
            blockEntity.markDirty();
            return ActionResult.SUCCESS_NO_ITEM_USED;
        } else if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof BucketItem bucketItem) {
            blockEntity.fluidStorage.setStack(FluidStack.create(bucketItem.arch$getFluid(), 1000));
            blockEntity.markDirty();
            return ActionResult.SUCCESS_NO_ITEM_USED;
        }
        
        return super.onUse(state, world, pos, player, hit);
        
    }
    
}
