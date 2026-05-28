package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.DelegatingFluidStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ColorableMachine;
import rearth.oritech.util.MachineSoundHandler;
import rearth.oritech.util.MultiblockMachineController;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;

public class RefineryModuleBlockEntity extends NetworkedBlockEntity implements MultiblockMachineController, FluidApi.BlockProvider, GeoBlockEntity, ColorableMachine {
    
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
      () -> owningRefinery.getFluidStorageForModule(worldPosition),
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
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return null;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return null;
    }
    
    @Override
    public FluidApi.FluidStorage getFluidStorage(@Nullable Direction direction) {
        return fluidStorage;
    }
    
    @Override
    public void triggerSetupAnimation() {
        triggerAnim("base_controller", "setup");
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
        return new AnimationController<>(this, state -> {
            
            if (state.isCurrentAnimation(SETUP)) {
                if (state.getController().hasAnimationFinished()) {
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
        }).setSoundKeyframeHandler(new MachineSoundHandler<>()).triggerableAnim("setup", SETUP);
    }
    
    public boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }
    
    public void setOwningRefinery(RefineryBlockEntity owner) {
        this.owningRefinery = owner;
    }
    
    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
    
    }
}
