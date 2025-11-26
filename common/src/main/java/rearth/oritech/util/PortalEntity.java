package rearth.oritech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
    
    
    public PortalEntity(EntityType<?> type, Level world) {
        super(type, world);
        
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    
    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide) return;

        if (target != null) {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            
            ServerLevel targetWorld = this.getServer().getLevel(target.dimension());

            if (targetWorld != null) {
                BlockPos targetPos = target.pos();
                Vec3 centerPos = targetPos.getCenter();
                
                AugmentApplicationBlock.lastTeleportedPlayer = new Tuple<>(targetWorld.getGameTime(), serverPlayer);

                serverPlayer.teleportTo(
                    targetWorld,
                    centerPos.x, centerPos.y, centerPos.z,
                    serverPlayer.getYRot(), serverPlayer.getXRot()
                );
            } else {
                Oritech.LOGGER.warn("Attempted to teleport player to non-existent dimension: {}", target.dimension().location());
            }
        }
        
        this.remove(RemovalReason.DISCARDED);
    }
    
    @Override
    public void tick() {
        var world = this.level();
        if (world.isClientSide) return;
        
        tickCount++;
        
        if (tickCount > 100) {
            this.remove(RemovalReason.DISCARDED);
        }
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
    
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
    
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
