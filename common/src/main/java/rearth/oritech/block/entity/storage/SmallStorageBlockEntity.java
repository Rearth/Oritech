package rearth.oritech.block.entity.storage;

import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ComparatorOutputProvider;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

public class SmallStorageBlockEntity extends ExpandableEnergyStorageBlockEntity implements ComparatorOutputProvider {
    
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

    @Override
    public int getComparatorOutput() {
        if (energyStorage.amount == 0) return 0;
        return (int) (1 + ((energyStorage.amount / (float) energyStorage.capacity) * 14));
    }
    
    @Override
    public float getCoreQuality() {
        return 3;
    }
}
