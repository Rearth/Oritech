package rearth.oritech.spaceage.simulation.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.AsteroidImpactRules;
import rearth.oritech.spaceage.simulation.AsteroidTarget;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceObjects;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Segment;
import rearth.oritech.spaceage.simulation.mission.DockingTarget;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.mission.RocketDocking;
import rearth.oritech.spaceage.simulation.power.SolarPower;
import rearth.oritech.spaceage.simulation.power.SpacePowerHardware;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.RocketControllerState;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.RocketLayout;
import rearth.oritech.spaceage.simulation.rocket.RocketPayloads;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;
import rearth.oritech.spaceage.simulation.service.RocketServiceForecast;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings;
import rearth.oritech.spaceage.simulation.service.StationServiceRules;
import rearth.oritech.spaceage.simulation.service.VacuumProcessing;
import rearth.oritech.spaceage.simulation.survey.SurveyRules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Compiles high-level navigation actions into a bounded two-dimensional flight preview.
 * The guidance simulation is deliberately small, but it keeps velocity continuous between navigation actions.
 */
public final class RocketFlightPathCalculator {

    private static final Codec<Set<SpaceSimulation.SegmentRef>> SEGMENT_SET_CODEC = SpaceSimulation.SegmentRef.CODEC.listOf().xmap(Set::copyOf, List::copyOf);

    private RocketFlightPathCalculator() {
    }

    public static FlightPath calculate(ActiveRocketData rocket,
                                       List<SpaceSimulation.SpaceObjectData> objects,
                                       SpaceSimulation.FlightPlan plan) {

        return calculateFrom(rocket, objects, plan, null);
    }

    public static FlightPath calculateFrom(ActiveRocketData rocket, List<SpaceSimulation.SpaceObjectData> objects,
                                           SpaceSimulation.FlightPlan plan, MissionState.Position position) {

        return calculateFrom(rocket, objects, plan, position, List.of());
    }

    public static FlightPath calculateFrom(ActiveRocketData rocket, List<SpaceSimulation.SpaceObjectData> objects,
                                           SpaceSimulation.FlightPlan plan, MissionState.Position position,
                                           Collection<RecipeHolder<VacuumRecipe>> recipes) {

        return calculateFrom(rocket, objects, plan, position, recipes, List.of());
    }

    public static FlightPath calculateFrom(ActiveRocketData rocket, List<SpaceSimulation.SpaceObjectData> objects,
                                           SpaceSimulation.FlightPlan plan, MissionState.Position position,
                                           Collection<RecipeHolder<VacuumRecipe>> recipes,
                                           List<DockingTarget> stations) {

        var objectsById = new HashMap<UUID, SpaceSimulation.SpaceObjectData>();
        objects.forEach(object -> objectsById.put(object.id(), object));
        NavigationWaypoint.targets(plan).forEach(object -> objectsById.put(object.id(), object));
        stations.stream().filter(target -> !target.id().equals(rocket.getRocketId()))
                .forEach(target -> objectsById.put(target.id(), target.point()));
        var earth = objectsById.get(SpaceObjects.EARTH_ID);
        if (earth == null) return new FlightPath(List.of(), List.of(), 0);

        var branchesByParent = new HashMap<UUID, SpaceSimulation.FlightPlanBranch>();
        plan.branches().stream().filter(branch -> !branch.isRoot())
                .forEach(branch -> branchesByParent.put(branch.parentSeparationAction(), branch));
        var configurations = new HashMap<SpaceSimulation.SegmentRef, SpaceSimulation.SegmentConfiguration>();
        RocketPayloads.planView(rocket).getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of)
                .forEach(ref -> configurations.put(ref, plan.configurationFor(ref)));

        var context = new Context(objectsById, branchesByParent, configurations,
                RocketFlightPlanRules.stageCount(plan, rocket.getStaticSegments().size()));
        context.recipes = recipes;
        stations.forEach(target -> {
            context.stations.put(target.id(), target);
            var host = createInitialCraft(target.rocket(), earth, SpaceSimulation.FlightPlan.empty().root(), objectsById);
            host.x = target.position().x();
            host.y = target.position().y();
            host.dockedVisitorMass = Math.max(0,
                    target.visitorMass() - (target.visitors().contains(rocket.getRocketId()) ? RocketPerformanceCalculator.calculate(rocket).wetMassKilograms() : 0));
            context.stationCraft.put(target.id(), host);
        });
        var initial = createInitialCraft(rocket, earth, plan.root(), objectsById);
        if (position != null) {
            initial.x = position.x();
            initial.y = position.y();
            initial.headingX = position.headingX();
            initial.headingY = position.headingY();
            initial.velocityX = position.vx();
            initial.velocityY = position.vy();
            initial.currentTarget = position.target();
            initial.currentOrbit = position.orbit();
            initial.currentStage = position.stage();
            initial.attachedAsteroid = objectsById.get(position.asteroid());
            initial.asteroidAnchor = position.anchor();
        }
        stations.stream().filter(t -> t.id().equals(rocket.getRocketId())).findFirst().ifPresent(t -> initial.dockedVisitorMass = t.visitorMass());
        for (var target : stations)
            for (var link : target.links()) {
                if (link.host().equals(rocket.getRocketId()))
                    initial.occupiedPortSegments.add(link.hostPort().segment());
                if (link.visitor().equals(rocket.getRocketId())) {
                    initial.dockedHost = link.host();
                    initial.dockedHostPort = link.hostPort();
                    initial.occupiedPortSegments.add(link.visitorPort().segment());
                }
            }
        stations.stream().filter(t -> t.visitors().contains(rocket.getRocketId())).findFirst().ifPresent(t -> initial.dockedHost = t.id());
        simulateBranch(plan.root(), initial, context);

