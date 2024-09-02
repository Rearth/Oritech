package rearth.oritech.block.entity.machines.interaction;

import com.mojang.authlib.GameProfile;

import dev.emi.emi.api.widget.Bounds;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.behavior.LaserArmBlockBehavior;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.blocks.machines.interaction.LaserArmBlock;
import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.processing.AtomicForgeBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.datagen.data.TagContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public class LaserArmBlockEntity extends BlockEntity implements GeoBlockEntity, BlockEntityTicker<LaserArmBlockEntity>, EnergyProvider, ScreenProvider, ExtendedScreenHandlerFactory, MultiblockMachineController, MachineAddonController, InventoryProvider {
    
    public static final String LASER_PLAYER_NAME = "oritech_laser";
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
    public int yieldAddons = 0;
    public int hunterAddons = 0;
    public boolean hasCropFilterAddon = false;
    
    // config
    private final int range = Oritech.CONFIG.laserArmConfig.range();

    private Vec3d laserHead;
    
    // working data
    private BlockPos targetDirection;
    private BlockPos currentTarget;
    private LivingEntity currentLivingTarget;
    private long lastFiredAt;
    private int progress;
    private int targetBlockEnergyNeeded = BLOCK_BREAK_ENERGY;
    private boolean networkDirty;
    private boolean redstonePowered;
    private ArrayDeque<BlockPos> pendingArea;
    private ArrayDeque<LivingEntity> pendingLivingTargets;
    
    // needed only on client
    public Vec3d lastRenderPosition;
    private PlayerEntity laserPlayerEntity = null;
    
    public LaserArmBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LASER_ARM_ENTITY, pos, state);
        laserHead = Vec3d.of(pos.up()).add(0.5, 0.55, 0.5);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, LaserArmBlockEntity blockEntity) {
        if (world.isClient() || !isActive(state) || redstonePowered || (hunterAddons == 0 && currentTarget == null) || energyStorage.getAmount() < energyRequiredToFire())
            return;
        
        if (hunterAddons > 0) {
            fireAtLivingEntities(world, pos, state, blockEntity);
        }
        else {
            fireAtBlocks(world, pos, state, blockEntity);
        }
    
        if (networkDirty)
            updateNetwork();
    }

    private void fireAtBlocks(World world, BlockPos pos, BlockState state, LaserArmBlockEntity blockEntity) {
        var targetBlockPos = currentTarget;
        var targetBlockState = world.getBlockState(targetBlockPos);
        var targetBlock = targetBlockState.getBlock();
        var targetBlockEntity = world.getBlockEntity(targetBlockPos);

        LaserArmBlockBehavior behavior = LaserArmBlock.getBehaviorForBlock(targetBlock);
        boolean fired = false;
        if (behavior.fireAtBlock(world, this, targetBlock, targetBlockPos, targetBlockState, targetBlockEntity)) {
            energyStorage.amount -= energyRequiredToFire();
            lastFiredAt = world.getTime();
            networkDirty = true;
        } else {
            findNextBlockBreakTarget();
        }
    }

    private void fireAtLivingEntities(World world, BlockPos pos, BlockState state, LaserArmBlockEntity blockEntity) {
        // check that there is a target, that is still alive and still in range
        if (currentLivingTarget != null && validTarget(currentLivingTarget)) {

            var behavior = LaserArmBlock.getBehaviorForEntity(currentLivingTarget.getType());
            if (behavior.fireAtEntity(world, this, currentLivingTarget)) {
                energyStorage.amount -= energyRequiredToFire();
                this.targetDirection = currentLivingTarget.getBlockPos();
                lastFiredAt = world.getTime();
                networkDirty = true;
            } else {
                pendingLivingTargets.remove(currentLivingTarget);
                currentLivingTarget = null;
                currentTarget = null;
                networkDirty = true;
            };
        }

        findNextLivingTarget();
    }
    
    public void setRedstonePowered(boolean redstonePowered) {
        this.redstonePowered = redstonePowered;
    }

    public void addBlockBreakProgress(int progress) {
        this.progress += progress;
    }

    public int getBlockBreakProgress() {
        return this.progress;
    }

    public int getTargetBlockEnergyNeeded() {
        return targetBlockEnergyNeeded;
    }
    
    public void finishBlockBreaking(BlockPos targetPos, BlockState targetBlockState) {
        progress -= targetBlockEnergyNeeded;
        
        var targetEntity = world.getBlockEntity(targetPos);
        List<ItemStack> dropped;
        if (yieldAddons > 0) {
            dropped = DestroyerBlockEntity.getLootDrops(targetBlockState, (ServerWorld) world, targetPos, targetEntity, yieldAddons);
        } else {
            dropped = Block.getDroppedStacks(targetBlockState, (ServerWorld) world, targetPos, targetEntity);
        }
        
        if (targetBlockState.getBlock().equals(Blocks.AMETHYST_CLUSTER)) {
            var farmedCount = 1 + yieldAddons;
            dropped = List.of(new ItemStack(ItemContent.FLUXITE, farmedCount));
            ParticleContent.CHARGING.spawn(world, Vec3d.of(targetPos), 1);
        }
        
        // yes, this will discard items that wont fit anymore
        for (var stack : dropped) {
            this.inventory.addStack(stack);
        }
        
        try {
            targetBlockState.getBlock().onBreak(world, targetPos, targetBlockState, getLaserPlayerEntity());
        } catch (Exception exception) {
            Oritech.LOGGER.warn("Laser arm block break event failure when breaking " + targetBlockState + " at " + targetPos + ": " + exception.getLocalizedMessage());
        }
        world.addBlockBreakParticles(targetPos, world.getBlockState(targetPos));
        world.playSound(null, targetPos, targetBlockState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1f, 1f);
        world.breakBlock(targetPos, false);
        
        findNextBlockBreakTarget();
    }
    
    public PlayerEntity getLaserPlayerEntity() {
        if (laserPlayerEntity == null) {
            laserPlayerEntity = new PlayerEntity(world, pos, 0, new GameProfile(UUID.randomUUID(), LASER_PLAYER_NAME)) {
                @Override
                public boolean isSpectator() {
                    return false;
                }
                
                @Override
                public boolean isCreative() {
                    return false;
                }

                @Override
                public boolean canTakeDamage() {
                    return false;
                }

                @Override
                public boolean giveItemStack(ItemStack itemStack) {
                    LaserArmBlockEntity.this.inventory.addStack(itemStack);
                    return true;
                }
            };
        }

        if (hunterAddons > 0 && yieldAddons > 0) {
            var lootingSword = new ItemStack(Items.NETHERITE_SWORD);
            lootingSword.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
            var lootingEntry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.LOOTING).get();
            lootingSword.addEnchantment(lootingEntry, Math.min(yieldAddons, 3));
            laserPlayerEntity.getInventory().main.set(laserPlayerEntity.getInventory().selectedSlot, lootingSword);
        }
        
        return laserPlayerEntity;
    }
    
    private void findNextBlockBreakTarget() {
        
        while (pendingArea != null && !pendingArea.isEmpty()) {
            if (trySetNewTarget(pendingArea.pop(), false)) {
                if (pendingArea.isEmpty()) pendingArea = null;
                return;
            }
        }
        
        var direction = Vec3d.of(targetDirection.subtract(pos.up())).normalize();
        var from = laserHead.add(direction.multiply(1.5));
        
        var nextBlock = basicRaycast(from, direction, range, 0.45F);
        if (nextBlock == null) return;
        
        var maxSize = (int) from.distanceTo(nextBlock.toCenterPos()) - 1;
        var scanDist = Math.min(areaSize, maxSize);
        if (scanDist > 1)
            pendingArea = findNextAreaBlockTarget(nextBlock, scanDist);
        
        
        trySetNewTarget(nextBlock, false);
        
    }

    private double hunterRange() {
        // hunter range is 2^areaSize, with max areaSize of 3
        // range should be calculated near the center of the laser head's cube, so add 0.5 to start counting range from side of cube
        return Math.pow(4, Math.min(areaSize, 3)) + 0.5;
    }

    private boolean canSee(LivingEntity entity) {
        if (entity.getWorld() != this.getWorld() || entity.isInvisible()) {
            return false;
        } else {
            var target = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
            var direction = target.subtract(laserHead).normalize();
            if (laserHead.distanceTo(target) > 128.0) {
                return false;
            } else {
                // can see if basicRaycast() doesn't find anything it can't pass through between laser and target
                return basicRaycast(laserHead.add(direction.multiply(1.5)), direction, (int)(laserHead.distanceTo(target) - 0.5), 0.0F) == null;
            }
        }
    }

    private boolean validTarget(LivingEntity entity) {
        return entity.isAlive() && canSee(entity) && entity.getPos().isInRange(pos.up().toCenterPos(), hunterRange());
    }

    private void findNextLivingTarget() {
        // delay between searches
        if (world.getTime() % 40 != 0)
            return;

        while (pendingLivingTargets != null && !pendingLivingTargets.isEmpty()) {
            // to target the closest entity, the list of nearby entities would need to searched/sorted every time a new target is picked
            // not a big deal when there are only a couple of entities, but could really bog down if entities are crammed
            // for now, just let the laser target the first in the deque regardless of distance
            var nextEntity = pendingLivingTargets.peek();
            // if there is no closest, or the closest is out of range, then the cached list of nearby entities can be discarded
            // checking canSee() for the targetted entity every time is probably the slowest part of this, but the laser *really* shouldn't be targetting
            // entities that have gone around a corner or turned invisible
            if (nextEntity == null || !validTarget(nextEntity)) {
                currentLivingTarget = null;
                currentTarget = null;
                pendingLivingTargets.pop();
            } else {
                // keep targetting the same entity until it's out of sight, out of range, or no longer exists
                currentLivingTarget = nextEntity;
                currentTarget = currentLivingTarget.getBlockPos();
                break;
            }
        }

        var entityRange = hunterRange();
        // Only sort the list when getting a new list of entities in range.
        // The entities can move around so the sort order isn't guaranteed to be correct because entities can move around, but it should be good enough.
        // There's no need to spend the time
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, new Box(laserHead.x - entityRange, laserHead.y - entityRange, laserHead.z - entityRange, laserHead.x + entityRange, laserHead.y + entityRange, laserHead.z + entityRange), EntityPredicates.VALID_LIVING_ENTITY.and(EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));
        Collections.sort(targets, Comparator.comparingDouble((entity) -> entity.squaredDistanceTo(laserHead)));
        pendingLivingTargets = new ArrayDeque<>(targets);
    }
    
    // returns the first block in an X*X*X cube, from the outside in
    private ArrayDeque<BlockPos> findNextAreaBlockTarget(BlockPos center, int scanDist) {
        
        var targets = new ArrayList<BlockPos>();
        
        for (int x = -scanDist; x < scanDist; x++) {
            for (int y = -scanDist; y < scanDist; y++) {
                for (int z = -scanDist; z < scanDist; z++) {
                    var pos = center.add(x, y, z);
                    if (!canPassThrough(world.getBlockState(pos), pos) && !center.equals(pos))
                        targets.add(pos);
                }
            }
        }
        
        targets.sort(Comparator.comparingInt(pos::getManhattanDistance));
        return new ArrayDeque<>(targets);
    }
    
    private BlockPos basicRaycast(Vec3d from, Vec3d direction, int range, float searchOffset) {
        
        for (float i = 0; i < range; i += 0.3f) {
            var to = from.add(direction.multiply(i));
            var targetBlockPos = BlockPos.ofFloored(to.add(0, 0.3f, 0));
            var targetState = world.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;

            if (searchOffset == 0.0F)
                return null;
            
            var offsetTop = to.add(0, -searchOffset, 0);
            targetBlockPos = BlockPos.ofFloored(offsetTop);
            targetState = world.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            var offsetLeft = to.add(-searchOffset, 0, 0);
            targetBlockPos = BlockPos.ofFloored(offsetLeft);
            targetState = world.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            var offsetRight = to.add(searchOffset, 0, 0);
            targetBlockPos = BlockPos.ofFloored(offsetRight);
            targetState = world.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            var offsetFront = to.add(0, 0, searchOffset);
            targetBlockPos = BlockPos.ofFloored(offsetFront);
            targetState = world.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            var offsetBack = to.add(0, 0, -searchOffset);
            targetBlockPos = BlockPos.ofFloored(offsetBack);
            targetState = world.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
        }
        
        return null;
    }
    
    private boolean isSearchTerminatorBlock(BlockState state) {
        return state.getBlock().equals(Blocks.TARGET);
    }
    
    public boolean canPassThrough(BlockState state, BlockPos blockPos) {
        return state.isAir() || state.isLiquid() || state.isIn(TagContent.LASER_PASSTHROUGH) || (hunterAddons > 0 && !state.isSolidBlock(world, blockPos));
    }
    
    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        
        areaSize = 1;
        yieldAddons = 0;
        hunterAddons = 0;
        hasCropFilterAddon = false;
        
        MachineAddonController.super.gatherAddonStats(addons);
    }
    
    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        MachineAddonController.super.getAdditionalStatFromAddon(addonBlock);
        
        if (addonBlock.state().getBlock().equals(BlockContent.QUARRY_ADDON))
            areaSize++;
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_HUNTER_ADDON))
            hunterAddons++;
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_YIELD_ADDON))
            yieldAddons++;
        if (addonBlock.state().getBlock().equals(BlockContent.CROP_FILTER_ADDON))
            hasCropFilterAddon = true;
        
    }
    
    public int energyRequiredToFire() {
        return (int) (Oritech.CONFIG.laserArmConfig.energyPerTick() * (1 / addonData.speed()));
    }

    public float getDamageTick() {
        return (float)Math.pow(2, Math.min(3, hunterAddons));
    }
    
    private void updateNetwork() {
        var sendTarget = currentTarget != null ? currentTarget : BlockPos.ORIGIN;
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.LaserArmSyncPacket(pos, sendTarget, lastFiredAt, areaSize, yieldAddons, hunterAddons, hasCropFilterAddon));
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
        if (distance > range || blockHardness < 0.0 || targetState.getBlock().equals(Blocks.AIR)) {
            return false;
        }
        
        this.targetBlockEnergyNeeded = (int) (BLOCK_BREAK_ENERGY * Math.sqrt(blockHardness) * addonData.efficiency());
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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        addMultiblockToNbt(nbt);
        writeAddonToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        nbt.putBoolean("redstone", redstonePowered);
        nbt.putInt("areaSize", areaSize);
        nbt.putInt("yieldAddons", yieldAddons);
        nbt.putInt("hunterAddons", hunterAddons);
        nbt.putBoolean("cropAddon", hasCropFilterAddon);
        
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
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        loadMultiblockNbtData(nbt);
        loadAddonNbtData(nbt);
        
        updateEnergyContainer();
        
        redstonePowered = nbt.getBoolean("redstone");
        energyStorage.amount = nbt.getLong("energy_stored");
        targetDirection = BlockPos.fromLong(nbt.getLong("target_direction"));
        currentTarget = BlockPos.fromLong(nbt.getLong("target_position"));
        areaSize = nbt.getInt("areaSize");
        yieldAddons = nbt.getInt("yieldAddons");
        hunterAddons = nbt.getInt("hunterAddons");
        hasCropFilterAddon = nbt.getBoolean("cropAddon");
        
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
    
    public boolean isTargetingCatalyst() {
        return world.getBlockState(currentTarget).getBlock().equals(BlockContent.ENCHANTMENT_CATALYST_BLOCK);
    }
    
    public boolean isTargetingEnergyContainer() {
        var storageCandidate = EnergyStorage.SIDED.find(world, currentTarget, null);
        return storageCandidate != null || isTargetingAtomicForge() || isTargetingDeepdrill() || isTargetingCatalyst();
    }
    
    public boolean isTargetingBuddingAmethyst() {
        return world.getBlockState(currentTarget).getBlock() instanceof BuddingAmethystBlock;
    }
    
    @Override
    public List<Pair<Text, Text>> getExtraExtensionLabels() {
        if (areaSize == 1 && yieldAddons == 0 && hunterAddons == 0) return ScreenProvider.super.getExtraExtensionLabels();
        if (hunterAddons > 0)
            return List.of(new Pair<>(Text.translatable("title.oritech.machine.addon_range", (int)hunterRange()), Text.translatable("tooltip.oritech.laser_arm.addon_hunter_range")), new Pair<>(Text.translatable("title.oritech.laser_arm.addon_hunter_damage", (int)getDamageTick()), Text.translatable("Effective Beam Damage")), new Pair<>(Text.translatable("title.oritech.machine.addon_looting", yieldAddons), Text.translatable("tooltip.oritech.machine.addon_looting")));
        return List.of(new Pair<>(Text.literal(areaSize + " Size"), Text.literal("Effective Beam Radius")), new Pair<>(Text.literal(yieldAddons + " Fortune"), Text.literal("Yield addon count. 3 is the effective maximum")));
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 117, 20, true),
          new GuiSlot(1, 117, 38, true),
          new GuiSlot(2, 117, 56, true));
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
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        updateNetwork();
        return new ModScreens.UpgradableData(pos, getUiData(), getCoreQuality());
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
