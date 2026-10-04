package rearth.oritech.spaceage.block;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.spaceage.init.*;
import rearth.oritech.spaceage.simulation.*;
import java.util.*;

/** Ground modules draw from adjacent Oritech batteries; no duplicate power-storage implementation. */
public class GroundStationBlockEntity extends BlockEntity implements MenuProvider {
    public UUID owner = new UUID(0, 0);
    public int poweredAntennas;
    private final List<BlockPos> antennas = new ArrayList<>();
    public GroundStationBlockEntity(BlockPos pos, BlockState state) { super(SpaceAgeBlockEntities.GROUND_STATION.get(), pos, state); }
    @Override protected void saveAdditional(ValueOutput output) { super.saveAdditional(output); output.store("owner", UUIDUtil.CODEC, owner); }
    @Override protected void loadAdditional(ValueInput input) { super.loadAdditional(input); owner = input.read("owner", UUIDUtil.CODEC).orElse(new UUID(0, 0)); }
    public void tick() {
        if (!(level instanceof ServerLevel server) || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        if (getBlockState().is(SpaceAgeBlocks.SPACE_SCANNER)) {
            var data = SpaceSimulationSavedData.get(server.getServer());
            var system = data.getOrCreate(owner);
            boolean undiscovered = system.truth().stream().anyMatch(object -> object.type() == SpaceObjects.ObjectType.ASTEROID
                    && Math.hypot(object.x() + 3_060_000, object.y()) <= 180_000
                    && system.earthKnowledge.contact(object.id()) == null);
            if (undiscovered && draw(worldPosition, SpaceBalance.SCAN_RF)) {
                system.earthKnowledge.scan(system.truth(), MissionController.missionTime(level.getServer()), -3_060_000, 0, 180_000);
                data.setDirty();
            }
        }
        if (!getBlockState().is(SpaceAgeBlocks.MISSION_CONTROL)) return;
        if (level.getGameTime() % 20 == 0) findAntennas();
        poweredAntennas = 0;
        for (var pos : antennas) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(SpaceAgeBlocks.ANTENNA)
                    && draw(pos, SpaceBalance.ANTENNA_RF)) poweredAntennas++;
        }
        MissionSavedData.get(server.getServer()).stations.put(GlobalPos.of(level.dimension(), worldPosition), this);
    }
    private void findAntennas() {
        antennas.clear();
        var open = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        open.add(worldPosition);
        while (!open.isEmpty() && visited.size() < 256) {
            var pos = open.removeFirst();
            if (!visited.add(pos) || !level.hasChunkAt(pos)) continue;
            if (!pos.equals(worldPosition) && !level.getBlockState(pos).is(SpaceAgeBlocks.ANTENNA)) continue;
            if (!pos.equals(worldPosition)) antennas.add(pos);
            for (var direction : Direction.values()) open.add(pos.relative(direction));
        }
    }

    private boolean draw(BlockPos pos, long amount) {
        for (var direction : Direction.values()) {
            var other = pos.relative(direction);
            if (!level.hasChunkAt(other)) continue;
            var storage = level.getCapability(Capabilities.Energy.BLOCK, other, direction.getOpposite());
            if (storage == null) continue;
            try (var transaction = Transaction.openRoot()) {
                if (storage.extract((int) amount, transaction) == amount) { transaction.commit(); return true; }
            }
        }
        return false;
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MissionControlMenu(id, inventory, worldPosition);
    }
}
