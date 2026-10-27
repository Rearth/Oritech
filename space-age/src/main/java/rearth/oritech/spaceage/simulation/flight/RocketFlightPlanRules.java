package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.NavigationFailure;
import rearth.oritech.spaceage.simulation.mission.DockingTarget;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.mission.RocketDocking;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketPayloads;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.service.RocketProcessingService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the client editor and server storage on the same small set of mission-plan rules.
 */
public final class RocketFlightPlanRules {

    public static final int MAX_BRANCHES = 16;
    public static final int MAX_ACTIONS = 64;
    public static final int MAX_SEGMENT_NAME_LENGTH = 32;
    public static final int MAX_ROCKET_NAME_LENGTH = 48;
    private static final List<SpaceSimulation.OrbitBand> ASTEROID_DESTINATIONS = List.of(
            SpaceSimulation.OrbitBand.SURFACE, SpaceSimulation.OrbitBand.TIGHT);
    private static final List<SpaceSimulation.OrbitBand> CELESTIAL_DESTINATIONS = List.of(
            SpaceSimulation.OrbitBand.SURFACE, SpaceSimulation.OrbitBand.LOW,
            SpaceSimulation.OrbitBand.MEDIUM, SpaceSimulation.OrbitBand.HIGH);

    private RocketFlightPlanRules() {
    }

    public static Validation inspect(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket,
                                     List<SpaceSimulation.SpaceObjectData> objects, boolean launch,
                                     MissionState.Position position) {

        return inspect(plan, rocket, objects, launch, position, List.of(), List.of());
    }

    public static Validation inspect(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket,
                                     List<SpaceSimulation.SpaceObjectData> objects, boolean launch, MissionState.Position position,
                                     Collection<RecipeHolder<VacuumRecipe>> recipes,
                                     List<DockingTarget> stations) {

        return inspect(plan, rocket, objects, launch, position, recipes, stations, null);
    }

