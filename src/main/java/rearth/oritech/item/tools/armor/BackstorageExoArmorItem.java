package rearth.oritech.item.tools.armor;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
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
        
        var isPlayer = entity instanceof PlayerEntity;
        var isEquipped = ((PlayerEntity) entity).getEquippedStack(EquipmentSlot.CHEST).equals(stack);
        
        if (isPlayer && isEquipped) {
            distributePower((PlayerEntity) entity, stack, 2000);
        }
    }
    
    private void distributePower(PlayerEntity player, ItemStack pack, long maxTransfer) {
        
        var playerStorage = PlayerInventoryStorage.of(player);
        SingleSlotStorage<ItemVariant> packItem = null;
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i) == pack) {
                packItem = playerStorage.getSlot(i);
                break;
            }
        }
        
        if (packItem == null) return;
        var energyItem = ContainerItemContext.ofPlayerSlot(player, packItem).find(EnergyStorage.ITEM);
        if (energyItem == null) return;
        if (energyItem.getAmount() <= 10) return;
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            var stack = player.getInventory().getStack(i);
            if (stack.isEmpty() || stack == pack) continue;
            
            EnergyStorageUtil.move(energyItem, ContainerItemContext.ofPlayerSlot(player, playerStorage.getSlot(i)).find(EnergyStorage.ITEM), maxTransfer, null);
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
        var text = Text.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(stack), this.getEnergyCapacity(stack));
        tooltip.add(text.formatted(Formatting.GOLD));
    }
}
