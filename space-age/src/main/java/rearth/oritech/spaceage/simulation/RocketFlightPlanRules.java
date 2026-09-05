package rearth.oritech.spaceage.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Keeps the client editor and server storage on the same small set of mission-plan rules. */
public final class RocketFlightPlanRules {

    public static final int MAX_BRANCHES = 16;
    public static final int MAX_ACTIONS = 64;
    public static final int MAX_SEGMENT_NAME_LENGTH = 32;
    private static final List<SpaceSimulation.OrbitBand> ASTEROID_DESTINATIONS = List.of(
            SpaceSimulation.OrbitBand.SURFACE, SpaceSimulation.OrbitBand.TIGHT);
    private static final List<SpaceSimulation.OrbitBand> CELESTIAL_DESTINATIONS = List.of(
            SpaceSimulation.OrbitBand.SURFACE, SpaceSimulation.OrbitBand.LOW,
            SpaceSimulation.OrbitBand.MEDIUM, SpaceSimulation.OrbitBand.HIGH);

    private RocketFlightPlanRules() {
    }

    public static SpaceSimulation.FlightPlan normalize(SpaceSimulation.FlightPlan plan) {
        var root = plan.root();
        var byParentAction = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        plan.branches().stream().filter(branch -> !branch.isRoot())
                .forEach(branch -> byParentAction.putIfAbsent(branch.parentSeparationAction(), branch));

        var result = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        addBranchAndChildren(root, byParentAction, result, new HashSet<>());
        return new SpaceSimulation.FlightPlan(result, plan.segmentConfigurations());
    }

    /**
     * Replaces derived booster cards after recalculation. Their deterministic IDs preserve any child program while
     * removing branches for booster events that can no longer occur.
     */
    public static SpaceSimulation.FlightPlan synchronizeBoosterEvents(
            SpaceSimulation.FlightPlan plan, List<RocketFlightPathCalculator.BoosterEvent> events) {
        var eventsByNavigation = new HashMap<UUID, List<RocketFlightPathCalculator.BoosterEvent>>();
        events.forEach(event -> eventsByNavigation.computeIfAbsent(event.navigationActionId(), ignored -> new ArrayList<>())
                .add(event));
        eventsByNavigation.values().forEach(items -> items.sort(java.util.Comparator
                .comparingDouble(RocketFlightPathCalculator.BoosterEvent::timeSeconds)));

        var branches = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        for (var branch : plan.branches()) {
            var actions = new ArrayList<SpaceSimulation.FlightPlanAction>();
            for (var action : branch.actions()) {
                if (action.type() == SpaceSimulation.ActionType.DISCONNECT_BOOSTER) continue;
                actions.add(action);
                for (var event : eventsByNavigation.getOrDefault(action.id(), List.of())) {
                    actions.add(SpaceSimulation.FlightPlanAction.disconnectBooster(
                            event.id(), event.segment(), action.id()));
                }
            }
            branches.add(branch.withActions(actions));
        }
        return normalize(new SpaceSimulation.FlightPlan(branches, plan.segmentConfigurations()));
    }

    public static SpaceSimulation.FlightPlan validate(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket,
                                                       List<SpaceSimulation.SpaceObjectData> objects) {
        if (plan.branches().size() > MAX_BRANCHES
                || plan.branches().stream().mapToInt(branch -> branch.actions().size()).sum() > MAX_ACTIONS
                || plan.branches().stream().noneMatch(SpaceSimulation.FlightPlanBranch::isRoot)) return null;

        var objectsById = new HashMap<UUID, SpaceSimulation.SpaceObjectData>();
        objects.forEach(object -> objectsById.put(object.id(), object));
        var segmentIds = new HashMap<SpaceSimulation.SegmentRef, UUID>();
        rocket.getStaticSegments().forEach((id, segment) -> segmentIds.put(SpaceSimulation.SegmentRef.of(segment), id));

        var configurations = new ArrayList<SpaceSimulation.SegmentConfiguration>();
        var configuredSegments = new HashSet<SpaceSimulation.SegmentRef>();
        int segmentCount = segmentIds.size();
        for (var configuration : plan.segmentConfigurations()) {
            if (!segmentIds.containsKey(configuration.segment()) || !configuredSegments.add(configuration.segment())) {
                continue;
            }
            String name = configuration.name().strip();
            if (name.length() > MAX_SEGMENT_NAME_LENGTH) name = name.substring(0, MAX_SEGMENT_NAME_LENGTH);
            var stages = configuration.engineStages().stream()
                    .mapToInt(Integer::intValue)
                    .filter(stage -> stage >= 1 && stage <= segmentCount)
                    .distinct().sorted().boxed().toList();
            configurations.add(new SpaceSimulation.SegmentConfiguration(
                    configuration.segment(), name, configuration.booster(), stages));
        }

        var branchIds = new HashSet<UUID>();
        var actionIds = new HashSet<UUID>();
        var validatedBranches = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        for (var branch : plan.branches()) {
            if (!branchIds.add(branch.id())) continue;
            var actions = new ArrayList<SpaceSimulation.FlightPlanAction>();
            for (var action : branch.actions()) {
                if (!actionIds.add(action.id()) || !segmentsValid(action, segmentIds, rocket)) continue;
                var validatedAction = action;
                if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
                    var target = objectsById.get(action.targetId());
                    if (target == null) continue;
                    validatedAction = action.withOrbit(compatibleOrbit(target.type(), action.orbit()));
                }
                int targetVelocity = validatedAction.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                        && validatedAction.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM
                        ? Math.clamp(validatedAction.targetVelocity(), 0, 100_000) : 0;
                actions.add(validatedAction.withVelocity(validatedAction.velocityMode(), targetVelocity));
            }
            validatedBranches.add(branch.withActions(actions));
        }

