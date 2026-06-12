package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.ColorHelper;

import java.util.function.Consumer;

public class SmallFluidTankBlockItem extends BlockItem implements FluidProvider.Item {

    public SmallFluidTankBlockItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        var data = itemStack.getOrDefault(ComponentContent.STORED_FLUID, SimpleFluidContent.EMPTY);

        if (data.isEmpty()) {
            builder.accept(Component.translatable("tooltip.oritech.fluid_empty"));
        } else {
            var amount = data.getAmount() / (float) FluidType.BUCKET_VOLUME;
            builder.accept(Component.translatable("tooltip.oritech.fluid_content_tank_tooltip",
                    amount,
                    data.copy().getHoverName().getString()).withStyle(ChatFormatting.GRAY));
        }

    }

    @Override
    public Component getName(ItemStack stack) {
        var content = stack.getOrDefault(ComponentContent.STORED_FLUID, SimpleFluidContent.EMPTY);
        if (content.isEmpty()) {
            return super.getName(stack);
        } else {
            return content.copy().getHoverName().copy().append(Component.literal(" ")).append(super.getName(stack));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        var contentEmpty = stack.getOrDefault(ComponentContent.STORED_FLUID, SimpleFluidContent.EMPTY).isEmpty();
        return !contentEmpty;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        var content = stack.getOrDefault(ComponentContent.STORED_FLUID, SimpleFluidContent.EMPTY);
        if (content.isEmpty())
            return 0x07bdff;

        var fluidStack = new FluidStack(content.getFluid(), content.getAmount());

        if (fluidStack.getFluid().equals(Fluids.LAVA))
            return 0xff8000;

        return ColorHelper.getFluidTint(fluidStack) & 0xFFFFFF;
    }

    @Override
    public int getBarWidth(ItemStack stack) {

        var capacity = OritechConfig.portableTankCapacityBuckets.get() * FluidType.BUCKET_VOLUME;
        var fillAmount = stack.getOrDefault(ComponentContent.STORED_FLUID, SimpleFluidContent.EMPTY).getAmount();

        return Math.round((fillAmount * 100f / capacity) * MAX_BAR_WIDTH) / 100;
    }

    @Override
    public int getFluidCapacity() {
        return OritechConfig.portableTankCapacityBuckets.get() * FluidType.BUCKET_VOLUME;
    }
}
