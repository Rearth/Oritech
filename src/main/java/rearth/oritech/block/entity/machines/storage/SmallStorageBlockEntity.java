package rearth.oritech.block.entity.machines.storage;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
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
        return 64000;
    }
    
    @Override
    public long getDefaultInsertRate() {
        return 1000;
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return 1000;
    }
}
