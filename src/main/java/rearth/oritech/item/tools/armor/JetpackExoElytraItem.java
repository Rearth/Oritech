package rearth.oritech.item.tools.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import rearth.oritech.Oritech;
import rearth.oritech.config.OritechStartupConfig;

import java.util.List;

public class JetpackExoElytraItem extends BackstorageExoArmorItem implements BaseJetpackItem {
    public JetpackExoElytraItem(Holder<ArmorMaterial> material, Type type, Item.Properties settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        
        if (level.isClientSide && Minecraft.getInstance().player.isFallFlying()) {
            tickJetpack(stack, entity, level);
        } else {
            super.inventoryTick(stack, level, entity, slot, selected);
        }
    }
    
    @Override
    public boolean requireTakeoff() {
        return false;
    }
    
    public boolean useCustomElytra(LivingEntity entity, ItemStack chestStack, boolean tickElytra) {
        if (!tickElytra) return true;
        
        int nextRoll = entity.getFallFlyingTicks() + 1;
        if (!entity.level().isClientSide && nextRoll % 10 == 0) {
            entity.gameEvent(GameEvent.ELYTRA_GLIDE);
        }
        
        return true;
    }
    
    // this overrides the IItemExtension methods in neoforge
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return useCustomElytra(entity, entity.getItemBySlot(EquipmentSlot.CHEST), true);
    }
    
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        return true;
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
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return OritechStartupConfig.exoElytraJetpack.energyCapacity.get();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return OritechStartupConfig.exoElytraJetpack.chargeSpeed.get();
    }
}
