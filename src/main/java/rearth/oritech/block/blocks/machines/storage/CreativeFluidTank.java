package rearth.oritech.block.blocks.machines.storage;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.mixin.transfer.BucketItemAccessor;
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
import rearth.oritech.block.entity.machines.storage.SmallFluidTankEntity;

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
        return new SmallFluidTankEntity(pos, state, true);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if(world.isClient || !(world.getBlockEntity(pos) instanceof SmallFluidTankEntity blockEntity)) return super.onUse(state, world, pos, player, hit);
        
        var mainHandStack = player.getMainHandStack();
        if (mainHandStack.isOf(Items.BUCKET)) {
            blockEntity.fluidStorage.amount = 0;
            blockEntity.fluidStorage.variant = FluidVariant.blank();
            blockEntity.markDirty();
            return ActionResult.SUCCESS_NO_ITEM_USED;
        } else if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof BucketItem) {
            blockEntity.fluidStorage.variant = FluidVariant.of(((BucketItemAccessor) mainHandStack.getItem()).fabric_getFluid());
            blockEntity.markDirty();
            return ActionResult.SUCCESS_NO_ITEM_USED;
        }
        
        return super.onUse(state, world, pos, player, hit);
        
    }
    
}
