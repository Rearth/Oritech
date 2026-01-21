package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;

public class SpawnerControllerRenderer implements BlockEntityRenderer<SpawnerControllerBlockEntity> {
    
    @Override
    public void render(SpawnerControllerBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        
        
        if (entity.renderedEntity != null && entity.hasCage) {
            
            matrices.pushPose();
            matrices.translate(0, -Math.round(entity.renderedEntity.getBbHeight() + 0.4f), 0);
            matrices.mulPose(Axis.YP.rotationDegrees(45));
            
            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            
            var renderer = dispatcher.getRenderer(entity.renderedEntity);
            
            var progress = Math.min(1f, entity.collectedSouls / (float) entity.maxSouls);
            if (progress != 0)
                progress = (float) LaserArmRenderer.lerp(entity.lastProgress, progress, 0.03f);
            entity.lastProgress = progress;
            
            var color = FastColor.ARGB32.color((int) (75 + 180 * progress), (int) (255 * (1f - progress)), 255, 255);
            
            if (renderer instanceof LivingEntityRenderer livingEntityRenderer && entity.renderedEntity instanceof LivingEntity) {
                
                matrices.scale(-1.0F, -1.0F, 1.0F);
                matrices.translate(0.0F, -1.501F, 0.0F);
                matrices.scale(0.9f, 0.9f, 0.9f);
                var model = livingEntityRenderer.getModel();
                var renderLayer = RenderType.beaconBeam(ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png"), true);
                // var renderLayer = RenderLayer.getEndGateway();   // yeah this is fun
                var vertexConsumer = vertexConsumers.getBuffer(renderLayer);
                model.renderToBuffer(matrices, vertexConsumer, light, overlay, color);
            }
            matrices.popPose();
        }
    }
}
