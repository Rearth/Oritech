package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
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
import net.minecraft.network.PacketByteBuf;
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
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.blocks.machines.interaction.DronePortBlock;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.DroneScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.block.base.entity.MachineBlockEntity.*;

public class DronePortEntity extends BlockEntity implements InventoryProvider, EnergyProvider, GeoBlockEntity, BlockEntityTicker<DronePortEntity>, MultiblockMachineController, ExtendedScreenHandlerFactory, ScreenProvider {
    
    public record DroneTransferData(List<ItemStack> transferredStacks, long arrivesAt) {
    }
    
    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(1024 * 32, 1000, 0) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            DronePortEntity.this.markDirty();
        }
    };
    
    public final SimpleInventory inventory = new SimpleInventory(15) {
        @Override
        public void markDirty() {
            DronePortEntity.this.markDirty();
        }
        
        @Override
        public boolean canInsert(ItemStack stack) {
            if (DronePortEntity.this.incomingPacket != null) return false;
            return super.canInsert(stack);
        }
    };
    
    // not persisted, only to assign targets
    protected final SimpleInventory cardInventory = new SimpleInventory(2) {
        @Override
        public void markDirty() {
            DronePortEntity.this.markDirty();
        }
        
        @Override
        public boolean canInsert(ItemStack stack) {
            return stack.getItem().equals(ItemContent.TARGET_DESIGNATOR);
        }
    };
    
    protected final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    private float coreQuality = 1f;
    
    // animation
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AnimationController<DronePortEntity> animationController = getAnimationController();
    
    // multiblock
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
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
            sendNetworkEnergyUpdate();
        }
    }
    
    private void checkPositionCard() {
        
        var source = cardInventory.heldStacks.get(0);
        if (source.getItem().equals(ItemContent.TARGET_DESIGNATOR) && source.hasNbt()) {
            var target = BlockPos.fromLong(source.getNbt().getLong("target"));
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
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory.heldStacks, false);
        addMultiblockToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        
        if (targetPosition != null) {
            nbt.putLong("target_position", targetPosition.asLong());
        }
        
        if (incomingPacket != null) {
            var compound = new NbtCompound();
            DefaultedList<ItemStack> list = DefaultedList.ofSize(incomingPacket.transferredStacks.size());
            list.addAll(incomingPacket.transferredStacks);
            Inventories.writeNbt(compound, list, false);
            nbt.put("incoming", compound);
            nbt.putLong("incomingTime", incomingPacket.arrivesAt);
        } else {
            nbt.remove("incoming");
        }
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory.heldStacks);
        loadMultiblockNbtData(nbt);
        
        energyStorage.amount = nbt.getLong("energy_stored");
        targetPosition = BlockPos.fromLong(nbt.getLong("target_position"));
        
        if (nbt.contains("incoming")) {
            DefaultedList<ItemStack> list = DefaultedList.ofSize(15);
            Inventories.readNbt(nbt.getCompound("incoming"), list);
            var arrivalTime = nbt.getLong("incomingTime");
            incomingPacket = new DroneTransferData(list, arrivalTime);
        }
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
        
        try (var tx = Transaction.openOuter()) {
            for (var stack : incomingPacket.transferredStacks) {
                inventoryStorage.insert(ItemVariant.of(stack), stack.getCount(), tx);
            }
            tx.commit();
        }
        incomingPacket = null;
        markDirty();
    }
    
    private void sendDrone() {
        var targetPort = (DronePortEntity) world.getBlockEntity(targetPosition);
        var arriveTime = world.getTime() + takeOffTime + landTime;
        var data = new DroneTransferData(inventory.heldStacks.stream().filter(stack -> !stack.isEmpty()).toList(), arriveTime);
        targetPort.setIncomingPacket(data);
        
        inventory.clear();
        lastSentAt = world.getTime();
        energyStorage.amount -= calculateEnergyUsage();
        
        triggerNetworkSendAnimation();
        targetPort.markDirty();
        this.markDirty();
        
        Oritech.LOGGER.debug("sending drone package: " + data);
    }
    
    public boolean canAcceptItems(List<ItemStack> stacks) {
        var tx = Transaction.openOuter();
        for (var stack : stacks) {
            if (stack.isEmpty()) continue;
            if (inventoryStorage.insert(ItemVariant.of(stack.getItem()), stack.getCount(), tx) != stack.getCount()) {
                tx.abort();
                return false;
            }
        }
        
        tx.abort();
        return true;
    }
    
    private boolean canSend() {
        
        if (targetPosition == null || inventory.isEmpty() || energyStorage.amount < calculateEnergyUsage() || incomingPacket != null)
            return false;
        var targetEntity = world.getBlockEntity(targetPosition);
        if (!(targetEntity instanceof DronePortEntity targetPort) || targetPort.getIncomingPacket() != null || !targetPort.canAcceptItems(inventory.heldStacks))
            return false;
        
        
        var diff = world.getTime() - lastSentAt;
        return diff > takeOffTime;
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
    
    private void sendNetworkEnergyUpdate() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GenericEnergySyncPacket(pos, energyStorage.amount, energyStorage.capacity));
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
            sendNetworkStatusMessage("Target must be at least 50 blocks away.\n(current distance: " + distance + ")");
            return false;
        }
        
        if (world.getBlockState(targetPos).getBlock() instanceof DronePortBlock) {
            // store position
            this.targetPosition = targetPos;
            sendNetworkStatusMessage("Target port set.\nDrone will deliver whenever the inventory is not empty.");
            return true;
        }
        
        sendNetworkStatusMessage("Target is not a valid drone port.\nEnsure that the target port is loaded and active.");
        return false;
        
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        this.networkDirty = true;
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
        return energyStorage;
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
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        sendNetworkEnergyUpdate();
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
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
}
