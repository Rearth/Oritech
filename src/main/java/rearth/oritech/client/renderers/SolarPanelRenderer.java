package rearth.oritech.client.renderers;

import rearth.oritech.block.entity.generators.BigSolarPanelEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoBlockRenderer;

public class SolarPanelRenderer<T extends BigSolarPanelEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public SolarPanelRenderer(String modelPath) {
        super(new SolarPanelModel<>(modelPath));
    }
}


