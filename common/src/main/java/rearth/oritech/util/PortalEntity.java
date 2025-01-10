package rearth.oritech.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PortalEntity extends Entity implements GeoEntity {
    
    private AnimatableInstanceCache instanceCache = GeckoLibUtil.createInstanceCache(this);
    
    private int age = 0;
    
    public Vec3d target;
    
    public PortalEntity(EntityType<?> type, World world) {
        super(type, world);
        
    }
    
    @Override
    public boolean isCollidable() {
        return true;
    }
    
    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (target != null) {
            player.teleport(target.x, target.y, target.z, true);
        }
        
        this.remove(RemovalReason.DISCARDED);
    }
    
    @Override
    public void tick() {
        var world = this.getWorld();
        if (world.isClient) return;
        
        age++;
        
        if (age > 100) {
            this.remove(RemovalReason.DISCARDED);
        }
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    
    }
    
    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    
    }
    
    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return instanceCache;
    }
}
