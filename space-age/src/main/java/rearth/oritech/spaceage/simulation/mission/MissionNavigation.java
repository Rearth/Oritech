package rearth.oritech.spaceage.simulation.mission;

import net.minecraft.server.MinecraftServer;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.flight.FlightMotion;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compiles and plays one navigation card; arrival effects belong to MissionArrival. */
final class MissionNavigation {

    private MissionNavigation() {
    }

    static void tick(MinecraftServer server, MissionSavedData data, MissionState state, SpaceSimulation system, FlightPlanAction action, long tick) {

        var rendezvous = action.settings().docking();
        if (action.targetId().equals(rendezvous.host())) {
            var issue = RocketDocking.rendezvousIssue(data, state, rendezvous);
            if (!issue.isEmpty()) {
                state.status = "status.oritech_space_age." + issue;
                return;
            }
        }
        if (state.transfer == null) {
            state.positionRevision++;
            state.resetAction();
            state.transfer = new ActiveTransfer(new MissionState.Telemetry(MissionState.copyRocket(state.rocket),
                    state.plan, state.position, tick, MissionState.STATUS_TRANSFER), system.knownObjects(state.knowledge),
                    RocketDocking.targets(data, state.owner));
            var p = state.position;
            state.position = p.placed(p.x(), p.y(), p.vx(), p.vy(), p.target(), p.orbit(), -1);
        }
        if (state.transfer.path == null) {
            var frozenPlan = state.transfer.start.plan();
            var root = frozenPlan.root().withActions(List.of(action));
            var branches = new ArrayList<>(frozenPlan.branches());
            branches.removeIf(FlightPlanBranch::isRoot);
            branches.addFirst(root);
            state.transfer.path = RocketFlightPathCalculator.calculateFrom(state.transfer.start.rocket(), state.transfer.objects,
                    frozenPlan.withBranches(branches), state.transfer.start.position(), VacuumRecipe.all(server), state.transfer.stations);
        }
        var path = state.transfer.path.paths().stream().filter(p -> p.branchId().equals(state.plan.root().id())).findFirst().orElse(null);
        if (path == null || path.samples().isEmpty()) {
            state.status = MissionState.STATUS_NO_FEASIBLE_TRANSFER;
            return;
        }
        var before = state.actionTicks() / 20.0;
        var after = Math.min(path.durationSeconds(), before + 0.05);
        var samples = path.samples();
        for (int index = 1; index < samples.size(); index++) {
            var previous = samples.get(index - 1);
            var next = samples.get(index);
            var duration = Math.max(0, Math.min(after, next.timeSeconds()) - Math.max(before, previous.timeSeconds()));
            if (duration <= 0) continue;
            var insufficient = state.rocket.getStaticSegments().entrySet().stream().anyMatch(entry -> {
                var rate = next.resourceRates().get(SegmentRef.of(entry.getValue()));
                var resources = state.rocket.getDynamicSegments().get(entry.getKey());
                return rate != null && (resources.fuel() + 1e-6 < rate.fuel() * duration
                        || resources.rf() + 1e-6 < rate.rf() * duration);
            });
            if (insufficient) {
                state.resetAction();
                state.status = MissionState.STATUS_REPLANNING;
                return;
            }
            state.rocket.getStaticSegments().forEach((id, segment) -> {
                var rate = next.resourceRates().get(SegmentRef.of(segment));
                if (rate != null) rate.apply(state.rocket.getDynamicSegments().get(id), duration);
            });
        }
        state.position = FlightMotion.position(samples, after, state.position);
        state.advanceAction();
        state.status = MissionState.STATUS_IN_FLIGHT;
        for (var event : state.transfer.path.boosterEvents()) {
            if (!event.branchId().equals(state.plan.root().id()) || event.timeSeconds() > after) continue;
            var id = state.rocket.getStaticSegments().entrySet().stream().filter(e -> SegmentRef.of(e.getValue()).equals(event.segment())).map(Map.Entry::getKey).findFirst().orElse(null);
            if (id != null) MissionController.split(data, state, Set.of(id), event.id(), tick);
        }
        MissionArrival.tick(server, data, state, system, action, path, after);
    }
}
