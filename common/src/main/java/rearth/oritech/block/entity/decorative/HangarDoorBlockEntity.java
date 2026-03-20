package rearth.oritech.block.entity.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.decorative.HangarDoorBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ColorableMachine;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HangarDoorBlockEntity extends BlockEntity implements GeoBlockEntity, ColorableMachine {

    public static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("retract");
    public static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("deploy");

    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<HangarDoorBlockEntity> animationController = getAnimationController();
    private ColorVariant currentColor = getDefaultColor();

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