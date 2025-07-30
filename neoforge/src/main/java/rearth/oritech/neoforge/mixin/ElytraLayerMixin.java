package rearth.oritech.neoforge.mixin;

import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.oritech.item.tools.armor.JetpackElytraItem;
import rearth.oritech.item.tools.armor.JetpackExoElytraItem;

@OnlyIn(Dist.CLIENT)
@Mixin(ElytraLayer.class)
public class ElytraLayerMixin {
    
    @Inject(
      method = "shouldRender",
      at = @At("HEAD"),
      cancellable = true
    )
    private void oritech$shouldRenderJetpackElytra(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof JetpackElytraItem || stack.getItem() instanceof JetpackExoElytraItem) {
            cir.setReturnValue(true);
        }
    }
    
}
