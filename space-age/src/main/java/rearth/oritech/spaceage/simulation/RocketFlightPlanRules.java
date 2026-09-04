package rearth.oritech.spaceage.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Keeps client editing and server acceptance on the same small set of flight-plan rules. */
public final class RocketFlightPlanRules {

    public static final int MAX_BRANCHES = 16;
    public static final int MAX_ACTIONS = 64;

    private RocketFlightPlanRules() {
    }

    /**
     * Rebuilds the branch tree from separation actions. Branches use action IDs as their parent link so moving an
     * action does not invalidate its child, while removing the action naturally removes the entire child subtree.
     */
    public static SpaceSimulation.FlightPlan normalize(SpaceSimulation.FlightPlan plan) {
        var root = plan.root();
        var byParentAction = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        plan.branches().stream().filter(branch -> !branch.isRoot())
                .forEach(branch -> byParentAction.putIfAbsent(branch.parentSeparationAction(), branch));

        var result = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        addBranchAndChildren(root, byParentAction, result, new HashSet<>());
        return new SpaceSimulation.FlightPlan(result);
    }

    /** Returns a safe, normalized plan, or {@code null} when its basic structure exceeds the supported limits. */
    public static SpaceSimulation.FlightPlan validate(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket,
                                                       List<SpaceSimulation.SpaceObjectData> objects) {
        if (plan.branches().size() > MAX_BRANCHES
                || plan.branches().stream().mapToInt(branch -> branch.actions().size()).sum() > MAX_ACTIONS
                || plan.branches().stream().noneMatch(SpaceSimulation.FlightPlanBranch::isRoot)) return null;

        var objectIds = new HashSet<UUID>();
        objects.forEach(object -> objectIds.add(object.id()));

        var segmentIds = new HashMap<SpaceSimulation.SegmentRef, UUID>();
        for (var entry : rocket.getStaticSegments().entrySet()) {
            segmentIds.put(SpaceSimulation.SegmentRef.of(entry.getValue()), entry.getKey());
        }

        var branchIds = new HashSet<UUID>();
        var actionIds = new HashSet<UUID>();
        var validatedBranches = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        for (var branch : plan.branches()) {
            if (!branchIds.add(branch.id())) continue;

            // Invalid entries are discarded individually. A stale segment or target should not erase the rest of a
            // reusable plan when it is applied to a slightly different rocket or simulation.
            var validatedActions = new ArrayList<SpaceSimulation.FlightPlanAction>();
            for (var action : branch.actions()) {
                if (!actionIds.add(action.id()) || !segmentsValid(action, segmentIds, rocket)) continue;
                if (!targetValid(action, objectIds)) continue;

                long value = switch (action.type()) {
                    case WAIT_TICKS -> Math.clamp(action.value(), 1, 72_000);
                    case WAIT_SECONDS -> Math.clamp(action.value(), 1, 3_600);
                    case WAIT_UNTIL_DISTANCE -> Math.clamp(action.value(), 0, 100_000_000);
                    case WAIT_FOR_EVENT -> Math.clamp(action.value(), 0, SpaceSimulation.WaitEvent.values().length - 1);
                    case SET_SURFACE_DESTINATION -> Math.clamp(action.value(), -30_000_000, 30_000_000);
                    case SET_ARRIVAL_VELOCITY -> Math.clamp(action.value(), 0, 100_000);
                    default -> 0;
                };
                long secondaryValue = action.type() == SpaceSimulation.ActionType.SET_SURFACE_DESTINATION
                        ? Math.clamp(action.secondaryValue(), -30_000_000, 30_000_000) : 0;
                validatedActions.add(action.withValue(value).withSecondaryValue(secondaryValue));
            }
            validatedBranches.add(new SpaceSimulation.FlightPlanBranch(
                    branch.id(), branch.parentSeparationAction(), validatedActions));
        }

        var normalized = normalize(new SpaceSimulation.FlightPlan(validatedBranches));
        return normalized.branches().size() <= MAX_BRANCHES ? normalized : null;
    }

    private static boolean segmentsValid(SpaceSimulation.FlightPlanAction action,
                                         Map<SpaceSimulation.SegmentRef, UUID> segmentIds,
                                         ActiveRocketData rocket) {
        if (!action.segments().stream().allMatch(segmentIds::containsKey)) return false;
        if (action.type() != SpaceSimulation.ActionType.DISABLE_COUPLINGS) return true;
        if (action.segments().size() != 2) return false;

        UUID first = segmentIds.get(action.segments().get(0));
        UUID second = segmentIds.get(action.segments().get(1));
        var segment = rocket.getStaticSegments().get(first);
        return segment != null && segment.getConnectedSegments().contains(second);
    }

    private static boolean targetValid(SpaceSimulation.FlightPlanAction action, Set<UUID> objectIds) {
        return switch (action.type()) {
            case SET_NAVIGATION_TARGET -> objectIds.contains(action.targetId());
            case WAIT_UNTIL_DISTANCE -> action.targetId().equals(SpaceSimulation.FlightPlanAction.CURRENT_TARGET)
                    || action.targetId().equals(SpaceObjects.EARTH_ID);
            default -> true;
        };
    }

    private static void addBranchAndChildren(SpaceSimulation.FlightPlanBranch branch,
                                             Map<UUID, SpaceSimulation.FlightPlanBranch> byParentAction,
                                             List<SpaceSimulation.FlightPlanBranch> result, Set<UUID> visited) {
        if (!visited.add(branch.id())) return;
        result.add(branch);
        for (var action : branch.actions()) {
            if (action.type() != SpaceSimulation.ActionType.DISABLE_COUPLINGS || action.segments().size() != 2) continue;
            var child = byParentAction.get(action.id());
            if (child == null) {
                child = new SpaceSimulation.FlightPlanBranch(UUID.randomUUID(), action.id(), List.of());
            } else {
                child = new SpaceSimulation.FlightPlanBranch(child.id(), action.id(), child.actions());
            }
            addBranchAndChildren(child, byParentAction, result, visited);
        }
    }
}
