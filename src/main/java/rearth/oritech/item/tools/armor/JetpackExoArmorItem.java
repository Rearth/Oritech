package rearth.oritech.item.tools.armor;

import net.minecraft.entity.Entity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import rearth.oritech.Oritech;

import java.util.List;

public class JetpackExoArmorItem extends BackstorageExoArmorItem implements BaseJetpackItem {
    public JetpackExoArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Item.Settings settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        
        if (world.isClient) {
            tickJetpack(stack, entity, world);
        } else {
            super.inventoryTick(stack, world, entity, slot, selected);
        }
    }
    
    @Override
    public Identifier getModel() {
        return Oritech.id("armor/exo_armor_jetpack");
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return getJetpackBarColor(stack);
    }
    
    @Override
    public int getItemBarStep(ItemStack stack) {
        return getJetpackBarStep(stack);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var hint = Text.translatable("tooltip.oritech.jetpack_usage").formatted(Formatting.GRAY, Formatting.ITALIC);
        tooltip.add(hint);
        hint = Text.translatable("tooltip.oritech.jetpack_usage2").formatted(Formatting.GRAY, Formatting.ITALIC);
        tooltip.add(hint);
        
        super.appendTooltip(stack, context, tooltip, type);
        addJetpackTooltip(stack, tooltip, false);
    }
    
    @Override
    public boolean requireUpward() {
        return false;
    }
    
    @Override
    public float getSpeed() {
        return Oritech.CONFIG.exoJetpack.speed();
    }
    
    @Override
    public int getRfUsage() {
        return Oritech.CONFIG.exoJetpack.energyUsage();
    }
    
    @Override
    public int getFuelUsage() {
        return Oritech.CONFIG.exoJetpack.fuelUsage();
    }
    
    @Override
    public long getFuelCapacity() {
        return Oritech.CONFIG.exoJetpack.fuelCapacity();
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.exoJetpack.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.exoJetpack.chargeSpeed();
    }
}
