package rearth.oritech.item.other;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.TooltipHelper;

import java.util.function.Consumer;

public class SmallEnergyStorageBlockItem extends BlockItem implements EnergyProvider.Item {

    public SmallEnergyStorageBlockItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        var storedEnergy = itemStack.getOrDefault(ComponentContent.ENERGY, 0);

        if (storedEnergy != 0) {
            var text = Component.translatable("tooltip.oritech.energy_stored", TooltipHelper.getEnergyText(storedEnergy));
            builder.accept(text.withStyle(ChatFormatting.GOLD));
        }

    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        var contentEmpty = stack.getOrDefault(ComponentContent.ENERGY, 0) <= 0;
        return !contentEmpty;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }

    @Override
    public int getBarWidth(ItemStack stack) {

        var capacity = OritechConfig.smallEnergyStorage.energyCapacity.get();
        var fillAmount = stack.getOrDefault(ComponentContent.ENERGY, 0);

        return Math.round((fillAmount * 100f / capacity) * MAX_BAR_WIDTH) / 100;
    }

    @Override
    public int getCapacity() {
        return Math.toIntExact(OritechConfig.smallEnergyStorage.energyCapacity.get());
    }

    @Override
    public int getMaxInsert() {
        return Math.toIntExact(OritechConfig.smallEnergyStorage.maxEnergyInsertion.get());
    }

    @Override
    public int getMaxExtract() {
        return Math.toIntExact(OritechConfig.smallEnergyStorage.maxEnergyExtraction.get());
    }
}
