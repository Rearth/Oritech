package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionType;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlan;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanAction;
import rearth.oritech.spaceage.simulation.SpaceSimulation.FlightPlanBranch;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Program memory belongs to connected hardware; completed instructions never occupy it.
 */
public final class NavigationComputerRules {

    public static final int BASE_STEPS = 3;

    private NavigationComputerRules() {
    }

    public static int computers(ActiveRocketData rocket, Collection<SegmentRef> segments) {

        return rocket.getStaticSegments().values().stream().filter(s -> segments.contains(SegmentRef.of(s)))
                .mapToInt(s -> (int) s.blocks().stream().filter(b -> b.state().is(SpaceAgeBlocks.NAVIGATION_COMPUTER)).count()).sum();
    }

    public static Set<SegmentRef> allSegments(ActiveRocketData rocket) {

        return rocket.getStaticSegments().values().stream().map(SegmentRef::of).collect(Collectors.toSet());
    }

    public static int capacity(ActiveRocketData rocket) {

        return Math.min(RocketFlightPlanRules.MAX_ACTIONS, BASE_STEPS + computers(rocket, allSegments(rocket)));
    }

    public static boolean requiresComputer(FlightPlanAction action) {

        return !action.addons().isEmpty() || action.service().timeoutTicks() > 0;
    }

    public static int unfinished(FlightPlan plan, FlightPlanBranch branch, int fromIndex) {

        return unfinished(plan, branch, fromIndex, new HashSet<>());
    }

    private static int unfinished(FlightPlan plan, FlightPlanBranch branch, int fromIndex, Set<UUID> visited) {

        if (!visited.add(branch.id())) return 0;
        var count = 0;
        // Count child programs too: their instructions are onboard before their segments separate.
        for (int i = fromIndex; i < branch.actions().size(); i++) {
            var action = branch.actions().get(i);
            if (!action.isGenerated()) count++;
            var child = plan.branches().stream().filter(b -> !b.isRoot() && b.parentSeparationAction().equals(action.id())).findFirst();
            if (child.isPresent()) count += unfinished(plan, child.get(), 0, visited);
        }
        return count;
    }

    public static List<RocketFlightPlanRules.Issue> issues(FlightPlan plan, ActiveRocketData rocket,
                                                           RocketFlightPathCalculator.FlightPath forecast) {

        var issues = new ArrayList<RocketFlightPlanRules.Issue>();
        for (var branch : plan.branches()) {
            var path = forecast.paths().stream().filter(p -> p.branchId().equals(branch.id())).findFirst().orElse(null);
            var connected = branch.isRoot() ? allSegments(rocket)
                    : path == null || path.samples().isEmpty() ? branchSegments(plan, branch, rocket, new HashSet<>()) : path.samples().getFirst().connectedSegments();
            if (connected.isEmpty()) continue;
            checkCapacity(issues, branch.id(), FlightPlanAction.NO_TARGET, unfinished(plan, branch, 0), computers(rocket, connected));
            for (int i = 0; i < branch.actions().size(); i++) {
                var action = branch.actions().get(i);
                var available = computers(rocket, connected);
                if (requiresComputer(action) && available == 0)
                    issues.add(new RocketFlightPlanRules.Issue(branch.id(), action.id(), "computer_required", 1, 0));
                var moment = path == null ? null : path.actionMoments().stream().filter(m -> m.actionId().equals(action.id())).findFirst().orElse(null);
                connected = moment == null ? retainedAfter(action, connected, rocket) : moment.connectedSegments();
                checkCapacity(issues, branch.id(), action.id(), unfinished(plan, branch, i + 1), computers(rocket, connected));
            }
        }
        return issues.stream().distinct().toList();
    }

    private static Set<SegmentRef> branchSegments(FlightPlan plan, FlightPlanBranch branch, ActiveRocketData rocket,
                                                  Set<UUID> visited) {

        if (branch.isRoot()) return allSegments(rocket);
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
                if (action.isGenerated() && !action.segments().isEmpty()) return Set.of(action.segments().getFirst());
            }
        return Set.of();
    }

    private static Set<SegmentRef> retainedAfter(FlightPlanAction action, Set<SegmentRef> connected, ActiveRocketData rocket) {

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

    private static void checkCapacity(List<RocketFlightPlanRules.Issue> issues, UUID branch, UUID action, int used, int computers) {

        var available = Math.min(RocketFlightPlanRules.MAX_ACTIONS, BASE_STEPS + computers);
        if (used > available) issues.add(new RocketFlightPlanRules.Issue(branch, action, "capacity", used, available));
    }
}
