package rearth.oritech.client.renderers.models;

import com.geckolib.animatable.GeoAnimatable;
import rearth.oritech.block.entity.interaction.EndericLaserBlockEntity;
import rearth.oritech.util.Geometry;

public class EndericLaserModel<T extends EndericLaserBlockEntity & GeoAnimatable> extends MachineModel<T> {

    public EndericLaserModel(String subpath) {
        super(subpath);
    }

    public static float lerp(float a, float b, float f) {
        if (Math.abs(b - a) > 350 * Geometry.DEG_TO_RAD) return b;
        return a + f * (b - a);
    }
}
