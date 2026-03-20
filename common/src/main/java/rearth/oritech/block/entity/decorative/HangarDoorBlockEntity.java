package rearth.oritech.block.entity.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.decorative.HangarDoorBlock;
import rearth.oritech.init.BlockEntitiesContent;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HangarDoorBlockEntity extends BlockEntity implements GeoBlockEntity {

    public static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("retract");
    public static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("deploy");

    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<HangarDoorBlockEntity> animationController = getAnimationController();

    private long lastSoundEventAt = 0;

    public HangarDoorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.HANGAR_DOOR_ENTITY, pos, state);
    }

    public boolean shouldPlaySoundAgain() {
        var age = level.getGameTime() - lastSoundEventAt;
        lastSoundEventAt = level.getGameTime();
        return age > 40;
    }

    private AnimationController<HangarDoorBlockEntity> getAnimationController() {
        return new AnimationController<>(this, state -> {
            if (state.getController().getCurrentAnimation() == null) {
                state.getController().setAnimation(MachineBlockEntity.IDLE);
            }

            return getBlockState().getValue(HangarDoorBlock.OPENED) ? state.setAndContinue(OPEN) : state.setAndContinue(CLOSE);
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(animationController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
}