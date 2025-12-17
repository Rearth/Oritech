package rearth.oritech.block.entity.interaction;

import com.mojang.authlib.GameProfile;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.WorldPacketCodec;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.behavior.LaserArmBlockBehavior;
import rearth.oritech.block.blocks.interaction.LaserArmBlock;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.addons.CombiAddonEntity;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.stream.Collectors;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;


public class LaserArmBlockEntity extends NetworkedBlockEntity implements
  GeoBlockEntity, EnergyApi.BlockProvider, ScreenProvider, ExtendedMenuProvider, MultiblockMachineController, MachineAddonController,
    ItemApi.BlockProvider, RedstoneAddonBlockEntity.RedstoneControllable, ColorableMachine {
    
    private static final String LASER_PLAYER_NAME = "oritech_laser";
    public static final int BLOCK_BREAK_ENERGY = Oritech.CONFIG.laserArmConfig.blockBreakEnergyBase();
    
    // storage
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), 0, this::setChanged);
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(3, this::setChanged);
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<LaserArmBlockEntity> animationController = getAnimationController();
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    // addons
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> openSlots = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private float coreQuality = 1f;
    @SyncField(SyncType.GUI_OPEN)
    private BaseAddonData addonData = BaseAddonData.DEFAULT_ADDON_DATA;
    @SyncField(SyncType.GUI_OPEN)
    public int areaSize = 1;
    @SyncField(SyncType.GUI_OPEN)
    public int yieldAddons = 0;
    @SyncField(SyncType.GUI_OPEN)
    public int hunterAddons = 0;
    @SyncField(SyncType.GUI_OPEN)
    public boolean hasCropFilterAddon = false;
    @SyncField(SyncType.GUI_OPEN)
    public boolean hasSilkTouchAddon = false;
    
    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorableMachine.ColorVariant currentColor = ColorVariant.ORANGE;
    
    // config
    private final int range = Oritech.CONFIG.laserArmConfig.range();
    
    public Vec3 laserHead;
    
    // working data
    private BlockPos targetDirection;
    
    @SyncField
    private BlockPos currentTarget = BlockPos.ZERO;
    @SyncField
    public HunterTargetMode hunterTargetMode = HunterTargetMode.HOSTILE_ONLY;
    @SyncField
    private LivingEntity currentLivingTarget;
    @SyncField
    private long lastFiredAt;
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    private boolean redstonePowered;
    private int progress;
    private ArrayDeque<BlockPos> pendingArea;
    private final ArrayDeque<LivingEntity> pendingLivingTargets = new ArrayDeque<>();
    private int targetBlockEnergyNeeded = BLOCK_BREAK_ENERGY;
    
    // needed only on client
    public Vec3 lastRenderPosition;
    private Player laserPlayerEntity = null;
    
    public LaserArmBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LASER_ARM_ENTITY, pos, state);
        laserHead = getLaserHeadPosition().getCenter();
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (!isActive(state))
            return;
        
        if (!redstonePowered && energyStorage.getAmount() >= energyRequiredToFire()) {
            if (hunterAddons > 0) {
                fireAtLivingEntities(world, pos, state, this);
            } else if (currentTarget != null && !currentTarget.equals(BlockPos.ZERO)) {
                fireAtBlocks(world, pos, state, this);
            } else if (targetDirection != null && !targetDirection.equals(BlockPos.ZERO) && (world.getGameTime() + pos.getZ()) % 40 == 0) {
                // target pos is set, but no target is found (e.g. all blocks already mined). Periodically scan again for new blocks.
                findNextBlockBreakTarget();
            }
        }
    }
    
    private void fireAtBlocks(Level world, BlockPos pos, BlockState state, LaserArmBlockEntity blockEntity) {
        var targetBlockPos = currentTarget;
        var targetBlockState = world.getBlockState(targetBlockPos);
        var targetBlock = targetBlockState.getBlock();
        var targetBlockEntity = world.getBlockEntity(targetBlockPos);
        
        LaserArmBlockBehavior behavior = LaserArmBlock.getBehaviorForBlock(targetBlock);
        boolean fired = false;
        if (behavior.fireAtBlock(world, this, targetBlock, targetBlockPos, targetBlockState, targetBlockEntity)) {
            energyStorage.amount -= energyRequiredToFire();
            lastFiredAt = world.getGameTime();
        } else {
            findNextBlockBreakTarget();
        }
    }
    
    private void fireAtLivingEntities(Level world, BlockPos pos, BlockState state, LaserArmBlockEntity blockEntity) {
        // check that there is a target, that is still alive and still in range
        if (currentLivingTarget != null && validTarget(currentLivingTarget)) {
            
            var behavior = LaserArmBlock.getBehaviorForEntity(currentLivingTarget.getType());
            if (behavior.fireAtEntity(world, this, currentLivingTarget)) {
                energyStorage.amount -= energyRequiredToFire();
                this.targetDirection = currentLivingTarget.blockPosition();
                lastFiredAt = world.getGameTime();
            } else {
                pendingLivingTargets.remove(currentLivingTarget);
                currentLivingTarget = null;
                currentTarget = BlockPos.ZERO;
            }
        } else {
            loadNextLivingTarget();
        }
        
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
        
        var targetEntity = level.getBlockEntity(targetPos);
        List<ItemStack> dropped;
        // added getLaserPlayerEntity() to make ae2 certus quartz drop from certus
        // quartz clusters because it's expecting an entity in
        // LootContextParameters.THIS_ENTITY
        if (hasSilkTouchAddon) {
            dropped = DestroyerBlockEntity.getSilkTouchDrops(targetBlockState, (ServerLevel) level, targetPos, targetEntity, getLaserPlayerEntity());
        } else if (yieldAddons > 0) {
            dropped = DestroyerBlockEntity.getLootDrops(targetBlockState, (ServerLevel) level, targetPos, targetEntity, yieldAddons, getLaserPlayerEntity());
        } else {
            dropped = Block.getDrops(targetBlockState, (ServerLevel) level, targetPos, targetEntity, getLaserPlayerEntity(), ItemStack.EMPTY);
        }
        
        var blockRecipe = tryGetRecipeOfBlock(targetBlockState, level);
        if (blockRecipe != null) {
            var recipe = blockRecipe.value();
            var farmedCount = 1 + yieldAddons;
            dropped = List.of(new ItemStack(recipe.getResults().get(0).getItem(), farmedCount));
            ParticleContent.CHARGING.spawn(level, Vec3.atLowerCornerOf(targetPos), 1);
        }
        
        // yes, this will discard items that wont fit anymore
        for (var stack : dropped) {
            this.inventory.insert(stack, false);
        }
        
        try {
            targetBlockState.getBlock().playerWillDestroy(level, targetPos, targetBlockState, getLaserPlayerEntity());
        } catch (Exception exception) {
            Oritech.LOGGER.warn("Laser arm block break event failure when breaking " + targetBlockState + " at " + targetPos + ": " + exception.getLocalizedMessage());
        }
        level.addDestroyBlockEffect(targetPos, level.getBlockState(targetPos));
        level.playSound(null, targetPos, targetBlockState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
        level.destroyBlock(targetPos, false);
        
        findNextBlockBreakTarget();
    }
    
    public static RecipeHolder<OritechRecipe> tryGetRecipeOfBlock(BlockState destroyed, Level world) {
        var inputItem = destroyed.getBlock().asItem();
        var inputInv = new SimpleCraftingInventory(new ItemStack(inputItem));
        var candidate = world.getRecipeManager().getRecipeFor(RecipeContent.LASER, inputInv, world);
        return candidate.orElse(null);
    }
    
    public Player getLaserPlayerEntity() {
        if (!(level instanceof ServerLevel))
            return null;
        
        if (laserPlayerEntity == null) {
            laserPlayerEntity = FakeMachinePlayer.create((ServerLevel) level, new GameProfile(UUID.randomUUID(), LASER_PLAYER_NAME), inventory);
        }
        
        if (hunterAddons > 0 && yieldAddons > 0) {
            var lootingSword = new ItemStack(Items.NETHERITE_SWORD);
            lootingSword.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
            var lootingEntry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.LOOTING).get();
            lootingSword.enchant(lootingEntry, Math.min(yieldAddons, 3));
            laserPlayerEntity.getInventory().items.set(laserPlayerEntity.getInventory().selected, lootingSword);
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
        
        var direction = Vec3.atLowerCornerOf(targetDirection.subtract(getLaserHeadPosition())).normalize();
        var from = laserHead.add(direction.scale(1.5));
        
        var nextBlock = basicRaycast(from, direction, range, 0.45F);
        if (nextBlock == null) {
            currentTarget = BlockPos.ZERO;
            return;
        }
        
        var maxSize = (int) from.distanceTo(nextBlock.getCenter()) - 1;
        var scanDist = Math.min(areaSize, maxSize);
        if (scanDist > 1)
            pendingArea = findNextAreaBlockTarget(nextBlock, scanDist);
        
        
        if (!trySetNewTarget(nextBlock, false)) {
            currentTarget = BlockPos.ZERO;   // out of range or invalid for another reason
        }
        
    }
    
    private double hunterRange() {
        // hunter range is 2^hunterAddons, with max 3 hunterAddons
        // range should be calculated near the center of the laser head's cube, so add 0.5 to start counting range from side of cube
        return Math.pow(4, Math.min(hunterAddons, 3)) + 0.5;
    }
    
    private boolean canSee(LivingEntity entity) {
        if (entity.level() != this.getLevel() || entity.isInvisible()) {
            return false;
        } else {
            var target = entity.getEyePosition();
            var direction = target.subtract(laserHead).normalize();
            if (laserHead.distanceTo(target) > 128.0) {
                return false;
            } else {
                // can see if basicRaycast() doesn't find anything it can't pass through between laser and target
                return basicRaycast(laserHead.add(direction.scale(1.5)), direction, (int) (laserHead.distanceTo(target) - 1), 0.2f) == null;
            }
        }
    }
    
    private boolean validTarget(LivingEntity entity) {
        return entity.isAlive() && canSee(entity) && huntedTarget(entity) && entity.position().closerThan(getLaserHeadPosition().getCenter(), hunterRange());
    }
    
    private boolean huntedTarget(LivingEntity entity) {
        // Regardless of mode, laser will always target player to charge energy storing chestplate
        if (entity instanceof Player) return true;
        // Not including Allay, Villagers, Trader, Iron Golem, Snow Golem
        // Also not including pets
        return switch (hunterTargetMode) {
            case HunterTargetMode.HOSTILE_ONLY -> entity instanceof Enemy;
            case HunterTargetMode.HOSTILE_NEUTRAL -> {
                if ((entity instanceof Animal animal && animal.getLoveCause() == null) || entity instanceof WaterAnimal)
                    yield true;
                yield entity instanceof Enemy;
            }
            case HunterTargetMode.ALL -> true;
        };
    }
    
    // this only gets called if we don't have a target (e.g. null or not valid)
    private void loadNextLivingTarget() {
        
        // load targets if we don't have any (only every 10 ticks to save performance
        if (pendingLivingTargets.isEmpty() && (level.getGameTime() + worldPosition.asLong()) % 10 == 0) {
            updateEntityTargets();
        }
        
        // assign first target from cached, distance sorted target list
        while (!pendingLivingTargets.isEmpty()) {
            var candidate = pendingLivingTargets.pop();
            if (validTarget(candidate)) {
                currentLivingTarget = candidate;
                currentTarget = candidate.blockPosition();
                return;
            }
        }
    }
    
    private void updateEntityTargets() {
        var entityRange = hunterRange();
        // Only sort the list when getting a new list of entities in range.
        // The entities can move around so the sort order isn't guaranteed to be correct, but it should be good enough.
        // There's no need to spend the time re-sorting the list every time the laser needs to pick a new target from the cached list.
        var targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(laserHead.x - entityRange, laserHead.y - entityRange, laserHead.z - entityRange, laserHead.x + entityRange, laserHead.y + entityRange, laserHead.z + entityRange), EntitySelector.LIVING_ENTITY_STILL_ALIVE.and(EntitySelector.NO_CREATIVE_OR_SPECTATOR));
        targets.sort(Comparator.comparingDouble((entity) -> entity.distanceToSqr(laserHead)));
        pendingLivingTargets.addAll(targets);
    }
    
    // returns the first block in an X*X*X cube, from the outside in
    private ArrayDeque<BlockPos> findNextAreaBlockTarget(BlockPos center, int scanDist) {
        
        var targets = new ArrayList<BlockPos>();
        
        for (int x = -scanDist; x < scanDist; x++) {
            for (int y = -scanDist; y < scanDist; y++) {
                for (int z = -scanDist; z < scanDist; z++) {
                    var pos = center.offset(x, y, z);
                    if (!canPassThrough(level.getBlockState(pos), pos) && !center.equals(pos))
                        targets.add(pos);
                }
            }
        }
        
        targets.sort(Comparator.comparingInt(worldPosition::distManhattan));
        return new ArrayDeque<>(targets);
    }
    
    private BlockPos basicRaycast(Vec3 from, Vec3 direction, int range, float searchOffset) {
        
        for (float i = 0; i < range; i += 0.3f) {
            var to = from.add(direction.scale(i));
            var targetBlockPos = BlockPos.containing(to.add(0, searchOffset, 0));
            var targetState = level.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            
            if (searchOffset == 0.0F)
                return null;
            
            var offsetTop = to.add(0, -searchOffset, 0);
            targetBlockPos = BlockPos.containing(offsetTop);
            targetState = level.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            var offsetLeft = to.add(-searchOffset, 0, 0);
            targetBlockPos = BlockPos.containing(offsetLeft);
            targetState = level.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            var offsetRight = to.add(searchOffset, 0, 0);
            targetBlockPos = BlockPos.containing(offsetRight);
            targetState = level.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            var offsetFront = to.add(0, 0, searchOffset);
            targetBlockPos = BlockPos.containing(offsetFront);
            targetState = level.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
            
            var offsetBack = to.add(0, 0, -searchOffset);
            targetBlockPos = BlockPos.containing(offsetBack);
            targetState = level.getBlockState(targetBlockPos);
            if (isSearchTerminatorBlock(targetState)) return null;
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
        }
        
        return null;
    }
    
    private boolean isSearchTerminatorBlock(BlockState state) {
        return state.getBlock().equals(Blocks.TARGET);
    }
    
    public boolean canPassThrough(BlockState state, BlockPos blockPos) {
        // When targetting entities, don't let grass, vines, small mushrooms, pressure plates, etc. get in the way of the laser
        return state.isAir() || !state.getFluidState().isEmpty() || state.is(TagContent.LASER_PASSTHROUGH) || (hunterAddons > 0 && !state.isRedstoneConductor(level, blockPos));
    }
    
    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        
        areaSize = 1;
        yieldAddons = 0;
        hunterAddons = 0;
        hasCropFilterAddon = false;
        hasSilkTouchAddon = false;
        
        MachineAddonController.super.gatherAddonStats(addons);
        
        yieldAddons = Math.min(yieldAddons, 3);
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
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_SILK_TOUCH_ADDON))
            hasSilkTouchAddon = true;
        
        if (addonBlock.addonEntity() instanceof CombiAddonEntity combi) {
            areaSize = combi.getQuarryCount();
            yieldAddons =  combi.getYieldCount();
            hasCropFilterAddon = combi.hasCropFilter();
            hasSilkTouchAddon = combi.hasSilk();
        }
        
    }
    
    public int energyRequiredToFire() {
        return (int) (Oritech.CONFIG.laserArmConfig.energyPerTick() * (1 / addonData.speed()));
    }
    
    public float getDamageTick() {
        return (Oritech.CONFIG.laserArmConfig.damageTickBase() * (1 / addonData.speed()));
    }
    
    public boolean setTargetFromDesignator(BlockPos targetPos) {
        var success = trySetNewTarget(targetPos, true);
        findNextBlockBreakTarget();
        
        return success;
    }
    
    public void cycleHunterTargetMode() {
        hunterTargetMode = hunterTargetMode.next();
    }
    
    private boolean trySetNewTarget(BlockPos targetPos, boolean alsoSetDirection) {
        
        // if target is coreblock, adjust it to point to controller if connected
        var targetState = Objects.requireNonNull(level).getBlockState(targetPos);
        if (targetState.getBlock() instanceof MachineCoreBlock && targetState.getValue(MachineCoreBlock.USED)) {
            var coreEntity = (MachineCoreEntity) level.getBlockEntity(targetPos);
            var controllerPos = Objects.requireNonNull(coreEntity).getControllerPos();
            if (controllerPos != null) targetPos = controllerPos;
        }
        
        var distance = targetPos.distManhattan(worldPosition);
        var blockHardness = targetState.getBlock().defaultDestroyTime();
        if (distance > range || blockHardness < 0.0 || targetState.getBlock().equals(Blocks.AIR)) {
            return false;
        }
        
        this.targetBlockEnergyNeeded = (int) (BLOCK_BREAK_ENERGY * Math.pow(blockHardness, Oritech.CONFIG.blockBreakHardnessExponentialFactor()) * addonData.efficiency());
        
        if (targetState.is(TagContent.LASER_FAST_BREAKING))
            targetBlockEnergyNeeded /= 8;
        
        this.currentTarget = targetPos;
        
        if (alsoSetDirection) {
            this.targetDirection = targetPos;
            pendingArea = null;
            setChanged();
        }
        this.setChanged();
        
        return true;
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        addMultiblockToNbt(nbt);
        writeAddonToNbt(nbt);
        addColorToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        nbt.putBoolean("redstone", redstonePowered);
        nbt.putInt("areaSize", areaSize);
        nbt.putInt("yieldAddons", yieldAddons);
        nbt.putInt("hunterAddons", hunterAddons);
        nbt.putBoolean("cropAddon", hasCropFilterAddon);
        nbt.putBoolean("silkAddon", hasSilkTouchAddon);
        nbt.putInt("hunterTargetMode", hunterTargetMode.value);
        
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
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        loadMultiblockNbtData(nbt);
        loadAddonNbtData(nbt);
        loadColorFromNbt(nbt);
        
        updateEnergyContainer();
        
        redstonePowered = nbt.getBoolean("redstone");
        energyStorage.amount = nbt.getLong("energy_stored");
        targetDirection = BlockPos.of(nbt.getLong("target_direction"));
        currentTarget = BlockPos.of(nbt.getLong("target_position"));
        areaSize = nbt.getInt("areaSize");
        yieldAddons = nbt.getInt("yieldAddons");
        hunterAddons = nbt.getInt("hunterAddons");
        hunterTargetMode = HunterTargetMode.fromValue(nbt.getInt("hunterTargetMode"));
        hasCropFilterAddon = nbt.getBoolean("cropAddon");
        hasSilkTouchAddon = nbt.getBoolean("silkAddon");
        
        if (nbt.contains("pendingPositions")) {
            pendingArea = Arrays.stream(nbt.getLongArray("pendingPositions")).mapToObj(BlockPos::of).collect(Collectors.toCollection(ArrayDeque::new));
        }
    }
    
    //region multiblock
    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        var state = getBlockState();
        return state.getValue(BlockStateProperties.FACING).getOpposite();
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
    public float getCoreQuality() {
        return this.coreQuality;
    }
    
    @Override
    public void setCoreQuality(float quality) {
        this.coreQuality = quality;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return inventory;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(1, 0, 0)
        );
    }
    //endregion
    
    // energyprovider
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    //region addons
    @Override
    public List<BlockPos> getConnectedAddons() {
        return connectedAddons;
    }
    
    @Override
    public List<BlockPos> getOpenAddonSlots() {
        return openSlots;
    }
    
    @Override
    public Direction getFacingForAddon() {
        var state = getBlockState();
        return state.getValue(BlockStateProperties.FACING).getOpposite();
    }
    
    @Override
    public DynamicEnergyStorage getStorageForAddon() {
        return energyStorage;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryForAddon() {
        return inventory;
    }
    
    @Override
    public ScreenProvider getScreenProvider() {
        return null;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(-1, 0, 0)
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
            
            if (isActive(getBlockState())) {
                if (isFiring()) {
                    return state.setAndContinue(MachineBlockEntity.WORKING);
                } else {
                    return state.setAndContinue(MachineBlockEntity.IDLE);
                }
            } else {
                return state.setAndContinue(MachineBlockEntity.PACKAGED);
            }
        }).triggerableAnim("setup", MachineBlockEntity.SETUP).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>());
    }
    
    @Override
    public void triggerSetupAnimation() {
        triggerAnim("base_controller", "setup");
    }
    
    @Override
    public void sendUpdate(SyncType type, ServerPlayer player) {
        
        if (type.equals(SyncType.GUI_TICK)) {
            super.sendUpdate(SyncType.GUI_OPEN, player);
            return;
        }
        
        super.sendUpdate(type, player);
    }
    
    public boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    //endregion
    
    
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }
    
    public Vec3 getVisualTarget() {
        if (hunterAddons > 0 && currentLivingTarget != null) {
            return currentLivingTarget.getEyePosition().subtract(0.5f, 0, 0.5f);
        } else {
            return getCurrentTarget().getCenter();
        }
    }
    
    
    @Override
    public BlockPos getPosForAddon() {
        return getBlockPos();
    }
    
    @Override
    public Level getWorldForAddon() {
        return getLevel();
    }
    
    public boolean isFiring() {
        var idleTime = level.getGameTime() - lastFiredAt;
        return idleTime < 5;
    }
    
    @Override
    public int getTickUpdateInterval() {
        return 2;
    }
    
    public boolean isTargetingAtomicForge(Block block) {
        return block.equals(BlockContent.ATOMIC_FORGE_BLOCK);
    }
    
    public boolean isTargetingDeepdrill(Block block) {
        return block.equals(BlockContent.DEEP_DRILL_BLOCK);
    }
    
    public boolean isTargetingCatalyst(Block block) {
        return block.equals(BlockContent.ENCHANTMENT_CATALYST_BLOCK);
    }
    
    public boolean isTargetingUnstableContainer(Block block) {
        return block.equals(BlockContent.UNSTABLE_CONTAINER);
    }
    
    public boolean isTargetingEnergyContainer() {
        var storageCandidate = EnergyApi.BLOCK.find(level, currentTarget, null);
        var block = level.getBlockState(currentTarget).getBlock();
        return storageCandidate != null || isTargetingAtomicForge(block) || isTargetingDeepdrill(block) || isTargetingCatalyst(block) || isTargetingUnstableContainer(block);
    }
    
    public boolean isTargetingBuddingAmethyst() {
        return level.getBlockState(currentTarget).is(TagContent.LASER_ACCELERATED);
    }
    
    @Override
    public List<Tuple<Component, Component>> getExtraExtensionLabels() {
        if (areaSize == 1 && yieldAddons == 0 && hunterAddons == 0 && !hasSilkTouchAddon)
            return ScreenProvider.super.getExtraExtensionLabels();
        if (hunterAddons > 0)
            return List.of(
              new Tuple<>(Component.translatable("title.oritech.machine.addon_range", (int) hunterRange()), Component.translatable("tooltip.oritech.laser_arm.addon_hunter_range")),
              new Tuple<>(Component.translatable("title.oritech.laser_arm.addon_hunter_damage", String.format("%.2f", getDamageTick())), Component.translatable("tooltip.oritech.laser_arm.addon_hunter_damage")),
              new Tuple<>(Component.translatable("title.oritech.machine.addon_looting", yieldAddons), Component.translatable("tooltip.oritech.machine.addon_looting")));
        
        if (hasSilkTouchAddon) {
            return List.of(
              new Tuple<>(Component.translatable("title.oritech.machine.addon_range", areaSize), Component.translatable("tooltip.oritech.laser_arm.addon_range")),
              new Tuple<>(Component.translatable("enchantment.minecraft.silk_touch"), Component.translatable("tooltip.oritech.machine.addon_silk_touch")));
        }
        
        return List.of(
          new Tuple<>(Component.translatable("title.oritech.machine.addon_range", areaSize), Component.translatable("tooltip.oritech.laser_arm.addon_range")),
          new Tuple<>(Component.translatable("title.oritech.machine.addon_fortune", yieldAddons), Component.translatable("tooltip.oritech.machine.addon_fortune")));
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
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.maxInsert;
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.LASER_SCREEN;
    }
    
    @Override
    public Property<Direction> getBlockFacingProperty() {
        return BlockStateProperties.FACING;
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        this.sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
        
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }
    
    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.amount / (float) energyStorage.capacity) * 15);
    }
    
    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.heldStacks.size() <= slot) return 0;
        
        var stack = inventory.getItem(slot);
        if (stack.isEmpty()) return 0;
        
        return (int) ((stack.getCount() / (float) stack.getMaxStackSize()) * 15);
    }
    
    @Override
    public int getComparatorProgress() {
        if (currentTarget == null || currentTarget.equals(BlockPos.ZERO)) return 0;
        
        return (int) (currentTarget.distSqr(worldPosition) / range) * 15;
    }
    
    @Override
    public int getComparatorActiveState() {
        var idleTicks = level.getGameTime() - lastFiredAt;
        return idleTicks > 3 ? 15 : 0;
    }
    
    @Override
    public boolean hasRedstoneControlAvailable() {
        return true;
    }
    
    @Override
    public int receivedRedstoneSignal() {
        if (redstonePowered) return 15;
        return level.getBestNeighborSignal(worldPosition);
    }
    
    @Override
    public String currentRedstoneEffect() {
        if (redstonePowered) return "tooltip.oritech.redstone_disabled";
        return "tooltip.oritech.redstone_enabled_direct";
    }
    
    public BlockPos getLaserHeadPosition() {
        var state = getBlockState();
        var facing = state.getValue(BlockStateProperties.FACING);
        var offset = new Vec3i(-1, 0, 0);
        return new BlockPos(Geometry.offsetToWorldPosition(facing, offset, worldPosition));
    }
    
    @Override
    public void onRedstoneEvent(boolean isPowered) {
        this.redstonePowered = isPowered;
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
    
    public enum HunterTargetMode {
        HOSTILE_ONLY(1, "message.oritech.target_designator.hunter_hostile"),
        HOSTILE_NEUTRAL(2, "message.oritech.target_designator.hunter_neutral"),
        ALL(3, "message.oritech.target_designator.hunter_all");
        
        public final int value;
        public final String message;
        
        HunterTargetMode(int value, String message) {
            this.value = value;
            this.message = message;
        }
        
        private static final Map<Integer, HunterTargetMode> map = new HashMap<Integer, HunterTargetMode>();
        
        static {
            for (HunterTargetMode targetMode : HunterTargetMode.values())
                map.put(targetMode.value, targetMode);
        }
        
        public static HunterTargetMode fromValue(int i) {
            return map.getOrDefault(i, HOSTILE_ONLY);
        }
        
        public HunterTargetMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
    
    public static WorldPacketCodec<RegistryFriendlyByteBuf, LivingEntity> LASER_TARGET_PACKET_CODEC = new WorldPacketCodec<>() {
        @Override
        public LivingEntity decode(RegistryFriendlyByteBuf buf, @Nullable Level world) {
            
            var id = buf.readInt();
            if (world != null && id >= 0) {
                var candidate = world.getEntity(id);
                if (candidate instanceof LivingEntity livingEntity) {
                    return livingEntity;
                }
            }
            
            return null;
        }
        
        @Override
        public void encode(RegistryFriendlyByteBuf buf, LivingEntity value, @Nullable Level world) {
            var id = value != null ? value.getId() : -1;
            buf.writeInt(id);
        }
    };
}
