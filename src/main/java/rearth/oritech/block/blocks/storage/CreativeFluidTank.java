package rearth.oritech.block.blocks.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.access.ItemAccess;
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty() && level.getBlockEntity(pos) instanceof SmallTankEntity blockEntity) {
            var itemAccess = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
            var candidate = itemAccess.getCapability(Capabilities.Fluid.ITEM);

            if (candidate != null) {
                var resource = candidate.getResource(0);

                // allow setting fluid content of creative tanks
                if (!resource.isEmpty()) {
                    if (!level.isClientSide()) {
                        blockEntity.fluidStorage.set(0, resource, FluidType.BUCKET_VOLUME);
                        blockEntity.setChanged();
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

}
