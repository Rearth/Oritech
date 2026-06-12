package rearth.oritech.client.renderers.models;

import com.geckolib.model.GeoModel;
import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.util.PortalEntity;

public class PortalEntityModel extends GeoModel<PortalEntity> {
    @Override
    public Identifier getModelResource(PortalEntity animatable) {
        return Oritech.id("geo/entity/portal.geo.json");
    }

    @Override
    public Identifier getTextureResource(PortalEntity animatable) {
        return Oritech.id("textures/entity/portal.png");
    }

    @Override
    public Identifier getAnimationResource(PortalEntity animatable) {
        return Oritech.id("animations/entity/portal.animation.json");
    }
}
