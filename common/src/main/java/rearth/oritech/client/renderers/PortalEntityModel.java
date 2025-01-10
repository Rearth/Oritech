package rearth.oritech.client.renderers;

import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.util.PortalEntity;
import software.bernie.geckolib.model.GeoModel;

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
        return null;
    }
}
