package rearth.oritech.client.renderers;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import rearth.oritech.util.PortalEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PortalEntityRenderer extends GeoEntityRenderer<PortalEntity> {
    public PortalEntityRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new PortalEntityModel());
    }
    
    @Override
    protected void applyRotations(PortalEntity animatable, MatrixStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(animatable.getYaw()));
    }
}
