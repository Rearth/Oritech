package rearth.oritech.client.renderers;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import rearth.oritech.client.renderers.models.PortalEntityModel;
import rearth.oritech.util.PortalEntity;

import java.util.function.BiConsumer;

public class PortalEntityRenderer extends GeoEntityRenderer<PortalEntity, EntityRenderState> {

    private static final DataTicket<Float> Y_ROTATION = DataTicket.create("oritech_portal_y_rotation", Float.class);

    public PortalEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PortalEntityModel());
        withRenderLayer(new PortalRenderLayer(this));
    }

    @Override
    public void addRenderData(PortalEntity animatable, Void relatedObject, EntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(Y_ROTATION, animatable.getYRot());
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> renderPassInfo, PoseStack poseStack, float rotationYaw) {
        super.applyRotations(renderPassInfo, poseStack, rotationYaw);
        var yRotation = renderPassInfo.renderState().getOrDefaultGeckolibData(Y_ROTATION, 0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
    }

    public static class PortalRenderLayer extends GeoRenderLayer<PortalEntity, Void, EntityRenderState> {

        public PortalRenderLayer(GeoRenderer<PortalEntity, Void, EntityRenderState> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void addPerBoneRender(RenderPassInfo<EntityRenderState> info, BiConsumer<GeoBone, PerBoneRender<EntityRenderState>> perBoneRenders) {
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
