package rearth.oritech.client.renderers;

import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import rearth.oritech.util.PortalEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PortalEntityRenderer extends GeoEntityRenderer<PortalEntity> {
    public PortalEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new PortalEntityModel());
        
        addRenderLayer(new PortalRenderLayer(this));
        
    }
    
    @Override
    protected void applyRotations(PortalEntity animatable, MatrixStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(animatable.getYaw()));
    }
    
    public static class PortalRenderLayer extends GeoRenderLayer<PortalEntity> {
        
        public PortalRenderLayer(GeoRenderer<PortalEntity> entityRendererIn) {
            super(entityRendererIn);
        }
        
        @Override
        public void renderForBone(MatrixStack poseStack, PortalEntity animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            
            if (!bone.getName().equals("portal")) return;
            
            var layer = RenderLayer.of("portal_swirl", VertexFormats.POSITION, VertexFormat.DrawMode.QUADS, 1536, false, false, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.END_GATEWAY_PROGRAM).texture(RenderPhase.Textures.create().add(EndPortalBlockEntityRenderer.SKY_TEXTURE, false, false).add(Identifier.ofVanilla("textures/environment/moon_phases.png"), false, false).build()).build(false));
            
            var consumer = bufferSource.getBuffer(layer);
            
            consumer.vertex(poseStack.peek().getPositionMatrix(), 0, 0, 0.55f);
            consumer.vertex(poseStack.peek().getPositionMatrix(), 0, 1.95f, 0.55f);
            consumer.vertex(poseStack.peek().getPositionMatrix(), 0, 1.95f, -0.55f);
            consumer.vertex(poseStack.peek().getPositionMatrix(), 0, 0, -0.55f);
            
        }
    }
    
}
