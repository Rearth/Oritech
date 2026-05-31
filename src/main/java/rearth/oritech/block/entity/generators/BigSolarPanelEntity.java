package rearth.oritech.block.entity.generators;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.PassiveGeneratorBlockEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;


public class BigSolarPanelEntity extends PassiveGeneratorBlockEntity implements MultiblockMachineController, GeoBlockEntity {

    public static final RawAnimation FOLD = RawAnimation.begin().thenPlayAndHold("fold");
    public static final RawAnimation UNFOLD = RawAnimation.begin().thenPlayAndHold("unfold");

    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<BigSolarPanelEntity> animationController = getAnimationController();

    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;

    // self
    private boolean isFolded;

    public BigSolarPanelEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BIG_SOLAR_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        serializeMultiblock(output);
        output.putBoolean("folded", isFolded);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        deserializeMultiblock(input);
        isFolded = input.getBooleanOr("folded", false);
    }

    @Override
    public int getProductionRate() {
        var baseRate = OritechConfig.generators.solarGeneratorData.energyPerTick.get();
        var skyLightLevel = level.getBrightness(LightLayer.SKY, this.getBlockPos());
        isFolded = level.isBrightOutside() && skyLightLevel < 12;
        return (int) (coreQuality * baseRate);
    }

    @Override
    public boolean isProducing() {
        var skyLightLevel = level.getBrightness(LightLayer.SKY, this.getBlockPos());
        return !level.isBrightOutside() && skyLightLevel >= 12 && isActive(getBlockState());
    }

    public void sendInfoMessageToPlayer(Player player) {
        player.sendSystemMessage(Component.translatable("message.oritech.generator.production_rate", getProductionRate(), getCoreQuality()));
    }

    // output only to north, down and south
    @Override
    protected Set<Tuple<BlockPos, Direction>> getOutputTargets(BlockPos pos, Level level) {

        var res = new HashSet<Tuple<BlockPos, Direction>>();
        res.add(new Tuple<>(pos.below(), Direction.DOWN));
        res.add(new Tuple<>(pos.south(), Direction.NORTH));
        res.add(new Tuple<>(pos.north(), Direction.SOUTH));

        return res;

    }

    //region multiblock
    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }

    @Override
    public Direction getFacingForMultiblock() {
        return Direction.NORTH;
    }

    @Override
    public float getCoreQuality() {
        return this.coreQuality;
    }

    @Override
    public void setCoreQuality(float quality) {
        this.coreQuality = quality;
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
    public List<Vec3i> getCorePositions() {
        return List.of(
                // top
                new Vec3i(1, 1, 1),
                new Vec3i(0, 1, 1),
                new Vec3i(-1, 1, 1),
                new Vec3i(1, 1, 0),
                new Vec3i(0, 1, 0),
                new Vec3i(-1, 1, 0),
                new Vec3i(1, 1, -1),
                new Vec3i(0, 1, -1),
                new Vec3i(-1, 1, -1),
                // bottom
                new Vec3i(1, 0, 1),
                new Vec3i(0, 0, 1),
                new Vec3i(-1, 0, 1),
                new Vec3i(1, 0, -1),
                new Vec3i(0, 0, -1),
                new Vec3i(-1, 0, -1)
        );
    }
    //endregion


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(animationController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    private AnimationController<BigSolarPanelEntity> getAnimationController() {
        return new AnimationController<>("machine", state -> {

            if (!isActive(getBlockState()))
                return state.setAndContinue(MachineBlockEntity.PACKAGED);

            if (state.isCurrentAnimation(MachineBlockEntity.SETUP)) {
                if (state.controller().hasAnimationFinished()) {
                    return state.setAndContinue(MachineBlockEntity.IDLE);
                } else {
                    return state.setAndContinue(MachineBlockEntity.SETUP);
                }
            }

            // update correct state on client
            var timeOfDay = getAdjustedTimeOfDay();
            var skyLightLevel = level.getBrightness(LightLayer.SKY, this.getBlockPos());
            var isDay = timeOfDay > 0 && timeOfDay < 12500;
            isFolded = !isDay || skyLightLevel < 12;

            if (isFolded) {
                return state.setAndContinue(FOLD);
            } else {
                if (state.isCurrentAnimation(MachineBlockEntity.IDLE)) {
                    return state.setAndContinue(MachineBlockEntity.IDLE);
                } else {
                    return state.setAndContinue(UNFOLD);
                }
            }
        });
    }

    @Override
    public BlockPos getPosForMultiblock() {
        return getBlockPos();
    }

    @Override
    public Level getWorldForMultiblock() {
        return getLevel();
    }

    public long getAdjustedTimeOfDay() {
        return (level.getDefaultClockTime() + getTimeOffset()) % 24000;
    }

    public int getTimeOffset() {
        var base = worldPosition.getX() + worldPosition.getZ();
        return (int) (Math.sin((double) base / 60) * 100);
    }

    public boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }

    @Override
    public void triggerSetupAnimation() {
        // todo
    }
}
