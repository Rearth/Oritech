package rearth.oritech.block.entity.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.EnergyHistory;
import rearth.oritech.block.base.entity.ExpandableMultiblockEnergyStorageBlockEntity;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.List;

public class LargeStorageBlockEntity extends ExpandableMultiblockEnergyStorageBlockEntity implements RedstoneAddonBlockEntity.RedstoneControllable {

    @SyncField({SyncType.GUI_OPEN, SyncType.GUI_TICK})
    public final EnergyHistory energyHistory = new EnergyHistory();

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state, NetworkedBlockEntity entity) {
        super.serverTick(level, pos, state, entity);
        // The base tick advances the 20-tick statistics first; keep recording independently of GUI sync.
        if (level.getGameTime() % EnergyHistory.SAMPLE_TICKS == 0) {
            var stats = energyStorage.getCurrentStatistics(level.getGameTime());
            energyHistory.record(level.getGameTime(), energyStorage.energy, stats.avgInsertSecond(), stats.avgExtractSecond());
        }
    }

    public LargeStorageBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.LARGE_STORAGE.get(), pos, state);
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
                new Vec3i(0, 0, -1),
                new Vec3i(0, 0, 1),
                new Vec3i(0, 1, -1),
                new Vec3i(0, 1, 1),
                new Vec3i(1, 0, -1),
                new Vec3i(1, 0, 1),
                new Vec3i(1, 1, -1),
                new Vec3i(1, 1, 1)
        );
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.largeEnergyStorage.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.largeEnergyStorage.maxEnergyInsertion.get();
    }

    @Override
    public long getDefaultExtractionRate() {
        return OritechConfig.largeEnergyStorage.maxEnergyExtraction.get();
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 1, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(1, 1, 0)
        );
    }

    @Override
    public int getComparatorEnergyAmount() {
        if (energyStorage.energy == 0) return 0;
        return (int) (1 + ((energyStorage.energy / (float) energyStorage.capacity) * 14));
    }

    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.getStacks().size() <= slot) return 0;

        var stack = inventory.getResource(slot);
        if (stack.isEmpty()) return 0;

        return (int) (1 + (inventory.getAmountAsInt(slot) / (float) stack.getMaxStackSize()) * 15);
    }

    @Override
    public int getComparatorProgress() {
        return 0;
    }

    @Override
    public int getComparatorActiveState() {
        return 15;
    }

    @Override
    public void onRedstoneEvent(boolean isPowered) {
        this.setRedstonePowered(isPowered);
    }

}
