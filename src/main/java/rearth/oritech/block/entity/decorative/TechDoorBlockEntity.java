package rearth.oritech.block.entity.decorative;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.block.blocks.decorative.TechDoorBlock;
import rearth.oritech.init.BlockEntitiesContent;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Timer;
import java.util.TimerTask;

public class TechDoorBlockEntity extends BlockEntity implements GeoBlockEntity {
    
    public static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("door_open");
    public static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("door_close");
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<TechDoorBlockEntity> animationController = getAnimationController();
    
    private long lastSoundEventAt = 0;
    
    public TechDoorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.TECH_DOOR_ENTITY, pos, state);
    }
    
    public boolean shouldPlaySoundAgain() {
        var age = world.getTime() - lastSoundEventAt;
        lastSoundEventAt = world.getTime();
        
        return age > 40;
    }
    
    private AnimationController<TechDoorBlockEntity> getAnimationController() {
        return new AnimationController<>(this, state -> {
            
            // increase animation speed when newly loaded in, to avoid visible animation when initializing
            if (state.getController().getCurrentAnimation() == null) {
                state.getController().setAnimationSpeed(100);
                delayedTimerReset(state);
            }
            
            var opened = getCachedState().get(TechDoorBlock.OPENED);
            if (opened) {
                return state.setAndContinue(OPEN);
            } else {
                return state.setAndContinue(CLOSE);
            }
            
        });
    }
    
    private static void delayedTimerReset(AnimationState<TechDoorBlockEntity> state) {
        new Timer().schedule(
          new TimerTask() {
              
              @Override
              public void run() {
                  state.getController().setAnimationSpeed(1);
              }
          }, 1000
        );
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
