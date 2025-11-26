package rearth.oritech.block.base.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.NetworkedBlockEntity;
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
    private BaseAddonData addonData = BaseAddonData.DEFAULT_ADDON_DATA;
    
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public int remainingBurstTicks = 0;
    
    public UpgradableMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }
    
    @Override
    protected void useEnergy() {
        super.useEnergy();
        
        consumeBurstTicks();
        
    }
    
    public void consumeBurstTicks() {
        // consume burst tick with each tick that we progress (which uses energy once)
        remainingBurstTicks -= 2;
        remainingBurstTicks = Math.max(remainingBurstTicks, -addonData.maxBurstTicks());
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        super.serverTick(world, pos, state, blockEntity);
        
        addBurstTicks();
    }
    
    public void addBurstTicks() {
        remainingBurstTicks++;
        remainingBurstTicks = Math.min(remainingBurstTicks, addonData.maxBurstTicks());
    }
    
    @Override
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        super.craftItem(activeRecipe, outputInventory, inputInventory);
        
        if (supportExtraChambersAuto()) {
            var chamberCount = addonData.extraChambers();
            
            // craft N extra items if we have extra chambers
            for (int i = 0; i < chamberCount; i++) {
                var newRecipe = getRecipe();
                if (newRecipe.isEmpty() || !newRecipe.get().value().equals(currentRecipe) || !canOutputRecipe(activeRecipe) || !canProceed(activeRecipe))
                    break;
                super.craftItem(activeRecipe, outputInventory, inputInventory);
            }
        }
        
    }
    
    // this should return false if the default craftItem implementation should not handle extra chambers
    public boolean supportExtraChambersAuto() {
        return true;
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        writeAddonToNbt(nbt);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
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
        return getBlockPos();
    }
    
    @Override
    public Level getWorldForAddon() {
        return getLevel();
    }
    
    @Override
    public void setBaseAddonData(BaseAddonData data) {
        this.addonData = data;
        this.setChanged();
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
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
    
    public boolean isBurstAvailable() {
        return remainingBurstTicks > 0;
    }
    
    public boolean isBurstThrottled() {
        return remainingBurstTicks < 0;
    }
    
    public float getBurstBonus() {
        if (isBurstAvailable()) {
            return 1 / Oritech.CONFIG.addonConfig.burstAddonSpeedMultiplier();
        } else if(isBurstThrottled()) {
            return Oritech.CONFIG.addonConfig.burstAddonThrottleMultiplier();
        } else {
            return 1f;
        }
    }
    
    // values smaller than 1 are faster, higher than 1 are slower
    @Override
    public float getSpeedMultiplier() {
        return getBaseAddonData().speed() * getBurstBonus();
    }
    
    // same here
    @Override
    public float getEfficiencyMultiplier() {
        return getBaseAddonData().efficiency() * getBurstBonus();
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
