package rearth.oritech.client.renderers.models;

import com.geckolib.animatable.GeoAnimatable;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.util.Geometry;

public class LaserArmModel<T extends LaserArmBlockEntity & GeoAnimatable> extends MachineModel<T> {

    public LaserArmModel(String subpath) {
        super(subpath);
    }

    public static float lerp(float a, float b, float f) {
        if (Math.abs(b - a) > 350 * Geometry.DEG_TO_RAD) return b;
        return a + f * (b - a);
    }
}
