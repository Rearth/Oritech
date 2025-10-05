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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.oritech.util.FakePlayerMarker;


@Mixin(Entity.class)
public abstract class EntityLaserDropsMixin implements Attackable {


    @Shadow public abstract boolean isAlive();

    @Inject(
            method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At(value = "NEW", args = "class=net/minecraft/world/entity/item/ItemEntity"),
            cancellable = true
    )
    @SuppressWarnings("ConstantValue") // this is mixin
    private void oritech$teleportDropsToLaser(ItemStack stack, float offsetY, CallbackInfoReturnable<ItemEntity> cir) {
        if ((Object) this instanceof LivingEntity thiz && thiz.getLastAttacker() instanceof FakePlayerMarker) {
            Player attacker = (Player) thiz.getLastAttacker();

            if (!isAlive()) { // only teleport when we are dead, ie were killed by the laser
                attacker.addItem(stack);
                cir.setReturnValue(null);
            }
        }
    }

    @Mixin(LivingEntity.class)
    static abstract class LivingEntityMixin extends Entity {
        public LivingEntityMixin(EntityType<?> entityType, Level level) {
            super(entityType, level);
        }

        @Inject(method = "dropExperience", at = @At(value = "HEAD"), cancellable = true)
        private void oritech$disableXpForLaser(Entity attacker, CallbackInfo ci) {
            if (attacker instanceof FakePlayerMarker)
                ci.cancel();
        }
    }

}