package rearth.oritech.block.entity.machines.storage;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.List;

public class SmallStorageBlockEntity extends ExpandableEnergyStorageBlockEntity {
    
    public SmallStorageBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.SMALL_STORAGE_ENTITY, pos, state);
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, 0,-1),
          new Vec3i(0, 0,1)
        );
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.smallEnergyStorage.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.smallEnergyStorage.maxEnergyInsertion();
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return Oritech.CONFIG.smallEnergyStorage.maxEnergyExtraction();
    }

    public int getComparatorOutput() {
        if (energyStorage.amount == 0) return 0;
        return (int) (1 + ((energyStorage.amount / (float) energyStorage.capacity) * 14));
    }
    
    @Override
    public float getCoreQuality() {
        return 3;
    }
}
