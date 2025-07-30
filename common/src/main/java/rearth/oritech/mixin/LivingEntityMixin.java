package rearth.oritech.mixin;

import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Attackable {
    
    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }
    
    @Override
    public ItemEntity spawnAtLocation(ItemStack stack) {
        return spawnAtLocation(stack, 0.0F);
    }
    
    @SuppressWarnings("resource")
    @Override
    public ItemEntity spawnAtLocation(ItemStack stack, float yOffset) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        LivingEntity attacker = thisEntity.getLastAttacker();
        
        if (stack.isEmpty() || thisEntity.level().isClientSide) return null;
        
        if (!thisEntity.isAlive() && oritech$isLaser(attacker)) {
            ((Player)attacker).addItem(stack);
            return null;
        }
        return super.spawnAtLocation(stack, yOffset);
    }
    
    @Inject(method = "dropExperience", at = @At(value = "HEAD"), cancellable = true)
    private void disableXpForLaser(Entity attacker, CallbackInfo ci) {
        if (oritech$isLaser(attacker))
            ci.cancel();
    }
    
    @Unique
    private boolean oritech$isLaser(Entity attacker) {
        return attacker instanceof Player player && player.getGameProfile().getName().equals(LaserArmBlockEntity.LASER_PLAYER_NAME);
    }
}