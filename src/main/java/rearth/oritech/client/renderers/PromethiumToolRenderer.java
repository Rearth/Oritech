package rearth.oritech.client.renderers;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.world.item.Item;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.models.PromethiumToolModel;

public class PromethiumToolRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {
    public PromethiumToolRenderer(String modelName) {
        this(modelName, false);
    }

    public PromethiumToolRenderer(String modelName, boolean glowing) {
        super(new PromethiumToolModel<>(Oritech.id("models/" + modelName)));

        if (glowing) {
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }
}
