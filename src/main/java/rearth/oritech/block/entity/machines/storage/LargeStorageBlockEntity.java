package rearth.oritech.block.entity.machines.storage;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.InventoryProvider;
import rearth.oritech.util.MultiblockMachineController;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LargeStorageBlockEntity extends ExpandableEnergyStorageBlockEntity implements MultiblockMachineController {
    
    private final ArrayList<BlockPos> coreBlocksConnected = new ArrayList<>();
    
    private float coreQuality = 1f;
    public LargeStorageBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LARGE_STORAGE_ENTITY, pos, state);
    }
    
    @Override
    protected HashMap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        return super.getNeighborCaches(pos, world);
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
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, 0,-1),
          new Vec3i(0, 0,1),
          new Vec3i(0, 1,-1),
          new Vec3i(0, 1,1),
          new Vec3i(1, 0,-1),
          new Vec3i(1, 0,1),
          new Vec3i(1, 1,-1),
          new Vec3i(1, 1,1)
        );
    }
    
    @Override
    public long getDefaultCapacity() {
        return 500000;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 5000;
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return 5000;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1,0),
          new Vec3i(1, 0,0),
          new Vec3i(1, 1,0)
        );
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
        return null;
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
