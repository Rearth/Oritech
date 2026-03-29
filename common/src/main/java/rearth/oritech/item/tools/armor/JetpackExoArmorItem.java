package rearth.oritech.item.tools.armor;

import rearth.oritech.init.OritechConfig;
import rearth.oritech.Oritech;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class JetpackExoArmorItem extends BackstorageExoArmorItem implements BaseJetpackItem {
    public JetpackExoArmorItem(Holder<ArmorMaterial> material, Type type, Item.Properties settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        
        if (world.isClientSide) {
            tickJetpack(stack, entity, world);
        } else {
            super.inventoryTick(stack, world, entity, slot, selected);
        }
    }
    
    @Override
    public ResourceLocation getModel() {
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        var hint = Component.translatable("tooltip.oritech.jetpack_usage").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        tooltip.add(hint);
        hint = Component.translatable("tooltip.oritech.jetpack_usage2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        tooltip.add(hint);
        
        super.appendHoverText(stack, context, tooltip, type);
        addJetpackTooltip(stack, tooltip, false);
    }
    
    @Override
    public boolean requireUpward() {
        return false;
    }
    
    @Override
    public float getSpeed() {
        return OritechConfig.exoJetpack.speed.get().floatValue();
    }
    
    @Override
    public int getRfUsage() {
        return OritechConfig.exoJetpack.energyUsage.get();
    }
    
    @Override
    public int getFuelUsage() {
        return OritechConfig.exoJetpack.fuelUsage.get();
    }
    
    @Override
    public long getFuelCapacity() {
        return OritechConfig.exoJetpack.fuelCapacity.get();
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return OritechConfig.exoJetpack.energyCapacity.get();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return OritechConfig.exoJetpack.chargeSpeed.get();
    }
}
