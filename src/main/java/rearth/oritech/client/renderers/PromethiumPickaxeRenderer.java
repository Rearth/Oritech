package rearth.oritech.client.renderers;

import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PromethiumPickaxeRenderer extends GeoItemRenderer<PromethiumPickaxeItem> {
    public PromethiumPickaxeRenderer() {
        super(new PromethiumPickaxeModel(new Identifier(Oritech.MOD_ID, "models/promethium_pickaxe")));
    }
}
