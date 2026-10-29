package rearth.oritech.spaceage.simulation.mission;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.power.SolarPower;
import rearth.oritech.spaceage.simulation.rocket.RocketExplosives;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;
import rearth.oritech.spaceage.simulation.rocket.RocketRecovery;
import rearth.oritech.spaceage.simulation.rocket.RocketSimulationController;

import java.util.HashSet;
import java.util.UUID;

/**
 * Owns orbit placement, reentry, recovery and impacts once guidance reaches its destination.
 */
final class MissionArrival {

    private MissionArrival() {
    }

    static void tick(MinecraftServer server, MissionSavedData data, MissionState state, SpaceSimulation system,
                     FlightPlanAction action, CraftPath path, double after) {

        var aborted = state.transfer.path.navigationAborts().stream().anyMatch(abort -> abort.actionId().equals(action.id()));
        var earthLanding = action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == OrbitBand.SURFACE && !aborted;
        if (earthLanding && server != null && state.transfer.landing == null && path.durationSeconds() - after <= 10) {
            var level = server.overworld();
            if (level.hasChunkAt(new BlockPos(action.landingX(), 0, action.landingZ()))) {
                state.transfer.landing = RocketRecovery.landingPosition(level, state.rocket,
                        action.landingX() + action.landingOffsetX(), action.landingZ() + action.landingOffsetZ());
                var last = path.samples().getLast();
                RocketSimulationController.beginReentry(level, state.rocket, state.transfer.landing,
                        Math.max(1, (long) ((path.durationSeconds() - after) * 20)), last.speedMetersPerSecond());
            }
        }
        if (after + 1e-7 < path.durationSeconds()) return;
        var completed = !path.actionMoments().isEmpty() && path.actionMoments().getLast().completed();
        if (!completed) {
            state.status = MissionState.STATUS_TRANSFER_BLOCKED;
            return;
        }
        if (aborted) {
            // Completing a condition ends guidance where it stopped, without claiming the destination.
            state.complete();
            state.status = MissionState.STATUS_READY;
            return;
        }
        var p = state.position;
        var slot = -1;
        if (SpaceBalance.hasSlots(action.targetId(), action.orbit())
                && Math.hypot(p.vx(), p.vy()) <= 12) {
            var occupied = new HashSet<Integer>();
            data.craft.values().stream().filter(c -> c != state && !c.ended && c.owner.equals(state.owner)
                            && c.position.target().equals(action.targetId()) && c.position.orbit() == action.orbit())
                    .forEach(c -> occupied.add(c.position.slot()));
            slot = SpaceBalance.freeSlot(action.service().slot(), occupied);
            if (slot >= SpaceBalance.slots(action.targetId(), action.orbit())) {
                state.status = MissionState.STATUS_ORBIT_FULL;
                return;
            }
        }
        var targetCraft = data.craft.get(action.targetId());
        if ((action.targetId().equals(SpaceSimulation.SUN_ID) || targetCraft != null
                && SolarPower.sunRegion(targetCraft.position)) && SolarPower.output(state) <= 0) {
            state.status = "status.oritech_space_age.solar_required";
            return;
        }
        var px = p.x();
        var py = p.y();
        if (slot >= 0) {
            var angle = Math.PI + slot * Math.PI * 2 / SpaceBalance.slots(action.targetId(), action.orbit());
            var body = system.truth().stream().filter(o -> o.id().equals(action.targetId())).findFirst().orElseThrow();
            px = body.x() + Math.cos(angle) * (body.radius() + action.orbit().altitude());
            py = body.y() + Math.sin(angle) * (body.radius() + action.orbit().altitude());
        }
        state.position = p.placed(px, py, p.vx(), p.vy(), action.targetId(), action.orbit(), slot);
        if (earthLanding) {
            var visual = state.rocket.getFlight();
            if (server != null && state.transfer.landing != null && visual != null
                    && server.overworld().getGameTime() < visual.impactTick()) {
                state.status = MissionState.STATUS_DESCENDING;
                return;
            }
            if (state.transfer.landing == null) {
                state.status = MissionState.STATUS_LANDING_UNLOADED;
                return;
            }
            // Terrain may have changed since reentry began.
            state.transfer.landing = RocketRecovery.landingPosition(server.overworld(), state.rocket,
                    action.landingX() + action.landingOffsetX(), action.landingZ() + action.landingOffsetZ());
            if (path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED || Math.hypot(p.vx(), p.vy()) > 12) {
                var level = server.overworld();
                var pos = state.transfer.landing;
                var energy = state.transfer.path.arrivalPredictions().stream().filter(a -> a.actionId().equals(action.id()))
                        .mapToDouble(a -> a.impact().totalEnergyJoules()).findFirst().orElse(
                                .5 * RocketPerformanceCalculator.calculate(state.rocket).wetMassKilograms() * (p.vx() * p.vx() + p.vy() * p.vy())
                                        + RocketExplosives.remaining(state.rocket));
                level.explode(null, pos.getX(), pos.getY(), pos.getZ(), RocketExplosives.worldStrength(energy), Level.ExplosionInteraction.BLOCK);
                state.ended = true;
                state.status = MissionState.STATUS_DESTROYED;
            } else if (RocketRecovery.restore(server.overworld(), state.rocket, state.transfer.landing)) {
                MissionController.deliverSurveyKnowledge(server, state, system);
                state.ended = true;
                state.status = MissionState.STATUS_RECOVERED;
            } else {
                state.status = MissionState.STATUS_LANDING_UNAVAILABLE;
                return;
            }
        }
        if (path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED && !earthLanding) {
            state.ended = true;
            state.status = MissionState.STATUS_DESTROYED;
            UUID debrisRegion = null;
            for (var arrival : state.transfer.path.arrivalPredictions()) {
                if (!arrival.actionId().equals(action.id())) continue;
                var created = system.applyAsteroidImpact(arrival.targetId(), arrival.impact());
                if (created != null) debrisRegion = created;
            }
            if (debrisRegion != null) {
                var position = state.position;
                state.position = position.placed(position.x(), position.y(), position.vx(), position.vy(), debrisRegion, OrbitBand.SURFACE, -1);
            }
        }
        if (earthLanding && state.ended && !p.asteroid().equals(FlightPlanAction.NO_TARGET)) {
            state.transfer.path.arrivalPredictions().stream().filter(a -> a.actionId().equals(action.id())).findFirst()
                    .ifPresent(arrival -> system.recoverAsteroid(server.overworld(), p.asteroid(), arrival.impact(),
                            state.status.equals(MissionState.STATUS_RECOVERED)
                                    ? RocketRecovery.highestBlockY(state.rocket, state.transfer.landing) : null));
        }
        state.complete();
    }

}
