package rearth.oritech.client.renderers;

import rearth.oritech.Oritech;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class PromethiumToolRenderer extends GeoItemRenderer<PromethiumPickaxeItem> {
    public PromethiumToolRenderer(String modelName) {
        this(modelName, false);
    }
    
    public PromethiumToolRenderer(String modelName, boolean glowing) {
        super(new PromethiumToolModel(Oritech.id("models/" + modelName)));
        
        if (glowing) {
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }
}
