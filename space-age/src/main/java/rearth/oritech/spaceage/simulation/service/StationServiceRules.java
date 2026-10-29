package rearth.oritech.spaceage.simulation.service;

import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddon;
import rearth.oritech.spaceage.simulation.SpaceSimulation.ActionAddonType;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathCalculator.StationKeepingEnd;
import rearth.oritech.spaceage.simulation.flight.RocketFlightPathState;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.power.SolarPower;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;
import rearth.oritech.spaceage.simulation.rocket.DynamicRocketSegment;
import rearth.oritech.spaceage.simulation.rocket.RocketHardware;
import rearth.oritech.spaceage.simulation.rocket.RocketPerformanceCalculator;
import rearth.oritech.spaceage.simulation.survey.SpaceCommunications;

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

    public static double rfPerSecond(RocketHardware hardware, double correctionRate) {

        return antennaRF(hardware, 20) + hardware.rfCost(correctionRate);
    }

    public static List<Budget> budgets(ActiveRocketData rocket) {

        return rocket.getStaticSegments().entrySet().stream().map(entry -> {
            var structure = entry.getValue();
            var resources = rocket.getDynamicSegments().get(entry.getKey());
            return new Budget(RocketHardware.of(structure), resources.fuel(), resources.rf(),
                    structure.initialFuel(), structure.initialRF());
        }).toList();
    }

    public static List<Budget> budgets(RocketFlightPathState.Craft craft) {

        return craft.segments.values().stream().map(segment -> new Budget(segment.hardware,
                segment.chemicalSeconds * segment.hardware.fuelCost(1), segment.rf,
                segment.initialFuelTicks, segment.initialRF)).toList();
    }

    public static Estimate solarEstimate(List<Budget> budgets, double solar, double services, ActionAddon condition) {

        var rf = budgets.stream().mapToDouble(Budget::rf).sum();
        var drain = services - solar;
        var duration = drain > 0 ? rf / drain / 20 : Double.POSITIVE_INFINITY;
        var end = drain > 0 ? StationKeepingEnd.RF : StationKeepingEnd.SUSTAINABLE;
        if (condition != null) {
            var limit = Double.POSITIVE_INFINITY;
            if (condition.type() == ActionAddonType.DURATION) limit = condition.value();
            else if (condition.type() == ActionAddonType.LOW_RF) {
                var initial = budgets.stream().mapToDouble(Budget::initialRF).sum();
                var threshold = initial * condition.value() / 100;
                if (rf <= threshold) limit = 0;
                else if (drain > 0) limit = (rf - threshold) / drain / 20;
            } else if (conditionReached(budgets, condition, 0, 0)) limit = 0;
            if (Double.isFinite(limit) && limit <= duration) {
                duration = limit;
                end = conditionEnd(condition);
            }
        }
        return new Estimate(Double.isFinite(duration) ? duration : 0, end);
    }

    /**
     * Beam output throttles itself at the reserve; only ordinary services can drain below it.
     */
    public static Estimate beamEstimate(List<Budget> budgets, double solar, double services, double limit, double reserve, ActionAddon condition) {

        var rf = budgets.stream().mapToDouble(Budget::rf).sum();
        var net = solar - services;
        var ticks = net < 0 ? beamThresholdTicks(rf, reserve, net, limit, 0) : Double.POSITIVE_INFINITY;
        var end = Double.isFinite(ticks) ? StationKeepingEnd.RF : StationKeepingEnd.SUSTAINABLE;
        if (condition != null) {
            var stop = Double.POSITIVE_INFINITY;
            if (condition.type() == ActionAddonType.DURATION) stop = condition.value() * 20d;
            else if (condition.type() == ActionAddonType.LOW_RF)
                stop = beamThresholdTicks(rf, reserve, net, limit, budgets.stream().mapToDouble(Budget::initialRF).sum() * condition.value() / 100);
            else if (conditionReached(budgets, condition, 0, 0)) stop = 0;
            if (Double.isFinite(stop) && stop <= ticks) {
                ticks = stop;
                end = conditionEnd(condition);
            }
        }
        return new Estimate(Double.isFinite(ticks) ? ticks / 20 : 0, end);
    }

    private static StationKeepingEnd conditionEnd(ActionAddon condition) {

        return switch (condition.type()) {
            case DURATION -> StationKeepingEnd.DURATION;
            case LOW_RF -> StationKeepingEnd.LOW_RF;
            default -> StationKeepingEnd.LOW_FUEL;
        };
    }

    private static double beamThresholdTicks(double rf, double reserve, double net, double limit, double threshold) {

        if (rf <= threshold) return 0;
        var ticks = 0d;
        if (rf > reserve) {
            var drain = limit - net;
            if (drain <= 0) return Double.POSITIVE_INFINITY;
            if (threshold >= reserve) return (rf - threshold) / drain;
            ticks = (rf - reserve) / drain;
            rf = reserve;
        }
        return net < 0 ? ticks + (rf - threshold) / -net : Double.POSITIVE_INFINITY;
    }

    public static double beamRemainingRF(double rf, double reserve, double net, double limit, double ticks) {

        if (rf > reserve) {
            var drain = limit - net;
            if (drain <= 0) return rf - drain * ticks;
            var first = Math.min(ticks, (rf - reserve) / drain);
            rf -= first * drain;
            ticks -= first;
        }
        if (net <= 0) return Math.max(0, rf + net * ticks);
        var filling = Math.min(ticks, Math.max(0, reserve - rf) / net);
        return rf + net * filling + Math.max(0, net - limit) * (ticks - filling);
    }

    public static double beamReserve(RocketFlightPathState.Craft craft, int percent) {

        return craft.segments.values().stream().mapToDouble(RocketFlightPathState.Segment::rfCapacity).sum() * percent / 100;
    }

    public static void limitRF(RocketFlightPathState.Craft craft, double remaining) {

        var spent = Math.max(0, craft.segments.values().stream().mapToDouble(segment -> segment.rf).sum() - remaining);
        for (var segment : craft.segments.values()) {
            var debit = Math.min(segment.rf, spent);
            segment.spendRF(debit);
            spent -= debit;
        }
    }

    public static Estimate estimate(List<Budget> budgets, double correctionRate, ActionAddon condition) {

        if (condition != null && condition.type() == ActionAddonType.DURATION) {
            var resources = estimate(budgets, correctionRate, null);
            return resources.seconds >= condition.value() ? new Estimate(condition.value(), StationKeepingEnd.DURATION) : resources;
        }
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
        if (conditionReached(budgets, condition, correctionRate, duration)) {
            var low = 0d;
            var high = duration;
            for (int i = 0; i < 64; i++) {
                var middle = (low + high) * .5;
                if (conditionReached(budgets, condition, correctionRate, middle)) high = middle;
                else low = middle;
            }
            if (high + 1e-7 < duration) {
                duration = high;
                end = conditionEnd(condition);
            }
        }
        return new Estimate(duration, end);
    }

    public static boolean conditionReached(List<Budget> budgets, ActionAddon condition, double correctionRate, double seconds) {

        if (condition == null) return false;
        if (condition.type() == ActionAddonType.DURATION) return seconds >= condition.value();
        var rf = condition.type() == ActionAddonType.LOW_RF;
        var initial = 0d;
        var remaining = 0d;
        for (var budget : budgets) {
            if (!rf && !budget.hardware.usesStationKeepingFuel()) continue;
            initial += rf ? budget.initialRF : budget.initialFuel;
            var rate = rf ? rfPerSecond(budget.hardware, correctionRate) : budget.hardware.fuelCost(correctionRate);
            remaining += Math.max(0, (rf ? budget.rf : budget.fuel) - rate * seconds);
        }
        return initial > 0 && remaining * 100 <= initial * condition.value();
    }

    public static boolean forecast(RocketFlightPathState.Craft craft, double ticks) {

        return forecast(craft, ticks, craft.mass(), true);
    }

    public static boolean forecast(RocketFlightPathState.Craft craft, double ticks, double mass, boolean generate) {

        if (ticks <= 0) return true;
        var solar = SolarPower.output(craft);
        if (solar > 0) {
            var rf = craft.segments.values().stream().mapToDouble(s -> s.rf).sum();
            var required = craft.segments.values().stream().mapToDouble(s -> antennaRF(s.hardware, ticks)).sum();
            if (rf + (generate ? solar * ticks : 0) < required) return false;
            var remaining = (generate ? solar * ticks : 0) - required;
            for (var segment : craft.segments.values()) {
                if (remaining >= 0) {
                    var accepted = Math.min(remaining, Math.max(0, segment.rfCapacity() - segment.rf));
                    segment.rf += accepted;
                    remaining -= accepted;
                } else {
                    var spent = Math.min(segment.rf, -remaining);
                    segment.spendRF(spent);
                    remaining += spent;
                }
            }
            return true;
        }
        var thrust = craft.segments.values().stream().mapToDouble(s -> s.thrust(false)).sum();
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

        var antennaReady = state.rocket.hardware().values().stream().noneMatch(s -> s.antennas() > 0)
                || SpaceCommunications.poweredAntennas(state, false) > 0;
        if (SolarPower.stationary(state) && SolarPower.output(state) > 0) {
            var rf = state.rocket.getDynamicSegments().values().stream().mapToDouble(DynamicRocketSegment::rf).sum();
            var capacity = state.rocket.getDynamicSegments().values().stream().mapToDouble(segment -> segment.rfCapacity).sum();
            return antennaReady || Math.min(capacity, rf + SolarPower.output(state)) >= SpaceBalance.ANTENNA_RF;
        }
        var performance = RocketPerformanceCalculator.calculate(state.rocket);
        return antennaReady && performance.thrustNewtons() > 0 && performance.availableBurnSeconds() >= burnSeconds(performance.thrustNewtons(),
                performance.wetMassKilograms(), SpaceBalance.STATION_KEEPING_INTERVAL_TICKS);
    }

    public static boolean upkeep(MissionState state, double mass, long ticks) {

        if (SolarPower.stationary(state) && SolarPower.output(state) > 0) return available(state);
        var performance = RocketPerformanceCalculator.calculate(state.rocket);
        if (performance.availableBurnSeconds() <= 0) return false;
        var seconds = burnSeconds(performance.thrustNewtons(), mass, ticks);
        if (performance.availableBurnSeconds() < seconds) return false;
        state.rocket.getStaticSegments().forEach((id, segment) -> RocketHardware.of(segment).burn(state.rocket.getDynamicSegments().get(id), seconds));
        return true;
    }

    public record Budget(RocketHardware hardware, double fuel, double rf, double initialFuel, double initialRF) {

    }

    public record Estimate(double seconds, StationKeepingEnd end) {

        public boolean conditionReached() {

            return end == StationKeepingEnd.LOW_RF || end == StationKeepingEnd.LOW_FUEL || end == StationKeepingEnd.DURATION;
        }
    }
}
