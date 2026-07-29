package rearth.oritech.block.entity.decorative;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import rearth.oritech.block.blocks.decorative.IndustrialDoorBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ColorableMachine;

import java.util.Timer;
import java.util.TimerTask;

public class IndustrialDoorBlockEntity extends BlockEntity implements GeoBlockEntity, ColorableMachine {

    public static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("door_open");
    public static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("door_close");

    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<IndustrialDoorBlockEntity> animationController = getAnimationController();
    private ColorVariant currentColor = getDefaultColor();

    private long lastSoundEventAt = 0;

    public IndustrialDoorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.INDUSTRIAL_DOOR.get(), pos, state);
    }

    public boolean shouldPlaySoundAgain() {
        var age = level.getGameTime() - lastSoundEventAt;
        lastSoundEventAt = level.getGameTime();

        return age > 40;
    }

    private AnimationController<IndustrialDoorBlockEntity> getAnimationController() {
        return new AnimationController<>("machine", state -> {

            // increase animation speed when newly loaded in, to avoid visible animation when initializing
            if (state.controller().getCurrentRawAnimation() == null) {
                state.setControllerSpeed(100);
                delayedTimerReset(state);
            }

            var opened = getBlockState().getValue(IndustrialDoorBlock.OPENED);
            if (opened) {
                return state.setAndContinue(OPEN);
            } else {
                return state.setAndContinue(CLOSE);
            }

        });
    }

    private static void delayedTimerReset(AnimationTest<IndustrialDoorBlockEntity> state) {
        new Timer().schedule(
                new TimerTask() {

                    @Override
                    public void run() {
                        state.setControllerSpeed(1);
                    }
                }, 1000
        );
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        serializeColor(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        deserializeColor(input);
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
