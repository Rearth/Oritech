package rearth.oritech.client.renderers;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import rearth.oritech.Oritech;
import rearth.oritech.util.PortalEntity;

import java.util.function.BiConsumer;

public class PortalEntityRenderer extends GeoEntityRenderer<PortalEntity, EntityRenderState> {

    public PortalEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Oritech.id("portal")));
        withRenderLayer(new PortalRenderLayer(this));
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
                        consumer.addVertex(pose.pose(), 0, 0, 1.05f);
                        consumer.addVertex(pose.pose(), 0, 1.85f, 1.05f);
                        consumer.addVertex(pose.pose(), 0, 1.85f, -0.1f);
                        consumer.addVertex(pose.pose(), 0, 0, -0.1f);
                    });
                });
            });
        }
    }

}
