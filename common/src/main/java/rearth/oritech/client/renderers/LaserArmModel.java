package rearth.oritech.client.renderers;

import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.util.Geometry;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class LaserArmModel<T extends LaserArmBlockEntity & GeoAnimatable> extends MachineModel<T> {
    
    private static final HashMap<Long, ModelRenderData> additionalData = new HashMap<>();
    private static final HashMap<Long, Vec3> drillOffsets = new HashMap<>();
    
    private Vec3 lastActivePlayerPos = Vec3.ZERO;
    
    public LaserArmModel(String subpath) {
        super(subpath);
    }
    
    private ModelRenderData getById(long id) {
        return additionalData.computeIfAbsent(id, s -> new ModelRenderData(0, 0, getAnimationProcessor().getBone("pivotX"), getAnimationProcessor().getBone("pivotY")));
    }
    
    private Vec3 getOffsetByDrillId(long id, T laserEntity) {
        return drillOffsets.computeIfAbsent(id, s -> {
            var drillFacing = laserEntity.getLevel().getBlockState(laserEntity.getCurrentTarget()).getValue(BlockStateProperties.HORIZONTAL_FACING);
            return Geometry.rotatePosition(new Vec3(1, 1.4, 0), drillFacing);
        });
    }
    
    @Override
    public void setCustomAnimations(T laserEntity, long instanceId, AnimationState<T> animationState) {
        
        Vec3 target;
        var isIdle = false;
        if (laserEntity.getCurrentTarget() == null || laserEntity.getCurrentTarget().closerThan(BlockPos.ZERO, 0.1f))  {
            target = getIdleTarget(laserEntity);
            isIdle = true;
        } else {
            target = laserEntity.getVisualTarget();
        }
        
        if (target == null || target == Vec3.ZERO) return;
        
        if (!isIdle && laserEntity.isTargetingDeepdrill(laserEntity.getLevel().getBlockState(laserEntity.getCurrentTarget()).getBlock())) {
            var drillId = laserEntity.getCurrentTarget().asLong();
            var offset = getOffsetByDrillId(drillId, laserEntity);
            target = target.add(offset);
        }
        
        var ownPos = laserEntity.laserHead;
        var facing = laserEntity.getBlockState().getValue(BlockStateProperties.FACING);
        var offset = Geometry.worldToOffsetPosition(facing, target, ownPos);
        
        // thanks to: https://math.stackexchange.com/questions/878785/how-to-find-an-angle-in-range0-360-between-2-vectors
        var offsetY = new Vector2f((float) offset.x(), (float) offset.y());
        var forwardY = new Vector2f(0, -1);
        if (facing == Direction.NORTH)
            forwardY = new Vector2f(0, 1);
        if (facing == Direction.WEST)
            forwardY = new Vector2f(1, 0);
        if (facing == Direction.EAST)
            forwardY = new Vector2f(-1, 0);
        var angleY = -offsetY.angle(forwardY);
        
        // to create a 2d vector in a plane based on normal angleY
        var lengthY = offsetY.length();
        var heightDiff = offset.z();
        
        var offsetX = new Vector2f(lengthY, (float) heightDiff);
        var forwardX = new Vector2f(0, 1);
        var detX = determinant(offsetX, forwardX);
        var dotX = offsetX.dot(forwardX);
        var angleX = Math.atan2(detX, dotX);
        
        angleX -= 47.5 * Geometry.DEG_TO_RAD; //to offset for parent bone rotations
        
        var data = getById(instanceId);
        
        if (data.boneX != null) {
            var newRotY = lerp(data.angleY, angleY, 0.06f);
            var newRotX = lerp(data.angleX, (float) angleX, 0.06f);
            data.boneY.setRotY(newRotY);
            data.boneX.setRotX(newRotX);
            
            data.angleY = newRotY;
            data.angleX = newRotX;
        }
        
    }
    
    private Vec3 getIdleTarget(T entity) {
        
        var offsetA = new Vec3(0, Math.pow(Math.sin(entity.getLevel().getGameTime() / 40f), 3), 0);
        var offsetB = new Vec3(Math.pow(Math.sin(entity.getLevel().getGameTime() / 40f + 1.3f), 3), 0, 0);
        
        if (entity.getLevel().getRandom().nextFloat() > 0.9f) {
             lastActivePlayerPos = Minecraft.getInstance().player.getEyePosition();
        }
        
        if (lastActivePlayerPos.equals(Vec3.ZERO))
            return Vec3.ZERO;
        
        // return lastActivePlayerPos;
        
        return lastActivePlayerPos.add(offsetA).add(offsetB);
    }
    
    public static float lerp(float a, float b, float f) {
        if (Math.abs(b-a) > 350 * Geometry.DEG_TO_RAD) return b;
        return a + f * (b - a);
    }
    
    public static float determinant(Vector2f a, Vector2f b) {
        return a.x * b.y - a.y * b.x;
    }
    
    private static class ModelRenderData {
        protected float angleY;
        protected float angleX;
        protected GeoBone boneX;
        protected GeoBone boneY;
        
        public ModelRenderData(float angleX, float angleY, GeoBone boneX, GeoBone boneY) {
            this.angleY = angleY;
            this.angleX = angleX;
            this.boneX = boneX;
            this.boneY = boneY;
        }
    }
}
