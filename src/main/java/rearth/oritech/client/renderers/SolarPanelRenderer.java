package rearth.oritech.client.renderers;

import rearth.oritech.block.entity.machines.generators.BigSolarPanelEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SolarPanelRenderer<T extends BigSolarPanelEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public SolarPanelRenderer(String modelPath) {
        super(new SolarPanelModel<>(modelPath));
    }
}


