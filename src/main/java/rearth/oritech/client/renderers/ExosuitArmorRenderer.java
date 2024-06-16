package rearth.oritech.client.renderers;

import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.item.tools.armor.ExoArmorItem;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ExosuitArmorRenderer extends GeoArmorRenderer<ExoArmorItem> {
    public ExosuitArmorRenderer() {
        super(new DefaultedEntityGeoModel<>(Oritech.id("armor/exo_armor")));
    }
}
