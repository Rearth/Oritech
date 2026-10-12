package rearth.oritech.client.renderers.models;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedBlockGeoModel;
import net.minecraft.resources.Identifier;

/**
 * A block geo model whose entity-style textures are kept out of the block atlas.
 */
public class OritechBlockGeoModel<T extends GeoAnimatable> extends DefaultedBlockGeoModel<T> {

    private static final String MODEL_TEXTURE_PREFIX = "models/";

    public OritechBlockGeoModel(Identifier assetSubpath) {
        super(assetSubpath);
    }

    @Override
    public Identifier buildFormattedTexturePath(Identifier basePath) {
        if (basePath.getPath().startsWith(MODEL_TEXTURE_PREFIX)) {
            return basePath.withPath("textures/" + basePath.getPath() + ".png");
        }

        return super.buildFormattedTexturePath(basePath);
    }
}
