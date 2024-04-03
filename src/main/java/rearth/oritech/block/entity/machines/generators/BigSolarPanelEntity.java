package rearth.oritech.block.entity.machines.generators;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.PassiveGeneratorBlockEntity;
import rearth.oritech.block.blocks.machines.generators.BigSolarPanelBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.MultiblockMachineController;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        super(BlockEntitiesContent.BIG_SOLAR_ENTITY, pos, state);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        addMultiblockToNbt(nbt);
        nbt.putBoolean("folded", isFolded);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        loadMultiblockNbtData(nbt);
        isFolded = nbt.getBoolean("folded");
    }
    
    @Override
    public int getProductionRate() {
        var baseRate = ((BigSolarPanelBlock) this.getCachedState().getBlock()).productionRate;
        isFolded = world.isNight();
        return (int) (coreQuality * baseRate);
    }
    
    @Override
    public boolean isProducing() {
        return !world.isNight();
    }
    
    public void sendInfoMessageToPlayer(PlayerEntity player) {
        player.sendMessage(Text.literal("Production rate: " + getProductionRate() + " with core multiplier: " + getCoreQuality()));
    }
    
    // output only to north and south
    @Override
    protected HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        
        var res = new HashMap<Direction, BlockApiCache<EnergyStorage, Direction>>(6);
        
        var northCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.north());
        res.put(Direction.SOUTH, northCache);
        var southCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.south());
        res.put(Direction.NORTH, southCache);
        
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
    public InventoryProvider getInventoryForLink() {
        return null;
    }
    
    @Override
    public EnergyStorage getEnergyStorageForLink() {
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
        return new AnimationController<>(this, state -> {
            
            if (!isActive(getCachedState()))
                return state.setAndContinue(MachineBlockEntity.PACKAGED);
            
            if (state.isCurrentAnimation(MachineBlockEntity.SETUP)) {
                if (state.getController().hasAnimationFinished()) {
                    return state.setAndContinue(MachineBlockEntity.IDLE);
                } else {
                    return state.setAndContinue(MachineBlockEntity.SETUP);
                }
            }
            
            // update correct state on client
            var timeOfDay = getAdjustedTimeOfDay();
            var isDay = timeOfDay > 0 && timeOfDay < 12500;
            isFolded = !isDay;
            
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
    
    public long getAdjustedTimeOfDay() {
        return (world.getTimeOfDay() + getTimeOffset()) % 24000;
    }
    
    public int getTimeOffset() {
        var base = pos.getX() + pos.getZ();
        return (int) (Math.sin((double) base / 60) * 100);
    }
    
    public boolean isActive(BlockState state) {
        return state.get(ASSEMBLED);
    }
    
    public void playSetupAnimation() {
        animationController.setAnimation(MachineBlockEntity.SETUP);
        animationController.forceAnimationReset();
    }
}
