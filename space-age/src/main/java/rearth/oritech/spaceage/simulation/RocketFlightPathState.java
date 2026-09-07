package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.ActionMoment;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.ArrivalPrediction;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.AsteroidPath;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.BoosterEvent;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.NavigationAbortMoment;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.TerminalState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Mutable state used while one flight-plan preview is being compiled. */
final class RocketFlightPathState {

    // Small enough to treat floating point leftovers as empty fuel.
    static final double BURN_TOLERANCE = 1e-9;

    private RocketFlightPathState() {
    }

    /** Shared inputs and collected results for every branch in this preview. */
    static final class Context {
        // Targets, child programs, and stage settings read by every branch.
        final Map<UUID, SpaceSimulation.SpaceObjectData> objects;
        final Map<UUID, SpaceSimulation.FlightPlanBranch> branchesByParent;
        final Map<SpaceSimulation.SegmentRef, SpaceSimulation.SegmentConfiguration> configurations;
        // Last configured stage stops empty-stage skipping from running forever.
        final int stageCount;
        // Results collected while branches split and finish.
        final Map<UUID, CraftPath> paths = new LinkedHashMap<>();
        final List<BoosterEvent> boosterEvents = new ArrayList<>();
        final List<ArrivalPrediction> arrivalPredictions = new ArrayList<>();
        final List<AsteroidPath> asteroidPaths = new ArrayList<>();
        final List<NavigationAbortMoment> navigationAborts = new ArrayList<>();

        Context(Map<UUID, SpaceSimulation.SpaceObjectData> objects,
                Map<UUID, SpaceSimulation.FlightPlanBranch> branchesByParent,
                Map<SpaceSimulation.SegmentRef, SpaceSimulation.SegmentConfiguration> configurations,
                int stageCount) {
            this.objects = objects;
            this.branchesByParent = branchesByParent;
            this.configurations = configurations;
            this.stageCount = stageCount;
        }

        SpaceSimulation.SegmentConfiguration configuration(SpaceSimulation.SegmentRef ref) {
            return configurations.getOrDefault(ref,
                    new SpaceSimulation.SegmentConfiguration(ref, "", false, List.of(1)));
        }
    }

    /** Remaining resources for one attached segment. */
    static final class Segment {
        // Fixed mass and whether this segment can provide thrust.
        final double wetMass;
        final double thrust;
        // Remaining full-power resources for this segment.
        double remainingDeltaV;
        double remainingBurnSeconds;

        Segment(double wetMass, double thrust, double remainingDeltaV, double remainingBurnSeconds) {
            this.wetMass = wetMass;
            this.thrust = thrust;
            this.remainingDeltaV = remainingDeltaV;
            this.remainingBurnSeconds = remainingBurnSeconds;
        }

        Segment copy() {
            return new Segment(wetMass, thrust, remainingDeltaV, remainingBurnSeconds);
        }
    }

    /** The moving craft and its branch-local history. */
    static final class Craft {
        // Attached parts and their coupling graph. Separations split this graph.
        final Map<SpaceSimulation.SegmentRef, Segment> segments;
        final Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections;
        // Only this branch's visual history and card outcomes.
        final List<PathSample> samples = new ArrayList<>();
        final List<ActionMoment> actionMoments = new ArrayList<>();
        // Branch currently writing this state.
        UUID branchId;
        // Shared-plane position, clock, and velocity carried between cards.
        double x;
        double y;
        double time;
        double velocityX;
        double velocityY;
        // One-based stage selects which configured engines may fire.
        int currentStage = 1;
        // Terminal cards and unsafe arrivals stop this branch.
        boolean maintainingPosition;
        boolean discarded;
        boolean destroyed;
        // Latest successful destination validates later asteroid connections.
        UUID currentTarget = SpaceSimulation.FlightPlanAction.NO_TARGET;
        SpaceSimulation.OrbitBand currentOrbit = SpaceSimulation.OrbitBand.LOW;
        // Attached asteroid and its anchor segment, if this craft carries one.
        SpaceSimulation.SpaceObjectData attachedAsteroid;
        SpaceSimulation.SegmentRef asteroidAnchor;
        // Landing settings stay with a released asteroid for its impact prediction.
        SpaceSimulation.FlightPlanAction lastEarthSurfaceAction;
        // Failure reason shown when a card cannot complete.
        TerminalState blockedState = TerminalState.PLAN_BLOCKED;

