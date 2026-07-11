package rearth.oritech.mixin.client;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rearth.oritech.client.cablesurfer.ZiplineRenderState;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements ZiplineRenderState {

    @Unique
    private boolean oritech$ziplining;

    @Override
    public boolean oritech$isZiplining() {
        return oritech$ziplining;
    }

    @Override
    public void oritech$setZiplining(boolean ziplining) {
        oritech$ziplining = ziplining;
    }
}
