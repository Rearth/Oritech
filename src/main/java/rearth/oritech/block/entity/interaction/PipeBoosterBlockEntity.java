package rearth.oritech.block.entity.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineSoundHandler;
import rearth.oritech.util.Geometry;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PipeBoosterBlockEntity extends BlockEntity implements BlockEntityTicker<PipeBoosterBlockEntity>, GeoBlockEntity, EnergyApi.BlockProvider {
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 4000, 0, this::setChanged);
    
    public static final RawAnimation EXPAND = RawAnimation.begin().thenPlayAndHold("expand");
    public static final RawAnimation RETRACT = RawAnimation.begin().thenPlayAndHold("retract");
    public static final RawAnimation EXTENDED = RawAnimation.begin().thenPlay("extended");
    public static final RawAnimation RETRACTED = RawAnimation.begin().thenPlay("retracted");
    public static final RawAnimation WORK = RawAnimation.begin().thenPlay("work");
    
    private static final int BOOST_ENERGY_COST = 32;
    
    private boolean setPipe;
    
    public PipeBoosterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PIPE_BOOSTER_BLOCK_ENTITY.get(), pos, state);
    }
    
    @Override
    public void tick(Level level, BlockPos pos, BlockState state, PipeBoosterBlockEntity blockEntity) {
        if (level.isClientSide()) return;
        
        if (!setPipe && (level.getGameTime() & 25) == 0) {
            // try find pipe entity behind
            var targetPos = pos.offset(Geometry.getBackward(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
            var candidate = level.getBlockEntity(targetPos);
            if (candidate instanceof GenericPipeInterfaceEntity pipe) {
                pipe.connectedBooster = pos;
                setPipe = true;
                triggerAnim("machine", "expand");
            }
        }
        
        // occasionally set the correct pipe anim state
        if (level.getGameTime() % 42 == 0) {
            if (setPipe) {
                triggerAnim("machine", "extended");
            } else {
                triggerAnim("machine", "retracted");
            }
        }
        
    }
    
    public boolean canUseBoost() {
        return energyStorage.energy >= BOOST_ENERGY_COST;
    }
    
    public void useBoost() {
        if (!canUseBoost()) return;
        energyStorage.energy -= BOOST_ENERGY_COST;
        this.setChanged();
        
        triggerAnim("machine", "work");
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy_stored", energyStorage.energy);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.energy = input.getLongOr("energy_stored", 0);
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 5, state -> PlayState.CONTINUE)
                          .triggerableAnim("work", WORK)
                          .triggerableAnim("extended", EXTENDED)
                          .triggerableAnim("retracted", RETRACTED)
                          .triggerableAnim("expand", EXPAND)
                          .triggerableAnim("retract", RETRACT)
                          .setSoundKeyframeHandler(new MachineSoundHandler<>()));
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return energyStorage;
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
}
