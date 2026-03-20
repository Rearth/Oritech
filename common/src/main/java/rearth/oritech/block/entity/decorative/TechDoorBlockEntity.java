package rearth.oritech.block.entity.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.decorative.TechDoorBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ColorableMachine;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Timer;
import java.util.TimerTask;

public class TechDoorBlockEntity extends BlockEntity implements GeoBlockEntity, ColorableMachine {
    
    public static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("door_open");
    public static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("door_close");
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<TechDoorBlockEntity> animationController = getAnimationController();
    private ColorVariant currentColor = getDefaultColor();
    
    private long lastSoundEventAt = 0;
    
    public TechDoorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.TECH_DOOR_ENTITY, pos, state);
    }
    
    public boolean shouldPlaySoundAgain() {
        var age = level.getGameTime() - lastSoundEventAt;
        lastSoundEventAt = level.getGameTime();
        
        return age > 40;
    }
    
    private AnimationController<TechDoorBlockEntity> getAnimationController() {
        return new AnimationController<>(this, state -> {
            
            // increase animation speed when newly loaded in, to avoid visible animation when initializing
            if (state.getController().getCurrentAnimation() == null) {
                state.getController().setAnimationSpeed(100);
                delayedTimerReset(state);
            }
            
            var opened = getBlockState().getValue(TechDoorBlock.OPENED);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registryLookup) {
        super.saveAdditional(tag, registryLookup);
        addColorToNbt(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registryLookup) {
        super.loadAdditional(tag, registryLookup);
        loadColorFromNbt(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        var tag = super.getUpdateTag(registryLookup);
        addColorToNbt(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ColorVariant getCurrentColor() {
        return currentColor;
    }

    @Override
    public void assignColor(ColorVariant color) {
        currentColor = color;

        if (level != null) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
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
