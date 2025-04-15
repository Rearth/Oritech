package rearth.oritech.block.entity.interaction;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
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
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.DynamicEnergyStorage;
import rearth.oritech.util.fluid.FluidApi;
import rearth.oritech.util.fluid.containers.SimpleFluidStorage;
import rearth.oritech.util.item.ItemApi;
import rearth.oritech.util.item.containers.SimpleInventoryStorage;
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
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;

public class DronePortEntity extends BlockEntity implements ItemApi.BlockProvider, FluidApi.BlockProvider, EnergyApi.BlockProvider, GeoBlockEntity, BlockEntityTicker<DronePortEntity>, MultiblockMachineController, MachineAddonController, ExtendedScreenHandlerFactory, ScreenProvider, RedstoneAddonBlockEntity.RedstoneControllable {
    
    // addon data
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openSlots = new ArrayList<>();
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(1024 * 32, 10000, 0, this::markDirty);
    
    public final DronePortItemInventory inventory = new DronePortItemInventory(15, this::markDirty);
    
    public final DronePortFluidStorage fluidStorage = new DronePortFluidStorage(128 * FluidStackHooks.bucketAmount(), this::markDirty);
    
    // not persisted, only to assign targets
    protected final SimpleInventory cardInventory = new SimpleInventory(2) {
        @Override
        public void markDirty() {
            DronePortEntity.this.markDirty();
        }
        
        @Override
        public boolean canInsert(ItemStack stack) {
            return stack.getItem() instanceof LaserTargetDesignator;
        }
    };
    
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<DronePortEntity> animationController = getAnimationController();
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    // fluid
    public boolean hasFluidAddon;
    
    // redstone
    public boolean disabledViaRedstone;
    
    // work data
    private BlockPos targetPosition;
    private long lastSentAt;
    private DroneTransferData incomingPacket;
    private DroneAnimState animState = DroneAnimState.IDLE;
    private boolean networkDirty;
    
    // config
    private final long baseEnergyUsage = 1024;
    private final int takeOffTime = 300;
    private final int landTime = 260;
    private final int totalFlightTime = takeOffTime + landTime;
    
    // client only
    private String statusMessage;
    
