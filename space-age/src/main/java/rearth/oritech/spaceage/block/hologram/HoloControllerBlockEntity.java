package rearth.oritech.spaceage.block.hologram;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import rearth.oritech.spaceage.block.mission_control.GroundStationBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.HoloNetworking.Projection;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionController;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Craft;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Point;

import java.util.ArrayList;
import java.util.List;

public final class HoloControllerBlockEntity extends BlockEntity implements MenuProvider {

    public HoloSettings settings = HoloSettings.DEFAULT;

    public HoloControllerBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.HOLO_CONTROLLER.get(), pos, state);
    }

    public GroundStationBlockEntity station() {

        if (level == null) return null;
        for (var direction : Direction.values()) {
            var pos = worldPosition.relative(direction);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(SpaceAgeBlocks.MISSION_CONTROL)
                    && level.getBlockEntity(pos) instanceof GroundStationBlockEntity station) return station;
        }
        return null;
    }

    public void tick() {

        if (!(level instanceof ServerLevel server) || (level.getGameTime() + worldPosition.asLong()) % 20 != 0) return;
        var viewers = server.players().stream().filter(p -> p.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 128 * 128).toList();
        if (viewers.isEmpty()) return;
        var snapshot = buildSnapshot();
        for (var player : viewers) PacketDistributor.sendToPlayer(player, new Projection(worldPosition, snapshot));
    }

    public HoloSnapshot buildSnapshot() {

        var station = station();
        var surface = HoloSurface.scan(level, worldPosition);
        var facing = (int) getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
        if (station == null || !(level instanceof ServerLevel server))
            return new HoloSnapshot(settings, surface, List.of(), List.of(), HoloSnapshot.EMPTY.categories(), 0, 1, facing, "no_station");
        var system = SpaceSimulationSavedData.get(server.getServer()).getOrCreate(station.owner);
        var missions = MissionSavedData.get(server.getServer());
        var tick = MissionController.missionTime(server.getServer());
        var crafts = new ArrayList<Craft>();
        for (var mission : missions.craft.values()) {
            if (!mission.owner.equals(station.owner)) continue;
            var pos = mission.position;
            var id = mission.rocket.getRocketId();
            var category = system.fleetCategories.category(id, pos.target().equals(SpaceSimulation.SUN_ID)
                    && pos.orbit() == SpaceSimulation.OrbitBand.SOLAR, mission.plan.category());
            var path = new ArrayList<Point>();
            // Read already compiled guidance; displaying a projector must never solve a transfer.
            if (mission.transfer != null && mission.transfer.path != null) {
                var root = mission.transfer.path.paths().stream().filter(p -> p.branchId().equals(mission.plan.root().id())).findFirst();
                if (root.isPresent()) {
                    var samples = root.get().samples();
                    var step = Math.max(1, (samples.size() + 126) / 127);
                    for (int i = 0; i < samples.size(); i += step) path.add(new Point(samples.get(i).x(), samples.get(i).y()));
                    if (!samples.isEmpty()) path.add(new Point(samples.getLast().x(), samples.getLast().y()));
                }
            }
            crafts.add(new Craft(id, mission.plan.name().isBlank() ? id.toString().substring(0, 8) : mission.plan.name(),
                    category, pos.x(), pos.y(), pos.vx(), pos.vy(), pos.target(), mission.canUpdateMission(), mission.status, List.copyOf(path)));
        }
        var status = surface.width() == 0 ? "no_screen" : !settings.mode().equals("area")
                && crafts.stream().noneMatch(c -> HoloView.selected(settings, c)) ? "empty" : "ready";
        return new HoloSnapshot(settings, surface, system.createObjectData(), List.copyOf(crafts), system.fleetCategories.saved(),
                tick, missions.debugSpeed, facing, status);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        output.store("projection", HoloSettings.CODEC, settings);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        settings = input.read("projection", HoloSettings.CODEC).orElse(HoloSettings.DEFAULT).normalized();
    }

    @Override
    public Component getDisplayName() {

        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {

        return new HoloControllerMenu(id, inventory, worldPosition);
    }
}
