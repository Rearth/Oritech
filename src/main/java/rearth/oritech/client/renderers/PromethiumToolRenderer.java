package rearth.oritech.client.renderers;

import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PromethiumToolRenderer extends GeoItemRenderer<PromethiumPickaxeItem> {
    public PromethiumToolRenderer(String modelName) {
        super(new PromethiumToolModel(Oritech.id("models/" + modelName)));
    }
}
