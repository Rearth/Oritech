package rearth.oritech.spaceage.simulation.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import rearth.oritech.spaceage.recipe.RocketPayloadRecipe;
import rearth.oritech.spaceage.recipe.RocketPayloadRecipe.Input;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.mission.MissionSavedData;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.service.RocketProcessingService;

import java.util.List;

/**
 * Destruction effects execute only in live missions, never in the flight planner.
 */
public final class RocketDetonation {

    private RocketDetonation() {
    }

    public static void applyPayload(MinecraftServer server, ActiveRocketData rocket, double earthAltitude) {

        RocketPayloadRecipe.select(RocketPayloadRecipe.all(server), Input.of(rocket), earthAltitude)
                .ifPresent(holder -> holder.value().effect().apply(server, holder.value().durationTicks()));
    }

    public static void explode(MinecraftServer server, MissionSavedData data, MissionState state, SpaceSimulation system) {

        if (state.ended) return;
        var p = state.position;
        var altitude = SpaceObjects.earthAltitude(p.x(), p.y());
        var energy = RocketExplosives.remaining(state.rocket);
        RocketProcessingService.cancel(data, state.rocket.getRocketId());
        if (server != null) {
            applyPayload(server, state.rocket, altitude);
            var level = server.overworld();
            var descent = state.completed.reversed().stream().filter(action -> action.type() == ActionType.NAVIGATE_TO).findFirst()
                    .filter(action -> action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == OrbitBand.SURFACE).orElse(null);
            var x = descent == null ? state.launch.getX() : descent.landingX();
            var z = descent == null ? state.launch.getZ() : descent.landingZ();
            var column = new BlockPos(x, 0, z);
            if (altitude <= SpaceBalance.ATMOSPHERE_HEIGHT && level.hasChunkAt(column)) {
                var flight = state.rocket.getFlight();
                var position = flight != null && !flight.isInSpace(level.getGameTime())
                        ? BlockPos.containing(RocketSimulationController.getRocketPosition(flight, level.getGameTime()))
                        : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column).above((int) Math.round(
                                altitude * SpaceBalance.LAUNCH_ORBIT_HEIGHT_BLOCKS / SpaceBalance.ATMOSPHERE_HEIGHT));
                level.explode(null, position.getX() + .5, position.getY(), position.getZ() + .5,
                        Math.max(1, RocketExplosives.worldStrength(energy)), Level.ExplosionInteraction.BLOCK);
            }
            RocketSimulationController.discard(server, List.of(state.rocket.getRocketId()));
        }
        if (!p.asteroid().equals(FlightPlanAction.NO_TARGET)) {
            system.releaseAsteroid(p.asteroid(), p.x(), p.y(), p.vx(), p.vy(), null);
            state.position = p.withAttachment(FlightPlanAction.NO_TARGET, p.anchor());
        }
        state.ended = true;
        state.status = MissionState.STATUS_EXPLODED;
        state.complete();
        data.power.changed();
        data.setDirty();
    }
}
