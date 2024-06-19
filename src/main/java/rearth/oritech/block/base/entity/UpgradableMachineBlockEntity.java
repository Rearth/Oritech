package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.util.DynamicEnergyStorage;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class UpgradableMachineBlockEntity extends MachineBlockEntity implements MachineAddonController {
    
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    private final List<BlockPos> openSlots = new ArrayList<>();
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    public UpgradableMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        writeAddonToNbt(nbt);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        loadAddonNbtData(nbt);
        
        updateEnergyContainer();
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
        return super.getEnergyStorage();
    }
    
    @Override
    public BaseAddonData getBaseAddonData() {
        return addonData;
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
    public void setBaseAddonData(BaseAddonData data) {
        this.addonData = data;
        this.markDirty();
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        sendNetworkEntry();
        return new ModScreens.UpgradableData(pos, getUiData(), getCoreQuality());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this, getUiData(), getCoreQuality());
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
        return getBaseAddonData().speed();
    }
    
    @Override
    public float getEfficiencyMultiplier() {
        return getBaseAddonData().efficiency();
    }
}
