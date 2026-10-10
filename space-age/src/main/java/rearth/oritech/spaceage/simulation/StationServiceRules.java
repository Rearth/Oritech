package rearth.oritech.spaceage.simulation;

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
