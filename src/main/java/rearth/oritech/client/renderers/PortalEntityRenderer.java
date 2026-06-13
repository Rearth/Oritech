package rearth.oritech.client.renderers;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import rearth.oritech.client.renderers.models.PortalEntityModel;
import rearth.oritech.util.PortalEntity;

public class PortalEntityRenderer extends GeoEntityRenderer<PortalEntity, PortalEntityRenderer.PortalEntityState> {

    public PortalEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PortalEntityModel());
        withRenderLayer(new PortalRenderLayer(this));
    }

    @Override
    public void extractRenderState(PortalEntity animatable, PortalEntityState state, float partialTick) {
        super.extractRenderState(animatable, state, partialTick);
        state.yRot = animatable.getYRot();
    }

    @Override
    protected void applyRotations(RenderPassInfo<PortalEntityState> renderPassInfo, PoseStack poseStack, float rotationYaw) {
        super.applyRotations(renderPassInfo, poseStack, rotationYaw);
        poseStack.mulPose(Axis.YP.rotationDegrees(renderPassInfo.renderState().yRot));
    }

    public static class PortalEntityState extends EntityRenderState implements GeoRenderState {
        public float yRot;
        private final java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, java.lang.Object> dataMap = new java.util.HashMap<>();

        @Override
        public java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, java.lang.Object> getDataMap() {
            return dataMap;
        }
    }

    public static class PortalRenderLayer extends GeoRenderLayer<PortalEntity, java.lang.Void, PortalEntityState> {

        public PortalRenderLayer(GeoRenderer<PortalEntity, java.lang.Void, PortalEntityState> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void addPerBoneRender(RenderPassInfo<PortalEntityState> info, java.util.function.BiConsumer<GeoBone, com.geckolib.renderer.base.PerBoneRender<PortalEntityState>> perBoneRenders) {
            info.model().getBone("portal").ifPresent(bone -> {
                perBoneRenders.accept(bone, (passInfo, b, collector) -> {
                    collector.submitCustomGeometry(passInfo.poseStack(), RenderTypes.endGateway(), (pose, consumer) -> {
                        consumer.addVertex(pose.pose(), 0, 0, 0.55f);
                        consumer.addVertex(pose.pose(), 0, 1.95f, 0.55f);
                        consumer.addVertex(pose.pose(), 0, 1.95f, -0.55f);
                        consumer.addVertex(pose.pose(), 0, 0, -0.55f);
                    });
                });
            });
        }
    }

}
