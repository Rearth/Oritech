package rearth.oritech.util;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
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

public class PortalEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache instanceCache = GeckoLibUtil.createInstanceCache(this);

    private final int age = 0;

    public GlobalPos target;
    protected static final RawAnimation PORTAL = RawAnimation.begin().thenPlay("create").thenLoop("idle");


    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);

    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide()) return;

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
        var level = this.level();
        if (level.isClientSide()) return;

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
