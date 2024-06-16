package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.processing.AtomicForgeBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

import java.util.*;
import java.util.stream.Collectors;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public class LaserArmBlockEntity extends BlockEntity implements GeoBlockEntity, BlockEntityTicker<LaserArmBlockEntity>, EnergyProvider, ScreenProvider, ExtendedScreenHandlerFactory, MultiblockMachineController, MachineAddonController, InventoryProvider {
    
    private static final int BLOCK_BREAK_ENERGY = Oritech.CONFIG.laserArmConfig.blockBreakEnergyBase();
    
    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), 0) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            LaserArmBlockEntity.this.markDirty();
        }
    };
    
    public final SimpleInventory inventory = new SimpleInventory(3) {
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
    public int areaSize = 1;
    
    // config
    private final int range = Oritech.CONFIG.laserArmConfig.range();
    
    // working data
    private BlockPos targetDirection;
    private BlockPos currentTarget;
    private long lastFiredAt;
    private int progress;
    private int targetBlockEnergyNeeded = BLOCK_BREAK_ENERGY;
    private boolean networkDirty;
    private boolean redstonePowered;
    private ArrayDeque<BlockPos> pendingArea;
    
    // needed only on client
    public Vec3d lastRenderPosition;
    
    public LaserArmBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LASER_ARM_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, LaserArmBlockEntity blockEntity) {
        if (world.isClient() || !isActive(state) || redstonePowered || currentTarget == null || energyStorage.getAmount() < energyRequiredToFire())
            return;
        
        var targetBlock = currentTarget;
        var targetBlockState = world.getBlockState(targetBlock);
        var targetBlockEntity = world.getBlockEntity(targetBlock);
        
        
        var storageCandidate = EnergyStorage.SIDED.find(world, targetBlock, targetBlockState, targetBlockEntity, null);
        if (targetBlockEntity instanceof AtomicForgeBlockEntity atomicForgeEntity) {
            storageCandidate = atomicForgeEntity.getEnergyStorage();
        } else if (targetBlockEntity instanceof DeepDrillEntity deepDrillEntity) {
            storageCandidate = deepDrillEntity.getStorage(null);
        }
        
        var fired = false;
        
        if (storageCandidate != null) {
            var insertAmount = storageCandidate.getCapacity() - storageCandidate.getAmount();
            if (insertAmount < 10) return;
            fired = true;
            
            if (storageCandidate instanceof DynamicEnergyStorage dynamicStorage) {
                var transferCapacity = Math.min(insertAmount, energyRequiredToFire());
                dynamicStorage.amount += transferCapacity;  // direct transfer, allowing to insert into any container, even when inserting isnt allowed (e.g. atomic forge)
                dynamicStorage.onFinalCommit(); // gross abuse of transaction system to force it to sync
            } else {
                // probably not how this should be used, but whatever
                try (var tx = Transaction.openOuter()) {
                    storageCandidate.insert(insertAmount, tx);
                    tx.commit();
                }
            }
        } else if (targetBlockState.getBlock() instanceof BuddingAmethystBlock amethystBlock && !canPassThrough(targetBlockState)) {
            fired = true;
            amethystBlock.randomTick(targetBlockState, (ServerWorld) world, targetBlock, world.random);
            ParticleContent.ACCELERATING.spawn(world, Vec3d.of(targetBlock));
        } else if (!canPassThrough(targetBlockState)) {
            fired = true;
            progress += energyRequiredToFire();
            
            if (progress >= targetBlockEnergyNeeded) {
                finishBlockBreaking(targetBlock, targetBlockState);
            }
        } else {
            // when targeting air
            if (world.getTime() % 40 == 0)
                findNextBlockBreakTarget();
        }
        
        if (fired) {
            energyStorage.amount -= energyRequiredToFire();
            networkDirty = true;
            lastFiredAt = world.getTime();
        }
        
        if (networkDirty)
            updateNetwork();
        
    }
    
    public void setRedstonePowered(boolean redstonePowered) {
        this.redstonePowered = redstonePowered;
    }
    
    private void finishBlockBreaking(BlockPos targetPos, BlockState targetBlockState) {
        progress -= targetBlockEnergyNeeded;
        
        var targetEntity = world.getBlockEntity(targetPos);
        var dropped = Block.getDroppedStacks(targetBlockState, (ServerWorld) world, targetPos, targetEntity);
        
        if (targetBlockState.getBlock().equals(Blocks.AMETHYST_CLUSTER)) {
            dropped = List.of(new ItemStack(ItemContent.FLUXITE));
            ParticleContent.CHARGING.spawn(world, Vec3d.of(targetPos), 1);
        }
        
        // yes, this will discard items that wont fit anymore
        for (var stack : dropped) {
            this.inventory.addStack(stack);
        }
        
        try {
            targetBlockState.getBlock().onBreak(world, targetPos, targetBlockState, null);
        } catch (Exception exception) {
            Oritech.LOGGER.warn("Laser arm block break event failure when breaking " + targetBlockState + " at " + targetPos + ": " + exception.getLocalizedMessage());
        }
        world.addBlockBreakParticles(targetPos, world.getBlockState(targetPos));
        world.playSound(null, targetPos, targetBlockState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1f, 1f);
        world.breakBlock(targetPos, false);
        
        findNextBlockBreakTarget();
    }
    
    private void findNextBlockBreakTarget() {
        
        if (pendingArea != null && !pendingArea.isEmpty()) {
            trySetNewTarget(pendingArea.pop(), false);
            if (pendingArea.isEmpty()) pendingArea = null;
            return;
        }
        
        var direction = Vec3d.of(targetDirection.subtract(pos.up())).normalize();
        var from = Vec3d.of(pos.up()).add(0.5, 0.55, 0.5).add(direction.multiply(1.5));
        
        var nextBlock = basicRaycast(from, direction, range);
        if (nextBlock == null) return;
        
        var maxSize = (int) from.distanceTo(nextBlock.toCenterPos()) - 1;
        var scanDist = Math.min(areaSize, maxSize);
        if (scanDist > 1)
            pendingArea = findNextAreaBlockTarget(nextBlock, scanDist);
        
        
        trySetNewTarget(nextBlock, false);
        
    }
    
    // returns the first block in an X*X*X cube, from the outside in
    private ArrayDeque<BlockPos> findNextAreaBlockTarget(BlockPos center, int scanDist) {
        
        var targets = new ArrayList<BlockPos>();
        
        for (int x = -scanDist; x < scanDist; x++) {
            for (int y = -scanDist; y < scanDist; y++) {
                for (int z = -scanDist; z < scanDist; z++) {
                    var pos = center.add(x, y, z);
                    if (!canPassThrough(world.getBlockState(pos)) && !center.equals(pos))
                        targets.add(pos);
                }
            }
        }
        
        targets.sort(Comparator.comparingInt(pos::getManhattanDistance));
        return new ArrayDeque<>(targets);
    }
    
    private BlockPos basicRaycast(Vec3d from, Vec3d direction, int range) {
        
        var searchOffset = 0.45;
        
        for (float i = 0; i < range; i += 0.3f) {
            var to = from.add(direction.multiply(i));
            var targetBlockPos = BlockPos.ofFloored(to.add(0, 0.3f, 0));
            var targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetTop = to.add(0, -searchOffset, 0);
            targetBlockPos = BlockPos.ofFloored(offsetTop);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetLeft = to.add(-searchOffset, 0, 0);
            targetBlockPos = BlockPos.ofFloored(offsetLeft);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetRight = to.add(searchOffset, 0, 0);
            targetBlockPos = BlockPos.ofFloored(offsetRight);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetFront = to.add(0, 0, searchOffset);
            targetBlockPos = BlockPos.ofFloored(offsetFront);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
            
            var offsetBack = to.add(0, 0, -searchOffset);
            targetBlockPos = BlockPos.ofFloored(offsetBack);
            targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState)) return targetBlockPos;
        }
        
        return null;
    }
    
    private boolean canPassThrough(BlockState state) {
        return state.isAir() || state.isIn(ConventionalBlockTags.GLASS_BLOCKS) || isUnfinishedAmethyst(state) || state.isLiquid();
    }
    
    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        
        areaSize = 1;
        
        MachineAddonController.super.gatherAddonStats(addons);
    }
    
    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        MachineAddonController.super.getAdditionalStatFromAddon(addonBlock);
        
        if (addonBlock.state().getBlock().equals(BlockContent.QUARRY_ADDON)) {
            areaSize++;
        }
        
    }
    
    private boolean isUnfinishedAmethyst(BlockState state) {
        return state.isOf(Blocks.SMALL_AMETHYST_BUD) || state.isOf(Blocks.MEDIUM_AMETHYST_BUD) || state.isOf(Blocks.LARGE_AMETHYST_BUD);
    }
    
    private int energyRequiredToFire() {
        return (int) (Oritech.CONFIG.laserArmConfig.energyPerTick() * (1 / addonData.speed()));
    }
    
    private void updateNetwork() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.LaserArmSyncPacket(pos, currentTarget, lastFiredAt, areaSize));
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GenericEnergySyncPacket(pos, energyStorage.amount, energyStorage.capacity));
        networkDirty = false;
    }
    
    public boolean setTargetFromDesignator(BlockPos targetPos) {
        var success = trySetNewTarget(targetPos, true);
        findNextBlockBreakTarget();
        
        return success;
    }
    
    private boolean trySetNewTarget(BlockPos targetPos, boolean alsoSetDirection) {
        
        // if target is coreblock, adjust it to point to controller if connected
        var targetState = Objects.requireNonNull(world).getBlockState(targetPos);
        if (targetState.getBlock() instanceof MachineCoreBlock && targetState.get(MachineCoreBlock.USED)) {
            var coreEntity = (MachineCoreEntity) world.getBlockEntity(targetPos);
            var controllerPos = Objects.requireNonNull(coreEntity).getControllerPos();
            if (controllerPos != null) targetPos = controllerPos;
        }
        
        var distance = targetPos.getManhattanDistance(pos);
        var blockHardness = targetState.getBlock().getHardness();
        if (distance > range || blockHardness < 0.0) {
            return false;
        }
        
        this.targetBlockEnergyNeeded = (int) (BLOCK_BREAK_ENERGY * Math.sqrt(blockHardness) *  addonData.efficiency());
        this.currentTarget = targetPos;
        
        if (alsoSetDirection) {
            this.targetDirection = targetPos;
            pendingArea = null;
            networkDirty = true;
        }
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
        nbt.putBoolean("redstone", redstonePowered);
        nbt.putInt("areaSize", areaSize);
        
        if (targetDirection != null && currentTarget != null) {
            nbt.putLong("target_position", currentTarget.asLong());
            nbt.putLong("target_direction", targetDirection.asLong());
        }
        
        if (pendingArea != null && !pendingArea.isEmpty()) {
            var positions = pendingArea.stream().mapToLong(BlockPos::asLong).toArray();
            nbt.putLongArray("pendingPositions", positions);
        } else {
            nbt.remove("pendingPositions");
        }
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory.heldStacks);
        loadMultiblockNbtData(nbt);
        loadAddonNbtData(nbt);
        
        updateEnergyContainer();
        
        redstonePowered = nbt.getBoolean("redstone");
        energyStorage.amount = nbt.getLong("energy_stored");
        targetDirection = BlockPos.fromLong(nbt.getLong("target_direction"));
        currentTarget = BlockPos.fromLong(nbt.getLong("target_position"));
        areaSize = nbt.getInt("areaSize");
        
        if (nbt.contains("pendingPositions")) {
            pendingArea = Arrays.stream(nbt.getLongArray("pendingPositions")).mapToObj(BlockPos::fromLong).collect(Collectors.toCollection(ArrayDeque::new));
        }
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
    public EnergyStorage getStorage(Direction direction) {
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
        return Oritech.CONFIG.laserArmConfig.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.laserArmConfig.maxEnergyInsertion();
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
                if (isFiring()) {
                    return state.setAndContinue(MachineBlockEntity.WORKING);
                } else {
                    return state.setAndContinue(MachineBlockEntity.IDLE);
                }
            } else {
                return state.setAndContinue(MachineBlockEntity.PACKAGED);
            }
        }).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>());
    }
    
    @Override
    public void playSetupAnimation() {
        animationController.setAnimation(MachineBlockEntity.SETUP);
        animationController.forceAnimationReset();
    }
    
    public boolean isActive(BlockState state) {
        return state.get(ASSEMBLED);
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return inventoryStorage;
    }
    //endregion
    
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
    
    public void setCurrentTarget(BlockPos currentTarget) {
        this.currentTarget = currentTarget;
    }
    
    public long getLastFiredAt() {
        return lastFiredAt;
    }
    
    
    @Override
    public BlockPos getMachinePos() {
        return getPos();
    }
    
    @Override
    public World getMachineWorld() {
        return getWorld();
    }
    
    public void setLastFiredAt(long lastFiredAt) {
        this.lastFiredAt = lastFiredAt;
    }
    
    public boolean isFiring() {
        var idleTime = world.getTime() - lastFiredAt;
        return idleTime < 3;
    }
    
    public boolean isTargetingAtomicForge() {
        return world.getBlockState(currentTarget).getBlock().equals(BlockContent.ATOMIC_FORGE_BLOCK);
    }
    
    public boolean isTargetingDeepdrill() {
        return world.getBlockState(currentTarget).getBlock().equals(BlockContent.DEEP_DRILL_BLOCK);
    }
    
    public boolean isTargetingEnergyContainer() {
        var storageCandidate = EnergyStorage.SIDED.find(world, currentTarget, null);
        return storageCandidate != null || isTargetingAtomicForge() || isTargetingDeepdrill();
    }
    
    public boolean isTargetingBuddingAmethyst() {
        return world.getBlockState(currentTarget).getBlock() instanceof BuddingAmethystBlock;
    }
    
    @Override
    public List<Pair<Text, Text>> getExtraExtensionLabels() {
        
        if (areaSize == 1) return ScreenProvider.super.getExtraExtensionLabels();
        
        return List.of(new Pair<>(Text.literal("Beam Size: " + areaSize), Text.literal("Maximum beam width")));
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 117, 20),
          new GuiSlot(1, 117, 38),
          new GuiSlot(2, 117, 56));
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return energyRequiredToFire();
    }
    
    @Override
    public float getProgress() {
        return 0;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.LASER_SCREEN;
    }
    
    @Override
    public Property<Direction> getBlockFacingProperty() {
        return ScreenProvider.super.getBlockFacingProperty();
    }
    
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.getPos());
        buf.write(ADDON_UI_ENDEC, getUiData());
        buf.writeFloat(getCoreQuality());
        updateNetwork();
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this, getUiData(), getCoreQuality());
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
}
