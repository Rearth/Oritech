package rearth.oritech.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.util.MachineControllerLifecycle;

@Mixin(BlockEntity.class)
public class MachineControllerLifecycleMixin {

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void oritech$restoreMachineConnections(Level level, CallbackInfo ci) {
        if ((Object) this instanceof MachineControllerLifecycle controller)
            controller.onControllerLoad((BlockEntity) (Object) this);
    }
}
