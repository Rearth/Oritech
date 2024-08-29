package rearth.oritech.block.entity.machines.interaction;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;

import java.util.*;

public class TreefellerBlockEntity extends BlockEntity implements BlockEntityTicker<TreefellerBlockEntity>, GeoBlockEntity, EnergyProvider, InventoryProvider, ScreenProvider, ExtendedScreenHandlerFactory {
    
    private static final int LOG_COST = 100;
    private static final int LEAF_COST = 10;
    
    private final Deque<BlockPos> pendingBlocks = new ArrayDeque<>();
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    private long lastWorkedAt = 0;
    private boolean networkDirty = false;
    
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 4000, 0) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            TreefellerBlockEntity.this.markDirty();
        }
    };
    
    public final SimpleInventory inventory = new SimpleInventory(6) {
        @Override
        public void markDirty() {
            TreefellerBlockEntity.this.markDirty();
        }
        
        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    };
    
    protected final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    
    public TreefellerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.TREEFELLER_BLOCK_ENTITY, pos, state);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, TreefellerBlockEntity blockEntity) {
        if (world.isClient || energyStorage.amount < LOG_COST) return;
        
        if (pendingBlocks.isEmpty() && world.getTime() % 20 == 0) {
            findTarget();
        }
        
        for (int i = 0; i < 6 && !pendingBlocks.isEmpty(); i++) {
            var candidate = pendingBlocks.pollLast();
            var candidateState = world.getBlockState(candidate);
            var isLog = candidateState.isIn(BlockTags.LOGS);
            
            breakTreeBlock(candidateState, candidate);
            lastWorkedAt = world.getTime();
            
            var energyCost = isLog ? LOG_COST : LEAF_COST;
            energyStorage.amount -= energyCost;
            if (energyCost > energyStorage.amount) break;
            this.markDirty();
            
            if (isLog) break; // only harvest 1 log, but multiple leaves
        }
        
        if (world.getTime() % 10 == 0) {
            var idleTicks = world.getTime() - lastWorkedAt;
            var isWorking = idleTicks < 20;
            var animName = isWorking ? "work" : "idle";
            playWorkAnimation(animName);
        }
        
        if (networkDirty && world.getTime() % 4 == 0) {
            networkDirty = false;
            sendNetworkEntry();
        }
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        networkDirty = true;
    }
    
    private void breakTreeBlock(BlockState candidateState, BlockPos candidate) {
        if (!candidateState.isIn(BlockTags.LOGS) && !candidateState.isIn(BlockTags.LEAVES)) return;
        
        var dropped = Block.getDroppedStacks(candidateState, (ServerWorld) world, candidate, null);
        world.addBlockBreakParticles(candidate, candidateState);
        if (world.getTime() % 2 == 0)
            world.playSound(null, candidate, candidateState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 0.5f, 1f);
        world.setBlockState(candidate, Blocks.AIR.getDefaultState());
        
        dropped.forEach(inventory::addStack);
    }
    
    public void findTarget() {
        
        var state = getCachedState();
        var facing = state.get(Properties.HORIZONTAL_FACING);
        var offset = Geometry.rotatePosition(new Vec3i(1, 0, 0), facing);
        var frontBlock = pos.add(offset);
        
        var res = getTreeBlocks(frontBlock, world);
        pendingBlocks.addAll(res);
        
    }
    
    public static Deque<BlockPos> getTreeBlocks(BlockPos startPos, World world) {
        
        var startState = world.getBlockState(startPos);
        if (!startState.isIn(BlockTags.LOGS)) return new ArrayDeque<>();
        
        var checkedPositions = new HashSet<BlockPos>();
        var foundPositions = new ArrayDeque<BlockPos>();
        var foundLogs = new HashSet<BlockPos>();
        var pendingPositions = new ArrayDeque<BlockPos>();
        
        checkedPositions.add(startPos);
        foundPositions.add(startPos);
        pendingPositions.addAll(getNeighbors(startPos));
        foundLogs.add(startPos);
        
        while (!pendingPositions.isEmpty() && checkedPositions.size() < 1500) {
            // do logs first, if none available then leaves
            var candidate = pendingPositions.pollFirst();
            if (candidate.getY() < startPos.getY()) continue;
            
            if (checkedPositions.contains(candidate)) continue;
            
            var candidateState = world.getBlockState(candidate);
            checkedPositions.add(candidate);
            
            var isLog = candidateState.isIn(BlockTags.LOGS);
            var isValidLeaf = candidateState.isIn(BlockTags.LEAVES) && !candidateState.get(Properties.PERSISTENT);
            
            if (!isLog && !isValidLeaf) continue;
            
            var isValid = false;
            if (isLog) {
                isValid = isInLogRange(candidate, foundLogs, 3);
            } else {
                var range = candidateState.get(Properties.DISTANCE_1_7);
                isValid = isInLogRange(candidate, foundLogs, range + 2);
            }
            
            if (!isValid) continue;
            
            if (isLog) {
                foundLogs.add(candidate);
            }
            
            foundPositions.add(candidate);
            pendingPositions.addAll(getNeighbors(candidate));
            
        }
        
        // when no leaves are found, return nothing to prevent accidentally destorying buildings
        if (foundLogs.size() == foundPositions.size()) return new ArrayDeque<>();
        
        return foundPositions;
    }
    
    private static boolean isInLogRange(BlockPos pos, Set<BlockPos> logs, int maxDist) {
        return logs.stream().anyMatch(elem -> elem.getManhattanDistance(pos) <= maxDist);
    }
    
    private static List<BlockPos> getNeighbors(BlockPos input) {
        return List.of(input.up(), input.north(), input.east(), input.south(), input.west(), input.down(),
          input.up().east(), input.up().west(), input.up().north(), input.up().south(),
          input.up().east().north(), input.up().west().north(), input.up().east().south(), input.up().west().south(),
          input.north().east(), input.north().west(), input.south().east(), input.south().west());
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy_stored", energyStorage.amount);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        energyStorage.amount = nbt.getLong("energy_stored");
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 5, state -> PlayState.CONTINUE)
                          .triggerableAnim("work", MachineBlockEntity.WORKING)
                          .triggerableAnim("idle", MachineBlockEntity.IDLE)
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
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
    public List<GuiSlot> getGuiSlots() {
        var list = new ArrayList<GuiSlot>();
        for (int i = 0; i < inventory.size(); i++) {
            list.add(new GuiSlot(i, 40 + i * 19, 25, true));
        }
        return list;
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return LOG_COST;
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
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.TREEFELLER_SCREEN;
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new ModScreens.BasicData(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        sendNetworkEntry();
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    public void playWorkAnimation(String animName) {
        triggerAnim("machine", animName);
    }
    
    private void sendNetworkEntry() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.GenericEnergySyncPacket(pos, energyStorage.amount, energyStorage.capacity));
    }
}
