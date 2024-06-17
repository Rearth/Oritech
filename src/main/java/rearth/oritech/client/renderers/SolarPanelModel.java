package rearth.oritech.client.renderers;

import net.minecraft.util.Pair;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.generators.BigSolarPanelEntity;
import rearth.oritech.util.Geometry;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import java.util.HashMap;

public class SolarPanelModel<T extends BigSolarPanelEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {
    
    private final HashMap<Long, Pair<GeoBone, Float>> renderData = new HashMap<>();
    
    private Pair<GeoBone, Float> getDataFromCache(long id) {
        return renderData.computeIfAbsent(id, s -> new Pair<>(getAnimationProcessor().getBone("pivotZ"), 0f));
    }
    
    public SolarPanelModel(String subpath) {
        super(Oritech.id(subpath));
    }
    
    @Override
    public void setCustomAnimations(T solarEntity, long instanceId, AnimationState<T> animationState) {
        
        var timeOfDay = solarEntity.getAdjustedTimeOfDay();
        var data = getDataFromCache(instanceId);
        if (timeOfDay > 13000) {
            data.setRight(0f);
        }
        
        var directionPercent = (timeOfDay - 6000) / 6000f;
        var maxAngle = 45;
        var targetAngle = directionPercent * maxAngle * Geometry.DEG_TO_RAD;
        var lastAngle = data.getRight();
        var angle = LaserArmModel.lerp(lastAngle, targetAngle, 0.06f);
        var bone = data.getLeft();
        bone.setRotZ(angle);
        data.setRight(angle);
        
    }
}
