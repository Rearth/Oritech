package rearth.oritech.client.renderers;

import rearth.oritech.Oritech;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;
import rearth.oritech.util.Geometry;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import java.util.HashMap;
import net.minecraft.util.Tuple;

public class SolarPanelModel<T extends BigSolarPanelEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {
    
    private final HashMap<Long, Tuple<GeoBone, Float>> renderData = new HashMap<>();
    
    private Tuple<GeoBone, Float> getDataFromCache(long id) {
        return renderData.computeIfAbsent(id, s -> new Tuple<>(getAnimationProcessor().getBone("pivotZ"), 0f));
    }
    
    public SolarPanelModel(String subpath) {
        super(Oritech.id(subpath));
    }
    
    @Override
    public void setCustomAnimations(T solarEntity, long instanceId, AnimationState<T> animationState) {
        
        var timeOfDay = solarEntity.getAdjustedTimeOfDay();
        var data = getDataFromCache(instanceId);
        if (timeOfDay > 13000) {
            data.setB(0f);
        }
        
        var directionPercent = (timeOfDay - 6000) / 6000f;
        var maxAngle = 45;
        var targetAngle = directionPercent * maxAngle * Geometry.DEG_TO_RAD;
        var lastAngle = data.getB();
        var angle = LaserArmModel.lerp(lastAngle, targetAngle, 0.06f);
        var bone = data.getA();
        bone.setRotZ(angle);
        data.setB(angle);
        
    }
}
