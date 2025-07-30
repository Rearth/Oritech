package rearth.oritech.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rearth.oritech.item.tools.armor.JetpackElytraItem;
import rearth.oritech.item.tools.armor.JetpackExoElytraItem;

@Mixin(ElytraLayer.class)
public class ElytraLayerMixin {
    
    // I have no idea how to actually configure the mixins, this is taken pretty much straight from mythic metals (https://github.com/Noaaan/MythicMetals/blob/1.21/src/main/java/nourl/mythicmetals/mixin/ElytraFeatureRendererMixin.java)
    @ModifyExpressionValue(
      method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z")
    )
    private boolean oritech$canRenderJetpackElytra(boolean original, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, LivingEntity entity) {
        var item = entity.getItemBySlot(EquipmentSlot.CHEST).getItem();
        return original || item instanceof JetpackElytraItem || item instanceof JetpackExoElytraItem;
    }
    
}
