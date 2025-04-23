package rearth.oritech.neoforge.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.Oritech;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    
    @Inject(
      method = "copyFrom",
      at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerPlayerEntity;setHealth(F)V"
      )
    )
    private void onCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        var newPlayer = (ServerPlayerEntity) (Object) this; // "this" is the new player instance
        
        // Iterate through all attributes of the old player and copy persistent modifiers.
        oldPlayer.getAttributes().getAttributesToSend().forEach(oldAttributeInstance -> {
            oldAttributeInstance.getModifiers().forEach(oldModifier -> {
                var isAugment = oldModifier.id().getNamespace().equals(Oritech.MOD_ID);
                if (!isAugment) return;
                
                var newInstance = newPlayer.getAttributeInstance(oldAttributeInstance.getAttribute());
                if (newInstance == null) return;
                newInstance.overwritePersistentModifier(oldModifier);
                
            });
        });
    }
}