        Craft(Map<SpaceSimulation.SegmentRef, Segment> segments,
              Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections,
              double x, double y, double time) {
            this.segments = segments;
            this.connections = connections;
            this.x = x;
            this.y = y;
            this.time = time;
        }

        double mass() {
            var asteroidMass = attachedAsteroid == null ? 0
                    : attachedAsteroid.mass() * AsteroidImpactRules.KILOGRAMS_PER_ASTEROID_MASS;
            return rocketMass() + asteroidMass;
        }

        double rocketMass() {
            return segments.values().stream().mapToDouble(segment -> segment.wetMass).sum();
        }

        List<SpaceSimulation.SegmentRef> activeSegments(Context context) {
            return segments.keySet().stream()
                    .filter(ref -> context.configuration(ref).usesEnginesDuring(currentStage))
                    .filter(ref -> segments.get(ref).thrust > 0
                            && segments.get(ref).remainingBurnSeconds > BURN_TOLERANCE
                            && segments.get(ref).remainingDeltaV > BURN_TOLERANCE)
                    .toList();
        }

        double deltaVPerSecond(List<SpaceSimulation.SegmentRef> active) {
            var mass = Math.max(1, mass());
            var result = 0.0;
            for (var ref : active) {
                var segment = segments.get(ref);
                result += segment.remainingDeltaV / Math.max(BURN_TOLERANCE, segment.remainingBurnSeconds)
                        * segment.wetMass / mass;
            }
            return result;
        }

        void advance(boolean burning, double directionX, double directionY, double acceleration,
                     List<SpaceSimulation.SegmentRef> active, double seconds) {
            var accelerationX = burning ? directionX * acceleration : 0;
            var accelerationY = burning ? directionY * acceleration : 0;
            x += velocityX * seconds + accelerationX * seconds * seconds * 0.5;
            y += velocityY * seconds + accelerationY * seconds * seconds * 0.5;
            velocityX += accelerationX * seconds;
            velocityY += accelerationY * seconds;
            time += seconds;
            if (burning) consumeBurnTime(active, seconds);
        }

        void consumeBurnTime(List<SpaceSimulation.SegmentRef> active, double seconds) {
            for (var ref : active) {
                var segment = segments.get(ref);
                var fraction = Math.min(1, seconds / Math.max(BURN_TOLERANCE, segment.remainingBurnSeconds));
                segment.remainingDeltaV *= 1 - fraction;
                segment.remainingBurnSeconds = Math.max(0, segment.remainingBurnSeconds - seconds);
            }
        }

        boolean stageFinished(Context context) {
            var firingBoosters = segments.keySet().stream().filter(ref -> {
                var configuration = context.configuration(ref);
                return configuration.booster() && configuration.usesEnginesDuring(currentStage);
            }).toList();
            var endingBoosters = firingBoosters.stream()
                    .filter(ref -> context.configuration(ref).lastEngineStage() == currentStage).toList();
            if (!endingBoosters.isEmpty()) return endingBoosters.stream().allMatch(this::isEmpty);
            return !firingBoosters.isEmpty() && firingBoosters.stream().allMatch(this::isEmpty);
        }

        List<SpaceSimulation.SegmentRef> boostersEndingCurrentStage(Context context) {
            return segments.keySet().stream().filter(ref -> {
                var configuration = context.configuration(ref);
                return configuration.booster() && (configuration.lastEngineStage() <= currentStage || isEmpty(ref));
            }).toList();
        }

        private boolean isEmpty(SpaceSimulation.SegmentRef ref) {
            var segment = segments.get(ref);
            return segment.remainingBurnSeconds <= BURN_TOLERANCE || segment.remainingDeltaV <= BURN_TOLERANCE;
        }

        double availableDeltaV(Context context) {
            return burnProfile(context).deltaV();
        }

