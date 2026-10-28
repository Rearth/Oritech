package rearth.oritech.spaceage.simulation.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.spaceage.recipe.VacuumRecipe;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.ActionMoment;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.ArrivalPrediction;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.AsteroidPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.BoosterEvent;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.CraftPath;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.NavigationAbortMoment;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.NavigationFailure;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathPhase;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.PathSample;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.ProcessingEstimate;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.ScanEstimate;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.StationKeepingEstimate;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.TerminalState;
import rearth.oritech.spaceage.simulation.flight.RocketTransferRoute.Burn;
import rearth.oritech.spaceage.simulation.mission.DockingTarget;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketControllerState;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.StaticRocketSegment;
import rearth.oritech.spaceage.simulation.service.RocketServiceSettings;
import rearth.oritech.spaceage.simulation.service.StationServiceRules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable state used while one flight-plan preview is being compiled.
 */
public final class RocketFlightPathState {

    // Small enough to treat floating point leftovers as empty fuel.
    public static final double BURN_TOLERANCE = 1e-9;

    private RocketFlightPathState() {
    }

    /**
     * Shared inputs and collected results for every branch in this preview.
     */
    public static final class Context {

        // Targets, child programs, and stage settings read by every branch.
        public final Map<UUID, SpaceSimulation.SpaceObjectData> objects;
        final Map<UUID, SpaceSimulation.FlightPlanBranch> branchesByParent;
        final Map<SpaceSimulation.SegmentRef, SpaceSimulation.SegmentConfiguration> configurations;
        // Last configured stage stops empty-stage skipping from running forever.
        final int stageCount;
        public final Map<UUID, DockingTarget> stations = new HashMap<>();
        public final Map<UUID, Craft> stationCraft = new HashMap<>();
        public final Map<UUID, Set<RocketServiceSettings.HardwareRef>> claimedPorts = new HashMap<>();
        // Results collected while branches split and finish.
        final Map<UUID, CraftPath> paths = new LinkedHashMap<>();
        final List<BoosterEvent> boosterEvents = new ArrayList<>();
        final List<ArrivalPrediction> arrivalPredictions = new ArrayList<>();
        final List<AsteroidPath> asteroidPaths = new ArrayList<>();
        final List<NavigationAbortMoment> navigationAborts = new ArrayList<>();
        public final List<RocketFlightPathCalculator.ProcessingEstimate> processingEstimates = new ArrayList<>();
        final List<RocketFlightPathCalculator.ScanEstimate> scanEstimates = new ArrayList<>();
        final List<RocketFlightPathCalculator.StationKeepingEstimate> stationKeepingEstimates = new ArrayList<>();
        public Collection<RecipeHolder<VacuumRecipe>> recipes = List.of();

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

    /**
     * Remaining resources for one attached segment.
     */
    public static final class Segment {

        public final Map<BlockPos, RocketControllerState> crafters = new HashMap<>();
        final double chemicalThrust;
        final double ionThrust;
        public final RocketHardware hardware;
        public double wetMass;
        public double fuelMass;
        public double dryMass;
        public double payloadEnergy;
        public double fuelTicks;
        int parachutes;
        public StaticRocketSegment structure;
        public DynamicRocketSegment physical;
        public double chemicalSeconds;
        public double ionSeconds;
        public double rf;
        public double initialRF;
        public double initialFuelTicks;

        public Segment(double mass, double chemicalThrust, double ionThrust, double chemicalSeconds, double ionSeconds,
                RocketHardware hardware) {

            this.wetMass = mass;
            this.chemicalThrust = chemicalThrust;
            this.ionThrust = ionThrust;
            this.chemicalSeconds = chemicalSeconds;
            this.ionSeconds = ionSeconds;
            this.hardware = hardware;
        }

        double seconds() {

            return Math.min(chemicalSeconds > BURN_TOLERANCE ? chemicalSeconds : Double.POSITIVE_INFINITY,
                    ionSeconds > BURN_TOLERANCE ? ionSeconds : Double.POSITIVE_INFINITY);
        }

        double burnSeconds(double fuelFactor) {

            var seconds = seconds() / fuelFactor;
            if (ionSeconds <= BURN_TOLERANCE) return seconds;
            var drain = hardware.rfCost(fuelFactor) + StationServiceRules.antennaRF(hardware, 20);
            return Math.min(seconds, rf / drain);
        }

