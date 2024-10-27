package rearth.oritech.item.tools.armor;

import net.fabricmc.fabric.api.entity.event.v1.FabricElytraItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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

public class JetpackExoElytraItem extends BackstorageExoArmorItem implements BaseJetpackItem, FabricElytraItem {
    public JetpackExoElytraItem(RegistryEntry<ArmorMaterial> material, Type type, Item.Settings settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        
        if (world.isClient && MinecraftClient.getInstance().player.isFallFlying()) {
            tickJetpack(stack, entity, world);
        } else {
            super.inventoryTick(stack, world, entity, slot, selected);
        }
    }
    
    @Override
    public boolean useCustomElytra(LivingEntity entity, ItemStack chestStack, boolean tickElytra) {
        if (tickElytra)
            doVanillaElytraTick(entity, chestStack);
        
        return true;
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
        return true;
    }
    
    @Override
    public float getSpeed() {
        return Oritech.CONFIG.exoElytraJetpack.speed();
    }
    
    @Override
    public int getRfUsage() {
        return Oritech.CONFIG.exoElytraJetpack.energyUsage();
    }
    
    @Override
    public int getFuelUsage() {
        return Oritech.CONFIG.exoElytraJetpack.fuelUsage();
    }
    
    @Override
    public long getFuelCapacity() {
        return Oritech.CONFIG.exoElytraJetpack.fuelCapacity();
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.exoElytraJetpack.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.exoElytraJetpack.chargeSpeed();
    }
}
