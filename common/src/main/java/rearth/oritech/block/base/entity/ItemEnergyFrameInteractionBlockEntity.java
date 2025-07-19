package rearth.oritech.block.base.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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

public abstract class ItemEnergyFrameInteractionBlockEntity extends FrameInteractionBlockEntity
  implements ItemApi.BlockProvider, EnergyApi.BlockProvider, ExtendedMenuProvider, ScreenProvider, MachineAddonController, RedstoneAddonBlockEntity.RedstoneControllable {
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), 0, this::markDirty);
    
    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(getInventorySize(), this::markDirty);
    
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
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        energyStorage.amount = nbt.getLong("energy_stored");
        disabledViaRedstone = nbt.getBoolean("oritech.redstone");
        
        loadAddonNbtData(nbt);
        updateEnergyContainer();
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
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
        return getPos();
    }
    
    @Override
    public World getWorldForAddon() {
        return getWorld();
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(pos);
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("");
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
    public Inventory getDisplayedInventory() {
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
        this.markDirty();
    }
    
    public boolean isActivelyWorking() {
        return world.getTime() - lastWorkedAt < 5;
    }
    
    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.amount / (float) energyStorage.capacity) * 15);
    }
    
    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.heldStacks.size() <= slot) return 0;
        
        var stack = inventory.getStack(slot);
        if (stack.isEmpty()) return 0;
        
        return (int) ((stack.getCount() / (float) stack.getMaxCount()) * 15);
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
