package rearth.oritech.client.renderers;

import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.PulverizerBlockEntity;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class PulverizerRenderer extends GeoBlockRenderer<PulverizerBlockEntity> {
    public PulverizerRenderer() {
        super(new PulverizerModel());
    }

    public static class PulverizerModel extends DefaultedBlockGeoModel<PulverizerBlockEntity> {
        public PulverizerModel() {
            super(new Identifier(Oritech.MOD_ID, "models/gem_station_anim"));
        }
    }

}
