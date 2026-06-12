package rearth.oritech.item.tools.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import rearth.oritech.Oritech;
import rearth.oritech.config.OritechStartupConfig;

import java.util.function.Consumer;

public class JetpackExoArmorItem extends BackstorageExoArmorItem implements BaseJetpackItem {
    public JetpackExoArmorItem(ArmorMaterial material, ArmorType type, Item.Properties settings) {
        super(material, type, settings);
    }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {

        if (level.isClientSide()) {
            tickJetpack(stack, entity, level);
        } else {
            super.inventoryTick(stack, level, entity, slot, selected);
        }
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
        return OritechStartupConfig.exoJetpack.energyCapacity.get();
    }

    @Override
    public int getMaxRFInputRate() {
        return OritechStartupConfig.exoJetpack.chargeSpeed.get();
    }

    @Override
    public int getMaxRFOutputRate() {
        return OritechStartupConfig.exoJetpack.energyUsage.get();
    }

    @Override
    public boolean requireUpward() {
        return false;
    }

    @Override
    public float getSpeed() {
        return OritechStartupConfig.exoJetpack.speed.get().floatValue();
    }

    @Override
    public int getRfUsage() {
        return OritechStartupConfig.exoJetpack.energyUsage.get();
    }

    @Override
    public int getFuelUsage() {
        return OritechStartupConfig.exoJetpack.fuelUsage.get();
    }

    @Override
    public long getFuelCapacity() {
        return OritechStartupConfig.exoJetpack.fuelCapacity.get();
    }
}
