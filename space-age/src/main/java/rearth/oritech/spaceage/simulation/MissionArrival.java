package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.OrbitBand;

import java.util.HashSet;
import java.util.UUID;

/** Owns orbit placement, reentry, recovery and impacts once guidance reaches its destination. */
final class MissionArrival {

    private MissionArrival() {
    }

    static void tick(MinecraftServer server, MissionSavedData data, MissionState state, SpaceSimulation system,
                     FlightPlanAction action, CraftPath path, double after) {

        var earthLanding = action.targetId().equals(SpaceObjects.EARTH_ID) && action.orbit() == OrbitBand.SURFACE
                && state.transfer.path.navigationAborts().stream().noneMatch(abort -> abort.actionId().equals(action.id()));
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
        var p = state.position;
        var slot = -1;
        if (action.targetId().equals(SpaceObjects.EARTH_ID) && SpaceBalance.hasSlots(action.orbit())
                && Math.hypot(p.vx(), p.vy()) <= 12) {
            slot = action.service().slot();
            if (slot < 0) {
                var occupied = new HashSet<Integer>();
                data.craft.values().stream().filter(c -> c.owner.equals(state.owner) && c.position.orbit() == action.orbit()).forEach(c -> occupied.add(c.position.slot()));
                slot = 0;
                while (occupied.contains(slot) && slot < SpaceBalance.slots(action.orbit()) - 1) slot++;
            }
            slot %= SpaceBalance.slots(action.orbit());
        }
        var px = p.x();
        var py = p.y();
        if (slot >= 0) {
            var angle = Math.PI + slot * Math.PI * 2 / SpaceBalance.slots(action.orbit());
            px = -3_000_000 + Math.cos(angle) * (60_000 + action.orbit().altitude());
            py = Math.sin(angle) * (60_000 + action.orbit().altitude());
        }
        state.position = new MissionState.Position(px, py, p.vx(), p.vy(), action.targetId(), action.orbit(), slot, p.stage(), p.asteroid(), p.anchor(),
                p.headingX(), p.headingY());
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
            state.transfer.landing = RocketRecovery.landingPosition(server.overworld(), state.rocket,
                    action.landingX() + action.landingOffsetX(), action.landingZ() + action.landingOffsetZ());
            if (path.terminalState() == RocketFlightPathCalculator.TerminalState.DESTROYED || Math.hypot(p.vx(), p.vy()) > 12) {
                var level = server.overworld();
                var pos = state.transfer.landing;
                var energy = state.transfer.path.arrivalPredictions().stream().filter(a -> a.targetId().equals(SpaceObjects.EARTH_ID))
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
                var created = system.applyAsteroidImpact(arrival.targetId(), arrival.impact());
                if (created != null) debrisRegion = created;
            }
            if (debrisRegion != null) {
                var position = state.position;
                state.position = new MissionState.Position(position.x(), position.y(), position.vx(), position.vy(),
                        debrisRegion, OrbitBand.SURFACE, -1, position.stage(), position.asteroid(), position.anchor(),
                        position.headingX(), position.headingY());
            }
        }
        if (earthLanding && state.ended && !p.asteroid().equals(FlightPlanAction.NO_TARGET)) {
            state.transfer.path.arrivalPredictions().stream().filter(a -> a.targetId().equals(SpaceObjects.EARTH_ID)).findFirst()
                    .ifPresent(arrival -> system.recoverAsteroid(server.overworld(), p.asteroid(), arrival.impact(), false));
        }
        state.complete();
    }

}
