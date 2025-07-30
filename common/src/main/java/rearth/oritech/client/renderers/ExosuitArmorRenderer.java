package rearth.oritech.client.renderers;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.item.tools.armor.ExoArmorItem;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ExosuitArmorRenderer extends GeoArmorRenderer<ExoArmorItem> {
    public ExosuitArmorRenderer(ResourceLocation model, ResourceLocation texture) {
        super(new DefaultedEntityGeoModel<ExoArmorItem>(model).withAltTexture(texture));
    }
}
