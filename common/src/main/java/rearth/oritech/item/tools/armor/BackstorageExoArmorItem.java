package rearth.oritech.item.tools.armor;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.SimpleEnergyItemStorage;
import rearth.oritech.util.energy.SingleSlotHandler;
import rearth.oritech.util.TooltipHelper;

import java.util.List;

import static rearth.oritech.item.tools.harvesting.DrillItem.BAR_STEP_COUNT;

public class BackstorageExoArmorItem extends ExoArmorItem implements OritechEnergyItem {
    
    public BackstorageExoArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Item.Settings settings) {
        super(material, type, settings);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) return;
        
        var tickPeriod = 10;
        if (world.getTime() % tickPeriod != 0) return;
        
        var isPlayer = entity instanceof PlayerEntity;
        var isEquipped = ((PlayerEntity) entity).getEquippedStack(EquipmentSlot.CHEST).equals(stack);
        
        if (isPlayer && isEquipped) {
            distributePower((PlayerEntity) entity, stack, slot);
        }
    }
    
    private void distributePower(PlayerEntity player, ItemStack pack, int slot) {
        
        var packStorage = new SimpleEnergyItemStorage(getEnergyMaxInput(pack), getEnergyMaxOutput(pack), getEnergyCapacity(pack), pack);
        if (packStorage.getAmount() < 10) return;

        for (int i = 0; i < player.getInventory().size(); i++) {
            var stack = player.getInventory().getStack(i);
            if (stack.isEmpty() || stack == pack || slot == i) continue;

            var stackContext = new SingleSlotHandler(stack);
            var stackStorage = EnergyApi.ITEM.find(stack, ContainerItemContext.ofSingleSlot(stackContext));
            if (stackStorage == null || stackStorage.getAmount() >= stackStorage.getCapacity()) continue;
            
            EnergyApi.transfer(packStorage, stackStorage, Long.MAX_VALUE, false);
            
            player.getInventory().setStack(i, stackContext.getStack());
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
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    public int getItemBarStep(ItemStack stack) {
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        var text = Text.translatable("tooltip.oritech.energy_indicator", TooltipHelper.getEnergyText(this.getStoredEnergy(stack)), TooltipHelper.getEnergyText(this.getEnergyCapacity(stack)));
        tooltip.add(text.formatted(Formatting.GOLD));
    }
}
