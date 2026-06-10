package rearth.oritech.item.tools.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.transfer.energy.EnergyProvider;

public interface OritechEnergyItem extends EnergyProvider.Item {

    default boolean tryUseEnergy(ItemStack stack, int amount, Player player) {
        RandomSource random = RandomSource.create();

        int unbreakingLevel = getUnbreakingLevel(stack);
        if (unbreakingLevel > 0) {
            amount = amount / (random.nextInt(unbreakingLevel) + 1);
        }

        var storage = getEnergyStorage(stack, ItemAccess.forStack(stack));
        if (storage != null) {
            try (var transaction = Transaction.openRoot()) {
                var extracted = storage.extract(amount, transaction);
                return extracted == amount;
            }
        }

        return false;

    }

    // A hack to do this without context of the DRM
    private int getUnbreakingLevel(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> entry : enchantments.keySet()) {
            if (entry.unwrapKey().isPresent() && entry.unwrapKey().get().equals(Enchantments.UNBREAKING)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }

    default long getStoredEnergy(ItemStack stack, ItemAccess itemAccess) {
        return getEnergyStorage(stack, itemAccess).getAmountAsInt();
    }

    default EnergyHandler getEnergyStorage(ItemStack stack, ItemAccess itemAccess) {
        return stack.getCapability(Capabilities.Energy.ITEM, itemAccess);
    }


}