        var orderedPaths = new ArrayList<CraftPath>();
        for (var branch : plan.branches()) {
            var path = context.paths.remove(branch.id());
            if (path != null) orderedPaths.add(path);
        }
        orderedPaths.addAll(context.paths.values());
        var lastCommand = orderedPaths.stream().flatMap(path -> path.actionMoments().stream())
                .mapToDouble(ActionMoment::timeSeconds).max().orElse(0);
        return new FlightPath(orderedPaths, List.copyOf(context.boosterEvents), lastCommand,
                List.copyOf(context.arrivalPredictions), List.copyOf(context.asteroidPaths),
                List.copyOf(context.navigationAborts), List.copyOf(context.scanEstimates),
                List.copyOf(context.stationKeepingEstimates), List.copyOf(context.processingEstimates));
    }

    public static Craft createInitialCraft(ActiveRocketData rocket,
                                    SpaceSimulation.SpaceObjectData earth,
                                    SpaceSimulation.FlightPlanBranch root,
                                    Map<UUID, SpaceSimulation.SpaceObjectData> objects) {

        var refsById = new HashMap<UUID, SpaceSimulation.SegmentRef>();
        rocket.getStaticSegments().forEach((id, segment) -> refsById.put(id, SpaceSimulation.SegmentRef.of(segment)));
        var segments = new LinkedHashMap<SpaceSimulation.SegmentRef, Segment>();
        var connections = new HashMap<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>>();
        for (var entry : rocket.getStaticSegments().entrySet()) {
            var ref = refsById.get(entry.getKey());
            var dynamic = rocket.getDynamicSegments().get(entry.getKey());
            var segmentRocket = new ActiveRocketData(Map.of(entry.getKey(), rocket.getOriginalSegments().get(entry.getKey())),
                    Map.of(entry.getKey(), dynamic));
            var performance = RocketPerformanceCalculator.calculate(segmentRocket);
            var hardware = RocketHardware.of(entry.getValue());
            var previewSegment = new Segment(performance.wetMassKilograms(),
                    hardware.chemical() * RocketPerformanceCalculator.ENGINE_THRUST_NEWTONS,
                    hardware.ion() * RocketPerformanceCalculator.ENGINE_THRUST_NEWTONS,
                    hardware.chemicalSeconds(dynamic), hardware.ionSeconds(dynamic), hardware);
            previewSegment.structure = rocket.getOriginalSegments().get(entry.getKey());
            previewSegment.physical = dynamic.copy();
            previewSegment.rf = dynamic.rf();
            previewSegment.fuelMass = performance.fuelMassKilograms();
            previewSegment.dryMass = performance.dryMassKilograms();
            previewSegment.payloadEnergy = entry.getValue().payloadEnergyJoules();
            previewSegment.fuelTicks = dynamic.fuel();
            previewSegment.parachutes = ParachuteLanding.count(entry.getValue());
            dynamic.crafters.forEach((pos, cargo) -> previewSegment.crafters.put(pos, cargo.copy()));
            previewSegment.initialRF = entry.getValue().initialRF();
            previewSegment.initialFuelTicks = entry.getValue().initialFuel();
            segments.put(ref, previewSegment);

            var neighbours = new LinkedHashSet<SpaceSimulation.SegmentRef>();
            dynamic.getConnectedSegments().stream().map(refsById::get).filter(Objects::nonNull)
                    .forEach(neighbours::add);
            connections.put(ref, neighbours);
        }

        var launch = rocket.getLaunchPosition();
        var angle = SpaceBalance.surfaceAngle(launch.getX(), launch.getZ());
        var craft = new Craft(segments, connections, earth.x() + Math.cos(angle) * earth.radius(),
                earth.y() + Math.sin(angle) * earth.radius(), 0);
        craft.craftId = rocket.getRocketId();
        craft.currentTarget = earth.id();
        craft.currentOrbit = SpaceSimulation.OrbitBand.SURFACE;
        return craft;
    }

    private static void simulateBranch(SpaceSimulation.FlightPlanBranch branch, Craft state,
                                       Context context) {

        state.branchId = branch.id();
        state.addSample(PathPhase.COAST, SpaceSimulation.FlightPlanAction.NO_TARGET,
                SpaceSimulation.FlightPlanAction.NO_TARGET);
        var completed = true;

        for (int index = 0; index < branch.actions().size(); index++) {
            var action = AsteroidTarget.resolve(branch.actions().get(index), context.objects.values(),
                    state.x, state.y, state.time, branch.actions().get(index).type() == SpaceSimulation.ActionType.DECOUPLE
                            && state.attachedAsteroid != null ? state.attachedAsteroid.id() : state.currentTarget);
            if (action.type() == SpaceSimulation.ActionType.DISCONNECT_BOOSTER) continue;
            state.segments.values().forEach(segment -> segment.wetMass = Math.max(1, segment.dryMass + segment.fuelMass));
            var requiresUndocking = (!state.occupiedPortSegments.isEmpty() || !state.dockedHost.equals(SpaceSimulation.FlightPlanAction.NO_TARGET))
                    && (action.type() == SpaceSimulation.ActionType.NAVIGATE_TO || action.type() == SpaceSimulation.ActionType.DOCK
                    || action.type() == SpaceSimulation.ActionType.DISCARD_CRAFT);
            completed = !requiresUndocking && switch (action.type()) {
                case SCAN, TRANSMIT_INFORMATION -> service(action, state, context);
                case NAVIGATE_TO ->
                        state.dockedHost.equals(SpaceSimulation.FlightPlanAction.NO_TARGET) && RocketFlightNavigation.navigate(action, state, context);
                case CONNECT_ASTEROID -> connectAsteroid(action, state, context);
                case DECOUPLE -> separate(action, state, context);
                case DEPLOY_CUBESAT -> deploy(action, state, context);
                case MAINTAIN_POSITION, BEAM_POWER -> {
                    if (action.type() == SpaceSimulation.ActionType.BEAM_POWER && !beamAvailable(action, state, context)) yield false;
                    maintainPosition(action, state, context);
                    yield true;
                }
                case PROCESS -> RocketServiceForecast.process(action, state, context);
                case DOCK -> RocketServiceForecast.dock(action, state, context);
                case UNDOCK -> RocketServiceForecast.undock(action, state, context);
                case DISCARD_CRAFT -> {
                    state.discarded = true;
                    yield true;
                }
                case DISCONNECT_BOOSTER -> true;
            };
            state.actionMoments.add(new ActionMoment(branch.id(), index, action.id(), state.time,
                    state.x, state.y, completed, Set.copyOf(state.segments.keySet()),
                    state.attachedAsteroid == null ? SpaceSimulation.FlightPlanAction.NO_TARGET
                            : state.attachedAsteroid.id()));
            if (!completed || state.maintainingPosition || state.discarded || state.destroyed) break;
        }

        var terminal = state.destroyed ? TerminalState.DESTROYED
                : state.discarded ? TerminalState.DISCARDED
                  : state.maintainingPosition ? TerminalState.SUSTAINABLE
                    : completed ? TerminalState.READY : state.blockedState;
        context.paths.put(branch.id(), state.toPath(terminal, context));
    }

    private static boolean service(SpaceSimulation.FlightPlanAction action, Craft state, Context context) {

        if (action.type() == SpaceSimulation.ActionType.SCAN) return scan(action, state, context);
        double remainingTicks = action.type() == SpaceSimulation.ActionType.TRANSMIT_INFORMATION
                ? Math.max(1, action.service().timeoutTicks()) : action.service().durationTicks();
        var cost = SpaceBalance.ANTENNA_RF;
        var installed = state.segments.values().stream().anyMatch(segment -> segment.hardware.antennas() > 0);
        if (!installed) {
            state.blockedState = TerminalState.NO_ANTENNA;
            return false;
        }
        // Spend local RF until the action finishes or the next module bank runs out.
        // Different banks may run for different durations; a short-lived bank still contributes.
        while (remainingTicks > 0.0001) {
            var modules = 0;
            var step = Double.POSITIVE_INFINITY;
            for (var segment : state.segments.values()) {
                var count = segment.hardware.antennas();
                if (count == 0 || segment.rf < cost) continue;
                modules += count;
                step = Math.min(step, segment.rf / (count * (double) cost));
            }
            if (modules == 0) {
                state.blockedState = TerminalState.NOT_ENOUGH_SERVICE_RF;
                return false;
            }
            step = Math.min(step, remainingTicks);
            for (var segment : state.segments.values()) {
                var count = segment.hardware.antennas();
                if (count > 0 && segment.rf >= cost) segment.spendRF(count * cost * step);
            }
            remainingTicks -= step;
            state.time += step / 20;
            state.x += state.velocityX * step / 20;
            state.y += state.velocityY * step / 20;
        }
        state.addSample(PathPhase.COAST, state.currentTarget, action.id());
        return true;
    }

    private static boolean scan(SpaceSimulation.FlightPlanAction action, Craft state, Context context) {

        var target = context.objects.get(action.targetId().equals(SpaceSimulation.FlightPlanAction.NO_TARGET)
                ? state.currentTarget : action.targetId());
        var scanners = state.segments.values().stream().mapToInt(segment -> segment.hardware.scanners()).sum();
        var available = state.segments.values().stream().filter(segment -> segment.hardware.scanners() > 0)
                .mapToDouble(segment -> segment.rf).sum();
        context.scanEstimates.add(new ScanEstimate(action.id(), (long) Math.floor(available), scanners));
        if (scanners == 0) {
            state.blockedState = TerminalState.NO_SCANNER;
            return false;
        }
        if (target == null || target.type() != SpaceObjects.ObjectType.SURVEY_REGION) {
            state.blockedState = TerminalState.NO_SCAN_TARGET;
            return false;
        }
        if (SurveyRules.surveyDistance(target, state.x, state.y) > SpaceBalance.SCAN_RANGE) {
            state.blockedState = TerminalState.SCAN_OUT_OF_RANGE;
            return false;
        }
        if (available < SpaceBalance.SCAN_RF) {
            state.blockedState = TerminalState.NOT_ENOUGH_SERVICE_RF;
            return false;
        }
        double remaining = SpaceBalance.SCAN_RF;
        for (var segment : state.segments.values()) {
            if (segment.hardware.scanners() == 0) continue;
            var spent = Math.min(remaining, segment.rf);
            segment.spendRF(spent);
            remaining -= spent;
            if (remaining <= 0) break;
        }
        return true;
    }

    private static boolean beamAvailable(SpaceSimulation.FlightPlanAction action, Craft state, Context context) {

        if (Math.hypot(state.velocityX, state.velocityY) > .001 || !VacuumProcessing.zeroG(state.x, state.y, context.objects.values())) {
            state.blockedState = TerminalState.BEAM_STATIONARY_REQUIRED;
            return false;
        }
        var hardware = SpacePowerHardware.scan(SolarPower.structures(state));
        if (hardware.throughput() <= 0) {
            state.blockedState = TerminalState.BEAM_APERTURE_REQUIRED;
            return false;
        }
        if (!action.targetId().equals(SpaceObjects.EARTH_ID) && (!context.stations.containsKey(action.targetId()) || action.targetId().equals(state.craftId))) {
            state.blockedState = TerminalState.TARGET_UNAVAILABLE;
            return false;
        }
        return true;
    }

    private static void maintainPosition(SpaceSimulation.FlightPlanAction action, Craft state, Context context) {

        var addon = action.addons().stream().filter(a -> a.type().isStationaryCondition()).findFirst().orElse(null);
        var beamOutput = action.type() == SpaceSimulation.ActionType.BEAM_POWER
                ? SpacePowerHardware.scan(SolarPower.structures(state)).throughput() : 0;
        var reserve = StationServiceRules.beamReserve(state, action.settings().beam().reservePercent());
        var solar = SolarPower.output(state);
        if (solar > 0) {
            var services = state.segments.values().stream().mapToDouble(s -> StationServiceRules.antennaRF(s.hardware, 1)).sum();
            var estimate = beamOutput > 0 ? StationServiceRules.beamEstimate(StationServiceRules.budgets(state), solar, services, beamOutput, reserve, addon)
                    : StationServiceRules.solarEstimate(StationServiceRules.budgets(state), solar, services, addon);
            var duration = estimate.seconds();
            var sustainable = estimate.end() == StationKeepingEnd.SUSTAINABLE;
            context.stationKeepingEstimates.add(new StationKeepingEstimate(action.id(), duration, estimate.end()));
            state.velocityX = 0;
            state.velocityY = 0;
            state.addSample(PathPhase.COAST, state.currentTarget, action.id());
            if (!sustainable) {
                var rf = state.segments.values().stream().mapToDouble(segment -> segment.rf).sum();
                var remaining = StationServiceRules.beamRemainingRF(rf, reserve, solar - services, beamOutput, duration * 20);
                StationServiceRules.forecast(state, duration * 20, state.mass(), true);
                if (beamOutput > 0) StationServiceRules.limitRF(state, remaining);
                state.time += duration;
                state.addSample(PathPhase.COAST, state.currentTarget, action.id());
            }
            state.maintainingPosition = sustainable;
            return;
        }
        var thrust = state.segments.values().stream().mapToDouble(segment -> segment.thrust(false)).sum();
        var correctionBurnSecondsPerSecond = SpaceBalance.stationKeepingBurnSecondsPerSecond(thrust, state.mass());
        var serviceRF = state.segments.values().stream()
                .mapToDouble(segment -> StationServiceRules.rfPerSecond(segment.hardware, correctionBurnSecondsPerSecond) / 20).sum();
        var estimate = StationServiceRules.estimate(StationServiceRules.budgets(state), correctionBurnSecondsPerSecond, addon);
        if (beamOutput > 0) {
            var beamEstimate = StationServiceRules.beamEstimate(StationServiceRules.budgets(state), 0, serviceRF, beamOutput, reserve, addon);
            if (beamEstimate.end() != StationKeepingEnd.SUSTAINABLE && beamEstimate.seconds() <= estimate.seconds()) estimate = beamEstimate;
        }
        var duration = estimate.seconds();
        var end = estimate.end();
        var conditionReached = estimate.conditionReached();
        context.stationKeepingEstimates.add(new StationKeepingEstimate(action.id(), duration, end));
        state.velocityX = 0;
        state.velocityY = 0;
        state.addSample(PathPhase.COAST, state.currentTarget, action.id());
        if (duration > 0 && Double.isFinite(duration)) {
            state.time += duration;
            state.addSample(PathPhase.COAST, state.currentTarget, action.id());
        }
        var storedRF = state.segments.values().stream().mapToDouble(segment -> segment.rf).sum();
        var beamRemaining = StationServiceRules.beamRemainingRF(storedRF, reserve, -serviceRF, beamOutput, duration * 20);
        state.segments.values().forEach(segment -> {
            if (segment.hardware.chemical() > 0)
                segment.consumeFuel(duration * correctionBurnSecondsPerSecond);
            segment.spendRF(duration * StationServiceRules.rfPerSecond(segment.hardware, correctionBurnSecondsPerSecond));
        });
        if (beamOutput > 0) StationServiceRules.limitRF(state, beamRemaining);
        if (addon != null && conditionReached) {
            context.navigationAborts.add(new NavigationAbortMoment(action.id(), state.branchId, addon.id(), addon.value(),
                    state.time, state.x, state.y, state.x, state.y));
        }
    }

    static boolean finishStage(SpaceSimulation.FlightPlanAction navigation, Craft state,
                               Context context) {

        var canAdvance = state.currentStage < context.stageCount;
        if (!state.stageFinished(context) && !(canAdvance && state.activeSegments(context).isEmpty())) return false;
        var finishedStage = state.currentStage;
        var detachedBoosters = state.boostersEndingCurrentStage(context);
        for (var booster : detachedBoosters) disconnectBooster(navigation, booster, state, context, finishedStage);
        if (!canAdvance) return !detachedBoosters.isEmpty();
        state.currentStage++;
        return true;
    }

    /**
     * Attach an asteroid only after the preceding arrival established a slow, close approach.
     */
    private static boolean connectAsteroid(SpaceSimulation.FlightPlanAction action, Craft state,
                                           Context context) {

        var asteroid = context.objects.get(action.targetId());
        var relativeSpeed = asteroid == null ? Double.POSITIVE_INFINITY
                : Math.hypot(state.velocityX - asteroid.velocityX(), state.velocityY - asteroid.velocityY());
        if (asteroid == null || asteroid.type() != SpaceObjects.ObjectType.ASTEROID) {
            state.blockedState = TerminalState.TARGET_UNAVAILABLE;
            return false;
        }
        if (asteroid.detectionState() != SpaceObjects.DetectionState.PRECISE) {
            state.blockedState = TerminalState.PRECISE_POSITION_REQUIRED;
            return false;
        }
        if (state.attachedAsteroid != null) {
            state.blockedState = TerminalState.ASTEROID_ALREADY_ATTACHED;
            return false;
        }
        if (!asteroid.id().equals(state.currentTarget)
                || state.currentOrbit != SpaceSimulation.OrbitBand.SURFACE && state.currentOrbit != SpaceSimulation.OrbitBand.TIGHT
                || !AsteroidImpactRules.canConnectAsteroid(relativeSpeed)) {
            state.blockedState = TerminalState.UNSAFE_ASTEROID_APPROACH;
            return false;
        }
        if (action.segments().size() != 1 || !state.segments.containsKey(action.segments().getFirst())) {
            state.blockedState = TerminalState.NO_ASTEROID_ANCHOR;
            return false;
        }
        state.attachedAsteroid = asteroid;
        state.asteroidAnchor = action.segments().getFirst();
        state.addSample(PathPhase.COAST, asteroid.id(), action.id());
        return true;
    }

    private static void disconnectBooster(SpaceSimulation.FlightPlanAction navigation,
                                          SpaceSimulation.SegmentRef booster, Craft state,
                                          Context context, int stage) {

        if (!state.segments.containsKey(booster)) return;
        // Copy before removing: the detached craft inherits velocity, stage and any anchored asteroid.
        var detached = state.copyFor(Set.of(booster));
        state.removeSegment(booster);
        if (booster.equals(state.asteroidAnchor)) state.clearAsteroidAttachment();

        UUID eventId = boosterEventId(navigation.id(), booster);
        var child = context.branchesByParent.get(eventId);
        if (child == null) child = new SpaceSimulation.FlightPlanBranch(eventId, eventId, List.of());
        context.boosterEvents.add(new BoosterEvent(eventId, state.branchId, child.id(), navigation.id(), booster,
                stage, state.time, state.x, state.y));
        assignChildJobs(detached, child.id());
        simulateBranch(child, detached, context);
    }

    private static UUID boosterEventId(UUID navigationAction, SpaceSimulation.SegmentRef booster) {

        var key = navigationAction + ":" + booster.anchor().asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean separate(SpaceSimulation.FlightPlanAction action, Craft state,
                                    Context context) {

        if (state.attachedAsteroid != null && action.targetId().equals(state.attachedAsteroid.id())
                && action.segments().size() == 1 && action.segments().getFirst().equals(state.asteroidAnchor)) {
            context.asteroidPaths.add(ReleasedAsteroidPrediction.predict(state, context));
            state.clearAsteroidAttachment();
            state.addSample(PathPhase.COAST, action.targetId(), action.id());
            return true;
        }
        if (action.segments().size() != 2) {
            state.blockedState = TerminalState.NO_COUPLING;
            return false;
        }
        var retainedRef = action.segments().get(0);
        var detachedRef = action.segments().get(1);
        if (!state.segments.containsKey(retainedRef) || !state.segments.containsKey(detachedRef)
                || !state.connections.getOrDefault(retainedRef, Set.of()).contains(detachedRef)) {
            state.blockedState = TerminalState.NO_COUPLING;
            return false;
        }

        if (!RocketDocking.retainedAfterCut(state.connections, retainedRef, detachedRef).containsAll(state.occupiedPortSegments))
            return false;
        state.connections.get(retainedRef).remove(detachedRef);
        state.connections.get(detachedRef).remove(retainedRef);
        var retained = state.connectedComponent(retainedRef);
        if (retained.contains(detachedRef)) return true;
        var detached = state.connectedComponent(detachedRef);
        var detachedState = state.copyFor(detached);
        detachedState.dockedHost = SpaceSimulation.FlightPlanAction.NO_TARGET;
        detachedState.dockedHostPort = RocketServiceSettings.HardwareRef.NONE;
        detachedState.occupiedPortSegments.clear();
        detachedState.dockedVisitorMass = 0;
        state.retain(retained);

        var child = context.branchesByParent.get(action.id());
        if (child == null) child = new SpaceSimulation.FlightPlanBranch(action.id(), action.id(), List.of());
        assignChildJobs(detachedState, child.id());
        simulateBranch(child, detachedState, context);
        return true;
    }

    /**
     * Releases a carried cube-sat like a decouple. The host loses the folded mass, and the cube continues from the
     * host's position and velocity with its real mass and the resources captured when it was shrunk.
     */
    private static boolean deploy(SpaceSimulation.FlightPlanAction action, Craft state, Context context) {

        var holder = state.segments.values().stream().filter(segment -> segment.physical != null
                && segment.physical.payloads.containsKey(action.payload())).findFirst().orElse(null);
        if (holder == null) {
            state.blockedState = TerminalState.NO_PAYLOAD;
            return false;
        }
        var cube = holder.physical.payloads.remove(action.payload());
        holder.physical.layout.put(action.payload(), Blocks.AIR.defaultBlockState());
        holder.dryMass = Math.max(0, holder.dryMass - RocketLayout.mass(SpaceAgeBlocks.CUBE_SAT.get().defaultBlockState())
                * RocketPerformanceCalculator.KILOGRAMS_PER_WEIGHT_UNIT);
        state.addSample(PathPhase.COAST, state.currentTarget, action.id());

        var child = context.branchesByParent.get(action.id());
        if (child == null) child = new SpaceSimulation.FlightPlanBranch(action.id(), action.id(), List.of());
        var deployed = createInitialCraft(RocketPayloads.unfold(action.payload(), cube), context.objects.get(SpaceObjects.EARTH_ID),
                child, context.objects);
        deployed.x = state.x;
        deployed.y = state.y;
        deployed.time = state.time;
        deployed.velocityX = state.velocityX;
        deployed.velocityY = state.velocityY;
        deployed.headingX = state.headingX;
        deployed.headingY = state.headingY;
        deployed.atmosphere = state.atmosphere;
        deployed.currentTarget = state.currentTarget;
        deployed.currentOrbit = state.currentOrbit;
        deployed.lastEarthSurfaceAction = state.lastEarthSurfaceAction;
        assignChildJobs(deployed, child.id());
        simulateBranch(child, deployed, context);
        return true;
    }

    private static void assignChildJobs(Craft craft, UUID childId) {

        var previous = craft.craftId;
        craft.segments.values().forEach(s -> s.crafters.values().forEach(c -> c.reassignOperator(previous, childId)));
        craft.craftId = childId;
    }

    public enum StationKeepingEnd {

        SUSTAINABLE,
        DURATION,
        RF,
        FUEL,
        LOW_RF,
        LOW_FUEL
    }

    public enum PathPhase {

        ACCELERATE,
        REDIRECT,
        COAST,
        BRAKE,
        PARACHUTE
    }

    public enum TerminalState {

        SOLAR_REQUIRED,
        BEAM_APERTURE_REQUIRED,
        BEAM_STATIONARY_REQUIRED,
        READY,
        SUSTAINABLE,
        STATION_KEEPING_EXHAUSTED,
        DISCARDED,
        DESTROYED,
        PLAN_BLOCKED,
        TARGET_UNAVAILABLE,
        EMPTY_CRAFT,
        ASTEROID_ALREADY_ATTACHED,
        UNSAFE_ASTEROID_APPROACH,
        NO_ASTEROID_ANCHOR,
        NO_COUPLING,
        NO_PAYLOAD,
        NO_SCAN_TARGET,
        SCAN_OUT_OF_RANGE,
        NO_SCANNER,
        NO_ANTENNA,
        NOT_ENOUGH_SERVICE_RF,
        PRECISE_POSITION_REQUIRED,
        NOT_ENOUGH_DELTA_V,
        NO_ACTIVE_ENGINES,
        NO_FEASIBLE_TRANSFER,
        INTEGRATION_STEP_LIMIT,
        INTEGRATION_TIME_LIMIT;

        public boolean isFailure() {

            return this != READY && this != SUSTAINABLE && this != STATION_KEEPING_EXHAUSTED && this != DISCARDED;
        }

        /**
         * Destruction may be the requested result of an impact mission.
         */
        public boolean preventsLaunch() {

            return isFailure() && this != DESTROYED;
        }
    }

    /**
     * @param timeSeconds          Seconds since the root branch started.
     * @param x                    X position in the simulation plane.
     * @param y                    Y position in the simulation plane.
     * @param speedMetersPerSecond Speed magnitude for display.
     * @param velocityX            X velocity in meters per second.
     * @param velocityY            Y velocity in meters per second.
     * @param phase                Engine phase for the interval ending at this sample.
     * @param targetId             Navigation target, or NO_TARGET.
     * @param actionId             Card which produced this result.
     * @param stage                One-based engine stage.
     * @param connectedSegments    Segments attached at this exact time.
     * @param firingSegments       Segments burning during this interval.
     * @param attachedAsteroidId   Carried asteroid, or NO_TARGET.
     */
    public record PathSample(double timeSeconds, double x, double y, double speedMetersPerSecond,
                             double velocityX, double velocityY, PathPhase phase, UUID targetId, UUID actionId,
                             int stage, Set<SpaceSimulation.SegmentRef> connectedSegments,
                             Set<SpaceSimulation.SegmentRef> firingSegments, UUID attachedAsteroidId) {

        public static final Codec<PathSample> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("timeSeconds").forGetter(PathSample::timeSeconds),
                Codec.DOUBLE.fieldOf("x").forGetter(PathSample::x),
                Codec.DOUBLE.fieldOf("y").forGetter(PathSample::y),
                Codec.DOUBLE.fieldOf("speedMetersPerSecond").forGetter(PathSample::speedMetersPerSecond),
                Codec.DOUBLE.fieldOf("velocityX").forGetter(PathSample::velocityX),
                Codec.DOUBLE.fieldOf("velocityY").forGetter(PathSample::velocityY),
                Codec.STRING.xmap(PathPhase::valueOf, PathPhase::name).fieldOf("phase").forGetter(PathSample::phase),
                UUIDUtil.STRING_CODEC.fieldOf("targetId").forGetter(PathSample::targetId),
                UUIDUtil.STRING_CODEC.fieldOf("actionId").forGetter(PathSample::actionId),
                Codec.INT.fieldOf("stage").forGetter(PathSample::stage),
                SEGMENT_SET_CODEC.fieldOf("connectedSegments").forGetter(PathSample::connectedSegments),
                SEGMENT_SET_CODEC.fieldOf("firingSegments").forGetter(PathSample::firingSegments),
                UUIDUtil.STRING_CODEC.fieldOf("attachedAsteroidId").forGetter(PathSample::attachedAsteroidId)
        ).apply(i, PathSample::new));

    }

    /**
     * @param branchId           Branch owning this result.
     * @param actionIndex        Card position within the branch.
     * @param actionId           Card which produced this result.
     * @param timeSeconds        Seconds since the root branch started.
     * @param x                  X position in the simulation plane.
     * @param y                  Y position in the simulation plane.
     * @param completed          Whether this card finished successfully.
     * @param connectedSegments  Segments attached at this exact time.
     * @param attachedAsteroidId Carried asteroid, or NO_TARGET.
     */
    public record ActionMoment(UUID branchId, int actionIndex, UUID actionId, double timeSeconds,
                               double x, double y, boolean completed,
                               Set<SpaceSimulation.SegmentRef> connectedSegments, UUID attachedAsteroidId) {

        public static final Codec<ActionMoment> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("branchId").forGetter(ActionMoment::branchId),
                Codec.INT.fieldOf("actionIndex").forGetter(ActionMoment::actionIndex),
                UUIDUtil.STRING_CODEC.fieldOf("actionId").forGetter(ActionMoment::actionId),
                Codec.DOUBLE.fieldOf("timeSeconds").forGetter(ActionMoment::timeSeconds),
                Codec.DOUBLE.fieldOf("x").forGetter(ActionMoment::x),
                Codec.DOUBLE.fieldOf("y").forGetter(ActionMoment::y),
                Codec.BOOL.fieldOf("completed").forGetter(ActionMoment::completed),
                SEGMENT_SET_CODEC.fieldOf("connectedSegments").forGetter(ActionMoment::connectedSegments),
                UUIDUtil.STRING_CODEC.fieldOf("attachedAsteroidId").forGetter(ActionMoment::attachedAsteroidId)
        ).apply(i, ActionMoment::new));

    }

    /**
     * @param id                 Stable event ID linking the separation to its child program.
     * @param branchId           Branch owning this result.
     * @param childBranchId      Detached craft's branch.
     * @param navigationActionId Navigation card active when the booster ran out.
     * @param segment            Booster which separated.
     * @param stage              One-based engine stage.
     * @param timeSeconds        Seconds since the root branch started.
     * @param x                  X position in the simulation plane.
     * @param y                  Y position in the simulation plane.
     */
    public record BoosterEvent(UUID id, UUID branchId, UUID childBranchId, UUID navigationActionId,
                               SpaceSimulation.SegmentRef segment, int stage,
                               double timeSeconds, double x, double y) {

    }

    /**
     * @param branchId        Branch owning this result.
     * @param segments        Segments remaining at the end of this branch.
     * @param samples         Ordered motion samples.
     * @param actionMoments   Exact card outcomes and attachment states.
     * @param durationSeconds End time on the shared preview clock.
     * @param remainingDeltaV Remaining velocity change budget with future stages included.
     * @param terminalState   Why this branch ended.
     */
    public record CraftPath(UUID branchId, Set<SpaceSimulation.SegmentRef> segments,
                            List<PathSample> samples, List<ActionMoment> actionMoments,
                            double durationSeconds, double remainingDeltaV, TerminalState terminalState, Optional<NavigationFailure> failure) {

        public CraftPath(UUID branchId, Set<SpaceSimulation.SegmentRef> segments, List<PathSample> samples,
                         List<ActionMoment> actionMoments, double durationSeconds, double remainingDeltaV, TerminalState terminalState) {

            this(branchId, segments, samples, actionMoments, durationSeconds, remainingDeltaV, terminalState, Optional.empty());
        }

        public static final Codec<CraftPath> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.STRING_CODEC.fieldOf("branchId").forGetter(CraftPath::branchId),
                SEGMENT_SET_CODEC.fieldOf("segments").forGetter(CraftPath::segments),
                PathSample.CODEC.listOf().fieldOf("samples").forGetter(CraftPath::samples),
                ActionMoment.CODEC.listOf().fieldOf("actionMoments").forGetter(CraftPath::actionMoments),
                Codec.DOUBLE.fieldOf("durationSeconds").forGetter(CraftPath::durationSeconds),
                Codec.DOUBLE.fieldOf("remainingDeltaV").forGetter(CraftPath::remainingDeltaV),
                Codec.STRING.xmap(TerminalState::valueOf, TerminalState::name).fieldOf("terminalState").forGetter(CraftPath::terminalState),
                NavigationFailure.CODEC.optionalFieldOf("failure").forGetter(CraftPath::failure)
        ).apply(i, CraftPath::new));

    }

    /** Numeric cause of an infeasible transfer, shared by cards, validation and network forecasts. */
    public record NavigationFailure(String reason, double actual, double limit) {

        public static final Codec<NavigationFailure> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("reason").forGetter(NavigationFailure::reason),
                Codec.DOUBLE.fieldOf("actual").forGetter(NavigationFailure::actual),
                Codec.DOUBLE.fieldOf("limit").forGetter(NavigationFailure::limit)
        ).apply(i, NavigationFailure::new));

        public MutableComponent description() {

            var days = SpaceBalance.DAY / 20d;
            return reason.equals("time_limit") ? Component.translatable("screen.oritech_space_age.transfer.time_limit",
                    number(actual / days), number(limit / days), number((actual - limit) / days))
                    : Component.translatable("screen.oritech_space_age.transfer." + reason, number(actual), number(limit));
        }

        private static String number(double value) {

            return String.format(Locale.ROOT, "%.2f", value);
        }
    }

    /**
     * @param paths              Craft previews in plan order.
     * @param boosterEvents      Automatic booster separations.
     * @param lastCommandSeconds Latest card completion time across all branches.
     * @param arrivalPredictions Surface arrival outcomes.
     * @param asteroidPaths      Motion after asteroids are released.
     * @param navigationAborts   Cards completed early by an addon.
     */
    public record FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents,
                             double lastCommandSeconds, List<ArrivalPrediction> arrivalPredictions,
                             List<AsteroidPath> asteroidPaths, List<NavigationAbortMoment> navigationAborts,
                             List<ScanEstimate> scanEstimates,
                             List<StationKeepingEstimate> stationKeepingEstimates,
                             List<ProcessingEstimate> processingEstimates) {

        public FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents, double lastCommandSeconds,
                          List<ArrivalPrediction> arrivalPredictions, List<AsteroidPath> asteroidPaths,
                          List<NavigationAbortMoment> navigationAborts) {

            this(paths, boosterEvents, lastCommandSeconds, arrivalPredictions, asteroidPaths, navigationAborts,
                    List.of(), List.of(), List.of());
        }

        public FlightPath(List<CraftPath> paths, List<BoosterEvent> boosterEvents, double lastCommandSeconds) {

            this(paths, boosterEvents, lastCommandSeconds, List.of(), List.of(), List.of());
        }
    }

    /**
     * The processing machine and the input pair it was assigned.
     */
    public record WorkAssignment(RocketServiceSettings.HardwareRef machine, RocketControllerState.Pair source) {

    }

    public record ProcessingEstimate(UUID actionId, List<Identifier> products, double ticks,
                                     long rf,
                                     long remainingModuleCraftRF, String issue, boolean conditional,
                                     List<WorkAssignment> assignments, List<String> skipped) {

        public ProcessingEstimate(UUID actionId, List<Identifier> products, double ticks, long rf,
                                  long remainingRF, String issue, boolean conditional) {

            this(actionId, products, ticks, rf, remainingRF, issue, conditional, List.of(), List.of());
        }

        public ProcessingEstimate {

            assignments = List.copyOf(assignments);
            skipped = List.copyOf(skipped);
        }
    }

    public record ScanEstimate(UUID actionId, long availableRF, int scanners) {

    }

    public record StationKeepingEstimate(UUID actionId, double durationSeconds, StationKeepingEnd end) {

    }

    /**
     * @param actionId Card which produced this result.
     * @param branchId Branch owning this result.
     * @param targetId Navigation target, or NO_TARGET.
     * @param impact   Predicted surface arrival outcome.
     */
    public record ArrivalPrediction(UUID actionId, UUID branchId, UUID targetId,
                                    AsteroidImpactRules.ImpactPrediction impact, ParachuteLanding.Result parachutes) {

    }

    /**
     * @param asteroidId               Asteroid being followed.
     * @param samples                  Ordered motion samples.
     * @param earthImpact              Predicted Earth impact, or null if none was found.
     * @param landingUncertaintyBlocks Landing spread estimated at the release point.
     */
    public record AsteroidPath(UUID asteroidId, List<MotionSample> samples,
                               AsteroidImpactRules.ImpactPrediction earthImpact, int landingUncertaintyBlocks) {

    }

    /**
     * @param actionId     Card which produced this result.
     * @param branchId     Branch owning this result.
     * @param addonId      Condition which stopped navigation.
     * @param actualValue  Measured value when the condition became true.
     * @param timeSeconds  Seconds since the root branch started.
     * @param x            X position in the simulation plane.
     * @param y            Y position in the simulation plane.
     * @param destinationX Original goal X before stopping early.
     * @param destinationY Original goal Y before stopping early.
     */
    public record NavigationAbortMoment(UUID actionId, UUID branchId, UUID addonId, double actualValue,
                                        double timeSeconds, double x, double y,
                                        double destinationX, double destinationY) {

    }

    /**
     * @param timeSeconds          Seconds since the root branch started.
     * @param x                    X position in the simulation plane.
     * @param y                    Y position in the simulation plane.
     * @param speedMetersPerSecond Speed magnitude for display.
     */
    public record MotionSample(double timeSeconds, double x, double y, double speedMetersPerSecond) {

    }
}