        public double thrust(boolean atmosphere) {

            return (chemicalSeconds > BURN_TOLERANCE ? chemicalThrust : 0)
                    + (ionSeconds > BURN_TOLERANCE ? ionThrust * (atmosphere ? SpaceBalance.ION_ATMOSPHERE : 1) : 0);
        }

        void consumeFuel(double seconds) {

            var previous = chemicalSeconds;
            chemicalSeconds = Math.max(0, previous - seconds);
            if (previous > 0) {
                fuelMass *= chemicalSeconds / previous;
                fuelTicks *= chemicalSeconds / previous;
            }
        }

        public void consume(double seconds) {

            consumeFuel(seconds);
            rf = Math.max(0, rf - hardware.rfCost(Math.min(ionSeconds, seconds)));
            ionSeconds = Math.max(0, ionSeconds - seconds);
        }

        /** Current layouts may have changed their battery capacity since launch. */
        public double rfCapacity() {

            return physical == null ? initialRF : physical.rfCapacity;
        }

        public void spendRF(double amount) {

            rf = Math.max(0, rf - amount);
            if (hardware.ion() > 0) ionSeconds = Math.min(ionSeconds, rf / hardware.rfCost(1));
        }

        Segment copy() {

            var copy = new Segment(wetMass, chemicalThrust, ionThrust, chemicalSeconds, ionSeconds, hardware);
            copy.structure = structure;
            copy.physical = physical == null ? null : physical.copy();
            copy.rf = rf;
            copy.fuelMass = fuelMass;
            copy.dryMass = dryMass;
            copy.payloadEnergy = payloadEnergy;
            copy.fuelTicks = fuelTicks;
            copy.parachutes = parachutes;
            crafters.forEach((pos, cargo) -> copy.crafters.put(pos, cargo.copy()));
            copy.initialRF = initialRF;
            copy.initialFuelTicks = initialFuelTicks;
            return copy;
        }
    }

    /**
     * The moving craft and its branch-local history.
     */
    public static final class Craft {

        // Attached parts and their coupling graph. Separations split this graph.
        public final Map<SpaceSimulation.SegmentRef, Segment> segments;
        final Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections;
        // Only this branch's visual history and card outcomes.
        final List<PathSample> samples = new ArrayList<>();
        final List<ActionMoment> actionMoments = new ArrayList<>();
        public final Set<SpaceSimulation.SegmentRef> occupiedPortSegments = new HashSet<>();
        // Branch currently writing this state.
        UUID branchId;
        public UUID craftId;
        public UUID dockedHost = SpaceSimulation.FlightPlanAction.NO_TARGET;
        public RocketServiceSettings.HardwareRef dockedHostPort = RocketServiceSettings.HardwareRef.NONE;
        double dockedVisitorMass;
        // Shared-plane position, clock, and velocity carried between cards.
        public double x;
        public double y;
        public double time;
        double headingX;
        double headingY;
        public double velocityX;
        public double velocityY;
        // One-based stage selects which configured engines may fire.
        int currentStage = 1;
        // Terminal cards and unsafe arrivals stop this branch.
        boolean atmosphere;
        boolean maintainingPosition;
        boolean discarded;
        boolean destroyed;
        // Latest successful destination validates later asteroid connections.
        public UUID currentTarget = SpaceSimulation.FlightPlanAction.NO_TARGET;
        public SpaceSimulation.OrbitBand currentOrbit = SpaceSimulation.OrbitBand.LOW;
        // Attached asteroid and its anchor segment, if this craft carries one.
        SpaceSimulation.SpaceObjectData attachedAsteroid;
        SpaceSimulation.SegmentRef asteroidAnchor;
        // Landing settings stay with a released asteroid for its impact prediction.
        SpaceSimulation.FlightPlanAction lastEarthSurfaceAction;
        // Failure reason shown when a card cannot complete.
        public TerminalState blockedState = TerminalState.PLAN_BLOCKED;
        NavigationFailure navigationFailure;

        Craft(Map<SpaceSimulation.SegmentRef, Segment> segments,
              Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections,
              double x, double y, double time) {

            this.segments = segments;
            this.connections = connections;
            this.x = x;
            this.y = y;
            this.time = time;
        }

        public double mass() {

            var asteroidMass = attachedAsteroid == null ? 0
                    : attachedAsteroid.mass() * SpaceBalance.KILOGRAMS_PER_ASTEROID_MASS;
            return rocketMass() + asteroidMass + dockedVisitorMass;
        }

