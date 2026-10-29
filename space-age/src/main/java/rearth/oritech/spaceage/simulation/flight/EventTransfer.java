package rearth.oritech.spaceage.simulation.flight;

import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentRef;
import rearth.oritech.spaceage.simulation.SpaceSimulation.SegmentConfiguration;
import rearth.oritech.spaceage.simulation.flight.FlightEnvironment.Cell;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Context;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.NavigationFailure;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState.Craft;
import rearth.oritech.spaceage.simulation.power.SolarPower;
import rearth.oritech.spaceage.simulation.service.StationServiceRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full-power event trials. Each interval is solved analytically, never by ticking the flight.
 */
final class EventTransfer {

    private static final double SPEED_TOLERANCE = .01;
    private static final int MAX_EVENTS = 1024;
    private final SegmentRef[] refs;
    private final SegmentConfiguration[] configurations;
    private final Craft source;
    private final Context context;
    private final List<Cell> environment;
    private final double length;
    private final double initial;
    private final double arrival;
    private final double efficiency;
    private final double credit;
    private final Map<SegmentRef, Double> solar = new HashMap<>();
    private boolean eventLimitReached;

    EventTransfer(Craft source, Context context, FlightCurve curve, double initial, double arrival, double efficiency, double credit) {

        this.source = source;
        this.refs = source.segments.keySet().toArray(SegmentRef[]::new);
        this.configurations = new SegmentConfiguration[refs.length];
        for (int i = 0; i < refs.length; i++) configurations[i] = context.configuration(refs[i]);
        this.context = context;
        this.length = curve.length();
        this.initial = initial;
        this.arrival = arrival;
        this.efficiency = efficiency;
        this.credit = credit;
        source.segments.forEach((ref, segment) -> solar.put(ref,
                segment.structure == null ? 0 : SolarPower.rating(List.of(segment.currentStructure())) * 20));
        this.environment = FlightEnvironment.along(curve, context, source.time, solar.values().stream().anyMatch(value -> value > 0));
    }

    static void apply(Craft craft, Map<SegmentRef, FlightResourceRate> rates, double seconds) {

        rates.forEach((ref, rate) -> {
            var segment = craft.segments.get(ref);
            if (segment == null) return;
            rate.apply(segment.physical, seconds);
        });
    }

    static double duration(List<Event> events) {

        return events.stream().mapToDouble(Event::seconds).sum();
    }

    List<Event> solve(double cap, boolean freeArrival) {

        if (!freeArrival && arrival > cap) return null;
        var acceleration = Math.max(.001, source.deltaVPerSecond(source.activeSegments(context)));
        if (solar.values().stream().anyMatch(value -> value > 0))
            acceleration = Math.max(acceleration, source.segments.values().stream()
                    .mapToDouble(segment -> segment.chemicalThrust + segment.ionThrust).sum() / Math.max(1, source.mass()));
        var distanceLimit = Math.sqrt(initial * initial + 2 * acceleration * length);
        var budgetLimit = initial + source.burnProfile(context).deltaV();
        var maximum = Math.min(cap, solar.values().stream().anyMatch(value -> value > 0)
                ? distanceLimit : Math.min(budgetLimit, distanceLimit));
        if (freeArrival) {
            var events = trial(maximum, length, true, true).events;
            if (events == null) reportLimit();
            return events;
        }
        Trial best = null;
        var bestPeak = 0d;
        // Sample first: gravity and charging can make the lowest peaks infeasible as well as the highest.
        for (int sample = 0; sample <= 8; sample++) {
            var peak = Math.max(arrival, maximum * sample / 8);
            var candidate = forPeak(peak);
            if (candidate != null && (best == null || candidate.duration < best.duration)) {
                best = candidate;
                bestPeak = peak;
            }
        }
        if (best == null) {
            reportLimit();
            return null;
        }
        var upper = Math.min(maximum, bestPeak + maximum / 8);
        for (int iteration = 0; iteration < 6; iteration++) {
            var peak = (bestPeak + upper) * .5;
            var candidate = forPeak(peak);
            if (candidate == null) upper = peak;
            else {
                bestPeak = peak;
                if (candidate.duration < best.duration) best = candidate;
            }
        }
        return trial(best.peak, best.brakeAt, false, true).events;
    }

