package rearth.oritech.block.blocks.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.util.TooltipHelper;

import java.util.function.Consumer;

public class CreativeFluidTank extends SmallFluidTank implements TooltipProvider {

    public CreativeFluidTank(Properties settings) {
        super(settings);
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        TooltipHelper.addMachineTooltip(consumer, this, this);
        if (Minecraft.getInstance().hasControlDown())
            consumer.accept(Component.translatable("tooltip.oritech.creative_tank").withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmallTankEntity(pos, state, true);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SmallTankEntity blockEntity))
            return super.useWithoutItem(state, level, pos, player, hit);

        // todo use proper api here
        var mainHandStack = player.getMainHandItem();
        if (mainHandStack.is(Items.BUCKET)) {
            blockEntity.fluidStorage.set(0, FluidResource.EMPTY, 0);
            blockEntity.setChanged();
            return InteractionResult.SUCCESS;
        } else if (!mainHandStack.isEmpty() && mainHandStack.getItem() instanceof BucketItem bucketItem) {
            blockEntity.fluidStorage.set(0, FluidResource.of(bucketItem.content), FluidType.BUCKET_VOLUME);
            blockEntity.setChanged();
            return InteractionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hit);

    }

}
