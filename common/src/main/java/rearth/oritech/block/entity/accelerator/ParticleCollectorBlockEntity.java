package rearth.oritech.block.entity.accelerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.energy.containers.DynamicEnergyStorage;
import rearth.oritech.util.energy.EnergyApi;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import static rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity.getOutputPosition;

public class ParticleCollectorBlockEntity extends BlockEntity implements BlockEntityTicker<ParticleCollectorBlockEntity>, EnergyApi.BlockProvider, GeoBlockEntity {
    
    // TODO recipes, wiki
    
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(Oritech.CONFIG.collectorEnergyStorage(), 0, Oritech.CONFIG.collectorEnergyStorage(), this::markDirty);
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    public static final RawAnimation WORK = RawAnimation.begin().thenPlayAndHold("collect");
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    
    private boolean setup = false;
    
    private final AnimationController<ParticleCollectorBlockEntity> animationController = getAnimationController();
    
    public ParticleCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PARTICLE_COLLECTOR_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public EnergyApi.EnergyContainer getStorage(@Nullable Direction direction) {
        return energyStorage;
    }
    
    public void onParticleCollided() {
        onParticleCollided(Oritech.CONFIG.blackHoleTachyonEnergy());
    }
    
    public void onParticleCollided(int amount) {
        energyStorage.amount = Math.min(energyStorage.capacity, energyStorage.amount + amount);
        energyStorage.update();
        triggerAnimation();
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, ParticleCollectorBlockEntity blockEntity) {
        if (world.isClient) return;
        
        if (!setup) {
            triggerAnimation();
            setup = true;
        }
        
        if (energyStorage.amount <= 0) return;
        
        // output energy to back
        var target = getOutputPosition(pos, getCachedState().get(FacingBlock.FACING).getOpposite());
        var candidate = EnergyApi.BLOCK.find(world, target.getRight(), target.getLeft());
        if (candidate != null) {
            EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putLong("energy", energyStorage.getAmount());
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        energyStorage.setAmount(nbt.getLong("energy"));
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(animationController);
    }
    
    private AnimationController<ParticleCollectorBlockEntity> getAnimationController() {
        return new AnimationController<>(this, state -> PlayState.CONTINUE);
    }
    
    public void triggerAnimation() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.ParticleAcceleratorAnimationPacket(pos));
    }
    
    public void playAnimation() {
        animationController.setAnimation(WORK);
        animationController.forceAnimationReset();
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
}
