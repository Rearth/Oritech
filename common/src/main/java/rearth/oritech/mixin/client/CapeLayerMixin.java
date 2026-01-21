package rearth.oritech.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.item.tools.armor.JetpackElytraItem;
import rearth.oritech.item.tools.armor.JetpackExoElytraItem;

@Mixin(CapeLayer.class)
public class CapeLayerMixin {
    
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        
        if (chestStack.getItem() instanceof JetpackElytraItem || chestStack.getItem() instanceof JetpackExoElytraItem) {
            ci.cancel();
        }
    }
}