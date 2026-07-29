package rearth.oritech.util;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.augmenter.CyberneticAugmentationCenterBlock;

import java.util.Set;

public class PortalEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache instanceCache = GeckoLibUtil.createInstanceCache(this);

    public GlobalPos target;
    protected static final RawAnimation PORTAL = RawAnimation.begin().thenPlay("create").thenLoop("idle");


    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);

    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return true;
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide()) return;

        if (target != null) {
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            ServerLevel targetWorld = level().getServer().getLevel(target.dimension());

            if (targetWorld != null) {
                BlockPos targetPos = target.pos();
                Vec3 centerPos = targetPos.getCenter();

                CyberneticAugmentationCenterBlock.lastTeleportedPlayer = new Tuple<>(targetWorld.getGameTime(), serverPlayer);

                serverPlayer.teleportTo(
                        targetWorld,
                        centerPos.x, centerPos.y, centerPos.z,
                        Set.of(),
                        serverPlayer.getYRot(), serverPlayer.getXRot(),
                        false
                );
            } else {
                Oritech.LOGGER.warn("Attempted to teleport player to non-existent dimension: {}", target.dimension().identifier());
            }
        }

        this.remove(RemovalReason.DISCARDED);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {

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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("portal", state -> state.setAndContinue(PORTAL)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return instanceCache;
    }
}
