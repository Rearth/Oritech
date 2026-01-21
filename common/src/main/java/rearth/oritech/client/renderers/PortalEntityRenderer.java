package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import rearth.oritech.util.PortalEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class PortalEntityRenderer extends GeoEntityRenderer<PortalEntity> {
    public PortalEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PortalEntityModel());
        
        addRenderLayer(new PortalRenderLayer(this));
        
    }
    
    @Override
    protected void applyRotations(PortalEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.mulPose(Axis.YP.rotationDegrees(animatable.getYRot()));
    }
    
    public static class PortalRenderLayer extends GeoRenderLayer<PortalEntity> {
        
        public PortalRenderLayer(GeoRenderer<PortalEntity> entityRendererIn) {
            super(entityRendererIn);
        }
        
        @Override
        public void renderForBone(PoseStack poseStack, PortalEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            super.renderForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            
            if (!bone.getName().equals("portal")) return;
            
            var layer = RenderType.create("portal_swirl", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 1536, false, false, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_END_GATEWAY_SHADER).setTextureState(RenderStateShard.MultiTextureStateShard.builder().add(TheEndPortalRenderer.END_SKY_LOCATION, false, false).add(ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png"), false, false).build()).createCompositeState(false));
            
            var consumer = bufferSource.getBuffer(layer);
            
            consumer.addVertex(poseStack.last().pose(), 0, 0, 0.55f);
            consumer.addVertex(poseStack.last().pose(), 0, 1.95f, 0.55f);
            consumer.addVertex(poseStack.last().pose(), 0, 1.95f, -0.55f);
            consumer.addVertex(poseStack.last().pose(), 0, 0, -0.55f);
            
        }
    }
    
}
