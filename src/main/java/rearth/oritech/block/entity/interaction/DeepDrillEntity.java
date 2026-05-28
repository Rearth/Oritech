package rearth.oritech.block.entity.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ColorableMachine;
import rearth.oritech.util.Geometry;
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


public class DeepDrillEntity extends NetworkedBlockEntity implements EnergyApi.BlockProvider, GeoBlockEntity, ItemApi.BlockProvider, MultiblockMachineController, ColorableMachine {
    
    // work data
    private boolean initialized;
    public final List<Block> targetedOre = new ArrayList<>();
    public int progress;
    @SyncField
    private long lastWorkTime;
    
    // config
    
    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(OritechConfig.deepDrillConfig.energyCapacity.get(), getMaxRfInput(), 0, this::setChanged);
    
    public final SimpleInventoryStorage inventory = createInventoryStorage();
    
    private @NotNull SimpleInventoryStorage createInventoryStorage() {
        return new SimpleInventoryStorage(1, this::setChanged);
    }
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<DeepDrillEntity> animationController = getAnimationController();
    
    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorVariant currentColor = getDefaultColor();
    
    public DeepDrillEntity(BlockPos pos, BlockState state) {
        this(BlockEntitiesContent.DEEP_DRILL_ENTITY.get(), pos, state);
    }
    
    // this second option is here to allow addons to create custom deep drill entities with special logic
    public DeepDrillEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public boolean init(boolean manual) {
        
        initialized = true;
        targetedOre.clear();
        loadOreBlocks(manual);
        
        return !targetedOre.isEmpty();
    }
    
    
    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if (isActive(state) && !initialized && (level.getGameTime() + pos.asLong()) % 60 == 0) {
            init(false);
        }
        
        if (level.isClientSide() || !initialized || targetedOre.isEmpty()) return;
        if (!inventory.isEmpty() && inventory.heldStacks.get(0).getCount() >= inventory.heldStacks.get(0).getMaxStackSize())
            return;    // inv full
        
        var energyPerStep = getRfPerStep();
        
        if (energyStorage.energy >= energyPerStep) {
            progress++;
            energyStorage.energy -= energyPerStep;
            lastWorkTime = level.getGameTime();
            setChanged();
            
            var particlePos = getCenter(0);
            if (level instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.LAVA, particlePos.getX() + 0.5, particlePos.getY() + 0.5, particlePos.getZ() + 0.5, 1, 0.6, 0.6, 0.6, 0);
        }
        
        // try increasing faster if too much energy is provided
        for (int i = 0; i < OritechConfig.deepDrillConfig.stepsPerOre.get(); i++) {
            if (energyStorage.energy >= energyPerStep) {
                progress++;
                energyStorage.energy -= energyPerStep;
            } else {
                break;
            }
        }
        
        if (progress >= OritechConfig.deepDrillConfig.stepsPerOre.get()) {
            craftResult(level, pos);
            progress -= OritechConfig.deepDrillConfig.stepsPerOre.get();
            this.setChanged();
        }
        
    }
    
    private BlockPos getCenter(int y) {
        var state = getBlockState();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return worldPosition.offset(Geometry.rotatePosition(new Vec3i(1, y, 0), facing));
    }
    
    public void loadOreBlocks(boolean manual) {
        var center = getCenter(-1);
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                // Only target the top-most uncovered resource node
                for (int y = 0; y >= -2; y--) {
                    var target = center.offset(x, y, z);
                    var targetState = level.getBlockState(target);
                    if (targetState.is(TagContent.RESOURCE_NODES)) {
                        if (manual) ParticleContent.DebugBlock(level, Vec3.atLowerCornerOf(target));
                        targetedOre.add(targetState.getBlock());
                        break;
                    } else if (!targetState.isAir()) break;
                }
            }
        }
    }
    
    private void craftResult(Level level, BlockPos pos) {
        var usedOre = targetedOre.get(level.random.nextInt(0, targetedOre.size()));
        var nodeOreBlockItem = usedOre.asItem();
        var sampleInv = new SimpleCraftingInventory(new ItemStack(nodeOreBlockItem, 1));
        
        var recipeCandidate = level.getRecipeManager().getRecipeFor(RecipeContent.DEEP_DRILL.get(), sampleInv, level);
        if (recipeCandidate.isEmpty())
            return;
        
        var output = recipeCandidate.get().value().getResults().get(0);
        inventory.insert(output, false);
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        serializeMultiblock(output);
        serializeColor(output);
        output.putLong("energy_stored", energyStorage.energy);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        deserializeMultiblock(input);
        deserializeColor(input);
        energyStorage.energy = input.getLongOr("energy_stored", 0);
    }
    
    @Override
    public DynamicEnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0, 1),
          new Vec3i(0, 0, -1),
          new Vec3i(-1, 0, 1),
          new Vec3i(-1, 0, 0),
          new Vec3i(-1, 0, -1),
          new Vec3i(-2, 0, 1),
          new Vec3i(-2, 0, 0),
          new Vec3i(-2, 0, -1),
          new Vec3i(0, 1, 1),
          new Vec3i(0, 1, 0),
          new Vec3i(0, 1, -1),
          new Vec3i(-1, 1, 1),
          new Vec3i(-1, 1, 0),
          new Vec3i(-1, 1, -1),
          new Vec3i(-2, 1, 1),
          new Vec3i(-2, 1, 0),
          new Vec3i(-2, 1, -1),
          new Vec3i(0, 2, 1),
          new Vec3i(0, 2, 0),
          new Vec3i(0, 2, -1),
          new Vec3i(-1, 2, 1),
          new Vec3i(-1, 2, 0),
          new Vec3i(-1, 2, -1),
          new Vec3i(-2, 2, 1),
          new Vec3i(-2, 2, 0),
          new Vec3i(-2, 2, -1)
        );
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        var state = getBlockState();
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
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
        return inventory;
    }
    
    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return null;
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
    
    public int getMaxRfInput() {
        return 0;
    }
    
    public int getRfPerStep() {
        return OritechConfig.deepDrillConfig.energyPerStep.get();
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
    
    private AnimationController<DeepDrillEntity> getAnimationController() {
        return new AnimationController<>(this, state -> {
            
            if (state.isCurrentAnimation(SETUP)) {
                if (state.getController().hasAnimationFinished()) {
                    state.setAndContinue(IDLE);
                } else {
                    return state.setAndContinue(SETUP);
                }
            }
            
            if (isActive(getBlockState())) {
                
                var idleTime = level.getGameTime() - lastWorkTime;
                
                if (idleTime < 60) {
                    return state.setAndContinue(WORKING);
                } else {
                    return state.setAndContinue(IDLE);
                }
            } else {
                return state.setAndContinue(PACKAGED);
            }
        }).setSoundKeyframeHandler(new MachineSoundHandler<>()).triggerableAnim("setup", SETUP);
    }
    
    public void setLastWorkTime(long lastWorkTime) {
        this.lastWorkTime = lastWorkTime;
    }
    
    private boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }
}
