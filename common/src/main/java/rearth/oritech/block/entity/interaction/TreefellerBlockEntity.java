package rearth.oritech.block.entity.interaction;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.AutoPlayingSoundKeyframeHandler;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TreefellerBlockEntity extends NetworkedBlockEntity implements BlockEntityTicker<NetworkedBlockEntity>, GeoBlockEntity, EnergyApi.BlockProvider, ItemApi.BlockProvider, ExtendedMenuProvider, ScreenProvider {
    
    private static final int LOG_COST = 100;
    private static final int LEAF_COST = 10;
    
    private final Deque<BlockPos> pendingBlocks = new ArrayDeque<>();
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    private long lastWorkedAt = 0;
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 4000, 0, this::setChanged);
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(6, this::setChanged) {
        
        @Override
        public boolean supportsInsertion() {
            return false;
        }
    };
    
    public TreefellerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.TREEFELLER_BLOCK_ENTITY, pos, state);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (energyStorage.amount >= LOG_COST) {
            if (pendingBlocks.isEmpty() && world.getGameTime() % 20 == 0) {
                findTarget();
            }
            
            for (int i = 0; i < 6 && !pendingBlocks.isEmpty(); i++) {
                var candidate = pendingBlocks.peekLast();
                var candidateState = world.getBlockState(candidate);
                var isLog = candidateState.is(TagContent.CUTTER_LOGS_MINEABLE);

                var energyCost = isLog ? LOG_COST : LEAF_COST;
                if (energyCost > energyStorage.amount) break;
                
                var actionResult = breakTreeBlock(candidateState, candidate);
                if (actionResult == InteractionResult.FAIL) break;
                pendingBlocks.pollLast();
                if (actionResult == InteractionResult.PASS) continue;
                lastWorkedAt = world.getGameTime();

                energyStorage.amount -= energyCost;
                setChanged();
                
                if (isLog) break; // only harvest 1 log, but multiple leaves
            }
        }
        
        if (world.getGameTime() % 10 == 0) {
            var idleTicks = world.getGameTime() - lastWorkedAt;
            var isWorking = idleTicks < 20;
            var animName = isWorking ? "work" : "idle";
            playWorkAnimation(animName);
        }
    }
    
    private InteractionResult breakTreeBlock(BlockState candidateState, BlockPos candidate) {
        if (!candidateState.is(TagContent.CUTTER_LOGS_MINEABLE) && !candidateState.is(TagContent.CUTTER_LEAVES_MINEABLE)) return InteractionResult.PASS;
        
        var dropped = net.minecraft.world.level.block.Block.getDrops(candidateState, (ServerLevel) level, candidate, null);
        if (dropped.stream().anyMatch((itemStack) -> !(itemStack.isEmpty() || canInsert(itemStack)))) return InteractionResult.FAIL;

        level.addDestroyBlockEffect(candidate, candidateState);
        if (level.getGameTime() % 2 == 0)
            level.playSound(null, candidate, candidateState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.5f, 1f);
        level.setBlockAndUpdate(candidate, Blocks.AIR.defaultBlockState());
        
        dropped.forEach(stack -> inventory.insert(stack, false));
        return InteractionResult.SUCCESS;
    }

    private boolean canInsert(ItemStack stack) {
        return inventory.heldStacks.stream().anyMatch((itemStack) -> 
            itemStack.isEmpty() || (ItemStack.isSameItemSameComponents(itemStack, stack) && itemStack.getCount() + stack.getCount() <= itemStack.getMaxStackSize())
        );
     }
    
    public void findTarget() {
        
        var state = getBlockState();
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var offset = Geometry.rotatePosition(new Vec3i(1, 0, 0), facing);
        var frontBlock = worldPosition.offset(offset);
        
        var res = getTreeBlocks(frontBlock, level);
        pendingBlocks.addAll(res);
        
    }
    
    public static Deque<BlockPos> getTreeBlocks(BlockPos startPos, Level world) {
        
        var startState = world.getBlockState(startPos);
        if (!startState.is(TagContent.CUTTER_LOGS_MINEABLE)) return new ArrayDeque<>();
        
        var checkedPositions = new HashSet<BlockPos>();
        var foundPositions = new ArrayDeque<BlockPos>();
        var foundLogs = new HashSet<BlockPos>();
        var pendingPositions = new ArrayDeque<BlockPos>();
        
        checkedPositions.add(startPos);
        foundPositions.add(startPos);
        pendingPositions.addAll(getNeighbors(startPos));
        foundLogs.add(startPos);
        
        while (!pendingPositions.isEmpty() && checkedPositions.size() < 8000) {
            // do logs first, if none available then leaves
            var candidate = pendingPositions.pollFirst();
            if (candidate.getY() < startPos.getY()) continue;
            
            if (checkedPositions.contains(candidate)) continue;
            
            var candidateState = world.getBlockState(candidate);
            checkedPositions.add(candidate);
            
            var isLog = candidateState.is(TagContent.CUTTER_LOGS_MINEABLE);
            var isValidLeaf = candidateState.is(TagContent.CUTTER_LEAVES_MINEABLE) && !candidateState.getOptionalValue(BlockStateProperties.PERSISTENT).orElse(false);
            
            if (!isLog && !isValidLeaf) continue;
            
            var isValid = false;
            if (isLog) {
                isValid = isInLogRange(candidate, foundLogs, 3);
            } else {
                // Give a default of 1 for "leaf" blocks without a DISTANCE_1_7 property (like shroomlights)
                var range = candidateState.getOptionalValue(BlockStateProperties.DISTANCE).orElse(1);
                isValid = isInLogRange(candidate, foundLogs, range + 2);
            }
            
            if (!isValid) continue;
            
            if (isLog) {
                foundLogs.add(candidate);
            }
            
            foundPositions.add(candidate);
            pendingPositions.addAll(getNeighbors(candidate));
            
        }
        
        // when no leaves are found, return nothing to prevent accidentally destroying buildings
        if (foundLogs.size() == foundPositions.size()) return new ArrayDeque<>();
        
        return foundPositions;
    }
    
    private static boolean isInLogRange(BlockPos pos, Set<BlockPos> logs, int maxDist) {
        return logs.stream().anyMatch(elem -> elem.distManhattan(pos) <= maxDist);
    }
    
    private static List<BlockPos> getNeighbors(BlockPos input) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (BlockPos pos : BlockPos.withinManhattan(input, 1, 1, 1)) {
            // Without toImmutable, all of the elements in the collected list end up being the same BlockPos
            neighbors.add(pos.immutable());
        }
        return neighbors;
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy_stored", energyStorage.amount);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
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
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        var list = new ArrayList<GuiSlot>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
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
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.TREEFELLER_SCREEN;
    }
    
    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    public void playWorkAnimation(String animName) {
        triggerAnim("machine", animName);
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        this.sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
}