        double explosiveEnergy() {

            return segments.values().stream().mapToDouble(s -> s.payloadEnergy
                    + s.fuelTicks * SpaceBalance.FUEL_ENERGY_PER_BURN_TICK).sum();
        }

        double rocketMass() {

            return segments.values().stream().mapToDouble(segment -> segment.wetMass).sum();
        }

        List<SpaceSimulation.SegmentRef> activeSegments(Context context) {

            return segments.keySet().stream()
                    .filter(ref -> context.configuration(ref).usesEnginesDuring(currentStage))
                    .filter(ref -> segments.get(ref).thrust(false) > 0)
                    .toList();
        }

        boolean inAtmosphere() {

            return atmosphere;
        }

        double deltaVPerSecond(List<SpaceSimulation.SegmentRef> active) {

            var mass = Math.max(1, mass());
            var result = 0.0;
            for (var ref : active) {
                var segment = segments.get(ref);
                result += segment.thrust(inAtmosphere()) / mass;
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
                segment.consume(seconds);
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
            return segment.thrust(false) == 0;
        }

        double availableDeltaV(Context context) {

            return burnProfile(context).deltaV();
        }

        RocketBurnProfile burnProfile(Context context) {

            return burnProfile(context, List.of(), false);
        }

        RocketBurnProfile navigationBurnProfile(Context context, List<Burn> schedule) {

            return burnProfile(context, schedule, true);
        }

        private RocketBurnProfile burnProfile(Context context, List<Burn> schedule, boolean radio) {

            // Plan braking against a copy so looking ahead never consumes the real craft.
            var copy = copyFor(Set.copyOf(segments.keySet()));
            var intervals = new ArrayList<RocketBurnProfile.Interval>();
            var phase = 0;
            var remaining = schedule.isEmpty() ? Double.POSITIVE_INFINITY : schedule.getFirst().seconds();
            while (true) {
                if (remaining <= BURN_TOLERANCE) {
                    phase++;
                    remaining = phase < schedule.size() ? schedule.get(phase).seconds() : Double.POSITIVE_INFINITY;
                    continue;
                }
                var burn = phase < schedule.size() ? schedule.get(phase)
                        : new Burn(Double.POSITIVE_INFINITY, 1, schedule.isEmpty() ? 1 : schedule.getLast().fuelFactor(), atmosphere);
                if (!burn.firing()) {
                    if (radio) copy.spendRadio(remaining);
                    remaining = 0;
                    continue;
                }
                var active = copy.activeSegments(context);
                if (copy.currentStage < context.stageCount && (copy.stageFinished(context) || active.isEmpty())) {
                    for (var ref : copy.boostersEndingCurrentStage(context)) copy.removeSegment(ref);
                    copy.currentStage++;
                    continue;
                }
                if (active.isEmpty()) break;
                var rate = copy.deltaVPerSecond(active);
                if (rate <= 0) break;
                var factor = Math.max(1e-9, burn.fuelFactor());
                var seconds = Math.min(remaining, active.stream().map(copy.segments::get)
                        .mapToDouble(segment -> radio ? segment.burnSeconds(factor) : segment.seconds() / factor).min().orElse(0));
                if (seconds <= 0) break;
                copy.consumeBurnTime(active, seconds * factor);
                if (radio) copy.spendRadio(seconds);
                intervals.add(new RocketBurnProfile.Interval(rate, seconds * factor));
                remaining -= seconds;
                if (copy.stageFinished(context)) {
                    for (var ref : copy.boostersEndingCurrentStage(context)) copy.removeSegment(ref);
                    copy.currentStage = Math.min(context.stageCount, copy.currentStage + 1);
                }
            }
            return new RocketBurnProfile(intervals);
        }

        void spendRadio(double seconds) {

            segments.values().forEach(segment -> segment.spendRF(StationServiceRules.antennaRF(segment.hardware, seconds * 20)));
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
            copy.craftId = craftId;
            copy.dockedHost = dockedHost;
            copy.dockedHostPort = dockedHostPort;
            copy.occupiedPortSegments.addAll(occupiedPortSegments);
            copy.dockedVisitorMass = dockedVisitorMass;
            copy.atmosphere = atmosphere;
            copy.headingX = headingX;
            copy.headingY = headingY;
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

        public void addSample(PathPhase phase, UUID target, UUID actionId) {

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
                    List.copyOf(actionMoments), time, availableDeltaV(context), terminal, Optional.ofNullable(navigationFailure));
        }
    }
}
