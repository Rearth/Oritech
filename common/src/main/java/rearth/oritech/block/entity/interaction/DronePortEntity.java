package rearth.oritech.block.entity.interaction;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.interaction.DronePortBlock;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.DroneScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.item.tools.LaserTargetDesignator;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;


public class DronePortEntity extends NetworkedBlockEntity
  implements ItemApi.BlockProvider, FluidApi.BlockProvider, EnergyApi.BlockProvider,
               GeoBlockEntity, MultiblockMachineController, MachineAddonController, ExtendedMenuProvider,
               ScreenProvider, RedstoneAddonBlockEntity.RedstoneControllable {
    
    // addon data
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> openSlots = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    // storage
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(1024 * 32, 10000, 0, this::setChanged);
    
    public final DronePortItemInventory inventory = new DronePortItemInventory(15, this::setChanged);
    
    @SyncField(SyncType.GUI_TICK)
    public final DronePortFluidStorage fluidStorage = new DronePortFluidStorage(128 * FluidStackHooks.bucketAmount(), this::setChanged);
    
    // not persisted, only to assign targets
    protected final SimpleContainer cardInventory = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            DronePortEntity.this.setChanged();
        }
        
        @Override
        public boolean canAddItem(ItemStack stack) {
            return stack.getItem() instanceof LaserTargetDesignator;
        }
    };
    
    @SyncField(SyncType.GUI_OPEN)
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    // fluid
    @SyncField(SyncType.GUI_OPEN)
    public boolean hasFluidAddon;
    
    // redstone
    @SyncField(SyncType.GUI_OPEN)
    public boolean disabledViaRedstone;
    
    // work data
    private BlockPos targetPosition;
    private long lastSentAt;
    private DroneTransferData incomingPacket;
    private boolean receivingPackage;
    
    // config
    private final long baseEnergyUsage = 1024;
    private final int takeOffTime = 300;
    private final int landTime = 260;
    private final int totalFlightTime = takeOffTime + landTime;
    
    // client only
    @SyncField(SyncType.GUI_TICK)
    private String statusMessage = "";
    
    public DronePortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DRONE_PORT_ENTITY, pos, state);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        checkPositionCard();
        
        if (incomingPacket != null)
            checkIncomingAnimation();
        
        if (world.getGameTime() % 20 == 0) {
            if (incomingPacket != null) {
                tryReceivePacket();
            } else if (canSend()) {
                sendDrone();
            }
        }
    }
    
    private void checkPositionCard() {
        
        var source = cardInventory.getItems().get(0);
        if (source.getItem() instanceof LaserTargetDesignator && source.has(ComponentContent.TARGET_POSITION.get())) {
            var target = source.get(ComponentContent.TARGET_POSITION.get());
            setTargetFromDesignator(target);
        } else {
            return;
        }
        
        cardInventory.getItems().set(1, source);
        cardInventory.getItems().set(0, ItemStack.EMPTY);
        cardInventory.setChanged();
        this.setChanged();
        
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.getHeldStacks(), false, registryLookup);
        addMultiblockToNbt(nbt);
        writeAddonToNbt(nbt);
        fluidStorage.writeNbt(nbt, "");
        nbt.putBoolean("has_fluid_addon", hasFluidAddon);
        nbt.putBoolean("disabled_via_redstone", disabledViaRedstone);
        nbt.putLong("energy_stored", energyStorage.amount);
        
        if (targetPosition != null) {
            nbt.putLong("target_position", targetPosition.asLong());
        }
        
        var cardCompound = new CompoundTag();
        ContainerHelper.saveAllItems(cardCompound, cardInventory.getItems(), false, registryLookup);
        nbt.put("cards", cardCompound);
        
        if (incomingPacket != null) {
            var compound = new CompoundTag();
            NonNullList<ItemStack> list = NonNullList.createWithCapacity(incomingPacket.transferredStacks.size());
            list.addAll(incomingPacket.transferredStacks);
            ContainerHelper.saveAllItems(compound, list, false, registryLookup);
            nbt.put("incoming", compound);
            FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, incomingPacket.movedFluid).result().ifPresent(tag -> nbt.put("fluidmoving", tag));
            nbt.putLong("incomingTime", incomingPacket.arrivesAt);
        } else {
            nbt.remove("incoming");
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.getHeldStacks(), registryLookup);
        loadMultiblockNbtData(nbt);
        loadAddonNbtData(nbt);
        fluidStorage.readNbt(nbt, "");
        
        hasFluidAddon = nbt.getBoolean("has_fluid_addon");
        disabledViaRedstone = nbt.getBoolean("disabled_via_redstone");
        energyStorage.amount = nbt.getLong("energy_stored");
        targetPosition = BlockPos.of(nbt.getLong("target_position"));
        
        ContainerHelper.loadAllItems(nbt.getCompound("cards"), cardInventory.getItems(), registryLookup);
        
        if (nbt.contains("incoming")) {
            NonNullList<ItemStack> list = NonNullList.withSize(15, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("incoming"), list, registryLookup);
            var fluid = FluidStack.CODEC.parse(NbtOps.INSTANCE, nbt.get("fluidmoving")).result().orElse(FluidStack.empty());
            var arrivalTime = nbt.getLong("incomingTime");
            incomingPacket = new DroneTransferData(list, fluid, arrivalTime);
        }
    }
    
    @Override
    public void initAddons() {
        MachineAddonController.super.initAddons();
        
        // Trigger block updates for pipes to connect
        level.blockUpdated(worldPosition, getBlockState().getBlock());
        for (Vec3i corePosition : getCorePositions()) {
            var worldPos = new BlockPos(Geometry.offsetToWorldPosition(getFacingForMultiblock(), corePosition, getPosForAddon()));
            level.blockUpdated(worldPos, level.getBlockState(worldPos).getBlock());
        }
    }
    
    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_FLUID_ADDON)) {
            hasFluidAddon = true;
        }
    }
    
    @Override
    public void resetAddons() {
        MachineAddonController.super.resetAddons();
        hasFluidAddon = false;
    }
    
    private void checkIncomingAnimation() {
        if (level.getGameTime() == incomingPacket.arrivesAt - landTime) {
            triggerNetworkReceiveAnimation();
        }
    }
    
    private void tryReceivePacket() {
        var hasArrived = level.getGameTime() - incomingPacket.arrivesAt > 0;
        if (!hasArrived) return;
        
        Oritech.LOGGER.debug("receiving drone package: " + incomingPacket);
        
        receivingPackage = true;
        long totalToInsert = incomingPacket.transferredStacks.stream().mapToLong(ItemStack::getCount).sum();
        long totalInserted = 0;
        for (var stack : incomingPacket.transferredStacks) {
            totalInserted += inventory.insert(stack, false);
        }
        
        if (totalInserted != totalToInsert) {
            Oritech.LOGGER.warn("Something weird has happened with drone port item storage. Caused at: " + worldPosition);
            return;
        }
        
        if (!incomingPacket.movedFluid.isEmpty()) {
            fluidStorage.insertFromDrone(incomingPacket.movedFluid, false);
        }
        
        receivingPackage = false;
        incomingPacket = null;
        setChanged();
    }
    
    private void sendDrone() {
        var targetPort = (DronePortEntity) level.getBlockEntity(targetPosition);
        var arriveTime = level.getGameTime() + takeOffTime + landTime;
        var data = new DroneTransferData(inventory.getHeldStacks().stream().filter(stack -> !stack.isEmpty()).toList(), fluidStorage.getStack(), arriveTime);
        targetPort.setIncomingPacket(data);
        
        inventory.clearContent();
        fluidStorage.setStack(FluidStack.empty());
        lastSentAt = level.getGameTime();
        energyStorage.amount -= calculateEnergyUsage();
        
        triggerNetworkSendAnimation();
        targetPort.setChanged();
        this.setChanged();
        
        Oritech.LOGGER.debug("sending drone package: " + data);
    }
    
    public boolean canAcceptPayload(List<ItemStack> stacks, FluidStack fluid) {
        
        // fail if items are incoming and inventory is not empty
        if (!stacks.isEmpty() && !inventory.isEmpty())
            return false;
        
        // fail if fluid is incoming and would not match
        if (!fluid.isEmpty() && (!hasFluidAddon || fluidStorage.insert(fluid, true) != fluid.getAmount())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if the drone is currently sending a package
     * Drone will be in a sending state for a certain amount of time after sending a package
     * (time it takes to take off)
     *
     * @return true if drone is sending a package
     */
    public boolean isSendingDrone() {
        var diff = level.getGameTime() - lastSentAt;
        return diff < takeOffTime;
    }
    
    private boolean canSend() {
        
        if (disabledViaRedstone || targetPosition == null || (inventory.isEmpty() && fluidStorage.getAmount() == 0) || energyStorage.amount < calculateEnergyUsage() || incomingPacket != null)
            return false;
        var targetEntity = level.getBlockEntity(targetPosition);
        if (!(targetEntity instanceof DronePortEntity targetPort) || targetPort.disabledViaRedstone || targetPort.getIncomingPacket() != null || !targetPort.canAcceptPayload(inventory.getHeldStacks(), fluidStorage.getStack()))
            return false;
        
        
        return !isSendingDrone();
    }
    
    private long calculateEnergyUsage() {
        if (targetPosition == null) return baseEnergyUsage;
        var distance = worldPosition.distManhattan(targetPosition);
        return (long) Math.sqrt(distance) * 50 + baseEnergyUsage;
    }
    
    private void triggerNetworkSendAnimation() {
        triggerAnim("machine", "takeoff");
    }
    
    private void triggerNetworkReceiveAnimation() {
        triggerAnim("machine", "landing");
    }
    
    public boolean setTargetFromDesignator(BlockPos targetPos) {
        
        // if target is coreblock, adjust it to point to controller if connected
        var targetState = Objects.requireNonNull(level).getBlockState(targetPos);
        if (targetState.getBlock() instanceof MachineCoreBlock && targetState.getValue(MachineCoreBlock.USED)) {
            var coreEntity = (MachineCoreEntity) level.getBlockEntity(targetPos);
            var controllerPos = Objects.requireNonNull(coreEntity).getControllerPos();
            if (controllerPos != null) targetPos = controllerPos;
        }
        
        var distance = targetPos.distManhattan(worldPosition);
        if (distance < 50) {
            statusMessage = "message.oritech.drone.invalid_distance";
            return false;
        }
        
        if (level.getBlockState(targetPos).getBlock() instanceof DronePortBlock) {
            // store position
            this.targetPosition = targetPos;
            statusMessage = "message.oritech.drone.target_set";
            return true;
        }
        
        statusMessage = "message.oritech.drone.target_invalid";
        return false;
        
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
          new Vec3i(0, 1, 0),
          new Vec3i(0, 1, 1),
          new Vec3i(-1, 1, -1)
        );
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        return Objects.requireNonNull(level).getBlockState(getBlockPos()).getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }
    
    @Override
    public BlockPos getPosForAddon() {
        return worldPosition;
    }
    
    @Override
    public Level getWorldForAddon() {
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
        return energyStorage;
    }
    
    @Override
    public @Nullable FluidApi.FluidStorage getFluidStorage(Direction direction) {
        return hasFluidAddon ? fluidStorage : null;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(3, 0, -1),
          new Vec3i(2, 0, -2)
        );
    }
    
    @Override
    public long getDefaultCapacity() {
        return 1024 * 32;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 512;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryForAddon() {
        return inventory;
    }
    
    @Override
    public ScreenProvider getScreenProvider() {
        return this;
    }
    
    public DynamicEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
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
        return Objects.requireNonNull(level).getBlockState(getBlockPos()).getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    @Override
    public DynamicEnergyStorage getStorageForAddon() {
        return getEnergyStorage();
    }
    
    @Override
    public BaseAddonData getBaseAddonData() {
        return addonData;
    }
    
    @Override
    public void setBaseAddonData(BaseAddonData data) {
        this.addonData = data;
        this.setChanged();
    }
    
    public DroneTransferData getIncomingPacket() {
        return incomingPacket;
    }
    
    public void setIncomingPacket(DroneTransferData incomingPacket) {
        this.incomingPacket = incomingPacket;
    }
    
    public boolean isActive(BlockState state) {
        return state.getValue(ASSEMBLED);
    }
    
    @Override
    public void triggerSetupAnimation() {
        triggerAnim("machine", "deploy");
    }
    
    public static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("takeoff");
    public static final RawAnimation LANDING = RawAnimation.begin().thenPlay("landing");
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 0, state -> {
            if (state.getController().getAnimationState().equals(AnimationController.State.STOPPED)) {
                var targetAnim = isActive(getBlockState()) ? MachineBlockEntity.IDLE : MachineBlockEntity.PACKAGED;
                state.resetCurrentAnimation();
                return state.setAndContinue(targetAnim);
            } else {
                // playing animation, keep going
                return PlayState.CONTINUE;
            }
        })
                          .triggerableAnim("takeoff", TAKEOFF)
                          .triggerableAnim("landing", LANDING)
                          .triggerableAnim("deploy", MachineBlockEntity.SETUP)
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.amount / (float) energyStorage.capacity) * 15);
    }
    
    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.getHeldStacks().size() <= slot)
            return hasFluidAddon ? ComparatorOutputProvider.getFluidStorageComparatorOutput(fluidStorage) : 0;
        
        var stack = inventory.getItem(slot);
        if (stack.isEmpty()) return
                               hasFluidAddon ? ComparatorOutputProvider.getFluidStorageComparatorOutput(fluidStorage) : 0;
        
        return hasFluidAddon ?
                 Math.max(ComparatorOutputProvider.getItemStackComparatorOutput(stack), ComparatorOutputProvider.getFluidStorageComparatorOutput(fluidStorage)) :
                 ComparatorOutputProvider.getItemStackComparatorOutput(stack);
    }
    
    @Override
    public int getComparatorProgress() {
        if (isSendingDrone()) {
            return (int) (((level.getGameTime() - lastSentAt) / (float) takeOffTime) * 15);
        } else if (incomingPacket != null) {
            return (int) ((totalFlightTime + (level.getGameTime() - incomingPacket.arrivesAt)) / (float) (totalFlightTime) * 15);
        } else {
            return 0;
        }
    }
    
    @Override
    public int getComparatorActiveState() {
        return isSendingDrone() || incomingPacket != null ? 15 : 0;
    }
    
    @Override
    public void onRedstoneEvent(boolean isPowered) {
        this.disabledViaRedstone = isPowered;
    }
    
    @Override
    public int receivedRedstoneSignal() {
        if (disabledViaRedstone) return 15;
        return 0;
    }
    
    @Override
    public String currentRedstoneEffect() {
        if (disabledViaRedstone) return "tooltip.oritech.redstone_disabled";
        return "tooltip.oritech.redstone_enabled";
    }
    
    @Override
    public boolean hasRedstoneControlAvailable() {
        return true;
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new DroneScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        
        var startX = 30;
        var startY = 26;
        var distance = 18;
        
        var list = new ArrayList<GuiSlot>();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                var index = y * 5 + x;
                list.add(new GuiSlot(index, startX + x * distance, startY + y * distance));
            }
        }
        
        return list;
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return calculateEnergyUsage();
    }
    
    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.maxInsert;
    }
    
    @Override
    public float getProgress() {
        return 0;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.DRONE_SCREEN;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    public SimpleContainer getCardInventory() {
        return cardInventory;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    @Override
    public BlockPos getPosForMultiblock() {
        return worldPosition;
    }
    
    @Override
    public Level getWorldForMultiblock() {
        return level;
    }
    
    public record DroneTransferData(List<ItemStack> transferredStacks, FluidStack movedFluid, long arrivesAt) {
    }
    
    public class DronePortItemInventory extends SimpleInventoryStorage {
        
        public DronePortItemInventory(int size, Runnable onUpdate) {
            super(size, onUpdate);
        }
        
        @Override
        public int insertToSlot(ItemStack addedStack, int slot, boolean simulate) {
            if (DronePortEntity.this.incomingPacket != null && !receivingPackage) return 0;
            return super.insertToSlot(addedStack, slot, simulate);
        }
    }
    
    public class DronePortFluidStorage extends SimpleFluidStorage {
        
        public DronePortFluidStorage(Long capacity, Runnable onUpdate) {
            super(capacity, onUpdate);
        }
        
        @Override
        public long insert(FluidStack toInsert, boolean simulate) {
            if (DronePortEntity.this.incomingPacket != null) return 0;
            return super.insert(toInsert, simulate);
        }
        
        /**
         * Insert from drone, bypasses the incoming packet check
         */
        public long insertFromDrone(FluidStack toInsert, boolean simulate) {
            return super.insert(toInsert, simulate);
        }
    }
}
