package rearth.oritech.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;

// needed for the precision mining augment
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private int destroyDelay;

    @Unique
    private int oritech$precisionMiningCooldown;

    @Inject(method = "tick", at = @At("HEAD"))
    private void oritech$tickPrecisionMiningCooldown(CallbackInfo ci) {
        if (oritech$precisionMiningCooldown > 0) {
            oritech$precisionMiningCooldown--;
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void oritech$applyPrecisionMiningCooldown(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (oritech$precisionMiningCooldown > 0 && oritech$isPrecisionMiningActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void oritech$applyHeldMiningCooldown(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        // Let vanilla count down its own delay after a block finished mining normally.
        if (destroyDelay == 0) {
            oritech$applyPrecisionMiningCooldown(pos, direction, cir);
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void oritech$onBlockDestroyed(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && oritech$isPrecisionMiningActive()) {
            // Creative skips five ticks before breaking again, giving a six-tick interval.
            oritech$precisionMiningCooldown = 4;
        }
    }

    @Unique
    private boolean oritech$isPrecisionMiningActive() {
        var player = minecraft.player;
        return player != null && !player.getAbilities().instabuild && CustomAugmentsCollection.precisionMining.isEnabled(player);
    }
}