    public DronePortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DRONE_PORT_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, DronePortEntity blockEntity) {
        
        if (world.isClient) return;
        
        checkPositionCard();
        
        if (incomingPacket != null)
            checkIncomingAnimation();
        
        if (world.getTime() % 20 == 0) {
            if (incomingPacket != null) {
                tryReceivePacket();
            } else if (canSend()) {
                sendDrone();
            }
        }
        
        if (networkDirty && world.getTime() % 10 == 0) {
            networkDirty = false;
            sendNetworkUpdate();
        }
    }
    
    private void checkPositionCard() {
        
        var source = cardInventory.heldStacks.get(0);
        if (source.getItem() instanceof LaserTargetDesignator && source.contains(ComponentContent.TARGET_POSITION.get())) {
            var target = source.get(ComponentContent.TARGET_POSITION.get());
            setTargetFromDesignator(target);
        } else {
            return;
        }
        
        cardInventory.heldStacks.set(1, source);
        cardInventory.heldStacks.set(0, ItemStack.EMPTY);
        cardInventory.markDirty();
        this.markDirty();
        
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        addMultiblockToNbt(nbt);
        writeAddonToNbt(nbt);
        fluidStorage.writeNbt(nbt, "");
        nbt.putBoolean("has_fluid_addon", hasFluidAddon);
        nbt.putBoolean("disabled_via_redstone", disabledViaRedstone);
        nbt.putLong("energy_stored", energyStorage.amount);
        
        if (targetPosition != null) {
            nbt.putLong("target_position", targetPosition.asLong());
        }
        
        if (incomingPacket != null) {
            var compound = new NbtCompound();
            DefaultedList<ItemStack> list = DefaultedList.ofSize(incomingPacket.transferredStacks.size());
            list.addAll(incomingPacket.transferredStacks);
            Inventories.writeNbt(compound, list, false, registryLookup);
            nbt.put("incoming", compound);
            FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, incomingPacket.movedFluid).result().ifPresent(tag -> nbt.put("fluidmoving", tag));
            nbt.putLong("incomingTime", incomingPacket.arrivesAt);
        } else {
            nbt.remove("incoming");
        }
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        loadMultiblockNbtData(nbt);
        loadAddonNbtData(nbt);
        fluidStorage.readNbt(nbt, "");
        
        hasFluidAddon = nbt.getBoolean("has_fluid_addon");
        disabledViaRedstone = nbt.getBoolean("disabled_via_redstone");
        energyStorage.amount = nbt.getLong("energy_stored");
        targetPosition = BlockPos.fromLong(nbt.getLong("target_position"));
        
        if (nbt.contains("incoming")) {
            DefaultedList<ItemStack> list = DefaultedList.ofSize(15);
            Inventories.readNbt(nbt.getCompound("incoming"), list, registryLookup);
            var fluid = FluidStack.CODEC.parse(NbtOps.INSTANCE, nbt.get("fluidmoving")).result().orElse(FluidStack.empty());
            var arrivalTime = nbt.getLong("incomingTime");
            incomingPacket = new DroneTransferData(list, fluid, arrivalTime);
        }
    }
    
    @Override
    public void initAddons() {
        MachineAddonController.super.initAddons();
        
        // Trigger block updates for pipes to connect
        world.updateNeighbors(pos, getCachedState().getBlock());
        for (Vec3i corePosition : getCorePositions()) {
            var worldPos = new BlockPos(Geometry.offsetToWorldPosition(getFacingForMultiblock(), corePosition, getPosForAddon()));
            world.updateNeighbors(worldPos, world.getBlockState(worldPos).getBlock());
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
        if (world.getTime() == incomingPacket.arrivesAt - landTime) {
            triggerNetworkReceiveAnimation();
        }
    }
    
    private void tryReceivePacket() {
        var hasArrived = world.getTime() - incomingPacket.arrivesAt > 0;
        if (!hasArrived) return;
        
        Oritech.LOGGER.debug("receiving drone package: " + incomingPacket);
        
        long totalToInsert = incomingPacket.transferredStacks.stream().mapToLong(ItemStack::getCount).sum();
        long totalInserted = 0;
        for (var stack : incomingPacket.transferredStacks) {
            totalInserted += inventory.insert(stack, false);
        }
        
        if (totalInserted != totalToInsert) {
            Oritech.LOGGER.warn("Something weird has happened with drone port item storage. Caused at: " + pos);
            return;
        }
        
        if (!incomingPacket.movedFluid.isEmpty()) {
            fluidStorage.insertFromDrone(incomingPacket.movedFluid, false);
        }
        
        incomingPacket = null;
        markDirty();
    }
    
    private void sendDrone() {
        var targetPort = (DronePortEntity) world.getBlockEntity(targetPosition);
        var arriveTime = world.getTime() + takeOffTime + landTime;
        var data = new DroneTransferData(inventory.heldStacks.stream().filter(stack -> !stack.isEmpty()).toList(), fluidStorage.getStack(), arriveTime);
        targetPort.setIncomingPacket(data);
        
        inventory.clear();
        fluidStorage.setStack(FluidStack.empty());
        lastSentAt = world.getTime();
        energyStorage.amount -= calculateEnergyUsage();
        
        triggerNetworkSendAnimation();
        targetPort.markDirty();
        this.markDirty();
        
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
        var diff = world.getTime() - lastSentAt;
        return diff < takeOffTime;
    }
    
    private boolean canSend() {
        
        if (disabledViaRedstone || targetPosition == null || (inventory.isEmpty() && fluidStorage.getAmount() == 0) || energyStorage.amount < calculateEnergyUsage() || incomingPacket != null)
            return false;
        var targetEntity = world.getBlockEntity(targetPosition);
        if (!(targetEntity instanceof DronePortEntity targetPort) || targetPort.disabledViaRedstone || targetPort.getIncomingPacket() != null || !targetPort.canAcceptPayload(inventory.heldStacks, fluidStorage.getStack()))
            return false;
        
        
        return !isSendingDrone();
    }
    
    private long calculateEnergyUsage() {
        if (targetPosition == null) return baseEnergyUsage;
        var distance = pos.getManhattanDistance(targetPosition);
        return (long) Math.sqrt(distance) * 50 + baseEnergyUsage;
    }
    
    private void triggerNetworkSendAnimation() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.DroneSendEventPacket(pos, true, false));
    }
    
    private void triggerNetworkReceiveAnimation() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.DroneSendEventPacket(pos, false, true));
    }
    
    private void sendNetworkUpdate() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GenericEnergySyncPacket(pos, energyStorage.amount, energyStorage.capacity));
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.DronePortFluidSyncPacket(pos, hasFluidAddon, Registries.FLUID.getId(fluidStorage.getFluid()).toString(), fluidStorage.getAmount()));
    }
    
    private void sendNetworkStatusMessage(String statusMessage) {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.DroneCardEventPacket(pos, statusMessage));
    }
    
    public boolean setTargetFromDesignator(BlockPos targetPos) {
        
        // if target is coreblock, adjust it to point to controller if connected
        var targetState = Objects.requireNonNull(world).getBlockState(targetPos);
        if (targetState.getBlock() instanceof MachineCoreBlock && targetState.get(MachineCoreBlock.USED)) {
            var coreEntity = (MachineCoreEntity) world.getBlockEntity(targetPos);
            var controllerPos = Objects.requireNonNull(coreEntity).getControllerPos();
            if (controllerPos != null) targetPos = controllerPos;
        }
        
        var distance = targetPos.getManhattanDistance(pos);
        if (distance < 50) {
            sendNetworkStatusMessage("message.oritech.drone.invalid_distance");
            return false;
        }
        
        if (world.getBlockState(targetPos).getBlock() instanceof DronePortBlock) {
            // store position
            this.targetPosition = targetPos;
            sendNetworkStatusMessage("message.oritech.drone.target_set");
            return true;
        }
        
        sendNetworkStatusMessage("message.oritech.drone.target_invalid");
        return false;
        
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        this.networkDirty = true;
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
        return Objects.requireNonNull(world).getBlockState(getPos()).get(Properties.HORIZONTAL_FACING).getOpposite();
    }
    
    @Override
    public BlockPos getPosForAddon() {
        return pos;
    }
    
    @Override
    public World getWorldForAddon() {
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
        return Objects.requireNonNull(world).getBlockState(getPos()).get(Properties.HORIZONTAL_FACING);
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
        this.markDirty();
    }
    
    public DroneTransferData getIncomingPacket() {
        return incomingPacket;
    }
    
    public void setIncomingPacket(DroneTransferData incomingPacket) {
        this.incomingPacket = incomingPacket;
    }
    
    public boolean isActive(BlockState state) {
        return state.get(ASSEMBLED);
    }
    
    @Override
    public void playSetupAnimation() {
        animationController.setAnimation(SETUP);
        animationController.forceAnimationReset();
    }
    
    public void playSendAnimation() {
        animState = DroneAnimState.TAKEOFF;
        animationController.forceAnimationReset();
    }
    
    public void playReceiveAnimation() {
        animState = DroneAnimState.LANDING;
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
    
    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.amount / (float) energyStorage.capacity) * 15);
    }
    
    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.heldStacks.size() <= slot)
            return hasFluidAddon ? ComparatorOutputProvider.getFluidStorageComparatorOutput(fluidStorage) : 0;
        
        var stack = inventory.getStack(slot);
        if (stack.isEmpty()) return
                               hasFluidAddon ? ComparatorOutputProvider.getFluidStorageComparatorOutput(fluidStorage) : 0;
        
        return hasFluidAddon ?
                 Math.max(ComparatorOutputProvider.getItemStackComparatorOutput(stack), ComparatorOutputProvider.getFluidStorageComparatorOutput(fluidStorage)) :
                 ComparatorOutputProvider.getItemStackComparatorOutput(stack);
    }
    
    @Override
    public int getComparatorProgress() {
        if (isSendingDrone()) {
            return (int) (((world.getTime() - lastSentAt) / (float) takeOffTime) * 15);
        } else if (incomingPacket != null) {
            return (int) ((totalFlightTime + (world.getTime() - incomingPacket.arrivesAt)) / (float) (totalFlightTime) * 15);
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
    
    private enum DroneAnimState {
        IDLE, TAKEOFF, LANDING
    }
    
    public static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("takeoff").thenPlay("idle");
    public static final RawAnimation LANDING = RawAnimation.begin().thenPlay("landing").thenPlay("idle");
    
    private AnimationController<DronePortEntity> getAnimationController() {
        return new AnimationController<>(this, state -> {
            
            if (state.isCurrentAnimation(SETUP)) {
                if (state.getController().hasAnimationFinished()) {
                    state.setAndContinue(IDLE);
                } else {
                    return state.setAndContinue(SETUP);
                }
            }
            
            if (isActive(getCachedState())) {
                switch (animState) {
                    case IDLE -> {
                        return state.setAndContinue(IDLE);
                    }
                    case TAKEOFF -> {
                        return state.setAndContinue(TAKEOFF);
                    }
                    case LANDING -> {
                        return state.setAndContinue(LANDING);
                    }
                    default -> {
                        return PlayState.CONTINUE;
                    }
                }
            } else {
                return state.setAndContinue(PACKAGED);
            }
        }).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>());
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        sendNetworkUpdate();
        return new ModScreens.UpgradableData(pos, getUiData(), coreQuality);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.FullEnergySyncPacket(pos, energyStorage.amount, energyStorage.capacity, energyStorage.maxInsert, energyStorage.maxExtract));
        return new DroneScreenHandler(syncId, playerInventory, this, getUiData(), coreQuality);
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
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
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
    
    public SimpleInventory getCardInventory() {
        return cardInventory;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    @Override
    public BlockPos getPosForMultiblock() {
        return pos;
    }
    
    @Override
    public World getWorldForMultiblock() {
        return world;
    }
    
    public record DroneTransferData(List<ItemStack> transferredStacks, FluidStack movedFluid, long arrivesAt) {
    }
    
    public class DronePortItemInventory extends SimpleInventoryStorage {
        
        public DronePortItemInventory(int size, Runnable onUpdate) {
            super(size, onUpdate);
        }
        
        @Override
        public int insertToSlot(ItemStack addedStack, int slot, boolean simulate) {
            if (DronePortEntity.this.incomingPacket != null) return 0;
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
