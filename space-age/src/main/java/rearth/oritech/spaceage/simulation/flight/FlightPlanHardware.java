package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.core.BlockPos;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketPayloads;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Resolves the hardware attached to a branch immediately before an instruction. */
public final class FlightPlanHardware {

    private FlightPlanHardware() {
    }

    public static ActiveRocketData before(FlightPlan plan, FlightPlanBranch branch, UUID actionId,
                                          ActiveRocketData rocket, RocketFlightPathCalculator.FlightPath forecast) {

        var view = RocketPayloads.planView(rocket);
        var connected = branchSegments(plan, branch, view, new HashSet<>());
        var path = forecast == null ? null : forecast.paths().stream().filter(p -> p.branchId().equals(branch.id())).findFirst().orElse(null);
        if (path != null && !path.samples().isEmpty()) connected = path.samples().getFirst().connectedSegments();
        var deployed = new HashSet<BlockPos>();
        for (var action : branch.actions()) {
            if (action.id().equals(actionId)) break;
            var moment = path == null ? null : path.actionMoments().stream().filter(m -> m.actionId().equals(action.id())).findFirst().orElse(null);
            connected = moment == null ? retainedAfter(action, connected, view) : moment.connectedSegments();
            if (action.type() == ActionType.DEPLOY_CUBESAT) deployed.add(action.payload());
        }
        var structures = new HashMap<UUID, StaticRocketSegment>();
        var resources = new HashMap<UUID, DynamicRocketSegment>();
        for (var entry : view.getOriginalSegments().entrySet()) {
            if (!connected.contains(SegmentRef.of(entry.getValue()))) continue;
            structures.put(entry.getKey(), entry.getValue());
            var dynamic = view.getDynamicSegments().get(entry.getKey()).copy();
            deployed.forEach(dynamic.payloads::remove);
            resources.put(entry.getKey(), dynamic);
        }
        resources.values().forEach(r -> r.getConnectedSegments().retainAll(structures.keySet()));
        var result = new ActiveRocketData(branch.isRoot() ? rocket.getRocketId() : branch.id(), structures, resources, null);
        result.copyLaunchFrom(rocket);
        return result;
    }

    static Set<SegmentRef> branchSegments(FlightPlan plan, FlightPlanBranch branch, ActiveRocketData rocket,
                                                  Set<UUID> visited) {

        if (branch.isRoot()) {
            var payloads = RocketPayloads.segments(rocket);
            return rocket.getStaticSegments().values().stream().map(SegmentRef::of)
                    .filter(ref -> !payloads.contains(ref)).collect(Collectors.toSet());
        }
        if (!visited.add(branch.id())) return Set.of();
        for (var parent : plan.branches())
            for (var action : parent.actions()) {
                if (!action.id().equals(branch.parentSeparationAction())) continue;
                var connected = new HashSet<>(branchSegments(plan, parent, rocket, visited));
                for (var prior : parent.actions()) {
                    if (prior.id().equals(action.id())) break;
                    connected = new HashSet<>(retainedAfter(prior, connected, rocket));
                }
                if (action.type() == ActionType.DECOUPLE && action.segments().size() == 2)
                    return component(action.segments().get(1), connected, rocket, action.segments());
                if (action.type() == ActionType.DEPLOY_CUBESAT) {
                    var cube = RocketPayloads.all(rocket).get(action.payload());
                    return cube == null ? Set.of() : RocketPayloads.segments(action.payload(), cube);
                }
                if (action.isGenerated() && !action.segments().isEmpty()) return Set.of(action.segments().getFirst());
            }
        return Set.of();
    }

    static Set<SegmentRef> retainedAfter(FlightPlanAction action, Set<SegmentRef> connected, ActiveRocketData rocket) {

        if (action.type() != ActionType.DECOUPLE || action.segments().size() != 2) return connected;
        return component(action.segments().getFirst(), connected, rocket, action.segments());
    }

    private static Set<SegmentRef> component(SegmentRef start, Set<SegmentRef> connected, ActiveRocketData rocket, List<SegmentRef> cut) {

        var refs = new HashMap<UUID, SegmentRef>();
        rocket.getStaticSegments().forEach((id, s) -> refs.put(id, SegmentRef.of(s)));
        var result = new HashSet<SegmentRef>();
        var open = new ArrayDeque<SegmentRef>();
        open.add(start);
        while (!open.isEmpty()) {
            var current = open.removeFirst();
            if (!connected.contains(current) || !result.add(current)) continue;
            rocket.getStaticSegments().forEach((id, segment) -> {
                if (!SegmentRef.of(segment).equals(current)) return;
                for (var neighbor : rocket.getDynamicSegments().get(id).getConnectedSegments()) {
                    var ref = refs.get(neighbor);
                    if (ref != null && !(cut.contains(current) && cut.contains(ref))) open.add(ref);
                }
            });
        }
        return result;
    }

}
