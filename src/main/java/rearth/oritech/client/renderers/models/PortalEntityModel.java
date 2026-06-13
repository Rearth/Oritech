package rearth.oritech.client.renderers.models;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.util.PortalEntity;

public class PortalEntityModel extends GeoModel<PortalEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Oritech.id("geo/entity/portal.geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Oritech.id("textures/entity/portal.png");
    }

    @Override
    public Identifier getAnimationResource(PortalEntity animatable) {
        return Oritech.id("animations/entity/portal.animation.json");
    }
}
