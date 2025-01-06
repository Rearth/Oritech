package rearth.oritech.client.renderers;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import rearth.oritech.item.tools.armor.ExoArmorItem;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ExosuitArmorRenderer extends GeoArmorRenderer<ExoArmorItem> {
    public ExosuitArmorRenderer(Identifier model, Identifier texture) {
        super(new DefaultedEntityGeoModel<ExoArmorItem>(model).withAltTexture(texture));
    }
}
