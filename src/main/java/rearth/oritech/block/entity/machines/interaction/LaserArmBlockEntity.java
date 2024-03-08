package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.machines.interaction.LaserArmBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class LaserArmBlockEntity extends BlockEntity implements GeoBlockEntity, BlockEntityTicker<LaserArmBlockEntity>, EnergyProvider, MultiblockMachineController, MachineAddonController, InventoryProvider {
    
    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), 0) {
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            LaserArmBlockEntity.this.markDirty();
        }
    };
    
    protected final SimpleInventory inventory = new SimpleInventory(3) {
        @Override
        public void markDirty() {
            LaserArmBlockEntity.this.markDirty();
        }
    };
    
    protected final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<LaserArmBlockEntity> animationController = getAnimationController();
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    // addons
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openSlots = new ArrayList<>();
    private float coreQuality = 1f;
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    // working data
    private BlockPos target;
    private boolean networkDirty;
    
    public LaserArmBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LASER_ARM_BLOCK, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, LaserArmBlockEntity blockEntity) {
        if (world.isClient()) return;
        
        if (networkDirty)
            updateNetwork();
        
    }
    
    private void updateNetwork() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.LaserArmSyncPacket(pos, target));
        networkDirty = false;
    }
    
    public void testTarget(BlockState state) {
        System.out.println("hello world");
    }
    
    public boolean setTargetFromDesignator(BlockPos targetPos) {
        
        // todo if target is coreblock, adjust it to point to controller if connected
        System.out.println("setting target: " + targetPos.toShortString());
        
        var distance = targetPos.getManhattanDistance(pos);
        if (distance > 64) {
            return false;
        }
        
        this.target = targetPos;
        this.networkDirty = true;
        this.markDirty();
        return true;
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory.heldStacks, false);
        addMultiblockToNbt(nbt);
        writeAddonToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        
        if (target != null)
            nbt.putLong("target_position", target.asLong());
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory.heldStacks);
        loadMultiblockNbtData(nbt);
        loadAddonNbtData(nbt);
        
        updateEnergyContainer();
        
        energyStorage.amount = nbt.getLong("energy_stored");
        target = BlockPos.fromLong(nbt.getLong("target_position"));
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
        return this;
    }
    
    @Override
    public EnergyStorage getEnergyStorageForLink() {
        return energyStorage;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1, 0)
        );
    }
    //endregion
    
    // energyprovider
    @Override
    public EnergyStorage getStorage() {
        return energyStorage;
    }
    
    //region addons
    @Override
    public List<BlockPos> getConnectedAddons() {
        return connectedAddons;
    }
    
    @Override
    public List<BlockPos> getOpenSlots() {
        return openSlots;
    }
    
    @Override
    public Direction getFacingForAddon() {
        return Direction.NORTH;
    }
    
    @Override
    public DynamicEnergyStorage getStorageForAddon() {
        return energyStorage;
    }
    
    @Override
    public SimpleInventory getInventoryForAddon() {
        return inventory;
    }
    
    @Override
    public ScreenProvider getScreenProvider() {
        return null;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, -1, 0)
        );
    }
    
    @Override
    public BaseAddonData getBaseAddonData() {
        return addonData;
    }
    
    @Override
    public void setBaseAddonData(BaseAddonData data) {
        this.addonData = data;
    }
    
    @Override
    public long getDefaultCapacity() {
        return 20000;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 100;
    }
    //endregion
    
    // region animation
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(animationController);
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    private AnimationController<LaserArmBlockEntity> getAnimationController() {
        return new AnimationController<>(this, state -> {
            
            if (state.isCurrentAnimation(MachineBlockEntity.SETUP)) {
                if (state.getController().hasAnimationFinished()) {
                    state.setAndContinue(MachineBlockEntity.IDLE);
                } else {
                    return state.setAndContinue(MachineBlockEntity.SETUP);
                }
            }
            
            if (isActive(getCachedState())) {
                return state.setAndContinue(MachineBlockEntity.IDLE);
            } else {
                return state.setAndContinue(MachineBlockEntity.PACKAGED);
            }
        });
    }
    
    public void playSetupAnimation() {
        animationController.setAnimation(MachineBlockEntity.SETUP);
        animationController.forceAnimationReset();
    }
    
    public boolean isActive(BlockState state) {
        return state.get(LaserArmBlock.ASSEMBLED);
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return inventoryStorage;
    }
    //endregion
    
    public BlockPos getTarget() {
        return target;
    }
    
    public void setTarget(BlockPos target) {
        this.target = target;
    }
}
