package rearth.oritech.block.base.entity;

import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ExpandableMultiblockEnergyStorageBlockEntity extends ExpandableEnergyStorageBlockEntity implements MultiblockMachineController {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    @SyncField(SyncType.GUI_OPEN)
    private float coreQuality = 1f;
    
    public ExpandableMultiblockEnergyStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        addMultiblockToNbt(nbt);
    }
    
    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        loadMultiblockNbtData(nbt);
    }
    
    @Override
    public Direction getFacingForMultiblock() {
        return super.getFacingForAddon();
    }
    
    @Override
    public ArrayList<BlockPos> getConnectedCores() {
        return coreBlocksConnected;
    }
    
    @Override
    public void setCoreQuality(float quality) {
        this.coreQuality = quality;
    }
    
    @Override
    public float getCoreQuality() {
        return this.coreQuality;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryForMultiblock() {
        return inventory;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public BlockPos getPosForMultiblock() {
        return worldPosition;
    }
    
    @Override
    public Level getWorldForMultiblock() {
        return level;
    }
    
    @Override
    public void triggerSetupAnimation() {
    
    }
}