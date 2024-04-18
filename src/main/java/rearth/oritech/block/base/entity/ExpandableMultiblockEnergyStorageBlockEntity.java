package rearth.oritech.block.base.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.MultiblockMachineController;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;

public abstract class ExpandableMultiblockEnergyStorageBlockEntity extends ExpandableEnergyStorageBlockEntity implements MultiblockMachineController {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    private float coreQuality = 1f;
    
    public ExpandableMultiblockEnergyStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        addMultiblockToNbt(nbt);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
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
    public InventoryProvider getInventoryForLink() {
        return this;
    }
    
    @Override
    public EnergyStorage getEnergyStorageForLink() {
        return energyStorage;
    }
    
    @Override
    public void playSetupAnimation() {
        // this block has no animation
    }
    
}