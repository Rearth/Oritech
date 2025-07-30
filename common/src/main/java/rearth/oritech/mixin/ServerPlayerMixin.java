package rearth.oritech.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.Oritech;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    
    @Inject(
      method = "restoreFrom",
      at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;setHealth(F)V"
      )
    )
    private void onCopyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        var newPlayer = (ServerPlayer) (Object) this; // "this" is the new player instance
        
        // Iterate through all attributes of the old player and copy persistent modifiers.
        oldPlayer.getAttributes().getSyncableAttributes().forEach(oldAttributeInstance -> {
            oldAttributeInstance.getModifiers().forEach(oldModifier -> {
                var isAugment = oldModifier.id().getNamespace().equals(Oritech.MOD_ID);
                if (!isAugment) return;
                System.out.println(oldModifier.id());
                
                var newInstance = newPlayer.getAttribute(oldAttributeInstance.getAttribute());
                if (newInstance == null) return;
                newInstance.addOrReplacePermanentModifier(oldModifier);
                
            });
        });
    }
}