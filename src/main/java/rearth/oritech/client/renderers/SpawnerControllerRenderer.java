package rearth.oritech.client.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;

public class SpawnerControllerRenderer implements BlockEntityRenderer<SpawnerControllerBlockEntity> {
    
    @Override
    public void render(SpawnerControllerBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        
        if (entity.renderedEntity != null && entity.hasCage) {
            
            matrices.push();
            matrices.translate(0, -Math.round(entity.spawnedMob.getHeight() + 0.4f), 0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
            
            var dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            
            var renderer = dispatcher.getRenderer(entity.renderedEntity);
            
            var progress = Math.min(1f, entity.collectedSouls / (float) entity.maxSouls);
            if (progress != 0)
                progress = (float) LaserArmRenderer.lerp(entity.lastProgress, progress, 0.03f);
            entity.lastProgress = progress;
            
            var color = ColorHelper.Argb.getArgb((int) (75 + 180 * progress), (int) (255 * (1f - progress)), 255, 255);
            
            if (renderer instanceof LivingEntityRenderer livingEntityRenderer && entity.renderedEntity instanceof LivingEntity) {
                
                matrices.scale(-1.0F, -1.0F, 1.0F);
                matrices.translate(0.0F, -1.501F, 0.0F);
                matrices.scale(0.9f, 0.9f, 0.9f);
                var model = livingEntityRenderer.getModel();
                var renderLayer = RenderLayer.getBeaconBeam(Identifier.ofVanilla("textures/entity/beacon_beam.png"), true);
                // var renderLayer = RenderLayer.getEndGateway();   // yeah this is fun
                var vertexConsumer = vertexConsumers.getBuffer(renderLayer);
                model.render(matrices, vertexConsumer, light, overlay, color);
            }
            matrices.pop();
        }
    }
}
