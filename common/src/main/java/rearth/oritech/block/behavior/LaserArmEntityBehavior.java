package rearth.oritech.block.behavior;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import rearth.oritech.block.blocks.interaction.LaserArmBlock;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.SingleSlotHandler;

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
                
                var chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
                var candidate = EnergyApi.ITEM.find(chestItem, ContainerItemContext.ofSingleSlot(new SingleSlotHandler(chestItem)));
                if (candidate != null) {
                    var amount = EnergyApi.transfer(laserEntity.getEnergyStorageForLink(), candidate, laserEntity.energyRequiredToFire(), false);
                    return amount > 0;
                }
                
                return false;
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
