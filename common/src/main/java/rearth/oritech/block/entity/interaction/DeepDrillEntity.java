package rearth.oritech.block.entity.interaction;

import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.SimpleCraftingInventory;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;


public class DeepDrillEntity extends NetworkedBlockEntity implements EnergyApi.BlockProvider, GeoBlockEntity, ItemApi.BlockProvider, MultiblockMachineController {
    
    // work data
    private boolean initialized;
    private final List<Block> targetedOre = new ArrayList<>();
    private int progress;
    @SyncField
    private long lastWorkTime;
    
    // config
    private final int worktime = Oritech.CONFIG.deepDrillConfig.stepsPerOre();
    private final int energyPerStep = Oritech.CONFIG.deepDrillConfig.energyPerStep();
    
    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(Oritech.CONFIG.deepDrillConfig.energyCapacity(), 0, 0, this::setChanged);
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(1, this::setChanged);
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<DeepDrillEntity> animationController = getAnimationController();
    
    public DeepDrillEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DEEP_DRILL_ENTITY, pos, state);
    }
    
    public boolean init(boolean manual) {
        
        initialized = true;
        targetedOre.clear();
        loadOreBlocks(manual);

        return !targetedOre.isEmpty();
    }
    
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if (isActive(state) && !initialized && (world.getGameTime() + pos.asLong()) % 60 == 0) {
            init(false);
        }
        
        if (world.isClientSide() || !initialized || targetedOre.isEmpty()) return;
        if (!inventory.isEmpty() && inventory.heldStacks.get(0).getCount() >= inventory.heldStacks.get(0).getMaxStackSize())
            return;    // inv full
        
        if (energyStorage.amount >= energyPerStep) {
            progress++;
            energyStorage.amount -= energyPerStep;
            lastWorkTime = world.getGameTime();
            setChanged();
            
            var particlePos = getCenter(0);
            ParticleContent.FURNACE_BURNING.spawn(world, Vec3.atLowerCornerOf(particlePos), 1);
        }
        
        // try increasing faster if too much energy is provided
        for (int i = 0; i < 5; i++) {
            if (energyStorage.amount >= energyPerStep) {
                progress++;
                energyStorage.amount -= energyPerStep;
            }
        }
        
        if (progress >= worktime) {
            craftResult(world, pos);
            progress -= worktime;
            this.setChanged();
        }
        
    }
    
    private BlockPos getCenter(int y) {
        var state = getBlockState();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return worldPosition.offset(Geometry.rotatePosition(new Vec3i(1, y, 0), facing));
    }
    
    private void loadOreBlocks(boolean manual) {
        var center = getCenter(-1);
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                // Only target the top-most uncovered resource node
                for (int y = 0; y >= -2; y--) {
                    var target = center.offset(x, y, z);
                    var targetState = level.getBlockState(target);
                    if (targetState.is(TagContent.RESOURCE_NODES)) {
                        if (manual) ParticleContent.DEBUG_BLOCK.spawn(level, Vec3.atLowerCornerOf(target));
                        targetedOre.add(targetState.getBlock());
                        break;
                    } else if (!targetState.isAir()) break;
                }
            }
        }  
    }
    
    private void craftResult(Level world, BlockPos pos) {
        var usedOre = targetedOre.get(world.random.nextInt(0, targetedOre.size()));
        var nodeOreBlockItem = usedOre.asItem();
        var sampleInv = new SimpleCraftingInventory(new ItemStack(nodeOreBlockItem, 1));
        
        var recipeCandidate = world.getRecipeManager().getRecipeFor(RecipeContent.DEEP_DRILL, sampleInv, world);
        if (recipeCandidate.isEmpty())
            return;
        
        var output = recipeCandidate.get().value().getResults().get(0);
        inventory.insert(output, false);
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        addMultiblockToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        loadMultiblockNbtData(nbt);
        energyStorage.amount = nbt.getLong("energy_stored");
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
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
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return inventory;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
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
        }).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()).triggerableAnim("setup", SETUP);
    }
    
    public void setLastWorkTime(long lastWorkTime) {
        this.lastWorkTime = lastWorkTime;
    }
    
    private boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }
}
