package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.simulation.RocketFlightPathCalculator.StationKeepingEnd;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;

import java.util.List;

/**
 * Service upkeep shares the same full-power correction cost as Maintain Position.
 */
public final class StationServiceRules {

    private StationServiceRules() {
    }

    public static double burnSeconds(double thrust, double mass, double ticks) {

        return SpaceBalance.stationKeepingBurnSecondsPerSecond(thrust, mass) * ticks / 20;
    }

    public static double antennaRF(RocketHardware hardware, double ticks) {

        return hardware.antennas() * SpaceBalance.ANTENNA_RF * ticks;
    }

    static double rfPerSecond(RocketHardware hardware, double correctionRate) {

        return antennaRF(hardware, 20) + hardware.rfCost(correctionRate);
    }

    static List<Budget> budgets(ActiveRocketData rocket) {

        return rocket.getStaticSegments().entrySet().stream().map(entry -> {
            var structure = entry.getValue();
            var resources = rocket.getDynamicSegments().get(entry.getKey());
            return new Budget(RocketHardware.of(structure), resources.fuel(), resources.rf(),
                    structure.initialFuel(), structure.initialRF());
        }).toList();
    }

    static List<Budget> budgets(RocketFlightPathState.Craft craft) {

        return craft.segments.values().stream().map(segment -> new Budget(segment.hardware,
                segment.chemicalSeconds * segment.hardware.fuelCost(1), segment.rf,
                segment.initialFuelTicks, segment.initialRF)).toList();
    }

    static Estimate estimate(List<Budget> budgets, double correctionRate, ActionAddon condition) {

        var propulsionSeconds = 0d;
        var antennaSeconds = Double.POSITIVE_INFINITY;
        var hasAntenna = false;
        var end = StationKeepingEnd.FUEL;
        for (var budget : budgets) {
            var hardware = budget.hardware;
            var fuelRate = hardware.fuelCost(correctionRate);
            var rfRate = rfPerSecond(hardware, correctionRate);
            if (fuelRate > 0 && budget.fuel / fuelRate > propulsionSeconds) {
                propulsionSeconds = budget.fuel / fuelRate;
                end = StationKeepingEnd.FUEL;
            }
            if (hardware.ion() > 0 && rfRate > 0 && budget.rf / rfRate > propulsionSeconds) {
                propulsionSeconds = budget.rf / rfRate;
                end = StationKeepingEnd.RF;
            }
            if (hardware.antennas() > 0) {
                if (!hasAntenna) antennaSeconds = 0;
                hasAntenna = true;
                if (rfRate > 0) antennaSeconds = Math.max(antennaSeconds, budget.rf / rfRate);
            }
        }
        var duration = Math.min(propulsionSeconds, antennaSeconds);
        if (antennaSeconds <= propulsionSeconds) end = StationKeepingEnd.RF;
        if (condition != null && conditionReached(budgets, condition, correctionRate, duration)) {
            var low = 0d;
            var high = duration;
            for (int i = 0; i < 64; i++) {
                var middle = (low + high) * .5;
                if (conditionReached(budgets, condition, correctionRate, middle)) high = middle;
                else low = middle;
            }
            if (high + 1e-7 < duration) {
                duration = high;
                end = condition.type() == ActionAddonType.LOW_RF ? StationKeepingEnd.LOW_RF : StationKeepingEnd.LOW_FUEL;
            }
        }
        return new Estimate(duration, end);
    }

    static boolean conditionReached(List<Budget> budgets, ActionAddon condition, double correctionRate, double seconds) {

        if (condition == null) return false;
        var rf = condition.type() == ActionAddonType.LOW_RF;
        var initial = 0d;
        var remaining = 0d;
        for (var budget : budgets) {
            if (rf ? !budget.hardware.usesStationKeepingRF() : !budget.hardware.usesStationKeepingFuel()) continue;
            initial += rf ? budget.initialRF : budget.initialFuel;
            var rate = rf ? rfPerSecond(budget.hardware, correctionRate) : budget.hardware.fuelCost(correctionRate);
            remaining += Math.max(0, (rf ? budget.rf : budget.fuel) - rate * seconds);
        }
        return initial > 0 && remaining * 100 <= initial * condition.value();
    }

    record Budget(RocketHardware hardware, double fuel, double rf, double initialFuel, double initialRF) {

    }

    record Estimate(double seconds, StationKeepingEnd end) {

        boolean conditionReached() {

            return end == StationKeepingEnd.LOW_RF || end == StationKeepingEnd.LOW_FUEL;
        }
    }

    static boolean forecast(RocketFlightPathState.Craft craft, double ticks) {

        return forecast(craft, ticks, craft.mass());
    }

    static boolean forecast(RocketFlightPathState.Craft craft, double ticks, double mass) {

        var thrust = craft.segments.values().stream().mapToDouble(s -> s.thrust(false)).sum();
        if (ticks <= 0) return true;
        if (thrust <= 0) return false;
        var seconds = burnSeconds(thrust, mass, ticks);
        var available = craft.segments.values().stream().mapToDouble(s -> Math.max(s.chemicalSeconds, s.ionSeconds)).max().orElse(0);
        if (available < seconds) return false;
        for (var segment : craft.segments.values()) {
            segment.consume(seconds);
            segment.spendRF(antennaRF(segment.hardware, ticks));
        }
        return craft.segments.values().stream().noneMatch(s -> s.hardware.antennas() > 0)
                || craft.segments.values().stream().anyMatch(s -> s.hardware.antennas() > 0 && s.rf >= SpaceBalance.ANTENNA_RF);
    }

    public static boolean available(MissionState state) {

        var performance = RocketPerformanceCalculator.calculate(state.rocket);
        return (state.rocket.getStaticSegments().values().stream().noneMatch(s -> RocketHardware.of(s).antennas() > 0)
                || SpaceCommunications.poweredAntennas(state, false) > 0)
                && performance.thrustNewtons() > 0 && performance.availableBurnSeconds() >= burnSeconds(performance.thrustNewtons(),
                        performance.wetMassKilograms(), SpaceBalance.STATION_KEEPING_INTERVAL_TICKS);
    }

    public static boolean upkeep(MissionState state, double mass, long ticks) {

        var performance = RocketPerformanceCalculator.calculate(state.rocket);
        if (performance.availableBurnSeconds() <= 0) return false;
        var seconds = burnSeconds(performance.thrustNewtons(), mass, ticks);
        if (performance.availableBurnSeconds() < seconds) return false;
        state.rocket.getStaticSegments().forEach((id, segment) -> RocketHardware.of(segment).burn(state.rocket.getDynamicSegments().get(id), seconds));
        return true;
    }
}
