package rearth.oritech.block.entity.processing;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.fluid.DelegatingFluidStorage;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ColorableMachine;
import rearth.oritech.util.MachineSoundHandler;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import java.util.List;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;

public class RefineryModuleBlockEntity extends NetworkedBlockEntity implements MultiblockMachineController, FluidProvider, GeoBlockEntity, ColorableMachine {

    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;

    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorVariant currentColor = getDefaultColor();

    //animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<RefineryModuleBlockEntity> animationController = getAnimationController();

    // this field is updated by a refinery when checking for modules
    private RefineryBlockEntity owningRefinery;

    // fluid delegator
    private final DelegatingFluidStorage fluidStorage = new DelegatingFluidStorage(
            () -> owningRefinery == null ? null : owningRefinery.getFluidStorageForModule(worldPosition),
            () -> isActive(getBlockState()) && owningRefinery != null);


    public RefineryModuleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REFINERY_MODULE_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        serializeMultiblock(output);
        serializeColor(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        deserializeMultiblock(input);
        deserializeColor(input);
    }

    @Override
    public ColorVariant getCurrentColor() {
        return currentColor;
    }

    @Override
    public void assignColor(ColorVariant color) {
        this.currentColor = color;

        if (this.level != null && !this.level.isClientSide()) {
            this.setChanged(false);
            this.sendUpdate(SyncType.SPARSE_TICK);
        }
    }

    @Override
    public ColorVariant getDefaultColor() {
        return ColorVariant.FLUXITE;
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 0, -1),    // right
                new Vec3i(1, 0, -1),    // back right
                new Vec3i(1, 0, 0),    // back middle
                new Vec3i(2, 0, -1),    // backer right
                new Vec3i(2, 0, 0)
        );
    }

    @Override
    public Direction getFacingForMultiblock() {
        var state = getBlockState();
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockPos getPosForMultiblock() {
        return worldPosition;
    }

    @Override
    public Level getWorldForMultiblock() {
        return level;
    }

    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }

    @Override
    public void setCoreQuality(float quality) {
        this.coreQuality = quality;
    }

    @Override
    public float getCoreQuality() {
        return coreQuality;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForMultiblock() {
        return null;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return null;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        return fluidStorage;
    }

    @Override
    public void triggerSetupAnimation() {
        triggerAnim("machine", "setup");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(animationController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    private AnimationController<RefineryModuleBlockEntity> getAnimationController() {
        return new AnimationController<RefineryModuleBlockEntity>("machine", state -> {

            if (state.isCurrentAnimation(SETUP)) {
                if (state.controller().hasAnimationFinished()) {
                    state.setAndContinue(IDLE);
                } else {
                    return state.setAndContinue(SETUP);
                }
            }

            if (isActive(getBlockState())) {
                return state.setAndContinue(IDLE);
            } else {
                return state.setAndContinue(PACKAGED);
            }
        }).setSoundKeyframeHandler(new MachineSoundHandler<RefineryModuleBlockEntity>())
                .triggerableAnim("setup", SETUP)
                .receiveTriggeredAnimations();
    }

    public boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }

    public void setOwningRefinery(RefineryBlockEntity owner) {
        this.owningRefinery = owner;
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {

    }
}
