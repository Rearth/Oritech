package rearth.oritech.item.tools.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.TooltipHelper;

import java.util.List;
import java.util.function.Consumer;

import static rearth.oritech.item.tools.harvesting.DrillItem.BAR_STEP_COUNT;


public class BackstorageExoArmorItem extends ExoArmorItem implements OritechEnergyItem {

    public BackstorageExoArmorItem(Holder<ArmorMaterial> material, ArmorType type, Item.Properties settings) {
        super(material, type, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide()) return;

        var tickPeriod = 10;
        if (level.getGameTime() % tickPeriod != 0) return;

        var isPlayer = entity instanceof Player;
        var isEquipped = ((Player) entity).getItemBySlot(EquipmentSlot.CHEST).equals(stack);

        if (isPlayer && isEquipped) {
            distributePower((Player) entity, stack, slot);
        }
    }

    private void distributePower(Player player, ItemStack pack, int slot) {

        var packAccess = ItemAccess.forStack(pack);
        var packStorage = packAccess.getCapability(Capabilities.Energy.ITEM);
        if (packStorage == null || packStorage.getAmountAsInt() < 10) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack == pack || slot == i) continue;

            var stackAccess = ItemAccess.forPlayerSlot(player, i);
            var stackStorage = stackAccess.getCapability(Capabilities.Energy.ITEM);
            if (stackStorage == null || stackStorage.getAmountAsInt() >= stackStorage.getCapacityAsInt()) continue;

            // transfer power
            var limit = Math.min(packStorage.getAmountAsInt(), stackStorage.getCapacityAsInt() - stackStorage.getAmountAsInt());
            if (limit > 0) {
                try (var transaction = Transaction.openRoot()) {
                    var extracted = packStorage.extract((int) limit, transaction);
                    var inserted = stackStorage.insert((int) extracted, transaction);
                    if (inserted > 0) {
                        transaction.commit();
                    }
                }
            }
        }
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return OritechStartupConfig.exoChestplate.energyCapacity.get();
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return OritechStartupConfig.exoChestplate.chargeSpeed.get();
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return OritechStartupConfig.exoChestplate.energyUsage.get();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((getStoredEnergy(stack, ItemAccess.forStack(stack)) * 100f / this.getCapacity()) * BAR_STEP_COUNT) / 100;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        var text = Component.translatable("tooltip.oritech.energy_indicator", TooltipHelper.getEnergyText(this.getStoredEnergy(itemStack, ItemAccess.forStack(itemStack))), TooltipHelper.getEnergyText(this.getCapacity()));
        builder.accept(text.withStyle(ChatFormatting.GOLD));
    }
}
