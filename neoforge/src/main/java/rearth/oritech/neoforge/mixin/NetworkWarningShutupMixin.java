package rearth.oritech.neoforge.mixin;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rearth.oritech.Oritech;

// Super ugly workaround for https://github.com/Rearth/Oritech/issues/358. If anyone knows how to properly fix this please let me know.
// I spent a few hours trying to fix it in owo lib or on my side, but this was the only approach I could find that works.
@Mixin(NetworkRegistry.class)
public class NetworkWarningShutupMixin {
    
    @Redirect(
      method = "getCodec",
      at = @At(
        value = "INVOKE",
                target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V",
                ordinal = 0,
                remap = false
      ),
      remap = false
    )
    private static void oritech_disableNetworkingWarning(Logger logger, String format, Object arg1) {
        if (arg1 instanceof ResourceLocation id && id.equals(Oritech.id("particles"))) {
            // nothing done here
        } else {
            logger.warn("No registration for payload {}; refusing to decode.", arg1);
        }
    }
}
