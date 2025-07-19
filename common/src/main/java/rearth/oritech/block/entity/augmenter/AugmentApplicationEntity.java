package rearth.oritech.block.entity.augmenter;

import dev.architectury.registry.menu.ExtendedMenuProvider;
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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.augmenter.AugmentResearchStationBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.client.ui.PlayerModifierScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import rearth.oritech.init.recipes.AugmentDataRecipe;

import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class AugmentApplicationEntity extends NetworkedBlockEntity implements MultiblockMachineController, GeoBlockEntity,
                                                                       ExtendedMenuProvider, ItemApi.BlockProvider, EnergyApi.BlockProvider, ScreenProvider {
    
    // config
    public static long maxEnergyTransfer = Oritech.CONFIG.augmenterMaxEnergy() / 10;
    public static long maxEnergyStored = Oritech.CONFIG.augmenterMaxEnergy();
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final Set<Identifier> researchedAugments = new HashSet<>();
    // working state
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final HashMap<Integer, ResearchState> availableStations = new HashMap<>();
    
    public boolean screenInvOverride = false;
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(5, this::markDirty);
    
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(maxEnergyTransfer, maxEnergyStored, maxEnergyStored, this::markDirty);
    
    
    public AugmentApplicationEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PLAYER_MODIFIER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
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
        
        var recipe = (AugmentDataRecipe) world.getRecipeManager().get(augment).get().value();
        
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
        
        var recipe = (AugmentDataRecipe) world.getRecipeManager().get(augment).get().value();
        
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
        this.markDirty();
        
        player.getWorld().playSound(null, player.getBlockPos(), SoundContent.SHORT_SERVO, SoundCategory.BLOCKS);
    }
    
    public void removeAugmentFromPlayer(Identifier augment, PlayerEntity player) {
        
        if (!PlayerAugments.allAugments.containsKey(augment)) {
            Oritech.LOGGER.error("Player augment with id" + augment + " not found. This should never happen");
            return;
        }
        
        var augmentInstance = PlayerAugments.allAugments.get(augment);
        augmentInstance.removeFromPlayer(player);
        this.markDirty();
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
                continue;
            }
            
            if (availableStations.containsKey(i) && availableStations.get(i) != null && availableStations.get(i).type.equals(candidateState.getBlock()))
                continue;
            
            var newState = new ResearchState(candidateState.getBlock(), false, Identifier.of(""), -1, -1);
            
            availableStations.put(i, newState);
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
    public void triggerSetupAnimation() {
        triggerAnim("machine", "setup");
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 0, state -> {
            
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
        }).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()).triggerableAnim("setup", MachineBlockEntity.SETUP));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.empty();
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        this.sendUpdate(SyncType.GUI_OPEN);
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
        return ModScreens.AUGMENTER_INV_SCREEN;
    }
    
    public static class ResearchState {
        
        public Block type;
        public boolean working;
        public Identifier selectedResearch;
        public int workTime;
        public long researchStartedAt;
        
        public static PacketCodec<RegistryByteBuf, ResearchState> PACKET_CODEC = PacketCodec.tuple(
          Identifier.PACKET_CODEC.xmap(Registries.BLOCK::get, Registries.BLOCK::getId), ResearchState::getType,
          PacketCodecs.BOOL, ResearchState::getWorking,
          Identifier.PACKET_CODEC, ResearchState::getSelectedResearch,
          PacketCodecs.INTEGER, ResearchState::getWorkTime,
          PacketCodecs.VAR_LONG, ResearchState::getResearchStartedAt,
          ResearchState::new
        );
        
        public Block getType() {
            return type;
        }
        
        public int getWorkTime() {
            return workTime;
        }
        
        public Identifier getSelectedResearch() {
            return selectedResearch;
        }
        
        public long getResearchStartedAt() {
            return researchStartedAt;
        }
        
        public boolean getWorking() {
            return working;
        }
        
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
