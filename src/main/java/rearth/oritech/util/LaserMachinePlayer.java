package rearth.oritech.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;

public final class LaserMachinePlayer extends FakePlayer {

    private final SimpleInventoryStorage targetInventory;

    public LaserMachinePlayer(ServerLevel level, GameProfile profile, SimpleInventoryStorage targetInventory) {
        super(level, profile);
        this.targetInventory = targetInventory;
    }

    public static void collectDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof LaserMachinePlayer player)) return;

        try (var transaction = Transaction.openRoot()) {
            long inserted = 0;

            for (var drop : event.getDrops()) {
                var stack = drop.getItem();
                inserted += player.targetInventory.insert(ItemResource.of(stack), stack.getCount(), transaction);
            }

            if (inserted > 0) transaction.commit();
        }

        // Match block-harvesting behavior: anything that does not fit is discarded.
        event.getDrops().clear();
    }

    public static void suppressExperienceDrops(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() instanceof LaserMachinePlayer) {
            event.setDroppedExperience(0);
        }
    }
}
