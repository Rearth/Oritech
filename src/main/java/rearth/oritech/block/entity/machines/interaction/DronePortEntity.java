package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.blocks.machines.interaction.DronePortBlock;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.DynamicEnergyStorage;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.MultiblockMachineController;
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

public class DronePortEntity extends BlockEntity implements InventoryProvider, EnergyProvider, GeoBlockEntity, BlockEntityTicker<DronePortEntity>, MultiblockMachineController {
    
    public record DroneTransferData(List<ItemStack> transferredStacks, long arrivesAt) {
    }
    
    // storage
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 1000, 0) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            DronePortEntity.this.markDirty();
        }
    };
    
    protected final SimpleInventory inventory = new SimpleInventory(15) {
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
    
    // config
    private final long baseEnergyUsage = 1000;
    private final int takeOffTime = 300;
    private final int landTime = 260;
    
    public DronePortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DRONE_PORT_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, DronePortEntity blockEntity) {
        
        if (world.isClient) return;
        
        if (incomingPacket != null)
            checkIncomingAnimation();
        
        if (world.getTime() % 20 == 0) {
            if (incomingPacket != null) {
                tryReceivePacket();
            } else if (canSend()) {
                sendDrone();
            }
        }
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
        
        System.out.println("receiving drone package: " + incomingPacket);
        
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
        triggerNetworkSendAnimation();
        targetPort.markDirty();
        this.markDirty();
        
        System.out.println("sending drone package: " + data);
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
        var distance = pos.getManhattanDistance(targetPosition);
        return (long) Math.sqrt(distance) + baseEnergyUsage;
    }
    
    private void triggerNetworkSendAnimation() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.DroneSendEventPacket(pos, true, false));
    }
    
    private void triggerNetworkReceiveAnimation() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.DroneSendEventPacket(pos, false, true));
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
            return false;
        }
        
        if (world.getBlockState(targetPos).getBlock() instanceof DronePortBlock) {
            // store position
            System.out.println("target port stored: " + targetPos);
            this.targetPosition = targetPos;
            return true;
        }
        
        return false;
        
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
        this.coreQuality = coreQuality;
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
        });
    }
}
