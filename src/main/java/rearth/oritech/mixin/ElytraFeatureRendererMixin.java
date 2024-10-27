package rearth.oritech.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rearth.oritech.item.tools.armor.JetpackElytraItem;
import rearth.oritech.item.tools.armor.JetpackExoElytraItem;

@Environment(EnvType.CLIENT)
@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {
    
    // I have no idea how to actually configure the mixins, this is taken pretty much straight from mythic metals (https://github.com/Noaaan/MythicMetals/blob/1.21/src/main/java/nourl/mythicmetals/mixin/ElytraFeatureRendererMixin.java)
    @ModifyExpressionValue(
      method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z")
    )
    private boolean mythicmetals$canRenderCelestiumElytra(boolean original, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, LivingEntity entity) {
        var item = entity.getEquippedStack(EquipmentSlot.CHEST).getItem();
        return original || item instanceof JetpackElytraItem || item instanceof JetpackExoElytraItem;
    }
    
}
