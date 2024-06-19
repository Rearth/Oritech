package rearth.oritech.item.tools.armor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import rearth.oritech.item.tools.util.Helpers;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class BackstorageExoArmorItem extends ExoArmorItem implements SimpleEnergyItem {
    
    public BackstorageExoArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) return;
        
        var tickPeriod = 20;
        if (world.getTime() % tickPeriod != 0) return;
        
        if (this.getSlotType() == EquipmentSlot.CHEST && entity instanceof PlayerEntity player && player.getEquippedStack(EquipmentSlot.CHEST).equals(stack)) {
            Helpers.distributePower(player, stack, 2000);
        }
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return 100000;
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return 1000;
    }
    
    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 1000;
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    public int getItemBarStep(ItemStack stack) {
        var energyItem = (SimpleEnergyItem) stack.getItem();
        return Math.round((energyItem.getStoredEnergy(stack) * 100f / energyItem.getEnergyCapacity(stack)) * 13) / 100;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        var text = Text.literal(String.format("%d/%d RF", this.getStoredEnergy(stack), this.getEnergyCapacity(stack)));
        tooltip.add(text.formatted(Formatting.GOLD));
    }
}
