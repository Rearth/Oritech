package rearth.oritech.client.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import rearth.oritech.block.entity.machines.accelerator.BlackHoleBlockEntity;
import rearth.oritech.init.BlockContent;

public class BlackHoleRenderer implements BlockEntityRenderer<BlackHoleBlockEntity> {
    
    @Override
    public void render(BlackHoleBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        var time = entity.getWorld().getTime();
        // render block getting sucked in
        if (entity.currentlyPullingFrom != null && entity.pullingStartedAt + entity.pullTime > time && !entity.currentlyPulling.isAir()) {
            
            var progress = (float) Math.pow((time + tickDelta - entity.pullingStartedAt) / (float) entity.pullTime, 1.3f);
            var startPos = Vec3d.of(entity.currentlyPullingFrom);
            var endPos = entity.getPos().toCenterPos();
            var renderedBlock = entity.currentlyPulling;
            var offset = endPos.subtract(startPos).multiply(1 - progress);
            var rotationY = progress * entity.pullTime * 3;
            
            matrices.push();
            matrices.translate(0.5, 0.5, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
            matrices.translate(-offset.x, -offset.y, -offset.z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationY));
            matrices.scale(1 - progress, 1 - progress, 1 - progress);
            
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
              renderedBlock,
              entity.getPos(),
              entity.getWorld(),
              matrices,
              vertexConsumers.getBuffer(RenderLayers.getBlockLayer(renderedBlock)),
              true,
              entity.getWorld().random
            );
            
            matrices.pop();
            
        }
        
        renderBlackHole(entity, tickDelta, matrices, vertexConsumers);
    }
    
    private static void renderBlackHole(BlackHoleBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        var time = entity.getWorld().getTime() + tickDelta;
        var rotationY = (time * 1.2f) % 360;
        var rotationX = Math.sin(time * 0.02) * 5;
        
        matrices.push();
        
        matrices.translate(0.5f, 0.5f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotationX));
        matrices.translate(-0.5f, -0.5f, -0.5f);
        
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
          BlockContent.BLACK_HOLE_INNER.getDefaultState(),
          entity.getPos(),
          entity.getWorld(),
          matrices,
          vertexConsumers.getBuffer(RenderLayer.getEndGateway()),
          true,
          entity.getWorld().random
        );
        
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
          BlockContent.BLACK_HOLE_MIDDLE.getDefaultState(),
          entity.getPos(),
          entity.getWorld(),
          matrices,
          vertexConsumers.getBuffer(RenderLayers.getBlockLayer(BlockContent.BLACK_HOLE_MIDDLE.getDefaultState())),
          true,
          entity.getWorld().random
        );
        
        matrices.pop();
        matrices.push();
        
        matrices.translate(0.5f, 0.5f, 0.5f);
        rotationY = (time * 1.1f) % 360;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotationX));
        matrices.translate(-0.5f, -0.5f, -0.5f);
        
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
          BlockContent.BLACK_HOLE_OUTER.getDefaultState(),
          entity.getPos(),
          entity.getWorld(),
          matrices,
          vertexConsumers.getBuffer(RenderLayers.getBlockLayer(BlockContent.BLACK_HOLE_OUTER.getDefaultState())),
          true,
          entity.getWorld().random
        );
        
        matrices.pop();
    }
}