        var normalized = trimEngineStageGaps(
                normalize(new SpaceSimulation.FlightPlan(validatedBranches, configurations)), segmentIds.keySet());
        return normalized.branches().size() <= MAX_BRANCHES ? normalized : null;
    }

    /** A booster creates the stage after its final enabled stage; ordinary engine selections do not. */
    public static int stageCount(SpaceSimulation.FlightPlan plan, int segmentCount) {
        if (segmentCount <= 0) return 1;
        int result = plan.segmentConfigurations().stream().anyMatch(SpaceSimulation.SegmentConfiguration::booster)
                ? 2 : 1;
        for (var configuration : plan.segmentConfigurations()) {
            if (configuration.booster()) result = Math.max(result, configuration.lastEngineStage() + 1);
        }
        return Math.clamp(result, 1, segmentCount);
    }

    /** A new stage only becomes available after at least one segment has been assigned to its predecessor. */
    public static int editableStageCount(SpaceSimulation.FlightPlan plan,
                                         Collection<SpaceSimulation.SegmentRef> segments) {
        if (segments.isEmpty()) return 1;
        int lastUsedStage = lastContiguousUsedStage(plan, segments);
        return Math.min(segments.size(), Math.max(1, lastUsedStage + 1));
    }

    /**
     * Removes selections beyond the first empty stage. Otherwise unchecking the last user of stage two could leave
     * a hidden stage three active in the simulation, with no way for the player to see or repair the gap.
     */
    public static SpaceSimulation.FlightPlan trimEngineStageGaps(
            SpaceSimulation.FlightPlan plan, Collection<SpaceSimulation.SegmentRef> segments) {
        int highestAllowedStage = Math.min(segments.size(), lastContiguousUsedStage(plan, segments) + 1);
        var configurations = plan.segmentConfigurations().stream().map(configuration ->
                configuration.withEngineStages(configuration.engineStages().stream()
                        .filter(stage -> stage <= highestAllowedStage).toList())).toList();
        return plan.withSegmentConfigurations(configurations);
    }

    private static int lastContiguousUsedStage(SpaceSimulation.FlightPlan plan,
                                               Collection<SpaceSimulation.SegmentRef> segments) {
        int lastUsedStage = 0;
        for (int stage = 1; stage <= segments.size(); stage++) {
            int checkedStage = stage;
            boolean used = segments.stream().map(plan::configurationFor)
                    .anyMatch(configuration -> configuration.usesEnginesDuring(checkedStage));
            if (!used) break;
            lastUsedStage = stage;
        }
        return lastUsedStage;
    }

    public static List<SpaceSimulation.OrbitBand> availableOrbits(SpaceObjects.ObjectType type) {
        return type == SpaceObjects.ObjectType.ASTEROID ? ASTEROID_DESTINATIONS : CELESTIAL_DESTINATIONS;
    }

    /** Keeps a destination useful when a card changes between an asteroid and a larger celestial object. */
    public static SpaceSimulation.OrbitBand compatibleOrbit(SpaceObjects.ObjectType type,
                                                             SpaceSimulation.OrbitBand requested) {
        var available = availableOrbits(type);
        if (available.contains(requested)) return requested;
        return type == SpaceObjects.ObjectType.ASTEROID
                ? SpaceSimulation.OrbitBand.TIGHT : SpaceSimulation.OrbitBand.LOW;
    }

    private static boolean segmentsValid(SpaceSimulation.FlightPlanAction action,
                                         Map<SpaceSimulation.SegmentRef, UUID> segmentIds,
                                         ActiveRocketData rocket) {
        if (!action.segments().stream().allMatch(segmentIds::containsKey)) return false;
        if (action.type() == SpaceSimulation.ActionType.DISCONNECT_BOOSTER) return action.segments().size() == 1;
        if (action.type() != SpaceSimulation.ActionType.DECOUPLE) return true;
        if (action.segments().size() != 2) return false;
        UUID first = segmentIds.get(action.segments().get(0));
        UUID second = segmentIds.get(action.segments().get(1));
        var segment = rocket.getStaticSegments().get(first);
        return segment != null && segment.getConnectedSegments().contains(second);
    }

    private static void addBranchAndChildren(SpaceSimulation.FlightPlanBranch branch,
                                             Map<UUID, SpaceSimulation.FlightPlanBranch> byParentAction,
                                             List<SpaceSimulation.FlightPlanBranch> result, Set<UUID> visited) {
        if (!visited.add(branch.id())) return;
        result.add(branch);
        for (var action : branch.actions()) {
            boolean separatesCraft = action.type() == SpaceSimulation.ActionType.DECOUPLE
                    && action.segments().size() == 2;
            boolean generatedBooster = action.type() == SpaceSimulation.ActionType.DISCONNECT_BOOSTER
                    && action.segments().size() == 1;
            if (!separatesCraft && !generatedBooster) continue;
            var child = byParentAction.get(action.id());
            if (child == null) child = new SpaceSimulation.FlightPlanBranch(UUID.randomUUID(), action.id(), List.of());
            addBranchAndChildren(child, byParentAction, result, visited);
        }
    }
}
