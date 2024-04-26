package rearth.oritech.item.other;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import rearth.oritech.init.ItemContent;

import java.util.List;

public class MobCaptureItem extends Item {
    
    public final List<EntityType<?>> targets;
    
    public MobCaptureItem(Settings settings, List<EntityType<?>> targets) {
        super(settings);
        this.targets = targets;
    }
    
    
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        
        var resultingItem = ItemContent.UNHOLY_INTELLIGENCE;
        
        for (var target : targets) {
            if (entity.getType().equals(target)) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    user.setStackInHand(hand, ItemStack.EMPTY);
                } else {
                    user.setStackInHand(hand, stack);
                }
                
                entity.kill();
                
                // add to inv
                var playerInv = InventoryStorage.of(user.getInventory(), null);
                var tx = Transaction.openOuter();
                if (playerInv.insert(ItemVariant.of(resultingItem), 1, tx) == 1) {
                    tx.commit();
                } else {
                    tx.abort();
                    user.getWorld().spawnEntity(new ItemEntity(user.getWorld(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack(resultingItem)));
                }
                
                return ActionResult.CONSUME;
            }
        }
        
        return ActionResult.PASS;
    }
}
