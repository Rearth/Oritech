package rearth.oritech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

// The best way to trap a shulker would be to place a spawner controller directly underneath a shulker
// and then break the block the shulker is attached to so it attaches to the spawner controller.
// Listening to the TELEPORT game event instead of using a mixin could still work to catch a shulker
// that teleports (if it teleports on top of the spawner), but it would be very difficult.
// Using a mixin allows for catching a shulker that teleports on top OR a shulker that turns in place
// to attach to the spawner controller.
@Mixin(ShulkerEntity.class)
public class ShulkerEntityMixin {
    private final ShulkerEntity shulkerEntity = (ShulkerEntity)(Object)this;

    @Inject(method = "setAttachedFace(Lnet/minecraft/util/math/Direction;)V", at = @At("TAIL"))
    private void stepOnBlock(CallbackInfo ci, @Local Direction face) {
        var world = shulkerEntity.getWorld();
        if (world.isClient)
            return;
        
        var pos = shulkerEntity.getBlockPos().offset(face);
        var state = world.getBlockState(pos);

        if (face == Direction.DOWN) {
            state.getBlock().onSteppedOn(world, pos, state, shulkerEntity);
        }
    }

    // tryTeleport calls setAttachedFace before updating the shulker position
    // this mixin updates the position before setAttachedFace is called so that stepOnBlock above has the right position
    @Inject(method = "tryTeleport()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/ShulkerEntity;setAttachedFace(Lnet/minecraft/util/math/Direction;)V"))
    private void tryTeleport(CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) BlockPos teleportPos) {
        shulkerEntity.setPosition((double)teleportPos.getX() + 0.5, (double)teleportPos.getY(), (double)teleportPos.getZ() + 0.5);
    }
}
