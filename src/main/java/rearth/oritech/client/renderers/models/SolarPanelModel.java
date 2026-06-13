package rearth.oritech.client.renderers.models;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedBlockGeoModel;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;

public class SolarPanelModel<T extends BigSolarPanelEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {

    public SolarPanelModel(String subpath) {
        super(Oritech.id(subpath));
    }
}
