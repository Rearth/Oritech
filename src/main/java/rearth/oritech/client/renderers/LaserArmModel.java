package rearth.oritech.client.renderers;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import org.joml.Vector3f;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import rearth.oritech.util.Geometry;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import java.util.HashMap;

public class LaserArmModel<T extends LaserArmBlockEntity & GeoAnimatable> extends DefaultedBlockGeoModel<T> {
    
    private static final HashMap<Long, ModelRenderData> additionalData = new HashMap<>();
    
    public LaserArmModel(String subpath) {
        super(new Identifier(Oritech.MOD_ID, subpath));
    }
    
    private ModelRenderData getById(long id) {
        return additionalData.computeIfAbsent(id, s -> new ModelRenderData(0, 0, getAnimationProcessor().getBone("pivotX"), getAnimationProcessor().getBone("pivotY")));
    }
    
    @Override
    public void setCustomAnimations(T laserEntity, long instanceId, AnimationState<T> animationState) {
        
        if (laserEntity.getTarget() == null) return;
        var target = Vec3d.of(laserEntity.getTarget());
        var ownPos = Vec3d.of(laserEntity.getPos());
        var offset = target.subtract(ownPos.add(0, 1.55 - 0.5, 0)); // add 1.55 to get to height of pivotX, minus block center offset
        
        // thanks to: https://math.stackexchange.com/questions/878785/how-to-find-an-angle-in-range0-360-between-2-vectors
        var offsetY = new Vector2f((float) offset.getX(), (float) offset.getZ());
        var forwardY = new Vector2f(0, 1);
        var detY = determinant(offsetY, forwardY);
        var dotY = offsetY.dot(forwardY);
        var angleY = Math.atan2(detY, dotY);
        
        // to create a 2d vector in a plane based on normal angleY
        var lengthY = offsetY.length();
        var heightDiff = offset.getY();
        
        var offsetX = new Vector2f(lengthY, (float) heightDiff);
        var forwardX = new Vector2f(0, 1);
        var detX = determinant(offsetX, forwardX);
        var dotX = offsetX.dot(forwardX);
        var angleX = Math.atan2(detX, dotX);
        
        angleX -= 42.5 * Geometry.DEG_TO_RAD; //to offset for parent bone rotations
        
        var data = getById(instanceId);
        
        if (data.boneX != null) {
            var newRotY = lerp(data.angleY, (float) angleY, 0.2f);
            var newRotX = lerp(data.angleX, (float) angleX, 0.2f);
            data.boneY.setRotY(newRotY);
            data.boneX.setRotX(newRotX);
            
            data.angleY = newRotY;
            data.angleX = newRotX;
        }
        
    }
    
    private static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }
    
    private static float determinant(Vector2f a, Vector2f b) {
        return a.x * b.y - a.y * b.x;
    }
    
    private static class ModelRenderData {
        protected float angleY;
        protected float angleX;
        protected CoreGeoBone boneX;
        protected CoreGeoBone boneY;
        
        public ModelRenderData(float angleX, float angleY, CoreGeoBone boneX, CoreGeoBone boneY) {
            this.angleY = angleY;
            this.angleX = angleX;
            this.boneX = boneX;
            this.boneY = boneY;
        }
    }
}
