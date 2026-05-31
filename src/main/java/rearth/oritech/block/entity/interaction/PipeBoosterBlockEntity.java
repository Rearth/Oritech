package rearth.oritech.block.entity.interaction;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.MachineSoundHandler;

public class PipeBoosterBlockEntity extends BlockEntity implements BlockEntityTicker<PipeBoosterBlockEntity>, GeoBlockEntity, EnergyProvider {

    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 4000, 0, 0, this::setChanged, false);

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
        controllers.add(new AnimationController<>("machine", 5, state -> PlayState.CONTINUE)
                .triggerableAnim("work", WORK)
                .triggerableAnim("extended", EXTENDED)
                .triggerableAnim("retracted", RETRACTED)
                .triggerableAnim("expand", EXPAND)
                .triggerableAnim("retract", RETRACT)
                .setSoundKeyframeHandler(new MachineSoundHandler<>()));
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
}
