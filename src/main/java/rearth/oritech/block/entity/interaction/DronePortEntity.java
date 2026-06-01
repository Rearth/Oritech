package rearth.oritech.block.entity.interaction;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.fluid.SimpleFluidStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.interaction.DronePortBlock;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.addons.CombiAddonEntity;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.DroneScreenHandler;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.item.tools.LaserTargetDesignator;
import rearth.oritech.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;


public class DronePortEntity extends NetworkedBlockEntity
        implements ItemProvider, FluidProvider, EnergyProvider,
        GeoBlockEntity, MultiblockMachineController, MachineAddonController, MenuProvider,
        ScreenProvider, RedstoneAddonBlockEntity.RedstoneControllable, ColorableMachine {

    // addon data
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> openSlots = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private BaseAddonData addonData = BaseAddonData.DEFAULT_ADDON_DATA;
    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorVariant currentColor = getDefaultColor();

    // storage
    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(1024 * 32, 10000, 0, 0, this::setChanged, false);

    public final DronePortItemInventory inventory = new DronePortItemInventory(15, this::setChanged);

    @SyncField(SyncType.GUI_TICK)
    public final DronePortFluidStorage fluidStorage = new DronePortFluidStorage(128 * 1000, this::setChanged);

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
        super(BlockEntitiesContent.DRONE_PORT_ENTITY.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        checkPositionCard();

        if (incomingPacket != null)
            checkIncomingAnimation();

        if (serverLevel.getGameTime() % 20 == 0) {
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        serializeMultiblock(output);
        serializeAddonData(output);
        serializeColor(output);
        fluidStorage.serialize(output);
        output.putBoolean("has_fluid_addon", hasFluidAddon);
        output.putBoolean("disabled_via_redstone", disabledViaRedstone);
        output.putLong("energy_stored", energyStorage.energy);

        if (targetPosition != null) {
            output.store("target_position", BlockPos.CODEC, targetPosition);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        deserializeMultiblock(input);
        deserializeAddonData(input);
        deserializeColor(input);
        fluidStorage.deserialize(input);

        hasFluidAddon = input.getBooleanOr("has_fluid_addon", false);
        disabledViaRedstone = input.getBooleanOr("disabled_via_redstone", false);
        energyStorage.energy = input.getLongOr("energy_stored", 0);
        targetPosition = input.read("target_position", BlockPos.CODEC).orElse(null);
        incomingPacket = null;
    }

    @Override
    public void initAddons() {
        MachineAddonController.super.initAddons();

        // Trigger block updates for pipes to connect
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        for (Vec3i corePosition : getCorePositions()) {
            var worldPos = new BlockPos(Geometry.offsetToWorldPosition(getFacingForMultiblock(), corePosition, getPosForAddon()));
            level.updateNeighborsAt(worldPos, level.getBlockState(worldPos).getBlock());
        }
    }

    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.MACHINE_FLUID_ADDON) || addonBlock.addonEntity() instanceof CombiAddonEntity combi && combi.hasFluid()) {
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

        try (var transaction = Transaction.openRoot()) {
            receivingPackage = true;
            for (var stack : incomingPacket.transferredStacks) {
                var inserted = inventory.insert(ItemResource.of(stack), stack.getCount(), transaction);
                if (inserted != stack.getCount()) {
                    Oritech.LOGGER.warn("Drone failed to insert item amount. This should never happen! At: {}", worldPosition.toShortString());
                    return;
                }
            }

            if (!incomingPacket.movedFluid.isEmpty()) {
                var inserted = fluidStorage.insertFromDrone(FluidResource.of(incomingPacket.movedFluid), incomingPacket.movedFluid.amount(), transaction);
                if (inserted != incomingPacket.movedFluid().amount()) {
                    Oritech.LOGGER.warn("Drone failed to insert correct fluid amount. This should never happen! At: {}", worldPosition.toShortString());
                }
            }

            receivingPackage = false;
            incomingPacket = null;
        }
    }

    private void sendDrone() {
        var targetPort = (DronePortEntity) level.getBlockEntity(targetPosition);
        var arriveTime = level.getGameTime() + takeOffTime + landTime;
        var data = new DroneTransferData(inventory.getStacks().stream().filter(stack -> !stack.isEmpty()).toList(), fluidStorage.getContent(), arriveTime);
        targetPort.setIncomingPacket(data);

        inventory.getStacks().clear();
        fluidStorage.set(0, FluidResource.EMPTY, 0);
        lastSentAt = level.getGameTime();
        energyStorage.energy -= calculateEnergyUsage();

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
        if (fluid.isEmpty()) return true;
        if (!hasFluidAddon) return false;

        try (var transaction = Transaction.openRoot()){
            return fluidStorage.insert(FluidResource.of(fluid), fluid.amount(), transaction) == fluid.getAmount();
        }
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

        if (disabledViaRedstone || targetPosition == null || (inventory.isEmpty() && fluidStorage.getAmount() == 0) || energyStorage.energy < calculateEnergyUsage() || incomingPacket != null)
            return false;
        var targetEntity = level.getBlockEntity(targetPosition);
        if (!(targetEntity instanceof DronePortEntity targetPort) || targetPort.disabledViaRedstone || targetPort.getIncomingPacket() != null || !targetPort.canAcceptPayload(inventory.getStacks(), fluidStorage.getContent()))
            return false;


        return !isSendingDrone();
    }

    private long calculateEnergyUsage() {
        if (targetPosition == null) return baseEnergyUsage;
        var distance = worldPosition.distManhattan(targetPosition);
        return (long) Math.sqrt(distance) * 50 + baseEnergyUsage;
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
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForMultiblock() {
        return inventory;
    }

    @Override
    public DynamicEnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return energyStorage;
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
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForAddon() {
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
        controllers.add(new AnimationController<>("machine", 0, state -> {
            if (state.controller().getPlayState().equals(PlayState.STOP)) {
                var targetAnim = isActive(getBlockState()) ? MachineBlockEntity.IDLE : MachineBlockEntity.PACKAGED;
                // state.resetCurrentAnimation();
                return state.setAndContinue(targetAnim);
            } else {
                // playing animation, keep going
                return PlayState.CONTINUE;
            }
        })
                .triggerableAnim("takeoff", TAKEOFF)
                .triggerableAnim("landing", LANDING)
                .triggerableAnim("deploy", MachineBlockEntity.SETUP)
                .setSoundKeyframeHandler(new MachineSoundHandler<>()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.energy / (float) energyStorage.capacity) * 15);
    }

    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.getStacks().size() <= slot)
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
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        sendUpdate(SyncType.GUI_OPEN);
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
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return inventory;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.DRONE_SCREEN.get();
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

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        return hasFluidAddon ? fluidStorage : null;
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory;
    }

    public record DroneTransferData(List<ItemStack> transferredStacks, FluidStack movedFluid, long arrivesAt) {
    }

    public class DronePortItemInventory extends SimpleInventoryStorage {

        public DronePortItemInventory(int size, Runnable onUpdate) {
            super(size, onUpdate);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (DronePortEntity.this.incomingPacket != null && !receivingPackage) return 0;
            return super.insert(index, resource, amount, transaction);
        }
    }

    public class DronePortFluidStorage extends SimpleFluidStorage {

        public DronePortFluidStorage(int capacity, Runnable onUpdate) {
            super(capacity, onUpdate);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (DronePortEntity.this.incomingPacket != null && !receivingPackage) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        public int insertFromDrone(FluidResource resource, int amount, TransactionContext transaction) {
            // bypasses insert check
            return super.insert(0, resource, amount, transaction);
        }
    }
}
