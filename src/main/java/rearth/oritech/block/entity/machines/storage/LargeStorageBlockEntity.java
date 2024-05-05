package rearth.oritech.block.entity.machines.storage;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.ExpandableMultiblockEnergyStorageBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.List;

public class LargeStorageBlockEntity extends ExpandableMultiblockEnergyStorageBlockEntity {
    
    public LargeStorageBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LARGE_STORAGE_ENTITY, pos, state);
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
        return Oritech.CONFIG.largeEnergyStorage.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.largeEnergyStorage.maxEnergyInsertion();
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return Oritech.CONFIG.largeEnergyStorage.maxEnergyExtraction();
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1,0),
          new Vec3i(1, 0,0),
          new Vec3i(1, 1,0)
        );
    }
    
}
