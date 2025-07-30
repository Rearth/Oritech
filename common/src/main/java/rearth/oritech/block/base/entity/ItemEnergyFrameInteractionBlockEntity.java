package rearth.oritech.block.base.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ItemEnergyFrameInteractionBlockEntity extends FrameInteractionBlockEntity
  implements ItemApi.BlockProvider, EnergyApi.BlockProvider, ExtendedMenuProvider, ScreenProvider, MachineAddonController, RedstoneAddonBlockEntity.RedstoneControllable {
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), 0, this::setChanged);
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(getInventorySize(), this::setChanged);
    
    @SyncField({SyncType.GUI_OPEN})
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField({SyncType.GUI_OPEN})
    private final List<BlockPos> openSlots = new ArrayList<>();
    
    @SyncField({SyncType.GUI_OPEN})
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    public ItemEnergyFrameInteractionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public abstract int getMoveEnergyUsage();
    
    public abstract int getOperationEnergyUsage();
    
    @Override
    protected boolean canProgress() {
        return !disabledViaRedstone &&
                 energyStorage.amount >= getMoveEnergyUsage() * getBaseAddonData().efficiency() * (1 / getBaseAddonData().speed()) &&
                 energyStorage.amount >= getOperationEnergyUsage() * getBaseAddonData().efficiency() * (1 / getBaseAddonData().speed());
    }
    
    @Override
    protected void doProgress(boolean moving) {
        var usedCost = moving ? getMoveEnergyUsage() : getOperationEnergyUsage();
        energyStorage.amount -= (long) (usedCost * getBaseAddonData().efficiency() * (1 / getBaseAddonData().speed()));
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        energyStorage.amount = nbt.getLong("energy_stored");
        disabledViaRedstone = nbt.getBoolean("oritech.redstone");
        
        loadAddonNbtData(nbt);
        updateEnergyContainer();
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy_stored", energyStorage.amount);
        nbt.putBoolean("oritech.redstone", disabledViaRedstone);
        writeAddonToNbt(nbt);
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
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
    public void saveExtraData(FriendlyByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 50, 11));
    }
    
    public int getInventorySize() {
        return 1;
    }
    
    @Override
    public float getProgress() {
        var maxTime = isMoving() ? getMoveTime() : getWorkTime();
        return (float) getCurrentProgress() / maxTime;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return getOperationEnergyUsage() * getBaseAddonData().efficiency() * (1 / getBaseAddonData().speed());
    }
    
    @Override
    public long getDefaultCapacity() {
        return 100_000;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 5000;
    }
    
    @Override
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.maxInsert;
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
    public float getSpeedMultiplier() {
        return addonData.speed();
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
        return super.getFacing();
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
    
    public boolean isActivelyWorking() {
        return level.getGameTime() - lastWorkedAt < 5;
    }
    
    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.amount / (float) energyStorage.capacity) * 15);
    }
    
    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.heldStacks.size() <= slot) return 0;
        
        var stack = inventory.getItem(slot);
        if (stack.isEmpty()) return 0;
        
        return (int) ((stack.getCount() / (float) stack.getMaxStackSize()) * 15);
    }
    
    @Override
    public int getComparatorProgress() {
        return 0;
    }
    
    @Override
    public int getComparatorActiveState() {
        return isActivelyWorking() ? 15 : 0;
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
}
