package rearth.oritech.item.tools.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import rearth.oritech.Oritech;
import rearth.oritech.config.OritechStartupConfig;

import java.util.function.Consumer;

public class JetpackExoElytraItem extends BackstorageExoArmorItem implements BaseJetpackItem {
    public JetpackExoElytraItem(ArmorMaterial material, ArmorType type, Item.Properties settings) {
        super(material, type, settings);
    }

    @Override
    public boolean requireTakeoff() {
        return false;
    }

    @Override
    public Identifier getModel() {
        return Oritech.id("armor/exo_armor_jetpack");
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getJetpackBarColor(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return getJetpackBarStep(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag type) {
        var hint = Component.translatable("tooltip.oritech.jetpack_usage").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        builder.accept(hint);
        hint = Component.translatable("tooltip.oritech.jetpack_usage2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        builder.accept(hint);

        super.appendHoverText(stack, context, display, builder, type);
        addJetpackTooltip(stack, builder, false);
    }

    @Override
    public int getEnergyCapacity() {
        return OritechStartupConfig.exoElytraJetpack.energyCapacity.get();
    }

    @Override
    public int getMaxRFInputRate() {
        return OritechStartupConfig.exoElytraJetpack.chargeSpeed.get();
    }

    @Override
    public int getMaxRFOutputRate() {
        return OritechStartupConfig.exoElytraJetpack.energyUsage.get();
    }

    @Override
    public boolean requireUpward() {
        return true;
    }

    @Override
    public float getSpeed() {
        return OritechStartupConfig.exoElytraJetpack.speed.get().floatValue();
    }

    @Override
    public int getRfUsage() {
        return OritechStartupConfig.exoElytraJetpack.energyUsage.get();
    }

    @Override
    public int getFuelUsage() {
        return OritechStartupConfig.exoElytraJetpack.fuelUsage.get();
    }

    @Override
    public long getFuelCapacity() {
        return OritechStartupConfig.exoElytraJetpack.fuelCapacity.get();
    }
}
