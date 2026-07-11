package rearth.oritech.mixin.client;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.client.cablesurfer.ClientZiplineHandler;
import rearth.oritech.client.cablesurfer.ZiplineRenderState;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void oritech$extractZiplineState(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ((ZiplineRenderState) state).oritech$setZiplining(entity instanceof Player player && ClientZiplineHandler.isZiplining(player));
    }
}
