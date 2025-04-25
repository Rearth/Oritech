package rearth.oritech.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.augmenter.AugmentApplicationBlock;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PortalEntity extends Entity implements GeoEntity {
    
    private final AnimatableInstanceCache instanceCache = GeckoLibUtil.createInstanceCache(this);
    
    private int age = 0;
    
    public GlobalPos target;
    protected static final RawAnimation PORTAL = RawAnimation.begin().thenPlay("create").thenLoop("idle");
    
    
    public PortalEntity(EntityType<?> type, World world) {
        super(type, world);
        
    }
    
    @Override
    public boolean isCollidable() {
        return true;
    }
    
    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (getWorld().isClient) return;

        if (target != null) {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
            
            ServerWorld targetWorld = this.getServer().getWorld(target.dimension());

            if (targetWorld != null) {
                BlockPos targetPos = target.pos();
                Vec3d centerPos = targetPos.toCenterPos();
                
                AugmentApplicationBlock.lastTeleportedPlayer = new Pair<>(targetWorld.getTime(), serverPlayer);

                serverPlayer.teleport(
                    targetWorld,
                    centerPos.x, centerPos.y, centerPos.z,
                    serverPlayer.getYaw(), serverPlayer.getPitch()
                );
            } else {
                Oritech.LOGGER.warn("Attempted to teleport player to non-existent dimension: {}", target.dimension().getValue());
            }
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
        controllers.add(new AnimationController<>(this, state -> state.setAndContinue(PORTAL)));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return instanceCache;
    }
}
