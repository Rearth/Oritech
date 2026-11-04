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
import rearth.oritech.spaceage.simulation.mission.MissionJourney;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot.Craft;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
            var path = new ArrayList<PathSample>();
            // Active guidance is authoritative; full-journey forecasts use the shared per-mission cache.
            if (mission.transfer != null && mission.transfer.path != null) {
                var root = mission.transfer.path.paths().stream().filter(p -> p.branchId().equals(mission.plan.root().id())).findFirst();
                if (root.isPresent()) {
                    var samples = root.get().samples();
                    // Preserve every Hermite knot; no rocket structure, inventory or resource accounting is transmitted.
                    for (var sample : samples) path.add(new PathSample(sample.timeSeconds(), sample.x(), sample.y(), sample.speedMetersPerSecond(),
                            sample.velocityX(), sample.velocityY(), sample.phase(), sample.targetId(), sample.actionId(), sample.stage(),
                            Set.of(), Set.of(), FlightPlanAction.NO_TARGET));
                }
            }
            var destination = destination(mission, path);
            var craft = new Craft(id, mission.plan.name().isBlank() ? id.toString().substring(0, 8) : mission.plan.name(),
                    category, pos.x(), pos.y(), pos.vx(), pos.vy(), destination, mission.canUpdateMission(), mission.status, List.copyOf(path));
            var journey = new ArrayList<>(mission.journeyHistory);
            if (HoloView.selected(settings, craft) && (settings.shows(HoloSettings.PATHS) || settings.framing().equals("journey"))) {
                var forecast = mission.projectedJourney(system, missions, VacuumRecipe.all(server.getServer()));
                forecast.paths().stream().filter(p -> p.branchId().equals(mission.plan.root().id())).findFirst()
                        .ifPresent(p -> journey.addAll(MissionJourney.legs(p)));
            }
            crafts.add(new Craft(id, craft.name(), category, pos.x(), pos.y(), pos.vx(), pos.vy(), destination, craft.connected(), mission.status,
                    List.copyOf(path), List.copyOf(journey), mission.actionTicks() / 20d));
        }
        var status = surface.width() == 0 ? "no_screen" : !settings.mode().equals("area")
                && crafts.stream().noneMatch(c -> HoloView.selected(settings, c)) ? "empty" : "ready";
        return new HoloSnapshot(settings, surface, system.createObjectData(), List.copyOf(crafts), system.fleetCategories.saved(),
                tick, missions.debugSpeed, facing, status);
    }

    static UUID destination(MissionState mission, List<PathSample> path) {

        var action = mission.action();
        if (action == null || action.type() != ActionType.NAVIGATE_TO) return mission.position.target();
        if (!path.isEmpty() && !path.getLast().targetId().equals(FlightPlanAction.NO_TARGET)) return path.getLast().targetId();
        return action.targetId().equals(FlightPlanAction.CLOSEST_ASTEROID) ? mission.position.target() : action.targetId();
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
