package rearth.oritech.client.renderers.models;

import com.geckolib.animatable.GeoAnimatable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;

public class SolarPanelModel<T extends BigSolarPanelEntity & GeoAnimatable> extends OritechBlockGeoModel<T> {

    public SolarPanelModel(String subpath) {
        super(Oritech.id(subpath));
    }
}
