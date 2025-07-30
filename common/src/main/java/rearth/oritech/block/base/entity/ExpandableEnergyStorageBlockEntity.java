package rearth.oritech.block.base.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.EnergyApi.EnergyStorage;
import rearth.oritech.api.energy.containers.DelegatingEnergyStorage;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.energy.containers.DynamicStatisticEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.blocks.storage.SmallStorageBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class ExpandableEnergyStorageBlockEntity extends NetworkedBlockEntity implements EnergyApi.BlockProvider, ItemApi.BlockProvider, MachineAddonController,
                                                                                          ScreenProvider, ExtendedMenuProvider {
    
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> openSlots = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    @SyncField(SyncType.GUI_TICK)
    private boolean redstonePowered;
    
    @SyncField(SyncType.GUI_TICK)
    public DynamicStatisticEnergyStorage.EnergyStatistics currentStats;
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(1, this::setChanged);
    
    //own storage
    @SyncField(SyncType.GUI_TICK)
    public final DynamicStatisticEnergyStorage energyStorage = new DynamicStatisticEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), getDefaultExtractionRate(), this::setChanged);
    
    private final EnergyApi.EnergyStorage outputStorage = new DelegatingEnergyStorage(energyStorage, null) {
        @Override
        public boolean supportsInsertion() {
            return false;
        }
        
        @Override
        public long insert(long amount, boolean simulate) {
            return 0L;
        }
    };
    
    private final EnergyApi.EnergyStorage inputStorage = new DelegatingEnergyStorage(energyStorage, null) {
        @Override
        public boolean supportsExtraction() {
            return false;
        }
        
        @Override
        public long extract(long amount, boolean simulate) {
            return 0L;
        }
    };
    
    public ExpandableEnergyStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (world.isClientSide) return;
        
        energyStorage.tick((int) world.getGameTime());
        
        if (!redstonePowered)
            outputEnergy();
        
        inputFromCrystal();
    }
    
    private void inputFromCrystal() {
        if (energyStorage.amount >= energyStorage.capacity || inventory.isEmpty()) return;
        
        if (!inventory.getItem(0).getItem().equals(ItemContent.OVERCHARGED_CRYSTAL)) return;
        
        energyStorage.amount = Math.min(energyStorage.capacity, energyStorage.amount + Oritech.CONFIG.overchargedCrystalChargeRate());
    }
    
    private void outputEnergy() {
        if (energyStorage.amount <= 0) return;
        
        chargeItems();
        
        // todo caching for targets? Used to be BlockApiCache.create()
        var target = getOutputPosition(worldPosition, getFacing());
        var candidate = EnergyApi.BLOCK.find(level, target.getB(), target.getA().getOpposite());
        if (candidate != null && candidate.supportsInsertion()) {
            EnergyApi.transfer(energyStorage, candidate, Long.MAX_VALUE, false);
        }
    }
    
    private void chargeItems() {
        
        var heldStack = inventory.heldStacks.get(0);
        if (heldStack.isEmpty() || heldStack.getCount() > 1) return;
        
        var stackRef = new StackContext(heldStack, updated -> inventory.heldStacks.set(0, updated));
        var slotEnergyContainer = EnergyApi.ITEM.find(stackRef);
        if (slotEnergyContainer != null) {
            EnergyApi.transfer(energyStorage, slotEnergyContainer, Long.MAX_VALUE, false);
        }
    }
    
    public static Tuple<Direction, BlockPos> getOutputPosition(BlockPos pos, Direction facing) {
        var blockInFront = (BlockPos) Geometry.offsetToWorldPosition(facing, new Vec3i(-1, 0, 0), pos);
        var worldOffset = blockInFront.subtract(pos);
        var direction = Direction.fromDelta(worldOffset.getX(), worldOffset.getY(), worldOffset.getZ());
        
        return new Tuple<>(direction, blockInFront);
    }
    
    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        writeAddonToNbt(nbt);
        nbt.putLong("energy_stored", energyStorage.amount);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putBoolean("redstone", redstonePowered);
    }
    
    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        loadAddonNbtData(nbt);
        updateEnergyContainer();
        energyStorage.amount = nbt.getLong("energy_stored");
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        redstonePowered = nbt.getBoolean("redstone");
    }
    
    @Override
    public void preNetworkUpdate(SyncType type) {
        super.preNetworkUpdate(type);
        currentStats = energyStorage.getCurrentStatistics(level.getGameTime());
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    public Direction getFacing() {
        return getBlockState().getValue(SmallStorageBlock.TARGET_DIR);
    }
    
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        
        if (direction == null)
            return energyStorage;
        
        if (direction.equals(getFacing())) {
            return outputStorage;
        } else {
            return inputStorage;
        }
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
        var facing = Objects.requireNonNull(level).getBlockState(getBlockPos()).getValue(SmallStorageBlock.TARGET_DIR);
        
        if (facing.equals(Direction.UP) || facing.equals(Direction.DOWN))
            return Direction.NORTH;
        
        return facing;
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
        return this;
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
    public void updateEnergyContainer() {
        MachineAddonController.super.updateEnergyContainer();
        energyStorage.maxExtract = getDefaultExtractionRate() + addonData.energyBonusTransfer();
        
    }
    
    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.maxInsert;
    }
    
    public abstract long getDefaultExtractionRate();
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 30, 42));
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
    public BlockPos getPosForAddon() {
        return getBlockPos();
    }
    
    @Override
    public Level getWorldForAddon() {
        return getLevel();
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
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.STORAGE_SCREEN;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    @Override
    public Property<Direction> getBlockFacingProperty() {
        return SmallStorageBlock.TARGET_DIR;
    }
    
    public void setRedstonePowered(boolean isPowered) {
        this.redstonePowered = isPowered;
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
        if (receivedRedstoneSignal() > 0) return "tooltip.oritech.redstone_disabled_storage";
        return "tooltip.oritech.redstone_enabled_direct";
    }
}
