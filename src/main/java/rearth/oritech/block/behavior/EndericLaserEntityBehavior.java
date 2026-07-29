package rearth.oritech.block.behavior;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.block.blocks.interaction.EndericLaserBlock;
import rearth.oritech.block.entity.interaction.EndericLaserBlockEntity;

public class EndericLaserEntityBehavior {
    static private EndericLaserEntityBehavior transferPowerBehavior;
    static private EndericLaserEntityBehavior chargeEntityBehavior;

    // possible improvement - the target designator could be used to set up scoreboard teams,
    // and the laser could respect the attackable TargetPredicate to avoid attacking "friendly" mobs or to attack players
    // instead of trying to charge their energy storage chestplates

    public boolean fireAtEntity(Level level, EndericLaserBlockEntity laserEntity, LivingEntity entity) {
        // Don't kill baby animals if the crop filter addon is applied
        if (laserEntity.hasCropFilterAddon && entity instanceof Animal && entity.isBaby()) {
            return false;
        }

        if (level.getGameTime() % 10 != 0) return true; // entities can only be damaged twice per second?

        entity.hurt(
                level.damageSources().source(DamageTypes.LIGHTNING_BOLT, laserEntity.getLaserPlayerEntity()),
                laserEntity.getDamageTick());


        return true;
    }

    public static void registerDefaults() {
        transferPowerBehavior = new EndericLaserEntityBehavior() {
            @Override
            public boolean fireAtEntity(Level level, EndericLaserBlockEntity laserEntity, LivingEntity entity) {
                if (!(entity instanceof Player player))
                    return false;

                var stack = player.getItemBySlot(EquipmentSlot.CHEST);

                if (stack.isEmpty()) return false;

                var candidate = stack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forStack(stack));
                if (candidate != null) {
                    int amount;
                    try (var transaction = Transaction.openRoot()) {
                        amount = candidate.insert(laserEntity.energyRequiredToFire(), transaction);
                        if (amount > 0) transaction.commit();
                    }
                    return amount > 0;
                }

                return false;
            }
        };
        EndericLaserBlock.registerEntityBehavior(EntityType.PLAYER, transferPowerBehavior);

        chargeEntityBehavior = new EndericLaserEntityBehavior() {
            @Override
            public boolean fireAtEntity(Level level, EndericLaserBlockEntity laserEntity, LivingEntity entity) {
                entity.getEntityData().set(Creeper.DATA_IS_POWERED, true);

                // still do the default mob behavior after setting the creeper to charged
                return super.fireAtEntity(level, laserEntity, entity);
            }
        };
        EndericLaserBlock.registerEntityBehavior(EntityType.CREEPER, chargeEntityBehavior);
    }
}
