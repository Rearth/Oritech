package rearth.oritech.client.renderers;

import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.tools.armor.ExoArmorItem;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ExosuitArmorRenderer extends GeoArmorRenderer<ExoArmorItem> {
    public ExosuitArmorRenderer() {
        super(new DefaultedEntityGeoModel<>(new Identifier(Oritech.MOD_ID, "armor/exo_armor")));
    }
}
