package rearth.oritech.client.renderers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import rearth.oritech.block.entity.machines.accelerator.BlackHoleBlockEntity;
import rearth.oritech.init.BlockContent;

public class BlackHoleRenderer implements BlockEntityRenderer<BlackHoleBlockEntity> {
    
    //    return of("energy_swirl", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS, 1536, false, true,                                           RenderLayer.MultiPhaseParameters.builder().program(ENERGY_SWIRL_PROGRAM).texture(new RenderPhase.Texture(texture, false, false)).texturing(new RenderPhase.OffsetTexturing(x, y)).transparency(ADDITIVE_TRANSPARENCY).cull(DISABLE_CULLING).lightmap(ENABLE_LIGHTMAP).overlay(ENABLE_OVERLAY_COLOR).build(false));
    //
    private static final RenderLayer.MultiPhase HOLE_GLOW = RenderLayer.of("energy_swirl",VertexFormats.POSITION, VertexFormat.DrawMode.QUADS, 1536, false, true, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.TRANSLUCENT_PROGRAM).texture(RenderPhase.Textures.create().add(EndPortalBlockEntityRenderer.SKY_TEXTURE, false, false).add(EndPortalBlockEntityRenderer.PORTAL_TEXTURE, false, false).build()).transparency(RenderPhase.Transparency.ADDITIVE_TRANSPARENCY).overlay(RenderPhase.ENABLE_OVERLAY_COLOR).build(false));
    
    @Override
    public void render(BlackHoleBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        
        
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
