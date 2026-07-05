package rearth.oritech.client.renderers.models;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedItemGeoModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class PromethiumToolModel<T extends Item & GeoAnimatable> extends DefaultedItemGeoModel<T> {

    public PromethiumToolModel(Identifier assetSubpath) {
        super(assetSubpath);
    }
}