        RocketBurnProfile burnProfile(Context context) {
            // Plan braking against a copy so looking ahead never consumes the real craft.
            var copy = copyFor(Set.copyOf(segments.keySet()));
            var intervals = new ArrayList<RocketBurnProfile.Interval>();
            while (true) {
                var active = copy.activeSegments(context);
                if (copy.currentStage < context.stageCount && (copy.stageFinished(context) || active.isEmpty())) {
                    for (var ref : copy.boostersEndingCurrentStage(context)) copy.removeSegment(ref);
                    copy.currentStage++;
                    continue;
                }
                if (active.isEmpty()) break;
                var rate = copy.deltaVPerSecond(active);
                if (rate <= 0) break;
                var seconds = active.stream().map(copy.segments::get)
                        .mapToDouble(segment -> segment.remainingBurnSeconds).min().orElse(0);
                if (seconds <= 0) break;
                copy.consumeBurnTime(active, seconds);
                intervals.add(new RocketBurnProfile.Interval(rate, seconds));
                if (copy.stageFinished(context)) {
                    for (var ref : copy.boostersEndingCurrentStage(context)) copy.removeSegment(ref);
                    copy.currentStage = Math.min(context.stageCount, copy.currentStage + 1);
                }
            }
            return new RocketBurnProfile(intervals);
        }

        void removeSegment(SpaceSimulation.SegmentRef ref) {
            segments.remove(ref);
            for (var neighbour : connections.getOrDefault(ref, Set.of())) {
                var neighbourConnections = connections.get(neighbour);
                if (neighbourConnections != null) neighbourConnections.remove(ref);
            }
            connections.remove(ref);
        }

        Set<SpaceSimulation.SegmentRef> connectedComponent(SpaceSimulation.SegmentRef start) {
            var result = new LinkedHashSet<SpaceSimulation.SegmentRef>();
            var open = new ArrayList<SpaceSimulation.SegmentRef>();
            open.add(start);
            while (!open.isEmpty()) {
                var current = open.removeLast();
                if (!result.add(current)) continue;
                connections.getOrDefault(current, Set.of()).stream().filter(neighbour -> !result.contains(neighbour))
                        .forEach(open::add);
            }
            return result;
        }

        Craft copyFor(Set<SpaceSimulation.SegmentRef> component) {
            var copiedSegments = new LinkedHashMap<SpaceSimulation.SegmentRef, Segment>();
            var copiedConnections = new HashMap<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>>();
            for (var ref : component) {
                copiedSegments.put(ref, segments.get(ref).copy());
                var neighbours = new LinkedHashSet<>(connections.getOrDefault(ref, Set.of()));
                neighbours.retainAll(component);
                copiedConnections.put(ref, neighbours);
            }
            var copy = new Craft(copiedSegments, copiedConnections, x, y, time);
            copy.velocityX = velocityX;
            copy.velocityY = velocityY;
            copy.currentStage = currentStage;
            copy.currentTarget = currentTarget;
            copy.currentOrbit = currentOrbit;
            if (asteroidAnchor != null && component.contains(asteroidAnchor)) {
                copy.attachedAsteroid = attachedAsteroid;
                copy.asteroidAnchor = asteroidAnchor;
            }
            copy.lastEarthSurfaceAction = lastEarthSurfaceAction;
            return copy;
        }

        void retain(Set<SpaceSimulation.SegmentRef> retained) {
            segments.keySet().removeIf(ref -> !retained.contains(ref));
            connections.keySet().removeIf(ref -> !retained.contains(ref));
            connections.values().forEach(neighbours -> neighbours.retainAll(retained));
            if (asteroidAnchor != null && !retained.contains(asteroidAnchor)) clearAsteroidAttachment();
        }

        void clearAsteroidAttachment() {
            attachedAsteroid = null;
            asteroidAnchor = null;
        }

        void addSample(PathPhase phase, UUID target, UUID actionId) {
            samples.add(createSample(phase, target, actionId, Set.of()));
        }

        PathSample createSample(PathPhase phase, UUID target,
                                                            UUID actionId,
                                                            Set<SpaceSimulation.SegmentRef> firingSegments) {
            return new PathSample(time, x, y, Math.hypot(velocityX, velocityY), velocityX,
                    velocityY, phase, target, actionId, currentStage, Set.copyOf(segments.keySet()), firingSegments,
                    attachedAsteroid == null ? SpaceSimulation.FlightPlanAction.NO_TARGET : attachedAsteroid.id());
        }

        CraftPath toPath(TerminalState terminal,
                                                     Context context) {
            return new CraftPath(branchId, Set.copyOf(segments.keySet()), List.copyOf(samples),
                    List.copyOf(actionMoments), time, availableDeltaV(context), terminal);
        }
    }
}
