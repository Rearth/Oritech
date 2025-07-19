package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;

public abstract class ExpandableMultiblockEnergyStorageBlockEntity extends ExpandableEnergyStorageBlockEntity implements MultiblockMachineController {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    private float coreQuality = 1f;
    
    public ExpandableMultiblockEnergyStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        addMultiblockToNbt(nbt);
    }
    
    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
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
        return pos;
    }
    
    @Override
    public World getWorldForMultiblock() {
        return world;
    }
    
    @Override
    public void triggerSetupAnimation() {
    
    }
}