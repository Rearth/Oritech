package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.datagen.data.TagContent;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.List;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;

public class DeepDrillEntity extends BlockEntity implements BlockEntityTicker<DeepDrillEntity>, EnergyProvider, GeoBlockEntity, InventoryProvider, MultiblockMachineController {
    
    // work data
    private boolean initialized;
    private List<Block> targetedOre = new ArrayList<>();
    private int progress;
    private long lastWorkTime;
    private boolean networkDirty;
    
    // config
    private int worktime = Oritech.CONFIG.deepDrillConfig.stepsPerOre();
    private int energyPerStep = Oritech.CONFIG.deepDrillConfig.energyPerStep();
    
    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(Oritech.CONFIG.deepDrillConfig.energyCapacity(), 0, 0) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            DeepDrillEntity.this.markDirty();
        }
    };
    
    public final SimpleInventory inventory = new SimpleInventory(1) {
        @Override
        public void markDirty() {
            DeepDrillEntity.this.markDirty();
        }
    };
    
    protected final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<DeepDrillEntity> animationController = getAnimationController();
    
    public DeepDrillEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DEEP_DRILL_ENTITY, pos, state);
    }
    
    public boolean init() {
        
        var startAt = pos.south().down();
        var checkState = world.getBlockState(startAt);
        
        initialized = true;
        targetedOre.clear();
        loadOreBlocks();

        return !targetedOre.isEmpty();
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, DeepDrillEntity blockEntity) {
        if (world.isClient() || !initialized) return;
        if (!inventory.isEmpty() && inventory.heldStacks.get(0).getCount() >= inventory.heldStacks.get(0).getMaxCount())
            return;    // inv full
        
        if (energyStorage.amount >= energyPerStep) {
            progress++;
            energyStorage.amount -= energyPerStep;
            lastWorkTime = world.getTime();
            networkDirty = true;
            
            var particlePos = getCenter(0);
            ParticleContent.FURNACE_BURNING.spawn(world, Vec3d.of(particlePos), 1);
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
            this.markDirty();
        }
        
        updateNetwork();
        
    }
    
    private BlockPos getCenter(int y) {
        var state = getCachedState();
        var facing = state.get(Properties.HORIZONTAL_FACING);
        return pos.add(Geometry.rotatePosition(new Vec3i(1, y, 0), facing));
    }
    
    private void loadOreBlocks() {
        var center = getCenter(-1);
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                // Only target the top-most uncovered resource node
                for (int y = 0; y >= -2; y--) {
                    var target = center.add(x, y, z);
                    var targetState = world.getBlockState(target);
                    if (targetState.isIn(TagContent.RESOURCE_NODES)) {
                        ParticleContent.DEBUG_BLOCK.spawn(world, Vec3d.of(target));
                        targetedOre.add(targetState.getBlock());
                        break;
                    } else if (!targetState.isAir()) break;
                }
            }
        }  
    }
    
    private void updateNetwork() {
        if (networkDirty && world.getTime() % 5 == 0) {
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.DeepDrillSyncPacket(pos, lastWorkTime));
            networkDirty = false;
        }
    }
    
    private void craftResult(World world, BlockPos pos) {
        var usedOre = targetedOre.get(world.random.nextBetweenExclusive(0, targetedOre.size()));
        var nodeOreBlockItem = usedOre.asItem();
        var sampleInv = new SimpleCraftingInventory(new ItemStack(nodeOreBlockItem, 1));
        
        var recipeCandidate = world.getRecipeManager().getFirstMatch(RecipeContent.DEEP_DRILL, sampleInv, world);
        if (recipeCandidate.isEmpty())
            return;
        
        var output = recipeCandidate.get().value().getResults().get(0);
        if (!inventory.canInsert(output)) return;
        try (var tx = Transaction.openOuter()) {
            inventoryStorage.insert(ItemVariant.of(output), output.getCount(), tx);
            tx.commit();
        }
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        addMultiblockToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        nbt.putBoolean("initialized", initialized);
        if (initialized) {
            for (int i = 0; i < targetedOre.size(); i++) {
                nbt.putString("nodeType" + i, Registries.BLOCK.getId(targetedOre.get(i)).toString());
            }
        }
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        loadMultiblockNbtData(nbt);
        energyStorage.amount = nbt.getLong("energy_stored");
        initialized = nbt.getBoolean("initialized");
        if (initialized) {
            for (int i = 0; i < targetedOre.size(); i++) {
                targetedOre.add(Registries.BLOCK.get(Identifier.of(nbt.getString("nodeType" + i))));
            }
        }
    }
    
    @Override
    public EnergyStorage getStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return inventoryStorage;
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
        var state = getCachedState();
        return state.get(Properties.HORIZONTAL_FACING).getOpposite();
    }
    
    @Override
    public BlockPos getMachinePos() {
        return pos;
    }
    
    @Override
    public World getMachineWorld() {
        return world;
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
    public InventoryProvider getInventoryForLink() {
        return this;
    }
    
    @Override
    public EnergyStorage getEnergyStorageForLink() {
        return null;
    }
    
    @Override
    public void playSetupAnimation() {
        animationController.setAnimation(SETUP);
        animationController.forceAnimationReset();
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
            
            if (isActive(getCachedState())) {
                
                var idleTime = world.getTime() - lastWorkTime;
                
                if (idleTime < 60) {
                    return state.setAndContinue(WORKING);
                } else {
                    return state.setAndContinue(IDLE);
                }
            } else {
                return state.setAndContinue(PACKAGED);
            }
        }).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>());
    }
    
    public void setLastWorkTime(long lastWorkTime) {
        this.lastWorkTime = lastWorkTime;
    }
    
    private boolean isActive(BlockState state) {
        return state.get(ASSEMBLED);
    }
}
