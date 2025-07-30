package rearth.oritech.item.tools.armor;

import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.EnergyApi.EnergyStorage;
import rearth.oritech.api.energy.containers.SimpleEnergyItemStorage;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.StackContext;
import rearth.oritech.util.TooltipHelper;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import static rearth.oritech.item.tools.harvesting.DrillItem.BAR_STEP_COUNT;


public class BackstorageExoArmorItem extends ExoArmorItem implements OritechEnergyItem {
    
    public BackstorageExoArmorItem(Holder<ArmorMaterial> material, Type type, Item.Properties settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide) return;
        
        var tickPeriod = 10;
        if (world.getGameTime() % tickPeriod != 0) return;
        
        var isPlayer = entity instanceof Player;
        var isEquipped = ((Player) entity).getItemBySlot(EquipmentSlot.CHEST).equals(stack);
        
        if (isPlayer && isEquipped) {
            distributePower((Player) entity, stack, slot);
        }
    }
    
    private void distributePower(Player player, ItemStack pack, int slot) {
        
        var packStorage = new SimpleEnergyItemStorage(getEnergyMaxInput(pack), getEnergyMaxOutput(pack), getEnergyCapacity(pack), pack);
        if (packStorage.getAmount() < 10) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack == pack || slot == i) continue;
            
            final int finalI = i;
            var stackRef = new StackContext(stack, updated -> player.getInventory().setItem(finalI, updated));
            var stackStorage = EnergyApi.ITEM.find(stackRef);
            if (stackStorage == null || stackStorage.getAmount() >= stackStorage.getCapacity()) continue;
            
            EnergyApi.transfer(packStorage, stackStorage, Long.MAX_VALUE, false);
            
            // player.getInventory().setStack(i, stackContext.getStack());
        }
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.exoChestplate.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.exoChestplate.chargeSpeed();
    }
    
    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return Oritech.CONFIG.exoChestplate.energyUsage();
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    public int getBarWidth(ItemStack stack) {
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        var text = Component.translatable("tooltip.oritech.energy_indicator", TooltipHelper.getEnergyText(this.getStoredEnergy(stack)), TooltipHelper.getEnergyText(this.getEnergyCapacity(stack)));
        tooltip.add(text.withStyle(ChatFormatting.GOLD));
    }
}
