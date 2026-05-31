package rearth.oritech.block.entity.decorative;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.decorative.HangarDoorBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ColorableMachine;

public class HangarDoorBlockEntity extends BlockEntity implements GeoBlockEntity, ColorableMachine {

    public static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("retract");
    public static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("deploy");

    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<HangarDoorBlockEntity> animationController = getAnimationController();
    private ColorVariant currentColor = getDefaultColor();

    private long lastSoundEventAt = 0;

    public HangarDoorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.HANGAR_DOOR_ENTITY.get(), pos, state);
    }

    public boolean shouldPlaySoundAgain() {
        var age = level.getGameTime() - lastSoundEventAt;
        lastSoundEventAt = level.getGameTime();
        return age > 40;
    }

    private AnimationController<HangarDoorBlockEntity> getAnimationController() {
        return new AnimationController<>("machine", state -> {
            if (state.controller().getCurrentRawAnimation() == null) {
                state.controller().setAnimation(MachineBlockEntity.IDLE);
            }

            return getBlockState().getValue(HangarDoorBlock.OPENED) ? state.setAndContinue(OPEN) : state.setAndContinue(CLOSE);
        });
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
