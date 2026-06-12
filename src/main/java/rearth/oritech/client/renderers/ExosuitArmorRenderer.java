package rearth.oritech.client.renderers;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.resources.Identifier;
import rearth.oritech.item.tools.armor.ExoArmorItem;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class ExosuitArmorRenderer extends GeoArmorRenderer<ExoArmorItem, HumanoidRenderState> {
    public ExosuitArmorRenderer(Identifier model, Identifier texture) {
        super(new DefaultedEntityGeoModel<ExoArmorItem>(model).withAltTexture(texture));
    }
}
