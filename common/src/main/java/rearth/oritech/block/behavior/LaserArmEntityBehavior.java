package rearth.oritech.block.behavior;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.block.blocks.interaction.LaserArmBlock;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.util.StackContext;

public class LaserArmEntityBehavior {
    static private LaserArmEntityBehavior transferPowerBehavior;
    static private LaserArmEntityBehavior chargeEntityBehavior;
    
    // possible improvement - the target designator could be used to set up scoreboard teams,
    // and the laser could respect the attackable TargetPredicate to avoid attacking "friendly" mobs or to attack players
    // instead of trying to charge their energy storage chestplates
    
    public boolean fireAtEntity(Level world, LaserArmBlockEntity laserEntity, LivingEntity entity) {
        // Don't kill baby animals if the crop filter addon is applied
        if (laserEntity.hasCropFilterAddon && entity instanceof Animal && entity.isBaby()) {
            return false;
        }
        
        if (world.getGameTime() % 10 != 0) return true; // entities can only be damaged twice per second?
        
        entity.hurt(
          new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT), laserEntity.getLaserPlayerEntity()),
          laserEntity.getDamageTick());
        
        return true;
    }
    
    public static void registerDefaults() {
        transferPowerBehavior = new LaserArmEntityBehavior() {
            @Override
            public boolean fireAtEntity(Level world, LaserArmBlockEntity laserEntity, LivingEntity entity) {
                if (!(entity instanceof Player player))
                    return false;
                
                var stackRef = new StackContext(player.getItemBySlot(EquipmentSlot.CHEST), updated -> player.getInventory().armor.set(EquipmentSlot.CHEST.getIndex(), updated));
                var candidate = EnergyApi.ITEM.find(stackRef);
                if (candidate != null) {
                    var amount = candidate.insert(laserEntity.energyRequiredToFire(), false);
                    if (amount > 0) candidate.update();
                    return amount > 0;
                }
                
                return false;
            }
        };
        LaserArmBlock.registerEntityBehavior(EntityType.PLAYER, transferPowerBehavior);
        
        chargeEntityBehavior = new LaserArmEntityBehavior() {
            @Override
            public boolean fireAtEntity(Level world, LaserArmBlockEntity laserEntity, LivingEntity entity) {
                entity.getEntityData().set(Creeper.DATA_IS_POWERED, true);
                
                // still do the default mob behavior after setting the creeper to charged
                return super.fireAtEntity(world, laserEntity, entity);
            }
        };
        LaserArmBlock.registerEntityBehavior(EntityType.CREEPER, chargeEntityBehavior);
    }
}
