package rearth.oritech.client.renderers.blocks;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoBlockRenderer;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;
import rearth.oritech.client.renderers.models.SolarPanelModel;

public class SolarPanelRenderer<T extends BigSolarPanelEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public SolarPanelRenderer(String modelPath) {
        super(new SolarPanelModel<>(modelPath));
    }
}


