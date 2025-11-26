package rearth.oritech.client.renderers;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.Oritech;
import rearth.oritech.util.PortalEntity;
import software.bernie.geckolib.model.GeoModel;

public class PortalEntityModel extends GeoModel<PortalEntity> {
    @Override
    public ResourceLocation getModelResource(PortalEntity animatable) {
        return Oritech.id("geo/entity/portal.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(PortalEntity animatable) {
        return Oritech.id("textures/entity/portal.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(PortalEntity animatable) {
        return Oritech.id("animations/entity/portal.animation.json");
    }
}
