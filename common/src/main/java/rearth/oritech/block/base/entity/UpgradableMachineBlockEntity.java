package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class UpgradableMachineBlockEntity extends MachineBlockEntity implements MachineAddonController {
    
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> openSlots = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private BaseAddonData addonData = MachineAddonController.DEFAULT_ADDON_DATA;
    
    public UpgradableMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        super.craftItem(activeRecipe, outputInventory, inputInventory);
        
        if (supportExtraChambersAuto()) {
            var chamberCount = addonData.extraChambers();
            
            // craft N extra items if we have extra chambers
            for (int i = 0; i < chamberCount; i++) {
                var newRecipe = getRecipe();
                if (newRecipe.isEmpty() || !newRecipe.get().value().equals(currentRecipe) || !canOutputRecipe(activeRecipe) || !canProceed(activeRecipe)) break;
                super.craftItem(activeRecipe, outputInventory, inputInventory);
            }
        }
        
    }
    
    // this should return false if the default craftItem implementation should not handle extra chambers
    public boolean supportExtraChambersAuto() {
        return true;
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
    public List<BlockPos> getOpenAddonSlots() {
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
    public BlockPos getPosForAddon() {
        return getPos();
    }
    
    @Override
    public World getWorldForAddon() {
        return getWorld();
    }
    
    @Override
    public void setBaseAddonData(BaseAddonData data) {
        this.addonData = data;
        this.markDirty();
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new UpgradableMachineScreenHandler(syncId, playerInventory, this);
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
        return getBaseAddonData().speed();
    }
    
    @Override
    public float getEfficiencyMultiplier() {
        return getBaseAddonData().efficiency();
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
