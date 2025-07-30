package rearth.oritech.block.entity.storage;

import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ComparatorOutputProvider;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeStorageBlockEntity extends ExpandableEnergyStorageBlockEntity implements ComparatorOutputProvider {

    public CreativeStorageBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.CREATIVE_STORAGE_ENTITY, pos, state);
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of();
    }

    @Override
    public long getDefaultCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getDefaultInsertRate() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getDefaultExtractionRate() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getComparatorOutput() {
        if (energyStorage.amount == 0) return 0;
        return (int) (1 + ((energyStorage.amount / (float) energyStorage.capacity) * 14));
    }

    @Override
    public float getCoreQuality() {
        return 0;
    }

    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        energyStorage.amount = (long) (Integer.MAX_VALUE * 0.9f);
        super.serverTick(world, pos, state, blockEntity);
    }
}
