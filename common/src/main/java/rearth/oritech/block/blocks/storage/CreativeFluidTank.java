package rearth.oritech.block.blocks.storage;

import dev.architectury.fluid.FluidStack;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.storage.SmallTankEntity;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public class CreativeFluidTank extends SmallFluidTank {

    public CreativeFluidTank(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        addMachineTooltip(tooltip, this, this);
        if (Screen.hasControlDown())
            tooltip.add(Component.translatable("tooltip.oritech.creative_tank").withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmallTankEntity(pos, state, true);
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if(world.isClientSide || !(world.getBlockEntity(pos) instanceof SmallTankEntity blockEntity)) return super.useWithoutItem(state, world, pos, player, hit);
        
        // todo use proper api here
        var mainHandStack = player.getMainHandItem();
        if (mainHandStack.is(Items.BUCKET)) {
            blockEntity.fluidStorage.setStack(FluidStack.empty());
            blockEntity.setChanged();
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        } else if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof BucketItem bucketItem) {
            blockEntity.fluidStorage.setStack(FluidStack.create(bucketItem.arch$getFluid(), 1000));
            blockEntity.setChanged();
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }
        
        return super.useWithoutItem(state, world, pos, player, hit);
        
    }
    
}
