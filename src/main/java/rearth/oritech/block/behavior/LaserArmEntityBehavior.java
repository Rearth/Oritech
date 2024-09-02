package rearth.oritech.block.behavior;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.machines.interaction.LaserArmBlock;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import team.reborn.energy.api.EnergyStorage;

public class LaserArmEntityBehavior {
    static private LaserArmEntityBehavior transferPowerBehavior;
    static private LaserArmEntityBehavior chargeEntityBehavior;

    // possible improvement - the target designator could be used to set up scoreboard teams,
    // and the laser could respect the attackable TargetPredicate to avoid attacking "friendly" mobs or to attack players
    // instead of trying to charge their energy storage chestplates

    public boolean fireAtEntity(World world, LaserArmBlockEntity laserEntity, LivingEntity entity) {
        // Don't kill baby animals if the crop filter addon is applied
        if (laserEntity.hasCropFilterAddon && entity instanceof AnimalEntity && entity.isBaby()) {
            return false;
        }

        entity.damage(
            new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.MAGIC), laserEntity.getLaserPlayerEntity()),
            (float)laserEntity.getDamageTick());
        
        return true;
    }

    public static void registerDefaults() {
        transferPowerBehavior = new LaserArmEntityBehavior() {
            @Override
            public boolean fireAtEntity(World world, LaserArmBlockEntity laserEntity, LivingEntity entity) {
                if (!(entity instanceof PlayerEntity player))
                    return false;
                var playerStorage = PlayerInventoryStorage.of(player);
                SingleSlotStorage<ItemVariant> packItem = playerStorage.getSlot(PlayerInventory.MAIN_SIZE + EquipmentSlot.CHEST.getEntitySlotId());
                if (packItem == null) return false;
                var energyItem = ContainerItemContext.ofPlayerSlot(player, packItem).find(EnergyStorage.ITEM);
                if (energyItem == null || energyItem.getAmount() >= energyItem.getCapacity()) {
                    return false;
                }

                try (var tx = Transaction.openOuter()) {
                    long inserted = energyItem.insert(laserEntity.energyRequiredToFire(), tx);
                    if (inserted > 0) {
                        tx.commit();
                        return true;
                    }
                    return false;
                }
            }
        };
        LaserArmBlock.registerEntityBehavior(EntityType.PLAYER, transferPowerBehavior);

        chargeEntityBehavior = new LaserArmEntityBehavior() {
            @Override
            public boolean fireAtEntity(World world, LaserArmBlockEntity laserEntity, LivingEntity entity) {
                entity.getDataTracker().set(CreeperEntity.CHARGED, true);

                // still do the default mob behavior after setting the creeper to charged
                return super.fireAtEntity(world, laserEntity, entity);
            }
        };
        LaserArmBlock.registerEntityBehavior(EntityType.CREEPER, chargeEntityBehavior);
    }
}
