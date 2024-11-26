package rearth.oritech.block.entity.machines.interaction;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.DynamicEnergyStorage;
import rearth.oritech.util.EnergyApi;
import rearth.oritech.util.Geometry;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PipeBoosterBlockEntity extends BlockEntity implements BlockEntityTicker<PipeBoosterBlockEntity>, GeoBlockEntity, EnergyApi.BlockEnergyApi.EnergyProvider {
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 4000, 0, this::markDirty);
    
    public static final RawAnimation EXPAND = RawAnimation.begin().thenPlayAndHold("expand");
    public static final RawAnimation RETRACT = RawAnimation.begin().thenPlayAndHold("retract");
    public static final RawAnimation EXTENDED = RawAnimation.begin().thenPlay("extended");
    public static final RawAnimation RETRACTED = RawAnimation.begin().thenPlay("retracted");
    public static final RawAnimation WORK = RawAnimation.begin().thenPlay("work");
    
    private static final int BOOST_ENERGY_COST = 32;
    
    private boolean setPipe;
    
    public PipeBoosterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PIPE_BOOSTER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, PipeBoosterBlockEntity blockEntity) {
        if (world.isClient) return;
        
        if (!setPipe && (world.getTime() & 25) == 0) {
            // try find pipe entity behind
            var targetPos = pos.add(Geometry.getBackward(state.get(Properties.HORIZONTAL_FACING)));
            var candidate = world.getBlockEntity(targetPos);
            if (candidate instanceof GenericPipeInterfaceEntity pipe) {
                pipe.connectedBooster = pos;
                setPipe = true;
                triggerAnim("machine", "expand");
            }
        }
        
        // occasionally set the correct pipe anim state
        if (world.getTime() % 42 == 0) {
            if (setPipe) {
                triggerAnim("machine", "extended");
            } else {
                triggerAnim("machine", "retracted");
            }
        }
        
    }
    
    public boolean canUseBoost() {
        return energyStorage.amount >= BOOST_ENERGY_COST;
    }
    
    public void useBoost() {
        if (!canUseBoost()) return;
        energyStorage.amount -= BOOST_ENERGY_COST;
        this.markDirty();
        
        triggerAnim("machine", "work");
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putLong("energy_stored", energyStorage.amount);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        energyStorage.amount = nbt.getLong("energy_stored");
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 5, state -> PlayState.CONTINUE)
                          .triggerableAnim("work", WORK)
                          .triggerableAnim("extended", EXTENDED)
                          .triggerableAnim("retracted", RETRACTED)
                          .triggerableAnim("expand", EXPAND)
                          .triggerableAnim("retract", RETRACT)
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public EnergyApi.EnergyContainer getStorage(@Nullable Direction direction) {
        return energyStorage;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
}
