package rearth.oritech.client.renderers;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoBlockRenderer;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;

public class SolarPanelRenderer<T extends BigSolarPanelEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public SolarPanelRenderer(String modelPath) {
        super(new SolarPanelModel<>(modelPath));
    }
}