    private Trial forPeak(double peak) {

        var low = 0d;
        var high = length;
        for (int iteration = 0; iteration < 40; iteration++) {
            var middle = (low + high) * .5;
            var trial = trial(peak, middle, false, false);
            if (trial.error > 0) high = middle;
            else low = middle;
            if (trial.fits) return trial;
        }
        return null;
    }

    private Trial trial(double peak, double brakeAt, boolean freeArrival, boolean recording) {

        var craft = source.copyForFlight();
        var events = recording ? new ArrayList<Event>() : null;
        var fuelRates = new double[refs.length];
        var rfRates = new double[refs.length];
        var firing = new boolean[refs.length];
        var elapsed = 0d;
        var checkStage = true;
        var s = 0d;
        var speed = initial;
        var cellIndex = 0;
        var cruising = Math.abs(speed - peak) < 1e-7;
        var factor = Math.max(1e-9, ParachuteLanding.fuelFactor(credit, Math.max(initial, peak) - arrival));
        for (int event = 0; event < MAX_EVENTS; event++) {
            while (cellIndex + 1 < environment.size() && s >= environment.get(cellIndex).end() - 1e-7) cellIndex++;
            var cell = environment.get(cellIndex);
            if (checkStage) {
                while (craft.finishStage(context, craft::removeSegment)) {
                }
                checkStage = false;
            }
            craft.atmosphere = cell.atmosphere();
            var braking = !freeArrival && s >= brakeAt - 1e-7;
            var direction = braking ? -1 : cruising ? 0 : speed > peak ? -1 : 1;
            var fuelFactor = direction < 0 ? factor : 1;
            var mass = Math.max(1, craft.mass());
            var charging = false;
            var thrust = 0d;
            var boundarySeconds = Double.POSITIVE_INFINITY;
            for (int i = 0; i < refs.length; i++) {
                var ref = refs[i];
                var segment = craft.segments.get(ref);
                if (segment == null) continue;
                var active = configurations[i].usesEnginesDuring(craft.currentStage);
                var generation = cell.atmosphere() ? 0 : solar.getOrDefault(ref, 0d) * cell.solar();
                var radio = StationServiceRules.antennaRF(segment.hardware, 20);
                var ion = segment.hardware.ion() > 0 && active && direction != 0 && (segment.rf() > 1e-7
                        || generation >= radio + segment.hardware.rfCost(fuelFactor));
                var chemical = active && direction != 0 && segment.chemicalSeconds() > 1e-8;
                var fuelRate = chemical ? segment.hardware.fuelCost(fuelFactor) : 0;
                var rfRate = radio + (ion ? segment.hardware.rfCost(fuelFactor) : 0) - generation;
                var capacity = segment.rfCapacity();
                if (rfRate > 0 && segment.rf() <= 1e-7 || rfRate < 0 && segment.rf() >= capacity - 1e-7) rfRate = 0;
                if (ion || chemical) {
                    thrust += (chemical ? segment.chemicalThrust : 0)
                            + (ion ? segment.ionThrust * (cell.atmosphere() ? SpaceBalance.ION_ATMOSPHERE : 1) : 0);
                }
                firing[i] = ion || chemical;
                fuelRates[i] = fuelRate;
                rfRates[i] = rfRate;
                charging |= rfRate < 0;
                if (chemical) {
                    boundarySeconds = Math.min(boundarySeconds, segment.chemicalSeconds() / fuelFactor);
                    // Re-evaluate acceleration after appreciable propellant mass changes.
                    if (segment.fuelMass() > mass * .02)
                        boundarySeconds = Math.min(boundarySeconds, segment.chemicalSeconds() / fuelFactor * mass * .02 / segment.fuelMass());
                }
                if (rfRate > 0) boundarySeconds = Math.min(boundarySeconds, segment.rf() / rfRate);
                else if (rfRate < 0)
                    boundarySeconds = Math.min(boundarySeconds, Math.max(0, capacity - segment.rf()) / -rfRate);
            }
            var engineAcceleration = thrust / mass * efficiency;
            if (engineAcceleration <= 1e-10) direction = 0;
            var acceleration = direction * engineAcceleration + cell.gravity();
            var end = Math.min(length, cell.end());
            if (!braking && !freeArrival) end = Math.min(end, brakeAt);
            var distance = Math.max(0, end - s);
            var squared = speed * speed + 2 * acceleration * distance;
            var seconds = squared >= 0 && speed + Math.sqrt(squared) > 1e-12
                    ? 2 * distance / (speed + Math.sqrt(squared)) : Double.POSITIVE_INFINITY;
            seconds = Math.min(seconds, boundarySeconds);
            var wanted = braking ? arrival : peak;
            if ((braking || !cruising) && acceleration != 0 && (wanted - speed) / acceleration >= 0)
                seconds = Math.min(seconds, (wanted - speed) / acceleration);
            if (acceleration < 0 && speed > 0) seconds = Math.min(seconds, -speed / acceleration);
            if (!Double.isFinite(seconds) || seconds <= 1e-9) {
                if (braking && speed <= arrival + SPEED_TOLERANCE)
                    return new Trial(s - length, elapsed, peak, brakeAt, false, null);
                return new Trial(direction == 0 && speed <= .001 ? -length : length, elapsed, peak, brakeAt, false, null);
            }
            var nextS = Math.min(end, s + speed * seconds + .5 * acceleration * seconds * seconds);
            var nextSpeed = Math.max(0, speed + acceleration * seconds);
            if (recording) {
                var rates = new HashMap<SegmentRef, FlightResourceRate>();
                var firingSegments = new HashSet<SegmentRef>();
                for (int i = 0; i < refs.length; i++) {
                    if (!craft.segments.containsKey(refs[i])) continue;
                    rates.put(refs[i], new FlightResourceRate(fuelRates[i], rfRates[i]));
                    if (firing[i]) firingSegments.add(refs[i]);
                }
                events.add(new Event(seconds, s, nextS, speed, nextSpeed, direction, fuelFactor,
                        cell.atmosphere(), Map.copyOf(rates), Set.copyOf(firingSegments)));
            }
            for (int i = 0; i < refs.length; i++) {
                var segment = craft.segments.get(refs[i]);
                if (segment == null) continue;
                var hadThrust = segment.thrust(false) > 0;
                new FlightResourceRate(fuelRates[i], rfRates[i]).apply(segment.physical, seconds);
                checkStage |= hadThrust && segment.thrust(false) == 0;
            }
            elapsed += seconds;
            s = nextS;
            speed = nextSpeed;
            if (!cruising && Math.abs(speed - peak) < 1e-6) cruising = true;
            if (s >= length - (freeArrival ? 1e-6 : .25)) {
                var stopped = Math.abs(speed - arrival) <= 1e-7
                        || acceleration < 0 && Math.abs(speed - arrival) <= SPEED_TOLERANCE;
                if (freeArrival ? speed <= peak + SPEED_TOLERANCE || initial <= peak : stopped) {
                    if (!freeArrival && recording) {
                        var last = events.removeLast();
                        events.add(new Event(last.seconds(), last.start(), length, last.initialSpeed(), arrival,
                                last.direction(), last.fuelFactor(), last.atmosphere(), last.rates(), last.firing()));
                    }
                    return new Trial(0, elapsed, peak, brakeAt, true, events);
                }
                return new Trial(speed > arrival ? length : -length, elapsed, peak, brakeAt, false, null);
            }
            if (braking && speed <= arrival + 1e-7) return new Trial(s - length, elapsed, peak, brakeAt, false, null);
            if (speed <= 1e-8 && acceleration <= 0 && !charging)
                return new Trial(-length, elapsed, peak, brakeAt, false, null);
        }
        eventLimitReached = true;
        return new Trial(length, elapsed, peak, brakeAt, false, null);
    }

    private void reportLimit() {

        if (eventLimitReached)
            source.navigationFailure = new NavigationFailure("calculation_limit", MAX_EVENTS, MAX_EVENTS);
    }

    record Event(double seconds, double start, double end, double initialSpeed, double endSpeed,
                 double direction, double fuelFactor, boolean atmosphere,
                 Map<SegmentRef, FlightResourceRate> rates, Set<SegmentRef> firing) {
    }

    private record Trial(double error, double duration, double peak, double brakeAt, boolean fits, List<Event> events) {
    }
}
