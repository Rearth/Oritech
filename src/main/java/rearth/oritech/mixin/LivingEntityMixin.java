package rearth.oritech.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Attackable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Attackable {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public ItemEntity dropStack(ItemStack stack) {
        return dropStack(stack, 0.0F);
    }

    @SuppressWarnings("resource")
    @Override
    public ItemEntity dropStack(ItemStack stack, float yOffset) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;
        LivingEntity attacker = thisEntity.getLastAttacker();

        if (stack.isEmpty() || thisEntity.getWorld().isClient) return null;
        
        if (!thisEntity.isAlive() && isLaser(attacker)) {
            ((PlayerEntity)attacker).giveItemStack(stack);
            return null;
        }
        return super.dropStack(stack, yOffset);
    }

    @Inject(method = "dropXp(Lnet/minecraft/entity/Entity;)V", at = @At(value = "HEAD"), cancellable = true)
    private void disableXpForLaser(@Nullable Entity attacker, CallbackInfo ci) {
        if (isLaser(attacker))
            ci.cancel();
    }

    private boolean isLaser(Entity attacker) {
        return attacker != null && attacker instanceof PlayerEntity player && player.getGameProfile().getName() == LaserArmBlockEntity.LASER_PLAYER_NAME;
    }
}
