package rearth.oritech.block.entity.augmenter;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.augmenter.AugmentResearchStationBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.client.ui.PlayerModifierScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.AugmentRecipe;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.SimpleEnergyStorage;
import rearth.oritech.util.item.ItemApi;
import rearth.oritech.util.item.containers.SimpleInventoryStorage;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class AugmentApplicationEntity extends BlockEntity implements BlockEntityTicker<AugmentApplicationEntity>, MultiblockMachineController, GeoBlockEntity,
                                                                       ExtendedScreenHandlerFactory, ItemApi.BlockProvider, EnergyApi.BlockProvider, ScreenProvider {
    
    public final Set<Identifier> researchedAugments = new HashSet<>();
    
    // config
    public static long maxEnergyTransfer = Oritech.CONFIG.augmenterMaxEnergy() / 10;
    public static long maxEnergyStored = Oritech.CONFIG.augmenterMaxEnergy();
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    // working state
    private boolean networkDirty = true;
    public final HashMap<Integer, ResearchState> availableStations = new HashMap<>();
    public boolean screenInvOverride = false;
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(5, this::markDirty);
    
    private final EnergyApi.EnergyStorage energyStorage = new SimpleEnergyStorage(maxEnergyTransfer, maxEnergyStored, maxEnergyStored, this::markDirty);
    private AnimationController<AugmentApplicationEntity> animationController;
    
    
    public AugmentApplicationEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, AugmentApplicationEntity blockEntity) {
        
        if (world.isClient) return;
        
        screenInvOverride = false;
        
        // update research stations
        for (int i = 0; i < 3; i++) {
            var station = availableStations.getOrDefault(i, null);
            if (station == null) continue;
            if (station.working) {
                var isDone = world.getTime() > station.researchStartedAt + station.workTime;
                if (!isDone) continue;
                
                researchedAugments.add(station.selectedResearch);
                station.working = false;
                this.markDirty();
            }
        }
        
        if (networkDirty) {
            updateNetwork();
        }
    }
    
    // persist researched augments, inventory, energy
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("rf", energyStorage.getAmount());
        addMultiblockToNbt(nbt);
        
        
        var list = new NbtList();
        for (var augment : researchedAugments) {
            list.add(NbtString.of(augment.getPath()));
        }
        
        // also put in pending researches to avoid having to separately store them
        for (var station : availableStations.values()) {
            if (station == null) continue;
            if (station.working) {
                list.add(NbtString.of(station.selectedResearch.getPath()));
            }
        }
        
        nbt.put("researched", list);
        
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        energyStorage.setAmount(nbt.getLong("rf"));
        loadMultiblockNbtData(nbt);
        
        var parsedList = nbt.getList("researched", NbtElement.STRING_TYPE);
        for (var element : parsedList) {
            var id = Oritech.id(element.asString());
            researchedAugments.add(id);
        }
        
    }
    
    public void researchAugment(Identifier augment, boolean creative, PlayerEntity player) {
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        if (researchedAugments.contains(augment)) {
            Oritech.LOGGER.warn("Player tried to research already researched augment " + augment);
            return;
        }
        
        var recipe = (AugmentRecipe) world.getRecipeManager().get(augment).get().value();
        
        var extracted = energyStorage.extract(recipe.getRfCost(), false);
        
        // remove available resources
        for (var wantedInput : recipe.getResearchCost()) {
            var type = wantedInput.ingredient();
            var missingCount = wantedInput.count();
            
            for (var stack : this.inventory.heldStacks) {
                if (type.test(stack)) {
                    var takeAmount = Math.min(stack.getCount(), missingCount);
                    missingCount -= takeAmount;
                    stack.decrement(takeAmount);
                    
                    if (missingCount <= 0) break;
                }
            }
            for (var stack : player.getInventory().main) {
                if (type.test(stack)) {
                    var takeAmount = Math.min(stack.getCount(), missingCount);
                    missingCount -= takeAmount;
                    stack.decrement(takeAmount);
                    
                    if (missingCount <= 0) break;
                }
            }
        }
        
        // assign first idle station
        for (int i = 0; i < 3; i++) {
            var station = availableStations.getOrDefault(i, null);
            if (station == null) continue;
            if (station.working) continue;
            
            
            if (!Registries.BLOCK.getId(station.type).equals(recipe.getRequiredStation())) continue;
            
            station.selectedResearch = augment;
            station.working = true;
            station.researchStartedAt = world.getTime();
            station.workTime = creative ? 5 : recipe.getTime();
            
            break;
            
        }
        this.markDirty();
    }
    
    public void installAugmentToPlayer(Identifier augment, PlayerEntity player) {
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        if (!researchedAugments.contains(augment)) {
            Oritech.LOGGER.warn("Player tried to install augment with id" + augment + " without researching it.");
            return;
        }
        
        var recipe = (AugmentRecipe) world.getRecipeManager().get(augment).get().value();
        
        // remove available resources
        for (var wantedInput : recipe.getApplyCost()) {
            var type = wantedInput.ingredient();
            var missingCount = wantedInput.count();
            
            for (var stack : this.inventory.heldStacks) {
                if (type.test(stack)) {
                    var takeAmount = Math.min(stack.getCount(), missingCount);
                    missingCount -= takeAmount;
                    stack.decrement(takeAmount);
                    
                    if (missingCount <= 0) break;
                }
            }
            
            for (var stack : player.getInventory().main) {
                if (type.test(stack)) {
                    var takeAmount = Math.min(stack.getCount(), missingCount);
                    missingCount -= takeAmount;
                    stack.decrement(takeAmount);
                    
                    if (missingCount <= 0) break;
                }
            }
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        augmentInstance.installToPlayer(player);
        this.markNetDirty();
    }
    
    public void removeAugmentFromPlayer(Identifier augment, PlayerEntity player) {
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        augmentInstance.removeFromPlayer(player);
        this.markNetDirty();
    }
    
    public static void toggleAugmentForPlayer(Identifier augment, PlayerEntity player) {
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        
        if (!augmentInstance.isInstalled(player)) {
            Oritech.LOGGER.error("Tried toggling not-installed augment id: " + augment + ". This should never happen");
            return;
        }
        
        augmentInstance.toggle(player);
    }
    
    public boolean hasPlayerAugment(Identifier augment, PlayerEntity player) {
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return false;
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        return augmentInstance.isInstalled(player);
        
    }
    
    public void loadResearchesFromPlayer(PlayerEntity player) {
        
        for (var augmentId : PlayerAugments.allAugments.keySet()) {
            var augment = PlayerAugments.allAugments.get(augmentId);
            var isInstalled = augment.isInstalled(player);
            var isResearched = researchedAugments.contains(augmentId);
            
            if (isInstalled && !isResearched) {
                researchedAugments.add(augmentId);
            }
        }
    }
    
    public void loadAvailableStations(PlayerEntity player) {
        var facing = this.getCachedState().get(Properties.HORIZONTAL_FACING);
        
        var targetPositions = List.of(
          new BlockPos(0, 0, -2),
          new BlockPos(1, 0, 2),
          new BlockPos(2, 0, -1)
        );
        
        for (int i = 0; i < targetPositions.size(); i++) {
            var candidatePosOffset = targetPositions.get(i);
            var candidatePos = new BlockPos(Geometry.offsetToWorldPosition(facing, candidatePosOffset, pos));
            
            var candidateState = world.getBlockState(candidatePos);
            if (!(candidateState.getBlock() instanceof AugmentResearchStationBlock) || !candidateState.get(MultiblockMachine.ASSEMBLED)) {
                availableStations.put(i, null);
                continue;
            }
            
            if (availableStations.containsKey(i) && availableStations.get(i) != null && availableStations.get(i).type.equals(candidateState.getBlock()))
                continue;
            
            var newState = new ResearchState(candidateState.getBlock(), false, Identifier.of(""), -1, -1);
            
            availableStations.put(i, newState);
        }
        
    }
    
    private void markNetDirty() {
        this.networkDirty = true;
    }
    
    private void updateNetwork() {
        this.networkDirty = false;
        
        var stations = new ArrayList<Identifier>();
        var states = new ArrayList<Boolean>();
        var targets = new ArrayList<Identifier>();
        var startTimes = new ArrayList<Long>();
        var durations = new ArrayList<Integer>();
        
        availableStations.values().forEach(station -> {
            if (station == null) return;
            stations.add(Registries.BLOCK.getId(station.type));
            states.add(station.working);
            targets.add(station.selectedResearch);
            startTimes.add(station.researchStartedAt);
            durations.add(station.workTime);
        });
        
        // collect researched augments, send them to client
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AugmentDataPacket(pos, researchedAugments.stream().toList(), stations, states, targets, startTimes, durations));
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GenericEnergySyncPacket(pos, energyStorage.getAmount(), energyStorage.getCapacity()));
    }
    
    public void handleAugmentUpdatePacket(NetworkContent.AugmentDataPacket packet) {
        this.researchedAugments.clear();
        this.researchedAugments.addAll(packet.allResearched());
        
        this.availableStations.clear();
        
        for (int i = 0; i < packet.researchBlocks().size(); i++) {
            var station = Registries.BLOCK.get(packet.researchBlocks().get(i));
            var state = packet.researchStates().get(i);
            var target = packet.activeResearches().get(i);
            var researchTime = packet.researchTimes().get(i);
            var startedTime = packet.startedTimes().get(i);
            
            var res = new ResearchState(station, state, target, researchTime, startedTime);
            availableStations.put(i, res);
        }
        
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0, 1),
          new Vec3i(0, 0, -1),
          new Vec3i(-1, 0, 0),
          new Vec3i(-1, 0, 1),
          new Vec3i(-1, 0, -1),
          new Vec3i(0, 1, 1),
          new Vec3i(0, 1, -1),
          new Vec3i(-1, 1, 0),
          new Vec3i(-1, 1, 1),
          new Vec3i(-1, 1, -1)
        );
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        var state = getCachedState();
        return state.get(Properties.HORIZONTAL_FACING).getOpposite();
    }
    
    @Override
    public BlockPos getPosForMultiblock() {
        return pos;
    }
    
    @Override
    public World getWorldForMultiblock() {
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
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return inventory;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        this.markNetDirty();
    }
    
    @Override
    public void playSetupAnimation() {
        animationController.setAnimation(MachineBlockEntity.SETUP);
        animationController.forceAnimationReset();
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        animationController = getController();
        controllers.add(animationController.setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    private AnimationController<AugmentApplicationEntity> getController() {
        return new AnimationController<>(this, "machine", 0, state -> {
            
            if (state.isCurrentAnimation(MachineBlockEntity.SETUP)) {
                if (state.getController().hasAnimationFinished()) {
                    return state.setAndContinue(MachineBlockEntity.IDLE);
                } else {
                    return state.setAndContinue(MachineBlockEntity.SETUP);
                }
            }
            
            if (this.getCachedState().get(MultiblockMachine.ASSEMBLED)) {
                return state.setAndContinue(MachineBlockEntity.IDLE);
            } else {
                return state.setAndContinue(MachineBlockEntity.PACKAGED);
            }
        });
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new ModScreens.BasicData(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.empty();
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        updateNetwork();
        
        var dist = player.squaredDistanceTo(this.pos.toBottomCenterPos());
        if (dist > 1 || screenInvOverride)
            return new BasicMachineScreenHandler(syncId, playerInventory, this);
        
        return new PlayerModifierScreenHandler(syncId, playerInventory, this);
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
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 30, 30),
          new GuiSlot(1, 50, 30),
          new GuiSlot(2, 70, 30),
          new GuiSlot(3, 90, 30),
          new GuiSlot(4, 110, 30)
        );
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
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
    public boolean showExpansionPanel() {
        return false;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.MODIFIED_INV_SCREEN;
    }
    
    public static class ResearchState {
        
        public Block type;
        public boolean working;
        public Identifier selectedResearch;
        public int workTime;
        public long researchStartedAt;
        
        public ResearchState(Block type, boolean working, Identifier selectedResearch, int workTime, long researchStartedAt) {
            this.type = type;
            this.working = working;
            this.selectedResearch = selectedResearch;
            this.workTime = workTime;
            this.researchStartedAt = researchStartedAt;
        }
        
        @Override
        public String toString() {
            return "ResearchState{" +
                     "type=" + type +
                     ", working=" + working +
                     ", selectedResearch=" + selectedResearch +
                     ", workTime=" + workTime +
                     ", researchStartedAt=" + researchStartedAt +
                     '}';
        }
    }
    
}
