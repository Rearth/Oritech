package rearth.oritech.block.base.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ItemEnergyFrameInteractionBlockEntity extends FrameInteractionBlockEntity implements InventoryProvider, EnergyProvider, ExtendedScreenHandlerFactory, ScreenProvider, MachineAddonController {
    
    public final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), 0) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            ItemEnergyFrameInteractionBlockEntity.this.markDirty();
        }
    };
    
    public final SimpleInventory inventory = new SimpleInventory(getInventorySize()) {
        @Override
        public void markDirty() {
            ItemEnergyFrameInteractionBlockEntity.this.markDirty();
        }
    };
    
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openSlots = new ArrayList<>();
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    public ItemEnergyFrameInteractionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public abstract int getMoveEnergyUsage();
    
    public abstract int getOperationEnergyUsage();
    
    @Override
    protected boolean canProgress() {
        return
          energyStorage.amount >= getMoveEnergyUsage() * getBaseAddonData().efficiency() * (1 / getBaseAddonData().speed()) &&
            energyStorage.amount >= getOperationEnergyUsage() * getBaseAddonData().efficiency() * (1 / getBaseAddonData().speed());
    }
    
    @Override
    protected void doProgress(boolean moving) {
        energyStorage.amount -= (long) (getMoveEnergyUsage() * getBaseAddonData().efficiency() * (1 / getBaseAddonData().speed()));
        this.markDirty();
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        energyStorage.amount -= getOperationEnergyUsage();
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        energyStorage.amount = nbt.getLong("energy_stored");
        
        loadAddonNbtData(nbt);
        updateEnergyContainer();
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putLong("energy_stored", energyStorage.amount);
        writeAddonToNbt(nbt);
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return InventoryStorage.of(inventory, direction);
    }
    
    @Override
    public EnergyStorage getStorage(Direction direction) {
        return energyStorage;
    }
    
    
    @Override
    public BlockPos getMachinePos() {
        return getPos();
    }
    
    @Override
    public World getMachineWorld() {
        return getWorld();
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        updateNetwork();
        return new ModScreens.UpgradableData(pos, getUiData(), getCoreQuality());
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this, getUiData(), getCoreQuality());
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("");
    }
    
    @Override
    public List<ScreenProvider.GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 50, 11));
    }
    
    public int getInventorySize() {
        return 1;
    }
    
    @Override
    public float getProgress() {
        if (world.isClient) {
            var time = world.getTime();
            // do not update progress while entity is moving
            if (time < this.getMoveStartedAt() + this.getMoveTime() && this.getMoveStartedAt() > 1) {
                return 0;
            }
        }
        return (float) getCurrentProgress() / this.getWorkTime();
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
        return 10000;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 512;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, FrameInteractionBlockEntity blockEntity) {
        super.tick(world, pos, state, blockEntity);
        
        if (!world.isClient && isActivelyViewed()) {
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.MachineFrameGuiPacket(pos, energyStorage.amount, energyStorage.capacity, getCurrentProgress()));
        }
    }
    
    protected boolean isActivelyViewed() {
        var closestPlayer = Objects.requireNonNull(world).getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5, false);
        return closestPlayer != null && closestPlayer.currentScreenHandler instanceof BasicMachineScreenHandler handler && getPos().equals(handler.getBlockPos());
    }
    
    @Override
    public SimpleInventory getInventoryForAddon() {
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
    public List<BlockPos> getOpenSlots() {
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
}