    public static Validation inspect(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket,
                                     List<SpaceSimulation.SpaceObjectData> objects, boolean launch, MissionState.Position position,
                                     Collection<RecipeHolder<VacuumRecipe>> recipes,
                                     List<DockingTarget> stations, RocketFlightPathCalculator.FlightPath forecast) {

        var navigationObjects = new ArrayList<>(objects);
        stations.stream().filter(target -> !target.id().equals(rocket.getRocketId()))
                .map(DockingTarget::point).forEach(navigationObjects::add);
        var normalized = normalizeAndValidate(plan, rocket, navigationObjects, launch);
        var issues = new ArrayList<Issue>();
        if (normalized == null) {
            issues.add(new Issue(plan.root().id(), SpaceSimulation.FlightPlanAction.NO_TARGET, "invalid_plan", 0, 0));
            return new Validation(plan, issues);
        }
        if (forecast == null)
            forecast = RocketFlightPathCalculator.calculateFrom(rocket, objects, normalized, position, recipes, stations);
        var actions = new HashMap<UUID, SpaceSimulation.FlightPlanAction>();
        normalized.branches().forEach(b -> b.actions().forEach(a -> actions.put(a.id(), a)));
        var seen = new HashSet<UUID>();
        for (var branch : plan.branches())
            for (var action : branch.actions()) {
                var accepted = actions.get(action.id());
                var hardware = action.type() == SpaceSimulation.ActionType.PROCESS || action.type() == SpaceSimulation.ActionType.DOCK
                        ? FlightPlanHardware.before(normalized, branch, action.id(), rocket, forecast) : rocket;
                if (action.type() == SpaceSimulation.ActionType.PROCESS) {
                    var modules = action.settings().processing().modules();
                    if (modules.isEmpty() || new HashSet<>(modules).size() != modules.size()
                            || action.settings().processing().host().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)
                            && modules.stream().anyMatch(ref -> RocketProcessingService.controller(hardware, ref) == null))
                        issues.add(new Issue(branch.id(), action.id(), "invalid_action", 0, 0));
                }
                if (action.asteroidTarget().closest() && !action.asteroidTarget().valid())
                    issues.add(new Issue(branch.id(), action.id(), "asteroid_size_invalid", 0, 0));
                var serviceIssue = serviceIssue(action, hardware, stations);
                if (!serviceIssue.isEmpty()) issues.add(new Issue(branch.id(), action.id(), serviceIssue, 0, 0));
                if (!seen.add(action.id()) || accepted == null || !accepted.addons().equals(action.addons()))
                    issues.add(new Issue(branch.id(), action.id(), "invalid_action", 0, 0));
            }
        if (normalized.branches().size() < plan.branches().size())
            issues.add(new Issue(plan.root().id(), SpaceSimulation.FlightPlanAction.NO_TARGET, "invalid_plan", 0, 0));
        issues.addAll(NavigationComputerRules.issues(normalized, RocketPayloads.planView(rocket), forecast));
        for (var path : forecast.paths()) {
            if (!path.terminalState().preventsLaunch() || path.actionMoments().isEmpty()) continue;
            var moment = path.actionMoments().getLast();
            var action = actions.get(moment.actionId());
            if (action != null && (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                    || action.type() == SpaceSimulation.ActionType.DEPLOY_CUBESAT))
                issues.add(new Issue(path.branchId(), action.id(), "navigation_" + path.terminalState().name().toLowerCase(Locale.ROOT), 0, 0, path.failure().orElse(null)));
        }
        // Preserve invalid instructions in editable drafts; callers decide whether to accept them.
        return new Validation(issues.isEmpty() ? normalized : plan, issues);
    }

    private static String serviceIssue(SpaceSimulation.FlightPlanAction action, ActiveRocketData rocket, List<DockingTarget> stations) {

        var hostId = switch (action.type()) {
            case DOCK, NAVIGATE_TO -> action.settings().docking().host();
            case PROCESS -> action.settings().processing().host();
            default -> SpaceSimulation.FlightPlanAction.NO_TARGET;
        };
        if (hostId.equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            return action.type() == SpaceSimulation.ActionType.DOCK ? "dock_target_missing" : "";
        }
        var host = stations.stream().filter(t -> t.id().equals(hostId)).findFirst().orElse(null);
        if (host == null || host.id().equals(rocket.getRocketId())) return "dock_target_missing";
        if (action.type() == SpaceSimulation.ActionType.DOCK || action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
            var d = action.settings().docking();
            if (host.revision() != d.revision() || Math.hypot(host.position().x() - d.x(), host.position().y() - d.y()) > .001)
                return "dock_target_moved";
            if (action.type() == SpaceSimulation.ActionType.DOCK
                    && (!host.ports().contains(d.hostPort()) || !RocketDocking.ports(rocket).contains(d.localPort())))
                return "dock_port_missing";
        } else if (action.settings().processing().modules().stream().anyMatch(ref -> RocketProcessingService.controller(host.rocket(), ref) == null))
            return "invalid_action";
        return "";
    }

    public static SpaceSimulation.FlightPlan normalize(SpaceSimulation.FlightPlan plan) {

        var root = plan.root();
        var byParentAction = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        plan.branches().stream().filter(branch -> !branch.isRoot())
                .forEach(branch -> byParentAction.putIfAbsent(branch.parentSeparationAction(), branch));

        var result = new ArrayList<SpaceSimulation.FlightPlanBranch>();
        addBranchAndChildren(root, byParentAction, result, new HashSet<>());
        return new SpaceSimulation.FlightPlan(result, plan.segmentConfigurations(), plan.name(), plan.category());
    }

    /**
     * Keep generated links until recalculation can decide which booster events still exist.
     */
    public static List<SpaceSimulation.FlightPlanAction> preserveBoosterLinks(
            SpaceSimulation.FlightPlanBranch previous, List<SpaceSimulation.FlightPlanAction> edited) {

        var result = new ArrayList<SpaceSimulation.FlightPlanAction>();
        for (var action : edited) {
            if (action.isGenerated()) continue;
            result.add(action);
            if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
                previous.actions().stream().filter(SpaceSimulation.FlightPlanAction::isGenerated)
                        .filter(link -> link.targetId().equals(action.id())).forEach(result::add);
            }
        }
        return result;
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
        eventsByNavigation.values().forEach(items -> items.sort(Comparator
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
        return normalize(new SpaceSimulation.FlightPlan(branches, plan.segmentConfigurations(), plan.name(), plan.category()));
    }

    private static SpaceSimulation.FlightPlan normalizeAndValidate(SpaceSimulation.FlightPlan plan, ActiveRocketData rocket,
                                                                   List<SpaceSimulation.SpaceObjectData> objects, boolean launch) {

        if (plan.branches().size() > MAX_BRANCHES
                || plan.branches().stream().flatMap(branch -> branch.actions().stream())
                .filter(action -> !action.isGenerated()).count() > MAX_ACTIONS
                || plan.branches().stream().noneMatch(SpaceSimulation.FlightPlanBranch::isRoot)) return null;

        var objectsById = new HashMap<UUID, SpaceSimulation.SpaceObjectData>();
        objects.forEach(object -> objectsById.put(object.id(), object));
        NavigationWaypoint.targets(plan).forEach(object -> objectsById.put(object.id(), object));
        // Carried cube-sats bring their own segments into the plan.
        var view = RocketPayloads.planView(rocket);
        var segmentIds = new HashMap<SpaceSimulation.SegmentRef, UUID>();
        view.getStaticSegments().forEach((id, segment) -> segmentIds.put(SpaceSimulation.SegmentRef.of(segment), id));

        var configurations = new ArrayList<SpaceSimulation.SegmentConfiguration>();
        var configuredSegments = new HashSet<SpaceSimulation.SegmentRef>();
        var segmentCount = segmentIds.size();
        for (var configuration : plan.segmentConfigurations()) {
            if (!segmentIds.containsKey(configuration.segment()) || !configuredSegments.add(configuration.segment())) {
                continue;
            }
            var name = configuration.name().strip();
            if (name.length() > MAX_SEGMENT_NAME_LENGTH) name = name.substring(0, MAX_SEGMENT_NAME_LENGTH);
            var stages = configuration.engineStages().stream()
                    .mapToInt(Integer::intValue)
                    .filter(stage -> stage >= 1 && stage <= (launch ? segmentCount : MAX_ACTIONS))
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
                if (!actionIds.add(action.id()) || !segmentsValid(action, segmentIds, view)) continue;
                var validatedAction = action;
                if (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO) {
                    var target = objectsById.get(action.targetId());
                    if (action.asteroidTarget().closest()) {
                        validatedAction = action.withOrbit(compatibleOrbit(SpaceObjects.ObjectType.ASTEROID, action.orbit()));
                    } else {
                        if (target == null) continue;
                        validatedAction = action.withOrbit(compatibleOrbit(target.type(), action.orbit()));
                    }
                } else if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID) {
                    var target = objectsById.get(action.targetId());
                    if (!action.targetId().equals(SpaceSimulation.FlightPlanAction.CLOSEST_ASTEROID)
                            && (target == null || target.type() != SpaceObjects.ObjectType.ASTEROID)) continue;
                    validatedAction = action.withOrbit(compatibleOrbit(SpaceObjects.ObjectType.ASTEROID, action.orbit()));
                } else if (action.type() == SpaceSimulation.ActionType.SCAN) {
                    validatedAction = action.withService(SpaceSimulation.ServiceSettings.DEFAULT);
                }
                if (validatedAction.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                        && !SpaceBalance.hasSlots(validatedAction.targetId(), validatedAction.orbit())) {
                    var settings = validatedAction.service();
                    validatedAction = validatedAction.withService(new SpaceSimulation.ServiceSettings(settings.durationTicks(),
                            settings.timeoutTicks(), -1));
                }
                var addons = validAddons(validatedAction);
                var targetVelocity = validatedAction.type() == SpaceSimulation.ActionType.NAVIGATE_TO
                        && validatedAction.velocityMode() == SpaceSimulation.ArrivalVelocityMode.CUSTOM
                        ? Math.clamp(validatedAction.targetVelocity(), 0, 100_000) : 0;
                var boundedAction = validatedAction.withVelocity(validatedAction.velocityMode(), targetVelocity)
                        .withMaxSpeed(Math.clamp(validatedAction.maxSpeed(), 0, 100_000)).withAddons(addons);
                actions.add(applyLandingUncertainty(boundedAction));
            }
            validatedBranches.add(branch.withActions(actions));
        }

        var rocketName = plan.name().strip();
        if (rocketName.length() > MAX_ROCKET_NAME_LENGTH) rocketName = rocketName.substring(0, MAX_ROCKET_NAME_LENGTH);
        var normalized = normalize(new SpaceSimulation.FlightPlan(validatedBranches, configurations, rocketName, plan.category()));
        if (launch) normalized = trimEngineStageGaps(normalized, segmentIds.keySet());
        return normalized.branches().size() <= MAX_BRANCHES ? normalized : null;
    }

    /** Each card keeps at most one condition of a type it supports, clamped to its limits. */
    private static List<SpaceSimulation.ActionAddon> validAddons(SpaceSimulation.FlightPlanAction action) {

        var type = action.type();
        if (type != SpaceSimulation.ActionType.NAVIGATE_TO && !type.stationKeeping()) return List.of();
        return action.addons().stream()
                .filter(addon -> type == SpaceSimulation.ActionType.NAVIGATE_TO
                        ? addon.type().isNavigationCondition() : addon.type().isStationaryCondition())
                .limit(1)
                .map(addon -> addon.withValue(clampAddonValue(addon.type(), addon.value())))
                .toList();
    }

    public static SpaceSimulation.FlightPlanAction applyLandingUncertainty(
            SpaceSimulation.FlightPlanAction action) {

        if (!action.targetId().equals(SpaceObjects.EARTH_ID)
                || action.orbit() != SpaceSimulation.OrbitBand.SURFACE) return action;
        var uncertainty = action.addons().stream()
                .filter(addon -> addon.type() == SpaceSimulation.ActionAddonType.DESIRED_UNCERTAINTY)
                .mapToInt(SpaceSimulation.ActionAddon::value).findFirst().orElse(0);
        if (uncertainty <= 0) return action.withLanding(action.landingX(), action.landingZ(), 0, 0);
        var random = new Random(action.id().getMostSignificantBits() ^ action.id().getLeastSignificantBits());
        var angle = random.nextDouble() * Math.PI * 2;
        var radius = Math.sqrt(random.nextDouble()) * uncertainty;
        return action.withLanding(action.landingX(), action.landingZ(),
                (int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius));
    }

    /**
     * Keep configured stage numbers after separation, even when only one segment remains.
     * Boosters also create the stage after their final enabled stage.
     */
    public static int stageCount(SpaceSimulation.FlightPlan plan, int segmentCount) {

        if (segmentCount <= 0) return 1;
        var result = plan.segmentConfigurations().stream().anyMatch(SpaceSimulation.SegmentConfiguration::booster)
                ? 2 : 1;
        for (var configuration : plan.segmentConfigurations()) {
            result = Math.max(result, configuration.lastEngineStage() + (configuration.booster() ? 1 : 0));
        }
        return Math.clamp(result, 1, MAX_ACTIONS);
    }

    /**
     * A new stage only becomes available after at least one segment has been assigned to its predecessor.
     */
    public static int editableStageCount(SpaceSimulation.FlightPlan plan,
                                         Collection<SpaceSimulation.SegmentRef> segments) {

        if (segments.isEmpty()) return 1;
        var lastUsedStage = lastContiguousUsedStage(plan, segments);
        return Math.min(segments.size(), Math.max(1, lastUsedStage + 1));
    }

    /**
     * Removes selections beyond the first empty stage. Otherwise unchecking the last user of stage two could leave
     * a hidden stage three active in the simulation, with no way for the player to see or repair the gap.
     */
    public static SpaceSimulation.FlightPlan trimEngineStageGaps(
            SpaceSimulation.FlightPlan plan, Collection<SpaceSimulation.SegmentRef> segments) {

        var highestAllowedStage = Math.min(segments.size(), lastContiguousUsedStage(plan, segments) + 1);
        var configurations = plan.segmentConfigurations().stream().map(configuration ->
                configuration.withEngineStages(configuration.engineStages().stream()
                        .filter(stage -> stage <= highestAllowedStage).toList())).toList();
        return plan.withSegmentConfigurations(configurations);
    }

    private static int lastContiguousUsedStage(SpaceSimulation.FlightPlan plan,
                                               Collection<SpaceSimulation.SegmentRef> segments) {

        var lastUsedStage = 0;
        for (int stage = 1; stage <= segments.size(); stage++) {
            var checkedStage = stage;
            var used = segments.stream().map(plan::configurationFor)
                    .anyMatch(configuration -> configuration.usesEnginesDuring(checkedStage));
            if (!used) break;
            lastUsedStage = stage;
        }
        return lastUsedStage;
    }

    public static List<SpaceSimulation.OrbitBand> availableOrbits(SpaceObjects.ObjectType type) {

        if (type == SpaceObjects.ObjectType.SUN) return List.of(SpaceSimulation.OrbitBand.SOLAR);
        if (type == SpaceObjects.ObjectType.SURVEY_REGION || type == SpaceObjects.ObjectType.CRAFT || type == SpaceObjects.ObjectType.WAYPOINT)
            return List.of(SpaceSimulation.OrbitBand.SURFACE);
        return type == SpaceObjects.ObjectType.ASTEROID ? ASTEROID_DESTINATIONS : CELESTIAL_DESTINATIONS;
    }

    /**
     * Keeps a destination useful when a card changes between an asteroid and a larger celestial object.
     */
    public static SpaceSimulation.OrbitBand compatibleOrbit(SpaceObjects.ObjectType type,
                                                            SpaceSimulation.OrbitBand requested) {

        var available = availableOrbits(type);
        if (available.contains(requested)) return requested;
        if (available.size() == 1) return available.getFirst();
        return type == SpaceObjects.ObjectType.ASTEROID
                ? SpaceSimulation.OrbitBand.TIGHT : SpaceSimulation.OrbitBand.LOW;
    }

    private static boolean segmentsValid(SpaceSimulation.FlightPlanAction action,
                                         Map<SpaceSimulation.SegmentRef, UUID> segmentIds,
                                         ActiveRocketData rocket) {

        if (action.type() == SpaceSimulation.ActionType.DEPLOY_CUBESAT)
            return action.segments().size() == 1 && RocketPayloads.all(rocket).containsKey(action.payload());
        if (!action.segments().stream().allMatch(segmentIds::containsKey)) return false;
        if (action.type() == SpaceSimulation.ActionType.DISCONNECT_BOOSTER) return action.segments().size() == 1;
        if (action.type() == SpaceSimulation.ActionType.CONNECT_ASTEROID
                || action.type() == SpaceSimulation.ActionType.DECOUPLE
                && !action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)) {
            return action.segments().size() == 1 && hasAsteroidAnchor(rocket.getStaticSegments().get(segmentIds.get(action.segments().getFirst())));
        }
        if (action.type() != SpaceSimulation.ActionType.DECOUPLE) return true;
        if (action.segments().size() != 2) return false;
        UUID first = segmentIds.get(action.segments().get(0));
        UUID second = segmentIds.get(action.segments().get(1));
        var segment = rocket.getStaticSegments().get(first);
        return segment != null && segment.getConnectedSegments().contains(second);
    }

    public static int clampAddonValue(SpaceSimulation.ActionAddonType type, int value) {

        return switch (type) {
            case DISTANCE_FROM_TARGET -> Math.clamp(value, 1, 10_000_000);
            case TIME_BEFORE_ARRIVAL, DURATION -> Math.clamp(value, 1, 1_000_000);
            case DESIRED_UNCERTAINTY -> Math.clamp(value, 1, 100_000);
            case LOW_RF, LOW_FUEL -> Math.clamp(value, 1, 100);
        };
    }

    public static List<SpaceSimulation.SegmentRef> asteroidAnchorSegments(ActiveRocketData rocket) {

        return rocket.getStaticSegments().values().stream()
                .filter(RocketFlightPlanRules::hasAsteroidAnchor)
                .map(SpaceSimulation.SegmentRef::of).sorted(Comparator.comparingLong(ref -> ref.anchor().asLong()))
                .toList();
    }

    private static boolean hasAsteroidAnchor(StaticRocketSegment segment) {

        return segment != null && segment.blocks().stream().anyMatch(block -> block.state().is(SpaceAgeBlocks.ASTEROID_ANCHOR));
    }

    private static void addBranchAndChildren(SpaceSimulation.FlightPlanBranch branch,
                                             Map<UUID, SpaceSimulation.FlightPlanBranch> byParentAction,
                                             List<SpaceSimulation.FlightPlanBranch> result, Set<UUID> visited) {

        if (!visited.add(branch.id())) return;
        result.add(branch);
        for (var action : branch.actions()) {
            var separatesCraft = action.type() == SpaceSimulation.ActionType.DECOUPLE
                    && action.segments().size() == 2 || action.type() == SpaceSimulation.ActionType.DEPLOY_CUBESAT;
            var generatedBooster = action.type() == SpaceSimulation.ActionType.DISCONNECT_BOOSTER
                    && action.segments().size() == 1;
            if (!separatesCraft && !generatedBooster) continue;
            var child = byParentAction.get(action.id());
            if (child == null) child = new SpaceSimulation.FlightPlanBranch(UUID.randomUUID(), action.id(), List.of());
            addBranchAndChildren(child, byParentAction, result, visited);
        }
    }

    public record Issue(UUID branchId, UUID actionId, String code, int required, int available, NavigationFailure failure) {

        public Issue(UUID branchId, UUID actionId, String code, int required, int available) {

            this(branchId, actionId, code, required, available, null);
        }

        public Component description() {

            if (failure != null) return failure.description();
            return code.startsWith("navigation_") ? Component.translatable("screen.oritech_space_age.blocked." + code.substring("navigation_".length()))
                    : Component.translatable("screen.oritech_space_age.validation." + code, required, available);
        }
    }

    public record Validation(SpaceSimulation.FlightPlan plan, List<Issue> issues) {

        public Validation {

            issues = List.copyOf(issues);
        }

        public boolean valid() {

            return issues.isEmpty();
        }
    }
}
