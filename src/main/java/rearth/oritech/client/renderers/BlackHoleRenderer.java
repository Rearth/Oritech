package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.block.entity.accelerator.BlackHoleBlockEntity;
import rearth.oritech.init.BlockContent;

public class BlackHoleRenderer implements BlockEntityRenderer<BlackHoleBlockEntity> {
    
    @Override
    public void render(BlackHoleBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        
        var time = entity.getLevel().getGameTime();
        // render block getting sucked in
        if (entity.currentlyPullingFrom != null && entity.currentlyPulling != null && entity.pullingStartedAt + entity.pullTime > time && !entity.currentlyPulling.isAir()) {
            
            var progress = (float) Math.pow((time + tickDelta - entity.pullingStartedAt) / (float) entity.pullTime, 1.3f);
            var startPos = Vec3.atLowerCornerOf(entity.currentlyPullingFrom);
            var endPos = entity.getBlockPos().getCenter();
            var renderedBlock = entity.currentlyPulling;
            var offset = endPos.subtract(startPos).scale(1 - progress);
            var rotationY = progress * entity.pullTime * 3;
            
            matrices.pushPose();
            matrices.translate(0.5, 0.5, 0.5);
            matrices.mulPose(Axis.YP.rotationDegrees(rotationY));
            matrices.translate(-offset.x, -offset.y, -offset.z);
            matrices.mulPose(Axis.XP.rotationDegrees(rotationY));
            matrices.mulPose(Axis.ZP.rotationDegrees(rotationY));
            matrices.scale(1 - progress, 1 - progress, 1 - progress);
            
            Minecraft.getInstance().getBlockRenderer().renderBatched(
              renderedBlock,
              entity.getBlockPos(),
              entity.getLevel(),
              matrices,
              vertexConsumers.getBuffer(ItemBlockRenderTypes.getChunkRenderType(renderedBlock)),
              true,
              entity.getLevel().random
            );
            
            matrices.popPose();
            
        }
        
        renderBlackHole(entity, tickDelta, matrices, vertexConsumers);
    }
    
    private static void renderBlackHole(BlackHoleBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers) {
        var time = entity.getLevel().getGameTime() + tickDelta;
        var rotationY = (time * 1.2f) % 360;
        var rotationX = Math.sin(time * 0.02) * 5;
        
        matrices.pushPose();
        
        matrices.translate(0.5f, 0.5f, 0.5f);
        matrices.mulPose(Axis.YP.rotationDegrees(rotationY));
        matrices.mulPose(Axis.XP.rotationDegrees((float) rotationX));
        matrices.translate(-0.5f, -0.5f, -0.5f);
        
        Minecraft.getInstance().getBlockRenderer().renderBatched(
          BlockContent.BLACK_HOLE_INNER.defaultBlockState(),
          entity.getBlockPos(),
          entity.getLevel(),
          matrices,
          vertexConsumers.getBuffer(RenderType.endPortal()),
          true,
          entity.getLevel().random
        );
        
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
          BlockContent.BLACK_HOLE_MIDDLE.defaultBlockState(),
          matrices,
          vertexConsumers,
          LightTexture.FULL_BRIGHT,
          LightTexture.FULL_BRIGHT
        );
        
        matrices.popPose();
        matrices.pushPose();
        
        matrices.translate(0.5f, 0.5f, 0.5f);
        rotationY = (time * 1.1f) % 360;
        matrices.mulPose(Axis.YP.rotationDegrees(rotationY));
        matrices.mulPose(Axis.XP.rotationDegrees((float) rotationX));
        matrices.translate(-0.5f, -0.5f, -0.5f);
        
        Minecraft.getInstance().getBlockRenderer().renderBatched(
          BlockContent.BLACK_HOLE_OUTER.defaultBlockState(),
          entity.getBlockPos(),
          entity.getLevel(),
          matrices,
          vertexConsumers.getBuffer(ItemBlockRenderTypes.getChunkRenderType(BlockContent.BLACK_HOLE_OUTER.defaultBlockState())),
          true,
          entity.getLevel().random
        );
        
        matrices.popPose();
    }
}
