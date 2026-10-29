package rearth.oritech.spaceage.simulation.flight;

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
import rearth.oritech.spaceage.simulation.mission.DockingTarget;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.RocketLayout;
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
import java.util.function.Consumer;

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
        public final Map<UUID, DockingTarget> stations = new HashMap<>();
        public final Map<UUID, Craft> stationCraft = new HashMap<>();
        public final Map<UUID, Set<RocketServiceSettings.HardwareRef>> claimedPorts = new HashMap<>();
        public final List<RocketFlightPathCalculator.ProcessingEstimate> processingEstimates = new ArrayList<>();
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

        public final StaticRocketSegment structure;
        public final DynamicRocketSegment physical;
        public final double initialRF;
        public final double initialFuelTicks;
        public RocketHardware hardware;
        public double dryMass;
        public double payloadEnergy;
        double chemicalThrust;
        double ionThrust;
        int parachutes;
        private StaticRocketSegment currentStructure;

        public Segment(StaticRocketSegment structure, DynamicRocketSegment resources) {

            this.structure = structure;
            this.physical = resources.copy();
            this.initialRF = structure.initialRF();
            this.initialFuelTicks = structure.initialFuel();
            refreshLayout();
        }

        public Segment(double mass, double chemicalThrust, double ionThrust, double chemicalSeconds, double ionSeconds,
                       RocketHardware hardware) {

            this.dryMass = mass;
            this.chemicalThrust = chemicalThrust;
            this.ionThrust = ionThrust;
            this.hardware = hardware;
            this.structure = null;
            this.physical = new DynamicRocketSegment((long) Math.ceil(hardware.fuelCost(chemicalSeconds)),
                    (long) Math.ceil(hardware.rfCost(ionSeconds)), 0, Set.of());
            this.physical.setResources(hardware.fuelCost(chemicalSeconds), hardware.rfCost(ionSeconds));
            this.initialRF = physical.rf();
            this.initialFuelTicks = physical.fuel();
        }

        private Segment(Segment source, boolean resourcesOnly) {

            structure = source.structure;
            currentStructure = source.currentStructure;
            physical = resourcesOnly ? source.physical.copyResources() : source.physical.copy();
            hardware = source.hardware;
            chemicalThrust = source.chemicalThrust;
            ionThrust = source.ionThrust;
            dryMass = source.dryMass;
            payloadEnergy = source.payloadEnergy;
            parachutes = source.parachutes;
            initialRF = source.initialRF;
            initialFuelTicks = source.initialFuelTicks;
        }

        public StaticRocketSegment currentStructure() {

            return currentStructure;
        }

        public void refreshLayout() {

            var current = RocketLayout.current(structure, physical);
            currentStructure = current;
            hardware = RocketHardware.of(current);
            chemicalThrust = hardware.chemicalThrust();
            ionThrust = hardware.ionThrust();
            dryMass = current.staticWeight() * SpaceBalance.KILOGRAMS_PER_WEIGHT_UNIT;
            payloadEnergy = current.payloadEnergyJoules();
            parachutes = ParachuteLanding.count(current);
        }

        public double mass() {

            return dryMass + fuelMass();
        }

        public double fuelMass() {

            return physical.currentFuelWeight * SpaceBalance.KILOGRAMS_PER_WEIGHT_UNIT;
        }

        public double chemicalSeconds() {

            return hardware.chemicalSeconds(physical);
        }

        public double ionSeconds() {

            return hardware.ionSeconds(physical);
        }

        public double rf() {

            return physical.rf();
        }

        double seconds() {

            var chemical = chemicalSeconds();
            var ion = ionSeconds();
            return Math.min(chemical > BURN_TOLERANCE ? chemical : Double.POSITIVE_INFINITY,
                    ion > BURN_TOLERANCE ? ion : Double.POSITIVE_INFINITY);
        }

        public double thrust(boolean atmosphere) {

            return (chemicalSeconds() > BURN_TOLERANCE ? chemicalThrust : 0)
                    + (ionSeconds() > BURN_TOLERANCE ? ionThrust * (atmosphere ? SpaceBalance.ION_ATMOSPHERE : 1) : 0);
        }

        void consumeFuel(double seconds) {

            physical.consumeFuel(hardware.fuelCost(seconds));
        }

        public void consume(double seconds) {

            hardware.burn(physical, seconds);
        }

        /**
         * Current layouts may have changed their battery capacity since launch.
         */
        public double rfCapacity() {

            return physical.rfCapacity;
        }

        public void spendRF(double amount) {

            physical.spendRF(amount);
        }

    }

    /**
     * The moving craft and its branch-local history.
     */
    public static final class Craft {

        // Attached parts and their coupling graph. Separations split this graph.
        public final Map<SpaceSimulation.SegmentRef, Segment> segments;
        public final Set<SpaceSimulation.SegmentRef> occupiedPortSegments = new HashSet<>();
        final Map<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>> connections;
        // Only this branch's visual history and card outcomes.
        final List<PathSample> samples = new ArrayList<>();
        final List<ActionMoment> actionMoments = new ArrayList<>();
        public UUID craftId;
        public UUID dockedHost = SpaceSimulation.FlightPlanAction.NO_TARGET;
        public RocketServiceSettings.HardwareRef dockedHostPort = RocketServiceSettings.HardwareRef.NONE;
        // Shared-plane position, clock, and velocity carried between cards.
        public double x;
        public double y;
        public double time;
        public double velocityX;
        public double velocityY;
        // Latest successful destination validates later asteroid connections.
        public UUID currentTarget = SpaceSimulation.FlightPlanAction.NO_TARGET;
        public SpaceSimulation.OrbitBand currentOrbit = SpaceSimulation.OrbitBand.LOW;
        // Failure reason shown when a card cannot complete.
        public TerminalState blockedState = TerminalState.PLAN_BLOCKED;
        // Branch currently writing this state.
        UUID branchId;
        double dockedVisitorMass;
        double headingX;
        double headingY;
        // One-based stage selects which configured engines may fire.
        int currentStage = 1;
        // Terminal cards and unsafe arrivals stop this branch.
        boolean atmosphere;
        boolean maintainingPosition;
        boolean discarded;
        boolean destroyed;
        // Attached asteroid and its anchor segment, if this craft carries one.
        SpaceSimulation.SpaceObjectData attachedAsteroid;
        SpaceSimulation.SegmentRef asteroidAnchor;
        // Landing settings stay with a released asteroid for its impact prediction.
        SpaceSimulation.FlightPlanAction lastEarthSurfaceAction;
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
                    + s.physical.fuel() * SpaceBalance.FUEL_ENERGY_PER_BURN_TICK).sum();
        }

        double rocketMass() {

            var mass = 0d;
            for (var segment : segments.values()) mass += segment.mass();
            return mass;
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

            var anyBooster = false;
            var anyEnding = false;
            var allEmpty = true;
            var endingEmpty = true;
            for (var entry : segments.entrySet()) {
                var configuration = context.configuration(entry.getKey());
                if (!configuration.booster() || !configuration.usesEnginesDuring(currentStage)) continue;
                anyBooster = true;
                var empty = entry.getValue().thrust(false) == 0;
                allEmpty &= empty;
                if (configuration.lastEngineStage() == currentStage) {
                    anyEnding = true;
                    endingEmpty &= empty;
                }
            }
            return anyEnding ? endingEmpty : anyBooster && allEmpty;
        }

        boolean finishStage(Context context, Consumer<SpaceSimulation.SegmentRef> detach) {

            var canAdvance = currentStage < context.stageCount;
            if (!stageFinished(context) && !(canAdvance && activeSegments(context).isEmpty())) return false;
            var boosters = boostersEndingCurrentStage(context);
            boosters.forEach(detach);
            if (canAdvance) currentStage++;
            return canAdvance || !boosters.isEmpty();
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

            // Vacuum rating and a geometry estimate; navigation budgets resources in EventTransfer.
            var copy = copyForFlight();
            copy.atmosphere = false;
            var intervals = new ArrayList<RocketBurnProfile.Interval>();
            while (true) {
                while (copy.finishStage(context, copy::removeSegment)) {
                }
                var active = copy.activeSegments(context);
                if (active.isEmpty()) break;
                var rate = copy.deltaVPerSecond(active);
                var seconds = active.stream().map(copy.segments::get).mapToDouble(Segment::seconds).min().orElse(0);
                if (rate <= 0 || seconds <= 0) break;
                copy.consumeBurnTime(active, seconds);
                intervals.add(new RocketBurnProfile.Interval(rate, seconds));
            }
            return new RocketBurnProfile(intervals);
        }

        void spendRadio(double seconds) {

            segments.values().forEach(segment -> segment.spendRF(StationServiceRules.antennaRF(segment.hardware, seconds * 20)));
        }

        void removeSegment(SpaceSimulation.SegmentRef ref) {

            segments.remove(ref);
            if (ref.equals(asteroidAnchor)) clearAsteroidAttachment();
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

            return copyFor(component, false);
        }

        Craft copyForFlight() {

            return copyFor(segments.keySet(), true);
        }

        private Craft copyFor(Set<SpaceSimulation.SegmentRef> component, boolean resourcesOnly) {

            var copiedSegments = new LinkedHashMap<SpaceSimulation.SegmentRef, Segment>();
            var copiedConnections = new HashMap<SpaceSimulation.SegmentRef, Set<SpaceSimulation.SegmentRef>>();
            for (var ref : component) {
                copiedSegments.put(ref, new Segment(segments.get(ref), resourcesOnly));
                if (resourcesOnly) continue;
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
