package rearth.oritech.client.renderers;

import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.item.tools.PortableLaserItem;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

public class PortableLaserModel extends DefaultedItemGeoModel<PortableLaserItem> {
    
    public PortableLaserModel(Identifier assetSubpath) {
        super(assetSubpath);
    }
}
