package rearth.oritech.client.renderers;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.models.PromethiumToolModel;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;

public class PromethiumToolRenderer extends GeoItemRenderer<PromethiumPickaxeItem> {
    public PromethiumToolRenderer(String modelName) {
        this(modelName, false);
    }

    public PromethiumToolRenderer(String modelName, boolean glowing) {
        super(new PromethiumToolModel(Oritech.id("models/" + modelName)));

        if (glowing) {
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }
}
